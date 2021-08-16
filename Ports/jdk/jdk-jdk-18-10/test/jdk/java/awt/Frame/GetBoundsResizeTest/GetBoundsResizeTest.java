/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4103095
   @summary Test for getBounds() after a Frame resize.
   @author andrei.dmitriev : area=awt.toplevel
   @run main/manual GetBoundsResizeTest
*/

import java.applet.Applet;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;

class Globals {
  static boolean testPassed=false;
  static Thread mainThread=null;
}

public class GetBoundsResizeTest extends Applet {

  public static void main(String args[]) throws Exception {
    GetBoundsResizeTest app = new GetBoundsResizeTest();
    app.start();
    Globals.mainThread = Thread.currentThread();
    try {
      Thread.sleep(300000);
    } catch (InterruptedException e) {
      if (!Globals.testPassed)
        throw new Exception("GetBoundsResizeTest failed.");
    }
  }

  public void start()
  {
    String[] message = {
      "Resize the window using the upper left corner.",
      "Press the button to print the result of getBounds() to the terminal.",
      "If getBounds() prints the correct values for the window",
      "then click Pass, else click Fail."
    };
    new TestDialog(new Frame(), "GetBoundsResizeTest", message).start();
    new GetBoundsResizeTester("GetBoundsResizeTester").start();
  }
}

////////////////////////////////////////////////////////////////////////
//  Test Dialog
////////////////////////////////////////////////////////////////////////

class TestDialog extends Dialog
    implements ActionListener {

  static TextArea output;
  Button passButton;
  Button failButton;
  String name;

  public TestDialog(Frame frame, String name, String[] message)
  {
    super(frame, name + " Pass/Fail Dialog");
    this.name = name;
    int maxStringLength = 0;
    for (int i=0; i<message.length; i++) {
      maxStringLength = Math.max(maxStringLength, message[i].length());
    }
    output = new TextArea(10, maxStringLength);
    add("North", output);
    for (int i=0; i<message.length; i++){
        output.append(message[i] + "\n");
    }
    Panel buttonPanel = new Panel();
    passButton = new Button("Pass");
    failButton = new Button("Fail");
    passButton.addActionListener(this);
    failButton.addActionListener(this);
    buttonPanel.add(passButton);
    buttonPanel.add(failButton);
    add("South", buttonPanel);
    pack();
  }

  public void start()
  {
    show();
  }

  public void actionPerformed(ActionEvent event)
  {
    if ( event.getSource() == passButton ) {
      Globals.testPassed = true;
      System.err.println(name + " Passed.");
    }
    else if ( event.getSource() == failButton ) {
      Globals.testPassed = false;
      System.err.println(name + " Failed.");
    }
    this.dispose();
    if (Globals.mainThread != null)
      Globals.mainThread.interrupt();
  }
}


  ////////////////////////////////////////////////////////////////////////
  //  Test Class
  ////////////////////////////////////////////////////////////////////////

class GetBoundsResizeTester extends Frame {
    Button b = new Button("Press");

  GetBoundsResizeTester(String name)
  {
    super(name);
    final Frame f = this;
    Panel p = new Panel();
    f.add(p);
    p.setLayout(new BorderLayout());
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent be){
        Point cp = b.getLocationOnScreen();
        TestDialog.output.append("Current Frame.getBounds() = " + f.getBounds()+"\n");
      }
    });
    p.add("Center", b);
    f.pack();
  }

  public void start ()
  {
      setVisible(true);
      Robot robot;
      try {
          robot = new Robot();
          robot.waitForIdle();
      }catch(Exception ignorex) {
      }
      Point cp = b.getLocationOnScreen();
      TestDialog.output.append("Original Frame.getBounds() = " + this.getBounds()+"\n");
  }

  public static void main(String[] args)
  {
    new GetBoundsResizeTester("GetBoundsResizeTester").start();
  }

}
