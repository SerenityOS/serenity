/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4496801 8151015
   @summary Tests HTMLDocument.insertBeforeEnd() method
   @author Peter Zhelezniakov
   @run main bug4496801
*/

import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.text.html.*;
import javax.swing.text.*;
import java.io.IOException;

public class bug4496801 {
    public static void main(String[] args) {
        HTMLDocument doc = new HTMLDocument();
        doc.setParser(new ParserDelegator());

        Element html = doc.getRootElements()[0];
        Element body = html.getElement(0);

        try {
            doc.insertBeforeEnd(body, "<h2>foo</h2>");
        } catch (IOException e) {
        } catch (BadLocationException e) {
            throw new RuntimeException("Insertion failed");
        }
    }
}
