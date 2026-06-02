import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CollectionBenchmarker {

    private static long sink = 0;
    private static final int[] SIZES = {1_000, 10_000, 100_000, 1_000_000, 10_000_000};

    interface BenchmarkTask {
        void execute(Object collection, int size, Random rand);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("====================================================");
        System.out.println(" Starting Live Progress Collection Benchmark        ");
        System.out.println("====================================================\n");
        long totalStart = System.currentTimeMillis();

        List<ResultRow> results = new ArrayList<>();

        // Part A benchmarks — to be added

        System.out.println("\nTotal execution time: " + ((System.currentTimeMillis() - totalStart) / 1000.0) + " seconds.");
        System.out.println("[System Verification] Global sink value: " + sink);
    }

    static class ResultRow {
        String collection;
        String operation;
        double[] nsPerOp;
        double ratio;
        String guessedBigO;

        ResultRow(String collection, String operation, double[] nsPerOp, double ratio, String guessedBigO) {
            this.collection = collection;
            this.operation = operation;
            this.nsPerOp = nsPerOp;
            this.ratio = ratio;
            this.guessedBigO = guessedBigO;
        }
    }
}
