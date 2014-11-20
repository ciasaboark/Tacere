/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.ciasaboark.tacere.R;

import java.util.Random;

public class TutorialWelcomeFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_page_welcome;
    private Quote quote1 = new Quote("Max Ehrmann", "Go placidly amid the noise and haste, and remember what peace there may be in silence.", "Desiderata");
//    private Quote quote2 = new Quote("Francis Bacon", "Silence is the sleep that nourishes wisdom.", null);
//    private Quote quote3 = new Quote("Confucius", "Silence is a true friend who never betrays.", null);


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        Quote quote = getRandomQuote();
        TextView quoteText = (TextView) rootView.findViewById(R.id.quote_text);
        TextView quoteAuthor = (TextView) rootView.findViewById(R.id.quote_author);
        TextView quoteSource = (TextView) rootView.findViewById(R.id.quote_source);
        quoteText.setText("\"" + quote.getQuote() + "\"");
        quoteAuthor.setText("-" + quote.author);
        quoteSource.setText(quote.getSource());

        return rootView;
    }

    private Quote getRandomQuote() {
        Random random = new Random();
        int quoteNumber = random.nextInt(2);
        switch (quoteNumber) {
            default:
                return quote1;
        }
    }

    private class Quote {
        String author = null;
        String quote = null;
        String source = null;

        public Quote(String author, String quote, String source) {
            this.author = author;
            this.quote = quote;
        }

        public String getAuthor() {
            return author == null ? "" : author;
        }

        public String getQuote() {
            return quote == null ? "" : quote;
        }

        public String getSource() {
            return source == null ? "" : source;
        }
    }
}
