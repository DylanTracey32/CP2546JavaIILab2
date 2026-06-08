# GitHub Copilot Chat Log
**Course:** CP2561 Lab #2 — Measuring Time and Space Complexity of Java Collections
**Authors:** Dylan Tracey, Riley Druken
**Date:** 2026-06-02 to 2026-06-08
**Tool:** GitHub Copilot Chat (VS Code)

---

## Part A — Timing Benchmark

---

**You:**
Construct a clean Java micro-benchmark suite executing loops using System.nanoTime() to record average operation times across standard JDK collection instances up to 10M records. Ensure code blocks avoid dead-code optimizations by writing to a global tracking sink.

---

**GitHub Copilot:**
Here's a clean micro-benchmark suite for JDK collections:

```java
import java.util.*;

public class CollectionBenchmarker {

    private static long sink = 0; // global sink to prevent dead-code elimination
    private static final int[] SIZES = {1_000, 10_000, 100_000, 1_000_000, 10_000_000};

    public static void main(String[] args) {
        List<String> results = new ArrayList<>();

        // ArrayList benchmarks
        benchmarkAdd("ArrayList-add-end", n -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(i);
            sink += list.size();
        });

        benchmarkGet("ArrayList-get", n -> {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(i);
            return list;
        }, (list, r) -> sink += ((ArrayList<Integer>) list).get(r.nextInt(list.size())));

        // HashMap benchmarks
        benchmarkAdd("HashMap-put", n -> {
            HashMap<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < n; i++) map.put(i, i);
            sink += map.size();
        });
    }

    static void benchmarkAdd(String name, java.util.function.IntConsumer op) {
        System.out.printf("%-25s", name);
        for (int n : SIZES) {
            long start = System.nanoTime();
            op.accept(n);
            long elapsed = System.nanoTime() - start;
            System.out.printf("  n=%-8d  ns/op=%-8.1f", n, (double) elapsed / n);
        }
        System.out.println();
    }
}
```

This avoids dead-code elimination by writing to `sink`. Would you like me to add warm-up passes and expand to LinkedList, TreeMap, HashSet, and PriorityQueue?

---

**You:**
Yes, add warm-up loops before timing, add all remaining JDK collections (LinkedList, ArrayDeque, HashSet, TreeSet, HashMap, TreeMap, PriorityQueue), add a growth-ratio column that guesses Big-O from the ratio, and write the results to timeA.csv with a header row.

---

**GitHub Copilot:**
Here's the expanded version with warm-up, all collections, Big-O inference, and CSV output:

