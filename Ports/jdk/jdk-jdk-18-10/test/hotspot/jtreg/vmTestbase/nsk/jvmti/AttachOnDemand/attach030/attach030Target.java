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
package nsk.jvmti.AttachOnDemand.attach030;

import nsk.share.aod.TargetApplicationWaitingAgents;

public class attach030Target extends TargetApplicationWaitingAgents {

    private final String getStringBeforeRedefine  = "ClassToRedefine01: Class isn't redefined";

    private final String getStringAfterRedefine  = "ClassToRedefine01: Class is redefined";

    protected void init(String[] args) {
        checkClassToRedefine01(true);
    }

    private void checkClassToRedefine01(boolean beforeRedefine) {
        if (beforeRedefine)
            log.display("Checking ClassToRedefine01 before redefine");
        else
            log.display("Checking ClassToRedefine01 after redefine");

        ClassToRedefine01 instance = new ClassToRedefine01();
        String string = instance.getString();
        log.display("ClassToRedefine01.getString(): " + string);

        String expectedString = beforeRedefine ? getStringBeforeRedefine : getStringAfterRedefine;

        if (!string.equals(expectedString)) {
            setStatusFailed("ClassToRedefine01.getString() returned unexpected value, expected is '" + expectedString + "'");
        }
    }

    protected void afterAgentsFinished() {
        checkClassToRedefine01(false);
    }

    public static void main(String[] args) {
        new attach030Target().runTargetApplication(args);
    }
}
