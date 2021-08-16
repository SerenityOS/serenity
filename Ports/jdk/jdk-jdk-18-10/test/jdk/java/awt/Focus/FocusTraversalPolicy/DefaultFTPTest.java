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
  @bug       6463545
  @summary   Tests java.awt.DefaultFocusTraversalPolicy functionality.
  @author    anton.tarasov area=awt.focus
  @library   ../../regtesthelpers
  @build     AbstractPolicyTest
  @run       main DefaultFTPTest
*/

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import test.java.awt.regtesthelpers.AbstractPolicyTest;

/*

Below are some notes about changes in DefaultFocusTraversalPolicy behaviour.

container(root) [...]           - focus traversal cycle with the <container> as the root.
container(provider) [...]       - focus traversal cycle with the <container> as the provider.
container(..)(focusable) [...]  - <container> is implicitly set focusable.
comp[unfocusable]               - <comp> is set unfocusable.


1. frame [ container(root)(focusable) [...] ]

- getComponentAfter(<frame>, <container>) returns <container>.

  If <container> is the default component to focus in its own cycle.  * NO CHANGE *


3. frame [ comp1 container(root)(focusable) [ comp2 ] comp3 ]

- getComponentBefore(<frame>, <comp3>) returns <comp2>.                                 ** BEHAVIOUR CHANGE **

  Previously <container> would be returned. This was a bug as it
  wasn't according to the spec.

- getComponentBefore(<container>, <comp2>) returns <container>.     * NO CHANGE *

- getComponentBefore(<frame>, <container>) returns <comp1>.         * NO CHANGE *

- getComponentBefore(<container>, <container>) returns <comp2>.     * NO CHANGE *


4. frame [ container(provider) [...] comp ]

- getComponentAfter(<frame>, <container>) returns <container>'s default.                ** BEHAVIOUR CHANGE. SPEC ADDITION **

  Previously <comp> would be returned. Not specified in the spec.

- getComponentBefore(<frame>, <comp>) returns <container>'s last.                       ** SPEC CHANGE **

  The spec says (incorrectly) that default should be returned.


5. frame [ container(provider)(focusable) [...] comp2 ]

- getComponentBefore(<frame>, <comp2>) returns <container>'s last.                      ** BEHAVIOUR CHANGE. SPEC ADDITION **

  Previously <container> would be returned. Not specified in the spec.


6. frame [ comp1 container(root) [...] comp2 ]

- getComponentAfter(<frame>, <comp1>) returns <container>'s default.                    ** BEHAVIOUR CHANGE. SPEC ADDITION **

  Previously <comp2> would be returned. It's just the fix for 6240842.
  Not specified in the spec.


7. frame [ comp1 container(root) [...] comp2(unfocusable) comp3 ]

- getComponentBefore(<frame>, <comp3>) returns <container>'s default.                   ** BEHAVIOUR CHANGE **

  Previously <comp1> would be returned. This was a bug, because
  in case if <comp2> is focusable getComponentBefore(<frame>, <comp2>) would
  return <container>'s default.

*/

public class DefaultFTPTest {
    final int TESTS_NUMBER = 11;

    public static void main(String[] args) {
        DefaultFTPTest app = new DefaultFTPTest();
        app.start();
    }

