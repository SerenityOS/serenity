/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8139192
 * @summary Test to check OffScreenImageSource handles
 *          ImageFilter.imageComplete() behaviors
 * @run main ImageFilterTest
 */

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;


public class ImageFilterTest {

    public static void main(String[] args) {
        String[] scenarios = {
            "SUCCESS",
            "SINGLEFRAMEDONE exception",
            "STATICIMAGEDONE exception"
        };

        for (String str : scenarios) {
            MyImageFilter testFilter = new MyImageFilter(str);
            test(testFilter);
        }
    }

    public static void test(MyImageFilter testFilter) {
        Image image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        FilteredImageSource filtered =
                new FilteredImageSource(image.getSource(), testFilter);

        Image img = Toolkit.getDefaultToolkit().createImage(filtered);

        BufferedImage buffImage = new BufferedImage(img.getWidth(null),
                img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    }
}

class MyImageFilter extends ImageFilter {

    private String testScenario;
    private boolean intermediateStatus;

    public MyImageFilter(String scenario) {
        super();

        testScenario = scenario;
        intermediateStatus = false;
    }

    @Override
    public void imageComplete(int status) {
        switch (testScenario) {
            case "SUCCESS":
                if (status == SINGLEFRAMEDONE) {
                    intermediateStatus = true;
                }

                if (status == STATICIMAGEDONE) {
                    if (!intermediateStatus) {
                        throw new RuntimeException("STATICIMAGEDONE is not expected");
                    }
                }

                if (status == IMAGEERROR) {
                    throw new RuntimeException("IMAGEERROR is not expected");
                }
                break;

            case "SINGLEFRAMEDONE exception":
                if (status == SINGLEFRAMEDONE) {
                    intermediateStatus = true;

                    throw new NullPointerException("NullPointerException for testing purpose");
                }

                if (status == IMAGEERROR) {
                    if (!intermediateStatus) {
                        throw new RuntimeException("IMAGEERROR is not expected");
                    }
                }

                if (status == STATICIMAGEDONE) {
                    throw new RuntimeException("STATICIMAGEDONE is not expected");
                }
                break;

            case "STATICIMAGEDONE exception":
                if (status == SINGLEFRAMEDONE) {
                    intermediateStatus = true;
                }

                if (status == STATICIMAGEDONE) {
                    if (intermediateStatus) {
                        throw new RuntimeException("RuntimeException for testing purpose");
                    }
                }

                if (status == IMAGEERROR) {
                    throw new RuntimeException("IMAGEERROR is not expected");
                }
                break;

            default:
                throw new RuntimeException("Invalid Test Scenario");
        }
    }
}


