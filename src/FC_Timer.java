


/* FC_Timer.java
 Version 1.0
 Praktikum Rechnernetze HAW Hamburg
 Autor: M. H�bner
 */

public class FC_Timer extends Thread {
  /* Special timer for FileCopy Objects */
  private FileCopyClient myFCC;
  private long delay;
  private long seqNum;

  /**
   * Timeout value (delay) must be given in nanoseconds [see
   * System.nanoTime()]
   * 
   */
  public FC_Timer(long timeout, FileCopyClient fc, long seqNum) {
    this.delay = timeout; // nanoseconds
    this.myFCC = fc;
    this.seqNum = seqNum;
  }

  public void run() {
    /*
     * Timer sleeps until delay is over (--> timeoutTask - call!) or is
     * interrupted
     */
    long millis = delay / 1000000L;
    int nanos = (int) (delay % 1000000L);
    myFCC.testOut("FC_Timer started for packet: " + seqNum + " Delay: "
        + delay + "ns");

    try {
      sleep(millis, nanos);
    } catch (InterruptedException e) {
      /* Timer cancelled */
      Thread.currentThread().interrupt();
    }

    myFCC.testOut("FC_Timer stopped! Packet: " + seqNum);

    /* Perform task if not cancelled */
    if (!isInterrupted()) {
      myFCC.timeoutTask(seqNum);
    }
  }
}
