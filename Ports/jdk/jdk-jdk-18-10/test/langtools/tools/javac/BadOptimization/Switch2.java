/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4099995 4118025
 * @summary Bad optimization of "switch" statement
 * @author dps
 *
 * @run clean Switch2
 * @run compile -O Switch2.java
 * @run main Switch2
 */

public class Switch2
{
    private boolean isZero( int d ) {
        switch (d) {
        case 0:
            return true;
        default:
            return false;
        }
    }

    void useSecondArgument( String a, int b ) {
        if ( !isZero( b ) )
            throw new RuntimeException("isZero(0) returns false?!");
    }

    public static void main( String[] args ) {
        Switch2 object = new Switch2();
        object.useSecondArgument( "42", 0 );
    }
}
