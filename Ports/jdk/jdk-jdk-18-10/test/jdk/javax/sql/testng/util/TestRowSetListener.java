/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package util;

import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;

public class TestRowSetListener implements RowSetListener {

    // Flags to indicate which listener events should have been notified
    public final static int ROWSET_CHANGED = 1;
    public final static int ROW_CHANGED = 2;
    public final static int CURSOR_MOVED = 4;
    private int flag;

    @Override
    public void rowSetChanged(RowSetEvent event) {
        flag |= ROWSET_CHANGED;
    }

    @Override
    public void rowChanged(RowSetEvent event) {
        flag |= ROW_CHANGED;
    }

    @Override
    public void cursorMoved(RowSetEvent event) {
        flag |= CURSOR_MOVED;
    }

    /*
     * Clear the flag indicating which events we were notified for
     */
    public void resetFlag() {
        flag = 0;
    }

    /*
     *  Method used to validate that the correct event was notified
     */
    public boolean isNotified( int val) {
        return flag == val;
    }
}
