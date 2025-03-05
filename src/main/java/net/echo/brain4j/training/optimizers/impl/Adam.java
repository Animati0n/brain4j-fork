package net.echo.brain4j.training.optimizers.impl;

import net.echo.brain4j.layer.Layer;
import net.echo.brain4j.model.impl.Sequential;
import net.echo.brain4j.structure.Synapse;
import net.echo.brain4j.structure.cache.Parameters;
import net.echo.brain4j.structure.cache.StatesCache;
import net.echo.brain4j.training.optimizers.Optimizer;
import net.echo.brain4j.training.updater.Updater;

import java.util.List;

public class Adam extends Optimizer {

    // Momentum vectors
    protected float[] firstMomentum;
    protected float[] secondMomentum;

    protected double beta1Timestep;
    protected double beta2Timestep;

    protected float beta1;
    protected float beta2;
    protected float epsilon;
    protected int timestep = 0;

    public Adam(double learningRate) {
        this(learningRate, 0.9, 0.999, 1e-8);
    }

    public Adam(double learningRate, double beta1, double beta2, double epsilon) {
        super(learningRate);
        this.beta1 = (float) beta1;
        this.beta2 = (float) beta2;
        this.epsilon = (float) epsilon;
    }

    public void postInitialize(Sequential model) {
        this.firstMomentum = new float[Parameters.TOTAL_SYNAPSES];
        this.secondMomentum = new float[Parameters.TOTAL_SYNAPSES];
    }

    @Override
    public double update(StatesCache cacheHolder, Synapse synapse) {
        double delta = synapse.getOutputNeuron().getDelta(cacheHolder);
        double value = synapse.getInputNeuron().getValue(cacheHolder);

        float gradient = (float) (delta * value);

        int synapseId = synapse.getSynapseId();

        float currentFirstMomentum = firstMomentum[synapseId];
        float currentSecondMomentum = secondMomentum[synapseId];

        float m = beta1 * currentFirstMomentum + (1 - beta1) * gradient;
        float v = beta2 * currentSecondMomentum + (1 - beta2) * gradient * gradient;

        firstMomentum[synapseId] = m;
        secondMomentum[synapseId] = v;

        double mHat = m / (1 - beta1Timestep);
        double vHat = v / (1 - beta2Timestep);

        return (learningRate * mHat) / (Math.sqrt(vHat) + epsilon);
    }

    @Override
    public void postIteration(StatesCache cacheHolder, Updater updater, List<Layer<?, ?>> layers) {
        this.timestep++;

        this.beta1Timestep = Math.pow(beta1, timestep);
        this.beta2Timestep = Math.pow(beta2, timestep);

        for (Layer<?, ?> layer : layers) {
            for (Synapse synapse : layer.getSynapses()) {
                float change = (float) update(cacheHolder, synapse);
                updater.acknowledgeChange(synapse, change);
            }
        }
    }

    public double getBeta1() {
        return beta1;
    }

    public void setBeta1(double beta1) {
        this.beta1 = (float) beta1;
    }

    public double getBeta2() {
        return beta2;
    }

    public void setBeta2(double beta2) {
        this.beta2 = (float) beta2;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = (float) epsilon;
    }
}