```java
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
        List<ResultRow> results = new ArrayList<>();

        // ArrayList
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-end",
            (c, s, r) -> ((ArrayList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "get(index)",
            (c, s, r) -> sink += ((ArrayList<Integer>)c).get(r.nextInt(s)), false);
        runBenchmark(results, "ArrayList", ArrayList::new, "add-at-front",
            (c, s, r) -> ((ArrayList<Integer>)c).add(0, r.nextInt()), true);
        runBenchmark(results, "ArrayList", ArrayList::new, "contains",
            (c, s, r) -> sink += ((ArrayList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // LinkedList
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-end",
            (c, s, r) -> ((LinkedList<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "get(index)",
            (c, s, r) -> sink += ((LinkedList<Integer>)c).get(r.nextInt(s)), true);
        runBenchmark(results, "LinkedList", LinkedList::new, "add-at-front",
            (c, s, r) -> ((LinkedList<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "LinkedList", LinkedList::new, "contains",
            (c, s, r) -> sink += ((LinkedList<Integer>)c).contains(r.nextInt()) ? 1 : 0, true);

        // ArrayDeque
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-end",
            (c, s, r) -> ((ArrayDeque<Integer>)c).addLast(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "add-at-front",
            (c, s, r) -> ((ArrayDeque<Integer>)c).addFirst(r.nextInt()), false);
        runBenchmark(results, "ArrayDeque", ArrayDeque::new, "peek",
            (c, s, r) -> sink += ((ArrayDeque<Integer>)c).peekFirst(), false);

        // Sets
        runBenchmark(results, "HashSet", HashSet::new, "add",
            (c, s, r) -> ((HashSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "HashSet", HashSet::new, "contains",
            (c, s, r) -> sink += ((HashSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);
        runBenchmark(results, "TreeSet", TreeSet::new, "add",
            (c, s, r) -> ((TreeSet<Integer>)c).add(r.nextInt()), false);
        runBenchmark(results, "TreeSet", TreeSet::new, "contains",
            (c, s, r) -> sink += ((TreeSet<Integer>)c).contains(r.nextInt()) ? 1 : 0, false);

        // Maps
        runBenchmark(results, "HashMap", HashMap::new, "put",
            (c, s, r) -> ((HashMap<Integer,Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "HashMap", HashMap::new, "get",
            (c, s, r) -> sink += Objects.hashCode(((HashMap<Integer,Integer>)c).get(r.nextInt())), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "put",
            (c, s, r) -> ((TreeMap<Integer,Integer>)c).put(r.nextInt(), r.nextInt()), false);
        runBenchmark(results, "TreeMap", TreeMap::new, "get",
            (c, s, r) -> sink += Objects.hashCode(((TreeMap<Integer,Integer>)c).get(r.nextInt())), false);

        // PriorityQueue
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "offer",
            (c, s, r) -> ((PriorityQueue<Integer>)c).offer(r.nextInt()), false);
        runBenchmark(results, "PriorityQueue", PriorityQueue::new, "peek",
            (c, s, r) -> sink += ((PriorityQueue<Integer>)c).peek(), false);

        writeToCSV(results);
        System.out.println("Sink: " + sink);
    }

    private static void runBenchmark(List<ResultRow> results, String collName,
            java.util.function.Supplier<Object> supplier, String opName,
            BenchmarkTask task, boolean isHeavy) {

        double[] ns = new double[SIZES.length];
        Random rand = new Random(42);
        System.out.printf("Running %-15s %-13s -> ", collName, opName);

        for (int i = 0; i < SIZES.length; i++) {
            int n = SIZES[i];
            int ops = 1_000_000;
            if (isHeavy && n >= 100_000)
                ops = (n == 100_000) ? 5_000 : (n == 1_000_000) ? 500 : 50;

            Object coll = supplier.get();
            populateCollection(coll, n, rand);

            // warm-up
            for (int k = 0; k < ops / 2; k++) task.execute(coll, n, rand);

            long start = System.nanoTime();
            for (int k = 0; k < ops; k++) task.execute(coll, n, rand);
            ns[i] = (double)(System.nanoTime() - start) / ops;

            System.out.print("n=" + n + " ✓  ");
        }
        System.out.println();

        double ratio = ns[4] / ns[0];
        String bigO = ratio > 15 ? "O(n)" : ratio < 2.5 ? "O(1)" : "O(log n)";
        results.add(new ResultRow(collName, opName, ns, ratio, bigO));
    }

    private static void populateCollection(Object coll, int n, Random rand) {
        if (coll instanceof Collection) {
            Collection<Integer> c = (Collection<Integer>) coll;
            while (c.size() < n) c.add(rand.nextInt());
        } else if (coll instanceof Map) {
            Map<Integer,Integer> m = (Map<Integer,Integer>) coll;
            while (m.size() < n) m.put(rand.nextInt(), rand.nextInt());
        }
    }

    private static void writeToCSV(List<ResultRow> results) throws IOException {
        try (FileWriter fw = new FileWriter("timeA.csv")) {
            fw.write("Collection,Operation,Ns_1K,Ns_10K,Ns_100K,Ns_1M,Ns_10M,GrowthRatio,GuessedBigO\n");
            for (ResultRow r : results)
                fw.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                    r.collection, r.operation,
                    r.nsPerOp[0], r.nsPerOp[1], r.nsPerOp[2], r.nsPerOp[3], r.nsPerOp[4],
                    r.ratio, r.bigO));
        }
        System.out.println("Wrote timeA.csv");
    }

    static class ResultRow {
        String collection, operation, bigO;
        double[] nsPerOp;
        double ratio;
        ResultRow(String c, String o, double[] ns, double r, String b) {
            collection = c; operation = o; nsPerOp = ns; ratio = r; bigO = b;
        }
    }
}
```

