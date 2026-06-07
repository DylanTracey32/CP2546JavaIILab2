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
        }, true); // Tagging poll as true to apply safety limits on large size operations

        printConsoleTable(results);
        writeToCSV(results);

        // ---- Custom vs. JDK head-to-head (memoryB.csv + compareD.csv) -----
        runCustomVsJdkBenchmarks();

        System.out.println("\nTotal execution tracking completed in: " + ((System.currentTimeMillis() - totalStart) / 1000.0) + " seconds.");
        System.out.println("[System Verification] Global sink value: " + sink);
    }

    // -----------------------------------------------------------------------
    // Custom-structure comparative benchmarks
    // -----------------------------------------------------------------------

    private static void runCustomVsJdkBenchmarks() throws IOException {
        final int[] SCALES = SIZES;
        Runtime rt = Runtime.getRuntime();

        // CSV headers
        String timeHeader = "n,ArrayList_add_ns,MyArrayList_add_ns,HashMap_put_ns,MyHashMap_put_ns,"
                          + "ArrayList_get_ns,MyArrayList_get_ns,HashMap_get_ns,MyHashMap_get_ns";
        String memHeader  = timeHeader.replace("_ns", "_bytes");

        StringBuilder timeSB = new StringBuilder(timeHeader).append('\n');
        StringBuilder memSB  = new StringBuilder(memHeader).append('\n');
        StringBuilder cmpSB  = new StringBuilder(timeHeader).append('\n');  // compareD mirrors time

        for (int n : SCALES) {
            long[] t = new long[8];
            long[] m = new long[8];

            // Slot 0: ArrayList add
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                ArrayList<Integer> c = new ArrayList<>();
                for (int i = 0; i < n; i++) { c.add(i); sink += i; }
                t[0] = System.nanoTime() - t0; m[0] = Math.max(0, used(rt) - mb);
                c = null; gcPause(rt);
            }
            // Slot 1: MyArrayList add
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                MyArrayList<Integer> c = new MyArrayList<>();
                for (int i = 0; i < n; i++) { c.add(i); sink += i; }
                t[1] = System.nanoTime() - t0; m[1] = Math.max(0, used(rt) - mb);
                c = null; gcPause(rt);
            }
            // Slot 2: HashMap put
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                HashMap<Integer,Integer> c = new HashMap<>();
                for (int i = 0; i < n; i++) { c.put(i, i); sink += i; }
                t[2] = System.nanoTime() - t0; m[2] = Math.max(0, used(rt) - mb);
                c = null; gcPause(rt);
            }
            // Slot 3: MyHashMap put
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                MyHashMap<Integer,Integer> c = new MyHashMap<>();
                for (int i = 0; i < n; i++) { c.put(i, i); sink += i; }
                t[3] = System.nanoTime() - t0; m[3] = Math.max(0, used(rt) - mb);
                c = null; gcPause(rt);
            }

            // Pre-build for get benchmarks (not timed)
            ArrayList<Integer>         alPre = new ArrayList<>();
            MyArrayList<Integer>       malPre = new MyArrayList<>();
            HashMap<Integer,Integer>   hmPre = new HashMap<>();
            MyHashMap<Integer,Integer> mhmPre = new MyHashMap<>();
            for (int i = 0; i < n; i++) { alPre.add(i); malPre.add(i); hmPre.put(i,i); mhmPre.put(i,i); }

            // Slot 4: ArrayList get
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) sink += alPre.get(i);
                t[4] = System.nanoTime() - t0; m[4] = Math.max(0, used(rt) - mb);
            }
            // Slot 5: MyArrayList get
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) sink += malPre.get(i);
                t[5] = System.nanoTime() - t0; m[5] = Math.max(0, used(rt) - mb);
            }
            // Slot 6: HashMap get
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) { Integer v = hmPre.get(i); if (v!=null) sink+=v; }
                t[6] = System.nanoTime() - t0; m[6] = Math.max(0, used(rt) - mb);
            }
            // Slot 7: MyHashMap get
            {
                gcPause(rt);
                long mb = used(rt); long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) { Integer v = mhmPre.get(i); if (v!=null) sink+=v; }
                t[7] = System.nanoTime() - t0; m[7] = Math.max(0, used(rt) - mb);
            }

            alPre = null; malPre = null; hmPre = null; mhmPre = null; gcPause(rt);

            timeSB.append(csvRow(n, t));
            memSB.append(csvRow(n, m));
            cmpSB.append(csvRow(n, t));

            System.out.printf("Custom/JDK n=%-9d  AL=%dns  MAL=%dns  HM=%dns  MHM=%dns%n",
                n, t[0]/n, t[1]/n, t[2]/n, t[3]/n);
        }

        writeCsvFile("memoryB.csv",  memSB.toString());
        writeCsvFile("compareD.csv", cmpSB.toString());
        System.out.println("Wrote memoryB.csv and compareD.csv");
    }

    private static String csvRow(int n, long[] v) {
        return n+","+v[0]+","+v[1]+","+v[2]+","+v[3]+","+v[4]+","+v[5]+","+v[6]+","+v[7]+"\n";
    }

    private static long used(Runtime rt) {
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void gcPause(Runtime rt) {
        System.gc(); System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    private static void writeCsvFile(String filename, String content) throws IOException {
        try (FileWriter fw = new FileWriter(filename)) { fw.write(content); }
    }

    private static void runBenchmark(List<ResultRow> results, String collectionName, java.util.function.Supplier<Object> supplier, String operationName, BenchmarkTask task, boolean isHeavyOp) {
        double[] nsPerOpArray = new double[SIZES.length];
        Random rand = new Random(42);

        System.out.printf("Running: %-15s | %-13s -> ", collectionName, operationName);

        for (int i = 0; i < SIZES.length; i++) {
            int n = SIZES[i];
            System.out.print("[" + formatSize(n) + "... ");
            long stepStart = System.currentTimeMillis();

            // Adaptively scale operations down for heavy loops
            int operations = 1_000_000;
            if (isHeavyOp && n >= 100_000) {
                if (n == 100_000) operations = 5_000;
                else if (n == 1_000_000) operations = 500;
                else if (n == 10_000_000) operations = 50;
            }
            int warmupOps = Math.max(10, operations / 2);

            Object coll = supplier.get();
            populateCollection(collectionName, coll, n, rand);

            // 1. Warm-up
            for (int k = 0; k < warmupOps; k++) {
                task.execute(coll, n, rand);
            }

            // 2. Timed Phase with live safety tracking
            long startTime = System.nanoTime();
            int completedOps = 0;
            for (int k = 0; k < operations; k++) {
                task.execute(coll, n, rand);
                completedOps++;

                // Safety Timeout: If a single size pass takes longer than 3 seconds, break out
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
