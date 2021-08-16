/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.text.html.parser.ContentModel;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Element;

/*
 * @test
 * @bug 8074956
 * @author Alexey Ivanov
 * @summary Tests correct handling of additional HTML elements in ContentModel
 * @run main bug8074956
 */
public class bug8074956 {
    public static void main(String[] args) throws Exception {
        final DTD html32 = DTD.getDTD("html32");
        ContentModel contentModel = new ContentModel('&', new ContentModel());

        Element elem1 = html32.getElement("html-element");
        contentModel.first(elem1);

        Element elem2 = html32.getElement("test-element");
        // Shouldn't throw ArrayIndexOutOfBoundsException
        contentModel.first(elem2);
    }
}
