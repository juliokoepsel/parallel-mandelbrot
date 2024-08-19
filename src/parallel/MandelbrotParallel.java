package parallel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MandelbrotParallel {

    public static int calcMandelbrot(double cr, double ci, int maxIterations) {
        int i = 0;
        double zr = 0.0, zi = 0.0;

        while (i < maxIterations && zr * zr + zi * zi < 4.0) {
            double temp = zr * zr - zi * zi + cr;
            zi = 2.0 * zr * zi + ci;
            zr = temp;
            i++;
        }

        return i;
    }

    public static double mapCoord(int x, int imageWidth, double minR, double maxR) {
        double range = maxR - minR;
        return x * (range / imageWidth) + minR;
    }

    public static void main(String[] args) {
        int imageWidth = 512, imageHeight = 512, maxIterations = 10000;
        double minR = -1.5, maxR = 0.7, minI = -1.0, maxI = 1.0;

        long tStart = System.currentTimeMillis();
        int totalPixels = imageHeight * imageWidth;
        int[][] pixels = new int[totalPixels][3];

        // Número de Threads:
        final int NUM_THREADS = 4;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        for (int y = 0; y < imageHeight; y++) {
            final int row = y;
            executor.submit(() -> {
                for (int x = 0; x < imageWidth; x++) {
                    double cr = mapCoord(x, imageWidth, minR, maxR);
                    double ci = mapCoord(row, imageHeight, minI, maxI);

                    int n = calcMandelbrot(cr, ci, maxIterations);

                    int r = (n % 256);
                    int g = ((n * 3) % 256);
                    int b = (n % 256);

                    synchronized (pixels) {
                        pixels[imageWidth * row + x][0] = r;
                        pixels[imageWidth * row + x][1] = g;
                        pixels[imageWidth * row + x][2] = b;
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long tEnd = System.currentTimeMillis();
        double elapsedTimeMs = tEnd - tStart;
        System.out.printf("Time (milliseconds): %.2f\n", elapsedTimeMs);

        try (PrintWriter fout = new PrintWriter(new FileWriter("output_image_par.ppm"))) {
            fout.println("P3");
            fout.println(imageWidth + " " + imageHeight);
            fout.println("256");

            System.out.println("Generating file:");
            for (int i = 0; i < totalPixels; i++) {
                fout.println(pixels[i][0] + " " + pixels[i][1] + " " + pixels[i][2]);
            }
            System.out.println("Finished!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

