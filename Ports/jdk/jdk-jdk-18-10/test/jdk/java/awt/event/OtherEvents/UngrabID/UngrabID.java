/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6960516
  @summary check if the ungrab event has the ID < AWTEvent.RESERVED_ID_MAX
  @author Andrei Dmitriev : area=awt.event
  @modules java.desktop/sun.awt
  @run main UngrabID
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class UngrabID {
    public static void main(String[] args){
        Frame f = new Frame("Dummy");
        sun.awt.UngrabEvent event = new sun.awt.UngrabEvent(f);
        if (event.getID() > AWTEvent.RESERVED_ID_MAX) {
                System.out.println( " Event ID : "+event.getID() + " " + event.toString());
                throw new RuntimeException(" Ungrab Event ID should be less than AWTEvent.RESERVED_ID_MAX ("+AWTEvent.RESERVED_ID_MAX+"). Actual value : "+event.getID() + " Event:" + event.toString());
        }
        System.out.println("Test passed. ");
   }
}
