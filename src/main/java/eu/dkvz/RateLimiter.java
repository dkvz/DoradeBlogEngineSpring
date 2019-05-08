package eu.dkvz;

import java.time.*;

public class RateLimiter {

  // Max request counter value:
	public static final int MAX_REQUESTS = 150;
	// We rate limit if max requests is reached in that timeframe (seconds):
  public static final int MAX_REQUESTS_TIME = 60;
  // Block duration in seconds:
  public static final int BLOCK_TIME = 60;
	
	private boolean rateLimit = false;
  private long lastUpdate = Instant.now().getEpochSecond();
  private int counter = 0;

  public boolean isAllowed() {
    // Increment counter and make all the checks:
    final long now = Instant.now().getEpochSecond();
    boolean past = false;
    if ((now - this.lastUpdate) >= RateLimiter.MAX_REQUESTS_TIME) {
      past = true;
    }
    if ((this.rateLimit && (now - this.lastUpdate) >= RateLimiter.BLOCK_TIME) || past) {
      // Unblock:
      this.reset();
    } else if (!this.rateLimit && 
      this.counter >= RateLimiter.MAX_REQUESTS &&
      !past) {
      // Block and reset last Update:
      this.rateLimit = true;
      this.lastUpdate = now;
    }
    this.counter++;
    return !this.rateLimit;
  }

  /**
   * @return the rateLimit
   */
  public boolean isRateLimit() {
    return rateLimit;
  }

  public void reset() {
    this.rateLimit = false;
    this.counter = 0;
    this.lastUpdate = Instant.now().getEpochSecond();
  }

}