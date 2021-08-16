/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.print;

import java.lang.annotation.Native;

/**
 * The {@code Pageable} implementation represents a set of
 * pages to be printed. The {@code Pageable} object returns
 * the total number of pages in the set as well as the
 * {@link PageFormat} and {@link Printable} for a specified page.
 * @see java.awt.print.PageFormat
 * @see java.awt.print.Printable
 */
public interface Pageable {

    /**
     * This constant is returned from the
     * {@link #getNumberOfPages() getNumberOfPages}
     * method if a {@code Pageable} implementation does not know
     * the number of pages in its set.
     */
    @Native int UNKNOWN_NUMBER_OF_PAGES = -1;

    /**
     * Returns the number of pages in the set.
     * To enable advanced printing features,
     * it is recommended that {@code Pageable}
     * implementations return the true number of pages
     * rather than the
     * UNKNOWN_NUMBER_OF_PAGES constant.
     * @return the number of pages in this {@code Pageable}.
     */
    int getNumberOfPages();

    /**
     * Returns the {@code PageFormat} of the page specified by
     * {@code pageIndex}.
     * @param pageIndex the zero based index of the page whose
     *            {@code PageFormat} is being requested
     * @return the {@code PageFormat} describing the size and
     *          orientation.
     * @throws IndexOutOfBoundsException if
     *          the {@code Pageable} does not contain the requested
     *          page.
     */
    PageFormat getPageFormat(int pageIndex)
        throws IndexOutOfBoundsException;

    /**
     * Returns the {@code Printable} instance responsible for
     * rendering the page specified by {@code pageIndex}.
     * @param pageIndex the zero based index of the page whose
     *            {@code Printable} is being requested
     * @return the {@code Printable} that renders the page.
     * @throws IndexOutOfBoundsException if
     *            the {@code Pageable} does not contain the requested
     *            page.
     */
    Printable getPrintable(int pageIndex)
        throws IndexOutOfBoundsException;
}
