package net.echo.brain4j.loss.impl;

import net.echo.brain4j.loss.LossFunction;
import net.echo.brain4j.utils.Vector;

public class BinaryCrossEntropy implements LossFunction {

    @Override
    public double calculate(Vector actual, Vector predicted) {
        double error = 0.0;

        for (int i = 0; i < actual.size(); i++) {
            double p = Math.max(Math.min(predicted.get(i), 1 - 1e-15), 1e-15);
            error += -actual.get(i) * Math.log(p) - (1 - actual.get(i)) * Math.log(1 - p);
        }

        return error / actual.size();
    }
}
