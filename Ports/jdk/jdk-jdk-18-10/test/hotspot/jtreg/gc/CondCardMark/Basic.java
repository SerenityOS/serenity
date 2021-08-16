/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.CondCardMark;

/**
 * @test
 * @bug 8076987
 * @bug 8078438
 * @summary Verify UseCondCardMark works
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -Xint gc.CondCardMark.Basic
 * @run main/othervm -Xint -XX:+UseCondCardMark gc.CondCardMark.Basic
 * @run main/othervm -XX:TieredStopAtLevel=1 gc.CondCardMark.Basic
 * @run main/othervm -XX:TieredStopAtLevel=1 -XX:+UseCondCardMark gc.CondCardMark.Basic
 * @run main/othervm -XX:TieredStopAtLevel=4 gc.CondCardMark.Basic
 * @run main/othervm -XX:TieredStopAtLevel=4 -XX:+UseCondCardMark gc.CondCardMark.Basic
 * @run main/othervm -XX:-TieredCompilation gc.CondCardMark.Basic
 * @run main/othervm -XX:-TieredCompilation -XX:+UseCondCardMark gc.CondCardMark.Basic
*/
public class Basic {

    static volatile MyObject sink;

    public static void main(String args[]) {
        final int COUNT = 10000000;
        for (int c = 0; c < COUNT; c++) {
             MyObject o = new MyObject();
             o.x = c;
             doStore(o);
        }

        if (sink.x != COUNT-1) {
             throw new IllegalStateException("Failed");
        }
    }

    public static void doStore(MyObject o) {
        sink = o;
    }

    static class MyObject {
        int x;
    }

}
