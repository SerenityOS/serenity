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
 * @bug 4429876
 * @run main GetChildNames
 * @summary Tests that the getChildNames method of
 * IIOMetadataFormatImpl returns null for a CHILD_POLICY_EMPTY node.
 */

import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.ImageTypeSpecifier;

public class GetChildNames {

    public static void main(String argv[]) {
        GCNFormatImpl fmt = new GCNFormatImpl("root", 1, 10);
        fmt.addElement("cc", "root", fmt.CHILD_POLICY_EMPTY);

        String[] result = fmt.getChildNames("cc");
        if (result != null) {
            throw new RuntimeException
                ("Failed, result is not null: " + result);
        }
    }
}

class GCNFormatImpl extends IIOMetadataFormatImpl {

    GCNFormatImpl(String root, int minChildren, int maxChildren) {
        super(root, minChildren, maxChildren);
    }

    public void addElement(String elementName,
                           String parentName, int childPolicy) {
        super.addElement(elementName, parentName, childPolicy);
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
        return true;
    }
}
