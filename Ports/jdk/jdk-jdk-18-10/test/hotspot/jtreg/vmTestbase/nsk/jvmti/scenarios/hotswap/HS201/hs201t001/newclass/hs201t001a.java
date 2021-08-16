/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.hotswap.HS201;

public class hs201t001a extends Exception {

    public hs201t001a () {
        System.out.println("Current step: " + hs201t001.currentStep); // Avoid calling classloader to find hs201t001 in doInit()
        doInit();
    }

    private void doInit() {
        // Mark that we are in the doInit function.
        hs201t001.isInDoInitFunction = true;

        int localVariable;
        localVariable = 1;
        localVariable = 2;
        localVariable = 3;

        while (hs201t001.currentStep <= 4) {
            localVariable = 3;
        }
    }
}
