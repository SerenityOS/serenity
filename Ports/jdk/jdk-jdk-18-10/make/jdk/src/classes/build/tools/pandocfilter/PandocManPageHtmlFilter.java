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
import build.tools.pandocfilter.json.JSONObject;
import build.tools.pandocfilter.json.JSONValue;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PandocManPageHtmlFilter extends PandocFilter {

    private JSONValue MetaInlines(JSONValue value) {
        return createPandocNode("MetaInlines", value);
    }

    private JSONValue changeTitle(String type, JSONValue value) {
        if (type.equals("MetaInlines")) {
            String subType = value.get(0).get("t").asString();
            String subContent = value.get(0).get("c").asString();
            if (subType.equals("Str")) {
                Pattern pattern = Pattern.compile("^([A-Z0-9]+)\\([0-9]+\\)$");
                Matcher matcher = pattern.matcher(subContent);
                if (matcher.find()) {
                    String commandName = matcher.group(1).toLowerCase();
                    return MetaInlines(new JSONArray(
                            createStr("The"), createSpace(),
                            createStr(commandName),
                            createSpace(), createStr("Command")));
                }
            }
        }
        return null;
    }

    /**
     * Main function
     */
    public static void main(String[] args) throws FileNotFoundException {
        JSONValue json = loadJson(args);

        PandocManPageHtmlFilter filter = new PandocManPageHtmlFilter();

        JSONValue meta = json.get("meta");
        if (meta != null && meta instanceof JSONObject) {
            JSONObject metaobj = (JSONObject) meta;
            metaobj.remove("date");
            JSONValue title = meta.get("title");
            if (title != null) {
                metaobj.put("title", filter.traverse(title, filter::changeTitle, true));
            }
        }

        System.out.println(json);
    }
}
