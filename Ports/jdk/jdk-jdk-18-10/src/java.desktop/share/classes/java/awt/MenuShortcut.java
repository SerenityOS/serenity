/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;

/**
 * The {@code MenuShortcut} class represents a keyboard accelerator
 * for a MenuItem.
 * <p>
 * Menu shortcuts are created using virtual keycodes, not characters.
 * For example, a menu shortcut for Ctrl-a (assuming that Control is
 * the accelerator key) would be created with code like the following:
 * <p>
 * {@code MenuShortcut ms = new MenuShortcut(KeyEvent.VK_A, false);}
 * <p> or alternatively
 * <p>
 * {@code MenuShortcut ms = new MenuShortcut(KeyEvent.getExtendedKeyCodeForChar('A'), false);}
 * <p>
 * Menu shortcuts may also be constructed for a wider set of keycodes
 * using the {@code java.awt.event.KeyEvent.getExtendedKeyCodeForChar} call.
 * For example, a menu shortcut for "Ctrl+cyrillic ef" is created by
 * <p>
 * <code>MenuShortcut ms = new MenuShortcut(KeyEvent.getExtendedKeyCodeForChar('\u0444'), false);</code>
 * <p>
 * Note that shortcuts created with a keycode or an extended keycode defined as a constant in {@code KeyEvent}
 * work regardless of the current keyboard layout. However, a shortcut made of
 * an extended keycode not listed in {@code KeyEvent}
 * only work if the current keyboard layout produces a corresponding letter.
 * <p>
 * The accelerator key is platform-dependent and may be obtained
 * via {@link Toolkit#getMenuShortcutKeyMaskEx()}.
 *
 * @author Thomas Ball
 * @since 1.1
 */
public class MenuShortcut implements java.io.Serializable
{
    /**
     * The virtual keycode for the menu shortcut.
     * This is the keycode with which the menu shortcut will be created.
     * Note that it is a virtual keycode, not a character,
     * e.g. KeyEvent.VK_A, not 'a'.
     * Note: in 1.1.x you must use setActionCommand() on a menu item
     * in order for its shortcut to work, otherwise it will fire a null
     * action command.
     *
     * @serial
     * @see #getKey()
     * @see #usesShiftModifier()
     * @see java.awt.event.KeyEvent
     * @since 1.1
     */
    int key;

    /**
     * Indicates whether the shift key was pressed.
     * If true, the shift key was pressed.
     * If false, the shift key was not pressed
     *
     * @serial
     * @see #usesShiftModifier()
     * @since 1.1
     */
    boolean usesShift;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = 143448358473180225L;

    /**
     * Constructs a new MenuShortcut for the specified virtual keycode.
     * @param key the raw keycode for this MenuShortcut, as would be returned
     * in the keyCode field of a {@link java.awt.event.KeyEvent KeyEvent} if
     * this key were pressed.
     * @see java.awt.event.KeyEvent
     **/
    public MenuShortcut(int key) {
        this(key, false);
    }

    /**
     * Constructs a new MenuShortcut for the specified virtual keycode.
     * @param key the raw keycode for this MenuShortcut, as would be returned
     * in the keyCode field of a {@link java.awt.event.KeyEvent KeyEvent} if
     * this key were pressed.
     * @param useShiftModifier indicates whether this MenuShortcut is invoked
     * with the SHIFT key down.
     * @see java.awt.event.KeyEvent
     **/
    public MenuShortcut(int key, boolean useShiftModifier) {
        this.key = key;
        this.usesShift = useShiftModifier;
    }

    /**
     * Returns the raw keycode of this MenuShortcut.
     * @return the raw keycode of this MenuShortcut.
     * @see java.awt.event.KeyEvent
     * @since 1.1
     */
    public int getKey() {
        return key;
    }

    /**
     * Returns whether this MenuShortcut must be invoked using the SHIFT key.
     * @return {@code true} if this MenuShortcut must be invoked using the
     * SHIFT key, {@code false} otherwise.
     * @since 1.1
     */
    public boolean usesShiftModifier() {
        return usesShift;
    }

    /**
     * Returns whether this MenuShortcut is the same as another:
     * equality is defined to mean that both MenuShortcuts use the same key
     * and both either use or don't use the SHIFT key.
     * @param s the MenuShortcut to compare with this.
     * @return {@code true} if this MenuShortcut is the same as another,
     * {@code false} otherwise.
     * @since 1.1
     */
    public boolean equals(MenuShortcut s) {
        return (s != null && (s.getKey() == key) &&
                (s.usesShiftModifier() == usesShift));
    }

    /**
     * Returns whether this MenuShortcut is the same as another:
     * equality is defined to mean that both MenuShortcuts use the same key
     * and both either use or don't use the SHIFT key.
     * @param obj the Object to compare with this.
     * @return {@code true} if this MenuShortcut is the same as another,
     * {@code false} otherwise.
     * @since 1.2
     */
    public boolean equals(Object obj) {
        if (obj instanceof MenuShortcut) {
            return equals( (MenuShortcut) obj );
        }
        return false;
    }

    /**
     * Returns the hashcode for this MenuShortcut.
     * @return the hashcode for this MenuShortcut.
     * @since 1.2
     */
    public int hashCode() {
        return (usesShift) ? (~key) : key;
    }

    /**
     * Returns an internationalized description of the MenuShortcut.
     * @return a string representation of this MenuShortcut.
     * @since 1.1
     */
    public String toString() {
        int modifiers = 0;
        if (!GraphicsEnvironment.isHeadless()) {
            modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        }
        if (usesShiftModifier()) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        return InputEvent.getModifiersExText(modifiers) + "+" +
               KeyEvent.getKeyText(key);
    }

    /**
     * Returns the parameter string representing the state of this
     * MenuShortcut. This string is useful for debugging.
     * @return    the parameter string of this MenuShortcut.
     * @since 1.1
     */
    protected String paramString() {
        String str = "key=" + key;
        if (usesShiftModifier()) {
            str += ",usesShiftModifier";
        }
        return str;
    }
}
