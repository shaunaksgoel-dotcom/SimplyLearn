package com.example.simplylearn.service;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    public static List<String> chunk(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        int index = 0;

        while (index < text.length()) {
            int end = Math.min(index + maxChars, text.length());
            chunks.add(text.substring(index, end));
            index = end;
        }

        return chunks;
    }
}
