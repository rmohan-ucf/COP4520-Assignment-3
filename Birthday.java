
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Birthday {
  public static final int GIFT_COUNT = 500_000;
  public static final int SERVANT_COUNT = 8;

  public static AtomicInteger counter = new AtomicInteger();// used to pick random presents
  public static List<Present> bag = new ArrayList<>();
  public static PresentChain chain = new PresentChain();

  public static Set<Integer> thanked = new HashSet<>();

  // Servant thread
  public static class Servant extends Thread {
    public void run() {
      Random rand = new Random();
      boolean flip = true;

      // Continue with not all gift givers have been thanked
      while (thanked.size() < GIFT_COUNT) {
        // Random check from minotaur
        if (rand.nextInt(50) == 0) {
          int s = chain.size.get();
          if (s > 0) {
            chain.has(rand.nextInt(s));
          }
        }
        // positive flip, add item from bag into chain
        else if (flip) {
          if (bag.size() > 0) {
            Present x = bag.remove(0);
            chain.add(x);
          }
          flip = !flip; // flip flop
        }
        // negative flip, remove item from chain and thank
        else {
          if (chain.size.get() > 0) {
            Present x = chain.pop(chain.head.next);
            thanked.add(x.id);
          }
          flip = !flip; // flip flop
        }
      }
    }
  }

  public static void main(String[] args) {
    long startTime = System.nanoTime();

    // Initialize bag
    for (int i = 1; i <= GIFT_COUNT; i++) {
      bag.add(new Present(i));
    }

    // Shuffle for randomization
    Collections.shuffle(bag);

    // Run servant threads
    Servant[] servants = new Servant[SERVANT_COUNT];
    for (int i = 0; i < SERVANT_COUNT; i++) {
      servants[i] = new Servant();
      servants[i].start();
      try {
        servants[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long endtime = System.nanoTime();

    // Output
    System.out.println("Servant threads finished");
    System.out.println("Number of thank you notes: " + thanked.size());
    System.out.println("Presents left on chain: " + chain.size.get());
    System.out.println("Total execution time " + (endtime - startTime) / 1_000_000_000.0 + " seconds");
  }
}

// Class representing a present, doubles as a linked-list node
class Present implements Comparable<Present> {
  public int id;
  public Present next;
  public boolean marked;

  Lock lock;

  public Present(int id) {
    this.id = id;
    this.next = null;
    this.marked = false;
    this.lock = new ReentrantLock();
  }

  void lock() {
    lock.lock();
  }

  void unlock() {
    lock.unlock();
  }

  @Override
  public int compareTo(Present other) {
    return this.id - other.id;
  }
}

// Class representing a present linked list
class PresentChain {
  Present head;
  AtomicInteger size;

  public PresentChain() {
    head = new Present(Integer.MIN_VALUE);
    head.next = new Present(Integer.MAX_VALUE);

    size = new AtomicInteger(0);
  }

  // Add item to chain
  public void add(Present x) {
    while (true) {
      // Find add position
      Present prev = head;
      Present cur = head.next;

      while (cur.id < x.id) {
        prev = cur;
        cur = cur.next;
      }

      prev.lock();
      try {
        cur.lock();
        try {
          // Ensure nothing has changed between the time the current
          // node was found and the nodes were locked
          if (!prev.marked && !cur.marked && prev.next == cur) {
            // Prevent present from being added to chain twice
            if (cur.id != x.id) {
              prev.next = x;
              x.next = cur;

              size.getAndIncrement();

              return; // Exit loop
            }
          }
        } finally {
          cur.unlock();
        }
      } finally {
        prev.unlock();
      }
    }
  }

  // Remove item from chain
  public Present pop(Present x) {
    while (true) {
      // Find pop position
      Present prev = this.head;
      Present cur = head.next;
      while (cur.id < x.id) {
        prev = cur;
        cur = cur.next;
      }

      prev.lock();
      try {
        cur.lock();
        try {
          // Ensure nothing has changed between the time the current
          // node was found and the nodes were locked
          if (!prev.marked && !cur.marked && prev.next == cur) {
            // Node doesn't exist
            if (cur.id != x.id) {
              return null;
            }
            // Remove from chain and connect predecessor and successor
            else {
              cur.marked = true;
              prev.next = cur.next;

              size.getAndDecrement();

              return cur;
            }
          }
        } finally {
          cur.unlock();
        }
      } finally {
        prev.unlock();
      }
    }
  }

  // Check if chain contains a gift
  public boolean has(int id) {
    Present curr = head;
    while (curr.id < id) {
      curr = curr.next;
    }
    return curr.id == id && !curr.marked;
  }
}
