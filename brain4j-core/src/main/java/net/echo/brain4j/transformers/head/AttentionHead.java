package net.echo.brain4j.transformers.head;

import net.echo.brain4j.model.initialization.WeightInitializer;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;

import java.util.Random;

public class AttentionHead {

    protected final int inputDimension;
    protected final int headDimension;

    protected final Tensor queryWeightsTensor;
    protected final Tensor keyWeightsTensor;
    protected final Tensor valueWeightsTensor;

    public AttentionHead(WeightInitializer weightInit, int inputDimension, int headDimension) {
        this.inputDimension = inputDimension;
        this.headDimension = headDimension;

        this.queryWeightsTensor = TensorFactory.matrix(inputDimension, headDimension);
        this.keyWeightsTensor = TensorFactory.matrix(inputDimension, headDimension);
        this.valueWeightsTensor = TensorFactory.matrix(inputDimension, headDimension);

        initializeWeights(weightInit);
    }

    public int size() {
        return 3 * inputDimension * headDimension;
    }

    public Tensor attend(Tensor input) {
        Tensor Q = input.matmul(queryWeightsTensor);
        Tensor K = input.matmul(keyWeightsTensor);
        Tensor V = input.matmul(valueWeightsTensor);

        double normalizer = Math.sqrt(headDimension);

        Tensor scores = Q.matmul(K.transpose()).div(normalizer);
        Tensor attentionWeights = scores.softmax();

        return attentionWeights.matmul(V);
    }

    protected void initializeWeights(WeightInitializer initializer) {
        Random rng = new Random();

        double bound = initializer.getBound(inputDimension, headDimension);

        for (int i = 0; i < inputDimension; i++) {
            for (int j = 0; j < headDimension; j++) {
                queryWeightsTensor.set(rng.nextDouble(2 * bound) - bound, i, j);
                keyWeightsTensor.set(rng.nextDouble(2 * bound) - bound, i, j);
                valueWeightsTensor.set(rng.nextDouble(2 * bound) - bound, i, j);
            }
        }
    }
}
