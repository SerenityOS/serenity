/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.accessibility.util;

import java.awt.*;
import java.util.*;
import javax.accessibility.*;

/**
 * The {@code TopLevelWindowListener} interface is used by the {@link EventQueueMonitor}
 * class to notify an interested party when a top level window is created
 * or destroyed in the Java Virtual Machine.  Classes wishing to express
 * an interest in top level window events should implement this interface
 * and register themselves with the {@code EventQueueMonitor} by calling the
 * {@link EventQueueMonitor#addTopLevelWindowListener EventQueueMonitor.addTopLevelWindowListener}
 * class method.
 *
 * @see EventQueueMonitor
 * @see EventQueueMonitor#addTopLevelWindowListener
 * @see EventQueueMonitor#removeTopLevelWindowListener
 *
 */
public interface TopLevelWindowListener extends EventListener {

    /**
     * Invoked when a new top level window has been created.
     *
     * @param w the Window that was created
     */
    public void topLevelWindowCreated(Window w);

    /**
     * Invoked when a top level window has been destroyed.
     *
     * @param w the Window that was destroyed
     */
    public void topLevelWindowDestroyed(Window w);
}
