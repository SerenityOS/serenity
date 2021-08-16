/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.dtdbuilder;

import javax.swing.text.html.parser.*;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Hashtable;

/**
 * A class for mapping public identifiers to locations.
 * Note: I'm not sure if there is a more natural mapping
 * then this. Maybe we should use properties instead?
 *
 * @author      Arthur van Hoff
 */
final class PublicMapping {

    String baseStr;
    Hashtable<String, String> tab = new Hashtable<>();

    /**
     * Create a mapping.
     */
    public PublicMapping (String baseStr, String mapFile) throws IOException {
        // System.err.println("start loading " + baseStr);
        this.baseStr = baseStr;
        load(new FileInputStream(baseStr+mapFile));
        // System.err.println("stop loading");
    }

    /**
     * Load a set of mappings from a stream.
     */
    public void load(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader data = new BufferedReader(reader);

        for (String ln = data.readLine() ; ln != null ; ln = data.readLine()) {
            if (ln.startsWith("PUBLIC")) {
                int len = ln.length();
                int i = 6;
                while ((i < len) && (ln.charAt(i) != '"')) i++;
                int j = ++i;
                while ((j < len) && (ln.charAt(j) != '"')) j++;
                String id = ln.substring(i, j);
                i = ++j;
                while ((i < len) && ((ln.charAt(i) == ' ') || (ln.charAt(i) == '\t'))) i++;
                j = i + 1;
                while ((j < len) && (ln.charAt(j) != ' ') && (ln.charAt(j) != '\t')) j++;
                String where = ln.substring(i, j);
                put(id, baseStr + where);
            }
        }
        data.close();
    }

    /**
     * Add a mapping from a public identifier to a path.
     */
    public void put(String id, String str) {

        // System.err.println("ADD = '" + id + "' = " + str);
        tab.put(id, str);
        if (str.endsWith(".dtd")) {
            int i = str.lastIndexOf(File.separator);
            if (i >= 0) {
                tab.put(str.substring(i + 1, str.length() - 4), str);
            }
        }
    }

    /**
     * Map a public identifier to a path.. You can also map
     * a DTD file name (without the .dtd) to its path.
     */
    public String get(String id) {
        // System.err.println(" id = "+id);
        return tab.get(id);
    }
}
