/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Toolkit that provides resource support for JConsole.
 */
public final class Resources {
    private static Map<String, Integer> MNEMONIC_LOOKUP = Collections
            .synchronizedMap(new IdentityHashMap<String, Integer>());

    private Resources() {
        throw new AssertionError();
    }

    /**
     * Convenience method for {@link MessageFormat#format(String, Object...)}.
     *
     * @param pattern the pattern
     * @param objects the arguments for the pattern
     *
     * @return a formatted string
     */
    public static String format(String pattern, Object... arguments) {
            return MessageFormat.format(pattern, arguments);
    }

    /**
     * Returns the mnemonic for a message.
     *
     * @param message the message
     *
     * @return the mnemonic <code>int</code>
     */
    public static int getMnemonicInt(String message) {
        Integer integer = MNEMONIC_LOOKUP.get(message);
        if (integer != null) {
            return integer.intValue();
        }
        return 0;
    }

    /**
     * Initializes all public static non-final fields in the given class with
     * messages from a {@link ResourceBundle}.
     *
     * @param clazz the class containing the fields
     */
    public static void initializeMessages(Class<?> clazz, String rbName) {
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(rbName);
        } catch (MissingResourceException mre) {
            // fall through, handled later
        }
        for (Field field : clazz.getFields()) {
            if (isWritableField(field)) {
                String key = field.getName();
                String message = getMessage(rb, key);
                int mnemonicInt = findMnemonicInt(message);
                message = removeMnemonicAmpersand(message);
                message = replaceWithPlatformLineFeed(message);
                setFieldValue(field, message);
                MNEMONIC_LOOKUP.put(message, mnemonicInt);
            }
        }
    }

    private static boolean isWritableField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                && !Modifier.isFinal(modifiers);
    }

    /**
     * Returns the message corresponding to the key in the bundle or a text
     * describing it's missing.
     *
     * @param rb the resource bundle
     * @param key the key
     *
     * @return the message
     */
    private static String getMessage(ResourceBundle rb, String key) {
        if (rb == null) {
            return "missing resource bundle";
        }
        try {
            return rb.getString(key);
        } catch (MissingResourceException mre) {
            return "missing message for key = \"" + key
                    + "\" in resource bundle ";
        }
    }

    private static void setFieldValue(Field field, String value) {
        try {
            field.set(null, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new Error("Unable to access or set message for field " + field.getName());
        }
    }

    /**
     * Returns a {@link String} where all <code>\n</code> in the <text> have
     * been replaced with the line separator for the platform.
     *
     * @param text the to be replaced
     *
     * @return the replaced text
     */
    private static String replaceWithPlatformLineFeed(String text) {
        return text.replace("\n", System.getProperty("line.separator"));
    }

    /**
     * Removes the mnemonic identifier (<code>&</code>) from a string unless
     * it's escaped by <code>&&</code> or placed at the end.
     *
     * @param message the message
     *
     * @return a message with the mnemonic identifier removed
     */
    private static String removeMnemonicAmpersand(String message) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char current = message.charAt(i);
            if (current != '&' || i == message.length() - 1
                    || message.charAt(i + 1) == '&') {
                s.append(current);
            }
        }
        return s.toString();
    }

    /**
     * Finds the mnemonic character in a message.
     *
     * The mnemonic character is the first character followed by the first
     * <code>&</code> that is not followed by another <code>&</code>.
     *
     * @return the mnemonic as an <code>int</code>, or <code>0</code> if it
     *         can't be found.
     */
    private static int findMnemonicInt(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '&') {
                if (s.charAt(i + 1) != '&') {
                    return lookupMnemonicInt(s.substring(i + 1, i + 2));
                } else {
                    i++;
                }
            }
        }
        return 0;
    }

    /**
     * Lookups the mnemonic for a key in the {@link KeyEvent} class.
     *
     * @param c the character to lookup
     *
     * @return the mnemonic as an <code>int</code>, or <code>0</code> if it
     *         can't be found.
     */
    private static int lookupMnemonicInt(String c) {
        try {
            return KeyEvent.class.getDeclaredField("VK_" + c.toUpperCase())
                    .getInt(null);
        } catch (IllegalArgumentException | IllegalAccessException
                | NoSuchFieldException | SecurityException e) {
            // Missing VK is okay
            return 0;
        }
    }
}
