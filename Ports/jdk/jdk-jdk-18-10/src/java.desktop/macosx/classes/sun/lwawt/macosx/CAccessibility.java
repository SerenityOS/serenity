/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.lwawt.macosx;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Native;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.Arrays;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleTable;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.KeyStroke;

import sun.awt.AWTAccessor;
import sun.lwawt.LWWindowPeer;

class CAccessibility implements PropertyChangeListener {
    private static Set<String> ignoredRoles;

    static {
        loadAWTLibrary();
    }

    @SuppressWarnings("removal")
    private static void loadAWTLibrary() {
            // Need to load the native library for this code.
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("awt");
                    return null;
                }
            });
    }

    static CAccessibility sAccessibility;
    static synchronized CAccessibility getAccessibility(final String[] roles) {
        if (sAccessibility != null) return sAccessibility;
        sAccessibility = new CAccessibility();

        if (roles != null) {
            ignoredRoles = new HashSet<String>(roles.length);
            for (final String role : roles) ignoredRoles.add(role);
        } else {
            ignoredRoles = new HashSet<String>();
        }

        return sAccessibility;
    }

    private CAccessibility() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", this);
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        Object newValue = evt.getNewValue();
        if (newValue == null) return;
        // Don't post focus on things that don't matter, i.e. alert, colorchooser,
        // desktoppane, dialog, directorypane, filechooser, filler, fontchoose,
        // frame, glasspane, layeredpane, optionpane, panel, rootpane, separator,
        // tooltip, viewport, window.
        // List taken from initializeRoles() in JavaComponentUtilities.m.
        if (newValue instanceof Accessible) {
            AccessibleContext nvAC = ((Accessible) newValue).getAccessibleContext();
            AccessibleRole nvRole = nvAC.getAccessibleRole();
            if (!ignoredRoles.contains(roleKey(nvRole))) {
                focusChanged();
            }
        }
    }

    private native void focusChanged();

    static <T> T invokeAndWait(final Callable<T> callable, final Component c) {
        return invokeAndWait(callable, c, null);
    }

    static <T> T invokeAndWait(final Callable<T> callable, final Component c, final T defValue) {
        T value = null;
        if (c != null) {
            try {
                value = LWCToolkit.invokeAndWait(callable, c);
            } catch (final Exception e) { e.printStackTrace(); }
        }

        return value != null ? value : defValue;
    }

    static void invokeLater(final Runnable runnable, final Component c) {
        if (c != null) {
            try {
                LWCToolkit.invokeLater(runnable, c);
            } catch (InvocationTargetException e) { e.printStackTrace(); }
        }
    }

    public static String getAccessibleActionDescription(final AccessibleAction aa, final int index, final Component c) {
        if (aa == null) return null;

        return invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                return aa.getAccessibleActionDescription(index);
            }
        }, c);
    }

    public static void doAccessibleAction(final AccessibleAction aa, final int index, final Component c) {
        // We make this an invokeLater because we don't need a reply.
        if (aa == null) return;

        invokeLater(new Runnable() {
            public void run() {
                aa.doAccessibleAction(index);
            }
        }, c);
    }

    public static Dimension getSize(final AccessibleComponent ac, final Component c) {
        if (ac == null) return null;

        return invokeAndWait(new Callable<Dimension>() {
            public Dimension call() throws Exception {
                return ac.getSize();
            }
        }, c);
    }

    public static AccessibleSelection getAccessibleSelection(final AccessibleContext ac, final Component c) {
        if (ac == null) return null;

        return invokeAndWait(new Callable<AccessibleSelection>() {
            public AccessibleSelection call() throws Exception {
                return ac.getAccessibleSelection();
            }
        }, c);
    }

    public static Accessible ax_getAccessibleSelection(final AccessibleContext ac, final int index, final Component c) {
        if (ac == null) return null;

        return invokeAndWait(new Callable<Accessible>() {
            public Accessible call() throws Exception {
                final AccessibleSelection as = ac.getAccessibleSelection();
                if (as == null) return null;
                return as.getAccessibleSelection(index);
            }
        }, c);
    }

    // KCH - can we make this a postEvent?
    public static void addAccessibleSelection(final AccessibleContext ac, final int index, final Component c) {
        if (ac == null) return;

        invokeLater(new Runnable() {
            public void run() {
                final AccessibleSelection as = ac.getAccessibleSelection();
                if (as == null) return;
                as.addAccessibleSelection(index);
            }
        }, c);
    }

    public static AccessibleContext getAccessibleContext(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<AccessibleContext>() {
            public AccessibleContext call() throws Exception {
                return a.getAccessibleContext();
            }
        }, c);
    }

    public static boolean isAccessibleChildSelected(final Accessible a, final int index, final Component c) {
        if (a == null) return false;

        return invokeAndWait(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return Boolean.FALSE;

                final AccessibleSelection as = ac.getAccessibleSelection();
                if (as == null) return Boolean.FALSE;

                return as.isAccessibleChildSelected(index);
            }
        }, c, false);
    }

    public static AccessibleStateSet getAccessibleStateSet(final AccessibleContext ac, final Component c) {
        if (ac == null) return null;

        return invokeAndWait(new Callable<AccessibleStateSet>() {
            public AccessibleStateSet call() throws Exception {
                return ac.getAccessibleStateSet();
            }
        }, c);
    }

    public static boolean contains(final AccessibleContext ac, final AccessibleState as, final Component c) {
        if (ac == null || as == null) return false;

        return invokeAndWait(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                final AccessibleStateSet ass = ac.getAccessibleStateSet();
                if (ass == null) return null;
                return ass.contains(as);
            }
        }, c, false);
    }

    static String getAccessibleRoleFor(final Accessible a) {
        final AccessibleContext ac = a.getAccessibleContext();
        if (ac == null) return null;

        final AccessibleRole role = ac.getAccessibleRole();
        return AWTAccessor.getAccessibleBundleAccessor().getKey(role);
    }

    public static String getAccessibleRole(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final Accessible sa = CAccessible.getSwingAccessible(a);
                final String role = getAccessibleRoleFor(a);

                if (!"text".equals(role)) return role;
                if (sa instanceof JTextArea || sa instanceof JEditorPane) {
                    return "textarea";
                }
                return role;
            }
        }, c);
    }

    public static Point getLocationOnScreen(final AccessibleComponent ac, final Component c) {
        if (ac == null) return null;

        return invokeAndWait(new Callable<Point>() {
            public Point call() throws Exception {
                return ac.getLocationOnScreen();
            }
        }, c);
    }

    public static int getCharCount(final AccessibleText at, final Component c) {
        if (at == null) return 0;

        return invokeAndWait(new Callable<Integer>() {
            public Integer call() throws Exception {
                return at.getCharCount();
            }
        }, c, 0);
    }

    // Accessibility Threadsafety for JavaComponentAccessibility.m
    public static Accessible getAccessibleParent(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<Accessible>() {
            public Accessible call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                return ac.getAccessibleParent();
            }
        }, c);
    }

    public static int getAccessibleIndexInParent(final Accessible a, final Component c) {
        if (a == null) return -1;

        return invokeAndWait(new Callable<Integer>() {
            public Integer call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                return ac.getAccessibleIndexInParent();
            }
        }, c, -1);
    }

    public static AccessibleComponent getAccessibleComponent(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<AccessibleComponent>() {
            public AccessibleComponent call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                return ac.getAccessibleComponent();
            }
        }, c);
    }

    public static AccessibleValue getAccessibleValue(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<AccessibleValue>() {
            public AccessibleValue call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                AccessibleValue accessibleValue = ac.getAccessibleValue();
                return accessibleValue;
            }
        }, c);
    }

    public static String getAccessibleName(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final String accessibleName = ac.getAccessibleName();
                if (accessibleName == null) {
                    return ac.getAccessibleDescription();
                }
                final String acceleratorText = getAcceleratorText(ac);
                if (!acceleratorText.isEmpty()) {
                    return accessibleName +' '+ acceleratorText;
                }
                return accessibleName;
            }
        }, c);
    }

    public static AccessibleText getAccessibleText(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<AccessibleText>() {
            public AccessibleText call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                AccessibleText accessibleText = ac.getAccessibleText();
                return accessibleText;
            }
        }, c);
    }

    /*
     * Returns the JMenuItem accelerator. Implementation of this method is based
     * on AccessBridge.getAccelerator(AccessibleContext) to access the KeyStroke
     * and on AquaMenuPainter.paintMenuItem() to convert it to string.
     */
    @SuppressWarnings("deprecation")
    private static String getAcceleratorText(AccessibleContext ac) {
        String accText = "";
        Accessible parent = ac.getAccessibleParent();
        if (parent != null) {
            // workaround for getAccessibleKeyBinding not returning the
            // JMenuItem accelerator
            int indexInParent = ac.getAccessibleIndexInParent();
            Accessible child = parent.getAccessibleContext()
                                     .getAccessibleChild(indexInParent);
            if (child instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) child;
                KeyStroke keyStroke = menuItem.getAccelerator();
                if (keyStroke != null) {
                    int modifiers = keyStroke.getModifiers();
                    if (modifiers > 0) {
                        accText = KeyEvent.getKeyModifiersText(modifiers);
                    }
                    int keyCode = keyStroke.getKeyCode();
                    if (keyCode != 0) {
                        accText += KeyEvent.getKeyText(keyCode);
                    } else {
                        accText += keyStroke.getKeyChar();
                    }
                }
            }
        }
        return accText;
    }

    public static String getAccessibleDescription(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final String accessibleDescription = ac.getAccessibleDescription();
                if (accessibleDescription == null) {
                    if (c instanceof JComponent) {
                        String toolTipText = ((JComponent)c).getToolTipText();
                        if (toolTipText != null) {
                            return toolTipText;
                        }
                    }
                }

                return accessibleDescription;
            }
        }, c);
    }

    public static boolean isFocusTraversable(final Accessible a, final Component c) {
        if (a == null) return false;

        return invokeAndWait(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleComponent aComp = ac.getAccessibleComponent();
                if (aComp == null) return null;

                return aComp.isFocusTraversable();
            }
        }, c, false);
    }

    public static Accessible accessibilityHitTest(final Container parent, final float hitPointX, final float hitPointY) {
        return invokeAndWait(new Callable<Accessible>() {
            public Accessible call() throws Exception {
                final Point p = parent.getLocationOnScreen();

                // Make it into local coords
                final Point localPoint = new Point((int)(hitPointX - p.getX()), (int)(hitPointY - p.getY()));

                final Component component = parent.findComponentAt(localPoint);
                if (component == null) return null;

                final AccessibleContext axContext = component.getAccessibleContext();
                if (axContext == null) return null;

                final AccessibleComponent axComponent = axContext.getAccessibleComponent();
                if (axComponent == null) return null;

                final int numChildren = axContext.getAccessibleChildrenCount();
                if (numChildren > 0) {
                    // It has children, check to see which one is hit.
                    final Point p2 = axComponent.getLocationOnScreen();
                    final Point localP2 = new Point((int)(hitPointX - p2.getX()), (int)(hitPointY - p2.getY()));
                    return CAccessible.getCAccessible(axComponent.getAccessibleAt(localP2));
                }

                if (!(component instanceof Accessible)) return null;
                return CAccessible.getCAccessible((Accessible)component);
            }
        }, parent);
    }

    public static AccessibleAction getAccessibleAction(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<AccessibleAction>() {
            public AccessibleAction call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                return ac.getAccessibleAction();
            }
        }, c);
    }

    public static boolean isEnabled(final Accessible a, final Component c) {
        if (a == null) return false;

        return invokeAndWait(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleComponent aComp = ac.getAccessibleComponent();
                if (aComp == null) return null;

                return aComp.isEnabled();
            }
        }, c, false);
    }

    // KCH - can we make this a postEvent instead?
    public static void requestFocus(final Accessible a, final Component c) {
        if (a == null) return;

        invokeLater(new Runnable() {
            public void run() {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return;

                final AccessibleComponent aComp = ac.getAccessibleComponent();
                if (aComp == null) return;

                aComp.requestFocus();
            }
        }, c);
    }

    public static void requestSelection(final Accessible a, final Component c) {
        if (a == null) return;
        invokeLater(new Runnable() {
            public void run() {
                AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return;
                int i = ac.getAccessibleIndexInParent();
                if (i == -1) return;
                Accessible parent = ac.getAccessibleParent();
                AccessibleContext pac = parent.getAccessibleContext();
                if (pac == null) return;
                AccessibleSelection as = pac.getAccessibleSelection();
                if (as == null) return;
                if (parent instanceof JList) {
                    ((JList) parent).setSelectedIndex(i);
                    return;
                }
                as.addAccessibleSelection(i);
            }
        }, c);
    }

    public static Number getMaximumAccessibleValue(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<Number>() {
            public Number call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleValue av = ac.getAccessibleValue();
                if (av == null) return null;

                return av.getMaximumAccessibleValue();
            }
        }, c);
    }

    public static Number getMinimumAccessibleValue(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<Number>() {
            public Number call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleValue av = ac.getAccessibleValue();
                if (av == null) return null;

                return av.getMinimumAccessibleValue();
            }
        }, c);
    }

    public static String getAccessibleRoleDisplayString(final Accessible a, final Component c) {
        if (a == null) return null;

        return invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleRole ar = ac.getAccessibleRole();
                if (ar == null) return null;

                return ar.toDisplayString();
            }
        }, c);
    }

    public static Number getCurrentAccessibleValue(final AccessibleValue av, final Component c) {
        if (av == null) return null;

        return invokeAndWait(new Callable<Number>() {
            public Number call() throws Exception {
                Number currentAccessibleValue = av.getCurrentAccessibleValue();
                return currentAccessibleValue;
            }
        }, c);
    }

    public static Accessible getFocusOwner(final Component c) {
        return invokeAndWait(new Callable<Accessible>() {
            public Accessible call() throws Exception {
                Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (c == null || !(c instanceof Accessible)) return null;
                return CAccessible.getCAccessible((Accessible)c);
            }
        }, c);
    }

    public static boolean[] getInitialAttributeStates(final Accessible a, final Component c) {
        final boolean[] ret = new boolean[7];
        if (a == null) return ret;

        return invokeAndWait(new Callable<boolean[]>() {
            public boolean[] call() throws Exception {
                final AccessibleContext aContext = a.getAccessibleContext();
                if (aContext == null) return ret;

                final AccessibleComponent aComponent = aContext.getAccessibleComponent();
                ret[0] = (aComponent != null);
                ret[1] = ((aComponent != null) && (aComponent.isFocusTraversable()));
                ret[2] = (aContext.getAccessibleValue() != null);
                ret[3] = (aContext.getAccessibleText() != null);

                final AccessibleStateSet aStateSet = aContext.getAccessibleStateSet();
                ret[4] = (aStateSet.contains(AccessibleState.HORIZONTAL) || aStateSet.contains(AccessibleState.VERTICAL));
                ret[5] = (aContext.getAccessibleName() != null);
                ret[6] = (aContext.getAccessibleChildrenCount() > 0);
                return ret;
            }
        }, c);
    }

    // Duplicated from JavaComponentAccessibility
    // Note that values >=0 are indexes into the child array
    @Native static final int JAVA_AX_ALL_CHILDREN = -1;
    @Native static final int JAVA_AX_SELECTED_CHILDREN = -2;
    @Native static final int JAVA_AX_VISIBLE_CHILDREN = -3;

    // Each child takes up two entries in the array: one for itself and one for its role
    public static Object[] getChildrenAndRoles(final Accessible a, final Component c, final int whichChildren, final boolean allowIgnored) {
        if (a == null) return null;
        return invokeAndWait(new Callable<Object[]>() {
            public Object[] call() throws Exception {
                return getChildrenAndRolesImpl(a, c, whichChildren, allowIgnored);
            }
        }, c);
    }

    private static Object[] getChildrenAndRolesImpl(final Accessible a, final Component c, final int whichChildren, final boolean allowIgnored) {
        if (a == null) return null;

        ArrayList<Object> childrenAndRoles = new ArrayList<Object>();
        _addChildren(a, whichChildren, allowIgnored, childrenAndRoles);

        /* In case of fetching a selection, we need to check if
* the active descendant is at the beginning of the list, or
* otherwise move it, so that VoiceOver announces it correctly.
                * The java list is always in order from top to bottom, but when
                * (1) shift-selecting downward (extending the list) or (2) multi-selecting with
                * the VO keys (CTRL+ALT+CMD+RETURN) the active descendant
                * is not at the top of the list in the 1st case and may not be in the 2nd.
                */
        if (whichChildren == JAVA_AX_SELECTED_CHILDREN) {
            if (!childrenAndRoles.isEmpty()) {
                AccessibleContext activeDescendantAC =
                        CAccessible.getActiveDescendant(a);
                if (activeDescendantAC != null) {
                    String activeDescendantName =
                            activeDescendantAC.getAccessibleName();
                    AccessibleRole activeDescendantRole =
                            activeDescendantAC.getAccessibleRole();
                    // Move active descendant to front of list.
                    // List contains pairs of each selected item's
                    // Accessible and AccessibleRole.
                    ArrayList<Object> newArray = new ArrayList<Object>();
                    int count = childrenAndRoles.size();
                    Accessible currentAccessible = null;
                    AccessibleContext currentAC = null;
                    String currentName = null;
                    AccessibleRole currentRole = null;
                    for (int i = 0; i < count; i += 2) {
                        // Is this the active descendant?
                        currentAccessible = (Accessible) childrenAndRoles.get(i);
                        currentAC = currentAccessible.getAccessibleContext();
                        currentName = currentAC.getAccessibleName();
                        currentRole = (AccessibleRole) childrenAndRoles.get(i + 1);
                        if (currentName != null && currentName.equals(activeDescendantName) &&
                                currentRole.equals(activeDescendantRole)) {
                            newArray.add(0, currentAccessible);
                            newArray.add(1, currentRole);
                        } else {
                            newArray.add(currentAccessible);
                            newArray.add(currentRole);
                        }
                    }
                    childrenAndRoles = newArray;
                }
            }
        }

        if ((whichChildren < 0) || (whichChildren * 2 >= childrenAndRoles.size())) {
            return childrenAndRoles.toArray();
        }

        return new Object[]{childrenAndRoles.get(whichChildren * 2), childrenAndRoles.get((whichChildren * 2) + 1)};
    }

    // This method is called from the native
    // Each child takes up three entries in the array: one for itself, one for its role, and one for the recursion level
    private static Object[] getChildrenAndRolesRecursive(final Accessible a, final Component c, final int whichChildren, final boolean allowIgnored, final int level) {
        if (a == null) return null;
        return invokeAndWait(new Callable<Object[]>() {
            public Object[] call() throws Exception {
                ArrayList<Object> currentLevelChildren = new ArrayList<Object>();
                ArrayList<Object> allChildren = new ArrayList<Object>();
                ArrayList<Accessible> parentStack = new ArrayList<Accessible>();
                parentStack.add(a);
                ArrayList<Integer> indexses = new ArrayList<Integer>();
                Integer index = 0;
                int currentLevel = level;
                while (!parentStack.isEmpty()) {
                    Accessible p = parentStack.get(parentStack.size() - 1);

                    currentLevelChildren.addAll(Arrays.asList(getChildrenAndRolesImpl(p, c, JAVA_AX_ALL_CHILDREN, allowIgnored)));
                    if ((currentLevelChildren.size() == 0) || (index >= currentLevelChildren.size())) {
                        if (!parentStack.isEmpty()) parentStack.remove(parentStack.size() - 1);
                        if (!indexses.isEmpty()) index = indexses.remove(indexses.size() - 1);
                        currentLevel -= 1;
                        currentLevelChildren.clear();
                        continue;
                    }

                    Accessible ca = null;
                    Object obj = currentLevelChildren.get(index);
                    if (!(obj instanceof Accessible)) {
                        index += 2;
                        currentLevelChildren.clear();
                        continue;
                    }
                    ca = (Accessible) obj;
                    Object role = currentLevelChildren.get(index + 1);
                    currentLevelChildren.clear();

                    AccessibleContext cac = ca.getAccessibleContext();
                    if (cac == null) {
                        index += 2;
                        continue;
                    }

                    if ((cac.getAccessibleStateSet().contains(AccessibleState.SELECTED) && (whichChildren == JAVA_AX_SELECTED_CHILDREN)) ||
                            (cac.getAccessibleStateSet().contains(AccessibleState.VISIBLE) && (whichChildren == JAVA_AX_VISIBLE_CHILDREN)) ||
                            (whichChildren == JAVA_AX_ALL_CHILDREN)) {
                        allChildren.add(ca);
                        allChildren.add(role);
                        allChildren.add(String.valueOf(currentLevel));
                    }

                    index += 2;

                    if (cac.getAccessibleStateSet().contains(AccessibleState.EXPANDED)) {
                        parentStack.add(ca);
                        indexses.add(index);
                        index = 0;
                        currentLevel += 1;
                        continue;
                    }

                }

                return allChildren.toArray();
            }
        }, c);
    }

    @Native private static final int JAVA_AX_ROWS = 1;
    @Native private static final int JAVA_AX_COLS = 2;

    public static int getTableInfo(final Accessible a, final Component c,
                                   final int info) {
        if (a == null) return 0;
        return invokeAndWait(() -> {
            AccessibleContext ac = a.getAccessibleContext();
            AccessibleTable table = ac.getAccessibleTable();
            if (table != null) {
                if (info == JAVA_AX_COLS) {
                    return table.getAccessibleColumnCount();
                } else if (info == JAVA_AX_ROWS) {
                    return table.getAccessibleRowCount();
                }
            }
            return 0;
        }, c);
    }

    private static int[] getTableSelectedInfo(final Accessible a, final Component c,
                                              final int info) {
        if (a == null) return null;
        return invokeAndWait(() -> {
            AccessibleContext ac = a.getAccessibleContext();
            AccessibleTable table = ac.getAccessibleTable();
            if (table != null) {
                if (info == JAVA_AX_COLS) {
                    return table.getSelectedAccessibleColumns();
                } else if (info == JAVA_AX_ROWS) {
                    return table.getSelectedAccessibleRows();
                }
            }
            return null;
        }, c);
    }

    private static AccessibleRole getAccessibleRoleForLabel(JLabel l, AccessibleRole fallback) {
        String text = l.getText();
        if (text != null && text.length() > 0) {
            return fallback;
        }
        Icon icon = l.getIcon();
        if (icon != null) {
            return AccessibleRole.ICON;
        }
        return fallback;
    }

    private static AccessibleRole getAccessibleRole(Accessible a) {
        AccessibleContext ac = a.getAccessibleContext();
        AccessibleRole role = ac.getAccessibleRole();
        Object component = CAccessible.getSwingAccessible(a);
        if (role == null) return null;
        String roleString = role.toString();
        if ("label".equals(roleString) && component instanceof JLabel) {
            return getAccessibleRoleForLabel((JLabel) component, role);
        }
        return role;
    }


    // Either gets the immediate children of a, or recursively gets all unignored children of a
    private static void _addChildren(final Accessible a, final int whichChildren, final boolean allowIgnored, final ArrayList<Object> childrenAndRoles) {
        if (a == null) return;

        final AccessibleContext ac = a.getAccessibleContext();
        if (ac == null) return;

        final int numChildren = ac.getAccessibleChildrenCount();

        // each child takes up two entries in the array: itself, and its role
        // so the array holds alternating Accessible and AccessibleRole objects
        for (int i = 0; i < numChildren; i++) {
            final Accessible child = ac.getAccessibleChild(i);
            if (child == null) continue;

            final AccessibleContext context = child.getAccessibleContext();
            if (context == null) continue;

            if (whichChildren == JAVA_AX_VISIBLE_CHILDREN) {
                AccessibleComponent acomp = context.getAccessibleComponent();
                if (acomp == null || !acomp.isVisible()) {
                    continue;
                }
            } else if (whichChildren == JAVA_AX_SELECTED_CHILDREN) {
                AccessibleSelection sel = ac.getAccessibleSelection();
                if (sel == null || !sel.isAccessibleChildSelected(i)) {
                    continue;
                }
            }

            if (!allowIgnored) {
                final AccessibleRole role = context.getAccessibleRole();
                if (role != null && ignoredRoles != null && ignoredRoles.contains(roleKey(role))) {
                    // Get the child's unignored children.
                    _addChildren(child, whichChildren, false, childrenAndRoles);
                } else {
                    childrenAndRoles.add(child);
                    childrenAndRoles.add(getAccessibleRole(child));
                }
            } else {
                childrenAndRoles.add(child);
                childrenAndRoles.add(getAccessibleRole(child));
            }

            // If there is an index, and we are beyond it, time to finish up
            if ((whichChildren >= 0) && (childrenAndRoles.size() / 2) >= (whichChildren + 1)) {
                return;
            }
        }
    }

    private static native String roleKey(AccessibleRole aRole);

    public static Object[] getChildren(final Accessible a, final Component c) {
        if (a == null) return null;
        return invokeAndWait(new Callable<Object[]>() {
            public Object[] call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final int numChildren = ac.getAccessibleChildrenCount();
                final Object[] children = new Object[numChildren];
                for (int i = 0; i < numChildren; i++) {
                    children[i] = ac.getAccessibleChild(i);
                }
                return children;
            }
        }, c);
    }

    /**
     * @return AWTView ptr, a peer of the CPlatformView associated with the toplevel container of the Accessible, if any
     */
    private static long getAWTView(Accessible a) {
        Accessible ax = CAccessible.getSwingAccessible(a);
        if (!(ax instanceof Component)) return 0;

        return invokeAndWait(new Callable<Long>() {
            public Long call() throws Exception {
                Component cont = (Component) ax;
                while (cont != null && !(cont instanceof Window)) {
                    cont = cont.getParent();
                }
                if (cont != null) {
                    LWWindowPeer peer = (LWWindowPeer) AWTAccessor.getComponentAccessor().getPeer(cont);
                    if (peer != null) {
                        return ((CPlatformWindow) peer.getPlatformWindow()).getContentView().getAWTView();
                    }
                }
                return 0L;
            }
        }, (Component)ax);
    }

    private static boolean isTreeRootVisible(Accessible a, Component c) {
        if (a == null) return false;

        return invokeAndWait(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                Accessible sa = CAccessible.getSwingAccessible(a);
                if (sa instanceof JTree) {
                    return ((JTree) sa).isRootVisible();
                }
                return false;
            }
        }, c);
    }
}
