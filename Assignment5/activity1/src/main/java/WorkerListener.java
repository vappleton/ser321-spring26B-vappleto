/**
 * WorkerListener is a coordination helper used by the Leader to track worker responses during a consensus round.
 *
 * It allows the Leader to:
 * -Wait until all worker threads have completed their execution
 * -Avoid using fixed delays with Thread.sleep
 * -Continue execution when either all repsonses are received or a timeout occurs.
 *
 * Each worker thread calls workerDone() when it finishes processing and tge Leader waits using waitforAll(timeout)
 *
 * This enables event-based synchronization instead of time-based waiting.
 */
public class WorkerListener {
    private int expected;
    private int received =0;

    public WorkerListener(int expected) {
        this.expected = expected;
    }

    public synchronized void workerDone() {
        received++;
        notifyAll();
    }

    public synchronized void waitForAll(long timeout) throws InterruptedException {

        long start = System.currentTimeMillis();

        while (received < expected) {
            long elapsed = System.currentTimeMillis() - start;
            long remaining = timeout - elapsed;

            if(remaining <=0) break;

            wait(remaining);
        }
    }
}
