package com.example.genkiplayer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlideshowTimings {
    private static final String TAG = "SlideshowTimings";
    private static int DEFAULT_DISPLAY_TIME = 7;
    private int[] pages = new int[100];
    private int defaultTime = DEFAULT_DISPLAY_TIME;
    private String pattern = "((\\d+)|(default))";
    private Pattern r = Pattern.compile(pattern);

    private String getFirstNumberOrDefault(String input) {
        Matcher m = r.matcher(input);
        m.find();
        return m.group(1);
    }

    public SlideshowTimings() {

    }

    public SlideshowTimings(String config) {
        config = config.replaceAll("(\\r|\\n)+", "\n");
        String[] lines = config.split("\n");
        for(String line : lines) {
            String[] parts = line.split(":");
            if (parts.length > 1) {
                String key = getFirstNumberOrDefault(parts[0]);
                String value = getFirstNumberOrDefault(parts[1]);
                int parsedValue = tryToParseInt(value);
                if (parsedValue > 0) {
                    if (key.equals("default")) {
                        defaultTime = parsedValue;
                    } else {
                        int keyAsInteger = tryToParseInt(key);
                        if (keyAsInteger > 0 && keyAsInteger < pages.length + 1) {
                            pages[keyAsInteger - 1] = parsedValue;
                        }
                    }
                }
            }
        }
//        Log.v(TAG, "timings: " + Arrays.toString(pages));
//        Log.v(TAG, "default: " + defaultTime);
    }

    public int secondsForSlide(int slideNumber) {
        try {
            if(pages[slideNumber] != 0) {
                return pages[slideNumber];
            }
        } catch (IndexOutOfBoundsException iobe) { }
        return defaultTime;
    }

    private int tryToParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
