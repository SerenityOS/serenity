/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6860438
   @summary Tests various MultiUIDefaults methods
   @author Peter Zhelezniakov
   @run main Test6860438
*/

import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.UIManager;

public class Test6860438
{
    static final String KEY = "Test6860438.key";
    static final String VALUE = "Test6860438.value";

    void check(Object key, Object value, boolean present, int size) {
        check(UIManager.get(key) == value, "UIManager.get()");
        check(UIManager.getDefaults().size() == size, "MultiUIDefaults.size()");

        checkEnumeration(UIManager.getDefaults().keys(),
                key, present, "MultiUIDefaults.keys()");
        checkEnumeration(UIManager.getDefaults().elements(),
                value, present, "MultiUIDefaults.elements()");

        // check MultiUIDefaults.entrySet()
        boolean found = false;
        Set<Entry<Object, Object>> entries = UIManager.getDefaults().entrySet();
        for (Entry<Object, Object> e: entries) {
            if (e.getKey() == key) {
                check(e.getValue() == value, "MultiUIDefaults.entrySet()");
                found = true;
            }
        }
        check(found == present, "MultiUIDefaults.entrySet()");
    }

    void checkEnumeration(Enumeration<Object> e, Object elem,
            boolean present, String error) {
        boolean found = false;
        while (e.hasMoreElements()) {
            if (e.nextElement() == elem) {
                found = true;
            }
        }
        check(found == present, error);
    }

    void check(boolean condition, String methodName) {
        if (! condition) {
            throw new RuntimeException(methodName + " failed");
        }
    }

    void test() {
        int size = UIManager.getDefaults().size();

        // create a new value, size increases
        UIManager.getLookAndFeelDefaults().put(KEY, VALUE);
        check(KEY, VALUE, true, size + 1);

        // override the value, size remains the same
        UIManager.put(KEY, VALUE);
        check(KEY, VALUE, true, size + 1);

        // remove the value, size decreases
        UIManager.getDefaults().remove(KEY);
        check(KEY, null, false, size);
    }

    public static void main(String[] args) {
        new Test6860438().test();
    }
}
