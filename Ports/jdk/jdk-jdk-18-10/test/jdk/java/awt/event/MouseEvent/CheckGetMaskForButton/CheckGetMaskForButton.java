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
  @modules java.desktop/java.awt.event:open
  @summary verifies that InputEvent.getMaskForButton() returns the same values as in InputEvent.BUTTON_DOWN_MASK
  @author Andrei Dmitriev : area=awt.event
  @run main CheckGetMaskForButton
*/

import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class CheckGetMaskForButton{
    static Robot robot;

    public static void main(String []s){
        System.out.println("Number Of Buttons = "+ MouseInfo.getNumberOfButtons());
        CheckGetMaskForButton f = new CheckGetMaskForButton();
        int [] buttonMasksViaAPI = new int[MouseInfo.getNumberOfButtons()];
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonMasksViaAPI[i] = InputEvent.getMaskForButton(i+1);
            System.out.println("Test (API): "+ buttonMasksViaAPI[i]);
        }

        //get same array via reflection
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

        if (obj == null){
            throw new RuntimeException("Test failed. The value obtained via reflection is "+obj);
        }

        int [] buttonDownMasksViaReflection = new int [Array.getLength(obj)];
        //check that length of API array greater or equals then Reflect array.
        if (Array.getLength(obj) < buttonMasksViaAPI.length){
            throw new RuntimeException("Test failed. The length of API array greater or equals then the length of  Reflect array.");
        }

        //Check that the values obtained via reflection from InputEvent.BUTTON_DOWN_MASK are the
        // same as for standard API.
        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            System.out.println("Test (Reflection): "+ Array.getInt(obj, i));
            if (buttonMasksViaAPI[i] != Array.getInt(obj, i)){
                throw new RuntimeException("Test failed. Values of InputEvent array are different for API and Reflection invocations");
            }
        }
        System.out.println("Test passed.");
    }
}
