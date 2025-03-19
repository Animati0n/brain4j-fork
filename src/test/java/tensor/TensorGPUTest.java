package tensor;

import static net.echo.brain4j.utils.math.tensor.TensorFactory.*;

import net.echo.brain4j.utils.math.tensor.Tensor;
import net.echo.brain4j.utils.math.tensor.TensorGPU;
import net.echo.brain4j.utils.opencl.DeviceUtils;
import net.echo.brain4j.utils.opencl.GPUProfiler;


public class TensorGPUTest {

    public static void main(String[] args) {
        System.out.println("GPU acceleration available: " + TensorGPU.isGpuAvailable());
        System.out.println("Using GPU: " + isUsingGPU());
        
        if (TensorGPU.isGpuAvailable()) {
            GPUProfiler.printDeviceInfo(DeviceUtils.getDevice());
        }
        
        benchmarkMatrixMultiplication();
        benchmarkLargeMatrixMultiplication(500, 500, 500);
    }
    
    private static void benchmarkMatrixMultiplication() {
        int m = 100;
        int n = 100;
        int p = 100;
        
        System.out.println("\n=== Matrix multiplication (" + m + "x" + n + ") * (" + n + "x" + p + ") ===");
        
        forceCPU();
        Tensor cpuA = random(m, n);
        Tensor cpuB = random(n, p);
        
        useGPUIfAvailable();
        Tensor gpuA = random(m, n);
        Tensor gpuB = random(n, p);
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                gpuA.set(cpuA.get(i, j), i, j);
            }
        }
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                gpuB.set(cpuB.get(i, j), i, j);
            }
        }
        
        // CPU benchmark
        long cpuStart = System.currentTimeMillis();
        Tensor cpuResult = cpuA.matmul(cpuB);
        long cpuEnd = System.currentTimeMillis();
        
        // GPU benchmark
        long gpuStart = System.currentTimeMillis();
        Tensor gpuResult = gpuA.matmul(gpuB);
        long gpuEnd = System.currentTimeMillis();
        
        boolean resultsMatch = checkResultsMatch(cpuResult, gpuResult);
        
        System.out.println("CPU: " + (cpuEnd - cpuStart) + " ms");
        System.out.println("GPU: " + (gpuEnd - gpuStart) + " ms");
        System.out.println("Speedup: " + (double)(cpuEnd - cpuStart) / (gpuEnd - gpuStart) + "x");
        System.out.println("Results match: " + resultsMatch);
    }
    
    private static void benchmarkLargeMatrixMultiplication(int m, int n, int p) {
        System.out.println("\n=== Large matrix multiplication (" + m + "x" + n + ") * (" + n + "x" + p + ") ===");
        
        forceCPU();
        System.out.println("Creating matrices...");
        Tensor cpuA = ones(m, n);
        Tensor cpuB = ones(n, p);  
        
        useGPUIfAvailable();
        Tensor gpuA = ones(m, n);
        Tensor gpuB = ones(n, p);
        
        System.out.println("Benchmark CPU in progress...");
        long cpuStart = System.currentTimeMillis();
        Tensor cpuResult = cpuA.matmul(cpuB);
        long cpuEnd = System.currentTimeMillis();
        
        System.out.println("Benchmark GPU in progress...");
        long gpuStart = System.currentTimeMillis();
        Tensor gpuResult = gpuA.matmul(gpuB);
        long gpuEnd = System.currentTimeMillis();
        
        boolean resultsMatch = Math.abs(cpuResult.get(0, 0) - gpuResult.get(0, 0)) < 0.001;
        
        System.out.println("CPU: " + (cpuEnd - cpuStart) + " ms");
        System.out.println("GPU: " + (gpuEnd - gpuStart) + " ms");
        System.out.println("Speedup: " + (double)(cpuEnd - cpuStart) / (gpuEnd - gpuStart) + "x");
        System.out.println("Results check: " + resultsMatch);
    }
    
    private static boolean checkResultsMatch(Tensor a, Tensor b) {
        if (a.numel() != b.numel()) return false;
        if (!java.util.Arrays.equals(a.shape(), b.shape())) return false;
        
        double epsilon = 1e-5;  
        
        int[] shape = a.shape();
        
        if (shape.length == 1) {
            for (int i = 0; i < shape[0]; i++) {
                if (Math.abs(a.get(i) - b.get(i)) > epsilon) {
                    System.out.println("Mismatch at index " + i + ": " + a.get(i) + " vs " + b.get(i));
                    return false;
                }
            }
        } else if (shape.length == 2) {
            for (int i = 0; i < shape[0]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    if (Math.abs(a.get(i, j) - b.get(i, j)) > epsilon) {
                        System.out.println("Mismatch at [" + i + "," + j + "]: " + 
                                           a.get(i, j) + " vs " + b.get(i, j));
                        return false;
                    }
                }
            }
        } else {
            for (int i = 0; i < a.numel(); i++) {
                float valueA = getLinearElement(a, i);
                float valueB = getLinearElement(b, i);
                
                if (Math.abs(valueA - valueB) > epsilon) {
                    System.out.println("Mismatch at linear index " + i + ": " + valueA + " vs " + valueB);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private static float getLinearElement(Tensor tensor, int linearIndex) {
        int[] shape = tensor.shape();
        
        if (shape.length == 1) {
            return tensor.get(linearIndex);
        }
        else if (shape.length == 2) {
            int cols = shape[1];
            int row = linearIndex / cols;
            int col = linearIndex % cols;
            return tensor.get(row, col);
        } 
        else {
            int[] indices = new int[shape.length];
            int remaining = linearIndex;
            for (int i = shape.length - 1; i >= 0; i--) {
                indices[i] = remaining % shape[i];
                remaining /= shape[i];
            }
            return tensor.get(indices);
        }
    }
}
