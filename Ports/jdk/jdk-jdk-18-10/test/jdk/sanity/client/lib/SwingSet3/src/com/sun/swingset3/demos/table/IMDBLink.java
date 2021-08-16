/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.swingset3.demos.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Class used to support converting a movie title string into an IMDB URI
 * corresponding to that movie's IMDB entry.   Since IMDB encodes entries with
 * an alpha-numeric key (rather than title), we have to use Yahoo search on the
 * title and then screenscrape the search results to find the IMDB key.
 *
 * @author aim
 */
public class IMDBLink {

    private IMDBLink() {
    }

    /**
     * @param movieTitle the title of the movie
     * @param year       the year the movie was nominated for the oscar
     * @return String containing URI for movie's IMDB entry or null if URI could not be found
     */
    public static String getMovieURIString(String movieTitle, int year) throws IOException {
        ArrayList<String> matches = new ArrayList<String>();
        URL url;
        BufferedReader reader;

        // btw, google rejects the request with a 403 return code!
        // URL url = new URL("http://www.google.com/search?q=Dazed+and+confused");
        // Thank you, yahoo, for granting our search request :-)
        try {
            String urlKey = URLEncoder.encode(movieTitle, "UTF-8");
            url = new URL("http://search.yahoo.com/search?ei=utf-8&fr=sfp&p=imdb+" +
                    urlKey + "&iscqry=");
        } catch (Exception ex) {
            System.err.println(ex);

            return null;
        }

        URLConnection conn = url.openConnection();
        conn.connect();

        // Get the response from Yahoo search query
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        // Parse response a find each imdb/titleString result
        String line;
        String imdbString = ".imdb.com";
        String titleStrings[] = {"/title", "/Title"};

        while ((line = reader.readLine()) != null) {
            for (String titleString : titleStrings) {
                String scrapeKey = imdbString + titleString;
                int index = line.indexOf(scrapeKey);
                if (index != -1) {
                    // The IMDB key looks something like "tt0032138"
                    // so we look for the 9 characters after the scrape key
                    // to construct the full IMDB URI.
                    // e.g. http://www.imdb.com/title/tt0032138
                    int len = scrapeKey.length();
                    String imdbURL = "http://www" +
                            line.substring(index, index + len) +
                            line.substring(index + len, index + len + 10);

                    if (!matches.contains(imdbURL)) {
                        matches.add(imdbURL);
                    }
                }
            }
        }
        reader.close();

        // Since imdb contains entries for multiple movies of the same titleString,
        // use the year to find the right entry
        if (matches.size() > 1) {
            for (String matchURL : matches) {
                if (verifyYear(matchURL, year)) {
                    return matchURL;
                }
            }
        }
        return matches.isEmpty()? null : matches.get(0);
    }


    private static boolean verifyYear(String imdbURL, int movieYear) throws IOException {
        boolean yearMatches = false;

        URLConnection conn = new URL(imdbURL).openConnection();
        conn.connect();

        // Get the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            int index = line.indexOf("</title>");
            if (index != -1) {
                // looking for "<title>movie title (YEAR)</title>"
                try {
                    int year = Integer.parseInt(line.substring(index - 5, index - 1));
                    // Movie may have been made the year prior to oscar award
                    yearMatches = year == movieYear || year == movieYear - 1;

                } catch (NumberFormatException ex) {
                    // ignore title lines that have other formatting
                }
                break; // only interested in analyzing the one line
            }
        }
        reader.close();

        return yearMatches;
    }
}
