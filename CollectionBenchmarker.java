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

        // ArrayList
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-end", (c, s, r) -> ((ArrayList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "get(index)", (c, s, r) -> sink += ((ArrayList<Integer>)c).get(r.nextInt(s)), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-front", (c, s, r) -> ((ArrayList<Integer>)c).add(0, r.nextInt()), true);
        runBenchmark(results, "ArrayList", ArrayList::new, "contains", (c, s, r) -> sink += ((ArrayList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // LinkedList
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-end", (c, s, r) -> ((LinkedList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-front", (c, s, r) -> ((LinkedList<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "contains", (c, s, r) -> sink += ((LinkedList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // ArrayDeque
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-end", (c, s, r) -> ((ArrayDeque<Integer>)c).addLast(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-front", (c, s, r) -> ((ArrayDeque<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "peek", (c, s, r) -> sink += ((ArrayDeque<Integer>)c).peekFirst(), false);

        // Sets
        runBenchmark(results, "HashSet", HashSet::new, "add", (c, s, r) -> ((HashSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "HashSet", HashSet::new, "contains", (c, s, r) -> sink += ((HashSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);
        runBenchmark(results, "TreeSet", TreeSet::new, "add", (c, s, r) -> ((TreeSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "TreeSet", TreeSet::new, "contains", (c, s, r) -> sink += ((TreeSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);

        // Maps
        runBenchmark(results, "HashMap", HashMap::new, "put", (c, s, r) -> ((HashMap<Integer, Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "HashMap", HashMap::new, "get", (c, s, r) -> sink += Objects.hashCode(((HashMap<Integer, Integer>)c).get(r.nextInt())), false);
        runBenchmark(results, "HashMap", HashMap::new, "containsKey", (c, s, r) -> sink += ((HashMap<Integer, Integer>)c).containsKey(r.nextInt()) ? 1 : 0, false);

        runBenchmark(results, "TreeMap", TreeMap::new, "put", (c, s, r) -> ((TreeMap<Integer, Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "get", (c, s, r) -> sink += Objects.hashCode(((TreeMap<Integer, Integer>)c).get(r.nextInt())), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "containsKey", (c, s, r) -> sink += ((TreeMap<Integer, Integer>)c).containsKey(r.nextInt()) ? 1 : 0, false);

        // PriorityQueue
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "offer", (c, s, r) -> ((PriorityQueue<Integer>)c).offer(r.nextInt()), false);
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "peek", (c, s, r) -> sink += ((PriorityQueue<Integer>)c).peek(), false);
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "poll", (c, s, r) -> {
            PriorityQueue<Integer> pq = (PriorityQueue<Integer>)c;
            if (!pq.isEmpty()) sink += pq.poll();
            pq.offer(r.nextInt());
        }, true);

        printConsoleTable(results);
        writeToCSV(results);

        System.out.println("\nTotal execution time: " + ((System.currentTimeMillis() - totalStart) / 1000.0) + " seconds.");
        System.out.println("[System Verification] Global sink value: " + sink);
    }

    private static void runBenchmark(List<ResultRow> results, String collectionName, java.util.function.Supplier<Object> supplier, String operationName, BenchmarkTask task, boolean isHeavyOp) {
        double[] nsPerOpArray = new double[SIZES.length];
        Random rand = new Random(42);

        System.out.printf("Running: %-15s | %-13s -> ", collectionName, operationName);

        for (int i = 0; i < SIZES.length; i++) {
            int n = SIZES[i];
            System.out.print("[" + formatSize(n) + "... ");
            long stepStart = System.currentTimeMillis();

            int operations = 1_000_000;
            if (isHeavyOp && n >= 100_000) {
                if (n == 100_000) operations = 5_000;
                else if (n == 1_000_000) operations = 500;
                else if (n == 10_000_000) operations = 50;
            }
            int warmupOps = Math.max(10, operations / 2);

            Object coll = supplier.get();
            populateCollection(collectionName, coll, n, rand);

            for (int k = 0; k < warmupOps; k++) {
                task.execute(coll, n, rand);
            }

            long startTime = System.nanoTime();
            int completedOps = 0;
            for (int k = 0; k < operations; k++) {
                task.execute(coll, n, rand);
                completedOps++;
                if (k % 10 == 0 && (System.currentTimeMillis() - stepStart) > 3000) {
                    break;
                }
            }
            long endTime = System.nanoTime();

            nsPerOpArray[i] = (double) (endTime - startTime) / completedOps;
            coll = null;
            System.gc();

            long stepDuration = System.currentTimeMillis() - stepStart;
            System.out.print(stepDuration + "ms] ");
        }
        System.out.println("Done ✓");

        double ratio1kTo10m = nsPerOpArray[4] / nsPerOpArray[0];
        String guessedBigO = deduceBigO(ratio1kTo10m, isHeavyOp);

        results.add(new ResultRow(collectionName, operationName, nsPerOpArray, ratio1kTo10m, guessedBigO));
    }

    private static void populateCollection(String name, Object coll, int n, Random rand) {
        if (coll instanceof Collection) {
            Collection<Integer> c = (Collection<Integer>) coll;
            while (c.size() < n) c.add(rand.nextInt());
        } else if (coll instanceof Map) {
            Map<Integer, Integer> m = (Map<Integer, Integer>) coll;
            while (m.size() < n) m.put(rand.nextInt(), rand.nextInt());
        }
    }

    private static String formatSize(int n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    private static String deduceBigO(double totalGrowthRatio, boolean knownLinear) {
        if (knownLinear || totalGrowthRatio > 15.0) return "O(n)";
        if (totalGrowthRatio < 2.5) return "O(1)";
        return "O(log n)";
    }

    private static void printConsoleTable(List<ResultRow> results) {
        System.out.printf("\n%-15s | %-13s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-7s\n",
                "Collection", "Operation", "1K (ns)", "10K (ns)", "100K (ns)", "1M (ns)", "10M (ns)", "Ratio", "Big-O");
        System.out.println("-".repeat(105));
        for (ResultRow r : results) {
            System.out.printf("%-15s | %-13s | %-8.1f | %-8.1f | %-8.1f | %-8.1f | %-8.1f | %-8.2f | %-7s\n",
                    r.collection, r.operation, r.nsPerOp[0], r.nsPerOp[1], r.nsPerOp[2], r.nsPerOp[3], r.nsPerOp[4], r.ratio, r.guessedBigO);
        }
    }

    private static void writeToCSV(List<ResultRow> results) {
        try (FileWriter writer = new FileWriter("timeA.csv")) {
            writer.write("Collection,Operation,Ns_1K,Ns_10K,Ns_100K,Ns_1M,Ns_10M,GrowthRatio,GuessedBigO\n");
            for (ResultRow r : results) {
                writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                        r.collection, r.operation, r.nsPerOp[0], r.nsPerOp[1], r.nsPerOp[2], r.nsPerOp[3], r.nsPerOp[4], r.ratio, r.guessedBigO));
            }
            System.out.println("\nSuccessfully exported benchmark metrics to timeA.csv");
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
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
