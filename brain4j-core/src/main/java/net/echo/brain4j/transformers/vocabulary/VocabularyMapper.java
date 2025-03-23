package net.echo.brain4j.transformers.vocabulary;

import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.loss.LossFunction;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;
import net.echo.math4j.math.tensor.index.Range;

import java.util.Arrays;
import java.util.List;

public class VocabularyMapper extends Layer<Tensor, Tensor> {

    private final Tensor outProjectionWeights;
    private final int vocabularySize;
    private final double temperature;

    public VocabularyMapper(int vocabularySize, int dimension, double temperature) {
        this.vocabularySize = vocabularySize;
        this.outProjectionWeights = TensorFactory.random(dimension, vocabularySize); // TODO: matmul support for 1d tensors
        this.temperature = Math.max(1e-15, temperature);
    }

    @Override
    public void computeLoss(StatesCache cache, Tensor targets, Tensor outputs, LossFunction lossFunction) {
        Tensor delta = outputs.clone().sub(targets);

        // delta as a 1 x vocabSize matrix
        Tensor gradZ = delta.reshape(1, vocabularySize);
        Tensor gradW = cache.getOutputTensor(this).transpose().matmul(gradZ);

        double learningRate = 0.01;
        outProjectionWeights.sub(gradW.mul(learningRate));

        cache.setDeltaTensor(this, delta);
    }

    @Override
    public Tensor forward(StatesCache cache, Layer<?, ?> lastLayer, Tensor input) {
        int rows = input.shape()[0];
        int columns = input.shape()[1];

        cache.setInputTensor(this, input);

        Range range = new Range((rows - 1) * columns, rows * columns);

        Tensor sliced = input.reshape(columns * rows).slice(range);
        Tensor reshaped = sliced.reshape(1, columns);

        cache.setOutputTensor(this, reshaped);

        Tensor result = reshaped.matmul(outProjectionWeights);
        return result.reshape(vocabularySize).softmax(temperature);
    }

    @Override
    public int getTotalParams() {
        return outProjectionWeights.elements();
    }
}
