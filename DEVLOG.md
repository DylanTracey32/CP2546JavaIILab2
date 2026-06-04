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

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**

---

## Entry 5

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**

---

## Entry 6

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**

---

## Entry 7

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**

---

## Entry 8

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**

---

## Entry 9

- **Date:**
- **Author:**
- **Description:**
- **Files changed:**
- **Notes:**
