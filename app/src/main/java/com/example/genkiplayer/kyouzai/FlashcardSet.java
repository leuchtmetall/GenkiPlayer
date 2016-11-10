package com.example.genkiplayer.kyouzai;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.Stack;


public class FlashcardSet extends SlideSet {
    private static final String TAG = "FLASHCARD SET";
    private Stack<Pair<SlideSpan, Integer>> history;
    private Stack<Pair<SlideSpan, Integer>> future;

    // Front: odd numbers, Back: even numbers
    public FlashcardSet(ArrayList<String> paths, View container) {
        super(paths, container);
        history = new Stack<>();
        future = new Stack<>();
    }

    public String getSlideName(int slideNumber) {
        return super.getSlideName(slideNumber * 2);
    }

    @Override
    public int length() {
        return super.length() / 2;
    }

    public Bitmap getFront(int slideNumber) {
        Log.v(TAG, "get Front #" + slideNumber + ", slide " + (slideNumber * 2 ) + "/" + super.length());
        return super.getSlide(slideNumber * 2);
    }

    public Bitmap getBack(int slideNumber) {
        int offset = (super.length() == slideNumber * 2 - 1) ? 0 : 1;
        Log.v(TAG, "get Back  #" + slideNumber + ", slide " + (slideNumber*2+offset) + "/" + super.length());
        return super.getSlide(slideNumber * 2 + offset);
    }

    public Bitmap getThumbnailSlide(int slideNumber) {
        Pair<SlideSpan, Integer> pair = slides.get(slideNumber * 2);
        return getThumbnailSlideForPair(pair);
    }

    @Override
    public void removeSlide(int index) {
        super.removeSlide(index * 2);
        super.removeSlide(index * 2);
    }

    public void copyToHistoryStack(int index) {
        history.add(super.getRawSlide(index * 2));
        int offset = (super.length() == index * 2 - 1) ? 0 : 1;
        history.add(super.getRawSlide(index * 2 + offset));
    }

    public Bitmap getFromHistoryStack() {
        if(history.empty()) {
            return null;
        }

        Pair<SlideSpan, Integer> lastItem = history.pop();
        Log.v(TAG, "getFromHistoryStack: " + lastItem + ", remaining count: " + history.size());
        future.push(lastItem);
        return lastItem.first.getSlide(lastItem.second);
    }

    public Bitmap getFromFutureStack() {
        if(future.empty()) {
            return null;
        }
        Pair<SlideSpan, Integer> lastItem = future.pop();
        if(future.empty()) {
            return null;
        }
        Log.v(TAG, "getFromFutureStack: " + lastItem + ", remaining count: " + future.size());
        history.push(lastItem);
        lastItem = future.peek();
        return lastItem.first.getSlide(lastItem.second);
    }
}
