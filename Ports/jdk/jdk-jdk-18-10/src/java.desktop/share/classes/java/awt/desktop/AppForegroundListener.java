/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Implementors are notified when the app becomes the foreground app and when it
 * is no longer the foreground app. This notification is useful for hiding and
 * showing transient UI like palette windows which should be hidden when the app
 * is in the background.
 *
 * @since 9
 */
public interface AppForegroundListener extends SystemEventListener {

    /**
     * Called when the app becomes the foreground app.
     *
     * @param  e event
     */
    public void appRaisedToForeground(AppForegroundEvent e);

    /**
     * Called when the app becomes the background app and another app becomes
     * the foreground app.
     *
     * @param  e event
     */
    public void appMovedToBackground(AppForegroundEvent e);
}
