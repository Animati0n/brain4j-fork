package net.echo.brain4j.convolution.impl;

import net.echo.brain4j.activation.Activations;
import net.echo.brain4j.layer.Layer;

public class ConvolutionalLayer extends Layer {

    protected final int kernelWidth;
    protected final int kernelHeight;
    protected final int filters;
    protected int padding;
    protected int stride;

    public ConvolutionalLayer(int kernelWidth, int kernelHeight, int filters, Activations activation) {
        super(0, activation);
        this.kernelWidth = kernelWidth;
        this.kernelHeight = kernelHeight;
        this.filters = filters;
    }

    public ConvolutionalLayer(int kernelWidth, int kernelHeight, int filters, int stride, Activations activation) {
        this(kernelWidth, kernelHeight, filters, activation);
        this.stride = stride;
    }

    public ConvolutionalLayer(int kernelWidth, int kernelHeight, int filters, int stride, int padding, Activations activation) {
        this(kernelWidth, kernelHeight, filters, activation);
        this.stride = stride;
        this.padding = padding;
    }

    public int getKernelWidth() {
        return kernelWidth;
    }

    public int getKernelHeight() {
        return kernelHeight;
    }

    public int getFilters() {
        return filters;
    }

    public int getPadding() {
        return padding;
    }

    public int getStride() {
        return stride;
    }
}
