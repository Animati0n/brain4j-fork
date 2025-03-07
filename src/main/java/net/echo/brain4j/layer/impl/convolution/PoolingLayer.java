package net.echo.brain4j.layer.impl.convolution;

import com.google.common.base.Preconditions;
import net.echo.brain4j.activation.Activations;
import net.echo.brain4j.convolution.Kernel;
import net.echo.brain4j.convolution.pooling.PoolingType;
import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.structure.Neuron;
import net.echo.brain4j.structure.cache.Parameters;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.training.updater.Updater;

public class PoolingLayer extends Layer<Kernel, Kernel> {

    protected final PoolingType poolingType;
    protected final int kernelWidth;
    protected final int kernelHeight;
    protected int stride;
    protected int padding;

    public PoolingLayer(PoolingType poolingType, int kernelWidth, int kernelHeight) {
        this(poolingType, kernelWidth, kernelHeight, 1, 0);
    }

    public PoolingLayer(PoolingType poolingType, int kernelWidth, int kernelHeight, int stride) {
        this(poolingType, kernelWidth, kernelHeight, stride, 0);
    }

    public PoolingLayer(PoolingType poolingType, int kernelWidth, int kernelHeight, int stride, int padding) {
        super(kernelWidth * kernelHeight, Activations.LINEAR);
        this.id = Parameters.TOTAL_CONV_LAYER++;
        this.poolingType = poolingType;
        this.kernelHeight = kernelHeight;
        this.kernelWidth = kernelWidth;
        this.stride = stride;
        this.padding = padding;
    }

    @Override
    public boolean isConvolutional() {
        return true;
    }

    @Override
    public Kernel forward(StatesCache cache, Layer<?, ?> lastLayer, Kernel input) {
        Preconditions.checkNotNull(input, "Last convolutional input is null");

        double initialWidth = input.getWidth() - kernelWidth + 2 * padding;
        double initialHeight = input.getHeight() - kernelHeight + 2 * padding;

        int outputWidth = (int) Math.ceil(initialWidth / stride) + 1;
        int outputHeight = (int) Math.ceil(initialHeight / stride) + 1;

        Kernel output = new Kernel(outputWidth, outputHeight);

        for (int i = 0; i < outputHeight; i++) {
            for (int j = 0; j < outputWidth; j++) {
                double value = poolingType.getFunction().apply(this, input, i, j);
                output.setValue(j, i, value);
            }
        }

        cache.setFeatureMap(this, output);
        cache.setInput(this, input);

        return output;
    }
    @Override
    public void propagate(StatesCache cache, Layer<?, ?> nextLayer, Updater updater, Optimizer optimizer) {
        System.out.println("Layer id: " + id);
        Kernel output = cache.getFeatureMap(this);
        Kernel input = cache.getInput(this);

        Kernel deltaPooling = new Kernel(output.getWidth(), output.getHeight());

        if (nextLayer instanceof ConvLayer convLayer) {
            System.out.println("Getting pooling from conv");
            deltaPooling = cache.getDelta(convLayer);
        } else if (nextLayer instanceof FlattenLayer flattenLayer) {
            int outW = output.getWidth();
            int outH = output.getHeight();

            for (int h = 0; h < outH; h++) {
                for (int w = 0; w < outW; w++) {
                    int index = h * outW + w;

                    Neuron neuron = flattenLayer.getNeuronAt(index);
                    double neuronDelta = neuron.getDelta(cache);

                    deltaPooling.setValue(w, h, neuronDelta);
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported layer after pooling layer!");
        }

        Kernel deltaUnpooled = new Kernel(input.getWidth(), input.getHeight());

        System.out.println("OutX: " + output.getWidth());
        System.out.println("OutY: " + output.getHeight());

        System.out.println("DX: " + deltaPooling.getWidth());
        System.out.println("DY: " + deltaPooling.getHeight());
        for (int outY = 0; outY < output.getHeight(); outY++) {
            for (int outX = 0; outX < output.getWidth(); outX++) {
                poolingType.getFunction().unpool(this, outX, outY, deltaPooling, deltaUnpooled, input);
            }
        }

        cache.setDelta(this, deltaUnpooled);
    }


    public PoolingType getPoolingType() {
        return poolingType;
    }

    public int getKernelWidth() {
        return kernelWidth;
    }

    public int getKernelHeight() {
        return kernelHeight;
    }

    public int getStride() {
        return stride;
    }

    public int getPadding() {
        return padding;
    }
}
