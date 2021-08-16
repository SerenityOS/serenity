/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.beans.editors;

import java.beans.*;

public class StringEditor extends PropertyEditorSupport {

    public String getJavaInitializationString() {
        Object value = getValue();
        if (value == null)
            return "null";

        String str = value.toString();
        int length = str.length();
        StringBuilder sb = new StringBuilder(length + 2);
        sb.append('"');
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            switch (ch) {
            case '\b': sb.append("\\b");  break;
            case '\t': sb.append("\\t");  break;
            case '\n': sb.append("\\n");  break;
            case '\f': sb.append("\\f");  break;
            case '\r': sb.append("\\r");  break;
            case '\"': sb.append("\\\""); break;
            case '\\': sb.append("\\\\"); break;
            default:
                if ((ch < ' ') || (ch > '~')) {
                    sb.append("\\u");
                    String hex = Integer.toHexString((int) ch);
                    for (int len = hex.length(); len < 4; len++) {
                        sb.append('0');
                    }
                    sb.append(hex);
                } else {
                    sb.append(ch);
                }
                break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public void setAsText(String text) {
        setValue(text);
    }

}
