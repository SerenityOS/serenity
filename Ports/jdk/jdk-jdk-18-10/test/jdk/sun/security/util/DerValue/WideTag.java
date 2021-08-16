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

/*
 * @test
 * @bug 8264864
 * @summary Multiple byte tag not supported by ASN.1 encoding
 * @modules java.base/sun.security.util
 * @library /test/lib
 */

import jdk.test.lib.Utils;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import java.io.IOException;

public class WideTag {

    public static void main(String[] args) throws Exception {

        // Small ones
        DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)30);
        DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0);

        // Big ones
        Utils.runAndCheckException(
                () -> DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)31),
                IllegalArgumentException.class);
        Utils.runAndCheckException(
                () -> DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)222),
                IllegalArgumentException.class);

        // We don't accept number 31
        Utils.runAndCheckException(() -> new DerValue((byte)0xbf, new byte[10]),
                IllegalArgumentException.class);

        // CONTEXT [98] size 97. Not supported. Should fail.
        // Before this fix, it was interpreted as CONTEXT [31] size 98.
        byte[] wideDER = new byte[100];
        wideDER[0] = (byte)0xBF;
        wideDER[1] = (byte)98;
        wideDER[2] = (byte)97;

        Utils.runAndCheckException(() -> new DerValue(wideDER),
                IOException.class);
        Utils.runAndCheckException(() -> new DerInputStream(wideDER).getDerValue(),
                IOException.class);
    }
}
