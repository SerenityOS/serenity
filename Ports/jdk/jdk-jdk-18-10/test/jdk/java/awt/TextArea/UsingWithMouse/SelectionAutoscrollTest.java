/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6497109 6734341
  @summary TextArea must have selection expanding, and also be autoscrolled, if mouse is dragged from inside.
  @library ../../regtesthelpers
  @build Util
  @author Konstantin Voloshin: area=TextArea
  @run main SelectionAutoscrollTest
*/

/**
 * SelectionAutoscrollTest.java
 *
 * summary: TextArea should be auto-scrolled and text should be selected to
 *   the end, if mouse is dragged from inside box-for-text to outside it, and
 *   is hold pressed there.
 */


import java.awt.Frame;
import java.awt.Panel;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Robot;
import java.awt.Toolkit;
import test.java.awt.regtesthelpers.Util;


public class SelectionAutoscrollTest {
    TextArea textArea;
    Robot robot;
    final int desiredSelectionEnd = ('z'-'a'+1)*2;  // 52
    final static int SCROLL_DELAY = 10; // ms

    public static void main(String[] args) {
        SelectionAutoscrollTest selectionAutoscrollTest
                = new SelectionAutoscrollTest();
        selectionAutoscrollTest.createObjects();
        selectionAutoscrollTest.manipulateMouse();
        selectionAutoscrollTest.checkResults();
    }

    void createObjects() {
        textArea = new TextArea( bigString() );
        robot = Util.createRobot();

        Panel panel = new Panel();
        panel.setLayout( new GridLayout(3,3) );

        for( int y=0; y<3; ++y ) {
            for( int x=0; x<3; ++x ) {
                if( x==1 && y==1 ) {
                    panel.add( textArea );
                } else {
                    panel.add( new Panel() );
                }
            }
        }

        Frame frame = new Frame( "TextArea cursor icon test" );
        frame.setSize( 300, 300 );
        frame.add( panel );
        frame.setVisible( true );
    }

    static String bigString() {
        String s = "";
        for( char c='a'; c<='z'; ++c ) {
            s += c+"\n";
        }
        return s;
    }

    void manipulateMouse() {
        moveMouseToCenterOfTextArea();
        Util.waitForIdle( robot );

        robot.mousePress( MouseEvent.BUTTON1_MASK );
        Util.waitForIdle( robot );

        for( int tremble=0; tremble < 10; ++tremble ) {
            // Mouse is moved repeatedly here (with conservatively chosen
            // ammount of times), to give some time/chance for TextArea to
            // autoscroll and for text-selection to expand to the end.
            // This is because:
            // - On Windows,
            //   autoscrolling and selection-expansion happens only once per
            //   each mouse-dragged event received, and only for some ammount,
            //   not to the end. So, we have to drag mouse repeatedly.
            // - on X,
            //   only 1 mouse-dragged event is required for autoscrolling/
            //   selection-expanding to commence. Once commenced, it will
            //   continue to the end of text (provided that mouse-button is
            //   hold pressed), but it may take hardly predictable ammount of
            //   time. However, repeatedly dragging mouse seems perfectly help
            //   here, instead of having to use 'Thread.sleep( ??? )'.
            // Note: It's required here to move mouse 2 times to receive the
            //   1-st drag-event. After 1-st movement, only mouse-exited event
            //   will be generated. If mouse was released after first movement
            //   here, we would even get mouse-clicked event (at least for now,
            //   and this is probably a bug). But, starting with 2nd iteration,
            //   all events received will be mouse-dragged events.

            moveMouseBelowTextArea( tremble );
            Util.waitForIdle( robot );
            // it is needed to add some small delay on Gnome
            waitUntilScrollIsPerformed(robot);
        }

        robot.mouseRelease( MouseEvent.BUTTON1_MASK );
        Util.waitForIdle( robot );
    }

    void moveMouseToCenterOfTextArea() {
        Dimension d = textArea.getSize();
        Point l = textArea.getLocationOnScreen();
        Util.mouseMove(robot, l, new Point((int) (l.x + d.width * .5),
                (int) (l.y + d.height * .5)));
    }

    void moveMouseBelowTextArea(int tremble) {
        Dimension d = textArea.getSize();
        Point l = textArea.getLocationOnScreen();
        Point p1;
        if (tremble == 0) {
            p1 = new Point((int) (l.x + d.width * .5),
                    (int) (l.y + d.height * 0.5));
        } else {
            p1 = new Point((int) (l.x + d.width * .5),
                    (int) (l.y + d.height * 1.5));
        }
        Point p2 = new Point((int) (l.x + d.width * .5),
                (int) (l.y + d.height * 1.5) + 15);
        if (tremble % 2 == 0) {
            Util.mouseMove(robot, p1, p2);
        } else {
            Util.mouseMove(robot, p2, p1);
        }
    }

    void waitUntilScrollIsPerformed(Robot robot) {
        try {
            Thread.sleep( SCROLL_DELAY );
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    void checkResults() {
        final int currentSelectionEnd = textArea.getSelectionEnd();
        System.out.println(
                "TEST: Selection range after test is: ( "
                + textArea.getSelectionStart() + ", "
                + currentSelectionEnd + " )"
        );

        boolean resultOk = ( currentSelectionEnd == desiredSelectionEnd );
        String desiredSelectionEndString = "" + desiredSelectionEnd;

        // On Windows, last empty line is surprisingly not selected.
        // Even if it's a bug, it's not for this test.
        // So, we have 2 acceptable results in this case.
        String toolkitName = Toolkit.getDefaultToolkit().getClass().getName();
        if( toolkitName.equals("sun.awt.windows.WToolkit") ) {
            final int desiredSelectionEnd2 = desiredSelectionEnd-1;  // 51
            resultOk |= ( currentSelectionEnd == desiredSelectionEnd2 );
            desiredSelectionEndString += " or " + desiredSelectionEnd2;
        }

        if( resultOk ) {
            System.out.println(
                "TEST: passed: Text is selected to the end"
                + " (expected selection range end is "
                + desiredSelectionEndString + ")."
            );
        } else {
            System.out.println(
                "TEST: FAILED: Text should be selected to the end"
                + " (selection range end should be "
                + desiredSelectionEndString + ")."
            );
            throw new RuntimeException(
                "TEST: FAILED: Text should be selected to the end, but it is not."
            );
        }
    }
}
