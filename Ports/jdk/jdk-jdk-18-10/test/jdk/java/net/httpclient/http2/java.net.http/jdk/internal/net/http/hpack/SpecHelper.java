/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//
// THIS IS NOT A TEST
//
public final class SpecHelper {

    private SpecHelper() {
        throw new AssertionError();
    }

    public static ByteBuffer toBytes(String hexdump) {
        Pattern hexByte = Pattern.compile("[0-9a-fA-F]{2}");
        List<String> bytes = new ArrayList<>();
        Matcher matcher = hexByte.matcher(hexdump);
        while (matcher.find()) {
            bytes.add(matcher.group(0));
        }
        ByteBuffer result = ByteBuffer.allocate(bytes.size());
        for (String f : bytes) {
            result.put((byte) Integer.parseInt(f, 16));
        }
        result.flip();
        return result;
    }

    public static String toHexdump(ByteBuffer bb) {
        List<String> words = new ArrayList<>();
        int i = 0;
        while (bb.hasRemaining()) {
            if (i % 2 == 0) {
                words.add("");
            }
            byte b = bb.get();
            String hex = Integer.toHexString(256 + Byte.toUnsignedInt(b)).substring(1);
            words.set(i / 2, words.get(i / 2) + hex);
            i++;
        }
        return words.stream().collect(Collectors.joining(" "));
    }
}
