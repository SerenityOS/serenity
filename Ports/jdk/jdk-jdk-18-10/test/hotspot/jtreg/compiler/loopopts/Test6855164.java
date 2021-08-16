/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6855164
 * @summary SIGSEGV during compilation of method involving loop over CharSequence
 *
 * @run main/othervm -Xbatch compiler.loopopts.Test6855164
 */

package compiler.loopopts;

public class Test6855164 {
    public static void main(String[] args) throws Exception {
        StringBuffer builder = new StringBuffer();

        for(int i = 0; i < 100; i++)
            builder.append("I am the very model of a modern major general\n");

        for(int j = 0; j < builder.length(); j++){
            previousSpaceIndex(builder, j);
        }
    }

    private static final int previousSpaceIndex(CharSequence sb, int seek) {
        seek--;
        while (seek > 0) {
            if (sb.charAt(seek) == ' ') {
                while (seek > 0 && sb.charAt(seek - 1) == ' ')
                    seek--;
                return seek;
            }
            seek--;
        }
        return 0;
    }
}
