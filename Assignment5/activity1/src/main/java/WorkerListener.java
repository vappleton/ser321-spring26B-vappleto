/**
 * Waits for workers to finish their work and notifies the Leader
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
