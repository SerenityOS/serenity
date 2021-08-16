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
  @bug 5079469
  @summary DnD of File-List across JVM adds two empty items to the list
  @library ../../regtesthelpers
  @library ../../regtesthelpers/process
  @build Util
  @build ProcessResults ProcessCommunicator
  @run main/othervm FileListBetweenJVMsTest main
*/

import static java.lang.Thread.sleep;

import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;
import test.java.awt.regtesthelpers.Util;
import java.awt.*;
import java.awt.event.InputEvent;

public class FileListBetweenJVMsTest {

    // information related to the test in common
    static int VISIBLE_RAWS_IN_LIST=15;

    public void start() {

        SourceFileListFrame sourceFrame = new SourceFileListFrame();

        Util.waitForIdle(null);

        String [] args = new String [] {
                String.valueOf(sourceFrame.getNextLocationX()),
                String.valueOf(sourceFrame.getNextLocationY()),
                String.valueOf(sourceFrame.getDragSourcePointX()),
                String.valueOf(sourceFrame.getDragSourcePointY()),
                String.valueOf(sourceFrame.getSourceFilesNumber())
        };

        ProcessResults processResults =
                ProcessCommunicator.executeChildProcess(this.getClass(), args);

        verifyTestResults(processResults);

    }// start()

    private static void verifyTestResults(ProcessResults processResults) {
        if ( InterprocessMessages.WRONG_FILES_NUMBER_ON_TARGET ==
                processResults.getExitValue())
        {
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED: Target has recieved" +
                    " wrong number of files.");
        }
        processResults.verifyStdErr(System.err);
        processResults.verifyProcessExitValue(System.err);
        processResults.printProcessStandartOutput(System.out);
    }

    //We cannot make an instance of the applet without the default constructor
    public FileListBetweenJVMsTest () {
        super();
    }

    //We need in this constructor to pass frame position between JVMs
    public FileListBetweenJVMsTest (Point targetFrameLocation, Point dragSourcePoint,
            int transferredFilesNumber)
            throws InterruptedException
    {
        TargetFileListFrame targetFrame = new TargetFileListFrame(targetFrameLocation,
                transferredFilesNumber);

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

    }

    enum InterprocessArguments {
        TARGET_FRAME_X_POSITION_ARGUMENT,
        TARGET_FRAME_Y_POSITION_ARGUMENT,
        DRAG_SOURCE_POINT_X_ARGUMENT,
        DRAG_SOURCE_POINT_Y_ARGUMENT,
        FILES_IN_THE_LIST_NUMBER_ARGUMENT;

        int extract (String [] args) {
            return Integer.parseInt(args[this.ordinal()]);
        }
    }

    public static void main(final String [] args) {
        if (args.length > 0 && args[0].equals("main")) {
            new FileListBetweenJVMsTest().start();
            return;
        }
        Point dragSourcePoint = new Point(InterprocessArguments.DRAG_SOURCE_POINT_X_ARGUMENT.extract(args),
                InterprocessArguments.DRAG_SOURCE_POINT_Y_ARGUMENT.extract(args));
        Point targetFrameLocation = new Point(InterprocessArguments.TARGET_FRAME_X_POSITION_ARGUMENT.extract(args),
                InterprocessArguments.TARGET_FRAME_Y_POSITION_ARGUMENT.extract(args));
        int transferredFilesNumber = InterprocessArguments.FILES_IN_THE_LIST_NUMBER_ARGUMENT.extract(args);

        try {
            new FileListBetweenJVMsTest(targetFrameLocation, dragSourcePoint, transferredFilesNumber);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}// class FileListBetweenJVMsTest
