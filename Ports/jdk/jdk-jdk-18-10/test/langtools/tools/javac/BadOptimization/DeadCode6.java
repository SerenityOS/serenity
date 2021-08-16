/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4073244
 * @summary nested classes:  Verify that overzealous dead-code elimination no
 * longer removes live code.
 * @author dps
 *
 * @modules java.desktop
 * @run clean DeadCode6
 * @run compile -O DeadCode6.java
 */

// This class will crash the compiler if the bug recurs.

import java.awt.*;
import java.awt.event.*;

public class DeadCode6 extends Window
{
    public DeadCode6( Frame parent ) {
        super( parent );
    }

    public void init() {
        final DeadCode6 wndThis = this;

        /*******************************************************************
         * add window listener
         ******************************************************************/
        addWindowListener( new WindowAdapter () {
            public void windowClosing( WindowEvent evt ) {
                wndThis.doCancelAction();
            }
        } );
    }

    public void doCancelAction() { }
}
