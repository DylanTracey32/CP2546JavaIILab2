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
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-end",   (c, s, r) -> ((ArrayList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "get(index)",   (c, s, r) -> sink += ((ArrayList<Integer>)c).get(r.nextInt(s)), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-front", (c, s, r) -> ((ArrayList<Integer>)c).add(0, r.nextInt()), true);
        runBenchmark(results, "ArrayList", ArrayList::new, "contains",     (c, s, r) -> sink += ((ArrayList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // LinkedList — get(index) is O(n) so marked heavy to throttle ops at large n
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-end",   (c, s, r) -> ((LinkedList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "get(index)",   (c, s, r) -> sink += ((LinkedList<Integer>)c).get(r.nextInt(s)), true);
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-front", (c, s, r) -> ((LinkedList<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "contains",     (c, s, r) -> sink += ((LinkedList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // ArrayDeque
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-end",   (c, s, r) -> ((ArrayDeque<Integer>)c).addLast(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-front", (c, s, r) -> ((ArrayDeque<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "peek",         (c, s, r) -> sink += ((ArrayDeque<Integer>)c).peekFirst(), false);

        // Sets
        runBenchmark(results, "HashSet", HashSet::new, "add",      (c, s, r) -> ((HashSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "HashSet", HashSet::new, "contains", (c, s, r) -> sink += ((HashSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);
        runBenchmark(results, "TreeSet", TreeSet::new, "add",      (c, s, r) -> ((TreeSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "TreeSet", TreeSet::new, "contains", (c, s, r) -> sink += ((TreeSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);

        // Maps
        runBenchmark(results, "HashMap", HashMap::new, "put",         (c, s, r) -> ((HashMap<Integer,Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "HashMap", HashMap::new, "get",         (c, s, r) -> sink += Objects.hashCode(((HashMap<Integer,Integer>)c).get(r.nextInt())), false);
        runBenchmark(results, "HashMap", HashMap::new, "containsKey", (c, s, r) -> sink += ((HashMap<Integer,Integer>)c).containsKey(r.nextInt()) ? 1 : 0, false);

        runBenchmark(results, "TreeMap", TreeMap::new, "put",         (c, s, r) -> ((TreeMap<Integer,Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "get",         (c, s, r) -> sink += Objects.hashCode(((TreeMap<Integer,Integer>)c).get(r.nextInt())), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "containsKey", (c, s, r) -> sink += ((TreeMap<Integer,Integer>)c).containsKey(r.nextInt()) ? 1 : 0, false);

        // PriorityQueue
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "offer", (c, s, r) -> ((PriorityQueue<Integer>)c).offer(r.nextInt()), false);
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "peek",  (c, s, r) -> sink += ((PriorityQueue<Integer>)c).peek(), false);
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "poll",  (c, s, r) -> {
            PriorityQueue<Integer> pq = (PriorityQueue<Integer>)c;
            if (!pq.isEmpty()) sink += pq.poll();
            pq.offer(r.nextInt());
        }, true);

        printConsoleTable(results);
        writeToCSV(results);

        // ---- Part B: memory footprint of all JDK collections → memoryB.csv ----
        runAllJdkMemoryBenchmarks();

        // ---- Part D: custom vs JDK head-to-head → compareD.csv ----
        runCustomVsJdkBenchmarks();

        System.out.println("\nTotal execution tracking completed in: " + ((System.currentTimeMillis() - totalStart) / 1000.0) + " seconds.");
        System.out.println("[System Verification] Global sink value: " + sink);
    }

    // -----------------------------------------------------------------------
    // Part B — memory footprint of all 8 JDK collections
    // Measures at a fixed N=1_000_000. One row per structure.
    // -----------------------------------------------------------------------

    private static void runAllJdkMemoryBenchmarks() throws IOException {
        final int N = 1_000_000;
        Runtime rt = Runtime.getRuntime();
        StringBuilder sb = new StringBuilder("Structure,Elements,HeapUsed_MB,BytesPerElement\n");

        System.out.println("\n--- Part B: JDK Memory Footprint (n=" + N + ") ---");

        // ArrayList
        gcPause(rt); long b = used(rt);
        ArrayList<Integer> al = new ArrayList<>();
        for (int i = 0; i < N; i++) { al.add(i); sink += i; }
        appendMemRow(sb, "ArrayList", N, Math.max(0, used(rt) - b));
        al = null; gcPause(rt);

        // LinkedList
        gcPause(rt); b = used(rt);
        LinkedList<Integer> ll = new LinkedList<>();
        for (int i = 0; i < N; i++) { ll.add(i); sink += i; }
        appendMemRow(sb, "LinkedList", N, Math.max(0, used(rt) - b));
        ll = null; gcPause(rt);

        // ArrayDeque
        gcPause(rt); b = used(rt);
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        for (int i = 0; i < N; i++) { ad.addLast(i); sink += i; }
        appendMemRow(sb, "ArrayDeque", N, Math.max(0, used(rt) - b));
        ad = null; gcPause(rt);

        // HashSet
        gcPause(rt); b = used(rt);
        HashSet<Integer> hs = new HashSet<>();
        for (int i = 0; i < N; i++) { hs.add(i); sink += i; }
        appendMemRow(sb, "HashSet", N, Math.max(0, used(rt) - b));
        hs = null; gcPause(rt);

        // TreeSet
        gcPause(rt); b = used(rt);
        TreeSet<Integer> ts = new TreeSet<>();
        for (int i = 0; i < N; i++) { ts.add(i); sink += i; }
        appendMemRow(sb, "TreeSet", N, Math.max(0, used(rt) - b));
        ts = null; gcPause(rt);

        // HashMap
        gcPause(rt); b = used(rt);
        HashMap<Integer,Integer> hm = new HashMap<>();
        for (int i = 0; i < N; i++) { hm.put(i, i); sink += i; }
        appendMemRow(sb, "HashMap", N, Math.max(0, used(rt) - b));
        hm = null; gcPause(rt);

        // TreeMap
        gcPause(rt); b = used(rt);
        TreeMap<Integer,Integer> tm = new TreeMap<>();
        for (int i = 0; i < N; i++) { tm.put(i, i); sink += i; }
        appendMemRow(sb, "TreeMap", N, Math.max(0, used(rt) - b));
        tm = null; gcPause(rt);

        // PriorityQueue
        gcPause(rt); b = used(rt);
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int i = 0; i < N; i++) { pq.offer(i); sink += i; }
        appendMemRow(sb, "PriorityQueue", N, Math.max(0, used(rt) - b));
        pq = null; gcPause(rt);

        writeCsvFile("memoryB.csv", sb.toString());
        System.out.println("Wrote memoryB.csv");
    }

    private static void appendMemRow(StringBuilder sb, String name, int n, long bytes) {
        double mb  = bytes / 1_048_576.0;
        double bpe = (double) bytes / n;
        sb.append(String.format("%s,%d,%.2f,%.2f%n", name, n, mb, bpe));
        System.out.printf("  %-15s  heap=%.2f MB  bytes/elem=%.2f%n", name, mb, bpe);
    }

    // -----------------------------------------------------------------------
    // Part D — custom vs JDK head-to-head (time + memory) → compareD.csv
    // -----------------------------------------------------------------------

    private static void runCustomVsJdkBenchmarks() throws IOException {
        final int[] SCALES = SIZES;
        Runtime rt = Runtime.getRuntime();

        String cmpHeader =
            "n," +
            "ArrayList_add_ns,MyArrayList_add_ns,HashMap_put_ns,MyHashMap_put_ns," +
            "ArrayList_get_ns,MyArrayList_get_ns,HashMap_get_ns,MyHashMap_get_ns," +
            "ArrayList_add_bytes,MyArrayList_add_bytes,HashMap_put_bytes,MyHashMap_put_bytes," +
            "ArrayList_get_bytes,MyArrayList_get_bytes,HashMap_get_bytes,MyHashMap_get_bytes";

        StringBuilder cmpSB = new StringBuilder(cmpHeader).append('\n');

        System.out.println("\n--- Part D: Custom vs JDK Benchmark ---");

        for (int n : SCALES) {
            long[] t = new long[8];
            long[] m = new long[8];

            // Slot 0: ArrayList add
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              ArrayList<Integer> c = new ArrayList<>();
              for (int i = 0; i < n; i++) { c.add(i); sink += i; }
              t[0] = System.nanoTime() - t0; m[0] = Math.max(0, used(rt) - mb);
              c = null; } gcPause(rt);

            // Slot 1: MyArrayList add
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              MyArrayList<Integer> c = new MyArrayList<>();
              for (int i = 0; i < n; i++) { c.add(i); sink += i; }
              t[1] = System.nanoTime() - t0; m[1] = Math.max(0, used(rt) - mb);
              c = null; } gcPause(rt);

            // Slot 2: HashMap put
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              HashMap<Integer,Integer> c = new HashMap<>();
              for (int i = 0; i < n; i++) { c.put(i, i); sink += i; }
              t[2] = System.nanoTime() - t0; m[2] = Math.max(0, used(rt) - mb);
              c = null; } gcPause(rt);

            // Slot 3: MyHashMap put
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              MyHashMap<Integer,Integer> c = new MyHashMap<>();
              for (int i = 0; i < n; i++) { c.put(i, i); sink += i; }
              t[3] = System.nanoTime() - t0; m[3] = Math.max(0, used(rt) - mb);
              c = null; } gcPause(rt);

            // Pre-build structures for get benchmarks (untimed)
            ArrayList<Integer>         alPre  = new ArrayList<>();
            MyArrayList<Integer>       malPre = new MyArrayList<>();
            HashMap<Integer,Integer>   hmPre  = new HashMap<>();
            MyHashMap<Integer,Integer> mhmPre = new MyHashMap<>();
            for (int i = 0; i < n; i++) { alPre.add(i); malPre.add(i); hmPre.put(i,i); mhmPre.put(i,i); }

            // Slot 4: ArrayList get
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              for (int i = 0; i < n; i++) sink += alPre.get(i);
              t[4] = System.nanoTime() - t0; m[4] = Math.max(0, used(rt) - mb); }

            // Slot 5: MyArrayList get
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              for (int i = 0; i < n; i++) sink += malPre.get(i);
              t[5] = System.nanoTime() - t0; m[5] = Math.max(0, used(rt) - mb); }

            // Slot 6: HashMap get
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              for (int i = 0; i < n; i++) { Integer v = hmPre.get(i); if (v != null) sink += v; }
              t[6] = System.nanoTime() - t0; m[6] = Math.max(0, used(rt) - mb); }

            // Slot 7: MyHashMap get
            gcPause(rt);
            { long mb = used(rt); long t0 = System.nanoTime();
              for (int i = 0; i < n; i++) { Integer v = mhmPre.get(i); if (v != null) sink += v; }
              t[7] = System.nanoTime() - t0; m[7] = Math.max(0, used(rt) - mb); }

            alPre = null; malPre = null; hmPre = null; mhmPre = null; gcPause(rt);

            cmpSB.append(csvRow16(n, t, m));

            System.out.printf("n=%-9d  AL_add=%dns  MAL_add=%dns  HM_put=%dns  MHM_put=%dns%n",
                n, t[0]/n, t[1]/n, t[2]/n, t[3]/n);
        }

        writeCsvFile("compareD.csv", cmpSB.toString());
        System.out.println("Wrote compareD.csv");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String csvRow16(int n, long[] t, long[] m) {
        return n + "," +
               t[0]+","+t[1]+","+t[2]+","+t[3]+","+t[4]+","+t[5]+","+t[6]+","+t[7] + "," +
               m[0]+","+m[1]+","+m[2]+","+m[3]+","+m[4]+","+m[5]+","+m[6]+","+m[7] + "\n";
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

    private static void runBenchmark(List<ResultRow> results, String collectionName,
            java.util.function.Supplier<Object> supplier, String operationName,
            BenchmarkTask task, boolean isHeavyOp) {

        double[] nsPerOpArray = new double[SIZES.length];
        Random rand = new Random(42);

        System.out.printf("Running: %-15s | %-13s -> ", collectionName, operationName);

        for (int i = 0; i < SIZES.length; i++) {
            int n = SIZES[i];
            System.out.print("[" + formatSize(n) + "... ");
            long stepStart = System.currentTimeMillis();

            int operations = 1_000_000;
            if (isHeavyOp && n >= 100_000) {
                if      (n == 100_000)    operations = 5_000;
                else if (n == 1_000_000)  operations = 500;
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

            nsPerOpArray[i] = (double)(endTime - startTime) / completedOps;
            coll = null;
            System.gc();

            System.out.print((System.currentTimeMillis() - stepStart) + "ms] ");
        }
        System.out.println("Done ✓");

        double ratio1kTo10m = nsPerOpArray[4] / nsPerOpArray[0];
        String guessedBigO  = deduceBigO(ratio1kTo10m, isHeavyOp);
        results.add(new ResultRow(collectionName, operationName, nsPerOpArray, ratio1kTo10m, guessedBigO));
    }

    private static void populateCollection(String name, Object coll, int n, Random rand) {
        if (coll instanceof Collection) {
            Collection<Integer> c = (Collection<Integer>) coll;
            while (c.size() < n) c.add(rand.nextInt());
        } else if (coll instanceof Map) {
            Map<Integer,Integer> m = (Map<Integer,Integer>) coll;
            while (m.size() < n) m.put(rand.nextInt(), rand.nextInt());
        }
    }

    private static String formatSize(int n) {
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000)     return (n / 1_000) + "K";
        return String.valueOf(n);
    }

    private static String deduceBigO(double ratio, boolean knownLinear) {
        if (knownLinear || ratio > 15.0) return "O(n)";
        if (ratio < 2.5)                 return "O(1)";
        return "O(log n)";
    }

    private static void printConsoleTable(List<ResultRow> results) {
        System.out.printf("\n%-15s | %-13s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-7s\n",
                "Collection", "Operation", "1K (ns)", "10K (ns)", "100K (ns)", "1M (ns)", "10M (ns)", "Ratio", "Big-O");
        System.out.println("-".repeat(105));
        for (ResultRow r : results) {
            System.out.printf("%-15s | %-13s | %-8.1f | %-8.1f | %-8.1f | %-8.1f | %-8.1f | %-8.2f | %-7s\n",
                    r.collection, r.operation,
                    r.nsPerOp[0], r.nsPerOp[1], r.nsPerOp[2], r.nsPerOp[3], r.nsPerOp[4],
                    r.ratio, r.guessedBigO);
        }
    }

    private static void writeToCSV(List<ResultRow> results) {
        try (FileWriter writer = new FileWriter("timeA.csv")) {
            writer.write("Collection,Operation,Ns_1K,Ns_10K,Ns_100K,Ns_1M,Ns_10M,GrowthRatio,GuessedBigO\n");
            for (ResultRow r : results) {
                writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                        r.collection, r.operation,
                        r.nsPerOp[0], r.nsPerOp[1], r.nsPerOp[2], r.nsPerOp[3], r.nsPerOp[4],
                        r.ratio, r.guessedBigO));
            }
            System.out.println("\nSuccessfully exported benchmark metrics to timeA.csv");
        } catch (IOException e) {
            System.err.println("Error writing timeA.csv: " + e.getMessage());
        }
    }

    static class ResultRow {
        String   collection;
        String   operation;
        double[] nsPerOp;
        double   ratio;
        String   guessedBigO;

        ResultRow(String collection, String operation, double[] nsPerOp, double ratio, String guessedBigO) {
            this.collection  = collection;
            this.operation   = operation;
            this.nsPerOp     = nsPerOp;
            this.ratio       = ratio;
            this.guessedBigO = guessedBigO;
        }
    }
}
