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
   @bug 4096341
   @summary StringBuffer.getChars(): JLS requires exception if
            srcBegin > srcEnd
   @author Anand Palaniswamy
 */
public class GetCharsSrcEndLarger {
    public static void main(String[] args) throws Exception {
        boolean exceptionOccurred = false;
        try {
            new StringBuffer("abc").getChars(1, 0, new char[10], 0);
        } catch (StringIndexOutOfBoundsException sioobe) {
            exceptionOccurred = true;
        }
        if (!exceptionOccurred) {
            throw new Exception("StringBuffer.getChars() must throw" +
                                " an exception if srcBegin > srcEnd");
        }
    }
}