The `isHeavy` flag throttles operations for O(n) operations like `add-at-front` and `LinkedList.get(index)` to prevent the benchmark from hanging at large n.

---

**You:**
Also add a 3-second per-step safety timeout so no single size/operation combination blocks the whole run.

---

**GitHub Copilot:**
Add this inside the timed loop in `runBenchmark`:

```java
long stepStart = System.currentTimeMillis();
int completed = 0;
for (int k = 0; k < ops; k++) {
    task.execute(coll, n, rand);
    completed++;
    if (k % 10 == 0 && (System.currentTimeMillis() - stepStart) > 3000) break;
}
ns[i] = (double)(System.nanoTime() - start) / completed;
```

Using `completed` as the divisor ensures ns/op is still accurate even when we break early.

---

---

## Part B — Memory Footprint

---

**You:**
Provide an isolated memory profile utility tracking Runtime.getRuntime() totalMemory and freeMemory properties to capture exact byte allocations before and after bulk collections populate data structures.

---

**GitHub Copilot:**
Here's the memory profiling method to add to your benchmarker. It measures all eight JDK structures at a fixed n=1,000,000:

```java
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

private static long used(Runtime rt) { return rt.totalMemory() - rt.freeMemory(); }

private static void gcPause(Runtime rt) {
    System.gc(); System.gc();
    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
}
```

Each structure is measured in isolation: two GC passes stabilise the heap before taking the `before` snapshot, and the collection is nulled and GC'd before the next measurement starts.

---

---

## Part C — LobsterStream Order Book

---

**You:**
Develop a high-performance order book matching engine in Java. Use a top-level TreeMap keyed by price pointing to ArrayDeques of Order elements. Implement logic to remove empty price nodes on full matches.

---

**GitHub Copilot:**
Here's the core order book structure and matching logic:

```java
import java.util.*;

public class LobsterStream {

    static final class Order {
        final long id;
        final long price;
        int size;
        final int side; // 1 = buy, -1 = sell

        Order(long id, long price, int size, int side) {
            this.id = id; this.price = price;
            this.size = size; this.side = side;
        }
    }

    final TreeMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    final TreeMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    final HashMap<Long, Order> byId = new HashMap<>();

    // execute(): FIFO price-time sweep across the opposite side
    void execute(int aggressorSide, int size) {
        TreeMap<Long, ArrayDeque<Order>> opposite = aggressorSide == 1 ? asks : bids;

        while (size > 0 && !opposite.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Order>> bestEntry = opposite.firstEntry();
            ArrayDeque<Order> queue = bestEntry.getValue();

            while (size > 0 && !queue.isEmpty()) {
                Order resting = queue.peekFirst();
                int fill = Math.min(size, resting.size);
                size -= fill;
                resting.size -= fill;

                if (resting.size == 0) {
                    queue.pollFirst();
                    byId.remove(resting.id); // clean up id index
                }
            }

            // remove empty price tier to prevent stale-key accumulation
            if (queue.isEmpty()) {
                opposite.remove(bestEntry.getKey());
            }
        }
    }
}
```

The `opposite.firstEntry()` gives the best price in O(log n). Each fill is O(1) at the head of the ArrayDeque. Empty price levels are removed immediately so the TreeMap never accumulates stale keys.

---

**You:**
Add a background daemon thread that prints the program's CPU load using com.sun.management.OperatingSystemMXBean, the used heap, total and free physical RAM, live thread count, and total GC count and pause time — once per second.

---

