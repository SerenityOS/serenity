/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4323074
 * @summary Basic test for new Enumeration -> List converter
 */

import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class Enum {
    public static void main(String[] args) throws Exception {
        int[] sizes = {0, 10, 100};
        for (int i=0; i<sizes.length; i++) {
            Vector v = new Vector();
            int size = sizes[i];
            for (int j=0; j<size; j++)
                v.add(new Integer(j));
            List l = Collections.list(v.elements());
            if (!l.equals(v))
                throw new Exception("Copy failed: "+size);
        }
    }
}
