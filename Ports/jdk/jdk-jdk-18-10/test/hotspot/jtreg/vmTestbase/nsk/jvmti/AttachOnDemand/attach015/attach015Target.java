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
package nsk.jvmti.AttachOnDemand.attach015;

import nsk.share.aod.TargetApplicationWaitingAgents;

public class attach015Target extends TargetApplicationWaitingAgents {

    private void loadClass(String className) {
        try {
            Class.forName(className, true, this.getClass().getClassLoader());
        } catch (Throwable t ){
            setStatusFailed("Unexpected exception during class loading: " + t);
            t.printStackTrace(log.getOutStream());
        }
    }

    /*
     * Load class using JNI FindClass
     */
    private native void loadClassFromNative();

    private String className1 = "nsk.jvmti.AttachOnDemand.attach015.ClassToLoad1";

    protected void targetApplicationActions() throws Throwable {
        try {
            System.loadLibrary("attach015Target");
        } catch (UnsatisfiedLinkError e) {
            exitAsFailed("UnsatisfiedLinkError was thrown during native library loading, stop test");
        }

        log.display("is loading class '" + className1 + "'");
        loadClass(className1);

        log.display("is loading class using jni->FindClass");
        loadClassFromNative();
    }

    public static void main(String[] args) {
        new attach015Target().runTargetApplication(args);
    }
}
