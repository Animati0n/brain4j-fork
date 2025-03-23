package net.echo.brain4j.transformers.masked;

import net.echo.brain4j.model.initialization.WeightInitializer;
import net.echo.brain4j.transformers.attention.AttentionHead;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;

public class MaskedAttentionHead extends AttentionHead {

    public MaskedAttentionHead(WeightInitializer weightInit, int inputDimension, int headDimension, double temperature) {
        super(weightInit, inputDimension, headDimension, temperature);
    }

    @Override
    public Tensor attend(Tensor input) {
        Tensor Q = input.matmul(queryWeightsTensor);
        Tensor K = input.matmul(keyWeightsTensor);
        Tensor V = input.matmul(valueWeightsTensor);

        double normalizer = Math.sqrt(headDimension);

        Tensor scores = Q.matmul(K.transpose()).div(normalizer);
        Tensor mask = TensorFactory.triangularMask(scores.shape()[0]);

        Tensor maskedScores = scores.add(mask);
        Tensor attentionWeights = maskedScores.softmax();

        return attentionWeights.matmul(V);
    }
}
