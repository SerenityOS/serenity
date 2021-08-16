/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.DataFlavor;
import java.io.InputStream;

/**
 * @test
 * @bug 8259519
 * @summary InputStream should be used as a default data flavor representation
 */
public final class DefaultRepresentation extends InputStream {

    public static void main(String[] args) {
        DataFlavor df = new DataFlavor(DefaultRepresentation.class, "stream");
        if (df.getDefaultRepresentationClass() != InputStream.class) {
            // default representation class is not specified!
            // this check just defends against accidental changes
            throw new RuntimeException("InputStream class is expected");
        }
        if (!df.isRepresentationClassInputStream()) {
            throw new RuntimeException("true is expected");
        }
    }

    @Override
    public int read() {
        return 0;
    }
}
