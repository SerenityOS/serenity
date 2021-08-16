/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4408582
 * @summary Test fix for: JDWP: WatchpointEvents outside of class filtered out
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g FieldWatchpoints.java
 * @run driver FieldWatchpoints
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

class A extends Object {
    public static int aField = 0;
}

class B extends A {
}

class FieldWatchpointsDebugee {
    public void update (){
        /* test direct modify access by other class */
        A.aField = 7;
        B.aField = 11;
    }
    public void access (){
        /* test direct read access by other class */
        System.out.print("aField is: ");
        System.out.println(A.aField);
    }
    public static void main(String[] args){
        A testA = new A();
        B testB = new B();
        FieldWatchpointsDebugee my =
            new FieldWatchpointsDebugee();
        my.update();
        my.access();
    }
}

public class FieldWatchpoints extends TestScaffold {
    boolean fieldModifyReported = false;
    boolean fieldAccessReported = false;

    FieldWatchpoints (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new FieldWatchpoints (args).startTests();
    }

    protected void runTests() throws Exception {
        startTo("FieldWatchpointsDebugee", "update", "()V");

        try {
            /*
             * Set a modification watch on aField
             */
            ReferenceType rt = findReferenceType("A");
            String fieldName = "aField";
            Field field = rt.fieldByName(fieldName);
            if (field == null) {
                throw new Exception ("Field name not found: " + fieldName);
            }
            com.sun.jdi.request.EventRequest req =
               eventRequestManager().createModificationWatchpointRequest(field);
            req.setSuspendPolicy(com.sun.jdi.request.EventRequest.SUSPEND_ALL);
            req.enable();

            /*
             * Set an access watch on aField
             */
            req =
               eventRequestManager().createAccessWatchpointRequest(field);
            req.setSuspendPolicy(com.sun.jdi.request.EventRequest.SUSPEND_ALL);
            req.enable();

            addListener (new TargetAdapter() {
                    EventSet lastSet = null;

                    public void eventSetReceived(EventSet set) {
                        lastSet = set;
                    }
                    public void fieldModified(ModificationWatchpointEvent event) {
                        System.out.println("Field modified: " + event);
                        fieldModifyReported = true;
                        lastSet.resume();
                    }
                    public void fieldAccessed(AccessWatchpointEvent event) {
                        System.out.println("Field accessed: " + event);
                        fieldAccessReported = true;
                        lastSet.resume();
                    }
                });

            vm().resume();

        } catch (Exception ex){
            ex.printStackTrace();
            testFailed = true;
        } finally {
            // Allow application to complete and shut down
            resumeToVMDisconnect();
        }
        if (!testFailed && fieldModifyReported && fieldAccessReported) {
            System.out.println("FieldWatchpoints: passed");
        } else {
            throw new Exception("FieldWatchpoints: failed");
        }
    }
}
