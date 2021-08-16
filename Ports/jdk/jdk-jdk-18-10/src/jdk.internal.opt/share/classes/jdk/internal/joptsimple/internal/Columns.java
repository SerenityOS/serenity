/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * The MIT License
 *
 * Copyright (c) 2004-2015 Paul R. Holser, Jr.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package jdk.internal.joptsimple.internal;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import static java.text.BreakIterator.*;

import static jdk.internal.joptsimple.internal.Strings.*;

/**
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
class Columns {
    private static final int INDENT_WIDTH = 2;

    private final int optionWidth;
    private final int descriptionWidth;

    Columns( int optionWidth, int descriptionWidth ) {
        this.optionWidth = optionWidth;
        this.descriptionWidth = descriptionWidth;
    }

    List<Row> fit( Row row ) {
        List<String> options = piecesOf( row.option, optionWidth );
        List<String> descriptions = piecesOf( row.description, descriptionWidth );

        List<Row> rows = new ArrayList<>();
        for ( int i = 0; i < Math.max( options.size(), descriptions.size() ); ++i )
            rows.add( new Row( itemOrEmpty( options, i ), itemOrEmpty( descriptions, i ) ) );

        return rows;
    }

    private static String itemOrEmpty( List<String> items, int index ) {
        return index >= items.size() ? "" : items.get( index );
    }

    private List<String> piecesOf( String raw, int width ) {
        List<String> pieces = new ArrayList<>();

        for ( String each : raw.trim().split( LINE_SEPARATOR ) )
            pieces.addAll( piecesOfEmbeddedLine( each, width ) );

        return pieces;
    }

    private List<String> piecesOfEmbeddedLine( String line, int width ) {
        List<String> pieces = new ArrayList<>();

        BreakIterator words = BreakIterator.getLineInstance();
        words.setText( line );

        StringBuilder nextPiece = new StringBuilder();

        int start = words.first();
        for ( int end = words.next(); end != DONE; start = end, end = words.next() )
            nextPiece = processNextWord( line, nextPiece, start, end, width, pieces );

        if ( nextPiece.length() > 0 )
            pieces.add( nextPiece.toString() );

        return pieces;
    }

    private StringBuilder processNextWord( String source, StringBuilder nextPiece, int start, int end, int width,
                                           List<String> pieces ) {
        StringBuilder augmented = nextPiece;

        String word = source.substring( start, end );
        if ( augmented.length() + word.length() > width ) {
            pieces.add( augmented.toString().replaceAll( "\\s+$", "" ) );
            augmented = new StringBuilder( repeat( ' ', INDENT_WIDTH ) ).append( word );
        }
        else
            augmented.append( word );

        return augmented;
    }
}
