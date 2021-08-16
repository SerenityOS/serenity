/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jndi.toolkit.dir.HierMemDirCtx;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

/*
 * This class is responsible for creating a temporary namespace to be
 * used in federation tests.
 */
public class FedSubordinateNs {
    static DirContext root = null;

    static {
        try {
            Attributes rootAttrs = new BasicAttributes("name", "root");
            rootAttrs.put("description", "in-memory root");

            Attributes aAttrs = new BasicAttributes("name", "a");
            aAttrs.put("description", "in-memory 1st level");

            Attributes bAttrs = new BasicAttributes("name", "b");
            bAttrs.put("description", "in-memory 2nd level");

            Attributes cAttrs = new BasicAttributes("name", "c");
            cAttrs.put("description", "in-memory 3rd level");

            root = new HierMemDirCtx();
            root.modifyAttributes("", DirContext.ADD_ATTRIBUTE, rootAttrs);

            root.createSubcontext("a", aAttrs);
            root.createSubcontext("a/b", bAttrs);
            root.createSubcontext("a/b/c", cAttrs);
            root.createSubcontext("x");
        } catch (NamingException e) {
            System.out.println("Problem in static initialization of root:" + e);
        }
    }

    static DirContext getRoot() {
        return root;
    }
}
