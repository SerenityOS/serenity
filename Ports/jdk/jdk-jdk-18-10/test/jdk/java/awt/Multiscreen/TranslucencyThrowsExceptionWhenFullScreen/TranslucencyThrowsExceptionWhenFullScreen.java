/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6838089
  @summary Translucent windows should throw exception in FS mode
  @author dmitry.cherepanov@oracle.com: area=awt-multiscreen
  @run main TranslucencyThrowsExceptionWhenFullScreen
*/

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class TranslucencyThrowsExceptionWhenFullScreen
{
    public static void main(String[] args)
        throws InvocationTargetException, InterruptedException
    {
        EventQueue.invokeAndWait(
            new Runnable(){
                public void run() {
                    Frame frame = new Frame();
                    frame.setBounds(100,100,100,100);
                    frame.setVisible(true);

                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice[] devices = ge.getScreenDevices();
                    for (GraphicsDevice device : devices) {
                        testGraphicsDevice(device, frame);
                    }

                    frame.dispose();
                }
            }
        );
    }

    private static void testGraphicsDevice(GraphicsDevice device, Frame frame) {
        device.setFullScreenWindow(frame);
        try {
            frame.setOpacity(0.5f);
            throw new RuntimeException("Test fails, there's no exception for device="+device);
        } catch(IllegalComponentStateException e) {
            device.setFullScreenWindow(null);
        }
    }
}
