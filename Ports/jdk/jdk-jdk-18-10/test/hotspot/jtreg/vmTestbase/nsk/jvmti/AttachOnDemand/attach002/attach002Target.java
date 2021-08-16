/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.AttachOnDemand.attach002;

import nsk.share.aod.TargetApplicationWaitingAgents;

public class attach002Target extends TargetApplicationWaitingAgents {

    private native boolean agentGotCapabilities();

    protected void targetApplicationActions() {
        /*
         * If VM is run with -Xshare:on agent can't get required capabilities (see 6718407)
         */
        if (!agentGotCapabilities()) {
            log.display("WARNING: agent failed to get required capabilities, can't execute test checks");
        } else {
            final int expectedSize = 10;

            /*
             * ClassToRedefine should be redefined by test agent during ClassToRedefine loading,
             * after redefinition method ClassToRedefine.getSize() should return value 10
             */
            ClassToRedefine redefinedClassInstance = new ClassToRedefine();

            int size = redefinedClassInstance.getSize();
            log.display("ClassToRedefine.getSize(): " + size);
            if (size != expectedSize) {
                setStatusFailed("ClassToRedefine.getSize() returned unexpected value (expected is " + expectedSize + ")" +
                        ", (probably class wasn't redefined");
            }
        }
    }

    public static void main(String[] args) {
        new attach002Target().runTargetApplication(args);
    }
}
