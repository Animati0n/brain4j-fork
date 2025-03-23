package net.echo.brain4j.transformers;

import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;
import net.echo.math4j.math.tensor.index.Range;

public class VocabularyMapper extends Layer<Tensor, Tensor> {

    private final Tensor outProjectionWeights;
    private final int vocabularySize;

    public VocabularyMapper(int vocabularySize, int dimension) {
        this.vocabularySize = vocabularySize;
        this.outProjectionWeights = TensorFactory.random(dimension, vocabularySize); // TODO: matmul support for 1d tensors
    }

    @Override
    public Tensor forward(StatesCache cache, Layer<?, ?> lastLayer, Tensor input) {
        int rows = input.shape()[0];
        int columns = input.shape()[1];

        Range range = new Range((rows - 1) * columns, rows * columns);

        Tensor sliced = input.reshape(columns * rows).slice(range);
        Tensor reshaped = sliced.reshape(1, columns);

        Tensor result = reshaped.matmul(outProjectionWeights).reshape(vocabularySize);
        return result.softmax();
    }

    public Tensor getOutProjectionWeights() {
        return outProjectionWeights;
    }
}
