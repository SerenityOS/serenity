/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.AWTKeyStroke;
import java.awt.event.KeyEvent;
import java.io.Serial;

import sun.swing.SwingAccessor;

/**
 * A KeyStroke represents a key action on the keyboard, or equivalent input
 * device. KeyStrokes can correspond to only a press or release of a particular
 * key, just as KEY_PRESSED and KEY_RELEASED KeyEvents do; alternately, they
 * can correspond to typing a specific Java character, just as KEY_TYPED
 * KeyEvents do. In all cases, KeyStrokes can specify modifiers (alt, shift,
 * control, meta, altGraph, or a combination thereof) which must be present during the
 * action for an exact match.
 * <p>
 * KeyStrokes are used to define high-level (semantic) action events. Instead
 * of trapping every keystroke and throwing away the ones you are not
 * interested in, those keystrokes you care about automatically initiate
 * actions on the Components with which they are registered.
 * <p>
 * KeyStrokes are immutable, and are intended to be unique. Client code cannot
 * create a KeyStroke; a variant of <code>getKeyStroke</code> must be used
 * instead. These factory methods allow the KeyStroke implementation to cache
 * and share instances efficiently.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see javax.swing.text.Keymap
 * @see #getKeyStroke
 *
 * @author Arnaud Weber
 * @author David Mendenhall
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class KeyStroke extends AWTKeyStroke {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -9060180771037902530L;

    static {
        SwingAccessor.setKeyStrokeAccessor(new SwingAccessor.KeyStrokeAccessor() {

            @Override
            public KeyStroke create() {
                return new KeyStroke();
            }
        });
    }

    private KeyStroke() {
    }
    private KeyStroke(char keyChar, int keyCode, int modifiers,
                      boolean onKeyRelease) {
        super(keyChar, keyCode, modifiers, onKeyRelease);
    }

    /**
     * Returns a shared instance of a <code>KeyStroke</code>
     * that represents a <code>KEY_TYPED</code> event for the
     * specified character.
     *
     * @param keyChar the character value for a keyboard key
     * @return a KeyStroke object for that key
     */
    public static KeyStroke getKeyStroke(char keyChar) {
        synchronized (AWTKeyStroke.class) {
            return (KeyStroke)getAWTKeyStroke(keyChar);
        }
    }

    /**
     * Returns an instance of a KeyStroke, specifying whether the key is
     * considered to be activated when it is pressed or released. Unlike all
     * other factory methods in this class, the instances returned by this
     * method are not necessarily cached or shared.
     *
     * @param keyChar the character value for a keyboard key
     * @param onKeyRelease <code>true</code> if this KeyStroke corresponds to a
     *        key release; <code>false</code> otherwise.
     * @return a KeyStroke object for that key
     * @deprecated use getKeyStroke(char)
     */
    @Deprecated
    public static KeyStroke getKeyStroke(char keyChar, boolean onKeyRelease) {
        return new KeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, onKeyRelease);
    }

    /**
     * Returns a shared instance of a {@code KeyStroke}
     * that represents a {@code KEY_TYPED} event for the
     * specified Character object and a
      * set of modifiers. Note that the first parameter is of type Character
     * rather than char. This is to avoid inadvertent clashes with calls to
     * <code>getKeyStroke(int keyCode, int modifiers)</code>.
     *
     * The modifiers consist of any combination of following:<ul>
     * <li>java.awt.event.InputEvent.SHIFT_DOWN_MASK
     * <li>java.awt.event.InputEvent.CTRL_DOWN_MASK
     * <li>java.awt.event.InputEvent.META_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK
     * </ul>
     * The old modifiers listed below also can be used, but they are
     * mapped to _DOWN_ modifiers. <ul>
     * <li>java.awt.event.InputEvent.SHIFT_MASK
     * <li>java.awt.event.InputEvent.CTRL_MASK
     * <li>java.awt.event.InputEvent.META_MASK
     * <li>java.awt.event.InputEvent.ALT_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_MASK
     * </ul>
     * also can be used, but they are mapped to _DOWN_ modifiers.
     *
     * Since these numbers are all different powers of two, any combination of
     * them is an integer in which each bit represents a different modifier
     * key. Use 0 to specify no modifiers.
     *
     * @param keyChar the Character object for a keyboard character
     * @param modifiers a bitwise-ored combination of any modifiers
     * @return an KeyStroke object for that key
     * @throws IllegalArgumentException if keyChar is null
     *
     * @see java.awt.event.InputEvent
     * @since 1.3
     */
    public static KeyStroke getKeyStroke(Character keyChar, int modifiers) {
        synchronized (AWTKeyStroke.class) {
            return (KeyStroke)getAWTKeyStroke(keyChar, modifiers);
        }
    }

    /**
     * Returns a shared instance of a KeyStroke, given a numeric key code and a
     * set of modifiers, specifying whether the key is activated when it is
     * pressed or released.
     * <p>
     * The "virtual key" constants defined in java.awt.event.KeyEvent can be
     * used to specify the key code. For example:<ul>
     * <li>java.awt.event.KeyEvent.VK_ENTER
     * <li>java.awt.event.KeyEvent.VK_TAB
     * <li>java.awt.event.KeyEvent.VK_SPACE
     * </ul>
     * Alternatively, the key code may be obtained by calling
     * <code>java.awt.event.KeyEvent.getExtendedKeyCodeForChar</code>.
     *
     * The modifiers consist of any combination of:<ul>
     * <li>java.awt.event.InputEvent.SHIFT_DOWN_MASK
     * <li>java.awt.event.InputEvent.CTRL_DOWN_MASK
     * <li>java.awt.event.InputEvent.META_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK
     * </ul>
     * The old modifiers <ul>
     * <li>java.awt.event.InputEvent.SHIFT_MASK
     * <li>java.awt.event.InputEvent.CTRL_MASK
     * <li>java.awt.event.InputEvent.META_MASK
     * <li>java.awt.event.InputEvent.ALT_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_MASK
     * </ul>
     * also can be used, but they are mapped to _DOWN_ modifiers.
     *
     * Since these numbers are all different powers of two, any combination of
     * them is an integer in which each bit represents a different modifier
     * key. Use 0 to specify no modifiers.
     *
     * @param keyCode an int specifying the numeric code for a keyboard key
     * @param modifiers a bitwise-ored combination of any modifiers
     * @param onKeyRelease <code>true</code> if the KeyStroke should represent
     *        a key release; <code>false</code> otherwise.
     * @return a KeyStroke object for that key
     *
     * @see java.awt.event.KeyEvent
     * @see java.awt.event.InputEvent
     */
    public static KeyStroke getKeyStroke(int keyCode, int modifiers,
                                         boolean onKeyRelease) {
        synchronized (AWTKeyStroke.class) {
            return (KeyStroke)getAWTKeyStroke(keyCode, modifiers,
                                              onKeyRelease);
        }
    }

    /**
     * Returns a shared instance of a KeyStroke, given a numeric key code and a
     * set of modifiers. The returned KeyStroke will correspond to a key press.
     * <p>
     * The "virtual key" constants defined in java.awt.event.KeyEvent can be
     * used to specify the key code. For example:<ul>
     * <li>java.awt.event.KeyEvent.VK_ENTER
     * <li>java.awt.event.KeyEvent.VK_TAB
     * <li>java.awt.event.KeyEvent.VK_SPACE
     * </ul>
     * Alternatively, the key code may be obtained by calling
     * <code>java.awt.event.KeyEvent.getExtendedKeyCodeForChar</code>.
     *
     * The modifiers consist of any combination of:<ul>
     * <li>java.awt.event.InputEvent.SHIFT_DOWN_MASK
     * <li>java.awt.event.InputEvent.CTRL_DOWN_MASK
     * <li>java.awt.event.InputEvent.META_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_DOWN_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK
     * </ul>
     * The old modifiers <ul>
     * <li>java.awt.event.InputEvent.SHIFT_MASK
     * <li>java.awt.event.InputEvent.CTRL_MASK
     * <li>java.awt.event.InputEvent.META_MASK
     * <li>java.awt.event.InputEvent.ALT_MASK
     * <li>java.awt.event.InputEvent.ALT_GRAPH_MASK
     * </ul>
     * also can be used, but they are mapped to _DOWN_ modifiers.
     *
     * Since these numbers are all different powers of two, any combination of
     * them is an integer in which each bit represents a different modifier
     * key. Use 0 to specify no modifiers.
     *
     * @param keyCode an int specifying the numeric code for a keyboard key
     * @param modifiers a bitwise-ored combination of any modifiers
     * @return a KeyStroke object for that key
     *
     * @see java.awt.event.KeyEvent
     * @see java.awt.event.InputEvent
     */
    public static KeyStroke getKeyStroke(int keyCode, int modifiers) {
        synchronized (AWTKeyStroke.class) {
            return (KeyStroke)getAWTKeyStroke(keyCode, modifiers);
        }
    }

    /**
     * Returns a KeyStroke which represents the stroke which generated a given
     * KeyEvent.
     * <p>
     * This method obtains the keyChar from a KeyTyped event, and the keyCode
     * from a KeyPressed or KeyReleased event. The KeyEvent modifiers are
     * obtained for all three types of KeyEvent.
     *
     * @param anEvent the KeyEvent from which to obtain the KeyStroke
     * @throws NullPointerException if <code>anEvent</code> is null
     * @return the KeyStroke that precipitated the event
     */
    public static KeyStroke getKeyStrokeForEvent(KeyEvent anEvent) {
        synchronized (AWTKeyStroke.class) {
            return (KeyStroke)getAWTKeyStrokeForEvent(anEvent);
        }
    }

    /**
     * Parses a string and returns a <code>KeyStroke</code>.
     * The string must have the following syntax:
     * <pre>
     *    &lt;modifiers&gt;* (&lt;typedID&gt; | &lt;pressedReleasedID&gt;)
     *
     *    modifiers := shift | control | ctrl | meta | alt | altGraph
     *    typedID := typed &lt;typedKey&gt;
     *    typedKey := string of length 1 giving Unicode character.
     *    pressedReleasedID := (pressed | released) key
     *    key := KeyEvent key code name, i.e. the name following "VK_".
     * </pre>
     * If typed, pressed or released is not specified, pressed is assumed. Here
     * are some examples:
     * <pre>
     *     "INSERT" =&gt; getKeyStroke(KeyEvent.VK_INSERT, 0);
     *     "control DELETE" =&gt; getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK);
     *     "alt shift X" =&gt; getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK);
     *     "alt shift released X" =&gt; getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, true);
     *     "typed a" =&gt; getKeyStroke('a');
     * </pre>
     *
     * In order to maintain backward-compatibility, specifying a null String,
     * or a String which is formatted incorrectly, returns null.
     *
     * @param s a String formatted as described above
     * @return a KeyStroke object for that String, or null if the specified
     *         String is null, or is formatted incorrectly
     *
     * @see java.awt.event.KeyEvent
     */
    public static KeyStroke getKeyStroke(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        synchronized (AWTKeyStroke.class) {
            try {
                return (KeyStroke)getAWTKeyStroke(s);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
