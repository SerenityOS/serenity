/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4368402
 * @summary UnpreparedClasses verifies that all the classes in the
 * loaded class list are prepared classes.
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection
 * @run compile -g InnerTarg.java
 * @run build UnpreparedClasses
 *
 * @run driver UnpreparedClasses InnerTarg
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;
import java.util.Iterator;

public class UnpreparedClasses extends TestScaffold {

    public static void main(String args[]) throws Exception {
        new UnpreparedClasses(args).startTests();
    }

    UnpreparedClasses(String args[]) throws Exception {
        super(args);
    }

    protected void runTests() throws Exception {
        startTo("InnerTarg", "go", "()V");

        List all = vm().allClasses();
        for (Iterator it = all.iterator(); it.hasNext(); ) {
            ReferenceType cls = (ReferenceType)it.next();
            boolean preped = cls.isPrepared() || (cls instanceof ArrayReference);

            if (!preped) {
                System.err.println("Class not prepared: " + cls);
            }
            cls.methods();  // exception if methods unprepared
            if (!preped) {
                throw new Exception("Class not prepared: " + cls);
            }
        }

        // Allow application to complete
        resumeToVMDisconnect();
    }
}
