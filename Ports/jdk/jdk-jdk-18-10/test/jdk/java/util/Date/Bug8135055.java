/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8135055
 * @modules java.sql
 * @summary Test java.sql.TimeStamp instance should come after java.util.Date
 * if Nanos component of TimeStamp is not equal to 0 milliseconds.
*/
import java.sql.Timestamp;
import java.util.Date;

public class Bug8135055 {

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            Date d = new Date();
            Timestamp ts = new Timestamp(d.getTime());
            if (d.after(ts)) {
                throw new RuntimeException("date with time " + d.getTime()
                        + " should not be after TimeStamp , Nanos component of "
                                + "TimeStamp is " +ts.getNanos());
            }
            Thread.sleep(1);
        }
    }
}
