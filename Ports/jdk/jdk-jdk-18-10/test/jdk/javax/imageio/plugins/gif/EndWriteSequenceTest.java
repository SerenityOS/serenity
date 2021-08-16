/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6275366 6275369
 * @summary Verifies that behaviour of GIFImageWriter.endWriteSequence() is
 *          consistent with specification
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class EndWriteSequenceTest {
    public static void main(String[] args) throws IOException {
        ImageWriter w =
            ImageIO.getImageWritersByFormatName("GIF").next();

        boolean gotCorrectException = false;

        /**
         * check statement: "Throws: IllegalStateException -
         * if the output has not been set ...."
         */
        try {
            w.reset(); // to be shure that output is null
            w.endWriteSequence();
        } catch (IllegalStateException e) {
            gotCorrectException = true;
        } catch (Throwable e) {
            throw new RuntimeException("Test failed.", e);
        }
        if (!gotCorrectException) {
            throw new RuntimeException("Test failed.");
        }

        /**
         * set up output stream
         */
        ByteArrayOutputStream baos =
            new ByteArrayOutputStream();

        ImageOutputStream ios =
            ImageIO.createImageOutputStream(baos);

        w.setOutput(ios);

        /**
         * check statement: "Throws: IllegalStateException -
         * if .... prepareWriteSequence has not been called.
         */
        gotCorrectException = false;
        try {
            w.endWriteSequence();
        } catch  (IllegalStateException e) {
            gotCorrectException = true;
        } catch (Throwable e) {
            throw new RuntimeException("Test failed.", e);
        }
        if (!gotCorrectException) {
            throw new RuntimeException("Test failed.");
        }

        System.out.println("Test passed.");
    }
}
