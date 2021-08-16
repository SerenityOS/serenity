/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package build.tools.pandocfilter.json;

public class JSON {
    public static JSONValue parse(String s) {
        return new JSONParser().parse(s);
    }

    public static JSONValue of(int i) {
        return JSONValue.from(i);
    }

    public static JSONValue of(long l) {
        return JSONValue.from(l);
    }

    public static JSONValue of(double d) {
        return JSONValue.from(d);
    }

    public static JSONValue of(boolean b) {
        return JSONValue.from(b);
    }

    public static JSONValue of(String s) {
        return JSONValue.from(s);
    }

    public static JSONValue of() {
        return JSONValue.fromNull();
    }

    public static JSONArray array() {
        return new JSONArray();
    }

    public static JSONObject object() {
        return new JSONObject();
    }
}
