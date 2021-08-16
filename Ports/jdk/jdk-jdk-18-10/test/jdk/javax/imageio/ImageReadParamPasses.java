/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429365
 * @summary Checks that ImageReadParam.setSourceProgressivePasses handles
 *          overflow correctly
 */

import javax.imageio.ImageReadParam;

public class ImageReadParamPasses {

    private static final int maxint = Integer.MAX_VALUE;

    private static void expect(int i, int j) {
        if (i != j) {
            throw new RuntimeException("Expected " + i + ", got " + j);
        }
    }

    private static void checkForIAE(int minPass, int numPasses) {
        ImageReadParam param = new ImageReadParam();

        boolean gotIAE = false;
        try {
            param.setSourceProgressivePasses(minPass, numPasses);
        } catch (IllegalArgumentException iae) {
            gotIAE = true;
        }
        if (!gotIAE) {
            throw new RuntimeException("Failed to get IAE for wraparound!");
        }
    }

    private static void test(int minPass, int numPasses) {
        ImageReadParam param = new ImageReadParam();

        param.setSourceProgressivePasses(minPass, numPasses);
        expect(param.getSourceMinProgressivePass(), minPass);
        expect(param.getSourceNumProgressivePasses(), numPasses);

        int maxPass = numPasses == maxint ? maxint : minPass + numPasses - 1;
        expect(param.getSourceMaxProgressivePass(), maxPass);
    }

    public static void main(String[] args) {
        // Typical case
        test(17, 30);

        // Read all passes
        test(0, maxint);

        // Start at pass 17, continue indefinitely
        test(17, maxint);

        // Start at pass maxint - 10, continue indefinitely
        test(maxint - 10, maxint);

        // Start at pass maxint - 10, go up to maxint - 1
        test(maxint - 10, 10);

        // Start at pass maxint - 10, go up to maxint
        test(maxint - 10, 11);

        // Start at maxint, continue indefinitely :-)
        test(maxint, maxint);

        // Start at maxint, go up to maxint
        test(maxint, 1);

        // Check that an IllegalArgumentException is thrown if
        // wraparound occurs
        checkForIAE(maxint, 2);
        checkForIAE(maxint - 5, 10);
        checkForIAE(10, maxint - 5);
        checkForIAE(maxint - 1000, maxint - 1000);
        checkForIAE(maxint - 1, maxint - 1);
    }
}
