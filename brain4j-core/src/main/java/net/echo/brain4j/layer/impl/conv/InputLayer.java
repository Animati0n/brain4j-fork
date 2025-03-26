package net.echo.brain4j.layer.impl.conv;

import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.structure.StatesCache;
import net.echo.math4j.math.tensor.Tensor;

public class InputLayer extends Layer {

    private int width;
    private int height;

    private InputLayer() {
    }

    public InputLayer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Tensor forward(StatesCache cache, Layer lastLayer, Tensor input) {
        int[] shape = input.shape();

        if (input.elements() != width * height) {
            throw new IllegalArgumentException(String.format("Input dimension does not match! (%sx%s) != (%sx%s)",
                    width, height, shape[0], shape[1]));
        }

        return input.reshape(1, width, height);
    }

    @Override
    public int getTotalNeurons() {
        return width * height;
    }

    @Override
    public int getTotalParams() {
        return 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
