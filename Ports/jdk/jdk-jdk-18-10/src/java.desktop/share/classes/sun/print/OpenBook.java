/*
 * Copyright (c) 1998, 2000, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.awt.print.Pageable;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

/**
 * A Book with an unknown number of pages where each
 * page has the same format and painter. This class
 * is used by PrinterJob to print Pageable jobs.
 */

class OpenBook implements Pageable {

 /* Class Constants */

 /* Class Variables */

 /* Instance Variables */

    /**
     * The format of all of the pages.
     */
    private PageFormat mFormat;

    /**
     * The object that will render all of the pages.
     */
    private Printable mPainter;

 /* Instance Methods */

    /**
     * Create a  Pageable with an unknown number of pages
     * where every page shares the same format and
     * Printable.
     */
    OpenBook(PageFormat format, Printable painter) {
        mFormat = format;
        mPainter = painter;
    }

    /**
     * This object does not know the number of pages.
     */
    public int getNumberOfPages(){
        return UNKNOWN_NUMBER_OF_PAGES;
    }

    /**
     * Return the PageFormat of the page specified by 'pageIndex'.
     * @param pageIndex The zero based index of the page whose
     *                  PageFormat is being requested.
     * @return The PageFormat describing the size and orientation
     */
    public PageFormat getPageFormat(int pageIndex) {
        return mFormat;
    }

    /**
     * Return the Printable instance responsible for rendering
     * the page specified by 'pageIndex'.
     * @param pageIndex The zero based index of the page whose
     *                  Printable is being requested.
     * @return The Printable that will draw the page.
     */
    public Printable getPrintable(int pageIndex)
        throws IndexOutOfBoundsException
    {
        return mPainter;
    }
}
