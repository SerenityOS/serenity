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

package build.tools.pandocfilter;

import build.tools.pandocfilter.json.JSONArray;
import build.tools.pandocfilter.json.JSONValue;

import java.io.FileNotFoundException;

public class PandocManPageTroffFilter extends PandocFilter {

    private JSONValue createStrong(JSONValue value) {
        return createPandocNode("Strong", value);
    }

    private JSONValue createHeader(JSONValue value) {
        return createPandocNode("Header", value);
    }

    /**
     * Callback to change all Str texts to upper case
     */
    private JSONValue uppercase(String type, JSONValue value) {
        if (type.equals("Str")) {
            return createStr(value.asString().toUpperCase());
        }
        return null;
    }

    /**
     * Main callback function that performs our man page AST rewrites
     */
    private JSONValue manpageFilter(String type, JSONValue value) {
        // If it is a header, decrease the heading level by one, and
        // if it is a level 1 header, convert it to upper case.
        if (type.equals("Header")) {
            JSONArray array = value.asArray();
            int level = array.get(0).asInt();
            array.set(0, JSONValue.from(level - 1));
            if (value.asArray().get(0).asInt() == 1) {
                return createHeader(traverse(value, this::uppercase, false));
            }
        }

        // Man pages does not have superscript. We use it for footnotes, so
        // enclose in [...] for best representation.
        if (type.equals("Superscript")) {
            return new JSONArray(createStr("["), value, createStr("]"));
        }

        // If it is a link, put the link name in bold. If it is an external
        // link, put it in brackets. Otherwise, it is either an internal link
        // (like "#next-heading"), or a relative link to another man page
        // (like "java.html"), so remove it for man pages.
        if (type.equals("Link")) {
            JSONValue target = value.asArray().get(2).asArray().get(0);
            String targetStr = target.asString();
            if (targetStr.startsWith("https:") || targetStr.startsWith("http:")) {
                return new JSONArray(createStrong(value.asArray().get(1)), createSpace(), createStr("[" + targetStr + "]"));
            } else {
                return createStrong(value.asArray().get(1));
            }
        }

        return null;
    }

    /**
     * Main function
     */
    public static void main(String[] args) throws FileNotFoundException {
        JSONValue json = loadJson(args);
        build.tools.pandocfilter.PandocManPageTroffFilter filter = new build.tools.pandocfilter.PandocManPageTroffFilter();

        JSONValue transformed_json = filter.traverse(json, filter::manpageFilter, false);

        System.out.println(transformed_json);
    }
}
