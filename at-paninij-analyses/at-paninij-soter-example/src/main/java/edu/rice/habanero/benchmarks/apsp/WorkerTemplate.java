package edu.rice.habanero.benchmarks.apsp;

import java.util.HashMap;
import java.util.Map;

import org.paninij.lang.Capsule;
import org.paninij.lang.Imports;

@Capsule public class WorkerTemplate {
    int blockSize = ApspConfig.B;
    int graphSize = ApspConfig.N;
    int numBlocksInSingleDim = graphSize / blockSize;
    int numNeighbors = 2 * (numBlocksInSingleDim - 1);

    @Imports Master master;
    @Imports Worker[] neighbors = new Worker[numNeighbors];
    @Imports int myBlockId;

    int rowOffset;
    int colOffset;

    int k = -1;
    Map<Integer, long[][]> neighborDataPerIteration = new HashMap<Integer, long[][]>();
    long[][] currentIterData;


    public void initialize(long[][] initGraphData) {
        this.rowOffset = (myBlockId / numBlocksInSingleDim) * blockSize;
        this.colOffset = (myBlockId % numBlocksInSingleDim) * blockSize;
        this.currentIterData = ApspUtils.getBlock(initGraphData, myBlockId);
    }

    public void start() {
        notifyNeighbors();
    }

    public void iteration(int otherK, int otherId, long[][] initData) {
        boolean haveAllData = storeIterationData(otherK, otherId, initData);
        if (haveAllData) {
            k++;

            performComputation();
            notifyNeighbors();
            neighborDataPerIteration.clear();

            if (k == (graphSize - 1)) {
                for (Worker n : neighbors) n.exit();
                master.workerFinished();
                master.exit();
            }
        }
    }

    private boolean storeIterationData(int iteration, int sourceId, long[][] dataArray) {
        neighborDataPerIteration.put(sourceId, dataArray);
        return neighborDataPerIteration.size() == numNeighbors;
    }

    private void performComputation() {
        long[][] prevIterData = currentIterData;
        currentIterData = new long[blockSize][blockSize];

        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                int gi = rowOffset + i;
                int gj = colOffset + j;
                long newIterData = elementAt(gi, k, k - 1, prevIterData) + elementAt(k, gj, k - 1, prevIterData);
                currentIterData[i][j] = Math.min(prevIterData[i][j], newIterData);
            }
        }

    }

    private long elementAt(int row, int col, int srcIter, long[][] prevIterData) {
        int destBlockId = ((row / blockSize) * numBlocksInSingleDim) + (col / blockSize);
        int localRow = row % blockSize;
        int localCol = col % blockSize;

        if (destBlockId == myBlockId) {
            return prevIterData[localRow][localCol];
        }

        return neighborDataPerIteration.get(destBlockId)[localRow][localCol];
    }

    private void notifyNeighbors() {
        // send the current result to all other blocks who might need it
        // note: this is inefficient version where data is sent to neighbors
        // who might not need it for the current value of k
        for (Worker n : neighbors) n.iteration(k, myBlockId, currentIterData);
    }

}
