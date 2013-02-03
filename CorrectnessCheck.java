import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorrectnessCheck implements Runnable {

    private final int numThreads;
    private final int keyRange;
    private final int readPercent;
    private final IntSet impl;
    private final int millis;

    private volatile boolean done;

    public CorrectnessCheck(final int numThreads,
                            final int keyRange,
                            final int readPercent,
                            final IntSet impl,
                            final int millis) {
        this.numThreads = numThreads;
        this.keyRange = keyRange;
        this.readPercent = readPercent;
        this.impl = impl;
        this.millis = millis;
    }

    public void run() {
        try {
            runImpl();
        } catch (final InterruptedException xx) {
            throw new Error("unexpected", xx);
        } catch (final BrokenBarrierException xx) {
            throw new RuntimeException("unexpected", xx);
        }
    }

    private void runImpl() throws InterruptedException, BrokenBarrierException {
        System.out.printf("%2d threads, %16s, range %d, %d%% read: ",
                numThreads, impl.getClass().getSimpleName(), keyRange, readPercent);

        final CyclicBarrier barrier = new CyclicBarrier(numThreads + 1);

        final Thread[] threads = new Thread[numThreads];
        final long[] passed = new long[numThreads];
        final long[] failed = new long[numThreads];

        for (int i = 0; i < threads.length; ++i) {
            final int index = i;
            threads[i] = new Thread("worker #" + i) {
                public void run() {
                    final Set<Integer> reference = new HashSet<Integer>();
                    final Random rand = new Random(index);
                    try {
                        barrier.await();
                    } catch (final InterruptedException xx) {
                        throw new Error("unexpected", xx);
                    } catch (final BrokenBarrierException xx) {
                        throw new RuntimeException("unexpected", xx);
                    }
                    int passes = 0, failures = 0;
                    while (!done) {
                        final int key = rand.nextInt(keyRange/numThreads)*numThreads + index;
                        final int percent = rand.nextInt(200);
                        if (percent < readPercent * 2) {
                            if (impl.contains(key) == reference.contains(key)) {
                                passes++;
                            } else {
                                failures++;
                            }
                        } else if ((percent % 2) == 0) {
                            impl.add(key);
                            reference.add(key);
                        } else {
                            impl.remove(key);
                            reference.remove(key);
                        }
                    }
                    passed[index] = passes;
                    failed[index] = failures;
                }
            };
        }

        for (Thread t : threads) {
            t.start();
        }
        barrier.await();
        Thread.sleep(millis);
        done = true;

        for (Thread t : threads) {
            t.join();
        }

        long total_passed = 0, total_failed = 0;
        for (long passes : passed) {
            total_passed += passes;
        }
        for (long failures : failed) {
            total_failed += failures;
        }
        long total = total_passed + total_failed;
        double percent_passed = 100.0*((double)total_passed)/(total_passed + total_failed);

        System.out.printf("Passed %d of %d (%.1f%%)\n",
                          total_passed, total, percent_passed);
    }
}
