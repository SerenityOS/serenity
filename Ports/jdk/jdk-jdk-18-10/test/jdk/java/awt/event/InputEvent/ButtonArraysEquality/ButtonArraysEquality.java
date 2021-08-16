/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test %I% %E%
  @key headful
  @bug 6315717
  @summary verifies that InputEvents button masks arrays are the same
  @author Andrei Dmitriev : area=awt.event
  @modules java.desktop/java.awt.event:open
  @run main ButtonArraysEquality
 */

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

// get array InputEvent.BUTTON_MASK via reflection
// get array InputEvent.BUTTON_DOWN_MASK via reflection
// compare their lengths and values

public class ButtonArraysEquality {
    static int [] eventDownMask = new int []{InputEvent.BUTTON1_DOWN_MASK, InputEvent.BUTTON2_DOWN_MASK, InputEvent.BUTTON3_DOWN_MASK};

    public static void main(String []s){
        int [] buttonDownMasksAPI = new int [MouseInfo.getNumberOfButtons()];
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonDownMasksAPI[i] = InputEvent.getMaskForButton(i+1);
            System.out.println("TEST: "+buttonDownMasksAPI[i]);
        }

        // getButtonDownMasks()
        Object obj = AccessController.doPrivileged(
                                            new PrivilegedAction() {
                                                public Object run() {
                                                    try {
                                                        Class clazz = Class.forName("java.awt.event.InputEvent");
                                                        Method method  = clazz.getDeclaredMethod("getButtonDownMasks",new Class [] {});
                                                        if (method != null) {
                                                            method.setAccessible(true);
                                                            return method.invoke(null, (Object[])null);
                                                        }
                                                    }catch (Exception e){
                                                        throw new RuntimeException("Test failed. Exception occured:", e);
                                                    }
                                                    return null;
                                                }
                                            });

        int [] buttonDownMasks = new int [Array.getLength(obj)];
        checkNullAndPutValuesToArray(buttonDownMasks, obj);

        //check lengths: array shouldn't contain less elements then the number of buttons on a mouse
        if (buttonDownMasks.length < buttonDownMasksAPI.length){
            throw new RuntimeException("Test failed. The lengths array is less than the number of buttons");
        }

        // verify values for first three buttons
        for (int i = 0; i < 3; i++) {
            if (eventDownMask[i] != buttonDownMasks[i])
            {
                System.out.println("Test : "+ i + " | " + " | " +eventDownMask[i] + " | "+ buttonDownMasks[i]);
                throw new RuntimeException("Failure: masks are not correct for standard buttons");
            }
        }

        // verify values for extra buttons if any
        for (int i = 3; i < MouseInfo.getNumberOfButtons(); i++) {
            if (buttonDownMasksAPI[i] != buttonDownMasks[i]) {
                throw new RuntimeException("Failure: masks are not the same for extra buttons");
            }
        }
        System.out.println("Test passed.");
    }

    public static void checkNullAndPutValuesToArray(int [] array, Object obj){
        if (obj == null){
            throw new RuntimeException("Test failed. The array obtained via reflection is "+obj);
        }

        for (int i = 0; i < Array.getLength(obj); i++){
            System.out.println("Test (Reflection): "+ Array.getInt(obj, i));
            array[i] = Array.getInt(obj, i);
        }
    }
}
