/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.*;

/**
 * A class representing Cocoa NSEvent class with the fields only necessary for
 * JDK functionality.
 */
final class NSEvent {

    static final int SCROLL_PHASE_UNSUPPORTED = 1;
    static final int SCROLL_PHASE_BEGAN = 2;
    static final int SCROLL_PHASE_CONTINUED = 3;
    static final int SCROLL_PHASE_MOMENTUM_BEGAN = 4;
    static final int SCROLL_PHASE_ENDED = 5;

    private int type;
    private int modifierFlags;

    // Mouse event information
    private int clickCount;
    private int buttonNumber;
    private int x;
    private int y;
    private double scrollDeltaY;
    private double scrollDeltaX;
    private int scrollPhase;
    private int absX;
    private int absY;

    // Key event information
    private short keyCode;
    private String characters;
    private String charactersIgnoringModifiers;

    // Called from native
    NSEvent(int type, int modifierFlags, short keyCode, String characters, String charactersIgnoringModifiers) {
        this.type = type;
        this.modifierFlags = modifierFlags;
        this.keyCode = keyCode;
        this.characters = characters;
        this.charactersIgnoringModifiers = charactersIgnoringModifiers;
    }

    // Called from native
    NSEvent(int type, int modifierFlags, int clickCount, int buttonNumber,
                   int x, int y, int absX, int absY,
                   double scrollDeltaY, double scrollDeltaX, int scrollPhase) {
        this.type = type;
        this.modifierFlags = modifierFlags;
        this.clickCount = clickCount;
        this.buttonNumber = buttonNumber;
        this.x = x;
        this.y = y;
        this.absX = absX;
        this.absY = absY;
        this.scrollDeltaY = scrollDeltaY;
        this.scrollDeltaX = scrollDeltaX;
        this.scrollPhase = scrollPhase;
    }

    int getType() {
        return type;
    }

    int getModifierFlags() {
        return modifierFlags;
    }

    int getClickCount() {
        return clickCount;
    }

