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
 * @bug 4395378
 * @summary Checks that ImageIO.createImageInputStream and
 *          createImageOutputStream produce correct output when given a
 *          RandomAccessFile
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class ImageStreamFromRAF {

    public static void main(String[] args) {
        try {
            File f = new File("ImageInputStreamFromRAF.tmp");
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            ImageInputStream istream = ImageIO.createImageInputStream(raf);
            ImageOutputStream ostream = ImageIO.createImageOutputStream(raf);
            f.delete();
            if (istream == null) {
                throw new RuntimeException("ImageIO.createImageInputStream(RandomAccessFile) returned null!");
            }
            if (ostream == null) {
                throw new RuntimeException("ImageIO.createImageOutputStream(RandomAccessFile) returned null!");
            }
            if (!(istream instanceof FileImageInputStream)) {
                throw new RuntimeException("ImageIO.createImageInputStream(RandomAccessFile) did not return a FileImageInputStream!");
            }
            if (!(ostream instanceof FileImageOutputStream)) {
                throw new RuntimeException("ImageIO.createImageOutputStream(RandomAccessFile) did not return a FileImageOutputStream!");
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected IOException: " + ioe);
        }
    }
}
