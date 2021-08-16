/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import javax.swing.*;

/*
 * @test
 * @key headful
 * @summary Toplevel should be correctly positioned as relative to a component:
 *          so that their centers coincide
 *          or, if the component is hidden, centered on the screen.
 * @bug 8036915
 * @library /lib/client
 * @build ExtendedRobot
 * @run main/timeout=1200 SetLocationRelativeToTest
 */

public class SetLocationRelativeToTest {
    private static int delay = 500;
    private static boolean testEverything = false;// NB: change this to true to test everything
    java.util.List<Window> awtToplevels = new ArrayList<Window>();
    java.util.List<Window> swingToplevels = new ArrayList<Window>();
    java.util.List<Window> allToplevels = new ArrayList<Window>();
    java.util.List<Component> awtComponents = new ArrayList<Component>();
    java.util.List<Component> swingComponents = new ArrayList<Component>();
    java.util.List<Component> allComponents = new ArrayList<Component>();
    Label placeholder = new Label();
    JLabel jplaceholder = new JLabel();
    JFrame jcontainer;
    public SetLocationRelativeToTest() {
        Frame frame = new Frame("Frame");
        frame.setSize(200,100);
        Frame uframe = new Frame("U.Frame");
        uframe.setUndecorated(true);
        uframe.setSize(200,100);
        Window window = new Window(frame);
        window.setSize(200,100);
        Dialog dialog = new Dialog(frame, "Dialog");
        dialog.setSize(200,100);
        awtToplevels.add(frame);
        awtToplevels.add(uframe);
        awtToplevels.add(window);
        awtToplevels.add(dialog);

        awtComponents.add(new TextArea("Am a TextArea"));
        awtComponents.add(new TextField("Am a TextField"));
        awtComponents.add(new Button("Press"));
        awtComponents.add(new Label("Label"));
        Choice aChoice = new Choice();
        aChoice.add("One");
        aChoice.add("Two");
        awtComponents.add(aChoice);
        awtComponents.add(new Canvas());
        awtComponents.add(new List(4));
        awtComponents.add(new Checkbox("Me CheckBox"));
        awtComponents.add(new Scrollbar());

        swingComponents.add(new JTextArea("Am a JTextArea"));
        swingComponents.add(new JTextField("Am a JTextField"));
        swingComponents.add(new JButton("Press"));
        swingComponents.add(new JLabel("JLabel"));
        JComboBox jcombo = new JComboBox();
        swingComponents.add(jcombo);
        swingComponents.add(new JPanel());
        swingComponents.add(new JList());
        swingComponents.add(new JCheckBox("Me JCheckBox"));
        swingComponents.add(new JScrollBar());
    }

    public static void main(String args[]) {
        SetLocationRelativeToTest test = new SetLocationRelativeToTest();
        test.doAWTTest(true);
        test.doAWTTest(false);
        try {
            test.doSwingTest(true);
            test.doSwingTest(false);
        }catch(InterruptedException ie) {
            ie.printStackTrace();
        }catch(java.lang.reflect.InvocationTargetException ite) {
            ite.printStackTrace();
            throw new RuntimeException("InvocationTarget?");
        }
        return;
    }

    // In regular testing, we select just few components to test
    // randomly. If full testing required, select many ("all").
    void selectObjectsToTest(boolean doSwing) {
        allToplevels.clear();
        allComponents.clear();
        if(testEverything) {
            allToplevels.addAll(0, awtToplevels);
            allComponents.addAll(0, awtComponents);
            if(doSwing) {
                allToplevels.addAll(allToplevels.size(), swingToplevels);
                allComponents.addAll(allComponents.size(), swingComponents);
            }
        }else{
            //select a random of each
            int i = (int)(java.lang.Math.random()*awtToplevels.size());
            allToplevels.add(awtToplevels.get(i));
            i = (int)(java.lang.Math.random()*awtComponents.size());
            allComponents.add(awtComponents.get(i));
            if(doSwing) {
                i = (int)(java.lang.Math.random()*swingToplevels.size());
                allToplevels.add(swingToplevels.get(i));
                i = (int)(java.lang.Math.random()*swingComponents.size());
                allComponents.add(swingComponents.get(i));
            }
        }
    }

    // create Frame, add an AWT component to it,
    // hide it (or not) and position a new toplevel
    // relativeTo
    void doAWTTest(boolean isHidden) {
        boolean res;
        ExtendedRobot robot;
        try {
            robot = new ExtendedRobot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed: "+ex.getMessage());
        }
        Frame container = new Frame("Frame");
        container.setBounds(100,100,300,300);
        container.setLayout(new GridLayout(3,1));
        container.add(placeholder);
        container.setVisible(true);
        selectObjectsToTest(false);
        for(Component c: allComponents) {
            placeholder.setText((isHidden ? "Hidden: " : "Below is ")+ c.getClass().getName());
            c.setVisible(true);
            container.add(c);
            container.doLayout();
            if(isHidden) {
                c.setVisible(false);
            }
            robot.waitForIdle(delay);
            for(Window w: allToplevels) {
                w.setLocationRelativeTo(c);
                w.setVisible(true);
                robot.waitForIdle(delay);
                res = compareLocations(w, c, robot);
                System.out.println(c.getClass().getName()+"   \t: "+w.getClass().getName()+
                    ((w instanceof Frame) && (((Frame)w).isUndecorated()) ? " undec\t\t:" : "\t\t:")+" "+
                    (res ? "" : "Failed"));
                if(!res) {
                    throw new RuntimeException("Test failed.");
                }
                w.dispose();
            }
            container.remove(c);
            robot.waitForIdle(delay);
        }
        container.dispose();
    }

