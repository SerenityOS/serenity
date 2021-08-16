/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.desktop;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.io.Serial;
import java.util.List;

/**
 * Event sent when the app is asked to open a list of files.
 *
 * @see OpenFilesHandler#openFiles
 * @since 9
 */
public final class OpenFilesEvent extends FilesEvent {

    /**
     * Use serialVersionUID from JDK 9 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3982871005867718956L;

    /**
     * The search term used to find the files.
     */
    final String searchTerm;

    /**
     * Constructs an {@code OpenFilesEvent}.
     *
     * @param  files the list of files
     * @param  searchTerm the search term
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()}
     *         returns {@code true}
     * @throws UnsupportedOperationException if Desktop API is not supported on
     *         the current platform
     * @see Desktop#isDesktopSupported()
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public OpenFilesEvent(final List<File> files, final String searchTerm) {
        super(files);
        this.searchTerm = searchTerm == null
                            ? ""
                            : searchTerm;
    }

    /**
     * Gets the search term. The platform may optionally provide the search term
     * that was used to find the files. This is for example the case on MacOS,
     * when the files were opened using the Spotlight search menu or a Finder
     * search window.
     * <p>
     * This is useful for highlighting the search term in the documents when
     * they are opened.
     *
     * @return the search term used to find the files
     */
    public String getSearchTerm() {
        return searchTerm;
    }
}
