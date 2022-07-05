package com.appyhigh.newsfeedsdk.callbacks;

public interface VideoEventListener {
    void videoCompleted(int position);
    void onPlaybackStartedAt(int position);
    boolean getIsVisible();
    void hideVideoSwitcher(boolean hide);
}
