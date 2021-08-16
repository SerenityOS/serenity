/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.EventListener;
import javax.accessibility.*;


/**
 * The GUIInitializedMulticaster class is used to maintain a list of
 * GUIInitializedListener classes.  It is intended to be used primarily
 * for internal support in the EventQueueMonitor class, and is not intended
 * to be used by classes outside the Java Accessibility Utility package.
 *
 * @see EventQueueMonitor
 * @see EventQueueMonitor#addGUIInitializedListener
 * @see EventQueueMonitor#removeGUIInitializedListener
 *
 */
class GUIInitializedMulticaster
    extends AWTEventMulticaster implements GUIInitializedListener
{
    protected GUIInitializedMulticaster(EventListener a, EventListener b) {
        super(a, b);
    }

    public void guiInitialized() {
        ((GUIInitializedListener)a).guiInitialized();
        ((GUIInitializedListener)b).guiInitialized();
    }

    public static GUIInitializedListener add(GUIInitializedListener a, GUIInitializedListener b) {
        return (GUIInitializedListener)addInternal(a, b);
    }

    public static GUIInitializedListener remove(GUIInitializedListener l, GUIInitializedListener oldl) {
        return (GUIInitializedListener)removeInternal(l, oldl);
    }

    protected static EventListener addInternal(EventListener a, EventListener b) {
        if (a == null)  return b;
        if (b == null)  return a;
        return new GUIInitializedMulticaster(a, b);
    }

    protected static EventListener removeInternal(EventListener l, EventListener oldl) {
        if (l == oldl || l == null) {
            return null;
        } else if (l instanceof GUIInitializedMulticaster) {
            return ((GUIInitializedMulticaster)l).remove(oldl);
        } else {
            return l;           // it's not here
        }
    }

}
