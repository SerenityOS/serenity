/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.aod.VirtualMachine.VirtualMachine06;

import nsk.share.aod.TargetApplicationWaitingAgents;

import java.lang.instrument.Instrumentation;

public class VM06Agent00 {
    public static void agentmain(String options, Instrumentation inst) {
        boolean success = true;

        TargetApplicationWaitingAgents.agentLoaded(VM06Agent00.class.getName());
        try {
            System.out.println("Agent options: " + options);
            if (options != null) {
                success = false;
                System.out.println("ERROR: unexpected non-null options");
            }
        } catch (Throwable t) {
            success = false;
            System.out.println("Unexpected exception: " + t);
        } finally {
            TargetApplicationWaitingAgents.agentFinished(VM06Agent00.class.getName(), success);
        }
    }
}
