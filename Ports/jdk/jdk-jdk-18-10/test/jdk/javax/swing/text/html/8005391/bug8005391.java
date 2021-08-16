/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * @test
 * @bug 8005391
 * @author Alexander Shusherov
 * @summary Floating behavior of HTMLEditorKit parser
 * @run main bug8005391
 */
public class bug8005391 {

    private static final String htmlDoc = "<html><body><tt><a href='one'>1</a>2</tt></body></html>";

    public static void main(String[] args) throws Exception {
        int N = 10;

        for (int i = 0; i < N; i++) {
            HTMLEditorKit kit = new HTMLEditorKit();
            Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
            HTMLEditorKit.Parser parser = (HTMLEditorKit.Parser) c.newInstance();
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            HTMLEditorKit.ParserCallback htmlReader = doc.getReader(0);
            parser.parse(new CharArrayReader(htmlDoc.toCharArray()), htmlReader, true);
            htmlReader.flush();
            CharArrayWriter writer = new CharArrayWriter(1000);
            kit.write(writer, doc, 0, doc.getLength());
            writer.flush();

            String result = writer.toString();
            if (!result.contains("<tt><a")) {
                throw new RuntimeException("The <a> and <tt> tags are swapped");
            }
        }
    }
}
