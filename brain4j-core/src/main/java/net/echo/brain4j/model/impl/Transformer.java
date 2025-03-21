package net.echo.brain4j.model.impl;

import net.echo.math4j.DataSet;
import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.loss.LossFunction;
import net.echo.brain4j.loss.LossFunctions;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;
import net.echo.brain4j.model.Model;
import net.echo.brain4j.model.initialization.WeightInit;
import net.echo.brain4j.model.initialization.WeightInitializer;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.brain4j.training.evaluation.EvaluationResult;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.training.updater.Updater;
import net.echo.brain4j.training.updater.impl.StochasticUpdater;
import net.echo.brain4j.transformers.TransformerEncoder;
import net.echo.math4j.math.vector.Vector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Transformer extends Model<Object, List<Vector>, List<Vector>> {

    @SafeVarargs
    public Transformer(Layer<List<Tensor>, List<Tensor>>... layers) {
        super(layers);
    }

    @Override
    public Transformer compile(LossFunction function, Optimizer optimizer) {
        return compile(WeightInit.UNIFORM_XAVIER.getFunction(), function, optimizer, new StochasticUpdater());
    }

    @Override
    public Transformer compile(LossFunctions function, Optimizer optimizer) {
        return compile(function.getFunction(), optimizer);
    }

    @Override
    public Transformer compile(WeightInit initializer, LossFunctions lossFunction, Optimizer optimizer, Updater updater) {
        return compile(initializer.getFunction(), lossFunction.getFunction(), optimizer, updater);
    }

    @Override
    public Transformer compile(WeightInitializer initializer, LossFunction lossFunction, Optimizer optimizer, Updater updater) {
        super.compile(initializer, lossFunction, optimizer, updater);

        connect(initializer, true);

        return this;
    }

    @Override
    public EvaluationResult evaluate(DataSet<Object> dataSet) {
        return null;
    }

    @Override
    public void connect(WeightInitializer weightInit, boolean update) {
        super.connect(weightInit, update);
    }

    @Override
    public double loss(DataSet<Object> dataSet) {
        return 0;
    }

    @Override
    public void fit(DataSet<Object> dataSet) {
        throw new UnsupportedOperationException("Not implemented yet for this class.");
    }

    @Override
    public List<Vector> predict(StatesCache cache, List<Vector> input, boolean training) {
        List<Vector> result = new ArrayList<>(input);

        for (Layer<?, ?> layer : layers) {
            if (layer instanceof TransformerEncoder encoder) {
                // TODO: temporary fix, must be replaced
                List<Tensor> tensorInput = input.stream()
                    .map(TensorFactory::vector)
                    .toList();
                
                List<Tensor> tensorOutput = encoder.forward(cache, layer, tensorInput);
                
                result = tensorOutput.stream()
                    .map(tensor -> Vector.of(tensor.toArray()))
                    .toList();
            }
        }

        return result;
    }

    @Override
    public void reloadWeights() {

    }

    @Override
    public void serialize(DataOutputStream stream) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet for this class.");
    }

    @Override
    public void deserialize(DataInputStream stream) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet for this class.");
    }
}
