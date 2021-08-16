/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4432628 7186799
 * @run main RemoveElement
 * @summary Checks if ImageMetadataFormatImpl.removeElement properly
 * removes the element from its parent's child list.
 */

import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.ImageTypeSpecifier;

public class RemoveElement {

    public static void main(String[] args) {
        String elem = "elem2";
        int policy = IIOMetadataFormat.CHILD_POLICY_SOME;
        MyFormatImpl fmt = new MyFormatImpl("root", 1, 10);
        fmt.addElement("elem1", "root", policy);
        fmt.addElement(elem, "root", policy);
        fmt.removeElement("elem1");

        boolean gotIAE = false;
        try {
            fmt.getChildPolicy("elem1");
        } catch (IllegalArgumentException e) {
            gotIAE = true;
        }
        if (!gotIAE) {
            throw new RuntimeException("Element is still present!");
        }
        String[] chNames = fmt.getChildNames("root");
        if (chNames.length != 1) {
            throw new RuntimeException("Root still has more than 1 child!");
        }
        if (!elem.equals(chNames[0])) {
            throw new RuntimeException("Root's remaining child is incorrect!");
        }
    }

    static class MyFormatImpl extends IIOMetadataFormatImpl {

        MyFormatImpl(String root, int minChildren, int maxChildren) {
            super(root, minChildren, maxChildren);
        }

        public void addElement(String elementName,
                               String parentName,
                               int childPolicy) {
            super.addElement(elementName, parentName, childPolicy);
        }

        public void removeElement(String elementName) {
            super.removeElement(elementName);
        }

        public boolean canNodeAppear(String elementName,
                                     ImageTypeSpecifier imageType) {
            return true;
        }
    }

}
