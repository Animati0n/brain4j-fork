package net.echo.brain4j.activation.impl;

import net.echo.brain4j.activation.Activation;
import net.echo.brain4j.structure.Neuron;
import net.echo.brain4j.structure.StatesCache;

import java.util.List;

public class LinearActivation implements Activation {

    @Override
    public double activate(double input) {
        return input;
    }

    @Override
    public double getDerivative(double input) {
        return 1;
    }
}
