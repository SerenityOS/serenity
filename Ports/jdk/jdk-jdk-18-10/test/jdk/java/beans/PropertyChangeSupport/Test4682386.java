/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4682386
 * @summary Tests for PropertyChangeSupport refactoring
 * @author Mark Davidson
 */

import java.beans.Beans;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;

import java.lang.reflect.Method;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.JTable;

/**
 * This class tests the multi-threaded access to PropertyChangeSupport and
 * will also use reflection to test propertyChanges on Swing components.
 * <p/>
 * There is no new functionality from the implementation of this RFE.
 * Semantically, it should be equivalent.
 */
public class Test4682386 {
    private static final String FOO = "foo";
    private static final String BAR = "bar";

    private static final int NUM_LISTENERS = 100;
    private static final boolean DEBUG = true;

    private static final Class[] TYPES = {
            JApplet.class,
            JButton.class,
            JCheckBox.class,
            JComboBox.class,
            JLabel.class,
            JList.class,
            JMenuItem.class,
            JProgressBar.class,
            JTextArea.class,
            JTextPane.class,
            JTextField.class,
            JToolBar.class,
            JTabbedPane.class,
            JTree.class,
            JTable.class,
    };

    public static void main(String[] args) {
        testSwingProperties();

        // tests the multi-threaded access

        TestBean bean = new TestBean();

        Thread add = new Thread(new AddThread(bean));
        Thread remove = new Thread(new RemoveThread(bean));
        Thread prop = new Thread(new PropertyThread(bean));

        add.start();
        prop.start();
        remove.start();
    }

    /**
     * Should be exectuted with $JAVA_HOME/lib/dt.jar in the classpath
     * so that there will be a lot more bound properties.
     * <p/>
     * This test isn't really appropriate for automated testing.
     */
    private static void testSwingProperties() {
        long start = System.currentTimeMillis();
        for (Class type : TYPES) {
            try {
                Object bean = Beans.instantiate(type.getClassLoader(), type.getName());

                JComponent comp = (JComponent) bean;
                for (int k = 0; k < NUM_LISTENERS; k++) {
                    comp.addPropertyChangeListener(new PropertyListener());
                }

                for (PropertyDescriptor pd : getPropertyDescriptors(type)) {
                    if (pd.isBound()) {
                        if (DEBUG) {
                            System.out.println("Bound property found: " + pd.getName());
                        }

                        Method read = pd.getReadMethod();
                        Method write = pd.getWriteMethod();
                        try {
                            write.invoke(
                                    bean,
                                    getValue(
                                            pd.getPropertyType(),
                                            read.invoke(bean)));
                        } catch (Exception ex) {
                            // do nothing - just move on.
                            if (DEBUG) {
                                System.out.println("Reflective method invocation Exception for " + type + " : " + ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // do nothing - just move on.
                if (DEBUG) {
                    System.out.println("Exception for " + type.getName() +
                            " : " + ex.getMessage());
                }
            }
        }
        System.out.println("Exec time (ms): " + (System.currentTimeMillis() - start));
    }

    /**
     * Gets a fake value from a type and old value;
     */
    public static Object getValue(Class type, Object value) {
        if (String.class.equals(type)) {
            return "test string";
        }
        if (value instanceof Integer) {
            Integer i = (Integer) value;
            return Integer.valueOf(i + 1);
        }
        if (value instanceof Boolean) {
            Boolean b = (Boolean) value;
            return Boolean.valueOf(!b);
        }
        return null;
    }

    public static PropertyDescriptor[] getPropertyDescriptors(Class type) {
        try {
            return Introspector.getBeanInfo(type).getPropertyDescriptors();
        } catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException exception) {
        }
    }

    private static class AddThread implements Runnable {
        private final TestBean bean;

        AddThread(TestBean bean) {
            this.bean = bean;
        }

        public void run() {
            for (int i = 0; i < NUM_LISTENERS; i++) {
                for (int j = 0; j < 10; j++) {
                    this.bean.addPropertyChangeListener(new PropertyListener());
                }
                if (DEBUG) {
                    System.out.println("10 listeners added");
                }
                sleep(25L);
            }
        }
    }

    private static class RemoveThread implements Runnable {
        private final TestBean bean;

        RemoveThread(TestBean bean) {
            this.bean = bean;
        }

        public void run() {
            for (int k = 0; k < NUM_LISTENERS; k++) {
                sleep(100L);
                PropertyChangeListener[] listeners = this.bean.getPropertyChangeListners();
                for (int i = listeners.length - 1; i >= 0; i--) {
                    this.bean.removePropertyChangeListener(listeners[i]);
                }
                if (DEBUG) {
                    System.out.println(listeners.length + " listeners removed");
                }
            }
        }
    }

    private static class PropertyThread implements Runnable {
        private final TestBean bean;

        PropertyThread(TestBean bean) {
            this.bean = bean;
        }

        public void run() {
            for (int i = 0; i < NUM_LISTENERS; i++) {
                boolean flag = this.bean.isFoo();
                this.bean.setFoo(!flag);
                this.bean.setBar(Boolean.toString(flag));
                if (DEBUG) {
                    System.out.println("Executed property changes");
                }
                sleep(40L);
            }
        }
    }

    /**
     * Handler for the property change events.
     */
    private static class PropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            // blank since this should execute as fast as possible.
        }
    }

    /**
     * A simple bean to test multi-threaded acccess to the listeners.
     */
    public static class TestBean {
        private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private boolean foo;
        private String bar;

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
        }

        public PropertyChangeListener[] getPropertyChangeListners() {
            return this.pcs.getPropertyChangeListeners();
        }

        public boolean isFoo() {
            return this.foo;
        }

        public void setFoo(boolean foo) {
            boolean old = this.foo;
            this.foo = foo;
            this.pcs.firePropertyChange(FOO, old, foo);
        }

        public String getBar() {
            return this.bar;
        }

        public void setBar(String bar) {
            String old = this.bar;
            this.bar = bar;
            this.pcs.firePropertyChange(BAR, old, bar);
        }
    }
}
