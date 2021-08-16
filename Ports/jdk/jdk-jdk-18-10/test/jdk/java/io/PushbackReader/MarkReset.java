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
   @bug 4149941
   @summary mark and reset should throw an exception even when the
   underlying stream supports the operations.

*/


import java.io.*;

public class MarkReset {

    public static void main(String args[]) throws Exception {
        CharArrayReader car = new CharArrayReader(new char[32]);
        PushbackReader pb = new PushbackReader(car);
        boolean markResult = testMark(pb);
        boolean resetResult = testReset(pb);
        if ((!markResult) || (!resetResult))
            throw new Exception("Mark and reset should not be supported");
    }

    static boolean testMark(PushbackReader pb){
        try{
            pb.mark(100);
        } catch (IOException e) {
            return true;            //  Passed the test successfully
        }
        System.err.println("Mark error");
        return false;           // Failed
    }

    static boolean testReset(PushbackReader pb){
        try{
            pb.reset();
        } catch (IOException e) {
            return true;            //  Passed the test successfully
        }
        System.err.println("Reset error");
        return false;           // Failed
    }

}
