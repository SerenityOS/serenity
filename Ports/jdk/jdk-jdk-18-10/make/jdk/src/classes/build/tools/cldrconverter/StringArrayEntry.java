/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.cldrconverter;

class StringArrayEntry extends Entry<String[]> {
    private String[] value;

    StringArrayEntry(String qName, Container parent, String key, int length) {
        super(qName, parent, key);
        value = new String[length];
    }

    void addCharacters(int index, char[] characters, int start, int length) {
        if (value[index] != null) {
            StringBuilder sb = new StringBuilder(value[index]);
            sb.append(characters, start, length);
            value[index] = sb.toString();
        } else {
            value[index] = new String(characters, start, length);
        }
    }

    @Override
    String[] getValue() {
        // This method patches up a few oddities:
        // - Since am/pm strings are nested directly under the calendar element,
        //   am/pm arrays may be created for which there was no real data.
        //   This test avoids returning empty arrays.
        // - On the other hand, for month names it's OK to not have month 13,
        //   but this should be indicated by an empty string for compatibility
        //   with JRE resource bundles.
        // - Finally, CLDR doesn't really have string arrays; each string is
        //   supposed to be inherited separately. Although value is the partially filled array,
        //   we will return it so that it can be reconstructed later in the method,
        //   ConvertLocaleData.convertBundles()
        //   The CLDR's iheritance system is different from JRE's. CLDR can inherit
        //   by the element level in the array.
        if (getKey().startsWith("Month") && value[0] != null && value[12] == null) {
            value[12] = "";
        }
        for (String element : value) {
            if (element != null) {
                return value;
            }
        }
        return null;
    }

}
