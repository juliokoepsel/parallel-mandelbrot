#include <math.h>
#include <chrono>
#include <fstream>
#include <iostream>
#include <omp.h>

using namespace std;

int calcMandelbrot(double cr, double ci, int maxIterations) {
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

double mapCoord(int x, int imageWidth, double minR, double maxR) {
  double range = maxR - minR;

  return x * (range / imageWidth) + minR;
}

int main() {
  int imageWidth = 512, imageHeight = 512, maxIterations = 10000;
  double minR = -1.5, maxR = 0.7, minI = -1.0, maxI = 1.0;

  auto t_start = std::chrono::high_resolution_clock::now();
  int totalPixels = imageHeight * imageWidth; 
  int pixels[totalPixels][3] = {};

  // Número de Threads:
  const int NUM_THREADS = 4;
  omp_set_num_threads(NUM_THREADS);

#pragma omp parallel shared(pixels)
  {
#pragma omp for schedule(dynamic)
    for (int y = 0; y < imageHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        //printf("%d\n", omp_get_thread_num());
        double cr = mapCoord(x, imageWidth, minR, maxR);
        double ci = mapCoord(y, imageHeight, minI, maxI);

        int n = calcMandelbrot(cr, ci, maxIterations);

        int r = ((n) % 256);
        int g = ((n * 3) % 256);
        int b = (n % 256);

        pixels[imageWidth * y + x][0] = r;
        pixels[imageWidth * y + x][1] = g;
        pixels[imageWidth * y + x][2] = b;

      } 
    }
  }

  auto t_end = std::chrono::high_resolution_clock::now();
  double elapsed_time_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();
  printf("Time (milliseconds): %lf\n", elapsed_time_ms);

  ofstream fout("output_image_par.ppm");
  fout << "P3" << endl;
  fout << imageWidth << " " << imageHeight << endl;
  fout << "256" << endl;

  printf("Generating file:\n");
  for (int i = 0; i < totalPixels; i++)
  {
    for (int j = 0; j < 3; j++)
    {
      fout << pixels[i][j] << " ";
    }
  }
  
  fout.close();
  std::cout << "Finished!" << endl;
  return 0;
}