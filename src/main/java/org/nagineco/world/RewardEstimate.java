package org.nagineco.world;
// Compatibility adapter only.
public final class RewardEstimate {
 private final org.nagineco.doppel.core.RewardEstimate delegate=new org.nagineco.doppel.core.RewardEstimate();
 public void record(double reward){delegate.record(reward);}
 public int count(){return delegate.count();}
 public double mean(){return delegate.mean();}
 public double preference(){return delegate.preference();}
 public void restore(int count,double mean){delegate.restore(count,mean);}
}
