package net.echo.brain4j.model.impl;

import com.google.common.base.Preconditions;
import net.echo.brain4j.convolution.Kernel;
import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.layer.impl.DenseLayer;
import net.echo.brain4j.layer.impl.DropoutLayer;
import net.echo.brain4j.layer.impl.convolution.ConvLayer;
import net.echo.brain4j.layer.impl.convolution.FlattenLayer;
import net.echo.brain4j.layer.impl.convolution.InputLayer;
import net.echo.brain4j.layer.impl.convolution.PoolingLayer;
import net.echo.brain4j.loss.LossFunctions;
import net.echo.brain4j.model.Model;
import net.echo.brain4j.model.initialization.WeightInit;
import net.echo.brain4j.structure.Neuron;
import net.echo.brain4j.structure.Synapse;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.brain4j.training.BackPropagation;
import net.echo.brain4j.training.data.DataRow;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.training.updater.Updater;
import net.echo.brain4j.training.updater.impl.StochasticUpdater;
import net.echo.brain4j.utils.DataSet;
import net.echo.brain4j.utils.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static net.echo.brain4j.utils.MLUtils.waitAll;

public class Sequential extends Model<DataRow, Vector, Vector> {

    protected BackPropagation propagation;

    public Sequential(Layer<?, ?>... layers) {
        super(layers);

        if (this.layers.isEmpty()) return;

        validateLayers();
    }

    private void validateLayers() {
        boolean isInput = layers.getFirst() instanceof InputLayer;
        boolean hasConv = false;

        for (Layer<?, ?> layer : layers) {
            if (layer instanceof ConvLayer || layer instanceof PoolingLayer || layer instanceof FlattenLayer) {
                hasConv = true;
                break;
            }
        }

        if (isInput && !hasConv) throw new IllegalArgumentException("Cannot use the InputLayer outside of a convolutional model!");
        if (!isInput && hasConv) throw new IllegalArgumentException("Cannot use a convolutional layer without an InputLayer!");
    }

    private Thread predictPartition(List<DataRow> partition, AtomicReference<Double> totalError) {
        return Thread.startVirtualThread(() -> {
            for (DataRow row : partition) {
                Vector inputs = row.inputs();
                Vector targets = row.outputs();

                Vector outputs = predict(inputs);
                double loss = lossFunction.getFunction().calculate(targets, outputs);

                totalError.updateAndGet(v -> v + loss);
            }
        });
    }

    @Override
    public Sequential compile(LossFunctions function, Optimizer optimizer) {
        return this.compile(WeightInit.UNIFORM_XAVIER, function, optimizer, new StochasticUpdater());
    }

    @Override
    public Sequential compile(WeightInit weightInit, LossFunctions function, Optimizer optimizer, Updater updater) {
        super.compile(weightInit, function, optimizer, updater);

        this.propagation = new BackPropagation(this, optimizer, updater);

        connect(weightInit, true);

        this.optimizer.postInitialize(this);
        this.updater.postInitialize(this);

        reloadMatrices();
        return this;
    }

    @Override
    public double evaluate(DataSet<DataRow> set) {
        reloadMatrices();

        AtomicReference<Double> totalError = new AtomicReference<>(0.0);
        List<Thread> threads = new ArrayList<>();

        for (List<DataRow> partition : set.getPartitions()) {
            threads.add(predictPartition(partition, totalError));
        }

        waitAll(threads);
        return totalError.get() / set.size();
    }

    @Override
    public void fit(DataSet<DataRow> dataSet) {
        propagation.iteration(dataSet);
    }

    @Override
    public Vector predict(Vector input) {
        return predict(new StatesCache(), input);
    }

    @Override
    public Vector predict(StatesCache cache, Vector input) {
        Layer<?, ?> firstLayer = layers.getFirst();

        Preconditions.checkState(input.size() == firstLayer.getTotalNeurons(), "Input dimension does not " +
                "match model input dimension! (Input != Expected " + input.size() + " != " + firstLayer.getTotalNeurons() + ")");

        Layer<?, ?> lastLayer = firstLayer;

        Kernel convInput = null;
        Vector denseInput = input.clone();

        firstLayer.setInput(cache, denseInput);

        if (firstLayer instanceof InputLayer inputLayer) {
            convInput = inputLayer.getImage(cache);
        }

        for (int l = 1; l < layers.size(); l++) {
            Layer<?, ?> layer = layers.get(l);

            if (layer instanceof DropoutLayer) continue;

            if (layer instanceof ConvLayer convLayer) {
                convInput = convLayer.forward(cache, lastLayer, convInput);
            }

            if (layer instanceof PoolingLayer poolingLayer) {
                convInput = poolingLayer.forward(cache, lastLayer, convInput);
            }

            if (layer instanceof FlattenLayer flattenLayer) {
                denseInput = flattenLayer.flatten(cache, lastLayer, convInput);
            }

            if (layer instanceof DenseLayer denseLayer) {
                denseInput = denseLayer.forward(cache, lastLayer, denseInput);
            }

            lastLayer = layer;
        }

        Layer<?, ?> outputLayer = layers.getLast();
        Vector output = new Vector(outputLayer.getTotalNeurons());

        for (int i = 0; i < output.size(); i++) {
            output.set(i, outputLayer.getNeuronAt(i).getValue(cache));
        }

        return output;
    }

    @Override
    public void reloadMatrices() {
        Layer<?, ?> lastLayer = layers.getFirst();

        for (int i = 1; i < layers.size(); i++) {
            Layer<?, ?> layer = layers.get(i);

            if (!(layer instanceof DenseLayer)) continue;

            List<Neuron> neurons = layer.getNeurons();
            Vector[] synapseMatrixLayer = recalculateSynapseMatrix(lastLayer.getSynapses(), lastLayer.getTotalNeurons(), neurons.size());

            lastLayer.updateWeights(synapseMatrixLayer);
            lastLayer = layer;
        }
    }

    /**
     * Recalculates the synapse matrix, used to cache the synapse weights for faster computation.
     *
     * @param synapses list of synapses to cache
     * @param inSize input size of the vector
     * @param outSize output size of the vector
     *
     * @return the synapse matrix
     */
    public Vector[] recalculateSynapseMatrix(List<Synapse> synapses, int inSize, int outSize) {
        Vector[] synapseMatrix = new Vector[outSize];

        for (int i = 0; i < outSize; i++) {
            Vector vector = new Vector(inSize);
            synapseMatrix[i] = vector;

            for (int j = 0; j < inSize; j++) {
                Synapse synapse = synapses.get(j * outSize + i);
                vector.set(j, synapse.getWeight());
            }
        }

        return synapseMatrix;
    }
}
