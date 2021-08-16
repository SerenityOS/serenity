/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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


package j2dbench.tests;

import java.awt.Dimension;
import java.awt.Graphics;

import j2dbench.Group;
import j2dbench.Option;
import j2dbench.TestEnvironment;

public abstract class MiscTests extends GraphicsTests {
    static Group miscroot;
    static Group copytestroot;

    public MiscTests(Group parent, String nodeName, String description) {
        super(parent, nodeName, description);
    }

    public static void init() {
        miscroot = new Group(graphicsroot, "misc",
                             "Misc Benchmarks");
        copytestroot = new Group(miscroot, "copytests",
                                 "copyArea() Tests");

        new CopyArea("copyAreaVert", "Vertical copyArea()", 0, 1);
        new CopyArea("copyAreaHoriz", "Horizontal copyArea()", 1, 0);
        new CopyArea("copyAreaDiag", "Diagonal copyArea()", 1, 1);
    }

    private static class CopyArea extends MiscTests {
        private int dx, dy;

        CopyArea(String nodeName, String desc, int dx, int dy) {
            super(copytestroot, nodeName, desc);
            this.dx = dx;
            this.dy = dy;
        }

        public Dimension getOutputSize(int w, int h) {
            // we add one to each dimension to avoid copying outside the
            // bounds of the destination when "bounce" is enabled
            return new Dimension(w+1, h+1);
        }

        public void runTest(Object ctx, int numReps) {
            GraphicsTests.Context gctx = (GraphicsTests.Context)ctx;
            int size = gctx.size;
            int x = gctx.initX;
            int y = gctx.initY;
            Graphics g = gctx.graphics;
            g.translate(gctx.orgX, gctx.orgY);
            if (gctx.animate) {
                do {
                    g.copyArea(x, y, size, size, dx, dy);
                    if ((x -= 3) < 0) x += gctx.maxX;
                    if ((y -= 1) < 0) y += gctx.maxY;
                } while (--numReps > 0);
            } else {
                do {
                    g.copyArea(x, y, size, size, dx, dy);
                } while (--numReps > 0);
            }
            g.translate(-gctx.orgX, -gctx.orgY);
        }
    }
}