    public void start() {
        try {
            Class clazz = null;
            AbstractPolicyTest test = null;

            for (int i = 1; i <= TESTS_NUMBER; i++) {
                clazz = Class.forName("PolicyTest" + i);
                if (clazz != null) {
                    test = (AbstractPolicyTest)clazz.newInstance();
                    System.out.print("Test " + i + " is in progress...");
                    test.testIt();
                    System.out.println(" passed.");
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new RuntimeException("Error: unexpected exception cought!", e);
        }
    }
}

/*
 * frame [ container1 [...] container2 [...] container3 [...] ]
 * - verifies simple configuration.
 */
class PolicyTest1 extends AbstractPolicyTest {
    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new GridLayout(3, 1));

        for (int i = 0; i < 3; i++) {
            Container cont = (Container) registerComponent("panel" + i, new Panel());
            for (int j = 0; j < 3; j++) {
                cont.add(registerComponent("btn " + (j + i*100), new Button("button")));
            }
            frame.add(cont);
        }
        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 0", "btn 1");
        order.put("btn 1", "btn 2");
        order.put("btn 2", "btn 100");
        order.put("btn 100", "btn 101");
        order.put("btn 101", "btn 102");
        order.put("btn 102", "btn 200");
        order.put("btn 200", "btn 201");
        order.put("btn 201", "btn 202");
        order.put("btn 202", "btn 0");
        order.put("panel0", "btn 0");
        order.put("panel1", "btn 100");
        order.put("panel2", "btn 200");
        order.put("frame", "btn 0");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 0", "btn 202");
        order.put("btn 1", "btn 0");
        order.put("btn 2", "btn 1");
        order.put("btn 100", "btn 2");
        order.put("btn 101", "btn 100");
        order.put("btn 102", "btn 101");
        order.put("btn 200", "btn 102");
        order.put("btn 201", "btn 200");
        order.put("btn 202", "btn 201");
        order.put("panel0", "btn 202");
        order.put("panel1", "btn 2");
        order.put("panel2", "btn 102");
        order.put("frame", "btn 202");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        return "btn 0";
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return "btn 0";
    }

    protected String getLastComp(String focusCycleRoot_id) {
        return "btn 202";
    }
}

/*
 * frame [ comp container(provider) [...] comp ]
 * - transfering focus through a provider.
 */
class PolicyTest2 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn 1", new Button("button")));

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 2", new Button("button")));
        cont.add(registerComponent("btn 3", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusTraversalPolicyProvider(true);
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn 1");
        order.put("btn 1", "btn 2");
        order.put("btn 2", "btn 3");
        order.put("btn 3", "btn 4");
        order.put("btn 4", "btn 1");
        order.put("panel", "btn 2");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 3");
        order.put("btn 3", "btn 2");
        order.put("btn 2", "btn 1");
        order.put("btn 1", "btn 4");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame", "panel"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn 1";
        } else if ("panel".equals(focusCycleRoot_id)) {
            return "btn 2";
        }
        return null;
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return getDefaultComp(focusCycleRoot_id);
    }

    protected String getLastComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn 4";
        } else if ("panel".equals(focusCycleRoot_id)) {
            return "btn 3";
        }
        return null;
    }
}

/*
 * frame [ comp container(root) [...] comp ]
 * - transfering focus through a root (includes the case reported in the CR 6240842).
 */
class PolicyTest3 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn 1", new Button("button")));

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 2", new Button("button")));
        cont.add(registerComponent("btn 3", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusCycleRoot(true);
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn 1");
        order.put("btn 1", "btn 2");
        order.put("btn 2", "btn 3");
        order.put("btn 3", "btn 2");
        order.put("btn 4", "btn 1");
        order.put("panel", "btn 2");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 2");
        order.put("btn 3", "btn 2");
        order.put("btn 2", "btn 3");
        order.put("btn 1", "btn 4");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame", "panel"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn 1";
        } else if ("panel".equals(focusCycleRoot_id)) {
            return "btn 2";
        }
        return null;
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return getDefaultComp(focusCycleRoot_id);
    }

    protected String getLastComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn 4";
        } else if ("panel".equals(focusCycleRoot_id)) {
            return "btn 3";
        }
        return null;
    }
}

/*
 * frame [ container(provider) [...] comp1(unfocusable) comp2 ]
 * - getComponentBefore(<frame>, <comp2>) should return <container>'s last.
 */