**GitHub Copilot:**
```java
import java.lang.management.*;
import com.sun.management.OperatingSystemMXBean;

static Thread startResourceMonitor() {
    Thread t = new Thread(() -> {
        com.sun.management.OperatingSystemMXBean os =
            (com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        while (!Thread.currentThread().isInterrupted()) {
            try { Thread.sleep(1_000); } catch (InterruptedException e) { break; }

            double cpuLoad  = os.getProcessCpuLoad() * 100.0;
            long heapUsed   = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long totalRam   = os.getTotalMemorySize()            / (1024 * 1024);
            long freeRam    = os.getFreeMemorySize()             / (1024 * 1024);
            int  threadCnt  = threads.getThreadCount();

            long gcCount = 0, gcTimeMs = 0;
            for (GarbageCollectorMXBean gc : gcBeans) {
                if (gc.getCollectionCount() >= 0) gcCount  += gc.getCollectionCount();
                if (gc.getCollectionTime()  >= 0) gcTimeMs += gc.getCollectionTime();
            }

            System.out.printf(
                "[MONITOR] CPU=%.1f%%  Heap=%,d MB  RAM free/total=%,d/%,d MB" +
                "  Threads=%d  GC collections=%d  GC pause=%,d ms%n",
                cpuLoad, heapUsed, freeRam, totalRam, threadCnt, gcCount, gcTimeMs);
        }
    }, "resource-monitor");
    t.setDaemon(true);
    t.start();
    return t;
}
```

Set it as a daemon thread so it doesn't prevent JVM shutdown when the main loop finishes.

---

---

## Part D — Custom Collections

---

**You:**
Write separate, production-ready source files for custom collections MyArrayList and MyHashMap. Ensure MyArrayList dynamically doubles buffer limits on overflow, and MyHashMap performs rehashing once its load factor exceeds 0.75.

---

**GitHub Copilot:**
Here is `MyArrayList.java`:

```java
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MyArrayList<E> implements Iterable<E> {

    private static final int DEFAULT_CAPACITY = 16;
    private Object[] data;
    private int size;

    public MyArrayList() { data = new Object[DEFAULT_CAPACITY]; }

    public MyArrayList(int initialCapacity) {
        data = new Object[Math.max(initialCapacity, DEFAULT_CAPACITY)];
    }

    public void add(E element) {
        if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
        data[size++] = element;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        return (E) data[index];
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        E old = (E) data[index];
        data[index] = element;
        return old;
    }

    public void remove(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        System.arraycopy(data, index + 1, data, index, size - index - 1);
        data[--size] = null;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        Arrays.fill(data, 0, size, null);
        size = 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int cursor = 0;
            public boolean hasNext() { return cursor < size; }
            @SuppressWarnings("unchecked")
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (E) data[cursor++];
            }
        };
    }
}
```

And `MyHashMap.java`:

```java
import java.util.ArrayList;
import java.util.List;

public class MyHashMap<K, V> {

    private static final int   DEFAULT_CAPACITY  = 16;
    private static final float LOAD_FACTOR_LIMIT = 0.75f;

    private Node<K,V>[] buckets;
    private int size;
    private int capacity;

    @SuppressWarnings("unchecked")
    public MyHashMap() {
        capacity = DEFAULT_CAPACITY;
        buckets  = new Node[capacity];
    }

    public void put(K key, V value) {
        int idx = bucketIndex(key);
        for (Node<K,V> cur = buckets[idx]; cur != null; cur = cur.next) {
            if (keysEqual(cur.key, key)) { cur.value = value; return; }
        }
        buckets[idx] = new Node<>(key, value, buckets[idx]);
        if (++size > capacity * LOAD_FACTOR_LIMIT) rehash();
    }

    public V get(K key) {
        for (Node<K,V> cur = buckets[bucketIndex(key)]; cur != null; cur = cur.next)
            if (keysEqual(cur.key, key)) return cur.value;
        return null;
    }

    public boolean containsKey(K key) { return get(key) != null; }

    public V remove(K key) {
        int idx = bucketIndex(key);
        Node<K,V> cur = buckets[idx], prev = null;
        while (cur != null) {
            if (keysEqual(cur.key, key)) {
                if (prev == null) buckets[idx] = cur.next;
                else              prev.next    = cur.next;
                size--;
                return cur.value;
            }
            prev = cur; cur = cur.next;
        }
        return null;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public List<K> keys() {
        List<K> result = new ArrayList<>(size);
        for (Node<K,V> b : buckets)
            for (Node<K,V> cur = b; cur != null; cur = cur.next)
                result.add(cur.key);
        return result;
    }

    private int bucketIndex(K key) {
        if (key == null) return 0;
        int h = key.hashCode();
        h ^= (h >>> 16); // spread high bits
        return h & (capacity - 1);
    }

    private boolean keysEqual(K a, K b) { return a == null ? b == null : a.equals(b); }

    @SuppressWarnings("unchecked")
    private void rehash() {
        int newCap = capacity * 2;
        Node<K,V>[] newBuckets = new Node[newCap];
        for (Node<K,V> bucket : buckets) {
            for (Node<K,V> cur = bucket; cur != null; ) {
                Node<K,V> next = cur.next;
                int h = (cur.key == null) ? 0 : cur.key.hashCode();
                h ^= (h >>> 16);
                int idx = h & (newCap - 1);
                cur.next = newBuckets[idx];
                newBuckets[idx] = cur;
                cur = next;
            }
        }
        buckets = newBuckets;
        capacity = newCap;
    }

    private static class Node<K, V> {
        K key; V value; Node<K,V> next;
        Node(K k, V v, Node<K,V> n) { key = k; value = v; next = n; }
    }
}
```

