/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import sun.hotspot.WhiteBox;

public class UsedAllArchivedLambdasApp {
    public static boolean isRuntime;
    public static int NUM_CLASSES = 3;
    public static WhiteBox wb = WhiteBox.getWhiteBox();
    public static HashMap<Class<?>, Class<?>> inArchiveMap = new HashMap<>();
    public static HashMap<Class<?>, Class<?>> notInArchiveMap = new HashMap<>();

    public static void main(String args[]) {
        isRuntime = (args.length == 1 && args[0].equals("run")) ? true : false;
        {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
        {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
        {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
        if (isRuntime) {
            {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
            {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
            {Runnable run1 = UsedAllArchivedLambdasApp::myrun; run1.run();}
        }

        int mapSize = 0;

        if (isRuntime) {
            mapSize = inArchiveMap.size();
            System.out.println("Number of lambda classes in archive: " + mapSize);
            if (mapSize != NUM_CLASSES) {
                throw new RuntimeException("Expected number of lambda classes in archive is " +
                    NUM_CLASSES + " but got " + mapSize);
            }
            mapSize = notInArchiveMap.size();
            System.out.println("Number of lambda classes in archive: " + mapSize);
            if (mapSize != NUM_CLASSES) {
                throw new RuntimeException("Expected number of lambda classes NOT in archive is " +
                    NUM_CLASSES + " but got " + mapSize);
            }
        }
    }

    static void myrun() {
        Class<?> c = LambdaVerification.getCallerClass(1);
        if (isRuntime) {
            if (wb.isSharedClass(c)) {
                System.out.println(c.getName() + " is a shared class");
                inArchiveMap.put(c,c);
            } else {
                System.out.println(c.getName() + " is NOT a shared class");
                notInArchiveMap.put(c,c);
            }
        }
    }
}
