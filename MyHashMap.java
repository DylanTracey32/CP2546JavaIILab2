'import java.util.ArrayList;
import java.util.List;

public class MyHashMap<K, V> {

    private static final int    DEFAULT_CAPACITY   = 16;
    private static final float  LOAD_FACTOR_LIMIT  = 0.75f;

    private Node<K, V>[] buckets;
    private int          size;
    private int          capacity;

    @SuppressWarnings("unchecked")
    public MyHashMap() {
        capacity = DEFAULT_CAPACITY;
        buckets  = new Node[capacity];
        size     = 0;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void put(K key, V value) {
        int idx = bucketIndex(key);
        Node<K, V> head = buckets[idx];

        for (Node<K, V> cur = head; cur != null; cur = cur.next) {
            if (keysEqual(cur.key, key)) {
                cur.value = value;
                return;
            }
        }

        // Prepend new node
        buckets[idx] = new Node<>(key, value, head);
        size++;

        if ((float) size / capacity > LOAD_FACTOR_LIMIT) {
            rehash();
        }
    }

    public V get(K key) {
        int idx = bucketIndex(key);
        for (Node<K, V> cur = buckets[idx]; cur != null; cur = cur.next) {
            if (keysEqual(cur.key, key)) return cur.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        int idx = bucketIndex(key);
        Node<K, V> cur  = buckets[idx];
        Node<K, V> prev = null;

        while (cur != null) {
            if (keysEqual(cur.key, key)) {
                if (prev == null) buckets[idx] = cur.next;
                else              prev.next    = cur.next;
                size--;
                return cur.value;
            }
            prev = cur;
            cur  = cur.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public List<K> keys() {
        List<K> result = new ArrayList<>(size);
        for (Node<K, V> bucket : buckets) {
            for (Node<K, V> cur = bucket; cur != null; cur = cur.next) {
                result.add(cur.key);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private int bucketIndex(K key) {
        if (key == null) return 0;
        int h = key.hashCode();
        // Spread high bits to reduce clustering
        h ^= (h >>> 16);
        return (h & (capacity - 1));
    }

    private boolean keysEqual(K a, K b) {
        return a == null ? b == null : a.equals(b);
    }

    @SuppressWarnings("unchecked")
    private void rehash() {
        int        newCapacity = capacity * 2;
        Node<K,V>[] newBuckets = new Node[newCapacity];

        for (Node<K, V> bucket : buckets) {
            for (Node<K, V> cur = bucket; cur != null; ) {
                Node<K, V> next = cur.next;

                int h = (cur.key == null) ? 0 : cur.key.hashCode();
                h ^= (h >>> 16);
                int newIdx = h & (newCapacity - 1);

                cur.next         = newBuckets[newIdx];
                newBuckets[newIdx] = cur;

                cur = next;
            }
        }

        buckets  = newBuckets;
        capacity = newCapacity;
    }

    // -----------------------------------------------------------------------
    // Node
    // -----------------------------------------------------------------------

    private static class Node<K, V> {
        K          key;
        V          value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key   = key;
            this.value = value;
            this.next  = next;
        }
    }
}
