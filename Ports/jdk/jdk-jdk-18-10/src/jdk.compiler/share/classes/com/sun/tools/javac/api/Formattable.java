/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.util.Locale;

/**
 * This interface must be implemented by any javac class that has non-trivial
 * formatting needs (e.g. where toString() does not apply because of localization).
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @author Maurizio Cimadamore
 */
public interface Formattable {

    /**
     * Used to obtain a localized String representing the object accordingly
     * to a given locale
     *
     * @param locale locale in which the object's representation is to be rendered
     * @param messages messages object used for localization
     * @return a locale-dependent string representing the object
     */
    public String toString(Locale locale, Messages messages);
    /**
     * Retrieve a pretty name of this object's kind
     * @return a string representing the object's kind
     */
    String getKind();

    static class LocalizedString implements Formattable {
        String key;

        public LocalizedString(String key) {
            this.key = key;
        }

        public String toString(java.util.Locale l, Messages messages) {
            return messages.getLocalizedString(l, key);
        }
        public String getKind() {
            return "LocalizedString";
        }

        public String toString() {
            return key;
        }
    }
}
