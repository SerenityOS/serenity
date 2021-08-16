/*
 * Copyright (c) 1995, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * The peer interface for {@link Container}. This is the parent interface
 * for all container like widgets.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface ContainerPeer extends ComponentPeer {

    /**
     * Returns the insets of this container. Insets usually is the space that
     * is occupied by things like borders.
     *
     * @return the insets of this container
     */
    Insets getInsets();

    /**
     * Notifies the peer that validation of the component tree is about to
     * begin.
     *
     * @see Container#validate()
     */
    void beginValidate();

    /**
     * Notifies the peer that validation of the component tree is finished.
     *
     * @see Container#validate()
     */
    void endValidate();

    /**
     * Notifies the peer that layout is about to begin. This is called
     * before the container itself and its children are laid out.
     *
     * @see Container#validateTree()
     */
    void beginLayout();

    /**
     * Notifies the peer that layout is finished. This is called after the
     * container and its children have been laid out.
     *
     * @see Container#validateTree()
     */
    void endLayout();
}
