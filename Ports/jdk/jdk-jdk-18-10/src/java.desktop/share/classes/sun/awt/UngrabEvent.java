/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.AWTEvent;
import java.awt.Component;

/**
 * Sent when one of the following events occur on the grabbed window: <ul>
 * <li> it looses focus, but not to one of the owned windows
 * <li> mouse click on the outside area happens (except for one of the owned windows)
 * <li> switch to another application or desktop happens
 * <li> click in the non-client area of the owning window or this window happens
 * </ul>
 *
 * <p>Notice that this event is not generated on mouse click inside of the window area.
 * <p>To listen for this event, install AWTEventListener with {@value sun.awt.SunToolkit#GRAB_EVENT_MASK}
 */
@SuppressWarnings("serial")
public class UngrabEvent extends AWTEvent {

    private static final int UNGRAB_EVENT_ID = 1998;

    public UngrabEvent(Component source) {
        super(source, UNGRAB_EVENT_ID);
    }

    public String toString() {
        return "sun.awt.UngrabEvent[" + getSource() + "]";
    }
}
