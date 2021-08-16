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

import jdk.test.lib.hprof.util.Misc;

/**
 * A forward reference to an object.  This is an intermediate representation
 * for a JavaThing, when we have the thing's ID, but we might not have read
 * the thing yet.
 *
 * @author      Bill Foote
 */
public class JavaObjectRef extends JavaThing {
    private long id;

    public JavaObjectRef(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public boolean isHeapAllocated() {
        return true;
    }

    public JavaThing dereference(Snapshot snapshot, JavaField field) {
        return dereference(snapshot, field, true);
    }

    public JavaThing dereference(Snapshot snapshot, JavaField field, boolean verbose) {
        if (field != null && !field.hasId()) {
            // If this happens, we must be a field that represents an int.
            // (This only happens with .bod-style files)
            return new JavaLong(id);
        }
        if (id == 0) {
            return snapshot.getNullThing();
        }
        JavaThing result = snapshot.findThing(id);
        if (result == null) {
            if (!snapshot.getUnresolvedObjectsOK() && verbose) {
                String msg = "WARNING:  Failed to resolve object id "
                                + Misc.toHex(id);
                if (field != null) {
                    msg += " for field " + field.getName()
                            + " (signature " + field.getSignature() + ")";
                }
                System.out.println(msg);
                // Thread.dumpStack();
            }
            result = new HackJavaValue("Unresolved object "
                                        + Misc.toHex(id), 0);
        }
        return result;
    }

    @Override
    public long getSize() {
        return 0;
    }

    public String toString() {
        return "Unresolved object " + Misc.toHex(id);
    }
}