    // Create JFrame, add an AWT or Swing component to it,
    // hide it (or not) and position a new toplevel
    // relativeTo
    void doSwingTest(boolean isHidden) throws InterruptedException,
                       java.lang.reflect.InvocationTargetException {
        boolean res;
        ExtendedRobot robot;
        try {
            robot = new ExtendedRobot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed: "+ex.getMessage());
        }

        EventQueue.invokeAndWait( () -> {
            JFrame jframe = new JFrame("jframe");
            jframe.setSize(200,100);
            swingToplevels.add(jframe);
            JFrame ujframe = new JFrame("ujframe");
            ujframe.setSize(200,100);
            ujframe.setUndecorated(true);
            swingToplevels.add(ujframe);
            JWindow jwin = new JWindow();
            jwin.setSize(200,100);
            swingToplevels.add(jwin);
            JDialog jdia = new JDialog((Frame)null, "JDialog");
            jdia.setSize(200,100);
            swingToplevels.add(jdia);
            jcontainer = new JFrame("JFrame");
            jcontainer.setBounds(100,100,300,300);
            jcontainer.setLayout(new GridLayout(3,1));
            jcontainer.add(jplaceholder);
            jcontainer.setVisible(true);
            selectObjectsToTest(true);
        });
        robot.waitForIdle(delay);

        for(Component c: allComponents) {
            EventQueue.invokeAndWait( () -> {
                jplaceholder.setText((isHidden ? "Hidden: " : "Below is: ")+ c.getClass().getName());
                c.setVisible(true);
                jcontainer.add(c);
                jcontainer.doLayout();
                if(isHidden) {
                    c.setVisible(false);
                }
            });
            robot.waitForIdle(delay);
            for(Window w: allToplevels) {
                EventQueue.invokeAndWait( () -> {
                    w.setLocationRelativeTo(c);
                    w.setVisible(true);
                });
                robot.waitForIdle(delay);
                res = compareLocations(w, c, robot);
                System.out.println(c.getClass().getName()+"   \t: "+w.getClass().getName()+
                    ((w instanceof Frame) && (((Frame)w).isUndecorated()) ? " undec\t\t:" : "\t\t:")+" "+
                    (res ? "" : "Failed"));
                EventQueue.invokeAndWait( () -> {
                    w.dispose();
                });
                robot.waitForIdle();
                if(!res) {
                    throw new RuntimeException("Test failed.");
                }
            }
            EventQueue.invokeAndWait( () -> {
                jcontainer.remove(c);
            });
            robot.waitForIdle(delay);
        }
        EventQueue.invokeAndWait( () -> {
            jcontainer.dispose();
        });
    }

    // Check, finally, if w  either is concentric with c
    // or sits in the center of the screen (if c is hidden)
    boolean compareLocations(final Window w, final Component c, ExtendedRobot robot) {
        final Point pc = new Point();
        final Point pw = new Point();
        try {
            EventQueue.invokeAndWait( () -> {
                pw.setLocation(w.getLocationOnScreen());
                pw.translate(w.getWidth()/2, w.getHeight()/2);
                if(!c.isVisible()) {
                    Rectangle screenRect = w.getGraphicsConfiguration().getBounds();
                    pc.setLocation(screenRect.x+screenRect.width/2,
                                   screenRect.y+screenRect.height/2);
                }else{
                    pc.setLocation(c.getLocationOnScreen());
                    pc.translate(c.getWidth()/2, c.getHeight()/2);
                }
            });
        } catch(InterruptedException ie) {
            throw new RuntimeException("Interrupted");
        } catch(java.lang.reflect.InvocationTargetException ite) {
            ite.printStackTrace();
            throw new RuntimeException("InvocationTarget?");
        }
        robot.waitForIdle(delay);
        // Compare with 1 tolerance to forgive possible rounding errors
        if(pc.x - pw.x > 1 ||
           pc.x - pw.x < -1 ||
           pc.y - pw.y > 1 ||
           pc.y - pw.y < -1 ) {
            System.out.println("Center of "+(c.isVisible() ? "Component:" : "screen:")+pc);
            System.out.println("Center of Window:"+pw);
            System.out.println("Centers of "+w+" and "+c+" do not coincide");
            return false;
        }
        return true;
    }
}
