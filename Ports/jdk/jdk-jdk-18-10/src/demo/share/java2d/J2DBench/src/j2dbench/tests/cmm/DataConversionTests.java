/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package j2dbench.tests.cmm;

import java.awt.color.ColorSpace;

import j2dbench.Group;
import j2dbench.Result;
import j2dbench.TestEnvironment;

public class DataConversionTests extends ColorConversionTests {

    protected static Group dataConvRoot;

    public static void init() {
        dataConvRoot = new Group(colorConvRoot, "data", "Data Conversoion Tests");

        new FromRGBTest();
        new ToRGBTest();
        new FromCIEXYZTest();
        new ToCIEXYZTest();
    }

    public DataConversionTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    protected static class Context {

        ColorSpace cs;
        int numComponents;
        float[] val;
        float[] rgb;
        float[] cie;
        TestEnvironment env;
        Result res;

        public Context(TestEnvironment env, Result result, ColorSpace cs) {
            this.cs = cs;
            this.env = env;
            this.res = result;

            numComponents = cs.getNumComponents();

            val = new float[numComponents];

            for (int i = 0; i < numComponents; i++) {
                float min = cs.getMinValue(i);
                float max = cs.getMaxValue(i);

                val[i] = 0.5f * (max - min);
            }

            rgb = new float[]{0.5f, 0.5f, 0.5f};
            cie = new float[]{0.5f, 0.5f, 0.5f};
        }
    }

    public Object initTest(TestEnvironment env, Result result) {
        ColorSpace cs = getColorSpace(env);
        return new Context(env, result, cs);
    }

    public void cleanupTest(TestEnvironment te, Object o) {
    }

    private static class FromRGBTest extends DataConversionTests {

        public FromRGBTest() {
            super(dataConvRoot,
                    "fromRGB",
                    "ColorSpace.fromRGB()");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ColorSpace cs = ictx.cs;

            final float[] rgb = ictx.rgb;
            do {
                try {
                    cs.fromRGB(rgb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class FromCIEXYZTest extends DataConversionTests {

        public FromCIEXYZTest() {
            super(dataConvRoot,
                    "fromCIEXYZ",
                    "ColorSpace.fromCIEXYZ()");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ColorSpace cs = ictx.cs;

            final float[] val = ictx.cie;
            do {
                try {
                    cs.fromCIEXYZ(val);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class ToCIEXYZTest extends DataConversionTests {

        public ToCIEXYZTest() {
            super(dataConvRoot,
                    "toCIEXYZ",
                    "ColorSpace.toCIEXYZ()");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ColorSpace cs = ictx.cs;

            final float[] val = ictx.val;

            do {
                try {
                    cs.toCIEXYZ(val);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class ToRGBTest extends DataConversionTests {

        public ToRGBTest() {
            super(dataConvRoot,
                    "toRGB",
                    "ColorSpace.toRGB()");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ColorSpace cs = ictx.cs;

            final float[] val = ictx.val;

            do {
                try {
                    cs.toRGB(val);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }
}
