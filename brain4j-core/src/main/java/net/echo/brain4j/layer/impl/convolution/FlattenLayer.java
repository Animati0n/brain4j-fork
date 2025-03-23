package net.echo.brain4j.layer.impl.convolution;

import com.google.common.base.Preconditions;
import net.echo.brain4j.activation.Activations;
import net.echo.brain4j.convolution.Kernel;
import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.layer.impl.DenseLayer;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.math4j.math.tensor.Tensor;
import net.echo.math4j.math.tensor.TensorFactory;
import net.echo.math4j.math.vector.Vector;

public class FlattenLayer extends DenseLayer {

    public FlattenLayer(int input) {
        super(input, Activations.LINEAR);
    }

    @Override
    public Tensor forward(StatesCache cache, Layer<?, ?> lastLayer, Tensor input) {
        return input;
    }

    public Tensor flatten(StatesCache cache, Layer<?, ?> layer, Kernel input) {
        Preconditions.checkNotNull(input, "Last convolutional input is null! Missing an input layer?");

        boolean isConvolutional = layer instanceof ConvLayer || layer instanceof PoolingLayer;

        Tensor output = TensorFactory.create(getTotalNeurons());

        Preconditions.checkState(isConvolutional, "Flatten layer is not preceded by convolutional layer!");
        Preconditions.checkState(getTotalNeurons() == input.size(),
                "Flatten dimension != Conv dimension (" + getTotalNeurons() + " != " + input.size() + ")");

        for (int h = 0; h < input.getHeight(); h++) {
            for (int w = 0; w < input.getWidth(); w++) {
                double value = input.getValue(w, h);
                int index = h * input.getWidth() + w;

                output.set(value, index);
                neurons.get(index).setValue(cache, value);
            }
        }

        return output;
    }
}
