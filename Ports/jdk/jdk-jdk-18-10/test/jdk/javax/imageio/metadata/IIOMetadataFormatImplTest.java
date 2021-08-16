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
 * @bug 4403350 4403352 4436995 4438977
 * @run main IIOMetadataFormatImplTest
 * @summary Tests various methods of IIOMetadataFormatImpl:
 *
 * getElement{Min,Max}Children and getAttribute{Min,Max}Value
 * getAttributeDescription
 * getAttributeEnumerations
 */

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class IIOMetadataFormatImplTest {

    public static void main(String[] args) {
        test440335x();
        test4436995();
        test4438977();
    }

    static class IIOMetadataFormatImpl440335x extends IIOMetadataFormatImpl {

        public IIOMetadataFormatImpl440335x() {
            super("rootNode", 0, 1);
            addElement("anElement", "rootNode", 20, 200);
            addAttribute("anElement", "exclusiveAttr",
                         IIOMetadataFormat.DATATYPE_INTEGER,
                         true, null,
                         "50", "500",
                         false, false);
            addAttribute("anElement", "minAttr",
                         IIOMetadataFormat.DATATYPE_INTEGER,
                         true, null,
                         "60", "600",
                         true, false);
            addAttribute("anElement", "maxAttr",
                         IIOMetadataFormat.DATATYPE_INTEGER,
                         true, null,
                         "70", "700",
                         false, true);
            addAttribute("anElement", "minMaxAttr",
                         IIOMetadataFormat.DATATYPE_INTEGER,
                         true, null,
                         "80", "800",
                         true, true);
        }

        public boolean canNodeAppear(String nodeName,
                                     ImageTypeSpecifier imageType) {
            return true;
        }
    }

    private static void test440335x() {
        IIOMetadataFormat format = new IIOMetadataFormatImpl440335x();

        // Check that correct value is returned
        if (format.getElementMinChildren("anElement") != 20) {
            throw new RuntimeException("Error on getElementMinChildren!");
        }
        if (format.getElementMaxChildren("anElement") != 200) {
            throw new RuntimeException("Error on getElementMaxChildren!");
        }

        // Check that correct value is returned and no exception is thrown
        try {
            if (!format.getAttributeMinValue("anElement",
                                             "exclusiveAttr").equals("50")) {
                throw new RuntimeException("Error on exclusiveAttr min!");
            }
            if (!format.getAttributeMaxValue("anElement",
                                             "exclusiveAttr").equals("500")) {
                throw new RuntimeException("Error on exclusiveAttr max!");
            }
            if (!format.getAttributeMinValue("anElement",
                                             "minAttr").equals("60")) {
                throw new RuntimeException("Error on minAttr min!");
            }
            if (!format.getAttributeMaxValue("anElement",
                                             "minAttr").equals("600")) {
                throw new RuntimeException("Error on minAttr max!");
            }
            if (!format.getAttributeMinValue("anElement",
                                             "maxAttr").equals("70")) {
                throw new RuntimeException("Error on maxAttr min!");
            }
            if (!format.getAttributeMaxValue("anElement",
                                             "maxAttr").equals("700")) {
                throw new RuntimeException("Error on maxAttr max!");
            }
            if (!format.getAttributeMinValue("anElement",
                                             "minMaxAttr").equals("80")) {
                throw new RuntimeException("Error on minMaxAttr min!");
            }
            if (!format.getAttributeMaxValue("anElement",
                                             "minMaxAttr").equals("800")) {
                throw new RuntimeException("Error on minMaxAttr max!");
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException("Got IllegalStateException!");
        }
    }

    static class IIOMetadataFormatImpl4436995 extends IIOMetadataFormatImpl {

        public IIOMetadataFormatImpl4436995(String root,
                                            int minChildren, int maxChildren) {
            super(root, minChildren, maxChildren);
        }

        public void addAttribute(String elementName,
                                 String attrName,
                                 int dataType,
                                 boolean required,
                                 int listMinLength, int listMaxLength) {
            super.addAttribute(elementName,
                               attrName,
                               dataType,
                               required, listMinLength,
                               listMaxLength);
        }

        public boolean canNodeAppear(String elementName,
                                     ImageTypeSpecifier imageType) {
            return true;
        }
    }

    private static void test4436995() {
        String result;

        IIOMetadataFormatImpl4436995 fmt =
            new IIOMetadataFormatImpl4436995("root", 1, 10);
        fmt.addAttribute("root", "attr", fmt.DATATYPE_INTEGER, true, 2, 5);
        try {
            result = fmt.getAttributeDescription("root", "non-existent", null);
            throw new RuntimeException("Failed to get IAE!");
        } catch(IllegalArgumentException e) {
        }
    }

    static class IIOMetadataFormatImpl4438977 extends IIOMetadataFormatImpl {

        public IIOMetadataFormatImpl4438977(String root,
                                            int minChildren, int maxChildren) {
            super(root, minChildren, maxChildren);
        }

        public void addAttribute(String elementName,
                                 String attrName,
                                 int dataType,
                                 boolean required,
                                 int listMinLength, int listMaxLength) {
            super.addAttribute(elementName,
                               attrName,
                               dataType,
                               required, listMinLength,
                               listMaxLength);
        }

        public boolean canNodeAppear(String elementName,
                                     ImageTypeSpecifier imageType) {
            return true;
        }
    }

    private static void test4438977() {
        String[] result;

        IIOMetadataFormatImpl4438977 fmt =
            new IIOMetadataFormatImpl4438977("root", 1, 10);
        fmt.addAttribute("root", "attr", fmt.DATATYPE_INTEGER, true, 2, 5);
        try {
            result = fmt.getAttributeEnumerations("root", "attr");
            throw new RuntimeException("Failed to get IAE!");
        } catch(IllegalArgumentException e) {
        }
    }

}
