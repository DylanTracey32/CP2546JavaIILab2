import java.io.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * LobsterStream — LOBSTER-like limit-order-book simulator.
 *
 * Generates events entirely in memory (no file I/O for the event stream).
 * The live state that accumulates is the order book only; every event object
 * is discarded immediately after application.
 *
 * Collections used:
 *   TreeMap    — each side of the book, keyed by price              O(log n)
 *   ArrayDeque — FIFO queue of resting orders at one price level     O(1) ends
 *   HashMap    — id -> order for O(1) cancel lookups                 O(1) avg
 *
 * Run:
 *   javac LobsterStream.java
 *   java -Xms4g -Xmx8g LobsterStream 8
 */
public class LobsterStream {

    // -----------------------------------------------------------------------
    // Order model
    // -----------------------------------------------------------------------

    static final class Order {
        final long id;
        final long price;
        int        size;
        final int  side;   // 1 = buy, -1 = sell

        Order(long id, long price, int size, int side) {
            this.id    = id;
            this.price = price;
            this.size  = size;
            this.side  = side;
        }
    }

    // -----------------------------------------------------------------------
    // Book state
    // -----------------------------------------------------------------------

    final TreeMap<Long, ArrayDeque<Order>> bids   = new TreeMap<>(Collections.reverseOrder());
    final TreeMap<Long, ArrayDeque<Order>> asks   = new TreeMap<>();
    final HashMap<Long, Order>             byId   = new HashMap<>();
    final ArrayList<Long>                  liveIds = new ArrayList<>();

    long nextId = 1;
    long mid    = 100_00;   // mid price in cents ($100.00)

    // -----------------------------------------------------------------------
    // Timing accumulators (updated inside submit / cancel / execute)
    // -----------------------------------------------------------------------

    long submitNs    = 0;  long submitCount  = 0;
    long cancelNs    = 0;  long cancelCount  = 0;
    long lookupNs    = 0;  long lookupCount  = 0;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    TreeMap<Long, ArrayDeque<Order>> side(int s) { return s == 1 ? bids : asks; }

    static long usedBytes() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    // -----------------------------------------------------------------------
    // submit — timed
    // -----------------------------------------------------------------------

    void submit(int side, long price, int size) {
        long t0 = System.nanoTime();

        Order o = new Order(nextId++, price, size, side);
        side(side).computeIfAbsent(price, k -> new ArrayDeque<>()).addLast(o);
        byId.put(o.id, o);
        liveIds.add(o.id);

        submitNs += System.nanoTime() - t0;
        submitCount++;
    }

    // -----------------------------------------------------------------------
    // cancel — timed
    // -----------------------------------------------------------------------

    void cancel(long id) {
        long t0 = System.nanoTime();

        Order o = byId.remove(id);
        if (o != null) {
            ArrayDeque<Order> q = side(o.side).get(o.price);
            if (q != null) {
                q.remove(o);
                if (q.isEmpty()) side(o.side).remove(o.price);
            }
        }

        cancelNs += System.nanoTime() - t0;
        cancelCount++;
    }

    // -----------------------------------------------------------------------
    // execute — FIFO price-time sweep
    //
    // Walks the opposite side from its best price (firstEntry), consuming
    // the head of each FIFO queue in price order.  Fully-filled orders are
    // removed from both the queue and the byId index.  Emptied price tiers
    // are removed from the TreeMap to prevent stale-key accumulation.
    // -----------------------------------------------------------------------

    void execute(int aggressorSide, int size) {
        TreeMap<Long, ArrayDeque<Order>> opposite = side(-aggressorSide);

        while (size > 0 && !opposite.isEmpty()) {
            Map.Entry<Long, ArrayDeque<Order>> bestEntry = opposite.firstEntry();
            ArrayDeque<Order> queue = bestEntry.getValue();

            while (size > 0 && !queue.isEmpty()) {
                Order resting = queue.peekFirst();
                int   fill    = Math.min(size, resting.size);
                size        -= fill;
                resting.size -= fill;

                if (resting.size == 0) {
                    queue.pollFirst();
                    byId.remove(resting.id);
                    // Note: liveIds still holds the id but cancel() guards via byId.remove
                }
            }

            if (queue.isEmpty()) {
                opposite.remove(bestEntry.getKey());
            }
        }
    }

    // -----------------------------------------------------------------------
    // step — generate one event, apply it, discard the event object
    // -----------------------------------------------------------------------

