package net.echo.brain4j.utils.math.autograd.operations;

import net.echo.brain4j.utils.math.autograd.Operation;
import net.echo.brain4j.utils.math.tensor.Tensor;

public class MulOperation implements Operation {
    @Override
    public Tensor forward(Tensor... inputs) {
        return inputs[0].times(inputs[1]);
    }
    
    @Override
    public Tensor[] backward(Tensor gradOutput, Tensor... inputs) {
        return new Tensor[] { 
            gradOutput.times(inputs[1]),
            gradOutput.times(inputs[0])
        };
    }
} 