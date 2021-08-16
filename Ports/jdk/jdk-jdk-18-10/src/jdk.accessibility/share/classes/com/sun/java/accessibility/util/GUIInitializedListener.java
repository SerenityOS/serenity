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
 * The {@code GUIInitializedListener} interface is used by the {@link EventQueueMonitor}
 * class to notify an interested party when the GUI subsystem has been
 * initialized.  This is necessary because assistive technologies can
 * be loaded before the GUI subsystem is initialized.  As a result,
 * assistive technologies should check the
 * {@link EventQueueMonitor#isGUIInitialized isGUIInitialized} method
 * of {@code EventQueueMonitor} before creating any GUI components.  If the
 * return value is true, assistive technologies can create GUI components
 * following the same thread restrictions as any other application.  If
 * the return value is false, the assistive technology should register
 * a {@code GUIInitializedListener} with the {@code EventQueueMonitor} to be notified
 * when the GUI subsystem is initialized.
 *
 * @see EventQueueMonitor
 * @see EventQueueMonitor#isGUIInitialized
 * @see EventQueueMonitor#addGUIInitializedListener
 * @see EventQueueMonitor#removeGUIInitializedListener
 *
 */
public interface GUIInitializedListener extends EventListener {

    /**
     * Invoked when the GUI subsystem is initialized and it's OK for
     * the assisitive technology to create instances of GUI objects.
     */
    public void guiInitialized();

}
