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
package org.netbeans.jemmy;

import java.awt.Component;
import java.awt.Container;
import java.util.Vector;

/**
 *
 * Contains methods to search for components below a a given
 * {@code java.awt.Container} in the display containment hierarchy. Uses a
 * {@code ComponentChooser} interface implementation to find a component.
 *
 * @see ComponentChooser
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ComponentSearcher implements Outputable {

    private int ordinalIndex;
    private Container container;
    private TestOut out;
    private QueueTool queueTool;
    private String containerToString;

    /**
     * Contructor. The search is constrained so that only components that lie
     * below the given container in the containment hierarchy are considered.
     *
     * @param c Container to find components in.
     */
    public ComponentSearcher(Container c) {
        super();
        container = c;
        setOutput(JemmyProperties.getProperties().getOutput());
        queueTool = new QueueTool();
    }

    /**
     * Creates {@code ComponentChooser} implementation whose
     * {@code checkComponent(Component)} method returns {@code true}
     * for any component.
     *
     * @param description Component description.
     * @return ComponentChooser instance.
     */
    public static ComponentChooser getTrueChooser(String description) {
        class TrueChooser implements ComponentChooser {

            private String description;

            public TrueChooser(String desc) {
                description = desc;
            }

            @Override
            public boolean checkComponent(Component comp) {
                return true;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String toString() {
                return "TrueChooser{" + "description=" + description + '}';
            }
        }
        return new TrueChooser(description);
    }

    /**
     * Defines print output streams or writers.
     *
     * @param output ?out? Identify the streams or writers used for print
     * output.
     * @see org.netbeans.jemmy.TestOut
     * @see org.netbeans.jemmy.Outputable
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut output) {
        out = output;
    }

    /**
     * Returns print output streams or writers.
     *
     * @return an object that contains references to objects for printing to
     * output and err streams.
     * @see org.netbeans.jemmy.TestOut
     * @see org.netbeans.jemmy.Outputable
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return out;
    }

    /**
     * Returns container.toString(). It is called in dispatch thread.
     *
     * @return container.toString()
     */
    private String containerToString() {
        if (containerToString == null) {
            containerToString = container == null ? "null"
                    : queueTool.invokeSmoothly(
                            new QueueTool.QueueAction<String>("container.toString()") {
                        @Override
                        public String launch() {
                            return container.toString();
                        }
                    }
                    );
        }
        return containerToString;
    }

    /**
     * Searches for a component. The search for the component proceeds
     * recursively in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     *
     * @param chooser ComponentChooser instance, defining and applying the
     * search criteria.
     * @param index Ordinal component index. Indices start at 0.
     * @return the {@code index}'th component from among those components
     * for which the chooser's {@code checkComponent(Component)} method
     * returns {@code true}. A {@code null} reference is returned if
     * there are fewer than {@code index-1} components meeting the search
     * criteria exist in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     */
    public Component findComponent(ComponentChooser chooser, int index) {
        ordinalIndex = 0;
        final Component result = findComponentInContainer(container, chooser, index, null);
        if (result != null) {
            // get result.toString() - run in dispatch thread
            String resultToString = queueTool.invokeSmoothly(
                    new QueueTool.QueueAction<String>("result.toString()") {
                @Override
                public String launch() {
                    return result.toString();
                }
            }
            );
            out.printTrace("Component " + chooser.getDescription()
                    + "\n    was found in container " + containerToString()
                    + "\n    " + resultToString);
            out.printGolden("Component \"" + chooser.getDescription() + "\" was found");
        } else {
            out.printTrace("Component " + chooser.getDescription()
                    + "\n    was not found in container " + containerToString());
            out.printGolden("Component \"" + chooser.getDescription() + "\" was not found");
        }
        return result;
    }

    /**
     * Searches for a component. The search for the component proceeds
     * recursively in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     *
     * @param chooser ComponentChooser instance, defining and applying the
     * search criteria.
     * @return the first component for which the chooser's
     * {@code checkComponent(Component)} method returns {@code true}.
     * A {@code null} reference is returned if no component meeting the
     * search criteria exist in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     */
    public Component findComponent(ComponentChooser chooser) {
        return findComponent(chooser, 0);
    }

    /**
     * Searches for all components. The search for the components proceeds
     * recursively in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     *
     * @param chooser ComponentChooser instance, defining and applying the
     * search criteria.
     * @return the components for which the chooser's
     * {@code checkComponent(Component)} method returns {@code true}.
     * An empty array is returned if no component meeting the search criteria
     * exists in the component hierarchy rooted in this
     * {@code ComponentChooser}'s container.
     */
    public Component[] findComponents(ComponentChooser chooser) {
        Vector<Component> allSeen = new Vector<>();
        findComponentInContainer(container, chooser, 0, allSeen);
        Component[] result = new Component[allSeen.size()];
        for (int i = 0, n = allSeen.size(); i < n; i++) {
            result[i] = allSeen.get(i);
        }
        return result;
    }

    private Component findComponentInContainer(final Container cont, final ComponentChooser chooser, final int index,
            final Vector<Component> allSeen) {
        return queueTool.invokeSmoothly(new QueueTool.QueueAction<Component>("findComponentInContainer with " + chooser.getDescription()) {

            @Override
            public Component launch() throws Exception {
                Component[] components = cont.getComponents();
                Component target;
                for (Component component : components) {
                    if (component != null) {
                        if (chooser.checkComponent(component)) {
                            if (allSeen != null) {
                                allSeen.add(component);
                            } else if (ordinalIndex == index) {
                                return component;
                            } else {
                                ordinalIndex++;
                            }
                        }
                        if (component instanceof Container) {
                            if ((target = findComponentInContainer((Container) component,
                                    chooser, index, allSeen)) != null) {
                                return target;
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

}
