/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;


public final class CocoaConstants {
    private CocoaConstants(){}

    //from the NSEvent class reference:
    public static final int NSLeftMouseDown      = 1;
    public static final int NSLeftMouseUp        = 2;
    public static final int NSRightMouseDown     = 3;
    public static final int NSRightMouseUp       = 4;
    public static final int NSMouseMoved         = 5;
    public static final int NSLeftMouseDragged   = 6;
    public static final int NSRightMouseDragged  = 7;
    public static final int NSMouseEntered       = 8;
    public static final int NSMouseExited        = 9;
    public static final int NSKeyDown            = 10;
    public static final int NSKeyUp              = 11;
    public static final int NSFlagsChanged       = 12;

    public static final int NSScrollWheel        = 22;
    public static final int NSOtherMouseDown     = 25;
    public static final int NSOtherMouseUp       = 26;
    public static final int NSOtherMouseDragged  = 27;

    public static final int AllLeftMouseEventsMask =
        1 << NSLeftMouseDown |
        1 << NSLeftMouseUp |
        1 << NSLeftMouseDragged;

    public static final int AllRightMouseEventsMask =
        1 << NSRightMouseDown |
        1 << NSRightMouseUp |
        1 << NSRightMouseDragged;

    public static final int AllOtherMouseEventsMask =
        1 << NSOtherMouseDown |
        1 << NSOtherMouseUp |
        1 << NSOtherMouseDragged;

    /*
    NSAppKitDefined      = 13,
    NSSystemDefined      = 14,
    NSApplicationDefined = 15,
    NSPeriodic           = 16,
    NSCursorUpdate       = 17,
    NSScrollWheel        = 22,
    NSTabletPoint        = 23,
    NSTabletProximity    = 24,
    NSEventTypeGesture   = 29,
    NSEventTypeMagnify   = 30,
    NSEventTypeSwipe     = 31,
    NSEventTypeRotate    = 18,
    NSEventTypeBeginGesture = 19,
    NSEventTypeEndGesture   = 20
    */

    // See http://developer.apple.com/library/mac/#documentation/Carbon/Reference/QuartzEventServicesRef/Reference/reference.html

    public static final int kCGMouseButtonLeft   = 0;
    public static final int kCGMouseButtonRight  = 1;
    public static final int kCGMouseButtonCenter = 2;

    // See https://wiki.mozilla.org/NPAPI:CocoaEventModel

    public static final int NPCocoaEventDrawRect           = 1;
    public static final int NPCocoaEventMouseDown          = 2;
    public static final int NPCocoaEventMouseUp            = 3;
    public static final int NPCocoaEventMouseMoved         = 4;
    public static final int NPCocoaEventMouseEntered       = 5;
    public static final int NPCocoaEventMouseExited        = 6;
    public static final int NPCocoaEventMouseDragged       = 7;
    public static final int NPCocoaEventKeyDown            = 8;
    public static final int NPCocoaEventKeyUp              = 9;
    public static final int NPCocoaEventFlagsChanged       = 10;
    public static final int NPCocoaEventFocusChanged       = 11;
    public static final int NPCocoaEventWindowFocusChanged = 12;
    public static final int NPCocoaEventScrollWheel        = 13;
    public static final int NPCocoaEventTextInput          = 14;
}
