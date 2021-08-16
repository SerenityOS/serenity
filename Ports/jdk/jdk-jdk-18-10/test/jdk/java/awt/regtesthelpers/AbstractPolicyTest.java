/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p> This class is used to test a focus traversal policy implementation.
 * <p> When using jtreg you should include this class via something like:
 * <pre>
 * @library ../../../regtesthelpers
 * @build AbstractPolicyTest
 * @run main YourTest
 * </pre>
 * <p> And put "import test.java.awt.regtesthelpers.AbstractPolicyTest;" into the test.
 */

package test.java.awt.regtesthelpers;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPolicyTest {

    /** Creates a new instance of AbstractPolicyTest */
    protected AbstractPolicyTest() {
    }

    Map<String, Component> registered_comps = new HashMap<String, Component>();

    protected abstract Frame createFrame();
    protected abstract void customizeHierarchy();

    protected abstract Map<String, String> getForwardOrder();
    protected abstract Map<String, String> getBackwardOrder();

    protected abstract String[] getContainersToTest();
    protected abstract String getDefaultComp(String focusCycleRoot_id);
    protected abstract String getFirstComp(String focusCycleRoot_id);
    protected abstract String getLastComp(String focusCycleRoot_id);

    protected final Component registerComponent(final String id, final Component comp) {
        if (registered_comps.containsKey(id)) {
            throw new RuntimeException("The component with id (" + id + "), already registered.");
        }
        comp.setName(id);
        registered_comps.put(id, comp);
        return comp;
    }

    public void testIt() {
        Frame frame = createFrame();
        customizeHierarchy();
        try {
            frame.pack();
            frame.setVisible(true);
            testPolicy(getForwardOrder(), getBackwardOrder());
        } finally {
            frame.dispose();
        }
    }

    void testPolicy(final Map<String, String> forward_order, final Map<String, String> backward_order)
    {
        if (getContainersToTest() != null)
            for (String cont_id : getContainersToTest()) {
                final Container cont = (Container) getComponent(cont_id);
                FocusTraversalPolicy policy = cont.getFocusTraversalPolicy();
                assertEquals(cont_id, "Test default component", getComponent(getDefaultComp(cont_id)), policy.getDefaultComponent(cont));
                assertEquals(cont_id, "Test first component", getComponent(getFirstComp(cont_id)), policy.getFirstComponent(cont));
                assertEquals(cont_id, "Test last component", getComponent(getLastComp(cont_id)), policy.getLastComponent(cont));
            }
        if (forward_order != null)
            for (String key : forward_order.keySet()) {
                final Component current = getComponent(key);
                final Component next = getComponent(forward_order.get(key));
                Container focusCycleRoot = current.getParent() == null ? (Container)current : current.getFocusCycleRootAncestor();
                FocusTraversalPolicy policy = focusCycleRoot.getFocusTraversalPolicy();
                assertEquals(null, "Test getComponentAfter() for " + key, next, policy.getComponentAfter(focusCycleRoot, current));
            }
        if (backward_order != null)
            for (String key : backward_order.keySet()) {
                final Component current = getComponent(key);
                final Component previous = getComponent(backward_order.get(key));
                Container focusCycleRoot = current.getParent() == null ? (Container)current : current.getFocusCycleRootAncestor();
                FocusTraversalPolicy policy = focusCycleRoot.getFocusTraversalPolicy();
                assertEquals(null, "Test getComponentBefore() for " + key, previous, policy.getComponentBefore(focusCycleRoot, current));
            }
    }

    protected final Component getComponent(final String id) {
        if (!registered_comps.containsKey(id)) {
            throw new RuntimeException("There is no registered component with given id(" + id +")");
        }
        return registered_comps.get(id);
    }

    void assertEquals(final String message, final Object expected, final Object actual) {
        assertEquals(null, message, expected, actual);
    }

    void assertEquals(final String cont_id, final String message, final Object expected, final Object actual) {
        if (actual == null && expected == null
            || actual != null && actual.equals(expected))
        {
            // every thing ok.
            return;
        }
        throw new RuntimeException((cont_id != null ? (cont_id + ": ") : "") + message +
                                   "(actual = " + actual + ", expected = " + expected + ")");
    }
}
