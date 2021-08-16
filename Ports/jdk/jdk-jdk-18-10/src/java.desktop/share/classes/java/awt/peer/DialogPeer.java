/*
 * Copyright (c) 1995, 2007, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.peer;

import java.awt.*;

/**
 * The peer interface for {@link Dialog}. This adds a couple of dialog specific
 * features to the {@link WindowPeer} interface.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface DialogPeer extends WindowPeer {

    /**
     * Sets the title on the dialog window.
     *
     * @param title the title to set
     *
     * @see Dialog#setTitle(String)
     */
    void setTitle(String title);

    /**
     * Sets if the dialog should be resizable or not.
     *
     * @param resizeable {@code true} when the dialog should be resizable,
     *        {@code false} if not
     *
     * @see Dialog#setResizable(boolean)
     */
    void setResizable(boolean resizeable);

    /**
     * Block the specified windows. This is used for modal dialogs.
     *
     * @param windows the windows to block
     *
     * @see Dialog#modalShow()
     */
    void blockWindows(java.util.List<Window> windows);
}
