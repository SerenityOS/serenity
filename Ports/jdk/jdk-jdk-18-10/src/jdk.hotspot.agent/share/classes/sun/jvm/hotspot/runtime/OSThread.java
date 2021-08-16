/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.runtime;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// The OSThread class holds OS-specific thread information.  It is equivalent
// to the sys_thread_t structure of the classic JVM implementation.
public class OSThread extends VMObject {
    private static Field threadIdField;
    private static CIntegerField threadStateField;

    // ThreadStates read from underlying process
    private static int ALLOCATED;
    private static int INITIALIZED;
    private static int RUNNABLE;
    private static int MONITOR_WAIT;
    private static int CONDVAR_WAIT;
    private static int OBJECT_WAIT;
    private static int BREAKPOINTED;
    private static int SLEEPING;
    private static int ZOMBIE;

    static {
        VM.registerVMInitializedObserver(new Observer() {
            public void update(Observable o, Object data) {
                initialize(VM.getVM().getTypeDataBase());
            }
        });
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("OSThread");
        threadIdField = type.getField("_thread_id");
        threadStateField = type.getCIntegerField("_state");

        ALLOCATED = db.lookupIntConstant("ALLOCATED").intValue();
        INITIALIZED = db.lookupIntConstant("INITIALIZED").intValue();
        RUNNABLE = db.lookupIntConstant("RUNNABLE").intValue();
        MONITOR_WAIT = db.lookupIntConstant("MONITOR_WAIT").intValue();
        CONDVAR_WAIT = db.lookupIntConstant("CONDVAR_WAIT").intValue();
        OBJECT_WAIT = db.lookupIntConstant("OBJECT_WAIT").intValue();
        BREAKPOINTED = db.lookupIntConstant("BREAKPOINTED").intValue();
        SLEEPING = db.lookupIntConstant("SLEEPING").intValue();
        ZOMBIE = db.lookupIntConstant("ZOMBIE").intValue();
    }

    public OSThread(Address addr) {
        super(addr);
    }

    public int threadId() {
        return threadIdField.getJInt(addr);
    }

    public ThreadState getThreadState() {
        int val = (int)threadStateField.getValue(addr);
        if (val ==  ALLOCATED) {
            return ThreadState.ALLOCATED;
        } else if (val ==  INITIALIZED) {
            return ThreadState.INITIALIZED;
        } else if (val ==  RUNNABLE) {
            return ThreadState.RUNNABLE;
        } else if (val ==  MONITOR_WAIT) {
            return ThreadState.MONITOR_WAIT;
        } else if (val ==  CONDVAR_WAIT) {
            return ThreadState.CONDVAR_WAIT;
        } else if (val ==  OBJECT_WAIT) {
            return ThreadState.OBJECT_WAIT;
        } else if (val ==  BREAKPOINTED) {
            return ThreadState.BREAKPOINTED;
        } else if (val ==  SLEEPING) {
            return ThreadState.SLEEPING;
        } else if (val ==  ZOMBIE) {
            return ThreadState.ZOMBIE;
        } else {
            throw new RuntimeException("Illegal thread state " + val);
        }
    }
}
