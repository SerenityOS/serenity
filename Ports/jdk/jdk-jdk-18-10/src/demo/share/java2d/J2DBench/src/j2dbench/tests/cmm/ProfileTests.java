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

import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;

import j2dbench.Group;
import j2dbench.Result;
import j2dbench.TestEnvironment;

public class ProfileTests extends CMMTests {

    protected static Group profileRoot;

    public static void init() {
        profileRoot = new Group(cmmRoot, "profiles", "Profile Handling Benchmarks");

        new ReadHeaderTest();
        new GetNumComponentsTest();
    }

    protected ProfileTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    protected static class Context {

        ICC_Profile profile;
        TestEnvironment env;
        Result res;

        public Context(ICC_Profile profile, TestEnvironment env, Result res) {
            this.profile = profile;
            this.env = env;
            this.res = res;
        }
    }

    public Object initTest(TestEnvironment env, Result res) {
        ICC_ColorSpace cs = (ICC_ColorSpace) getColorSpace(env);
        return new Context(cs.getProfile(), env, res);
    }

    public void cleanupTest(TestEnvironment env, Object o) {
    }

    private static class ReadHeaderTest extends ProfileTests {

        public ReadHeaderTest() {
            super(profileRoot,
                    "getHeader",
                    "getData(icSigHead)");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ICC_Profile profile = ictx.profile;

            byte[] data = null;
            do {
                try {
                    data = profile.getData(ICC_Profile.icSigHead);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }

    private static class GetNumComponentsTest extends ProfileTests {

        public GetNumComponentsTest() {
            super(profileRoot,
                    "getNumComponents",
                    "getNumComponents");
        }

        public void runTest(Object ctx, int numReps) {
            final Context ictx = (Context) ctx;
            final ICC_Profile profile = ictx.profile;

            do {
                try {
                    int num = profile.getNumComponents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (--numReps >= 0);
        }
    }
}
