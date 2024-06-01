package org.example.tools.bot_tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BadWordsFilter {
    private Set<String> badWords;

    public BadWordsFilter() {
        badWords = new HashSet<>();
        loadBadWords();
    }

    private void loadBadWords() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/bad-words.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                badWords.add(line.toLowerCase().trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean containsBadWord(String text) {
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (badWords.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    public Set<String> returnBadWords (String text){
        Set<String> badWordsSet = new HashSet<>();
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (badWords.contains(word.toLowerCase())) {
                badWordsSet.add(word);
            }
        }
        return badWordsSet;
    }


}
