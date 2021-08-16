/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * Default implementation of CharBindingMap interface. Provides a mapping for
 * the following symbols:<BR>
 *
 * @see org.netbeans.jemmy.CharBindingMap
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class DefaultCharBindingMap implements CharBindingMap {

    private Hashtable<Character, CharKey> chars;

    /**
     * Constructor.
     */
    public DefaultCharBindingMap() {
        initMap();
    }

    /**
     * Returns the code of the primary key used to type a symbol.
     *
     * @param c Symbol code.
     * @return a key code.
     * @see CharBindingMap#getCharKey(char)
     * @see java.awt.event.InputEvent
     */
    @Override
    public int getCharKey(char c) {
        return getKeyAndModifiers(c)[0];
    }

    /**
     * Returns the modifiers that should be pressed to type a symbol.
     *
     * @param c Symbol code.
     * @return a combination of InputEvent MASK fields.
     * @see CharBindingMap#getCharModifiers(char)
     * @see java.awt.event.InputEvent
     */
    @Override
    public int getCharModifiers(char c) {
        return getKeyAndModifiers(c)[1];
    }

    /**
     * Returns key + modifiers pair.
     *
     * @param c Symbol code.
     * @return an array of two elements: key code and modifiers mask - a
     * combination of InputEvent MASK fields.
     */
    public int[] getKeyAndModifiers(char c) {
        CharKey key = chars.get(c);
        if (key != null) {
            return new int[]{key.key, key.modifiers};
        } else {
            return new int[]{KeyEvent.VK_UNDEFINED, 0};
        }
    }

    /**
     * Returns an array of all supported chars.
     *
     * @return an array of chars representing the supported chars values.
     */
    public char[] getSupportedChars() {
        char[] charArray = new char[chars.size()];
        Enumeration<Character> keys = chars.keys();
        int index = 0;
        while (keys.hasMoreElements()) {
            charArray[index] = keys.nextElement();
        }
        return charArray;
    }

    /**
     * Removes a char from supported.
     *
     * @param c Symbol code.
     */
    public void removeChar(char c) {
        chars.remove(c);
    }

    /**
     * Adds a char to supported.
     *
     * @param c Symbol code.
     * @param key key code.
     * @param modifiers a combination of InputEvent MASK fields.
     */
    public void addChar(char c, int key, int modifiers) {
        chars.put(c, new CharKey(key, modifiers));
    }

    private void initMap() {
        chars = new Hashtable<>();
        //first add latters and digits represented by KeyEvent.VK_. fields
        Field[] fields = KeyEvent.class.getFields();
        for (Field field : fields) {
            String name = field.getName();
            if ((field.getModifiers() & Modifier.PUBLIC) != 0
                    && (field.getModifiers() & Modifier.STATIC) != 0
                    && field.getType() == Integer.TYPE
                    && name.startsWith("VK_")
                    && name.length() == 4) {
                String latter = name.substring(3, 4);
                try {
                    int key = field.getInt(null);
                    addChar(latter.toLowerCase().charAt(0), key, 0);
                    if (!latter.toUpperCase().equals(latter.toLowerCase())) {
                        addChar(latter.toUpperCase().charAt(0), key, InputEvent.SHIFT_MASK);
                    }
                } catch (IllegalAccessException e) {
                    throw new AssertionError("Never could happen!", e);
                }
            }
        }
        //add special simbols
        addChar('\t', KeyEvent.VK_TAB, 0);
        addChar(' ', KeyEvent.VK_SPACE, 0);
        addChar('!', KeyEvent.VK_1, InputEvent.SHIFT_MASK);
        addChar('"', KeyEvent.VK_QUOTE, InputEvent.SHIFT_MASK);
        addChar('#', KeyEvent.VK_3, InputEvent.SHIFT_MASK);
        addChar('$', KeyEvent.VK_4, InputEvent.SHIFT_MASK);
        addChar('%', KeyEvent.VK_5, InputEvent.SHIFT_MASK);
        addChar('&', KeyEvent.VK_7, InputEvent.SHIFT_MASK);
        addChar('\'', KeyEvent.VK_QUOTE, 0);
        addChar('(', KeyEvent.VK_9, InputEvent.SHIFT_MASK);
        addChar(')', KeyEvent.VK_0, InputEvent.SHIFT_MASK);
        addChar('*', KeyEvent.VK_8, InputEvent.SHIFT_MASK);
        addChar('+', KeyEvent.VK_EQUALS, InputEvent.SHIFT_MASK);
        addChar(',', KeyEvent.VK_COMMA, 0);
        addChar('-', KeyEvent.VK_MINUS, 0);
        addChar('.', KeyEvent.VK_PERIOD, 0);
        addChar('/', KeyEvent.VK_SLASH, 0);
        addChar(':', KeyEvent.VK_SEMICOLON, InputEvent.SHIFT_MASK);
        addChar(';', KeyEvent.VK_SEMICOLON, 0);
        addChar('<', KeyEvent.VK_COMMA, InputEvent.SHIFT_MASK);
        addChar('=', KeyEvent.VK_EQUALS, 0);
        addChar('>', KeyEvent.VK_PERIOD, InputEvent.SHIFT_MASK);
        addChar('?', KeyEvent.VK_SLASH, InputEvent.SHIFT_MASK);
        addChar('@', KeyEvent.VK_2, InputEvent.SHIFT_MASK);
        addChar('[', KeyEvent.VK_OPEN_BRACKET, 0);
        addChar('\\', KeyEvent.VK_BACK_SLASH, 0);
        addChar(']', KeyEvent.VK_CLOSE_BRACKET, 0);
        addChar('^', KeyEvent.VK_6, InputEvent.SHIFT_MASK);
        addChar('_', KeyEvent.VK_MINUS, InputEvent.SHIFT_MASK);
        addChar('`', KeyEvent.VK_BACK_QUOTE, 0);
        addChar('{', KeyEvent.VK_OPEN_BRACKET, InputEvent.SHIFT_MASK);
        addChar('|', KeyEvent.VK_BACK_SLASH, InputEvent.SHIFT_MASK);
        addChar('}', KeyEvent.VK_CLOSE_BRACKET, InputEvent.SHIFT_MASK);
        addChar('~', KeyEvent.VK_BACK_QUOTE, InputEvent.SHIFT_MASK);
        addChar('\n', KeyEvent.VK_ENTER, 0);
    }

    private static class CharKey {

        public int key;
        public int modifiers;

        public CharKey(int key, int modifiers) {
            this.key = key;
            this.modifiers = modifiers;
        }
    }

}
