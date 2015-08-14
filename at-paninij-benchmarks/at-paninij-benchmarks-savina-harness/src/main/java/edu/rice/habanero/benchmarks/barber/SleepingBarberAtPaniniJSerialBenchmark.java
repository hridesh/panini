package edu.rice.habanero.benchmarks.barber;

import java.io.IOException;

import org.paninij.runtime.Panini$System;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;

public class SleepingBarberAtPaniniJSerialBenchmark
{
    static class SleepingBarberAtPaniniJSerial extends Benchmark {

        @Override
        public void cleanupIteration(boolean arg0, double arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public void initialize(String[] args) throws IOException {
            SleepingBarberConfig.parseArgs(args);
        }

        @Override
        public void printArgInfo() {
            SleepingBarberConfig.printArgs();
        }

        @Override
        public void runIteration() {
            SleepingBarber$Serial.main(null);
            try {
                Panini$System.threads.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        BenchmarkRunner.runBenchmark(args, new SleepingBarberAtPaniniJSerial());
    }
}
