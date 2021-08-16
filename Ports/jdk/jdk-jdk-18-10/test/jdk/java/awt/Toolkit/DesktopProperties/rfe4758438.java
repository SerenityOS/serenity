/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;

/*
 * @test
 * @key headful
 * @bug 4758438
 * @summary Testcase to check the implementation of RFE 4758438
 *          The RFE suggests that the GNOME desktop properties
 *          should be made accessible through the
 *          Toolkit.getDesktopProperty() API.
 * @author Girish R (girish.ramachandran@sun.com)
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run shell rfe4758438.sh
 */

public class rfe4758438 implements PropertyChangeListener {

    enum PROPS {
        drag_threshold(
                "org.gnome.settings-daemon.peripherals.mouse drag-threshold",
                "/desktop/gnome/peripherals/mouse/drag_threshold",
                "gnome.Net/DndDragThreshold",
                "int",
                new String[]{"5", "6"}),
        double_click(
                "org.gnome.settings-daemon.peripherals.mouse double-click",
                "/desktop/gnome/peripherals/mouse/double_click",
                "gnome.Net/DoubleClickTime",
                "int",
                new String[]{"200","300"}),
        cursor_blink(
                "org.gnome.desktop.interface cursor-blink",
                "/desktop/gnome/interface/cursor_blink",
                "gnome.Net/CursorBlink",
                "bool",
                new String[]{"true","false"}),
        cursor_blink_time(
                "org.gnome.desktop.interface cursor-blink-time",
                "/desktop/gnome/interface/cursor_blink_time",
                "gnome.Net/CursorBlinkTime",
                "int",
                new String[]{"1000","1500"}),
        gtk_theme(
                "org.gnome.desktop.interface gtk-theme",
                "/desktop/gnome/interface/gtk_theme",
                "gnome.Net/ThemeName",
                "string",
                new String[]{"Crux","Simple"});

        public final String gsettings;
        public final String gconftool;
        public final String java;
        public final String type;
        public final String[] values;

        PROPS(String gsettings, String gconftool, String java, String type, String[] values){
            this.gsettings = gsettings;
            this.gconftool = gconftool;
            this.java = java;
            this.type = type;
            this.values = values;
        }
    }

    static boolean useGsettings;
    static String tool;
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    String changedProperty;
    Object changedValue;
    Object lock = new Object();

    /**
     * Implementation of PropertyChangeListener method
     */
    public void propertyChange(PropertyChangeEvent event) {
        changedProperty = event.getPropertyName();
        changedValue = toolkit.getDesktopProperty(changedProperty);
        System.out.println("Property "+changedProperty+" changed. Changed value: "+changedValue);
        synchronized(lock) {
            try {
                lock.notifyAll();
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        useGsettings = System.getProperty("useGsettings").equals("true");
        tool = System.getProperty("tool");

        String osName = System.getProperty("os.name");
        if (!"Linux".equals(osName))
            System.out.println("This test need not be run on this platform");
        else
            new rfe4758438().doTest();
    }

    void doTest() throws Exception {
        for (PROPS p : PROPS.values())
            toolkit.addPropertyChangeListener(p.java, this);

        for (PROPS p : PROPS.values()) {
            Thread.sleep(1000);
            doTest(p);
        }
        System.out.println("Test passed");
    }

    /**
     * Do the test for each property. Find the current value
     * of the property, set the property to a value not equal
     * to the current value, check if the propertyChange event
     * is triggered. Reset the property to the actual value.
     */
    void doTest(PROPS property) throws Exception {
        //Choose the test value which is not same as the current value
        Object obj = toolkit.getDesktopProperty(property.java);
        if (obj == null)
            throw new RuntimeException("No such property available: " + property.java);

        //For boolean type values, getDesktopProperty method returns Integer objects
        if (property.type.equals("bool")) {
            if (obj.equals(new Integer(1))) {
                obj = new String("true");
            } else {
                obj = new String("false");
            }
        }
        Object value = property.values[0];
        if (obj.toString().equals(value)) {
            value = property.values[1];
        }

        //Create the command to execute
        StringBuffer sb = new StringBuffer(tool);
        if (useGsettings) {
            sb.append(" set ");
            sb.append(property.gsettings);
            sb.append(" ");
        } else {
            sb.append(" --set --type=");
            sb.append(property.type);
            sb.append(" ");
            sb.append(property.gconftool);
            sb.append(" ");
        }
        String tempCommand = sb.toString();
        sb.append(value.toString());

        //Initialize the variables and execute the command
        changedProperty = "";
        changedValue = null;
        if (executeCommand(sb.toString()) != 0)
            throw new RuntimeException("Could not execute the command");

        synchronized(lock) {
            try {
                lock.wait(5000);
            } catch (Exception e) {
            }
        }
        if (property.type.equals("bool")) {
            if (changedValue.equals(new Integer(1))) {
                changedValue = new String("true");
            } else {
                changedValue = new String("false");
            }
        }

        //Check if the event got triggered
        if (!changedProperty.equals(property.java)) {
            //Reset the property
            executeCommand(tempCommand + obj.toString());
            throw new RuntimeException("PropertyChangedEvent did not occur for " + property.java);
        } else if (!changedValue.toString().equals(value.toString())) {
            //Reset the property
            executeCommand(tempCommand + obj.toString());
            throw new RuntimeException("New value of the property is different from " +
                                       "the value supplied");
        }

        //Reset the property
        executeCommand(tempCommand + obj.toString());
    }

    /**
     * Uses the gconftool-2 command to change the value of the property.
     * Gets the output of the command and prints the output
     */
    int executeCommand(String command) throws Exception {
        System.out.println("Executing " + command);
        Process process = Runtime.getRuntime().exec(command);

        InputStream is = process.getInputStream();
        InputStream es = process.getErrorStream();
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        process.waitFor();

        while (is.available() > 0)
            stdout.append((char) is.read());

        while (es.available() > 0)
            stderr.append((char) es.read());

        if (stdout.length() > 0)
            System.out.println(stdout.toString());
        if (stderr.length() > 0)
            System.err.println(stderr.toString());
        return process.exitValue();
    }
}
