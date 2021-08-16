/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;

/*
 * @test
 * @key headful
 * @summary An attempt to set non-trivial background, shape, or translucency
 *          to a decorated toplevel should end with an exception.
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build ExtendedRobot
 * @run main DecoratedExceptions
 */
public class DecoratedExceptions {
    public static void main(String args[]) throws Exception{
        ExtendedRobot robot = new ExtendedRobot();
        Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(() -> {
            Frame frame = new Frame("Frame");
            frame.setBounds(50,50,400,200);
            try {
                frame.setOpacity(0.5f);
                throw new RuntimeException("No exception when Opacity set to a decorated Frame");
            }catch(IllegalComponentStateException e) {
            }
            try {
                frame.setShape(new Rectangle(50,50,400,200));
                throw new RuntimeException("No exception when Shape set to a decorated Frame");
            }catch(IllegalComponentStateException e) {
            }
            try {
                frame.setBackground(new Color(50, 50, 50, 100));
                throw new RuntimeException("No exception when Alpha background set to a decorated Frame");
            }catch(IllegalComponentStateException e) {
            }
            frame.setVisible(true);
            Dialog dialog = new Dialog( frame );
            try {
                dialog.setOpacity(0.5f);
                throw new RuntimeException("No exception when Opacity set to a decorated Dialog");
            }catch(IllegalComponentStateException e) {
            }
            try {
                dialog.setShape(new Rectangle(50,50,400,200));
                throw new RuntimeException("No exception when Shape set to a decorated Dialog");
            }catch(IllegalComponentStateException e) {
            }
            try {
                dialog.setBackground(new Color(50, 50, 50, 100));
                throw new RuntimeException("No exception when Alpha background set to a decorated Dialog");
            }catch(IllegalComponentStateException e) {
            }
            dialog.setVisible(true);
        });
        robot.waitForIdle(1000);
    }
}
