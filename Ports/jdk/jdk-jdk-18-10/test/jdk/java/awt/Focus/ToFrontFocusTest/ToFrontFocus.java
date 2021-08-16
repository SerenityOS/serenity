/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4092033 4529626
  @summary Tests that toFront makes window focused unless it is non-focusable
  @library ../../regtesthelpers
  @build Util
  @run main ToFrontFocus
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class ToFrontFocus {
     Frame cover, focus_frame, nonfocus_frame;
     Button focus_button, nonfocus_button;
     volatile boolean focus_gained = false, nonfocus_gained = false;

    public static void main(final String[] args) {
        ToFrontFocus app = new ToFrontFocus();
        app.init();
        app.start();
    }

    public void init()
    {
      cover = new Frame("Cover frame");
      cover.setBounds(100, 100, 200, 200);
      focus_frame = new Frame("Focusable frame");
      focus_frame.setBounds(150, 100, 250, 150);
      nonfocus_frame = new Frame("Non-focusable frame");
      nonfocus_frame.setFocusableWindowState(false);
      nonfocus_frame.setBounds(150, 150, 250, 200);
      focus_button = new Button("Button in focusable frame");
      focus_button.addFocusListener(new FocusAdapter() {
              public void focusGained(FocusEvent e) {
                  focus_gained = true;
              }
          });
      nonfocus_button = new Button("Button in non-focusable frame");
      nonfocus_button.addFocusListener(new FocusAdapter() {
              public void focusGained(FocusEvent e) {
                  nonfocus_gained = true;
              }
          });
    }//End  init()

   public void start ()
    {
      Util.waitForIdle(null);

      focus_frame.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
              public Component getInitialComponent(Window w) {
                  return null;
              }
          });
      focus_frame.setVisible(true);
      nonfocus_frame.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
              public Component getInitialComponent(Window w) {
                  return null;
              }
          });
      nonfocus_frame.setVisible(true);
      cover.setVisible(true);

      Util.waitForIdle(null);

      // So events are no generated at the creation add buttons here.
      focus_frame.add(focus_button);
      focus_frame.pack();
      focus_frame.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
              public Component getInitialComponent(Window w) {
                  return focus_button;
              }
          });
      nonfocus_frame.add(nonfocus_button);
      nonfocus_frame.pack();
      nonfocus_frame.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
              public Component getInitialComponent(Window w) {
                  return nonfocus_button;
              }
          });

      System.err.println("------------ Starting test ------------");
      // Make frame focused - focus_gained will be genereated for button.
      focus_frame.toFront();
      // focus_gained should not be generated
      nonfocus_frame.toFront();

      // Wait for events.
      Util.waitForIdle(null);

      if (!focus_gained || nonfocus_gained) {
          throw new RuntimeException("ToFront doesn't work as expected");
      }
    }// start()

 }// class ToFrontFocus