class PolicyTest4 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 1", new Button("button")));
        cont.add(registerComponent("btn 2", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 3", new Button("button")));
        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusTraversalPolicyProvider(true);
        ((Button)getComponent("btn 3")).setFocusable(false);
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 2");
        order.put("btn 2", "btn 1");
        order.put("btn 1", "btn 4");
        return order;
    }

    // no testing
    protected Map<String, String> getForwardOrder() {
        return null;
    }
    protected String[] getContainersToTest() {
        return null;
    }
    protected String getDefaultComp(String focusCycleRoot_id) {
        return null;
    }
    protected String getFirstComp(String focusCycleRoot_id) {
        return null;
    }
    protected String getLastComp(String focusCycleRoot_id) {
        return null;
    }
}

/*
 * frame [ container(root) [...] comp1(unfocusable) comp2 ]
 * - getComponentBefore(<frame>, <comp2>) should return <container>'s default.
 */
class PolicyTest5 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 1", new Button("button")));
        cont.add(registerComponent("btn 2", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 3", new Button("button")));
        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusCycleRoot(true);
        ((Button)getComponent("btn 3")).setFocusable(false);
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 1");
        order.put("btn 2", "btn 1");
        order.put("btn 1", "btn 2");
        return order;
    }

    // no testing
    protected Map<String, String> getForwardOrder() {
        return null;
    }
    protected String[] getContainersToTest() {
        return null;
    }
    protected String getDefaultComp(String focusCycleRoot_id) {
        return null;
    }
    protected String getFirstComp(String focusCycleRoot_id) {
        return null;
    }
    protected String getLastComp(String focusCycleRoot_id) {
        return null;
    }
}

/*
 * frame [ comp container(provider)(focusable) [...] comp ]
 * - transfering focus through a focusable provider.
 */
class PolicyTest6 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn 1", new Button("button")));

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 2", new Button("button")));
        cont.add(registerComponent("btn 3", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn 2");
                }
            });
        ((Container)getComponent("panel")).setFocusTraversalPolicyProvider(true);
        ((Container)getComponent("panel")).setFocusable(true);
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn 1");
        order.put("btn 1", "panel");
        order.put("btn 2", "btn 3");
        order.put("btn 3", "btn 4");
        order.put("btn 4", "btn 1");
        order.put("panel", "btn 2");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 3");
        order.put("btn 3", "btn 2");
        order.put("btn 2", "panel");
        order.put("btn 1", "btn 4");
        order.put("panel", "btn 1");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"panel"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        return "btn 2";
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return "panel";
    }

    protected String getLastComp(String focusCycleRoot_id) {
        return "btn 3";
    }
}

/*
 * frame [ comp container(root)(focusable) [...] comp ]
 * - transfering focus through a focusable root.
 */
class PolicyTest7 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn 1", new Button("button")));

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn 2", new Button("button")));
        cont.add(registerComponent("btn 3", new Button("button")));
        frame.add(cont);

        frame.add(registerComponent("btn 4", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn 2");
                }
            });
        ((Container)getComponent("panel")).setFocusCycleRoot(true);
        ((Container)getComponent("panel")).setFocusable(true);
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn 1");
        order.put("btn 1", "panel");
        order.put("btn 2", "btn 3");
        order.put("btn 3", "panel");
        order.put("btn 4", "btn 1");
        order.put("panel", "btn 2");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn 4", "btn 2");
        order.put("btn 3", "btn 2");
        order.put("btn 2", "panel");
        order.put("btn 1", "btn 4");
        order.put("panel", "btn 1");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"panel"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        return "btn 2";
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return "panel";
    }

    protected String getLastComp(String focusCycleRoot_id) {
        return "btn 3";
    }
}

/*
 * frame [ comp1 comp2 container1(provider) [...] container2(root) [...] ]
 * - verifies a case when a provider is followed by a root.
 */
