/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8161664
 * @summary Memory leak in com.apple.laf.AquaProgressBarUI: removed progress bar still referenced
 * @library ../../regtesthelpers
 * @build Util
 * @key headful
 * @run main/timeout=300/othervm -Xmx16m ProgressBarMemoryLeakTest
 */
import java.awt.EventQueue;
import java.lang.ref.WeakReference;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ProgressBarMemoryLeakTest {

  private static JFrame sFrame;
  private static WeakReference<JProgressBar> sProgressBar;

  public static void main(String[] args) throws Exception {
    UIManager.LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
    for ( UIManager.LookAndFeelInfo installedLookAndFeel : installedLookAndFeels ) {
      executeTestCase(installedLookAndFeel.getClassName());
    }
  }

  private static void executeTestCase(String lookAndFeelString) throws Exception{
    if (tryLookAndFeel(lookAndFeelString)) {
      EventQueue.invokeAndWait( new Runnable() {
        @Override
        public void run() {
          showUI();
        }
      } );
      EventQueue.invokeAndWait( new Runnable() {
        @Override
        public void run() {
          disposeUI();
        }
      } );
      Util.generateOOME();
      JProgressBar progressBar = sProgressBar.get();
      if ( progressBar != null ) {
        throw new RuntimeException( "Progress bar (using L&F: " + lookAndFeelString + ") should have been GC-ed" );
      }
    }
  }

  private static void showUI(){
    sFrame = new JFrame();

    JProgressBar progressBar = new JProgressBar();
    progressBar.setVisible(false);
    progressBar.setIndeterminate(false);
    progressBar.setIndeterminate(true);
    progressBar.setIndeterminate(false);
    progressBar.setValue(10);
    progressBar.setString("Progress");

    sFrame.add(progressBar);

    sProgressBar = new WeakReference<>(progressBar);

    sFrame.setSize(200,200);
    sFrame.setVisible(true);
  }

  private static void disposeUI(){
    sFrame.setContentPane(new JPanel());
    sFrame.dispose();
    sFrame = null;
  }

  private static boolean tryLookAndFeel(String lookAndFeelString) throws Exception {
    try {
      UIManager.setLookAndFeel(lookAndFeelString);
    } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      return false;
    }
    return true;
  }
}
