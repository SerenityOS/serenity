/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7011777
 * @summary Tests correct parsing of a HTML comment inside 'script' tags
 * @author Dmitry Markov
 */

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.StringReader;

public class bug7011777 {
    static String comment = "<!--\n" +
            "function foo() {\n" +
            "  var tag1 = \"</script>\";\n" +
            "  var tag2 = \"<div>\";\n" +
            "  var tag3 = \"</div>\";\n" +
            "  var tag4 = \"<script>\";\n" +
            "}\n" +
            "// -->";
    static String html = "<script>" + comment + "</script>";
    public static void main(String[] args) throws Exception {
            new ParserDelegator().parse(new StringReader(html), new MyParserCallback(), true);
    }

    static class MyParserCallback extends HTMLEditorKit.ParserCallback {

        @Override
        public void handleComment(char[] data, int pos) {
            String commentWithoutTags = comment.substring("<!--".length(), comment.length() - "-->".length());
            String str = new String(data);
            if (!commentWithoutTags.equals(str)) {
                System.out.println("Sample string:\n" + commentWithoutTags);
                System.out.println("Returned string:\n" + str);
                throw new RuntimeException("Test Failed, sample and returned strings are mismatched!");
            }
        }
    }

}
