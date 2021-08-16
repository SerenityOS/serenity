/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.text.*;

class StubBranchElement implements Element {
    Document document = new DefaultStyledDocument();
    String context;
    Element[] children = new StubLeafElement[3];

    public StubBranchElement(String context) {
        this.context = context;
        int len = context.length() / 3;
        for (int i = 0; i < 3; i++) {
            children[i] = new StubLeafElement(
                    context.substring(len * i, len * (i + 1)), this, len * i);
        }
        try {
            document.insertString(0, context, new SimpleAttributeSet());
        } catch (BadLocationException e) {
        }
    }

    public Document getDocument() {
        return document;
    }

    public Element getParentElement() {
        return null;
    }

    public String getName() {
        return "StubBranchElement";
    }

    public AttributeSet getAttributes() {
        return new SimpleAttributeSet();
    }

    public int getStartOffset() {
        return 0;
    }

    public int getEndOffset() {
        return document.getLength();
    }

    public int getElementIndex(int offset) {
        return offset / 3;
    }

    public int getElementCount() {
        return 3;
    }

    public Element getElement(int index) {
        return children[index];
    }

    public boolean isLeaf() {
        return false;
    }

    public Element[] getChildren() {
        return children;
    }
}
