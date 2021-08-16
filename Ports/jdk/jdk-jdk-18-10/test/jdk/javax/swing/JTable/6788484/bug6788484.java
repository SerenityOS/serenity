/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6788484
   @summary NPE in DefaultTableCellHeaderRenderer.getColumnSortOrder() with null table
   @modules java.desktop/sun.swing.table
   @compile -XDignore.symbol.file=true bug6788484.java
   @author Alexander Potochkin
   @run main bug6788484
*/

/*
 * Compile with -XDignore.symbol.file=true option as a workaround for
 * specific behaviour described in 6380059 which restricts proprietary
 * package loading
 */

import sun.swing.table.DefaultTableCellHeaderRenderer;

import javax.swing.*;

public class bug6788484 {

    public static void main(String[] args) throws Exception {
        DefaultTableCellHeaderRenderer.getColumnSortOrder(null, 0);
    }
}
