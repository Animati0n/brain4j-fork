package net.echo.brain4j.transformers;

import net.echo.brain4j.activation.Activations;
import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.layer.impl.DenseLayer;
import net.echo.brain4j.layer.impl.LayerNorm;
import net.echo.brain4j.loss.LossFunction;
import net.echo.brain4j.model.impl.Sequential;
import net.echo.brain4j.model.initialization.WeightInitializer;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.training.updater.Updater;
import net.echo.brain4j.transformers.attention.MultiHeadAttention;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;

import java.util.ArrayList;
import java.util.List;

public class TransformerEncoder extends Layer<Tensor, Tensor> {

    protected final Sequential feedForward;
    protected final LayerNorm normalizer;

    protected final int heads;
    protected final int dimension;

    private MultiHeadAttention attention;

    public TransformerEncoder(int numHeads, int dimension) {
        super(0, Activations.LINEAR);

        this.normalizer = new LayerNorm();
        this.feedForward = new Sequential(
                new DenseLayer(dimension, Activations.LINEAR),
                new DenseLayer(4 * dimension, Activations.GELU),
                new DenseLayer(dimension, Activations.LINEAR)
        );

        this.heads = numHeads;
        this.dimension = dimension;
    }

    @Override
    public int getTotalParams() {
        return getAttentionSize() + getFeedForwardSize();
    }

    @Override
    public int getTotalNeurons() {
        return feedForward.getTotalNeurons();
    }

    @Override
    public void compile(WeightInitializer weightInit, LossFunction lossFunction, Optimizer optimizer, Updater updater) {
        this.attention = new MultiHeadAttention(weightInit, heads, dimension);
        this.feedForward.compile(weightInit, lossFunction, optimizer, updater);
    }

    @Override
    public Tensor forward(StatesCache cache, Layer<?, ?> lastLayer, Tensor input) {
        Tensor attended = getAttention().attend(input);
        Tensor normalized = normalizer.normalize(attended.add(input));

        List<Tensor> normAttention = TensorFactory.toList(normalized);
        List<Tensor> feedForwardOutput = new ArrayList<>();

        for (Tensor tensor : normAttention) {
            Tensor output = feedForward.predict(tensor);
            feedForwardOutput.add(output.reshape(1, dimension));
        }

        Tensor merged = TensorFactory.mergeTensors(feedForwardOutput);
        return normalizer.normalize(merged.add(normalized));
    }

    public MultiHeadAttention getAttention() {
        return attention;
    }

    public Sequential getFeedForward() {
        return feedForward;
    }

    public LayerNorm getNormalizer() {
        return normalizer;
    }

    public int getAttentionSize() {
        return getAttention().getTotalNeurons();
    }

    public int getFeedForwardSize() {
        return feedForward.getTotalWeights();
    }
}