    int getButtonNumber() {
        return buttonNumber;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    double getScrollDeltaY() {
        return scrollDeltaY;
    }

    double getScrollDeltaX() {
        return scrollDeltaX;
    }

    int getScrollPhase() {
        return scrollPhase;
    }

    int getAbsX() {
        return absX;
    }

    int getAbsY() {
        return absY;
    }

    short getKeyCode() {
        return keyCode;
    }

    String getCharactersIgnoringModifiers() {
        return charactersIgnoringModifiers;
    }

    String getCharacters() {
        return characters;
    }

    @Override
    public String toString() {
        return "NSEvent[" + getType() + " ," + getModifierFlags() + " ,"
                + getClickCount() + " ," + getButtonNumber() + " ," + getX() + " ,"
                + getY() + " ," + getAbsX() + " ," + getAbsY()+ " ," + getKeyCode() + " ,"
                + getCharacters() + " ," + getCharactersIgnoringModifiers() + "]";
    }

    /*
     * Converts an NSEvent button number to a MouseEvent constant.
     */
    static int nsToJavaButton(int buttonNumber) {
        int jbuttonNumber = buttonNumber + 1;
        switch (buttonNumber) {
            case CocoaConstants.kCGMouseButtonLeft:
                jbuttonNumber = MouseEvent.BUTTON1;
                break;
            case CocoaConstants.kCGMouseButtonRight:
                jbuttonNumber = MouseEvent.BUTTON3;
                break;
            case CocoaConstants.kCGMouseButtonCenter:
                jbuttonNumber = MouseEvent.BUTTON2;
                break;
        }
        return jbuttonNumber;
    }

    /*
     * Converts NPCocoaEvent types to AWT event types.
     */
    static int npToJavaEventType(int npEventType) {
        int jeventType = 0;
        switch (npEventType) {
            case CocoaConstants.NPCocoaEventMouseDown:
                jeventType = MouseEvent.MOUSE_PRESSED;
                break;
            case CocoaConstants.NPCocoaEventMouseUp:
                jeventType = MouseEvent.MOUSE_RELEASED;
                break;
            case CocoaConstants.NPCocoaEventMouseMoved:
                jeventType = MouseEvent.MOUSE_MOVED;
                break;
            case CocoaConstants.NPCocoaEventMouseEntered:
                jeventType = MouseEvent.MOUSE_ENTERED;
                break;
            case CocoaConstants.NPCocoaEventMouseExited:
                jeventType = MouseEvent.MOUSE_EXITED;
                break;
            case CocoaConstants.NPCocoaEventMouseDragged:
                jeventType = MouseEvent.MOUSE_DRAGGED;
                break;
            case CocoaConstants.NPCocoaEventKeyDown:
                jeventType = KeyEvent.KEY_PRESSED;
                break;
            case CocoaConstants.NPCocoaEventKeyUp:
                jeventType = KeyEvent.KEY_RELEASED;
                break;
        }
        return jeventType;
    }

    /*
     * Converts NSEvent types to AWT event types.
     */
    static int nsToJavaEventType(int nsEventType) {
        int jeventType = 0;
        switch (nsEventType) {
            case CocoaConstants.NSLeftMouseDown:
            case CocoaConstants.NSRightMouseDown:
            case CocoaConstants.NSOtherMouseDown:
                jeventType = MouseEvent.MOUSE_PRESSED;
                break;
            case CocoaConstants.NSLeftMouseUp:
            case CocoaConstants.NSRightMouseUp:
            case CocoaConstants.NSOtherMouseUp:
                jeventType = MouseEvent.MOUSE_RELEASED;
                break;
            case CocoaConstants.NSMouseMoved:
                jeventType = MouseEvent.MOUSE_MOVED;
                break;
            case CocoaConstants.NSLeftMouseDragged:
            case CocoaConstants.NSRightMouseDragged:
            case CocoaConstants.NSOtherMouseDragged:
                jeventType = MouseEvent.MOUSE_DRAGGED;
                break;
            case CocoaConstants.NSMouseEntered:
                jeventType = MouseEvent.MOUSE_ENTERED;
                break;
            case CocoaConstants.NSMouseExited:
                jeventType = MouseEvent.MOUSE_EXITED;
                break;
            case CocoaConstants.NSScrollWheel:
                jeventType = MouseEvent.MOUSE_WHEEL;
                break;
            case CocoaConstants.NSKeyDown:
                jeventType = KeyEvent.KEY_PRESSED;
                break;
            case CocoaConstants.NSKeyUp:
                jeventType = KeyEvent.KEY_RELEASED;
                break;
        }
        return jeventType;
    }

    /**
     * Converts NSEvent key modifiers to AWT key modifiers. Note that this
     * method adds the current mouse state as a mouse modifiers.
     *
     * @param  modifierFlags the NSEvent key modifiers
     * @return the java key and mouse modifiers
     */
    static native int nsToJavaModifiers(int modifierFlags);

    /*
     * Converts NSEvent key info to AWT key info.
     */
    static native boolean nsToJavaKeyInfo(int[] in, int[] out);

    /*
     * Converts NSEvent key modifiers to AWT key info.
     */
    static native void nsKeyModifiersToJavaKeyInfo(int[] in, int[] out);

    /*
     * There is a small number of NS characters that need to be converted
     * into other characters before we pass them to AWT.
     */
    static native char nsToJavaChar(char nsChar, int modifierFlags, boolean spaceKeyTyped);

    static boolean isPopupTrigger(int jmodifiers) {
        final boolean isRightButtonDown = ((jmodifiers & InputEvent.BUTTON3_DOWN_MASK) != 0);
        final boolean isLeftButtonDown = ((jmodifiers & InputEvent.BUTTON1_DOWN_MASK) != 0);
        final boolean isControlDown = ((jmodifiers & InputEvent.CTRL_DOWN_MASK) != 0);
        return isRightButtonDown || (isControlDown && isLeftButtonDown);
    }
}
