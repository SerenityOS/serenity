/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4015830
   @summary Ensure that FilterOutputStream.write(byte[], int, int) calls the
            one-argument write method in the same class
 */

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;


public class Write {

    static class F extends FilterOutputStream {

        public F(OutputStream o) {
            super(o);
        }

        public void write(int b) {
            System.err.println("Ignoring write of " + b);
        }

    }

    static class Sink extends OutputStream {

        public void write(int b) {
            throw new RuntimeException("Filter stream directly invoked"
                                       + " write(int) method of underlying"
                                       + " stream");
        }

    }

    public static void main(String[] args) throws Exception {
        OutputStream f = new F(new Sink());
        f.write(new byte[] { 1, 2, 3 }, 0, 3);
    }

}
