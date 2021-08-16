/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;
import test.java.awt.regtesthelpers.Util;

import static java.lang.Thread.sleep;

/*
    @test
    @key headful
    @bug 8005932 8017456
    @summary Java 7 on mac os x only provides text clipboard formats
    @library ../../regtesthelpers
    @library ../../regtesthelpers/process
    @library /test/lib
    @build Util
    @build ProcessResults ProcessCommunicator
    @build jdk.test.lib.Platform
    @run main/othervm MissedHtmlAndRtfBug main
 */
public class MissedHtmlAndRtfBug {

    public void start() {
        if (!Platform.isOSX() && !Platform.isWindows()) {
            System.out.println("This test is for Windows and Mac only. Passed.");
            return;
        }

        final Frame sourceFrame = new Frame("Source frame");
        final SourcePanel sourcePanel = new SourcePanel();
        sourceFrame.add(sourcePanel);
        sourceFrame.pack();
        sourceFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sourceFrame.dispose();
            }
        });
        sourceFrame.setVisible(true);

        Util.waitForIdle(null);

        NextFramePositionCalculator positionCalculator = new NextFramePositionCalculator(sourceFrame);

        ArrayList<String> args = new ArrayList<String>(5);
        args.add(String.valueOf(positionCalculator.getNextLocationX()));
        args.add(String.valueOf(positionCalculator.getNextLocationY()));
        args.add(String.valueOf(AbsoluteComponentCenterCalculator.calculateXCenterCoordinate(sourcePanel)));
        args.add(String.valueOf(AbsoluteComponentCenterCalculator.calculateYCenterCoordinate(sourcePanel)));
        args.add(concatStrings(DataFlavorSearcher.RICH_TEXT_NAMES));

        ProcessResults processResults =
                ProcessCommunicator.executeChildProcess(this.getClass(),
                        "." + File.separator + System.getProperty("java.class.path"), args.toArray(new String[]{}));

        verifyTestResults(processResults);

        args.set(args.size() - 1, concatStrings(DataFlavorSearcher.HTML_NAMES));

        ProcessCommunicator.executeChildProcess(this.getClass(),
                "." + File.separator + System.getProperty("java.class.path"), args.toArray(new String[]{}));
        verifyTestResults(processResults);


    }// start()

    private String concatStrings(String[] strings) {
        StringBuffer result = new StringBuffer("\"");
        for (int i = 0; i < strings.length; i++) {
            result.append(strings[i]);
            result.append(",");
        }
        result.append("\"");
        return result.toString();
    }


    private static void verifyTestResults(ProcessResults processResults) {
        if (InterprocessMessages.DATA_IS_CORRUPTED ==
                processResults.getExitValue()) {
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("TEST IS FAILED: Target has received" +
                    " corrupted data.");
        }
        if (InterprocessMessages.NO_DROP_HAPPENED ==
                processResults.getExitValue()) {
            processResults.printProcessErrorOutput(System.err);
            throw new RuntimeException("Error. Drop did not happen." +
                " Target frame is possibly covered by a window of other application." +
                " Please, rerun the test with all windows minimized.");
        }
        processResults.verifyStdErr(System.err);
        processResults.verifyProcessExitValue(System.err);
        processResults.printProcessStandartOutput(System.out);
    }

    //We cannot make an instance of the applet without the default constructor
    public MissedHtmlAndRtfBug() {
        super();
    }

    //We need in this constructor to pass frame position between JVMs
    public MissedHtmlAndRtfBug(Point targetFrameLocation, Point dragSourcePoint, DataFlavor df)
            throws InterruptedException {
        final Frame targetFrame = new Frame("Target frame");
        final TargetPanel targetPanel = new TargetPanel(targetFrame, df);
        targetFrame.add(targetPanel);
        targetFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                targetFrame.dispose();
            }
        });
        targetFrame.setLocation(targetFrameLocation);
        targetFrame.pack();
        targetFrame.setVisible(true);

        doTest(dragSourcePoint, targetPanel);
    }

    private void doTest(Point dragSourcePoint, TargetPanel targetPanel) {
        Util.waitForIdle(null);

        final Robot robot = Util.createRobot();

        robot.mouseMove((int) dragSourcePoint.getX(), (int) dragSourcePoint.getY());
        try {
            sleep(100);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            sleep(100);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Util.drag(robot, dragSourcePoint, new Point(AbsoluteComponentCenterCalculator.calculateXCenterCoordinate(targetPanel),
                AbsoluteComponentCenterCalculator.calculateYCenterCoordinate(targetPanel)),
                InputEvent.BUTTON1_MASK);
    }


    enum InterprocessArguments {
        TARGET_FRAME_X_POSITION_ARGUMENT,
        TARGET_FRAME_Y_POSITION_ARGUMENT,
        DRAG_SOURCE_POINT_X_ARGUMENT,
        DRAG_SOURCE_POINT_Y_ARGUMENT,
        DATA_FLAVOR_NAMES;

        int extractInt(String[] args) {
            return Integer.parseInt(args[this.ordinal()]);
        }

        String[] extractStringArray(String[] args) {
            return args[this.ordinal()].replaceAll("\"", "").split(",");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("main")) {
            new MissedHtmlAndRtfBug().start();
            return;
        }

        Point dragSourcePoint = new Point(InterprocessArguments.DRAG_SOURCE_POINT_X_ARGUMENT.extractInt(args),
                InterprocessArguments.DRAG_SOURCE_POINT_Y_ARGUMENT.extractInt(args));
        Point targetFrameLocation = new Point(InterprocessArguments.TARGET_FRAME_X_POSITION_ARGUMENT.extractInt(args),
                InterprocessArguments.TARGET_FRAME_Y_POSITION_ARGUMENT.extractInt(args));
        String[] names = InterprocessArguments.DATA_FLAVOR_NAMES.extractStringArray(args);

        DataFlavor df = DataFlavorSearcher.getByteDataFlavorForNative(names);
        try {
            new MissedHtmlAndRtfBug(targetFrameLocation, dragSourcePoint, df);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sleep(5000);
        System.exit(InterprocessMessages.NO_DROP_HAPPENED);
    }


}
