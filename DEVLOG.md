# Development Log — CP2561 Lab #2: Measuring Time and Space Complexity of Java Collections

Each entry documents a discrete unit of work completed during the project sprint.
Fill in the fields; leave the structure intact.

---

## Entry 1

- **Date:** 2026-06-02
- **Author:** DylanTracey32
- **Description:** Initialized the project repository. Created CollectionBenchmarker skeleton with the BenchmarkTask functional interface and ResultRow data class. Set up the SIZES array covering test scales from 1 K to 10 M. Main method prints the benchmark header and global sink verification.
- **Files changed:** DEVLOG.md, CollectionBenchmarker.java, .gitignore
- **Notes:** Will tackle ArrayList and LinkedList timing benchmarks next session once the helper infrastructure is wired up.

---

## Entry 2

- **Date:** 2026-06-03
- **Author:** DylanTracey32
- **Description:** Added Part A timing benchmarks for ArrayList (add-at-end, get-by-index, add-at-front, contains) and LinkedList (add-at-end, add-at-front, contains). Implemented runBenchmark() with a warm-up phase, adaptive operation count for heavy ops, and a 3-second safety timeout. Added populateCollection(), formatSize(), and deduceBigO() helpers.
- **Files changed:** CollectionBenchmarker.java, DEVLOG.md
- **Notes:** ArrayList.get(index) confirms flat O(1); LinkedList.get shows clear O(n) growth due to sequential node traversal. ArrayDeque, Sets, Maps, and PriorityQueue still to be added.

---

## Entry 3

- **Date:** 2026-06-04
- **Author:** rileydruken2
- **Description:** Completed Part A by adding ArrayDeque (addLast, addFirst, peekFirst), HashSet (add, contains), TreeSet (add, contains), HashMap (put, get, containsKey), TreeMap (put, get, containsKey), and PriorityQueue (offer, peek, poll) benchmarks. Wired up printConsoleTable() for console output and writeToCSV() to emit timeA.csv. Used global sink variable throughout to prevent dead-code elimination by the JIT.
- **Files changed:** CollectionBenchmarker.java, DEVLOG.md
- **Notes:** timeA.csv successfully generated. HashMap.get shows O(1) flat behaviour as predicted. TreeMap.get shows O(log n) growth as expected from Red-Black tree traversal.

---

## Entry 4

- **Date:** 2026-06-04
- **Author:** rileydruken2
- **Description:** Added Part B memory footprint measurement. Implemented runMemoryBenchmarks() using Runtime.getRuntime() totalMemory minus freeMemory deltas bracketed around each collection population. Added gcPause() helper to force two GC cycles and a 50ms sleep before each measurement to stabilize the heap baseline. Outputs memoryB.csv.
- **Files changed:** CollectionBenchmarker.java, DEVLOG.md
- **Notes:** Initial results: ArrayList uses roughly 4 bytes/element for int references at small n; HashMap overhead is 3-5x higher per element due to Node object allocation. Will extend to custom collections once Part D is implemented.

---

## Entry 5

- **Date:** 2026-06-05
- **Author:** DylanTracey32
- **Description:** Implemented LobsterStream.java for Part C. Built a LOBSTER-style in-memory limit order book using TreeMap for each side keyed by price (bids in reverse order), ArrayDeque for FIFO queues at each price level, and HashMap for O(1) cancel lookups by order ID. Implemented execute() as a FIFO price-time sweep that walks firstEntry() of the opposite side, drains the queue in order, and removes empty price tiers. Added a daemon resource-monitor thread that prints CPU load, heap usage, and GC stats every second using JMX MXBeans.
- **Files changed:** LobsterStream.java, DEVLOG.md
- **Notes:** Ran with -Xms4g -Xmx8g. Reached approximately 3.21 GB live data with 13,184,567 resting orders at ~1.2 M events/sec. GC pressure increased noticeably past 30 M events.

---

## Entry 6

- **Date:** 2026-06-06
- **Author:** DylanTracey32
- **Description:** Implemented MyArrayList<E> for Part D. Backed by a raw Object[] with DEFAULT_CAPACITY of 16. ensureCapacity() doubles the buffer via Arrays.copyOf() on overflow, giving amortised O(1) add. add(), get(), set(), remove(), size(), isEmpty(), and clear() all implemented. Iterator provided via an anonymous inner class tracking a cursor index.
- **Files changed:** MyArrayList.java, DEVLOG.md
- **Notes:** Manually verified: added 20 elements, iterated the list, removed the mid-point element, confirmed size decremented and remaining elements shifted correctly.

---

## Entry 7

- **Date:** 2026-06-07
- **Author:** rileydruken2
- **Description:** Implemented MyHashMap<K,V> for Part D. Uses separate chaining with a private Node<K,V> inner class. bucketIndex() applies a high-bit spread (h ^= h >>> 16) before masking to capacity-1 to reduce clustering. rehash() triggers when load factor exceeds 0.75 and doubles capacity, re-inserting all nodes into the new bucket array. Full API: put(), get(), remove(), containsKey(), size(), isEmpty(), keys().
- **Files changed:** MyHashMap.java, DEVLOG.md
- **Notes:** Verified put/get/remove/containsKey against JDK HashMap output on 10 K random Integer key-value pairs. All results matched.

---

## Entry 8

- **Date:** 2026-06-07
- **Author:** rileydruken2
- **Description:** Replaced runMemoryBenchmarks() with runCustomVsJdkBenchmarks() which now benchmarks MyArrayList and MyHashMap side-by-side with JDK ArrayList and HashMap across all eight measurement slots (four timing, four memory). Renamed the output method to emit both memoryB.csv and compareD.csv from the same loop to avoid redundant passes. Also updated the CSV header format to include the custom-collection columns.
- **Files changed:** CollectionBenchmarker.java, DEVLOG.md
- **Notes:** At n=10 M, MyHashMap used ~730 MB vs JDK HashMap ~824 MB — our simpler growth strategy is marginally leaner. MyArrayList add was faster than JDK ArrayList add at large n, likely due to simpler ensureCapacity logic.

---

## Entry 9

- **Date:** 2026-06-08
- **Author:** DylanTracey32
- **Description:** Final cleanup and submission preparation. Ran the full benchmark suite to regenerate all CSV output files (timeA.csv, memoryB.csv, compareD.csv) and LobsterStream to regenerate scaleC.csv. Reviewed all four parts against the lab checklist. Committed all source files and CSV data files. Lab is complete and ready for submission.
- **Files changed:** DEVLOG.md, timeA.csv, memoryB.csv, scaleC.csv, compareD.csv
- **Notes:** All deliverables present. GitHub repository URL confirmed in lab report.
