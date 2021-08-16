/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7040592
 * @summary Test that the assertion in State.forceStackTop does not fail at compile time.
 * @modules java.xml
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import org.w3c.dom.Element;

public class CoerceNullToMoreSpecificTypeTest {
    abstract class NodeImpl {
    }

    NodeImpl ownerNode;

    public Element getElement() {
        return (Element) (isOwned() ? ownerNode : null);
    }

    boolean isOwned() {
        return true;
    }

    static void processArrays(boolean expectNulls, Object [] nulla, Object [][] nullaa) {
        if (expectNulls) {
            if (nulla != null || nullaa != null) {
                throw new AssertionError("Null actual, but not null formal");
            }
        } else {
            if (nulla.length != 123 || nullaa.length != 321)
                throw new AssertionError("Wrong arrays received");
        }
    }

    public static void main(String[] args) {
        ArrayList<Class<?>> typeList = new ArrayList<>();
        Field rf = null;
        typeList.add((rf != null) ? rf.getType() : null);
        processArrays(true, null, null);
        processArrays(false, new Object[123], new Object[321][]);
    }
}
