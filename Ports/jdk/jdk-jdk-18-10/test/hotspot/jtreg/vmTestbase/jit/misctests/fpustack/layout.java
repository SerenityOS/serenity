/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jit.misctests.fpustack;

import java.util.*;
import java.awt.*;
import java.applet.Applet;
import nsk.share.TestFailure;

public class layout implements ilayout {
    public  void formatNodes( Node[] nodes, Dimension d, FontMetrics fm )   {
        int h = d.height/2 - 10 ;

        double alpha = -Math.PI/2;
        for ( int j = 0; j < nodes.length; j++) {
            Node n = nodes[j];
            int w = d.width/2 - fm.stringWidth( n.lbl )/2;

            n.x = d.width/2 + (int)(w*Math.cos( alpha ));

            n.y = d.height/2 + (int)(h*Math.sin( alpha ));
            alpha += 2*Math.PI/nodes.length;
        }
    }

}