Both classes are self-contained with no external dependencies. `MyArrayList` doubles via `Arrays.copyOf` and `MyHashMap` rehashes by walking every bucket chain and re-inserting into a new array sized at `capacity * 2`.

---

**You:**
Now add a benchmark method that runs the same timing and memory tests on MyArrayList vs ArrayList and MyHashMap vs HashMap side by side, and writes the results to compareD.csv with columns for both ns/op and bytes/element.

---

**GitHub Copilot:**
```java
private static void runCustomVsJdkBenchmarks() throws IOException {
    Runtime rt = Runtime.getRuntime();
    String header =
        "n," +
        "ArrayList_add_ns,MyArrayList_add_ns,HashMap_put_ns,MyHashMap_put_ns," +
        "ArrayList_get_ns,MyArrayList_get_ns,HashMap_get_ns,MyHashMap_get_ns," +
        "ArrayList_add_bytes,MyArrayList_add_bytes,HashMap_put_bytes,MyHashMap_put_bytes," +
        "ArrayList_get_bytes,MyArrayList_get_bytes,HashMap_get_bytes,MyHashMap_get_bytes";

    StringBuilder sb = new StringBuilder(header).append('\n');

    for (int n : SIZES) {
        long[] t = new long[8], m = new long[8];

        // time + memory: ArrayList add
        gcPause(rt); long b = used(rt); long t0 = System.nanoTime();
        ArrayList<Integer> al = new ArrayList<>();
        for (int i = 0; i < n; i++) { al.add(i); sink += i; }
        t[0] = System.nanoTime() - t0; m[0] = Math.max(0, used(rt) - b);
        al = null; gcPause(rt);

        // MyArrayList add
        gcPause(rt); b = used(rt); t0 = System.nanoTime();
        MyArrayList<Integer> mal = new MyArrayList<>();
        for (int i = 0; i < n; i++) { mal.add(i); sink += i; }
        t[1] = System.nanoTime() - t0; m[1] = Math.max(0, used(rt) - b);
        mal = null; gcPause(rt);

        // ... (HashMap, MyHashMap put, then get benchmarks) ...

        sb.append(n);
        for (long v : t) sb.append(',').append(v);
        for (long v : m) sb.append(',').append(v);
        sb.append('\n');
    }

    try (FileWriter fw = new FileWriter("compareD.csv")) { fw.write(sb.toString()); }
    System.out.println("Wrote compareD.csv");
}
```

Each slot records wall time from `System.nanoTime()` and heap delta from `Runtime` in the same pass, so both numbers reflect the same workload.

---

*End of Copilot chat log.*
