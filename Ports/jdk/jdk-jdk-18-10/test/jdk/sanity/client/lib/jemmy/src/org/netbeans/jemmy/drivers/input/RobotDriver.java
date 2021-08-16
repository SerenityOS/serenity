/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.drivers.input;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;

/**
 * Superclass for all drivers using robot.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class RobotDriver extends LightSupportiveDriver {

    private boolean haveOldPos;
    private boolean smooth = false;
    private double oldX;
    private double oldY;
    private static final double CONSTANT1 = 0.75;
    private static final double CONSTANT2 = 12.0;
    /**
     * A reference to the robot instance.
     */
    protected ClassReference robotReference = null;

    /**
     * A QueueTool instance.
     */
    protected QueueTool qtool;

    protected Timeout autoDelay;

    /**
     * Constructs a RobotDriver object.
     *
     * @param autoDelay Time for {@code Robot.setAutoDelay(long)} method.
     * @param supported an array of supported class names
     */
    public RobotDriver(Timeout autoDelay, String[] supported) {
        super(supported);
        qtool = new QueueTool();
        qtool.setOutput(TestOut.getNullOutput());
        this.autoDelay = autoDelay;
    }

    public RobotDriver(Timeout autoDelay, String[] supported, boolean smooth) {
        this(autoDelay, supported);
        this.smooth = smooth;
    }

    /**
     * Constructs a RobotDriver object.
     *
     * @param autoDelay Time for {@code Robot.setAutoDelay(long)} method.
     */
    public RobotDriver(Timeout autoDelay) {
        this(autoDelay, new String[]{"org.netbeans.jemmy.operators.ComponentOperator"});
    }

    public RobotDriver(Timeout autoDelay, boolean smooth) {
        this(autoDelay);
        this.smooth = smooth;
    }

    public void pressMouse(int mouseButton, int modifiers) {
        pressModifiers(modifiers);
        makeAnOperation("mousePress",
                new Object[]{mouseButton},
                new Class<?>[]{Integer.TYPE});
    }

    public void releaseMouse(int mouseButton, int modifiers) {
        makeAnOperation("mouseRelease",
                new Object[]{mouseButton},
                new Class<?>[]{Integer.TYPE});
        releaseModifiers(modifiers);
    }

    public void moveMouse(int x, int y) {
        if (!smooth) {
            makeAnOperation("mouseMove",
                    new Object[]{x, y},
                    new Class<?>[]{Integer.TYPE, Integer.TYPE});
        } else {
            double targetX = x;
            double targetY = y;
            if (haveOldPos) {
                double currX = oldX;
                double currY = oldY;
                double vx = 0.0;
                double vy = 0.0;
                while (Math.round(currX) != Math.round(targetX)
                        || Math.round(currY) != Math.round(targetY)) {
                    vx = vx * CONSTANT1 + (targetX - currX) / CONSTANT2 * (1.0 - CONSTANT1);
                    vy = vy * CONSTANT1 + (targetY - currY) / CONSTANT2 * (1.0 - CONSTANT1);
                    currX += vx;
                    currY += vy;
                    makeAnOperation("mouseMove", new Object[]{
                                    (int) Math.round(currX),
                                    (int) Math.round(currY)},
                            new Class<?>[]{Integer.TYPE, Integer.TYPE});
                }
            } else {
                makeAnOperation("mouseMove", new Object[]{
                                (int) Math.round(targetX),
                                (int) Math.round(targetY)},
                        new Class<?>[]{Integer.TYPE, Integer.TYPE});
            }
            haveOldPos = true;
            oldX = targetX;
            oldY = targetY;
        }
    }

    public void clickMouse(int x, int y, int clickCount, int mouseButton,
            int modifiers, Timeout mouseClick) {
        pressModifiers(modifiers);
        moveMouse(x, y);
        makeAnOperation("mousePress", new Object[]{mouseButton}, new Class<?>[]{Integer.TYPE});
        for (int i = 1; i < clickCount; i++) {
            makeAnOperation("mouseRelease", new Object[]{mouseButton}, new Class<?>[]{Integer.TYPE});
            makeAnOperation("mousePress", new Object[]{mouseButton}, new Class<?>[]{Integer.TYPE});
        }
        mouseClick.sleep();
        makeAnOperation("mouseRelease", new Object[]{mouseButton}, new Class<?>[]{Integer.TYPE});
        releaseModifiers(modifiers);
    }

    public void dragMouse(int x, int y, int mouseButton, int modifiers) {
        moveMouse(x, y);
    }

    public void dragNDrop(int start_x, int start_y, int end_x, int end_y,
            int mouseButton, int modifiers, Timeout before, Timeout after) {
        moveMouse(start_x, start_y);
        pressMouse(mouseButton, modifiers);
        before.sleep();
        moveMouse(end_x, end_y);
        after.sleep();
        releaseMouse(mouseButton, modifiers);
    }

    /**
     * Presses a key.
     *
     * @param keyCode Key code ({@code KeyEventVK_*} field.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    public void pressKey(int keyCode, int modifiers) {
        pressModifiers(modifiers);
        makeAnOperation("keyPress",
                new Object[]{keyCode},
                new Class<?>[]{Integer.TYPE});
    }

    /**
     * Releases a key.
     *
     * @param keyCode Key code ({@code KeyEventVK_*} field.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    public void releaseKey(int keyCode, int modifiers) {
        releaseModifiers(modifiers);
        makeAnOperation("keyRelease",
                new Object[]{keyCode},
                new Class<?>[]{Integer.TYPE});
    }

    /**
     * Performs a single operation.
     *
     * @param method a name of {@code java.awt.Robot} method.
     * @param params method parameters
     * @param paramClasses method parameters classes
     */
    protected void makeAnOperation(final String method, final Object[] params, final Class<?>[] paramClasses) {
        if (robotReference == null) {
            initRobot();
        }
        try {
            robotReference.invokeMethod(method, params, paramClasses);
            synchronizeRobot();
        } catch (InvocationTargetException
                | IllegalStateException
                | NoSuchMethodException
                | IllegalAccessException e) {
            throw (new JemmyException("Exception during java.awt.Robot accessing", e));
        }
    }

    /**
     * Calls {@code java.awt.Robot.waitForIdle()} method.
     */
    protected void synchronizeRobot() {
        if (!QueueTool.isDispatchThread()) {
            if ((JemmyProperties.getCurrentDispatchingModel() & JemmyProperties.QUEUE_MODEL_MASK) != 0) {
                if (robotReference == null) {
                    initRobot();
                }
                try {
                    robotReference.invokeMethod("waitForIdle", null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Presses modifiers keys by robot.
     *
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    protected void pressModifiers(int modifiers) {
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            pressKey(KeyEvent.VK_SHIFT, modifiers & ~InputEvent.SHIFT_MASK);
        } else if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            pressKey(KeyEvent.VK_ALT_GRAPH, modifiers & ~InputEvent.ALT_GRAPH_MASK);
        } else if ((modifiers & InputEvent.ALT_MASK) != 0) {
            pressKey(KeyEvent.VK_ALT, modifiers & ~InputEvent.ALT_MASK);
        } else if ((modifiers & InputEvent.META_MASK) != 0) {
            pressKey(KeyEvent.VK_META, modifiers & ~InputEvent.META_MASK);
        } else if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            pressKey(KeyEvent.VK_CONTROL, modifiers & ~InputEvent.CTRL_MASK);
        }
    }

    /*
    protected void pressModifiers(ComponentOperator oper, int modifiers) {
        if       ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            pressKey(oper, KeyEvent.VK_SHIFT,     modifiers & ~InputEvent.SHIFT_MASK);
        } else if((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            pressKey(oper, KeyEvent.VK_ALT_GRAPH, modifiers & ~InputEvent.ALT_GRAPH_MASK);
        } else if((modifiers & InputEvent.ALT_MASK) != 0) {
            pressKey(oper, KeyEvent.VK_ALT,       modifiers & ~InputEvent.ALT_MASK);
        } else if((modifiers & InputEvent.META_MASK) != 0) {
            pressKey(oper, KeyEvent.VK_META,      modifiers & ~InputEvent.META_MASK);
        } else if((modifiers & InputEvent.CTRL_MASK) != 0) {
            pressKey(oper, KeyEvent.VK_CONTROL,   modifiers & ~InputEvent.CTRL_MASK);
        }
    }
     */
    /**
     * Releases modifiers keys by robot.
     *
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     */
    protected void releaseModifiers(int modifiers) {
        if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            releaseKey(KeyEvent.VK_SHIFT, modifiers & ~InputEvent.SHIFT_MASK);
        } else if ((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            releaseKey(KeyEvent.VK_ALT_GRAPH, modifiers & ~InputEvent.ALT_GRAPH_MASK);
        } else if ((modifiers & InputEvent.ALT_MASK) != 0) {
            releaseKey(KeyEvent.VK_ALT, modifiers & ~InputEvent.ALT_MASK);
        } else if ((modifiers & InputEvent.META_MASK) != 0) {
            releaseKey(KeyEvent.VK_META, modifiers & ~InputEvent.META_MASK);
        } else if ((modifiers & InputEvent.CTRL_MASK) != 0) {
            releaseKey(KeyEvent.VK_CONTROL, modifiers & ~InputEvent.CTRL_MASK);
        }
    }

    /*
    protected void releaseModifiers(ComponentOperator oper, int modifiers) {
        if       ((modifiers & InputEvent.SHIFT_MASK) != 0) {
            releaseKey(oper, KeyEvent.VK_SHIFT,     modifiers & ~InputEvent.SHIFT_MASK);
        } else if((modifiers & InputEvent.ALT_GRAPH_MASK) != 0) {
            releaseKey(oper, KeyEvent.VK_ALT_GRAPH, modifiers & ~InputEvent.ALT_GRAPH_MASK);
        } else if((modifiers & InputEvent.ALT_MASK) != 0) {
            releaseKey(oper, KeyEvent.VK_ALT,       modifiers & ~InputEvent.ALT_MASK);
        } else if((modifiers & InputEvent.META_MASK) != 0) {
            releaseKey(oper, KeyEvent.VK_META,      modifiers & ~InputEvent.META_MASK);
        } else if((modifiers & InputEvent.CTRL_MASK) != 0) {
            releaseKey(oper, KeyEvent.VK_CONTROL,   modifiers & ~InputEvent.CTRL_MASK);
        }
    }
     */
    private void initRobot() {
        // need to init Robot in dispatch thread because it hangs on Linux
        // (see http://www.netbeans.org/issues/show_bug.cgi?id=37476)
        if (QueueTool.isDispatchThread()) {
            doInitRobot();
        } else {
            qtool.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    doInitRobot();
                }
            });
        }
    }

    private void doInitRobot() {
        try {
            ClassReference robotClassReverence = new ClassReference("java.awt.Robot");
            robotReference = new ClassReference(robotClassReverence.newInstance(null, null));
            robotReference.invokeMethod("setAutoDelay",
                    new Object[]{(int) ((autoDelay != null)
                            ? autoDelay.getValue()
                            : 0)},
                    new Class<?>[]{Integer.TYPE});
        } catch (InvocationTargetException
                | IllegalStateException
                | NoSuchMethodException
                | IllegalAccessException
                | ClassNotFoundException
                | InstantiationException e) {
            throw (new JemmyException("Exception during java.awt.Robot accessing", e));
        }
    }

}
