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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author aim
 */
public class OscarCandidate {

    private String category;
    private Integer year;
    private boolean winner = false;
    private String movie;
    private URI imdbURI;
    private final ArrayList<String> persons = new ArrayList<String>();

    /**
     * Creates a new instance of OscarCandidate
     */
    public OscarCandidate(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public String getMovieTitle() {
        return movie;
    }

    public void setMovieTitle(String movie) {
        this.movie = movie;
    }

    public URI getIMDBMovieURI() {
        return imdbURI;
    }

    public void setIMDBMovieURI(URI uri) {
        this.imdbURI = uri;
    }

    public List<String> getPersons() {
        return persons;
    }


}
