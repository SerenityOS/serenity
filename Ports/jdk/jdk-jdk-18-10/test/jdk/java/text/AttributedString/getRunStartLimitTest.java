/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4151160
 * @summary Make sure to return correct run start and limit values
 * when the iterator has been created with begin and end index values.
 * @modules java.desktop
 */

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.Annotation;

public class getRunStartLimitTest {

    public static void main(String[] args) throws Exception {

        String text = "Hello world";
        AttributedString as = new AttributedString(text);

        // add non-Annotation attributes
        as.addAttribute(TextAttribute.WEIGHT,
                        TextAttribute.WEIGHT_LIGHT,
                        0,
                        3);
        as.addAttribute(TextAttribute.WEIGHT,
                        TextAttribute.WEIGHT_BOLD,
                        3,
                        5);
        as.addAttribute(TextAttribute.WEIGHT,
                        TextAttribute.WEIGHT_EXTRABOLD,
                        5,
                        text.length());

        // add Annotation attributes
        as.addAttribute(TextAttribute.WIDTH,
                        new Annotation(TextAttribute.WIDTH_EXTENDED),
                        0,
                        3);
        as.addAttribute(TextAttribute.WIDTH,
                        new Annotation(TextAttribute.WIDTH_CONDENSED),
                        3,
                        4);

        AttributedCharacterIterator aci = as.getIterator(null, 2, 4);

        aci.first();
        int runStart = aci.getRunStart();
        if (runStart != 2) {
            throw new Exception("1st run start is wrong. ("+runStart+" should be 2.)");
        }

        int runLimit = aci.getRunLimit();
        if (runLimit != 3) {
            throw new Exception("1st run limit is wrong. ("+runLimit+" should be 3.)");
        }

        Object value = aci.getAttribute(TextAttribute.WEIGHT);
        if (value != TextAttribute.WEIGHT_LIGHT) {
            throw new Exception("1st run attribute is wrong. ("
                                +value+" should be "+TextAttribute.WEIGHT_LIGHT+".)");
        }

        value = aci.getAttribute(TextAttribute.WIDTH);
        if (value != null) {
            throw new Exception("1st run annotation is wrong. ("
                                +value+" should be null.)");
        }

        aci.setIndex(runLimit);
        runStart = aci.getRunStart();
        if (runStart != 3) {
            throw new Exception("2nd run start is wrong. ("+runStart+" should be 3.)");
        }

        runLimit = aci.getRunLimit();
        if (runLimit != 4) {
            throw new Exception("2nd run limit is wrong. ("+runLimit+" should be 4.)");
        }
        value = aci.getAttribute(TextAttribute.WEIGHT);
        if (value != TextAttribute.WEIGHT_BOLD) {
            throw new Exception("2nd run attribute is wrong. ("
                                +value+" should be "+TextAttribute.WEIGHT_BOLD+".)");
        }

        value = aci.getAttribute(TextAttribute.WIDTH);
        if (!(value instanceof Annotation)
            || (((Annotation)value).getValue() !=  TextAttribute.WIDTH_CONDENSED)) {
            throw new Exception("2nd run annotation is wrong. (" + value + " should be "
                                + new Annotation(TextAttribute.WIDTH_CONDENSED)+".)");
        }
    }
}
