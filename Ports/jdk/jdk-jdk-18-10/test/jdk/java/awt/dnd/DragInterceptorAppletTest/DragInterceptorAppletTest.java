/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6887703
  @summary Unsigned applet can retrieve the dragged information before drop action occurs
  @library ../../regtesthelpers
  @library ../../regtesthelpers/process
  @build Util
  @build ProcessResults ProcessCommunicator
  @run main/othervm DragInterceptorAppletTest main
*/

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import test.java.awt.regtesthelpers.Util;
import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;

import static java.lang.Thread.sleep;

public class DragInterceptorAppletTest {

    public void start() {

        SourceFrame sourceFrame = new SourceFrame();

        Util.waitForIdle(null);

        String [] args = new String [] {
            String.valueOf(sourceFrame.getNextLocationX()),
            String.valueOf(sourceFrame.getNextLocationY()),
            String.valueOf(sourceFrame.getDragSourcePointX()),
            String.valueOf(sourceFrame.getDragSourcePointY()),
        };
        String classpath = System.getProperty("java.class.path");
        ProcessResults processResults =
            ProcessCommunicator.executeChildProcess(this.getClass(),classpath,args);

        verifyTestResults(processResults);

    }// start()

    private static void verifyTestResults(ProcessResults processResults) {

    switch (processResults.getExitValue()) {
        case InterprocessMessages.DATA_WAS_INTERCEPTED_AND_EXCEPTION_HANDLER_WAS_NOT_TRIGGERED:
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED: Target applet can intercept data " +
                    "without a clipboard permission and an exception handler was not triggered.");
            //Unreachable...

        case InterprocessMessages.DATA_WAS_INTERCEPTED:
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED: Target applet can intercept data " +
                    "without a clipboard permission");
            //Unreachable...

        case InterprocessMessages.EXCEPTION_HANDLER_WAS_NOT_TRIGGERED:
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED: An exception handler was not triggered.");
            //Unreachable...

    }

        //    The child process throws an exception. do not look at the stderr.
        processResults.verifyStdErr(System.err);
        processResults.verifyProcessExitValue(System.err);
        processResults.printProcessStandartOutput(System.out);
    }

    //We cannot make an instance of the applet without the default constructor
    public DragInterceptorAppletTest() {
        super();
    }

    //We need in this constructor to pass frame position between JVMs
    public DragInterceptorAppletTest(Point targetFrameLocation, Point dragSourcePoint)
            throws InterruptedException
    {
        DragInterceptorFrame targetFrame = new DragInterceptorFrame(targetFrameLocation);

        Util.waitForIdle(null);

        final Robot robot = Util.createRobot();

        robot.mouseMove((int)dragSourcePoint.getX(),(int)dragSourcePoint.getY());
        sleep(100);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        sleep(100);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        sleep(100);

        Util.drag(robot, dragSourcePoint, targetFrame.getDropTargetPoint(),
                InputEvent.BUTTON1_MASK);

        sleep(2000);
        ProcessCommunicator.destroyProcess();
    }

    enum InterprocessArguments {
        TARGET_FRAME_X_POSITION_ARGUMENT,
        TARGET_FRAME_Y_POSITION_ARGUMENT,
        DRAG_SOURCE_POINT_X_ARGUMENT,
        DRAG_SOURCE_POINT_Y_ARGUMENT;

        int extract (String [] args) {
            return Integer.parseInt(args[this.ordinal()]);
        }
    }

    public static void main(final String[] args) {
        if (args.length > 0 && args[0].equals("main")) {
            new DragInterceptorAppletTest().start();
            return;
        }
        Point dragSourcePoint = new Point(InterprocessArguments.DRAG_SOURCE_POINT_X_ARGUMENT.extract(args),
                InterprocessArguments.DRAG_SOURCE_POINT_Y_ARGUMENT.extract(args));
        Point targetFrameLocation = new Point(InterprocessArguments.TARGET_FRAME_X_POSITION_ARGUMENT.extract(args),
                InterprocessArguments.TARGET_FRAME_Y_POSITION_ARGUMENT.extract(args));
        try {
            new DragInterceptorAppletTest(targetFrameLocation, dragSourcePoint);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}// class DragInterceptorAppletTest
