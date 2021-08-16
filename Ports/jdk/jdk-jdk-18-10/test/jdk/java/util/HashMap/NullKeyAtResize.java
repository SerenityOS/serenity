/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7173432
 * @summary If the key to be inserted into a HashMap is null and the table
 * needs to be resized as part of the insertion then addEntry will try to
 * recalculate the hash of a null key. This will fail with an NPE.
 */

import java.util.*;

public class NullKeyAtResize {
    public static void main(String[] args) throws Exception {
        List<Object> old_order = new ArrayList<>();
        Map<Object,Object> m = new HashMap<>(16);
        int number = 0;
        while(number < 100000) {
            m.put(null,null); // try to put in null. This may cause resize.
            m.remove(null); // remove it.
            Integer adding = (number += 100);
            m.put(adding, null); // try to put in a number. This wont cause resize.
            List<Object> new_order = new ArrayList<>();
            new_order.addAll(m.keySet());
            new_order.remove(adding);
            if(!old_order.equals(new_order)) {
                // we resized and didn't crash.
                System.out.println("Encountered resize after " + (number / 100) + " iterations");
                break;
            }
            // remember this order for the next time around.
            old_order.clear();
            old_order.addAll(m.keySet());
        }
        if(number == 100000) {
            throw new Error("Resize never occurred");
        }
    }
}
