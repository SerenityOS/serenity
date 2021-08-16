/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * <code>TextAndMnemonicUtils</code> allows to extract text and mnemonic values
 * from the unified text & mnemonic strings. For example:
 *   LafMenu.laf.labelAndMnemonic=&Look && Feel
 * The extracted text is "Look & Feel" and the extracted mnemonic mnemonic is "L".
 *
 * There are several patterns for the text and mnemonic suffixes which are used
 * in the resource file. The patterns format is:
 * (resource key -> unified text & mnemonic resource key).
 *
 * Keys that have label suffixes:
 * (xxx_label -> xxx.labelAndMnemonic)
 *
 * Keys that have mnemonic suffixes:
 * (xxx_mnemonic -> xxx.labelAndMnemonic)
 *
 * Keys that do not have definite suffixes:
 * (xxx -> xxx.labelAndMnemonic)
 *
 * @author Alexander Scherbatiy
 */
public class TextAndMnemonicUtils {

    // Label suffix for the text & mnemonic resource
    private static final String LABEL_SUFFIX = ".labelAndMnemonic";

    // Resource bundle for internationalized and accessible text
    private static ResourceBundle bundle = null;

    // Resource properties for the mnemonic key defenition
    private static Properties properties = null;

    static {
        bundle = ResourceBundle.getBundle("resources.swingset");
        properties = new Properties();
        try {
            properties.load(TextAndMnemonicUtils.class.getResourceAsStream("resources/swingset.properties"));
        } catch (IOException ex) {
            System.out.println("java.io.IOException: Couldn't load properties from: resources/swingset.properties");
        }
    }

    /**
     * Returns accessible and internationalized strings or mnemonics from the
     * resource bundle. The key is converted to the text & mnemonic key.
     *
     * The following patterns are checked:
     * Keys that have label suffixes:
     * (xxx_label -> xxx.labelAndMnemonic)
     *
     * Keys that have mnemonic suffixes:
     * (xxx_mnemonic -> xxx.labelAndMnemonic)
     *
     * Keys that do not have definite suffixes:
     * (xxx -> xxx.labelAndMnemonic)
     *
     * Properties class is used to check if a key created for mnemonic exists.
     */
    public static String getTextAndMnemonicString(String key) {

        if (key.endsWith("_label")) {
            String compositeKey = composeKey(key, 6, LABEL_SUFFIX);
            String textAndMnemonic = bundle.getString(compositeKey);
            return getTextFromTextAndMnemonic(textAndMnemonic);
        }

        if (key.endsWith("_mnemonic")) {

            String compositeKey = composeKey(key, 9, LABEL_SUFFIX);
            Object value = properties.getProperty(compositeKey);

            if (value != null) {
                String textAndMnemonic = bundle.getString(compositeKey);
                return getMnemonicFromTextAndMnemonic(textAndMnemonic);
            }

        }

        String compositeKey = composeKey(key, 0, LABEL_SUFFIX);
        Object value = properties.getProperty(compositeKey);

        if (value != null) {
            String textAndMnemonic = bundle.getString(compositeKey);
            return getTextFromTextAndMnemonic(textAndMnemonic);
        }

        String textAndMnemonic = bundle.getString(key);
        return getTextFromTextAndMnemonic(textAndMnemonic);
    }

    /**
     * Convert the text & mnemonic string to text string
     *
     * The '&' symbol is treated as the mnemonic pointer
     * The double "&&" symbols are treated as the single '&'
     *
     * For example the string "&Look && Feel" is converted to "Look & Feel"
     */
    public static String getTextFromTextAndMnemonic(String text) {

        StringBuilder sb = new StringBuilder();

        int prevIndex = 0;
        int nextIndex = text.indexOf('&');
        int len = text.length();

        while (nextIndex != -1) {

            String s = text.substring(prevIndex, nextIndex);
            sb.append(s);

            nextIndex++;

            if (nextIndex != len && text.charAt(nextIndex) == '&') {
                sb.append('&');
                nextIndex++;
            }

            prevIndex = nextIndex;
            nextIndex = text.indexOf('&', nextIndex + 1);
        }

        sb.append(text.substring(prevIndex, text.length()));
        return sb.toString();
    }

    /**
     * Convert the text & mnemonic string to mnemonic
     *
     * The '&' symbol is treated the mnemonic pointer
     * The double "&&" symbols are treated as the single '&'
     *
     * For example the string "&Look && Feel" is converted to "L"
     */
    public static String getMnemonicFromTextAndMnemonic(String text) {
        int index = text.indexOf('&');

        while (0 <= index && index < text.length() - 1) {
            index++;
            if (text.charAt(index) == '&') {
                index = text.indexOf('&', index + 1);
            } else {
                char c = text.charAt(index);
                return String.valueOf(Character.toUpperCase(c));
            }
        }

        return null;
    }

    /**
     * Removes the last n characters and adds the suffix
     */
    private static String composeKey(String key, int reduce, String sufix) {
        return key.substring(0, key.length() - reduce) + sufix;
    }
}