class PolicyTest8 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn-1", new Button("button")));
        frame.add(registerComponent("btn-2", new Button("button")));

        Container cont1 = (Container)registerComponent("panel-1", new Panel());
        cont1.add(registerComponent("btn-3", new Button("button")));
        cont1.add(registerComponent("btn-4", new Button("button")));
        cont1.add(registerComponent("btn-5", new Button("button")));

        Container cont2 = (Container)registerComponent("panel-2", new Panel());
        cont2.add(registerComponent("btn-6", new Button("button")));
        cont2.add(registerComponent("btn-7", new Button("button")));
        cont2.add(registerComponent("btn-8", new Button("button")));

        frame.add(cont1);
        frame.add(cont2);

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("panel-1")).setFocusTraversalPolicyProvider(true);
        ((Container)getComponent("panel-1")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn-4");
                }
            });

        ((Container)getComponent("panel-2")).setFocusCycleRoot(true);
        ((Container)getComponent("panel-2")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn-7");
                }
            });
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn-1");
        order.put("btn-1", "btn-2");
        order.put("btn-2", "btn-4");
        order.put("btn-3", "btn-4");
        order.put("btn-4", "btn-5");
        order.put("btn-5", "btn-7");
        order.put("btn-6", "btn-7");
        order.put("btn-7", "btn-8");
        order.put("btn-8", "btn-6");
        order.put("panel-1", "btn-4");
        order.put("panel-2", "btn-7");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn-1", "btn-5");
        order.put("btn-2", "btn-1");
        order.put("btn-3", "btn-2");
        order.put("btn-4", "btn-3");
        order.put("btn-5", "btn-4");
        order.put("btn-6", "btn-8");
        order.put("btn-7", "btn-6");
        order.put("btn-8", "btn-7");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame", "panel-1", "panel-2"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-4";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-7";
        }
        return null;
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-3";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-6";
        }
        return null;
    }

    protected String getLastComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-5";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-5";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-8";
        }
        return null;
    }
}

/*
 * frame [ comp1 comp2 container1(root) [...] container2(provider) [...] ]
 * - verifies a case when a root is followed by a provider.
 */
class PolicyTest9 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        frame.add(registerComponent("btn-1", new Button("button")));
        frame.add(registerComponent("btn-2", new Button("button")));

        Container cont1 = (Container)registerComponent("panel-1", new Panel());
        cont1.add(registerComponent("btn-3", new Button("button")));
        cont1.add(registerComponent("btn-4", new Button("button")));
        cont1.add(registerComponent("btn-5", new Button("button")));

        Container cont2 = (Container)registerComponent("panel-2", new Panel());
        cont2.add(registerComponent("btn-6", new Button("button")));
        cont2.add(registerComponent("btn-7", new Button("button")));
        cont2.add(registerComponent("btn-8", new Button("button")));

        frame.add(cont1);
        frame.add(cont2);

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("panel-1")).setFocusCycleRoot(true);
        ((Container)getComponent("panel-1")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn-4");
                }
            });

        ((Container)getComponent("panel-2")).setFocusTraversalPolicyProvider(true);
        ((Container)getComponent("panel-2")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("btn-7");
                }
            });
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn-1");
        order.put("btn-1", "btn-2");
        order.put("btn-2", "btn-4");
        order.put("btn-3", "btn-4");
        order.put("btn-4", "btn-5");
        order.put("btn-5", "btn-3");
        order.put("btn-6", "btn-7");
        order.put("btn-7", "btn-8");
        order.put("btn-8", "btn-1");
        order.put("panel-1", "btn-4");
        order.put("panel-2", "btn-7");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn-1", "btn-8");
        order.put("btn-2", "btn-1");
        order.put("btn-3", "btn-5");
        order.put("btn-4", "btn-3");
        order.put("btn-5", "btn-4");
        order.put("btn-6", "btn-4");
        order.put("btn-7", "btn-6");
        order.put("btn-8", "btn-7");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame", "panel-1", "panel-2"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-4";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-7";
        }
        return null;
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-3";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-6";
        }
        return null;
    }

    protected String getLastComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-8";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-5";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-8";
        }
        return null;
    }
}

/*
 * frame [ container0 [...] container1(root) [ comp1 comp2 container2(provider) [...] ] ]
 * - verifies a case when a provider is nested in a root.
 */