    void step(ThreadLocalRandom rng) {
        mid += rng.nextInt(-3, 4);
        double r = rng.nextDouble();

        if (r < 0.62 || liveIds.isEmpty()) {
            int    s     = rng.nextBoolean() ? 1 : -1;
            int    depth = 0;
            while (rng.nextDouble() > 0.40 && depth < 40) depth++;
            long   price = s == 1 ? mid - 100 - 100L * depth
                                  : mid + 100 + 100L * depth;
            int    sz    = 100 * (1 + (int)(rng.nextDouble() * 4));
            submit(s, price, sz);

        } else if (r < 0.95) {
            int  idx = rng.nextInt(liveIds.size());
            long id  = liveIds.get(idx);
            liveIds.set(idx, liveIds.get(liveIds.size() - 1));
            liveIds.remove(liveIds.size() - 1);
            cancel(id);

        } else {
            execute(rng.nextBoolean() ? 1 : -1, 100 * (1 + rng.nextInt(5)));
        }
    }

    // -----------------------------------------------------------------------
    // Resource monitor daemon (runs every 1 second)
    // -----------------------------------------------------------------------

    static Thread startResourceMonitor() {
        Thread t = new Thread(() -> {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            ThreadMXBean threads = ManagementFactory.getThreadMXBean();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    break;
                }

                double cpuLoad   = os.getProcessCpuLoad() * 100.0;
                long   heapUsed  = mem.getHeapMemoryUsage().getUsed()  / (1024 * 1024);
                long   totalRam  = os.getTotalMemorySize()             / (1024 * 1024);
                long   freeRam   = os.getFreeMemorySize()              / (1024 * 1024);
                int    threadCnt = threads.getThreadCount();

                long gcCount = 0, gcTimeMs = 0;
                for (GarbageCollectorMXBean gc : gcBeans) {
                    long c = gc.getCollectionCount();
                    long t2 = gc.getCollectionTime();
                    if (c  >= 0) gcCount  += c;
                    if (t2 >= 0) gcTimeMs += t2;
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

    // -----------------------------------------------------------------------
    // main
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        double gb     = args.length > 0 ? Double.parseDouble(args[0]) : 8.0;
        long   target = (long)(gb * 1024L * 1024L * 1024L);

        startResourceMonitor();

        LobsterStream   s   = new LobsterStream();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // scaleC.csv — header
        PrintWriter csv = new PrintWriter(new BufferedWriter(new FileWriter("scaleC.csv")));
        csv.println("events,restingOrders,heapMB,bytesPerOrder," +
                    "submitAvgNs,cancelAvgNs,lookupAvgNs,liveHeapMB");

        long events = 0;
        long t0     = System.nanoTime();

        while (usedBytes() < target) {
            s.step(rng);
            events++;

            // Report + write CSV roughly every 16 M events
            if ((events & 0xFF_FFFF) == 0) {
                double secs    = (System.nanoTime() - t0) / 1e9;
                long   heapMB  = usedBytes() / (1024 * 1024);
                int    resting = s.byId.size();

                // --- timed bids.firstEntry() lookup ---
                long lt0 = System.nanoTime();
                Map.Entry<Long, ArrayDeque<Order>> best = s.bids.firstEntry();
                s.lookupNs    += System.nanoTime() - lt0;
                s.lookupCount++;
                long bestPrice = (best != null) ? best.getKey() : -1;

                long bytesPerOrder = resting > 0 ? usedBytes() / resting : 0;

                long submitAvg = s.submitCount > 0 ? s.submitNs / s.submitCount : 0;
                long cancelAvg = s.cancelCount > 0 ? s.cancelNs / s.cancelCount : 0;
                long lookupAvg = s.lookupCount > 0 ? s.lookupNs / s.lookupCount : 0;

                System.out.printf(
                    "events=%,dM  rate=%,.1fM/s  heap=%,d MB  resting=%,d  " +
                    "submit=%dns  cancel=%dns  lookup=%dns  bytesPerOrder=%d  bestBid=%d%n",
                    events / 1_000_000, (events / 1e6) / secs,
                    heapMB, resting,
                    submitAvg, cancelAvg, lookupAvg,
                    bytesPerOrder, bestPrice);

                csv.printf("%d,%d,%d,%d,%d,%d,%d,%d%n",
                    events, resting, heapMB, bytesPerOrder,
                    submitAvg, cancelAvg, lookupAvg, heapMB);
                csv.flush();
            }
        }

        double secs = (System.nanoTime() - t0) / 1e9;
        System.out.printf(
            "%nREACHED ~%.0f GB: processed %,d events in %.1fs (%,.1fM events/s), %,d resting orders%n",
            gb, events, secs, (events / 1e6) / secs, s.byId.size());

        csv.close();
        System.out.println("Wrote scaleC.csv");
    }
}
