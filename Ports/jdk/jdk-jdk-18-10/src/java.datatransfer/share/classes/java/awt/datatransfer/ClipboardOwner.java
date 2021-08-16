/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

/**
 * Defines the interface for classes that will provide data to a clipboard. An
 * instance of this interface becomes the owner of the contents of a clipboard
 * (clipboard owner) if it is passed as an argument to
 * {@link Clipboard#setContents} method of the clipboard and this method returns
 * successfully. The instance remains the clipboard owner until another
 * application or another object within this application asserts ownership of
 * this clipboard.
 *
 * @author Amy Fowler
 * @see Clipboard
 * @since 1.1
 */
public interface ClipboardOwner {

    /**
     * Notifies this object that it is no longer the clipboard owner. This
     * method will be called when another application or another object within
     * this application asserts ownership of the clipboard.
     *
     * @param  clipboard the clipboard that is no longer owned
     * @param  contents the contents which this owner had placed on the
     *         {@code clipboard}
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents);
}