class PolicyTest10 extends AbstractPolicyTest {

    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new GridLayout(2, 1));

        Container cont0 = new Panel();
        cont0.add(registerComponent("btn-1", new Button("button")));
        cont0.add(registerComponent("btn-2", new Button("button")));

        Container cont1 = (Container)registerComponent("panel-1", new Panel());
        cont1.add(registerComponent("btn-3", new Button("button")));
        cont1.add(registerComponent("btn-4", new Button("button")));

        Container cont2 = (Container)registerComponent("panel-2", new Panel());
        cont2.add(registerComponent("btn-5", new Button("button")));
        cont2.add(registerComponent("btn-6", new Button("button")));

        cont1.add(cont2);
        frame.add(cont0);
        frame.add(cont1);

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("panel-1")).setFocusCycleRoot(true);
        ((Container)getComponent("panel-1")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                public Component getDefaultComponent(Container aContainer) {
                    return getComponent("panel-2");
                }
            });
        ((Container)getComponent("panel-2")).setFocusTraversalPolicyProvider(true);
        ((Container)getComponent("panel-2")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn-1");
        order.put("btn-1", "btn-2");
        order.put("btn-2", "panel-2");
        order.put("btn-3", "btn-4");
        order.put("btn-4", "btn-5");
        order.put("btn-5", "btn-6");
        order.put("btn-6", "btn-3");
        order.put("panel-1", "panel-2");
        order.put("panel-2", "btn-5");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn-1", "btn-2");
        order.put("btn-2", "btn-1");
        order.put("btn-3", "btn-6");
        order.put("btn-4", "btn-3");
        order.put("btn-5", "btn-4");
        order.put("btn-6", "btn-5");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame", "panel-1", "panel-2"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "panel-2";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-5";
        }
        return null;
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-1";
        } else if ("panel-1".equals(focusCycleRoot_id)) {
            return "btn-3";
        } else if ("panel-2".equals(focusCycleRoot_id)) {
            return "btn-5";
        }
        return null;
    }

    protected String getLastComp(String focusCycleRoot_id) {
        if ("frame".equals(focusCycleRoot_id)) {
            return "btn-2";
        } else {
            return "btn-6";
        }
    }
}

/*
 * frame [ container(root) [...] comp ]
 * - getDefaultComponent(<frame>) should implicitly down-cycle into the <container>.
 * - getFirstComponent(<frame>) should implicitly down-cycle into the <container>.
 */
class PolicyTest11 extends AbstractPolicyTest {
    protected Frame createFrame() {
        Frame frame = (Frame) registerComponent("frame", new Frame("Test Frame"));
        frame.setLayout(new FlowLayout());

        Container cont = (Container)registerComponent("panel", new Panel());
        cont.add(registerComponent("btn-1", new Button("button")));
        cont.add(registerComponent("btn-2", new Button("button")));

        frame.add(cont);
        frame.add(registerComponent("btn-3", new Button("button")));

        return frame;
    }

    protected void customizeHierarchy() {
        ((Container)getComponent("frame")).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        ((Container)getComponent("panel")).setFocusCycleRoot(true);
    }

    protected Map<String, String> getForwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("frame", "btn-1");
        order.put("btn-1", "btn-2");
        order.put("btn-2", "btn-1");
        order.put("btn-3", "btn-1");
        return order;
    }

    protected Map<String, String> getBackwardOrder() {
        Map<String, String> order = new HashMap<String, String>();
        order.put("btn-3", "btn-1");
        order.put("btn-2", "btn-1");
        order.put("btn-1", "btn-2");
        order.put("frame", "btn-3");
        return order;
    }

    protected String[] getContainersToTest() {
        return new String[] {"frame"};
    }

    protected String getDefaultComp(String focusCycleRoot_id) {
        return "btn-1";
    }

    protected String getFirstComp(String focusCycleRoot_id) {
        return "btn-1";
    }

    protected String getLastComp(String focusCycleRoot_id) {
        return "btn-3";
    }
}
