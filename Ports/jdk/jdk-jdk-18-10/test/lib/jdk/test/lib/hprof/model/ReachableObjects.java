/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import jdk.test.lib.hprof.util.ArraySorter;
import jdk.test.lib.hprof.util.Comparer;

/**
 * @author      A. Sundararajan
 */

public class ReachableObjects {
    public ReachableObjects(JavaHeapObject root,
                            final ReachableExcludes excludes) {
        this.root = root;

        final Hashtable<JavaHeapObject, JavaHeapObject> bag = new Hashtable<JavaHeapObject, JavaHeapObject>();
        final Hashtable<String, String> fieldsExcluded = new Hashtable<String, String>();  //Bag<String>
        final Hashtable<String, String> fieldsUsed = new Hashtable<String, String>();   // Bag<String>
        JavaHeapObjectVisitor visitor = new AbstractJavaHeapObjectVisitor() {
            public void visit(JavaHeapObject t) {
                // Size is zero for things like integer fields
                if (t != null && t.getSize() > 0 && bag.get(t) == null) {
                    bag.put(t, t);
                    t.visitReferencedObjects(this);
                }
            }

            public boolean mightExclude() {
                return excludes != null;
            }

            public boolean exclude(JavaClass clazz, JavaField f) {
                if (excludes == null) {
                    return false;
                }
                String nm = clazz.getName() + "." + f.getName();
                if (excludes.isExcluded(nm)) {
                    fieldsExcluded.put(nm, nm);
                    return true;
                } else {
                    fieldsUsed.put(nm, nm);
                    return false;
                }
            }
        };
        // Put the closure of root and all objects reachable from root into
        // bag (depth first), but don't include root:
        visitor.visit(root);
        bag.remove(root);

        // Now grab the elements into a vector, and sort it in decreasing size
        JavaThing[] things = new JavaThing[bag.size()];
        int i = 0;
        for (Enumeration<JavaHeapObject> e = bag.elements(); e.hasMoreElements(); ) {
            things[i++] = (JavaThing) e.nextElement();
        }
        ArraySorter.sort(things, new Comparer() {
            public int compare(Object lhs, Object rhs) {
                JavaThing left = (JavaThing) lhs;
                JavaThing right = (JavaThing) rhs;
                long diff = right.getSize() - left.getSize();
                if (diff != 0) {
                    return Long.signum(diff);
                }
                return left.compareTo(right);
            }
        });
        this.reachables = things;

        this.totalSize = root.getSize();
        for (i = 0; i < things.length; i++) {
            this.totalSize += things[i].getSize();
        }

        excludedFields = getElements(fieldsExcluded);
        usedFields = getElements(fieldsUsed);
    }

    public JavaHeapObject getRoot() {
        return root;
    }

    public JavaThing[] getReachables() {
        return reachables;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String[] getExcludedFields() {
        return excludedFields;
    }

    public String[] getUsedFields() {
        return usedFields;
    }

    private String[] getElements(Hashtable<?, ?> ht) {
        Object[] keys = ht.keySet().toArray();
        int len = keys.length;
        String[] res = new String[len];
        System.arraycopy(keys, 0, res, 0, len);
        ArraySorter.sortArrayOfStrings(res);
        return res;
    }

    private JavaHeapObject root;
    private JavaThing[] reachables;
    private String[]  excludedFields;
    private String[]  usedFields;
    private long totalSize;
}
