/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4458401
 * @summary Ensure that the Pipe.{sink,source}() methods don't create
 *          superfluous channel objects
 * @library ..
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;


public class NonBlocking {

    public static void main(String[] args) throws Exception {
        test1();
    }

    static void test1() throws Exception {
        Pipe p = Pipe.open();
        try {
            p.sink().configureBlocking(false);
            if (p.sink().isBlocking())
                throw new Exception("Sink still blocking");
            p.source().configureBlocking(false);
            if (p.source().isBlocking())
                throw new Exception("Source still blocking");
        } finally {
            p.sink().close();
            p.source().close();
        }
    }

}
