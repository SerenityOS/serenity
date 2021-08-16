/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.accessibility.internal;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.*;
import java.lang.reflect.*;

import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.plaf.TreeUI;

import javax.accessibility.*;
import com.sun.java.accessibility.util.*;
import java.awt.geom.Rectangle2D;
import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Note: This class has to be public.  It's loaded from the VM like this:
 *       Class.forName(atName).newInstance();
 */
final public class AccessBridge {

    private static AccessBridge theAccessBridge;
    private ObjectReferences references;
    private EventHandler eventHandler;

    // Maps AccessibleRoles strings to AccessibleRoles.
    private ConcurrentHashMap<String,AccessibleRole> accessibleRoleMap = new ConcurrentHashMap<>();

    /**
       If the object's role is in the following array getVirtualAccessibleName
       will use the extended search algorithm.
    */
    private ArrayList<AccessibleRole> extendedVirtualNameSearchRoles = new ArrayList<>();
    /**
       If the role of the object's parent is in the following array
       getVirtualAccessibleName will NOT use the extended search
       algorithm even if the object's role is in the
       extendedVirtualNameSearchRoles array.
    */
    private ArrayList<AccessibleRole> noExtendedVirtualNameSearchParentRoles = new ArrayList<>();

    private static native boolean isSysWow();


    /**
     * Load DLLs
     */
    static {
        initStatic();
    }

    @SuppressWarnings("removal")
    private static void initStatic() {
        // Load the appropriate DLLs
        boolean is32on64 = false;
        if (System.getProperty("os.arch").equals("x86")) {
            // 32 bit JRE
            // Load jabsysinfo.dll so can determine Win bitness
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("jabsysinfo");
                        return null;
                    }
                }, null, new java.lang.RuntimePermission("loadLibrary.jabsysinfo")
            );
            if (isSysWow()) {
                // 32 bit JRE on 64 bit OS
                is32on64 = true;
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                        public Void run() {
                            System.loadLibrary("javaaccessbridge-32");
                            return null;
                        }
                    }, null, new java.lang.RuntimePermission("loadLibrary.javaaccessbridge-32")
                );
            }
        }
        if (!is32on64) {
            // 32 bit JRE on 32 bit OS or 64 bit JRE on 64 bit OS
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("javaaccessbridge");
                        return null;
                    }
                }, null, new java.lang.RuntimePermission("loadLibrary.javaaccessbridge")
            );
        }
    }

    /**
     * AccessBridge constructor
     *
     * Note: This constructor has to be public.  It's called from the VM like this:
     *       Class.forName(atName).newInstance();
     */
    public AccessBridge() {
        theAccessBridge = this;
        references = new ObjectReferences();

        // initialize shutdown hook
        Runtime runTime = Runtime.getRuntime();
        shutdownHook hook = new shutdownHook();
        runTime.addShutdownHook(new Thread(hook));

        // initialize AccessibleRole map
        initAccessibleRoleMap();

        // initialize the methods that map HWNDs and Java top-level
        // windows
        initHWNDcalls();

        // is this a JVM we can use?
        // install JDK 1.2 and later Swing ToolKit listener
        EventQueueMonitor.isGUIInitialized();

        // start the Java event handler
        eventHandler = new EventHandler(this);

        // register for menu selection events
        MenuSelectionManager.defaultManager().addChangeListener(eventHandler);

        // register as a NativeWindowHandler
        addNativeWindowHandler(new DefaultNativeWindowHandler());

        // start in a new thread
        Thread abthread = new Thread(new dllRunner());
        abthread.setDaemon(true);
        abthread.start();
        debugString("[INFO]:AccessBridge started");
    }

    /*
     * adaptor to run the AccessBridge DLL
     */
    private class dllRunner implements Runnable {
        public void run() {
            runDLL();
        }
    }

    /*
     * shutdown hook
     */
    private class shutdownHook implements Runnable {

        public void run() {
            debugString("[INFO]:***** shutdownHook: shutting down...");
            javaShutdown();
        }
    }


    /*
     * Initialize the hashtable that maps Strings to AccessibleRoles.
     */
    private void initAccessibleRoleMap() {
        /*
         * Initialize the AccessibleRoles map. This code uses methods in
         * java.lang.reflect.* to build the map.
         */
        try {
            Class<?> clAccessibleRole = Class.forName ("javax.accessibility.AccessibleRole");
            if (null != clAccessibleRole) {
                AccessibleRole roleUnknown = AccessibleRole.UNKNOWN;
                Field [] fields = clAccessibleRole.getFields ();
                int i = 0;
                for (i = 0; i < fields.length; i ++) {
                    Field f = fields [i];
                    if (javax.accessibility.AccessibleRole.class == f.getType ()) {
                        AccessibleRole nextRole = (AccessibleRole) (f.get (roleUnknown));
                        String nextRoleString = nextRole.toDisplayString (Locale.US);
                        accessibleRoleMap.put (nextRoleString, nextRole);
                    }
                }
            }
        } catch (Exception e) {}

    /*
      Build the extendedVirtualNameSearchRoles array list.
    */
    extendedVirtualNameSearchRoles.add (AccessibleRole.COMBO_BOX);
    try {
        /*
          Added in J2SE 1.4
        */
        extendedVirtualNameSearchRoles.add (AccessibleRole.DATE_EDITOR);
    } catch (NoSuchFieldError e) {}
    extendedVirtualNameSearchRoles.add (AccessibleRole.LIST);
    extendedVirtualNameSearchRoles.add (AccessibleRole.PASSWORD_TEXT);
    extendedVirtualNameSearchRoles.add (AccessibleRole.SLIDER);
    try {
        /*
          Added in J2SE 1.3
        */
        extendedVirtualNameSearchRoles.add (AccessibleRole.SPIN_BOX);
    } catch (NoSuchFieldError e) {}
    extendedVirtualNameSearchRoles.add (AccessibleRole.TABLE);
    extendedVirtualNameSearchRoles.add (AccessibleRole.TEXT);
    extendedVirtualNameSearchRoles.add (AccessibleRole.UNKNOWN);

    noExtendedVirtualNameSearchParentRoles.add (AccessibleRole.TABLE);
    noExtendedVirtualNameSearchParentRoles.add (AccessibleRole.TOOL_BAR);
    }

    /**
     * start the AccessBridge DLL running in its own thread
     */
    private native void runDLL();

    /**
     * debugging output (goes to OutputDebugStr())
     */
    private native void sendDebugString(String debugStr);

    /**
     * debugging output (goes to OutputDebugStr())
     */
    private void debugString(String debugStr) {
    sendDebugString(debugStr);
    }

    /* ===== utility methods ===== */

    /**
     * decrement the reference to the object (called by native code)
     */
    private void decrementReference(Object o) {
    references.decrement(o);
    }

    /**
     * get the java.version property from the JVM
     */
    private String getJavaVersionProperty() {
        String s = System.getProperty("java.version");
        if (s != null) {
            references.increment(s);
            return s;
        }
        return null;
    }

    /* ===== HWND/Java window mapping methods ===== */

    // Java toolkit methods for mapping HWNDs to Java components
    private Method javaGetComponentFromNativeWindowHandleMethod;
    private Method javaGetNativeWindowHandleFromComponentMethod;

    // native jawt methods for mapping HWNDs to Java components
    private native int jawtGetNativeWindowHandleFromComponent(Component comp);

    private native Component jawtGetComponentFromNativeWindowHandle(int handle);

    Toolkit toolkit;

    /**
     * map an HWND to an AWT Component
     */
    private void initHWNDcalls() {
        Class<?>[] integerParemter = new Class<?>[1];
        integerParemter[0] = Integer.TYPE;
        Class<?>[] componentParemter = new Class<?>[1];
        try {
            componentParemter[0] = Class.forName("java.awt.Component");
        } catch (ClassNotFoundException e) {
            debugString("[ERROR]:Exception: " + e.toString());
        }
        toolkit = Toolkit.getDefaultToolkit();
        return;
    }

    // native window handler interface
    private interface NativeWindowHandler {
        public Accessible getAccessibleFromNativeWindowHandle(int nativeHandle);
    }

    // hash table of native window handle to AccessibleContext mappings
    static private ConcurrentHashMap<Integer,AccessibleContext> windowHandleToContextMap = new ConcurrentHashMap<>();

    // hash table of AccessibleContext to native window handle mappings
    static private ConcurrentHashMap<AccessibleContext,Integer> contextToWindowHandleMap = new ConcurrentHashMap<>();

    /*
     * adds a virtual window handler to our hash tables
     */
    static private void registerVirtualFrame(final Accessible a,
                                             Integer nativeWindowHandle ) {
        if (a != null) {
            AccessibleContext ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                @Override
                public AccessibleContext call() throws Exception {
                    return a.getAccessibleContext();
                }
            }, a);
            windowHandleToContextMap.put(nativeWindowHandle, ac);
            contextToWindowHandleMap.put(ac, nativeWindowHandle);
        }
    }

    /*
     * removes a virtual window handler to our hash tables
     */
    static private void revokeVirtualFrame(final Accessible a,
                                           Integer nativeWindowHandle ) {
        AccessibleContext ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                return a.getAccessibleContext();
            }
        }, a);
        windowHandleToContextMap.remove(nativeWindowHandle);
        contextToWindowHandleMap.remove(ac);
    }

    // vector of native window handlers
    private static Vector<NativeWindowHandler> nativeWindowHandlers = new Vector<>();

    /*
    * adds a native window handler to our list
    */
    private static void addNativeWindowHandler(NativeWindowHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException();
        }
        nativeWindowHandlers.addElement(handler);
    }

    /*
     * removes a native window handler to our list
     */
    private static boolean removeNativeWindowHandler(NativeWindowHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException();
        }
        return nativeWindowHandlers.removeElement(handler);
    }

    /**
     * verifies that a native window handle is a Java window
     */
    private boolean isJavaWindow(int nativeHandle) {
        AccessibleContext ac = getContextFromNativeWindowHandle(nativeHandle);
        if (ac != null) {
            saveContextToWindowHandleMapping(ac, nativeHandle);
            return true;
        }
        return false;
    }

    /*
     * saves the mapping between an AccessibleContext and a window handle
     */
    private void saveContextToWindowHandleMapping(AccessibleContext ac,
                                                  int nativeHandle) {
        debugString("[INFO]:saveContextToWindowHandleMapping...");
        if (ac == null) {
            return;
        }
        if (! contextToWindowHandleMap.containsKey(ac)) {
            debugString("[INFO]: saveContextToWindowHandleMapping: ac = "+ac+"; handle = "+nativeHandle);
            contextToWindowHandleMap.put(ac, nativeHandle);
        }
    }

    /**
     * maps a native window handle to an Accessible Context
     */
    private AccessibleContext getContextFromNativeWindowHandle(int nativeHandle) {
        // First, look for the Accessible in our hash table of
        // virtual window handles.
        AccessibleContext ac = windowHandleToContextMap.get(nativeHandle);
        if(ac!=null) {
            saveContextToWindowHandleMapping(ac, nativeHandle);
            return ac;
        }

        // Next, look for the native window handle in our vector
        // of native window handles.
        int numHandlers = nativeWindowHandlers.size();
        for (int i = 0; i < numHandlers; i++) {
            NativeWindowHandler nextHandler = nativeWindowHandlers.elementAt(i);
            final Accessible a = nextHandler.getAccessibleFromNativeWindowHandle(nativeHandle);
            if (a != null) {
                ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return a.getAccessibleContext();
                    }
                }, a);
                saveContextToWindowHandleMapping(ac, nativeHandle);
                return ac;
            }
        }
        // Not found.
        return null;
    }

    /**
     * maps an AccessibleContext to a native window handle
     *     returns 0 on error
     */
    private int getNativeWindowHandleFromContext(AccessibleContext ac) {
    debugString("[INFO]: getNativeWindowHandleFromContext: ac = "+ac);
        try {
            return contextToWindowHandleMap.get(ac);
        } catch (Exception ex) {
            return 0;
        }
    }

    private class DefaultNativeWindowHandler implements NativeWindowHandler {
        /*
        * returns the Accessible associated with a native window
        */
        public Accessible getAccessibleFromNativeWindowHandle(int nativeHandle) {
            final Component c = jawtGetComponentFromNativeWindowHandle(nativeHandle);
            if (c instanceof Accessible) {
                AccessibleContext ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return c.getAccessibleContext();
                    }
                }, c);
                saveContextToWindowHandleMapping(ac, nativeHandle);
                return (Accessible)c;
            } else {
                return null;
            }
        }
    }

    /* ===== AccessibleContext methods =====*/

    /*
     * returns the inner-most AccessibleContext in parent at Point(x, y)
     */
    private AccessibleContext getAccessibleContextAt(int x, int y,
                                                    AccessibleContext parent) {
        if (parent == null) {
            return null;
        }
        if (windowHandleToContextMap != null &&
            windowHandleToContextMap.containsValue(getRootAccessibleContext(parent))) {
            // Path for applications that register their top-level
            // windows with the AccessBridge (e.g., StarOffice 6.1)
            return getAccessibleContextAt_1(x, y, parent);
        } else {
            // Path for applications that do not register
            // their top-level windows with the AccessBridge
            // (e.g., Swing/AWT applications)
            return getAccessibleContextAt_2(x, y, parent);
        }
    }

    /*
     * returns the root accessible context
     */
    private AccessibleContext getRootAccessibleContext(final AccessibleContext ac) {
        if (ac == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                Accessible parent = ac.getAccessibleParent();
                if (parent == null) {
                    return ac;
                }
                Accessible tmp = parent.getAccessibleContext().getAccessibleParent();
                while (tmp != null) {
                    parent = tmp;
                    tmp = parent.getAccessibleContext().getAccessibleParent();
                }
                return parent.getAccessibleContext();
            }
        }, ac);
    }

    /*
     * StarOffice version that does not use the EventQueueMonitor
     */
    private AccessibleContext getAccessibleContextAt_1(final int x, final int y,
                                                      final AccessibleContext parent) {
        debugString("[INFO]: getAccessibleContextAt_1 called");
        debugString("   -> x = " + x + " y = " + y + " parent = " + parent);

        if (parent == null) return null;
            final AccessibleComponent acmp = InvocationUtils.invokeAndWait(new Callable<AccessibleComponent>() {
                @Override
                public AccessibleComponent call() throws Exception {
                    return parent.getAccessibleComponent();
                }
            }, parent);
        if (acmp!=null) {
            final Point loc = InvocationUtils.invokeAndWait(new Callable<Point>() {
                @Override
                public Point call() throws Exception {
                    return acmp.getLocation();
                }
            }, parent);
            final Accessible a = InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                @Override
                public Accessible call() throws Exception {
                    return acmp.getAccessibleAt(new Point(x - loc.x, y - loc.y));
                }
            }, parent);
            if (a != null) {
                AccessibleContext foundAC = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return a.getAccessibleContext();
                    }
                }, parent);
                if (foundAC != null) {
                    if (foundAC != parent) {
                        // recurse down into the child
                        return getAccessibleContextAt_1(x - loc.x, y - loc.y,
                                                        foundAC);
                    } else
                        return foundAC;
                }
            }
        }
        return parent;
    }

    /*
     * AWT/Swing version
     */
    private AccessibleContext getAccessibleContextAt_2(final int x, final int y,
                                                      AccessibleContext parent) {
        debugString("[INFO]: getAccessibleContextAt_2 called");
        debugString("   -> x = " + x + " y = " + y + " parent = " + parent);

        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                Accessible a = EventQueueMonitor.getAccessibleAt(new Point(x, y));
                if (a != null) {
                    AccessibleContext childAC = a.getAccessibleContext();
                    if (childAC != null) {
                        debugString("[INFO]:   returning childAC = " + childAC);
                        return childAC;
                    }
                }
                return null;
            }
        }, parent);
    }

    /**
     * returns the Accessible that has focus
     */
    private AccessibleContext getAccessibleContextWithFocus() {
        Component c = AWTEventMonitor.getComponentWithFocus();
        if (c != null) {
            final Accessible a = Translator.getAccessible(c);
            if (a != null) {
                AccessibleContext ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return a.getAccessibleContext();
                    }
                }, c);
                if (ac != null) {
                    return ac;
                }
            }
        }
        return null;
    }

    /**
     * returns the AccessibleName from an AccessibleContext
     */
    private String getAccessibleNameFromContext(final AccessibleContext ac) {
        debugString("[INFO]: ***** ac = "+ac.getClass());
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ac.getAccessibleName();
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                debugString("[INFO]: Returning AccessibleName from Context: " + s);
                return s;
            } else {
                return null;
            }
        } else {
            debugString("[INFO]: getAccessibleNameFromContext; ac = null!");
            return null;
        }
    }

    /**
     * Returns an AccessibleName for a component using an algorithm optimized
     * for the JAWS screen reader.  This method is only intended for JAWS. All
     * other uses are entirely optional.
     */
    private String getVirtualAccessibleNameFromContext(final AccessibleContext ac) {
        if (null != ac) {
            /*
            Step 1:
            =======
            Determine if we can obtain the Virtual Accessible Name from the
            Accessible Name or Accessible Description of the object.
            */
            String nameString = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ac.getAccessibleName();
                }
            }, ac);
            if ( ( null != nameString ) && ( 0 != nameString.length () ) ) {
                debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from AccessibleContext::getAccessibleName.");
                references.increment (nameString);
                return nameString;
            }
            String descriptionString = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ac.getAccessibleDescription();
                }
            }, ac);
            if ( ( null != descriptionString ) && ( 0 != descriptionString.length () ) ) {
                debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from AccessibleContext::getAccessibleDescription.");
                references.increment (descriptionString);
                return descriptionString;
            }

            debugString ("[WARN]: The Virtual Accessible Name was not found using AccessibleContext::getAccessibleDescription. or getAccessibleName");
            /*
            Step 2:
            =======
            Decide whether the extended name search algorithm should be
            used for this object.
            */
            boolean bExtendedSearch = false;
            AccessibleRole role = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                @Override
                public AccessibleRole call() throws Exception {
                    return ac.getAccessibleRole();
                }
            }, ac);
            AccessibleContext parentContext = null;
            AccessibleRole parentRole = AccessibleRole.UNKNOWN;

            if ( extendedVirtualNameSearchRoles.contains (role) ) {
                parentContext = getAccessibleParentFromContext (ac);
                if ( null != parentContext ) {
                    final AccessibleContext parentContextInnerTemp = parentContext;
                    parentRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                        @Override
                        public AccessibleRole call() throws Exception {
                            return parentContextInnerTemp.getAccessibleRole();
                        }
                    }, ac);
                    if ( AccessibleRole.UNKNOWN != parentRole ) {
                        bExtendedSearch = true;
                        if ( noExtendedVirtualNameSearchParentRoles.contains (parentRole) ) {
                            bExtendedSearch = false;
                        }
                    }
                }
            }

            if (false == bExtendedSearch) {
                debugString ("[INFO]: bk -- getVirtualAccessibleNameFromContext will not use the extended name search algorithm.  role = " + ( role != null ? role.toDisplayString(Locale.US) : "null") );
                /*
                Step 3:
                =======
                We have determined that we should not use the extended name
                search algorithm for this object (we must obtain the name of
                the object from the object itself and not from neighboring
                objects).  However the object name cannot be obtained from
                the Accessible Name or Accessible Description of the object.

                Handle several special cases here that might yield a value for
                the Virtual Accessible Name.  Return null if the object does
                not match the criteria for any of these special cases.
                */
                if (AccessibleRole.LABEL == role) {
                    /*
                    Does the label support the Accessible Text Interface?
                    */
                    final AccessibleText at = InvocationUtils.invokeAndWait(new Callable<AccessibleText>() {
                        @Override
                        public AccessibleText call() throws Exception {
                            return ac.getAccessibleText();
                        }
                    }, ac);
                    if (null != at) {
                        int charCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return at.getCharCount();
                            }
                        }, ac);
                        String text = getAccessibleTextRangeFromContext (ac, 0, charCount);
                        if (null != text) {
                            debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from the Accessible Text of the LABEL object.");
                            references.increment (text);
                            return text;
                        }
                    }
                    /*
                    Does the label support the Accessible Icon Interface?
                    */
                    debugString ("[INFO]: bk -- Attempting to obtain the Virtual Accessible Name from the Accessible Icon information.");
                    final AccessibleIcon [] ai = InvocationUtils.invokeAndWait(new Callable<AccessibleIcon[]>() {
                        @Override
                        public AccessibleIcon[] call() throws Exception {
                            return ac.getAccessibleIcon();
                        }
                    }, ac);
                    if ( (null != ai) && (ai.length > 0) ) {
                        String iconDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                return ai[0].getAccessibleIconDescription();
                            }
                        }, ac);
                        if (iconDescription != null){
                            debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from the description of the first Accessible Icon found in the LABEL object.");
                            references.increment (iconDescription);
                            return iconDescription;
                        }
                    } else {
                        parentContext = getAccessibleParentFromContext (ac);
                        if ( null != parentContext ) {
                            final AccessibleContext parentContextInnerTemp = parentContext;
                            parentRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                                @Override
                                public AccessibleRole call() throws Exception {
                                    return parentContextInnerTemp.getAccessibleRole();
                                }
                            }, ac);
                            if ( AccessibleRole.TABLE == parentRole ) {
                                int indexInParent = InvocationUtils.invokeAndWait(new Callable<Integer>() {
                                    @Override
                                    public Integer call() throws Exception {
                                        return ac.getAccessibleIndexInParent();
                                    }
                                }, ac);
                                final AccessibleContext acTableCell = getAccessibleChildFromContext (parentContext, indexInParent);
                                debugString ("[INFO]: bk -- Making a second attempt to obtain the Virtual Accessible Name from the Accessible Icon information for the Table Cell.");
                                if (acTableCell != null) {
                                    final AccessibleIcon [] aiRet =InvocationUtils.invokeAndWait(new Callable<AccessibleIcon[]>() {
                                        @Override
                                        public AccessibleIcon[] call() throws Exception {
                                            return acTableCell.getAccessibleIcon();
                                        }
                                    }, ac);
                                    if ( (null != aiRet) && (aiRet.length > 0) ) {
                                        String iconDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                            @Override
                                            public String call() throws Exception {
                                                return aiRet[0].getAccessibleIconDescription();
                                            }
                                        }, ac);
                                        if (iconDescription != null){
                                            debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from the description of the first Accessible Icon found in the Table Cell object.");
                                            references.increment (iconDescription);
                                            return iconDescription;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if ( (AccessibleRole.TOGGLE_BUTTON == role) ||
                            (AccessibleRole.PUSH_BUTTON == role) ) {
                    /*
                    Does the button support the Accessible Icon Interface?
                    */
                    debugString ("[INFO]: bk -- Attempting to obtain the Virtual Accessible Name from the Accessible Icon information.");
                    final AccessibleIcon [] ai = InvocationUtils.invokeAndWait(new Callable<AccessibleIcon[]>() {
                        @Override
                        public AccessibleIcon[] call() throws Exception {
                            return ac.getAccessibleIcon();
                        }
                    }, ac);
                    if ( (null != ai) && (ai.length > 0) ) {
                        String iconDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                return ai[0].getAccessibleIconDescription();
                            }
                        }, ac);
                        if (iconDescription != null){
                            debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from the description of the first Accessible Icon found in the TOGGLE_BUTTON or PUSH_BUTTON object.");
                            references.increment (iconDescription);
                            return iconDescription;
                        }
                    }
                } else if ( AccessibleRole.CHECK_BOX == role ) {
                    /*
                    NOTE: The only case I know of in which a check box does not
                    have a name is when that check box is contained in a table.

                    In this case it would be appropriate to use the display string
                    of the check box object as the name (in US English the display
                    string is typically either "true" or "false").

                    I am using the AccessibleValue interface to obtain the display
                    string of the check box.  If the Accessible Value is 1, I am
                    returning Boolean.TRUE.toString (),  If the Accessible Value is
                    0, I am returning Boolean.FALSE.toString ().  If the Accessible
                    Value is some other number, I will return the display string of
                    the current numerical value of the check box.
                    */
                    final AccessibleValue av = InvocationUtils.invokeAndWait(new Callable<AccessibleValue>() {
                        @Override
                        public AccessibleValue call() throws Exception {
                            return ac.getAccessibleValue();
                        }
                    }, ac);
                    if ( null != av ) {
                        nameString = null;
                        Number value = InvocationUtils.invokeAndWait(new Callable<Number>() {
                            @Override
                            public Number call() throws Exception {
                                return av.getCurrentAccessibleValue();
                            }
                        }, ac);
                        if ( null != value ) {
                            if ( 1 == value.intValue () ) {
                                nameString = Boolean.TRUE.toString ();
                            } else if ( 0 == value.intValue () ) {
                                nameString = Boolean.FALSE.toString ();
                            } else {
                                nameString = value.toString ();
                            }
                            if ( null != nameString ) {
                                references.increment (nameString);
                                return nameString;
                            }
                        }
                    }
                }
                return null;
            }

            /*
            +
            Beginning of the extended name search
            +
            */
            final AccessibleContext parentContextOuterTemp = parentContext;
            String parentName = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return parentContextOuterTemp.getAccessibleName();
                }
            }, ac);
            String parentDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return parentContextOuterTemp.getAccessibleDescription();
                }
            }, ac);

            /*
            Step 4:
            =======
            Special case for Slider Bar objects.
            */
            if ( (AccessibleRole.SLIDER == role) &&
                 (AccessibleRole.PANEL == parentRole) &&
                 (null != parentName) ) {
                debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from the Accessible Name of the SLIDER object's parent object.");
                references.increment (parentName);
                return parentName;
            }

            boolean bIsEditCombo = false;

            AccessibleContext testContext = ac;
            /*
            Step 5:
            =======
            Special case for Edit Combo Boxes
            */
            if ( (AccessibleRole.TEXT == role) &&
                 (AccessibleRole.COMBO_BOX == parentRole) ) {
                bIsEditCombo = true;
                if (null != parentName) {
                    debugString ("[INFO]: bk -- The Virtual Accessible Name for this Edit Combo box was obtained from the Accessible Name of the object's parent object.");
                    references.increment (parentName);
                    return parentName;
                } else if (null != parentDescription) {
                    debugString ("[INFO]: bk -- The Virtual Accessible Name for this Edit Combo box was obtained from the Accessible Description of the object's parent object.");
                    references.increment (parentDescription);
                    return parentDescription;
                }
                testContext = parentContext;
                parentRole = AccessibleRole.UNKNOWN;
                parentContext = getAccessibleParentFromContext (testContext);
                if ( null != parentContext ) {
                    final AccessibleContext parentContextInnerTemp = parentContext;
                    parentRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                        @Override
                        public AccessibleRole call() throws Exception {
                            return parentContextInnerTemp.getAccessibleRole();
                        }
                    }, ac);
                }
            }

            /*
            Step 6:
            =======
            Attempt to get the Virtual Accessible Name of the object using the
            Accessible Relation Set Info (the LABELED_BY Accessible Relation).
            */
            {
                final AccessibleContext parentContextTempInner = parentContext;
                AccessibleRelationSet ars = InvocationUtils.invokeAndWait(new Callable<AccessibleRelationSet>() {
                    @Override
                    public AccessibleRelationSet call() throws Exception {
                        return parentContextTempInner.getAccessibleRelationSet();
                    }
                }, ac);
                if ( ars != null && (ars.size () > 0) && (ars.contains (AccessibleRelation.LABELED_BY)) ) {
                    AccessibleRelation labeledByRelation = ars.get (AccessibleRelation.LABELED_BY);
                    if (labeledByRelation != null) {
                        Object [] targets = labeledByRelation.getTarget ();
                        Object o = targets [0];
                        if (o instanceof Accessible) {
                            AccessibleContext labelContext = ((Accessible)o).getAccessibleContext ();
                            if (labelContext != null) {
                                String labelName = labelContext.getAccessibleName ();
                                String labelDescription = labelContext.getAccessibleDescription ();
                                if (null != labelName) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained using the LABELED_BY AccessibleRelation -- Name Case.");
                                    references.increment (labelName);
                                    return labelName;
                                } else if (null != labelDescription) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained using the LABELED_BY AccessibleRelation -- Description Case.");
                                    references.increment (labelDescription);
                                    return labelDescription;
                                }
                            }
                        }
                    }
                }
            }

            //Note: add AccessibleContext to use InvocationUtils.invokeAndWait
            /*
            Step 7:
            =======
            Search for a label object that is positioned either just to the left
            or just above the object and get the Accessible Name of the Label
            object.
            */
            int testIndexMax = 0;
            int testX = 0;
            int testY = 0;
            int testWidth = 0;
            int testHeight = 0;
            int targetX = 0;
            int targetY = 0;
            final AccessibleContext tempContext = testContext;
            int testIndex = InvocationUtils.invokeAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return tempContext.getAccessibleIndexInParent();
                }
            }, ac);
            if ( null != parentContext ) {
                final AccessibleContext parentContextInnerTemp = parentContext;
                testIndexMax =  InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return parentContextInnerTemp.getAccessibleChildrenCount() - 1;
                    }
                }, ac);
            }
            testX = getAccessibleXcoordFromContext (testContext);
            testY = getAccessibleYcoordFromContext (testContext);
            testWidth = getAccessibleWidthFromContext (testContext);
            testHeight = getAccessibleHeightFromContext (testContext);
            targetX = testX + 2;
            targetY = testY + 2;

            int childIndex = testIndex - 1;
            /*Accessible child = null;
            AccessibleContext childContext = null;
            AccessibleRole childRole = AccessibleRole.UNKNOWN;*/
            int childX = 0;
            int childY = 0;
            int childWidth = 0;
            int childHeight = 0;
            String childName = null;
            String childDescription = null;
            while (childIndex >= 0) {
                final int childIndexTemp = childIndex;
                final AccessibleContext parentContextInnerTemp = parentContext;
                final Accessible child  =  InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                    @Override
                    public Accessible call() throws Exception {
                        return parentContextInnerTemp.getAccessibleChild(childIndexTemp);
                    }
                }, ac);
                if ( null != child ) {
                    final AccessibleContext childContext = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                        @Override
                        public AccessibleContext call() throws Exception {
                            return child.getAccessibleContext();
                        }
                    }, ac);
                    if ( null != childContext ) {
                        AccessibleRole childRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                            @Override
                            public AccessibleRole call() throws Exception {
                                return childContext.getAccessibleRole();
                            }
                        }, ac);
                        if ( AccessibleRole.LABEL == childRole ) {
                            childX = getAccessibleXcoordFromContext (childContext);
                            childY = getAccessibleYcoordFromContext (childContext);
                            childWidth = getAccessibleWidthFromContext (childContext);
                            childHeight = getAccessibleHeightFromContext (childContext);
                            if ( (childX < testX) &&
                                 ((childY <= targetY) && (targetY <= (childY + childHeight))) ) {
                                childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleName();
                                    }
                                }, ac);
                                if ( null != childName ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a LABEL object positioned to the left of the object.");
                                    references.increment (childName);
                                    return childName;
                                }
                                childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleDescription();
                                    }
                                }, ac);
                                if ( null != childDescription ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a LABEL object positioned to the left of the object.");
                                    references.increment (childDescription);
                                    return childDescription;
                                }
                            } else if ( (childY < targetY) &&
                                        ((childX <= targetX) && (targetX <= (childX + childWidth))) ) {
                                childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleName();
                                    }
                                }, ac);
                                if ( null != childName ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a LABEL object positioned above the object.");
                                    references.increment (childName);
                                    return childName;
                                }
                                childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleDescription();
                                    }
                                }, ac);
                                if ( null != childDescription ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a LABEL object positioned above the object.");
                                    references.increment (childDescription);
                                    return childDescription;
                                }
                            }
                        }
                    }
                }
                childIndex --;
            }
            childIndex = testIndex + 1;
            while (childIndex <= testIndexMax) {
                final int childIndexTemp = childIndex;
                final AccessibleContext parentContextInnerTemp = parentContext;
                final Accessible child  =  InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                    @Override
                    public Accessible call() throws Exception {
                        return parentContextInnerTemp.getAccessibleChild(childIndexTemp);
                    }
                }, ac);
                if ( null != child ) {
                    final AccessibleContext childContext = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                        @Override
                        public AccessibleContext call() throws Exception {
                            return child.getAccessibleContext();
                        }
                    }, ac);
                    if ( null != childContext ) {
                        AccessibleRole childRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                            @Override
                            public AccessibleRole call() throws Exception {
                                return childContext.getAccessibleRole();
                            }
                        }, ac);
                        if ( AccessibleRole.LABEL == childRole ) {
                            childX = getAccessibleXcoordFromContext (childContext);
                            childY = getAccessibleYcoordFromContext (childContext);
                            childWidth = getAccessibleWidthFromContext (childContext);
                            childHeight = getAccessibleHeightFromContext (childContext);
                            if ( (childX < testX) &&
                                 ((childY <= targetY) && (targetY <= (childY + childHeight))) ) {
                                childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleName();
                                    }
                                }, ac);
                                if ( null != childName ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a LABEL object positioned to the left of the object.");
                                    references.increment (childName);
                                    return childName;
                                }
                                childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleDescription();
                                    }
                                }, ac);
                                if ( null != childDescription ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a LABEL object positioned to the left of the object.");
                                    references.increment (childDescription);
                                    return childDescription;
                                }
                            } else if ( (childY < targetY) &&
                                        ((childX <= targetX) && (targetX <= (childX + childWidth))) ) {
                                childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleName();
                                    }
                                }, ac);
                                if ( null != childName ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a LABEL object positioned above the object.");
                                    references.increment (childName);
                                    return childName;
                                }
                                childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                    @Override
                                    public String call() throws Exception {
                                        return childContext.getAccessibleDescription();
                                    }
                                }, ac);
                                if ( null != childDescription ) {
                                    debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a LABEL object positioned above the object.");
                                    references.increment (childDescription);
                                    return childDescription;
                                }
                            }
                        }
                    }
                }
                childIndex ++;
            }
            /*
            Step 8:
            =======
            Special case for combo boxes and text objects, based on a
            similar special case I found in some of our internal JAWS code.

            Search for a button object that is positioned either just to the left
            or just above the object and get the Accessible Name of the button
            object.
            */
            if ( (AccessibleRole.TEXT == role) ||
                 (AccessibleRole.COMBO_BOX == role) ||
                 (bIsEditCombo) ) {
                childIndex = testIndex - 1;
                while (childIndex >= 0) {
                    final int childIndexTemp = childIndex;
                    final AccessibleContext parentContextInnerTemp = parentContext;
                    final Accessible child = InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                        @Override
                        public Accessible call() throws Exception {
                            return parentContextInnerTemp.getAccessibleChild(childIndexTemp);
                        }
                    }, ac);
                    if ( null != child ) {
                        final AccessibleContext childContext = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                            @Override
                            public AccessibleContext call() throws Exception {
                                return child.getAccessibleContext();
                            }
                        }, ac);
                        if ( null != childContext ) {
                            AccessibleRole childRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                                @Override
                                public AccessibleRole call() throws Exception {
                                    return childContext.getAccessibleRole();
                                }
                            }, ac);
                            if ( ( AccessibleRole.PUSH_BUTTON == childRole ) ||
                                 ( AccessibleRole.TOGGLE_BUTTON == childRole )) {
                                childX = getAccessibleXcoordFromContext (childContext);
                                childY = getAccessibleYcoordFromContext (childContext);
                                childWidth = getAccessibleWidthFromContext (childContext);
                                childHeight = getAccessibleHeightFromContext (childContext);
                                if ( (childX < testX) &&
                                     ((childY <= targetY) && (targetY <= (childY + childHeight))) ) {
                                    childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            return childContext.getAccessibleName();
                                        }
                                    }, ac);
                                    if ( null != childName ) {
                                        debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a PUSH_BUTTON or TOGGLE_BUTTON object positioned to the left of the object.");
                                        references.increment (childName);
                                        return childName;
                                    }
                                    childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            return childContext.getAccessibleDescription();
                                        }
                                    }, ac);
                                    if ( null != childDescription ) {
                                        debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a PUSH_BUTTON or TOGGLE_BUTTON object positioned to the left of the object.");
                                        references.increment (childDescription);
                                        return childDescription;
                                    }
                                }
                            }
                        }
                    }
                    childIndex --;
                }
                childIndex = testIndex + 1;
                while (childIndex <= testIndexMax) {
                    final int childIndexTemp = childIndex;
                    final AccessibleContext parentContextInnerTemp = parentContext;
                    final Accessible child  =  InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                        @Override
                        public Accessible call() throws Exception {
                            return parentContextInnerTemp.getAccessibleChild(childIndexTemp);
                        }
                    }, ac);
                    if ( null != child ) {
                        final AccessibleContext childContext = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                            @Override
                            public AccessibleContext call() throws Exception {
                                return child.getAccessibleContext();
                            }
                        }, ac);
                        if ( null != childContext ) {
                            AccessibleRole childRole = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                                @Override
                                public AccessibleRole call() throws Exception {
                                    return childContext.getAccessibleRole();
                                }
                            }, ac);
                            if ( ( AccessibleRole.PUSH_BUTTON == childRole ) ||
                                    ( AccessibleRole.TOGGLE_BUTTON == childRole ) ) {
                                childX = getAccessibleXcoordFromContext (childContext);
                                childY = getAccessibleYcoordFromContext (childContext);
                                childWidth = getAccessibleWidthFromContext (childContext);
                                childHeight = getAccessibleHeightFromContext (childContext);
                                if ( (childX < testX) &&
                                     ((childY <= targetY) && (targetY <= (childY + childHeight))) ) {
                                    childName = InvocationUtils.invokeAndWait(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            return childContext.getAccessibleName();
                                        }
                                    }, ac);
                                    if ( null != childName ) {
                                        debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Name of a PUSH_BUTTON or TOGGLE_BUTTON object positioned to the left of the object.");
                                        references.increment (childName);
                                        return childName;
                                    }
                                    childDescription = InvocationUtils.invokeAndWait(new Callable<String>() {
                                        @Override
                                        public String call() throws Exception {
                                            return childContext.getAccessibleDescription();
                                        }
                                    }, ac);
                                    if ( null != childDescription ) {
                                        debugString ("[INFO]: bk -- The Virtual Accessible Name was obtained from Accessible Description of a PUSH_BUTTON or TOGGLE_BUTTON object positioned to the left of the object.");
                                        references.increment (childDescription);
                                        return childDescription;
                                    }
                                }
                            }
                        }
                    }
                    childIndex ++;
                }
            }
            return null;
        } else {
            debugString ("[ERROR]: AccessBridge::getVirtualAccessibleNameFromContext error - ac == null.");
            return null;
        }
    }

    /**
     * returns the AccessibleDescription from an AccessibleContext
     */
    private String getAccessibleDescriptionFromContext(final AccessibleContext ac) {
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return ac.getAccessibleDescription();
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                debugString("[INFO]: Returning AccessibleDescription from Context: " + s);
                return s;
            }
        } else {
            debugString("[ERROR]: getAccessibleDescriptionFromContext; ac = null");
        }
        return null;
    }

    /**
     * returns the AccessibleRole from an AccessibleContext
     */
    private String getAccessibleRoleStringFromContext(final AccessibleContext ac) {
        if (ac != null) {
            AccessibleRole role = InvocationUtils.invokeAndWait(new Callable<AccessibleRole>() {
                @Override
                public AccessibleRole call() throws Exception {
                    return ac.getAccessibleRole();
                }
            }, ac);
            if (role != null) {
                String s = role.toDisplayString(Locale.US);
                if (s != null) {
                    references.increment(s);
                    debugString("[INFO]: Returning AccessibleRole from Context: " + s);
                    return s;
                }
            }
        } else {
            debugString("[ERROR]: getAccessibleRoleStringFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the AccessibleRole from an AccessibleContext in the en_US locale
     */
    private String getAccessibleRoleStringFromContext_en_US(final AccessibleContext ac) {
        return getAccessibleRoleStringFromContext(ac);
    }

    /**
     * return the AccessibleStates from an AccessibleContext
     */
    private String getAccessibleStatesStringFromContext(final AccessibleContext ac) {
        if (ac != null) {
            AccessibleStateSet stateSet = InvocationUtils.invokeAndWait(new Callable<AccessibleStateSet>() {
                @Override
                public AccessibleStateSet call() throws Exception {
                    return ac.getAccessibleStateSet();
                }
            }, ac);
            if (stateSet != null) {
                String s = stateSet.toString();
                if (s != null &&
                    s.indexOf(AccessibleState.MANAGES_DESCENDANTS.toDisplayString(Locale.US)) == -1) {
                    // Indicate whether this component manages its own
                    // children
                    AccessibleRole role = InvocationUtils.invokeAndWait(() -> {
                            return ac.getAccessibleRole();
                        }, ac);
                    if (role == AccessibleRole.LIST ||
                        role == AccessibleRole.TABLE ||
                        role == AccessibleRole.TREE) {
                        s += ",";
                        s += AccessibleState.MANAGES_DESCENDANTS.toDisplayString(Locale.US);
                    }
                    references.increment(s);
                    debugString("[INFO]: Returning AccessibleStateSet from Context: " + s);
                    return s;
                }
            }
        } else {
            debugString("[ERROR]: getAccessibleStatesStringFromContext; ac = null");
        }
        return null;
    }

    /**
     * returns the AccessibleStates from an AccessibleContext in the en_US locale
     */
    private String getAccessibleStatesStringFromContext_en_US(final AccessibleContext ac) {
        if (ac != null) {
            AccessibleStateSet stateSet = InvocationUtils.invokeAndWait(new Callable<AccessibleStateSet>() {
                @Override
                public AccessibleStateSet call() throws Exception {
                    return ac.getAccessibleStateSet();
                }
            }, ac);
            if (stateSet != null) {
                String s = "";
                AccessibleState[] states = stateSet.toArray();
                if (states != null && states.length > 0) {
                    s = states[0].toDisplayString(Locale.US);
                    for (int i = 1; i < states.length; i++) {
                        s = s + "," + states[i].toDisplayString(Locale.US);
                    }
                }
                references.increment(s);
                debugString("[INFO]: Returning AccessibleStateSet en_US from Context: " + s);
                return s;
            }
        }
        debugString("[ERROR]: getAccessibleStatesStringFromContext; ac = null");
        return null;
    }

    /**
     * returns the AccessibleParent from an AccessibleContext
     */
    private AccessibleContext getAccessibleParentFromContext(final AccessibleContext ac) {
        if (ac==null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                Accessible a = ac.getAccessibleParent();
                if (a != null) {
                    AccessibleContext apc = a.getAccessibleContext();
                    if (apc != null) {
                        return apc;
                    }
                }
                return null;
            }
        }, ac);
    }

    /**
     * returns the AccessibleIndexInParent from an AccessibleContext
     */
    private int getAccessibleIndexInParentFromContext(final AccessibleContext ac) {
        if (ac==null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ac.getAccessibleIndexInParent();
            }
        }, ac);
    }

    /**
     * returns the AccessibleChild count from an AccessibleContext
     */
    private int getAccessibleChildrenCountFromContext(final AccessibleContext ac) {
        if (ac==null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ac.getAccessibleChildrenCount();
            }
        }, ac);
    }

    /**
     * returns the AccessibleChild Context from an AccessibleContext
     */
    private AccessibleContext getAccessibleChildFromContext(final AccessibleContext ac, final int index) {

        if (ac == null) {
            return null;
        }

        final JTable table = InvocationUtils.invokeAndWait(new Callable<JTable>() {
            @Override
            public JTable call() throws Exception {
                // work-around for AccessibleJTable.getCurrentAccessibleContext returning
                // wrong renderer component when cell contains more than one component
                Accessible parent = ac.getAccessibleParent();
                if (parent != null) {
                    int indexInParent = ac.getAccessibleIndexInParent();
                    Accessible child =
                            parent.getAccessibleContext().getAccessibleChild(indexInParent);
                    if (child instanceof JTable) {
                        return (JTable) child;
                    }
                }
                return null;
            }
        }, ac);

        if (table == null) {
            return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                @Override
                public AccessibleContext call() throws Exception {
                    Accessible a = ac.getAccessibleChild(index);
                    if (a != null) {
                        return a.getAccessibleContext();
                    }
                    return null;
                }
            }, ac);
        }

        final AccessibleTable at = getAccessibleTableFromContext(ac);

        final int row = getAccessibleTableRow(at, index);
        final int column = getAccessibleTableColumn(at, index);

        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                if (renderer == null) {
                    Class<?> columnClass = table.getColumnClass(column);
                    renderer = table.getDefaultRenderer(columnClass);
                }
                Component component =
                        renderer.getTableCellRendererComponent(table, table.getValueAt(row, column),
                                false, false, row, column);
                if (component instanceof Accessible) {
                    return component.getAccessibleContext();
                }
                return null;
            }
        }, ac);
    }

    /**
     * returns the AccessibleComponent bounds on screen from an AccessibleContext
     */
    private Rectangle getAccessibleBoundsOnScreenFromContext(final AccessibleContext ac) {
        if(ac==null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<Rectangle>() {
            @Override
            public Rectangle call() throws Exception {
                AccessibleComponent acmp = ac.getAccessibleComponent();
                if (acmp != null) {
                    Rectangle r = acmp.getBounds();
                    if (r != null) {
                        try {
                            Point p = acmp.getLocationOnScreen();
                            if (p != null) {
                                r.x = p.x;
                                r.y = p.y;
                                return r;
                            }
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
                return null;
            }
        }, ac);
    }

    /**
     * returns the AccessibleComponent x-coord from an AccessibleContext
     */
    private int getAccessibleXcoordFromContext(AccessibleContext ac) {
        if (ac != null) {
            Rectangle r = getAccessibleBoundsOnScreenFromContext(ac);
            if (r != null) {
                debugString("[INFO]: Returning Accessible x coord from Context: " + r.x);
                return r.x;
            }
        } else {
            debugString("[ERROR]: getAccessibleXcoordFromContext ac = null");
        }
        return -1;
    }

    /**
     * returns the AccessibleComponent y-coord from an AccessibleContext
     */
    private int getAccessibleYcoordFromContext(AccessibleContext ac) {
        debugString("[INFO]: getAccessibleYcoordFromContext() called");
        if (ac != null) {
            Rectangle r = getAccessibleBoundsOnScreenFromContext(ac);
            if (r != null) {
                return r.y;
            }
        } else {
        debugString("[ERROR]: getAccessibleYcoordFromContext; ac = null");
        }
        return -1;
    }

    /**
     * returns the AccessibleComponent height from an AccessibleContext
     */
    private int getAccessibleHeightFromContext(AccessibleContext ac) {
        if (ac != null) {
            Rectangle r = getAccessibleBoundsOnScreenFromContext(ac);
            if (r != null) {
                return r.height;
            }
        } else {
            debugString("[ERROR]: getAccessibleHeightFromContext; ac = null");
        }
        return -1;
    }

    /**
     * returns the AccessibleComponent width from an AccessibleContext
     */
    private int getAccessibleWidthFromContext(AccessibleContext ac) {
        if (ac != null) {
            Rectangle r = getAccessibleBoundsOnScreenFromContext(ac);
            if (r != null) {
                return r.width;
            }
        } else {
            debugString("[ERROR]: getAccessibleWidthFromContext; ac = null");
        }
        return -1;
    }


    /**
     * returns the AccessibleComponent from an AccessibleContext
     */
    private AccessibleComponent getAccessibleComponentFromContext(AccessibleContext ac) {
        if (ac != null) {
            AccessibleComponent acmp = InvocationUtils.invokeAndWait(() -> {
                    return ac.getAccessibleComponent();
                }, ac);
            if (acmp != null) {
                debugString("[INFO]: Returning AccessibleComponent Context");
                return acmp;
            }
        } else {
            debugString("[ERROR]: getAccessibleComponentFromContext; ac = null");
        }
        return null;
    }

    /**
     * returns the AccessibleAction from an AccessibleContext
     */
    private AccessibleAction getAccessibleActionFromContext(final AccessibleContext ac) {
        debugString("[INFO]: Returning AccessibleAction Context");
        return ac == null ? null : InvocationUtils.invokeAndWait(new Callable<AccessibleAction>() {
            @Override
            public AccessibleAction call() throws Exception {
                return ac.getAccessibleAction();
            }
        }, ac);
    }

    /**
     * returns the AccessibleSelection from an AccessibleContext
     */
    private AccessibleSelection getAccessibleSelectionFromContext(final AccessibleContext ac) {
        return ac == null ? null : InvocationUtils.invokeAndWait(new Callable<AccessibleSelection>() {
            @Override
            public AccessibleSelection call() throws Exception {
                return ac.getAccessibleSelection();
            }
        }, ac);
    }

    /**
     * return the AccessibleText from an AccessibleContext
     */
    private AccessibleText getAccessibleTextFromContext(final AccessibleContext ac) {
        return ac == null ? null : InvocationUtils.invokeAndWait(new Callable<AccessibleText>() {
            @Override
            public AccessibleText call() throws Exception {
                return ac.getAccessibleText();
            }
        }, ac);
    }

    /**
     * return the AccessibleComponent from an AccessibleContext
     */
    private AccessibleValue getAccessibleValueFromContext(final AccessibleContext ac) {
        return ac == null ? null : InvocationUtils.invokeAndWait(new Callable<AccessibleValue>() {
            @Override
            public AccessibleValue call() throws Exception {
                return ac.getAccessibleValue();
            }
        }, ac);
    }

    /* ===== AccessibleText methods ===== */

    /**
     * returns the bounding rectangle for the text cursor
     * XXX
     */
    private Rectangle getCaretLocation(final AccessibleContext ac) {
    debugString("[INFO]: getCaretLocation");
        if (ac==null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<Rectangle>() {
            @Override
            public Rectangle call() throws Exception {
                // workaround for JAAPI not returning cursor bounding rectangle
                Rectangle r = null;
                Accessible parent = ac.getAccessibleParent();
                if (parent instanceof Accessible) {
                    int indexInParent = ac.getAccessibleIndexInParent();
                    Accessible child =
                            parent.getAccessibleContext().getAccessibleChild(indexInParent);

                    if (child instanceof JTextComponent) {
                        JTextComponent text = (JTextComponent) child;
                        try {
                            r = text.modelToView2D(text.getCaretPosition()).getBounds();
                            if (r != null) {
                                Point p = text.getLocationOnScreen();
                                r.translate(p.x, p.y);
                            }
                        } catch (BadLocationException ble) {
                        }
                    }
                }
                return r;
            }
        }, ac);
    }

    /**
     * returns the x-coordinate for the text cursor rectangle
     */
    private int getCaretLocationX(AccessibleContext ac) {
        Rectangle r = getCaretLocation(ac);
        if (r != null) {
            return r.x;
        } else {
            return -1;
        }
    }

    /**
     * returns the y-coordinate for the text cursor rectangle
     */
    private int getCaretLocationY(AccessibleContext ac) {
        Rectangle r = getCaretLocation(ac);
        if (r != null) {
            return r.y;
        } else {
            return -1;
        }
    }

    /**
     * returns the height for the text cursor rectangle
     */
    private int getCaretLocationHeight(AccessibleContext ac) {
        Rectangle r = getCaretLocation(ac);
        if (r != null) {
            return r.height;
        } else {
            return -1;
        }
    }

    /**
     * returns the width for the text cursor rectangle
     */
    private int getCaretLocationWidth(AccessibleContext ac) {
        Rectangle r = getCaretLocation(ac);
        if (r != null) {
            return r.width;
        } else {
            return -1;
        }
    }

    /**
     * returns the character count from an AccessibleContext
     */
    private int getAccessibleCharCountFromContext(final AccessibleContext ac) {
        if (ac==null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    return at.getCharCount();
                }
                return -1;
            }
        }, ac);
    }

    /**
     * returns the caret position from an AccessibleContext
     */
    private int getAccessibleCaretPositionFromContext(final AccessibleContext ac) {
        if (ac==null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    return at.getCaretPosition();
                }
                return -1;
            }
        }, ac);
    }

    /**
     * Return the index at a specific point from an AccessibleContext
     * Point(x, y) is in screen coordinates.
     */
    private int getAccessibleIndexAtPointFromContext(final AccessibleContext ac,
                                                    final int x, final int y) {
        debugString("[INFO]: getAccessibleIndexAtPointFromContext: x = "+x+"; y = "+y);
        if (ac==null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                AccessibleComponent acomp = ac.getAccessibleComponent();
                if (at != null && acomp != null) {
                    // Convert x and y from screen coordinates to
                    // local coordinates.
                    try {
                        Point p = acomp.getLocationOnScreen();
                        int x1, y1;
                        if (p != null) {
                            x1 = x - p.x;
                            if (x1 < 0) {
                                x1 = 0;
                            }
                            y1 = y - p.y;
                            if (y1 < 0) {
                                y1 = 0;
                            }

                            Point newPoint = new Point(x1, y1);
                            int indexAtPoint = at.getIndexAtPoint(new Point(x1, y1));
                            return indexAtPoint;
                        }
                    } catch (Exception e) {
                    }
                }
                return -1;
            }
        }, ac);
    }

    /**
     * return the letter at a specific point from an AccessibleContext
     */
    private String getAccessibleLetterAtIndexFromContext(final AccessibleContext ac, final int index) {
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    AccessibleText at = ac.getAccessibleText();
                    if (at == null) return null;
                    return at.getAtIndex(AccessibleText.CHARACTER, index);
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getAccessibleLetterAtIndexFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the word at a specific point from an AccessibleContext
     */
    private String getAccessibleWordAtIndexFromContext(final AccessibleContext ac, final int index) {
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    AccessibleText at = ac.getAccessibleText();
                    if (at == null) return null;
                    return at.getAtIndex(AccessibleText.WORD, index);
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getAccessibleWordAtIndexFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the sentence at a specific point from an AccessibleContext
     */
    private String getAccessibleSentenceAtIndexFromContext(final AccessibleContext ac, final int index) {
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    AccessibleText at = ac.getAccessibleText();
                    if (at == null) return null;
                    return at.getAtIndex(AccessibleText.SENTENCE, index);
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getAccessibleSentenceAtIndexFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the text selection start from an AccessibleContext
     */
    private int getAccessibleTextSelectionStartFromContext(final AccessibleContext ac) {
        if (ac == null) return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    return at.getSelectionStart();
                }
                return -1;
            }
        }, ac);
    }

    /**
     * return the text selection end from an AccessibleContext
     */
    private int getAccessibleTextSelectionEndFromContext(final AccessibleContext ac) {
        if (ac == null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    return at.getSelectionEnd();
                }
                return -1;
            }
        }, ac);
    }

    /**
     * return the selected text from an AccessibleContext
     */
    private String getAccessibleTextSelectedTextFromContext(final AccessibleContext ac) {
        if (ac != null) {
            String s = InvocationUtils.invokeAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    AccessibleText at = ac.getAccessibleText();
                    if (at == null) return null;
                    return at.getSelectedText();
                }
            }, ac);
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getAccessibleTextSelectedTextFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the attribute string at a given index from an AccessibleContext
     */
    private String getAccessibleAttributesAtIndexFromContext(final AccessibleContext ac,
                                                             final int index) {
        if (ac == null)
            return null;
        AttributeSet as = InvocationUtils.invokeAndWait(new Callable<AttributeSet>() {
            @Override
            public AttributeSet call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    return at.getCharacterAttribute(index);
                }
                return null;
            }
        }, ac);
        String s = expandStyleConstants(as);
        if (s != null) {
            references.increment(s);
            return s;
        }
        return null;
    }

    /**
     * Get line info: left index of line
     *
     * algorithm:  cast back, doubling each time,
     *             'till find line boundaries
     *
     * return -1 if we can't get the info (e.g. index or at passed in
     * is bogus; etc.)
     */
    private int getAccessibleTextLineLeftBoundsFromContext(final AccessibleContext ac,
                                                          final int index) {
        if (ac == null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    int lineStart;
                    int offset;
                    Rectangle charRect;
                    Rectangle indexRect = at.getCharacterBounds(index);
                    int textLen = at.getCharCount();
                    if (indexRect == null) {
                        return -1;
                    }
                    // find the start of the line
                    //
                    offset = 1;
                    lineStart = index - offset < 0 ? 0 : index - offset;
                    charRect = at.getCharacterBounds(lineStart);
                    // slouch behind beginning of line
                    while (charRect != null
                            && charRect.y >= indexRect.y
                            && lineStart > 0) {
                        offset = offset << 1;
                        lineStart = index - offset < 0 ? 0 : index - offset;
                        charRect = at.getCharacterBounds(lineStart);
                    }
                    if (lineStart == 0) {    // special case: we're on the first line!
                        // we found it!
                    } else {
                        offset = offset >> 1;   // know boundary within last expansion
                        // ground forward to beginning of line
                        while (offset > 0) {
                            charRect = at.getCharacterBounds(lineStart + offset);
                            if (charRect.y < indexRect.y) { // still before line
                                lineStart += offset;
                            } else {
                                // leave lineStart alone, it's close!
                            }
                            offset = offset >> 1;
                        }
                        // subtract one 'cause we're already too far...
                        lineStart += 1;
                    }
                    return lineStart;
                }
                return -1;
            }
        }, ac);
    }

    /**
     * Get line info: right index of line
     *
     * algorithm:  cast back, doubling each time,
     *             'till find line boundaries
     *
     * return -1 if we can't get the info (e.g. index or at passed in
     * is bogus; etc.)
     */
    private int getAccessibleTextLineRightBoundsFromContext(final AccessibleContext ac, final int index) {
        if(ac == null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (at != null) {
                    int lineEnd;
                    int offset;
                    Rectangle charRect;
                    Rectangle indexRect = at.getCharacterBounds(index);
                    int textLen = at.getCharCount();
                    if (indexRect == null) {
                        return -1;
                    }
                    // find the end of the line
                    //
                    offset = 1;
                    lineEnd = index + offset > textLen - 1
                            ? textLen - 1 : index + offset;
                    charRect = at.getCharacterBounds(lineEnd);
                    // push past end of line
                    while (charRect != null &&
                            charRect.y <= indexRect.y &&
                            lineEnd < textLen - 1) {
                        offset = offset << 1;
                        lineEnd = index + offset > textLen - 1
                                ? textLen - 1 : index + offset;
                        charRect = at.getCharacterBounds(lineEnd);
                    }
                    if (lineEnd == textLen - 1) {    // special case: on the last line!
                        // we found it!
                    } else {
                        offset = offset >> 1;   // know boundary within last expansion
                        // pull back to end of line
                        while (offset > 0) {
                            charRect = at.getCharacterBounds(lineEnd - offset);
                            if (charRect.y > indexRect.y) { // still beyond line
                                lineEnd -= offset;
                            } else {
                                // leave lineEnd alone, it's close!
                            }
                            offset = offset >> 1;
                        }
                        // subtract one 'cause we're already too far...
                        lineEnd -= 1;
                    }
                    return lineEnd;
                }
                return -1;
            }
        }, ac);
    }

    /**
     * Get a range of text; null if indicies are bogus
     */
    private String getAccessibleTextRangeFromContext(final AccessibleContext ac,
                                                    final int start, final int end) {
        String s = InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (ac != null) {
                    AccessibleText at = ac.getAccessibleText();
                    if (at != null) {
                        // start - end is inclusive
                        if (start > end) {
                            return null;
                        }
                        if (end >= at.getCharCount()) {
                            return null;
                        }
                        StringBuffer buf = new StringBuffer(end - start + 1);
                        for (int i = start; i <= end; i++) {
                            buf.append(at.getAtIndex(AccessibleText.CHARACTER, i));
                        }
                        return buf.toString();
                    }
                }
                return null;
            }
        }, ac);
        if (s != null) {
            references.increment(s);
            return s;
        } else {
            return null;
        }
    }

    /**
     * return the AttributeSet object at a given index from an AccessibleContext
     */
    private AttributeSet getAccessibleAttributeSetAtIndexFromContext(final AccessibleContext ac,
                                                                    final int index) {
        return InvocationUtils.invokeAndWait(new Callable<AttributeSet>() {
            @Override
            public AttributeSet call() throws Exception {
                if (ac != null) {
                    AccessibleText at = ac.getAccessibleText();
                    if (at != null) {
                        AttributeSet as = at.getCharacterAttribute(index);
                        if (as != null) {
                            AccessBridge.this.references.increment(as);
                            return as;
                        }
                    }
                }
                return null;
            }
        }, ac);
    }


    /**
     * return the bounding rectangle at index from an AccessibleContext
     */
    private Rectangle getAccessibleTextRectAtIndexFromContext(final AccessibleContext ac,
                                                        final int index) {
        // want to do this in global coords, so need to combine w/ac global coords
        Rectangle r = InvocationUtils.invokeAndWait(new Callable<Rectangle>() {
            @Override
            public Rectangle call() throws Exception {
                // want to do this in global coords, so need to combine w/ac global coords
                if (ac != null) {
                    AccessibleText at = ac.getAccessibleText();
                    if (at != null) {
                        Rectangle rect = at.getCharacterBounds(index);
                        if (rect != null) {
                            String s = at.getAtIndex(AccessibleText.CHARACTER, index);
                            if (s != null && s.equals("\n")) {
                                rect.width = 0;
                            }
                            return rect;
                        }
                    }
                }
                return null;
            }
        }, ac);
        Rectangle acRect = getAccessibleBoundsOnScreenFromContext(ac);
        if (r != null && acRect != null) {
            r.translate(acRect.x, acRect.y);
            return r;
        }
        return null;
    }

    /**
     * return the AccessibleText character x-coord at index from an AccessibleContext
     */
    private int getAccessibleXcoordTextRectAtIndexFromContext(AccessibleContext ac, int index) {
        if (ac != null) {
            Rectangle r = getAccessibleTextRectAtIndexFromContext(ac, index);
            if (r != null) {
                return r.x;
            }
        } else {
            debugString("[ERROR]: getAccessibleXcoordTextRectAtIndexFromContext; ac = null");
        }
        return -1;
    }

    /**
     * return the AccessibleText character y-coord at index from an AccessibleContext
     */
    private int getAccessibleYcoordTextRectAtIndexFromContext(AccessibleContext ac, int index) {
        if (ac != null) {
            Rectangle r = getAccessibleTextRectAtIndexFromContext(ac, index);
            if (r != null) {
                return r.y;
            }
        } else {
            debugString("[ERROR]: getAccessibleYcoordTextRectAtIndexFromContext; ac = null");
        }
        return -1;
    }

    /**
     * return the AccessibleText character height at index from an AccessibleContext
     */
    private int getAccessibleHeightTextRectAtIndexFromContext(AccessibleContext ac, int index) {
        if (ac != null) {
            Rectangle r = getAccessibleTextRectAtIndexFromContext(ac, index);
            if (r != null) {
                return r.height;
            }
        } else {
            debugString("[ERROR]: getAccessibleHeightTextRectAtIndexFromContext; ac = null");
        }
        return -1;
    }

    /**
     * return the AccessibleText character width at index from an AccessibleContext
     */
    private int getAccessibleWidthTextRectAtIndexFromContext(AccessibleContext ac, int index) {
        if (ac != null) {
            Rectangle r = getAccessibleTextRectAtIndexFromContext(ac, index);
            if (r != null) {
                return r.width;
            }
        } else {
            debugString("[ERROR]: getAccessibleWidthTextRectAtIndexFromContext; ac = null");
        }
        return -1;
    }

    /* ===== AttributeSet methods for AccessibleText ===== */

    /**
     * return the bold setting from an AttributeSet
     */
    private boolean getBoldFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isBold(as);
        } else {
            debugString("[ERROR]: getBoldFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the italic setting from an AttributeSet
     */
    private boolean getItalicFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isItalic(as);
        } else {
            debugString("[ERROR]: getItalicFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the underline setting from an AttributeSet
     */
    private boolean getUnderlineFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isUnderline(as);
        } else {
            debugString("[ERROR]: getUnderlineFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the strikethrough setting from an AttributeSet
     */
    private boolean getStrikethroughFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isStrikeThrough(as);
        } else {
            debugString("[ERROR]: getStrikethroughFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the superscript setting from an AttributeSet
     */
    private boolean getSuperscriptFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isSuperscript(as);
        } else {
            debugString("[ERROR]: getSuperscriptFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the subscript setting from an AttributeSet
     */
    private boolean getSubscriptFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.isSubscript(as);
        } else {
            debugString("[ERROR]: getSubscriptFromAttributeSet; as = null");
        }
        return false;
    }

    /**
     * return the background color from an AttributeSet
     */
    private String getBackgroundColorFromAttributeSet(AttributeSet as) {
        if (as != null) {
            String s = StyleConstants.getBackground(as).toString();
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getBackgroundColorFromAttributeSet; as = null");
        }
        return null;
    }

    /**
     * return the foreground color from an AttributeSet
     */
    private String getForegroundColorFromAttributeSet(AttributeSet as) {
        if (as != null) {
            String s = StyleConstants.getForeground(as).toString();
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getForegroundColorFromAttributeSet; as = null");
        }
        return null;
    }

    /**
     * return the font family from an AttributeSet
     */
    private String getFontFamilyFromAttributeSet(AttributeSet as) {
        if (as != null) {
            String s = StyleConstants.getFontFamily(as).toString();
            if (s != null) {
                references.increment(s);
                return s;
            }
        } else {
            debugString("[ERROR]: getFontFamilyFromAttributeSet; as = null");
        }
        return null;
    }

    /**
     * return the font size from an AttributeSet
     */
    private int getFontSizeFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getFontSize(as);
        } else {
            debugString("[ERROR]: getFontSizeFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the alignment from an AttributeSet
     */
    private int getAlignmentFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getAlignment(as);
        } else {
            debugString("[ERROR]: getAlignmentFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the BiDi level from an AttributeSet
     */
    private int getBidiLevelFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getBidiLevel(as);
        } else {
            debugString("[ERROR]: getBidiLevelFromAttributeSet; as = null");
        }
        return -1;
    }


    /**
     * return the first line indent from an AttributeSet
     */
    private float getFirstLineIndentFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getFirstLineIndent(as);
        } else {
            debugString("[ERROR]: getFirstLineIndentFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the left indent from an AttributeSet
     */
    private float getLeftIndentFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getLeftIndent(as);
        } else {
            debugString("[ERROR]: getLeftIndentFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the right indent from an AttributeSet
     */
    private float getRightIndentFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getRightIndent(as);
        } else {
            debugString("[ERROR]: getRightIndentFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the line spacing from an AttributeSet
     */
    private float getLineSpacingFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getLineSpacing(as);
        } else {
            debugString("[ERROR]: getLineSpacingFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the space above from an AttributeSet
     */
    private float getSpaceAboveFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getSpaceAbove(as);
        } else {
            debugString("[ERROR]: getSpaceAboveFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * return the space below from an AttributeSet
     */
    private float getSpaceBelowFromAttributeSet(AttributeSet as) {
        if (as != null) {
            return StyleConstants.getSpaceBelow(as);
        } else {
            debugString("[ERROR]: getSpaceBelowFromAttributeSet; as = null");
        }
        return -1;
    }

    /**
     * Enumerate all StyleConstants in the AttributeSet
     *
     * We need to check explicitly, 'cause of the HTML package conversion
     * mechanism (they may not be stored as StyleConstants, just translated
     * to them when asked).
     *
     * (Use convenience methods where they are defined...)
     *
     * Not checking the following (which the IBM SNS guidelines says
     * should be defined):
     *    - ComponentElementName
     *    - IconElementName
     *    - NameAttribute
     *    - ResolveAttribute
     */
    private String expandStyleConstants(AttributeSet as) {
        Color c;
        Object o;
        String attrString = "";

        // ---------- check for various Character Constants

        attrString += "BidiLevel = " + StyleConstants.getBidiLevel(as);

        final Component comp = StyleConstants.getComponent(as);
        if (comp != null) {
            if (comp instanceof Accessible) {
                final AccessibleContext ac = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return comp.getAccessibleContext();
                    }
                }, comp);
                if (ac != null) {
                    attrString += "; Accessible Component = " + InvocationUtils.invokeAndWait(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return ac.getAccessibleName();
                        }
                    }, ac);
                } else {
                    attrString += "; Innaccessible Component = " + comp;
                }
            } else {
                attrString += "; Innaccessible Component = " + comp;
            }
        }

        Icon i = StyleConstants.getIcon(as);
        if (i != null) {
            if (i instanceof ImageIcon) {
                attrString += "; ImageIcon = " + ((ImageIcon) i).getDescription();
            } else {
                attrString += "; Icon = " + i;
            }
        }

        attrString += "; FontFamily = " + StyleConstants.getFontFamily(as);

        attrString += "; FontSize = " + StyleConstants.getFontSize(as);

        if (StyleConstants.isBold(as)) {
            attrString += "; bold";
        }

        if (StyleConstants.isItalic(as)) {
            attrString += "; italic";
        }

        if (StyleConstants.isUnderline(as)) {
            attrString += "; underline";
        }

        if (StyleConstants.isStrikeThrough(as)) {
            attrString += "; strikethrough";
        }

        if (StyleConstants.isSuperscript(as)) {
            attrString += "; superscript";
        }

        if (StyleConstants.isSubscript(as)) {
            attrString += "; subscript";
        }

        c = StyleConstants.getForeground(as);
        if (c != null) {
            attrString += "; Foreground = " + c;
        }

        c = StyleConstants.getBackground(as);
        if (c != null) {
            attrString += "; Background = " + c;
        }

        attrString += "; FirstLineIndent = " + StyleConstants.getFirstLineIndent(as);

        attrString += "; RightIndent = " + StyleConstants.getRightIndent(as);

        attrString += "; LeftIndent = " + StyleConstants.getLeftIndent(as);

        attrString += "; LineSpacing = " + StyleConstants.getLineSpacing(as);

        attrString += "; SpaceAbove = " + StyleConstants.getSpaceAbove(as);

        attrString += "; SpaceBelow = " + StyleConstants.getSpaceBelow(as);

        attrString += "; Alignment = " + StyleConstants.getAlignment(as);

        TabSet ts = StyleConstants.getTabSet(as);
        if (ts != null) {
            attrString += "; TabSet = " + ts;
        }

        return attrString;
    }


    /* ===== AccessibleValue methods ===== */

    /**
     * return the AccessibleValue current value from an AccessibleContext
     * returned using a String 'cause the value is a java Number
     *
     */
    private String getCurrentAccessibleValueFromContext(final AccessibleContext ac) {
        if (ac != null) {
            final Number value = InvocationUtils.invokeAndWait(new Callable<Number>() {
                @Override
                public Number call() throws Exception {
                    AccessibleValue av = ac.getAccessibleValue();
                    if (av == null) return null;
                    return av.getCurrentAccessibleValue();
                }
            }, ac);
            if (value != null) {
                String s = value.toString();
                if (s != null) {
                    references.increment(s);
                    return s;
                }
            }
        } else {
            debugString("[ERROR]: getCurrentAccessibleValueFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the AccessibleValue maximum value from an AccessibleContext
     * returned using a String 'cause the value is a java Number
     *
     */
    private String getMaximumAccessibleValueFromContext(final AccessibleContext ac) {
        if (ac != null) {
            final Number value = InvocationUtils.invokeAndWait(new Callable<Number>() {
                @Override
                public Number call() throws Exception {
                    AccessibleValue av = ac.getAccessibleValue();
                    if (av == null) return null;
                    return av.getMaximumAccessibleValue();
                }
            }, ac);
            if (value != null) {
                String s = value.toString();
                if (s != null) {
                    references.increment(s);
                    return s;
                }
            }
        } else {
            debugString("[ERROR]: getMaximumAccessibleValueFromContext; ac = null");
        }
        return null;
    }

    /**
     * return the AccessibleValue minimum value from an AccessibleContext
     * returned using a String 'cause the value is a java Number
     *
     */
    private String getMinimumAccessibleValueFromContext(final AccessibleContext ac) {
        if (ac != null) {
            final Number value = InvocationUtils.invokeAndWait(new Callable<Number>() {
                @Override
                public Number call() throws Exception {
                    AccessibleValue av = ac.getAccessibleValue();
                    if (av == null) return null;
                    return av.getMinimumAccessibleValue();
                }
            }, ac);
            if (value != null) {
                String s = value.toString();
                if (s != null) {
                    references.increment(s);
                    return s;
                }
            }
        } else {
            debugString("[ERROR]: getMinimumAccessibleValueFromContext; ac = null");
        }
        return null;
    }


    /* ===== AccessibleSelection methods ===== */

    /**
     * add to the AccessibleSelection of an AccessibleContext child i
     *
     */
    private void addAccessibleSelectionFromContext(final AccessibleContext ac, final int i) {
        try {
            InvocationUtils.invokeAndWait(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (ac != null) {
                        AccessibleSelection as = ac.getAccessibleSelection();
                        if (as != null) {
                            as.addAccessibleSelection(i);
                        }
                    }
                    return null;
                }
            }, ac);
        } catch(Exception e){}
    }

    /**
     * clear all of the AccessibleSelection of an AccessibleContex
     *
     */
    private void clearAccessibleSelectionFromContext(final AccessibleContext ac) {
        try {
            InvocationUtils.invokeAndWait(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        as.clearAccessibleSelection();
                    }
                    return null;
                }
            }, ac);
        } catch(Exception e){}

    }

    /**
     * get the AccessibleContext of the i-th AccessibleSelection of an AccessibleContext
     *
     */
    private AccessibleContext getAccessibleSelectionFromContext(final AccessibleContext ac, final int i) {
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                if (ac != null) {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        Accessible a = as.getAccessibleSelection(i);
                        if (a == null)
                            return null;
                        else
                            return a.getAccessibleContext();
                    }
                }
                return null;
            }
        }, ac);
    }

    /**
     * get number of things selected in the AccessibleSelection of an AccessibleContext
     *
     */
    private int getAccessibleSelectionCountFromContext(final AccessibleContext ac) {
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (ac != null) {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        return as.getAccessibleSelectionCount();
                    }
                }
                return -1;
            }
        }, ac);
    }

    /**
     * return true if the i-th child of the AccessibleSelection of an AccessibleContext is selected
     *
     */
    private boolean isAccessibleChildSelectedFromContext(final AccessibleContext ac, final int i) {
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (ac != null) {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        return as.isAccessibleChildSelected(i);
                    }
                }
                return false;
            }
        }, ac);
    }

    /**
     * remove the i-th child from the AccessibleSelection of an AccessibleContext
     *
     */
    private void removeAccessibleSelectionFromContext(final AccessibleContext ac, final int i) {
        InvocationUtils.invokeAndWait(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                if (ac != null) {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        as.removeAccessibleSelection(i);
                    }
                }
                return null;
            }
        }, ac);
    }

    /**
     * select all (if possible) of the children of the AccessibleSelection of an AccessibleContext
     *
     */
    private void selectAllAccessibleSelectionFromContext(final AccessibleContext ac) {
            InvocationUtils.invokeAndWait(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    if (ac != null) {
                        AccessibleSelection as = ac.getAccessibleSelection();
                        if (as != null) {
                            as.selectAllAccessibleSelection();
                        }
                    }
                    return null;
                }
            }, ac);
    }

    // ======== AccessibleTable ========

    ConcurrentHashMap<AccessibleTable,AccessibleContext> hashtab = new ConcurrentHashMap<>();

    /**
     * returns the AccessibleTable for an AccessibleContext
     */
    private AccessibleTable getAccessibleTableFromContext(final AccessibleContext ac) {
        return InvocationUtils.invokeAndWait(new Callable<AccessibleTable>() {
            @Override
            public AccessibleTable call() throws Exception {
                if (ac != null) {
                    AccessibleTable at = ac.getAccessibleTable();
                    if (at != null) {
                        AccessBridge.this.hashtab.put(at, ac);
                        return at;
                    }
                }
                return null;
            }
        }, ac);
    }


    /*
     * returns the AccessibleContext that contains an AccessibleTable
     */
    private AccessibleContext getContextFromAccessibleTable(AccessibleTable at) {
        return hashtab.get(at);
    }

    /*
     * returns the row count for an AccessibleTable
     */
    private int getAccessibleTableRowCount(final AccessibleContext ac) {
        debugString("[INFO]: ##### getAccessibleTableRowCount");
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (ac != null) {
                    AccessibleTable at = ac.getAccessibleTable();
                    if (at != null) {
                        return at.getAccessibleRowCount();
                    }
                }
                return -1;
            }
        }, ac);
    }

    /*
     * returns the column count for an AccessibleTable
     */
    private int getAccessibleTableColumnCount(final AccessibleContext ac) {
        debugString("[INFO]: ##### getAccessibleTableColumnCount");
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (ac != null) {
                    AccessibleTable at = ac.getAccessibleTable();
                    if (at != null) {
                        return at.getAccessibleColumnCount();
                    }
                }
                return -1;
            }
        }, ac);
    }

    /*
     * returns the AccessibleContext for an AccessibleTable cell
     */
    private AccessibleContext getAccessibleTableCellAccessibleContext(final AccessibleTable at,
                                                                      final int row, final int column) {
        debugString("[INFO]: getAccessibleTableCellAccessibleContext: at = "+at.getClass());
        if (at == null) return null;
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                if (!(at instanceof AccessibleContext)) {
                    Accessible a = at.getAccessibleAt(row, column);
                    if (a != null) {
                        return a.getAccessibleContext();
                    }
                } else {
                    // work-around for AccessibleJTable.getCurrentAccessibleContext returning
                    // wrong renderer component when cell contains more than one component
                    AccessibleContext ac = (AccessibleContext) at;
                    Accessible parent = ac.getAccessibleParent();
                    if (parent != null) {
                        int indexInParent = ac.getAccessibleIndexInParent();
                        Accessible child =
                                parent.getAccessibleContext().getAccessibleChild(indexInParent);
                        if (child instanceof JTable) {
                            JTable table = (JTable) child;

                            TableCellRenderer renderer = table.getCellRenderer(row, column);
                            if (renderer == null) {
                                Class<?> columnClass = table.getColumnClass(column);
                                renderer = table.getDefaultRenderer(columnClass);
                            }
                            Component component =
                                    renderer.getTableCellRendererComponent(table, table.getValueAt(row, column),
                                            false, false, row, column);
                            if (component instanceof Accessible) {
                                return component.getAccessibleContext();
                            }
                        }
                    }
                }
                return null;
            }
        }, getContextFromAccessibleTable(at));
    }

    /*
     * returns the index of a cell at a given row and column in an AccessibleTable
     */
    private int getAccessibleTableCellIndex(final AccessibleTable at, int row, int column) {
        debugString("[INFO]: ##### getAccessibleTableCellIndex: at="+at);
        if (at != null) {
            int cellIndex = row *
                InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return at.getAccessibleColumnCount();
                    }
                }, getContextFromAccessibleTable(at)) +
                column;
            debugString("[INFO]:    ##### getAccessibleTableCellIndex="+cellIndex);
            return cellIndex;
        }
        debugString("[ERROR]: ##### getAccessibleTableCellIndex FAILED");
        return -1;
    }

    /*
     * returns the row extent of a cell at a given row and column in an AccessibleTable
     */
    private int getAccessibleTableCellRowExtent(final AccessibleTable at, final int row, final int column) {
        debugString("[INFO]: ##### getAccessibleTableCellRowExtent");
        if (at != null) {
            int rowExtent = InvocationUtils.invokeAndWait(new Callable<Integer>() {
                                                              @Override
                                                              public Integer call() throws Exception {
                                                                  return at.getAccessibleRowExtentAt(row, column);
                                                              }
                                                          },
                    getContextFromAccessibleTable(at));
            debugString("[INFO]:   ##### getAccessibleTableCellRowExtent="+rowExtent);
            return rowExtent;
        }
        debugString("[ERROR]: ##### getAccessibleTableCellRowExtent FAILED");
        return -1;
    }

    /*
     * returns the column extent of a cell at a given row and column in an AccessibleTable
     */
    private int getAccessibleTableCellColumnExtent(final AccessibleTable at, final int row, final int column) {
        debugString("[INFO]: ##### getAccessibleTableCellColumnExtent");
        if (at != null) {
            int columnExtent = InvocationUtils.invokeAndWait(new Callable<Integer>() {
                                                                 @Override
                                                                 public Integer call() throws Exception {
                                                                     return at.getAccessibleColumnExtentAt(row, column);
                                                                 }
                                                             },
                    getContextFromAccessibleTable(at));
            debugString("[INFO]:   ##### getAccessibleTableCellColumnExtent="+columnExtent);
            return columnExtent;
        }
        debugString("[ERROR]: ##### getAccessibleTableCellColumnExtent FAILED");
        return -1;
    }

    /*
     * returns whether a cell is selected at a given row and column in an AccessibleTable
     */
    private boolean isAccessibleTableCellSelected(final AccessibleTable at, final int row,
                         final int column) {
        debugString("[INFO]: ##### isAccessibleTableCellSelected: ["+row+"]["+column+"]");
        if (at == null)
            return false;
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean isSelected = false;
                Accessible a = at.getAccessibleAt(row, column);
                if (a != null) {
                    AccessibleContext ac = a.getAccessibleContext();
                    if (ac == null)
                        return false;
                    AccessibleStateSet as = ac.getAccessibleStateSet();
                    if (as != null) {
                        isSelected = as.contains(AccessibleState.SELECTED);
                    }
                }
                return isSelected;
            }
        }, getContextFromAccessibleTable(at));
    }

    /*
     * returns an AccessibleTable that represents the row header in an
     * AccessibleTable
     */
    private AccessibleTable getAccessibleTableRowHeader(final AccessibleContext ac) {
        debugString("[INFO]: #####  getAccessibleTableRowHeader called");
        AccessibleTable at = InvocationUtils.invokeAndWait(new Callable<AccessibleTable>() {
            @Override
            public AccessibleTable call() throws Exception {
                if (ac != null) {
                    AccessibleTable at = ac.getAccessibleTable();
                    if (at != null) {
                        return at.getAccessibleRowHeader();
                    }
                }
                return null;
            }
        }, ac);
        if (at != null) {
            hashtab.put(at, ac);
        }
        return at;
    }

    /*
     * returns an AccessibleTable that represents the column header in an
     * AccessibleTable
     */
    private AccessibleTable getAccessibleTableColumnHeader(final AccessibleContext ac) {
    debugString("[INFO]: ##### getAccessibleTableColumnHeader");
        if (ac == null)
            return null;
        AccessibleTable at = InvocationUtils.invokeAndWait(new Callable<AccessibleTable>() {
            @Override
            public AccessibleTable call() throws Exception {
                // workaround for getAccessibleColumnHeader NPE
                // when the table header is null
                Accessible parent = ac.getAccessibleParent();
                if (parent != null) {
                    int indexInParent = ac.getAccessibleIndexInParent();
                    Accessible child =
                            parent.getAccessibleContext().getAccessibleChild(indexInParent);
                    if (child instanceof JTable) {
                        JTable table = (JTable) child;
                        if (table.getTableHeader() == null) {
                            return null;
                        }
                    }
                }
                AccessibleTable at = ac.getAccessibleTable();
                if (at != null) {
                    return at.getAccessibleColumnHeader();
                }
                return null;
            }
        }, ac);
        if (at != null) {
            hashtab.put(at, ac);
        }
        return at;
    }

    /*
     * returns the number of row headers in an AccessibleTable that represents
     * the row header in an AccessibleTable
     */
    private int getAccessibleTableRowHeaderRowCount(AccessibleContext ac) {

    debugString("[INFO]: #####  getAccessibleTableRowHeaderRowCount called");
        if (ac != null) {
            final AccessibleTable atRowHeader = getAccessibleTableRowHeader(ac);
            if (atRowHeader != null) {
                return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        if (atRowHeader != null) {
                            return atRowHeader.getAccessibleRowCount();
                        }
                        return -1;
                    }
                }, ac);
            }
        }
        return -1;
    }

    /*
     * returns the number of column headers in an AccessibleTable that represents
     * the row header in an AccessibleTable
     */
    private int getAccessibleTableRowHeaderColumnCount(AccessibleContext ac) {
        debugString("[INFO]: #####  getAccessibleTableRowHeaderColumnCount called");
        if (ac != null) {
            final AccessibleTable atRowHeader = getAccessibleTableRowHeader(ac);
            if (atRowHeader != null) {
                return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        if (atRowHeader != null) {
                            return atRowHeader.getAccessibleColumnCount();
                        }
                        return -1;
                    }
                }, ac);
            }
        }
        debugString("[ERROR]: ##### getAccessibleTableRowHeaderColumnCount FAILED");
        return -1;
    }

    /*
     * returns the number of row headers in an AccessibleTable that represents
     * the column header in an AccessibleTable
     */
    private int getAccessibleTableColumnHeaderRowCount(AccessibleContext ac) {

    debugString("[INFO]: ##### getAccessibleTableColumnHeaderRowCount");
        if (ac != null) {
            final AccessibleTable atColumnHeader = getAccessibleTableColumnHeader(ac);
            if (atColumnHeader != null) {
                return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        if (atColumnHeader != null) {
                            return atColumnHeader.getAccessibleRowCount();
                        }
                        return -1;
                    }
                }, ac);
            }
        }
        debugString("[ERROR]: ##### getAccessibleTableColumnHeaderRowCount FAILED");
        return -1;
    }

    /*
     * returns the number of column headers in an AccessibleTable that represents
     * the column header in an AccessibleTable
     */
    private int getAccessibleTableColumnHeaderColumnCount(AccessibleContext ac) {

    debugString("[ERROR]: #####  getAccessibleTableColumnHeaderColumnCount");
        if (ac != null) {
            final AccessibleTable atColumnHeader = getAccessibleTableColumnHeader(ac);
            if (atColumnHeader != null) {
                return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        if (atColumnHeader != null) {
                            return atColumnHeader.getAccessibleColumnCount();
                        }
                        return -1;
                    }
                }, ac);
            }
        }
        debugString("[ERROR]: ##### getAccessibleTableColumnHeaderColumnCount FAILED");
        return -1;
    }

    /*
     * returns the description of a row header in an AccessibleTable
     */
    private AccessibleContext getAccessibleTableRowDescription(final AccessibleTable table,
                                                              final int row) {
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                if (table != null) {
                    Accessible a = table.getAccessibleRowDescription(row);
                    if (a != null) {
                        return a.getAccessibleContext();
                    }
                }
                return null;
            }
        }, getContextFromAccessibleTable(table));
    }

    /*
     * returns the description of a column header in an AccessibleTable
     */
    private AccessibleContext getAccessibleTableColumnDescription(final AccessibleTable at,
                                                                 final int column) {
        if (at == null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                Accessible a = at.getAccessibleColumnDescription(column);
                if (a != null) {
                    return a.getAccessibleContext();
                }
                return null;
            }
        }, getContextFromAccessibleTable(at));
    }

    /*
     * returns the number of rows selected in an AccessibleTable
     */
    private int getAccessibleTableRowSelectionCount(final AccessibleTable at) {
        if (at != null) {
            return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int[] selections = at.getSelectedAccessibleRows();
                    if (selections != null)
                        return selections.length;
                    else
                        return -1;
                }
            }, getContextFromAccessibleTable(at));
        }
        return -1;
    }

    /*
     * returns the row number of the i-th selected row in an AccessibleTable
     */
    private int getAccessibleTableRowSelections(final AccessibleTable at, final int i) {
        if (at != null) {
            return InvocationUtils.invokeAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int[] selections = at.getSelectedAccessibleRows();
                    if (selections.length > i) {
                        return selections[i];
                    }
                    return -1;
                }
            }, getContextFromAccessibleTable(at));
        }
        return -1;
    }

    /*
     * returns whether a row is selected in an AccessibleTable
     */
    private boolean isAccessibleTableRowSelected(final AccessibleTable at,
                                                 final int row) {
        if (at == null)
            return false;
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return at.isAccessibleRowSelected(row);
            }
         }, getContextFromAccessibleTable(at));
    }

    /*
     * returns whether a column is selected in an AccessibleTable
     */
    private boolean isAccessibleTableColumnSelected(final AccessibleTable at,
                                                   final int column) {
        if (at == null)
            return false;
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return at.isAccessibleColumnSelected(column);
            }
         }, getContextFromAccessibleTable(at));
    }

    /*
     * returns the number of columns selected in an AccessibleTable
     */
    private int getAccessibleTableColumnSelectionCount(final AccessibleTable at) {
        if (at == null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int[] selections = at.getSelectedAccessibleColumns();
                if (selections != null)
                    return selections.length;
                else
                    return -1;
            }
        }, getContextFromAccessibleTable(at));
    }

    /*
     * returns the row number of the i-th selected row in an AccessibleTable
     */
    private int getAccessibleTableColumnSelections(final AccessibleTable at, final int i) {
        if (at == null)
            return -1;
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int[] selections = at.getSelectedAccessibleColumns();
                if (selections != null && selections.length > i) {
                    return selections[i];
                }
                return -1;
            }
        }, getContextFromAccessibleTable(at));
    }

    /* ===== AccessibleExtendedTable (since 1.4) ===== */

    /*
     * returns the row number for a cell at a given index in an AccessibleTable
     */
    private int getAccessibleTableRow(final AccessibleTable at, int index) {
        if (at == null)
            return -1;
        int colCount=InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return at.getAccessibleColumnCount();
            }
        }, getContextFromAccessibleTable(at));
        return index / colCount;
    }

    /*
     * returns the column number for a cell at a given index in an AccessibleTable
     */
    private int getAccessibleTableColumn(final AccessibleTable at, int index) {
        if (at == null)
            return -1;
        int colCount=InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return at.getAccessibleColumnCount();
            }
        }, getContextFromAccessibleTable(at));
        return index % colCount;
    }

    /*
     * returns the index for a cell at a given row and column in an
     * AccessibleTable
     */
    private int getAccessibleTableIndex(final AccessibleTable at, int row, int column) {
        if (at == null)
            return -1;
        int colCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return at.getAccessibleColumnCount();
            }
         }, getContextFromAccessibleTable(at));
        return row * colCount + column;
    }

    // ===== AccessibleRelationSet =====

    /*
     * returns the number of relations in the AccessibleContext's
     * AccessibleRelationSet
     */
    private int getAccessibleRelationCount(final AccessibleContext ac) {
        {
            if (ac != null) {
                AccessibleRelationSet ars = InvocationUtils.invokeAndWait(new Callable<AccessibleRelationSet>() {
                    @Override
                    public AccessibleRelationSet call() throws Exception {
                        return ac.getAccessibleRelationSet();
                    }
                }, ac);
                if (ars != null)
                    return ars.size();
            }
        }
        return 0;
    }

    /*
     * returns the ith relation key in the AccessibleContext's
     * AccessibleRelationSet
     */
    private String getAccessibleRelationKey(final AccessibleContext ac, final int i) {
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (ac != null) {
                    AccessibleRelationSet ars = ac.getAccessibleRelationSet();
                    if (ars != null) {
                        AccessibleRelation[] relations = ars.toArray();
                        if (relations != null && i >= 0 && i < relations.length) {
                            return relations[i].getKey();
                        }
                    }
                }
                return null;
            }
        }, ac);
    }

    /*
     * returns the number of targets in a relation in the AccessibleContext's
     * AccessibleRelationSet
     */
    private int getAccessibleRelationTargetCount(final AccessibleContext ac, final int i) {
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (ac != null) {
                    AccessibleRelationSet ars = ac.getAccessibleRelationSet();
                    if (ars != null) {
                        AccessibleRelation[] relations = ars.toArray();
                        if (relations != null && i >= 0 && i < relations.length) {
                            Object[] targets = relations[i].getTarget();
                            return targets.length;
                        }
                    }
                }
                return -1;
            }
        }, ac);
    }

    /*
     * returns the jth target in the ith relation in the AccessibleContext's
     * AccessibleRelationSet
     */
    private AccessibleContext getAccessibleRelationTarget(final AccessibleContext ac,
                                                         final int i, final int j) {
        debugString("[INFO]: ***** getAccessibleRelationTarget");
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                if (ac != null) {
                    AccessibleRelationSet ars = ac.getAccessibleRelationSet();
                    if (ars != null) {
                        AccessibleRelation[] relations = ars.toArray();
                        if (relations != null && i >= 0 && i < relations.length) {
                            Object[] targets = relations[i].getTarget();
                            if (targets != null && j >= 0 & j < targets.length) {
                                Object o = targets[j];
                                if (o instanceof Accessible) {
                                    return ((Accessible) o).getAccessibleContext();
                                }
                            }
                        }
                    }
                }
                return null;
            }
        }, ac);
    }

    // ========= AccessibleHypertext =========

    private Map<AccessibleHypertext, AccessibleContext> hyperTextContextMap = new WeakHashMap<>();
    private Map<AccessibleHyperlink, AccessibleContext> hyperLinkContextMap = new WeakHashMap<>();

    /*
     * Returns the AccessibleHypertext
     */
    private AccessibleHypertext getAccessibleHypertext(final AccessibleContext ac) {
        debugString("[INFO]: getAccessibleHyperlink");
        if (ac==null)
            return null;
        AccessibleHypertext hypertext = InvocationUtils.invokeAndWait(new Callable<AccessibleHypertext>() {
            @Override
            public AccessibleHypertext call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (!(at instanceof AccessibleHypertext)) {
                    return null;
                }
                return ((AccessibleHypertext) at);
            }
        }, ac);
        hyperTextContextMap.put(hypertext, ac);
        return hypertext;
    }

    /*
     * Returns the number of AccessibleHyperlinks
     */
    private int getAccessibleHyperlinkCount(AccessibleContext ac) {
        debugString("[INFO]: getAccessibleHyperlinkCount");
        if (ac == null) {
            return 0;
        }
        final AccessibleHypertext hypertext = getAccessibleHypertext(ac);
        if (hypertext == null) {
            return 0;
        }
        //return hypertext.getLinkCount();
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return hypertext.getLinkCount();
            }
        }, ac);
    }

    /*
     * Returns the hyperlink at the specified index
     */
    private AccessibleHyperlink getAccessibleHyperlink(final AccessibleHypertext hypertext, final int i) {
        debugString("[INFO]: getAccessibleHyperlink");
        if (hypertext == null) {
            return null;
        }
        AccessibleContext ac = hyperTextContextMap.get(hypertext);
        if ( i < 0 || i >=
             InvocationUtils.invokeAndWait(new Callable<Integer>() {
                 @Override
                 public Integer call() throws Exception {
                     return hypertext.getLinkCount();
                 }
             }, ac) ) {
            return null;
        }
        AccessibleHyperlink acLink = InvocationUtils.invokeAndWait(new Callable<AccessibleHyperlink>() {
            @Override
            public AccessibleHyperlink call() throws Exception {
                AccessibleHyperlink link = hypertext.getLink(i);
                if (link == null || (!link.isValid())) {
                    return null;
                }
                return link;
            }
        }, ac);
        hyperLinkContextMap.put(acLink, ac);
        return acLink;
    }

    /*
     * Returns the hyperlink object description
     */
    private String getAccessibleHyperlinkText(final AccessibleHyperlink link) {
        debugString("[INFO]: getAccessibleHyperlinkText");
        if (link == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Object o = link.getAccessibleActionDescription(0);
                if (o != null) {
                    return o.toString();
                }
                return null;
            }
        }, hyperLinkContextMap.get(link));
    }

    /*
     * Returns the hyperlink URL
     */
    private String getAccessibleHyperlinkURL(final AccessibleHyperlink link) {
        debugString("[INFO]: getAccessibleHyperlinkURL");
        if (link == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Object o = link.getAccessibleActionObject(0);
                if (o != null) {
                    return o.toString();
                } else {
                    return null;
                }
            }
        }, hyperLinkContextMap.get(link));
    }

    /*
     * Returns the start index of the hyperlink text
     */
    private int getAccessibleHyperlinkStartIndex(final AccessibleHyperlink link) {
        debugString("[INFO]: getAccessibleHyperlinkStartIndex");
        if (link == null) {
            return -1;
        }
        return  InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return link.getStartIndex();
            }
        }, hyperLinkContextMap.get(link));
    }

    /*
     * Returns the end index of the hyperlink text
     */
    private int getAccessibleHyperlinkEndIndex(final AccessibleHyperlink link) {
        debugString("[INFO]: getAccessibleHyperlinkEndIndex");
        if (link == null) {
            return -1;
        }
        return  InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return link.getEndIndex();
            }
        }, hyperLinkContextMap.get(link));
    }

    /*
     * Returns the index into an array of hyperlinks that
     * is associated with this character index, or -1 if there
     * is no hyperlink associated with this index.
     */
    private int getAccessibleHypertextLinkIndex(final AccessibleHypertext hypertext, final int charIndex) {
        debugString("[INFO]: getAccessibleHypertextLinkIndex: charIndex = "+charIndex);
        if (hypertext == null) {
            return -1;
        }
        int linkIndex = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return hypertext.getLinkIndex(charIndex);
            }
        }, hyperTextContextMap.get(hypertext));
        debugString("[INFO]: getAccessibleHypertextLinkIndex returning "+linkIndex);
        return linkIndex;
    }

    /*
     * Actives the hyperlink
     */
    private boolean activateAccessibleHyperlink(final AccessibleContext ac,
                                                final AccessibleHyperlink link) {
        //debugString("activateAccessibleHyperlink: link = "+link.getClass());
        if (link == null) {
            return false;
        }
        boolean retval = InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return link.doAccessibleAction(0);
            }
        }, ac);
        debugString("[INFO]: activateAccessibleHyperlink: returning = "+retval);
        return retval;
    }


    // ============ AccessibleKeyBinding =============

    /*
     * returns the component mnemonic
     */
    private KeyStroke getMnemonic(final AccessibleContext ac) {
        if (ac == null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<KeyStroke>() {
            @Override
            public KeyStroke call() throws Exception {
                AccessibleComponent comp = ac.getAccessibleComponent();
                if (!(comp instanceof AccessibleExtendedComponent)) {
                    return null;
                }
                AccessibleExtendedComponent aec = (AccessibleExtendedComponent) comp;
                if (aec != null) {
                    AccessibleKeyBinding akb = aec.getAccessibleKeyBinding();
                    if (akb != null) {
                        Object o = akb.getAccessibleKeyBinding(0);
                        if (o instanceof KeyStroke) {
                            return (KeyStroke) o;
                        }
                    }
                }
                return null;
            }
        }, ac);
    }

    /*
     * Returns the JMenuItem accelerator. Similar implementation is used on
     * macOS, see CAccessibility.getAcceleratorText(AccessibleContext).
     */
    private KeyStroke getAccelerator(final AccessibleContext ac) {
        // workaround for getAccessibleKeyBinding not returning the
        // JMenuItem accelerator
        if (ac == null)
            return null;
        return InvocationUtils.invokeAndWait(new Callable<KeyStroke>() {
            @Override
            public KeyStroke call() throws Exception {
                Accessible parent = ac.getAccessibleParent();
                if (parent instanceof Accessible) {
                    int indexInParent = ac.getAccessibleIndexInParent();
                    Accessible child =
                            parent.getAccessibleContext().getAccessibleChild(indexInParent);
                    if (child instanceof JMenuItem) {
                        JMenuItem menuItem = (JMenuItem) child;
                        if (menuItem == null)
                            return null;
                        KeyStroke keyStroke = menuItem.getAccelerator();
                        return keyStroke;
                    }
                }
                return null;
            }
        }, ac);
    }

    /*
     * returns 1-24 to indicate which F key is being used for a shortcut or 0 otherwise
     */
    private int fKeyNumber(KeyStroke keyStroke) {
        if (keyStroke == null)
            return 0;
        int fKey = 0;
        String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());
        if (keyText != null && (keyText.length() == 2 || keyText.length() == 3)) {
            String prefix = keyText.substring(0, 1);
            if (prefix.equals("F")) {
                try {
                    int suffix = Integer.parseInt(keyText.substring(1));
                    if (suffix >= 1 && suffix <= 24) {
                        fKey = suffix;
                    }
                } catch (Exception e) { // ignore NumberFormatException
                }
            }
        }
        return fKey;
    }

    /*
     * returns one of several important control characters or 0 otherwise
     */
    private int controlCode(KeyStroke keyStroke) {
        if (keyStroke == null)
            return 0;
        int code = keyStroke.getKeyCode();
        switch (code) {
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_END:
            case KeyEvent.VK_HOME:
            case KeyEvent.VK_INSERT:
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
                break;
            default:
                code = 0;
                break;
        }
        return code;
    }

    /*
     * returns the KeyStoke character
     */
    private char getKeyChar(KeyStroke keyStroke) {
        // If the shortcut is an FKey return 1-24
        if (keyStroke == null)
            return 0;
        int fKey = fKeyNumber(keyStroke);
        if (fKey != 0) {
            // return 0x00000001 through 0x00000018
            debugString("[INFO]:   Shortcut is: F" + fKey);
            return (char)fKey;
        }
        // If the accelerator is a control character, return it
        int keyCode = controlCode(keyStroke);
        if (keyCode != 0) {
            debugString("[INFO]:   Shortcut is control character: " + Integer.toHexString(keyCode));
            return (char)keyCode;
        }
        String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());
        debugString("[INFO]:   Shortcut is: " + keyText);
        if (keyText != null || keyText.length() > 0) {
            CharSequence seq = keyText.subSequence(0, 1);
            if (seq != null || seq.length() > 0) {
                return seq.charAt(0);
            }
        }
        return 0;
    }

    /*
     * returns the KeyStroke modifiers as an int
     */
    private int getModifiers(KeyStroke keyStroke) {
        if (keyStroke == null)
            return 0;
        debugString("[INFO]: In AccessBridge.getModifiers");
        // modifiers is a bit strip where bits 0-7 indicate a traditional modifier
        // such as Ctrl/Alt/Shift, bit 8 indicates an F key shortcut, and bit 9 indicates
        // a control code shortcut such as the delete key.

        int modifiers = 0;
        // Is the shortcut an FKey?
        if (fKeyNumber(keyStroke) != 0) {
            modifiers |= 1 << 8;
        }
        // Is the shortcut a control code?
        if (controlCode(keyStroke) != 0) {
            modifiers |= 1 << 9;
        }
        // The following is needed in order to handle translated modifiers.
        // getKeyModifiersText doesn't work because for example in German Strg is
        // returned for Ctrl.

        // There can be more than one modifier, e.g. if the modifier is ctrl + shift + B
        // the toString text is "shift ctrl pressed B". Need to parse through that.
        StringTokenizer st = new StringTokenizer(keyStroke.toString());
        while (st.hasMoreTokens()) {
            String text = st.nextToken();
            // Meta+Ctrl+Alt+Shift
            // 0-3 are shift, ctrl, meta, alt
            // 4-7 are for Solaris workstations (though not being used)
            if (text.startsWith("met")) {
                debugString("[INFO]:   found meta");
                modifiers |= ActionEvent.META_MASK;
            }
            if (text.startsWith("ctr")) {
                debugString("[INFO]:   found ctrl");
                modifiers |= ActionEvent.CTRL_MASK;
            }
            if (text.startsWith("alt")) {
                debugString("[INFO]:   found alt");
                modifiers |= ActionEvent.ALT_MASK;
            }
            if (text.startsWith("shi")) {
                debugString("   found shift");
                modifiers |= ActionEvent.SHIFT_MASK;
            }
        }
        debugString("[INFO]:   returning modifiers: 0x" + Integer.toHexString(modifiers));
        return modifiers;
    }

    /*
     * returns the number of key bindings associated with this context
     */
    private int getAccessibleKeyBindingsCount(AccessibleContext ac) {
        if (ac == null)
            return 0;
        int count = 0;

        if (getMnemonic(ac) != null) {
            count++;
        }
        if (getAccelerator(ac) != null) {
            count++;
        }
        return count;
    }

    /*
     * returns the key binding character at the specified index
     */
    private char getAccessibleKeyBindingChar(AccessibleContext ac, int index) {
        if (ac == null)
            return 0;
        if((index == 0) && getMnemonic(ac)==null) {// special case when there is no mnemonic
            KeyStroke keyStroke = getAccelerator(ac);
            if (keyStroke != null) {
                return getKeyChar(keyStroke);
            }
        }
        if (index == 0) {   // mnemonic
            KeyStroke keyStroke = getMnemonic(ac);
            if (keyStroke != null) {
                return getKeyChar(keyStroke);
            }
        } else if (index == 1) { // accelerator
            KeyStroke keyStroke = getAccelerator(ac);
            if (keyStroke != null) {
                return getKeyChar(keyStroke);
            }
        }
        return 0;
    }

    /*
     * returns the key binding modifiers at the specified index
     */
    private int getAccessibleKeyBindingModifiers(AccessibleContext ac, int index) {
        if (ac == null)
            return 0;
        if((index == 0) && getMnemonic(ac)==null) {// special case when there is no mnemonic
            KeyStroke keyStroke = getAccelerator(ac);
            if (keyStroke != null) {
                return getModifiers(keyStroke);
            }
        }
        if (index == 0) {   // mnemonic
            KeyStroke keyStroke = getMnemonic(ac);
            if (keyStroke != null) {
                return getModifiers(keyStroke);
            }
        } else if (index == 1) { // accelerator
            KeyStroke keyStroke = getAccelerator(ac);
            if (keyStroke != null) {
                return getModifiers(keyStroke);
            }
        }
        return 0;
    }

    // ========== AccessibleIcon ============

    /*
     * return the number of icons associated with this context
     */
    private int getAccessibleIconsCount(final AccessibleContext ac) {
        debugString("[INFO]: getAccessibleIconsCount");
        if (ac == null) {
            return 0;
        }
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleIcon[] ai = ac.getAccessibleIcon();
                if (ai == null) {
                    return 0;
                }
                return ai.length;
            }
        }, ac);
    }

    /*
     * return icon description at the specified index
     */
    private String getAccessibleIconDescription(final AccessibleContext ac, final int index) {
        debugString("[INFO]: getAccessibleIconDescription: index = "+index);
        if (ac == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                AccessibleIcon[] ai = ac.getAccessibleIcon();
                if (ai == null || index < 0 || index >= ai.length) {
                    return null;
                }
                return ai[index].getAccessibleIconDescription();
            }
        }, ac);
    }

    /*
     * return icon height at the specified index
     */
    private int getAccessibleIconHeight(final AccessibleContext ac, final int index) {
        debugString("[INFO]: getAccessibleIconHeight: index = "+index);
        if (ac == null) {
            return 0;
        }
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleIcon[] ai = ac.getAccessibleIcon();
                if (ai == null || index < 0 || index >= ai.length) {
                    return 0;
                }
                return ai[index].getAccessibleIconHeight();
            }
        }, ac);
    }

    /*
     * return icon width at the specified index
     */
    private int getAccessibleIconWidth(final AccessibleContext ac, final int index) {
        debugString("[INFO]: getAccessibleIconWidth: index = "+index);
        if (ac == null) {
            return 0;
        }
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleIcon[] ai = ac.getAccessibleIcon();
                if (ai == null || index < 0 || index >= ai.length) {
                    return 0;
                }
                return ai[index].getAccessibleIconWidth();
            }
        }, ac);
    }

    // ========= AccessibleAction ===========

    /*
     * return the number of icons associated with this context
     */
    private int getAccessibleActionsCount(final AccessibleContext ac) {
        debugString("[INFO]: getAccessibleActionsCount");
        if (ac == null) {
            return 0;
        }
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa == null)
                    return 0;
                return aa.getAccessibleActionCount();
            }
        }, ac);
    }

    /*
     * return icon description at the specified index
     */
    private String getAccessibleActionName(final AccessibleContext ac, final int index) {
        debugString("[INFO]: getAccessibleActionName: index = "+index);
        if (ac == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa == null) {
                    return null;
                }
                return aa.getAccessibleActionDescription(index);
            }
        }, ac);
    }
    /*
     * return icon description at the specified index
     */
    private boolean doAccessibleActions(final AccessibleContext ac, final String name) {
        debugString("[INFO]: doAccessibleActions: action name = "+name);
        if (ac == null || name == null) {
            return false;
        }
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa == null) {
                    return false;
                }
                int index = -1;
                int numActions = aa.getAccessibleActionCount();
                for (int i = 0; i < numActions; i++) {
                    String actionName = aa.getAccessibleActionDescription(i);
                    if (name.equals(actionName)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    return false;
                }
                boolean retval = aa.doAccessibleAction(index);
                return retval;
            }
        }, ac);
    }

    /* ===== AT utility methods ===== */

    /**
     * Sets the contents of an AccessibleContext that
     * implements AccessibleEditableText with the
     * specified text string.
     * Returns whether successful.
     */
    private boolean setTextContents(final AccessibleContext ac, final String text) {
        debugString("[INFO]: setTextContents: ac = "+ac+"; text = "+text);

        if (! (ac instanceof AccessibleEditableText)) {
            debugString("[WARN]:   ac not instanceof AccessibleEditableText: "+ac);
            return false;
        }
        if (text == null) {
            debugString("[WARN]:   text is null");
            return false;
        }

        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // check whether the text field is editable
                AccessibleStateSet ass = ac.getAccessibleStateSet();
                if (!ass.contains(AccessibleState.ENABLED)) {
                    return false;
                }
                ((AccessibleEditableText) ac).setTextContents(text);
                return true;
            }
        }, ac);
    }

    /**
     * Returns the Accessible Context of an Internal Frame object that is
     * the ancestor of a given object.  If the object is an Internal Frame
     * object or an Internal Frame ancestor object was found, returns the
     * object's AccessibleContext.
     * If there is no ancestor object that has an Accessible Role of
     * Internal Frame, returns (AccessibleContext)0.
     */
    private AccessibleContext getInternalFrame (AccessibleContext ac) {
        return getParentWithRole(ac, AccessibleRole.INTERNAL_FRAME.toString());
    }

    /**
     * Returns the Accessible Context for the top level object in
     * a Java Window.  This is same Accessible Context that is obtained
     * from GetAccessibleContextFromHWND for that window.  Returns
     * (AccessibleContext)0 on error.
     */
    private AccessibleContext getTopLevelObject (final AccessibleContext ac) {
        debugString("[INFO]: getTopLevelObject; ac = "+ac);
        if (ac == null) {
            return null;
        }
        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                if (ac.getAccessibleRole() == AccessibleRole.DIALOG) {
                    // return the dialog, not the parent window
                    return ac;
                }

                Accessible parent = ac.getAccessibleParent();
                if (parent == null) {
                    return ac;
                }
                Accessible tmp = parent;
                while (tmp != null && tmp.getAccessibleContext() != null) {
                    AccessibleContext ac2 = tmp.getAccessibleContext();
                    if (ac2 != null && ac2.getAccessibleRole() == AccessibleRole.DIALOG) {
                        // return the dialog, not the parent window
                        return ac2;
                    }
                    parent = tmp;
                    tmp = parent.getAccessibleContext().getAccessibleParent();
                }
                return parent.getAccessibleContext();
            }
        }, ac);
    }

    /**
     * Returns the parent AccessibleContext that has the specified AccessibleRole.
     * Returns null on error or if the AccessibleContext does not exist.
     */
    private AccessibleContext getParentWithRole (final AccessibleContext ac,
                                                 final String roleName) {
        debugString("[INFO]: getParentWithRole; ac = "+ac + "\n role = "+roleName);
        if (ac == null || roleName == null) {
            return null;
        }

        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                AccessibleRole role = AccessBridge.this.accessibleRoleMap.get(roleName);
                if (role == null) {
                    return ac;
                }

                Accessible parent = ac.getAccessibleParent();
                if (parent == null && ac.getAccessibleRole() == role) {
                    return ac;
                }

                Accessible tmp = parent;
                AccessibleContext tmp_ac = null;

                while (tmp != null && (tmp_ac = tmp.getAccessibleContext()) != null) {
                    AccessibleRole ar = tmp_ac.getAccessibleRole();
                    if (ar == role) {
                        // found
                        return tmp_ac;
                    }
                    parent = tmp;
                    tmp = parent.getAccessibleContext().getAccessibleParent();
                }
                // not found
                return null;
            }
        }, ac);
    }

    /**
     * Returns the parent AccessibleContext that has the specified AccessibleRole.
     * Otherwise, returns the top level object for the Java Window.
     * Returns (AccessibleContext)0 on error.
     */
    private AccessibleContext getParentWithRoleElseRoot (AccessibleContext ac,
                                                         String roleName) {
        AccessibleContext retval = getParentWithRole(ac, roleName);
        if (retval == null) {
            retval = getTopLevelObject(ac);
        }
        return retval;
    }

    /**
     * Returns how deep in the object hierarchy a given object is.
     * The top most object in the object hierarchy has an object depth of 0.
     * Returns -1 on error.
     */
    private int getObjectDepth(final AccessibleContext ac) {
        debugString("[INFO]: getObjectDepth: ac = "+ac);

        if (ac == null) {
            return -1;
        }
        return InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int count = 0;
                Accessible parent = ac.getAccessibleParent();
                if (parent == null) {
                    return count;
                }
                Accessible tmp = parent;
                while (tmp != null && tmp.getAccessibleContext() != null) {
                    parent = tmp;
                    tmp = parent.getAccessibleContext().getAccessibleParent();
                    count++;
                }
                return count;
            }
        }, ac);
    }

    /**
     * Returns the Accessible Context of the current ActiveDescendent of an object.
     * Returns (AccessibleContext)0 on error.
     */
    private AccessibleContext getActiveDescendent (final AccessibleContext ac) {
        debugString("[INFO]: getActiveDescendent: ac = "+ac);
        if (ac == null) {
            return null;
        }
        // workaround for JTree bug where the only possible active
        // descendent is the JTree root
        final Accessible parent = InvocationUtils.invokeAndWait(new Callable<Accessible>() {
            @Override
            public Accessible call() throws Exception {
                return ac.getAccessibleParent();
            }
        }, ac);

        if (parent != null) {
            Accessible child = InvocationUtils.invokeAndWait(new Callable<Accessible>() {
                @Override
                public Accessible call() throws Exception {
                    int indexInParent = ac.getAccessibleIndexInParent();
                    return parent.getAccessibleContext().getAccessibleChild(indexInParent);
                }
            }, ac);

            if (child instanceof JTree) {
                // return the selected node
                final JTree tree = (JTree)child;
                return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        return new AccessibleJTreeNode(tree,
                                tree.getSelectionPath(),
                                null);
                    }
                }, child);
            }
        }

        return InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
            @Override
            public AccessibleContext call() throws Exception {
                AccessibleSelection as = ac.getAccessibleSelection();
                if (as == null) {
                    return null;
                }
                // assume single selection
                if (as.getAccessibleSelectionCount() != 1) {
                    return null;
                }
                Accessible a = as.getAccessibleSelection(0);
                if (a == null) {
                    return null;
                }
                return a.getAccessibleContext();
            }
        }, ac);
    }


    /**
     * Additional methods for Teton
     */

    /**
     * Gets the AccessibleName for a component based upon the JAWS algorithm.
     * Returns whether successful.
     *
     * Bug ID 4916682 - Implement JAWS AccessibleName policy
     */
    private String getJAWSAccessibleName(final AccessibleContext ac) {
        debugString("[INFO]:  getJAWSAccessibleName");
        if (ac == null) {
            return null;
        }
        // placeholder
        return InvocationUtils.invokeAndWait(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return ac.getAccessibleName();
            }
        }, ac);
    }

    /**
     * Request focus for a component. Returns whether successful;
     *
     * Bug ID 4944757 - requestFocus method needed
     */
    private boolean requestFocus(final AccessibleContext ac) {
        debugString("[INFO]:  requestFocus");
        if (ac == null) {
            return false;
        }
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AccessibleComponent acomp = ac.getAccessibleComponent();
                if (acomp == null) {
                    return false;
                }
                acomp.requestFocus();
                return ac.getAccessibleStateSet().contains(AccessibleState.FOCUSED);
            }
        }, ac);
    }

    /**
     * Selects text between two indices.  Selection includes the
     * text at the start index and the text at the end index. Returns
     * whether successful;
     *
     * Bug ID 4944758 - selectTextRange method needed
     */
    private boolean selectTextRange(final AccessibleContext ac, final int startIndex, final int endIndex) {
        debugString("[INFO]:  selectTextRange: start = "+startIndex+"; end = "+endIndex);
        if (ac == null) {
            return false;
        }
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (!(at instanceof AccessibleEditableText)) {
                    return false;
                }
                ((AccessibleEditableText) at).selectText(startIndex, endIndex);

                boolean result = at.getSelectionStart() == startIndex &&
                        at.getSelectionEnd() == endIndex;
                return result;
            }
        }, ac);
    }

    /**
     * Set the caret to a text position. Returns whether successful;
     *
     * Bug ID 4944770 - setCaretPosition method needed
     */
    private boolean setCaretPosition(final AccessibleContext ac, final int position) {
        debugString("[INFO]: setCaretPosition: position = "+position);
        if (ac == null) {
            return false;
        }
        return InvocationUtils.invokeAndWait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                AccessibleText at = ac.getAccessibleText();
                if (!(at instanceof AccessibleEditableText)) {
                    return false;
                }
                ((AccessibleEditableText) at).selectText(position, position);
                return at.getCaretPosition() == position;
            }
        }, ac);
    }

    /**
     * Gets the number of visible children of an AccessibleContext.
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    private int _visibleChildrenCount;
    private AccessibleContext _visibleChild;
    private int _currentVisibleIndex;
    private boolean _foundVisibleChild;

    private int getVisibleChildrenCount(AccessibleContext ac) {
        debugString("[INFO]: getVisibleChildrenCount");
        if (ac == null) {
            return -1;
        }
        _visibleChildrenCount = 0;
        _getVisibleChildrenCount(ac);
        debugString("[INFO]:   _visibleChildrenCount = "+_visibleChildrenCount);
        return _visibleChildrenCount;
    }

    /*
     * Recursively descends AccessibleContext and gets the number
     * of visible children
     */
    private void _getVisibleChildrenCount(final AccessibleContext ac) {
        if (ac == null)
            return;
        if(ac instanceof AccessibleExtendedTable) {
            _getVisibleChildrenCount((AccessibleExtendedTable)ac);
            return;
        }
        int numChildren = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ac.getAccessibleChildrenCount();
            }
        }, ac);
        for (int i = 0; i < numChildren; i++) {
            final int idx = i;
            final AccessibleContext ac2 = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                @Override
                public AccessibleContext call() throws Exception {
                    Accessible a = ac.getAccessibleChild(idx);
                    if (a != null)
                        return a.getAccessibleContext();
                    else
                        return null;
                }
            }, ac);
            if ( ac2 == null ||
                 (!InvocationUtils.invokeAndWait(new Callable<Boolean>() {
                     @Override
                     public Boolean call() throws Exception {
                         return ac2.getAccessibleStateSet().contains(AccessibleState.SHOWING);
                     }
                 }, ac))
               ) {
                continue;
            }
            _visibleChildrenCount++;

            if (InvocationUtils.invokeAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ac2.getAccessibleChildrenCount();
                }
            }, ac) > 0 ) {
                _getVisibleChildrenCount(ac2);
            }
        }
    }

    /*
    * Recursively descends AccessibleContext and gets the number
    * of visible children. Stops search if get to invisible part of table.
    */
    private void _getVisibleChildrenCount(final AccessibleExtendedTable acTable) {
        if (acTable == null)
            return;
        int lastVisibleRow = -1;
        int lastVisibleColumn = -1;
        boolean foundVisible = false;
        int rowCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return acTable.getAccessibleRowCount();
            }
        }, acTable);
        int columnCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return acTable.getAccessibleColumnCount();
            }
        }, acTable);
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            for (int columnIdx = 0; columnIdx < columnCount; columnIdx++) {
                if (lastVisibleRow != -1 && rowIdx > lastVisibleRow) {
                    continue;
                }
                if (lastVisibleColumn != -1 && columnIdx > lastVisibleColumn) {
                    continue;
                }
                int finalRowIdx = rowIdx;
                int finalColumnIdx = columnIdx;
                final AccessibleContext ac2 = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        Accessible a = acTable.getAccessibleAt(finalRowIdx, finalColumnIdx);
                        if (a == null)
                            return null;
                        else
                            return a.getAccessibleContext();
                    }
                }, acTable);
                if (ac2 == null ||
                        (!InvocationUtils.invokeAndWait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ac2.getAccessibleStateSet().contains(AccessibleState.SHOWING);
                            }
                        }, acTable))
                        ) {
                    if (foundVisible) {
                        if (columnIdx != 0 && lastVisibleColumn == -1) {
                            //the same row, so we found the last visible column
                            lastVisibleColumn = columnIdx - 1;
                        } else if (columnIdx == 0 && lastVisibleRow == -1) {
                            lastVisibleRow = rowIdx - 1;
                        }
                    }
                    continue;
                }

                foundVisible = true;

                _visibleChildrenCount++;

                if (InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return ac2.getAccessibleChildrenCount();
                    }
                }, acTable) > 0) {
                    _getVisibleChildrenCount(ac2);
                }
            }
        }
    }

    /**
     * Gets the visible child of an AccessibleContext at the
     * specified index
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    private AccessibleContext getVisibleChild(AccessibleContext ac, int index) {
        debugString("[INFO]: getVisibleChild: index = "+index);
        if (ac == null) {
            return null;
        }
        _visibleChild = null;
        _currentVisibleIndex = 0;
        _foundVisibleChild = false;
        _getVisibleChild(ac, index);

        if (_visibleChild != null) {
            debugString( "[INFO]:     getVisibleChild: found child = " +
                         InvocationUtils.invokeAndWait(new Callable<String>() {
                             @Override
                             public String call() throws Exception {
                                 return AccessBridge.this._visibleChild.getAccessibleName();
                             }
                         }, ac) );
        }
        return _visibleChild;
    }

    /*
     * Recursively searchs AccessibleContext and finds the visible component
     * at the specified index
     */
    private void _getVisibleChild(final AccessibleContext ac, final int index) {
        if (_visibleChild != null) {
            return;
        }
        if(ac instanceof AccessibleExtendedTable) {
            _getVisibleChild((AccessibleExtendedTable)ac, index);
            return;
        }
        int numChildren = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ac.getAccessibleChildrenCount();
            }
        }, ac);
        for (int i = 0; i < numChildren; i++) {
            final int idx=i;
            final AccessibleContext ac2 = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                @Override
                public AccessibleContext call() throws Exception {
                    Accessible a = ac.getAccessibleChild(idx);
                    if (a == null)
                        return null;
                    else
                        return a.getAccessibleContext();
                }
            }, ac);
            if (ac2 == null ||
            (!InvocationUtils.invokeAndWait(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return ac2.getAccessibleStateSet().contains(AccessibleState.SHOWING);
                }
            }, ac))) {
                continue;
            }
            if (!_foundVisibleChild && _currentVisibleIndex == index) {
            _visibleChild = ac2;
            _foundVisibleChild = true;
            return;
            }
            _currentVisibleIndex++;

            if ( InvocationUtils.invokeAndWait(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return ac2.getAccessibleChildrenCount();
                }
            }, ac) > 0 ) {
                _getVisibleChild(ac2, index);
            }
        }
    }

    private void _getVisibleChild(final AccessibleExtendedTable acTable, final int index) {
        if (_visibleChild != null) {
            return;
        }
        int lastVisibleRow = -1;
        int lastVisibleColumn = -1;
        boolean foundVisible = false;
        int rowCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return acTable.getAccessibleRowCount();
            }
        }, acTable);
        int columnCount = InvocationUtils.invokeAndWait(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return acTable.getAccessibleColumnCount();
            }
        }, acTable);
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            for (int columnIdx = 0; columnIdx < columnCount; columnIdx++) {
                if (lastVisibleRow != -1 && rowIdx > lastVisibleRow) {
                    continue;
                }
                if (lastVisibleColumn != -1 && columnIdx > lastVisibleColumn) {
                    continue;
                }
                int finalRowIdx = rowIdx;
                int finalColumnIdx = columnIdx;
                final AccessibleContext ac2 = InvocationUtils.invokeAndWait(new Callable<AccessibleContext>() {
                    @Override
                    public AccessibleContext call() throws Exception {
                        Accessible a = acTable.getAccessibleAt(finalRowIdx, finalColumnIdx);
                        if (a == null)
                            return null;
                        else
                            return a.getAccessibleContext();
                    }
                }, acTable);
                if (ac2 == null ||
                        (!InvocationUtils.invokeAndWait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ac2.getAccessibleStateSet().contains(AccessibleState.SHOWING);
                            }
                        }, acTable))) {
                    if (foundVisible) {
                        if (columnIdx != 0 && lastVisibleColumn == -1) {
                            //the same row, so we found the last visible column
                            lastVisibleColumn = columnIdx - 1;
                        } else if (columnIdx == 0 && lastVisibleRow == -1) {
                            lastVisibleRow = rowIdx - 1;
                        }
                    }
                    continue;
                }
                foundVisible = true;

                if (!_foundVisibleChild && _currentVisibleIndex == index) {
                    _visibleChild = ac2;
                    _foundVisibleChild = true;
                    return;
                }
                _currentVisibleIndex++;

                if (InvocationUtils.invokeAndWait(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return ac2.getAccessibleChildrenCount();
                    }
                }, acTable) > 0) {
                    _getVisibleChild(ac2, index);
                }
            }
        }
    }

    /* ===== Java object memory management code ===== */

    /**
     * Class to track object references to ensure the
     * Java VM doesn't garbage collect them
     */
    private class ObjectReferences {

        private class Reference {
            private int value;

            Reference(int i) {
                value = i;
            }

            public String toString() {
                return ("refCount: " + value);
            }
        }

        /**
        * table object references, to keep 'em from being garbage collected
        */
        private ConcurrentHashMap<Object,Reference> refs;

        /**
        * Constructor
        */
        ObjectReferences() {
            refs = new ConcurrentHashMap<>(4);
        }

        /**
        * Debugging: dump the contents of ObjectReferences' refs Hashtable
        */
        String dump() {
            return refs.toString();
        }

        /**
        * Increment ref count; set to 1 if we have no references for it
        */
        void increment(Object o) {
            if (o == null){
                debugString("[WARN]: ObjectReferences::increment - Passed in object is null");
                return;
            }

            if (refs.containsKey(o)) {
                (refs.get(o)).value++;
            } else {
                refs.put(o, new Reference(1));
            }
        }

        /**
        * Decrement ref count; remove if count drops to 0
        */
        void decrement(Object o) {
            Reference aRef = refs.get(o);
            if (aRef != null) {
                aRef.value--;
                if (aRef.value == 0) {
                    refs.remove(o);
                } else if (aRef.value < 0) {
                    debugString("[ERROR]: decrementing reference count below 0");
                }
            } else {
                debugString("[ERROR]: object to decrement not in ObjectReferences table");
            }
        }

    }

    /* ===== event handling code ===== */

   /**
     * native method for handling property change events
     */
    private native void propertyCaretChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        int oldValue, int newValue);
    private native void propertyDescriptionChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        String oldValue, String newValue);
    private native void propertyNameChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        String oldValue, String newValue);
    private native void propertySelectionChange(PropertyChangeEvent e,
                        AccessibleContext src);
    private native void propertyStateChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        String oldValue, String newValue);
    private native void propertyTextChange(PropertyChangeEvent e,
                        AccessibleContext src);
    private native void propertyValueChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        String oldValue, String newValue);
    private native void propertyVisibleDataChange(PropertyChangeEvent e,
                        AccessibleContext src);
    private native void propertyChildChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        AccessibleContext oldValue,
                        AccessibleContext newValue);
    private native void propertyActiveDescendentChange(PropertyChangeEvent e,
                        AccessibleContext src,
                        AccessibleContext oldValue,
                        AccessibleContext newValue);

    private native void javaShutdown();

    /**
     * native methods for handling focus events
     */
    private native void focusGained(FocusEvent e, AccessibleContext src);
    private native void focusLost(FocusEvent e, AccessibleContext src);

    /**
     * native method for handling caret events
     */
    private native void caretUpdate(CaretEvent e, AccessibleContext src);

    /**
     * native methods for handling mouse events
     */
    private native void mouseClicked(MouseEvent e, AccessibleContext src);
    private native void mouseEntered(MouseEvent e, AccessibleContext src);
    private native void mouseExited(MouseEvent e, AccessibleContext src);
    private native void mousePressed(MouseEvent e, AccessibleContext src);
    private native void mouseReleased(MouseEvent e, AccessibleContext src);

    /**
     * native methods for handling menu & popupMenu events
     */
    private native void menuCanceled(MenuEvent e, AccessibleContext src);
    private native void menuDeselected(MenuEvent e, AccessibleContext src);
    private native void menuSelected(MenuEvent e, AccessibleContext src);
    private native void popupMenuCanceled(PopupMenuEvent e, AccessibleContext src);
    private native void popupMenuWillBecomeInvisible(PopupMenuEvent e,
                                                     AccessibleContext src);
    private native void popupMenuWillBecomeVisible(PopupMenuEvent e,
                                                   AccessibleContext src);

    /* ===== event definitions ===== */

    private static final long PROPERTY_CHANGE_EVENTS = 1;
    private static final long FOCUS_GAINED_EVENTS = 2;
    private static final long FOCUS_LOST_EVENTS = 4;
    private static final long FOCUS_EVENTS = (FOCUS_GAINED_EVENTS | FOCUS_LOST_EVENTS);

    private static final long CARET_UPATE_EVENTS = 8;
    private static final long CARET_EVENTS = CARET_UPATE_EVENTS;

    private static final long MOUSE_CLICKED_EVENTS = 16;
    private static final long MOUSE_ENTERED_EVENTS = 32;
    private static final long MOUSE_EXITED_EVENTS = 64;
    private static final long MOUSE_PRESSED_EVENTS = 128;
    private static final long MOUSE_RELEASED_EVENTS = 256;
    private static final long MOUSE_EVENTS = (MOUSE_CLICKED_EVENTS | MOUSE_ENTERED_EVENTS |
                                             MOUSE_EXITED_EVENTS | MOUSE_PRESSED_EVENTS |
                                             MOUSE_RELEASED_EVENTS);

    private static final long MENU_CANCELED_EVENTS = 512;
    private static final long MENU_DESELECTED_EVENTS = 1024;
    private static final long MENU_SELECTED_EVENTS = 2048;
    private static final long MENU_EVENTS = (MENU_CANCELED_EVENTS | MENU_DESELECTED_EVENTS |
                                            MENU_SELECTED_EVENTS);

    private static final long POPUPMENU_CANCELED_EVENTS = 4096;
    private static final long POPUPMENU_WILL_BECOME_INVISIBLE_EVENTS = 8192;
    private static final long POPUPMENU_WILL_BECOME_VISIBLE_EVENTS = 16384;
    private static final long POPUPMENU_EVENTS = (POPUPMENU_CANCELED_EVENTS |
                                                 POPUPMENU_WILL_BECOME_INVISIBLE_EVENTS |
                                                 POPUPMENU_WILL_BECOME_VISIBLE_EVENTS);

    /* These use their own numbering scheme, to ensure sufficient expansion room */
    private static final long PROPERTY_NAME_CHANGE_EVENTS = 1;
    private static final long PROPERTY_DESCRIPTION_CHANGE_EVENTS = 2;
    private static final long PROPERTY_STATE_CHANGE_EVENTS = 4;
    private static final long PROPERTY_VALUE_CHANGE_EVENTS = 8;
    private static final long PROPERTY_SELECTION_CHANGE_EVENTS = 16;
    private static final long PROPERTY_TEXT_CHANGE_EVENTS = 32;
    private static final long PROPERTY_CARET_CHANGE_EVENTS = 64;
    private static final long PROPERTY_VISIBLEDATA_CHANGE_EVENTS = 128;
    private static final long PROPERTY_CHILD_CHANGE_EVENTS = 256;
    private static final long PROPERTY_ACTIVEDESCENDENT_CHANGE_EVENTS = 512;


    private static final long PROPERTY_EVENTS = (PROPERTY_NAME_CHANGE_EVENTS |
                                                PROPERTY_DESCRIPTION_CHANGE_EVENTS |
                                                PROPERTY_STATE_CHANGE_EVENTS |
                                                PROPERTY_VALUE_CHANGE_EVENTS |
                                                PROPERTY_SELECTION_CHANGE_EVENTS |
                                                PROPERTY_TEXT_CHANGE_EVENTS |
                                                PROPERTY_CARET_CHANGE_EVENTS |
                                                PROPERTY_VISIBLEDATA_CHANGE_EVENTS |
                                                PROPERTY_CHILD_CHANGE_EVENTS |
                                                PROPERTY_ACTIVEDESCENDENT_CHANGE_EVENTS);

    /**
     * The EventHandler class listens for Java events and
     * forwards them to the AT
     */
    private class EventHandler implements PropertyChangeListener,
                                          FocusListener, CaretListener,
                                          MenuListener, PopupMenuListener,
                                          MouseListener, WindowListener,
                                          ChangeListener {

        private AccessBridge accessBridge;
        private long javaEventMask = 0;
        private long accessibilityEventMask = 0;

        EventHandler(AccessBridge bridge) {
            accessBridge = bridge;

            // Register to receive WINDOW_OPENED and WINDOW_CLOSED
            // events.  Add the event source as a native window
            // handler is it implements NativeWindowHandler.
            // SwingEventMonitor.addWindowListener(this);
        }

        // --------- Event Notification Registration methods

        /**
         * Invoked the first time a window is made visible.
         */
        public void windowOpened(WindowEvent e) {
            // If the window is a NativeWindowHandler, add it.
            Object o = null;
            if (e != null)
                o = e.getSource();
            if (o instanceof NativeWindowHandler) {
                addNativeWindowHandler((NativeWindowHandler)o);
            }
        }

        /**
         * Invoked when the user attempts to close the window
         * from the window's system menu.  If the program does not
         * explicitly hide or dispose the window while processing
         * this event, the window close operation will be canceled.
         */
        public void windowClosing(WindowEvent e) {}

        /**
         * Invoked when a window has been closed as the result
         * of calling dispose on the window.
         */
        public void windowClosed(WindowEvent e) {
            // If the window is a NativeWindowHandler, remove it.
            Object o = null;
            if (e != null)
                o = e.getSource();
            if (o instanceof NativeWindowHandler) {
                removeNativeWindowHandler((NativeWindowHandler)o);
            }
        }

        /**
         * Invoked when a window is changed from a normal to a
         * minimized state. For many platforms, a minimized window
         * is displayed as the icon specified in the window's
         * iconImage property.
         * @see java.awt.Frame#setIconImage
         */
        public void windowIconified(WindowEvent e) {}

        /**
         * Invoked when a window is changed from a minimized
         * to a normal state.
         */
        public void windowDeiconified(WindowEvent e) {}

        /**
         * Invoked when the Window is set to be the active Window. Only a Frame or
         * a Dialog can be the active Window. The native windowing system may
         * denote the active Window or its children with special decorations, such
         * as a highlighted title bar. The active Window is always either the
         * focused Window, or the first Frame or Dialog that is an owner of the
         * focused Window.
         */
        public void windowActivated(WindowEvent e) {}

        /**
         * Invoked when a Window is no longer the active Window. Only a Frame or a
         * Dialog can be the active Window. The native windowing system may denote
         * the active Window or its children with special decorations, such as a
         * highlighted title bar. The active Window is always either the focused
         * Window, or the first Frame or Dialog that is an owner of the focused
         * Window.
         */
        public void windowDeactivated(WindowEvent e) {}

        /**
         * Turn on event monitoring for the event type passed in
         * If necessary, add the appropriate event listener (if
         * no other event of that type is being listened for)
         */
        void addJavaEventNotification(long type) {
            long newEventMask = javaEventMask | type;
            /*
            if ( ((javaEventMask & PROPERTY_EVENTS) == 0) &&
                 ((newEventMask & PROPERTY_EVENTS) != 0) ) {
                AccessibilityEventMonitor.addPropertyChangeListener(this);
            }
            */
            if ( ((javaEventMask & FOCUS_EVENTS) == 0) &&
                ((newEventMask & FOCUS_EVENTS) != 0) ) {
                SwingEventMonitor.addFocusListener(this);
            }
            if ( ((javaEventMask & CARET_EVENTS) == 0) &&
                ((newEventMask & CARET_EVENTS) != 0) ) {
                SwingEventMonitor.addCaretListener(this);
            }
            if ( ((javaEventMask & MOUSE_EVENTS) == 0) &&
                ((newEventMask & MOUSE_EVENTS) != 0) ) {
                SwingEventMonitor.addMouseListener(this);
            }
            if ( ((javaEventMask & MENU_EVENTS) == 0) &&
                ((newEventMask & MENU_EVENTS) != 0) ) {
                SwingEventMonitor.addMenuListener(this);
                SwingEventMonitor.addPopupMenuListener(this);
            }
            if ( ((javaEventMask & POPUPMENU_EVENTS) == 0) &&
                ((newEventMask & POPUPMENU_EVENTS) != 0) ) {
                SwingEventMonitor.addPopupMenuListener(this);
            }

            javaEventMask = newEventMask;
        }

        /**
         * Turn off event monitoring for the event type passed in
         * If necessary, remove the appropriate event listener (if
         * no other event of that type is being listened for)
         */
        void removeJavaEventNotification(long type) {
            long newEventMask = javaEventMask & (~type);
            /*
            if ( ((javaEventMask & PROPERTY_EVENTS) != 0) &&
                 ((newEventMask & PROPERTY_EVENTS) == 0) ) {
                AccessibilityEventMonitor.removePropertyChangeListener(this);
            }
            */
            if (((javaEventMask & FOCUS_EVENTS) != 0) &&
                ((newEventMask & FOCUS_EVENTS) == 0)) {
                SwingEventMonitor.removeFocusListener(this);
            }
            if (((javaEventMask & CARET_EVENTS) != 0) &&
                ((newEventMask & CARET_EVENTS) == 0)) {
                SwingEventMonitor.removeCaretListener(this);
            }
            if (((javaEventMask & MOUSE_EVENTS) == 0) &&
                ((newEventMask & MOUSE_EVENTS) != 0)) {
                SwingEventMonitor.removeMouseListener(this);
            }
            if (((javaEventMask & MENU_EVENTS) == 0) &&
                ((newEventMask & MENU_EVENTS) != 0)) {
                SwingEventMonitor.removeMenuListener(this);
            }
            if (((javaEventMask & POPUPMENU_EVENTS) == 0) &&
                ((newEventMask & POPUPMENU_EVENTS) != 0)) {
                SwingEventMonitor.removePopupMenuListener(this);
            }

            javaEventMask = newEventMask;
        }

        /**
         * Turn on event monitoring for the event type passed in
         * If necessary, add the appropriate event listener (if
         * no other event of that type is being listened for)
         */
        void addAccessibilityEventNotification(long type) {
            long newEventMask = accessibilityEventMask | type;
            if ( ((accessibilityEventMask & PROPERTY_EVENTS) == 0) &&
                 ((newEventMask & PROPERTY_EVENTS) != 0) ) {
                AccessibilityEventMonitor.addPropertyChangeListener(this);
            }
            accessibilityEventMask = newEventMask;
        }

        /**
         * Turn off event monitoring for the event type passed in
         * If necessary, remove the appropriate event listener (if
         * no other event of that type is being listened for)
         */
        void removeAccessibilityEventNotification(long type) {
            long newEventMask = accessibilityEventMask & (~type);
            if ( ((accessibilityEventMask & PROPERTY_EVENTS) != 0) &&
                 ((newEventMask & PROPERTY_EVENTS) == 0) ) {
                AccessibilityEventMonitor.removePropertyChangeListener(this);
            }
            accessibilityEventMask = newEventMask;
        }

        /**
         *  ------- property change event glue
         */
        // This is invoked on the EDT , as
        public void propertyChange(PropertyChangeEvent e) {

            accessBridge.debugString("[INFO]: propertyChange(" + e.toString() + ") called");

            if (e != null && (accessibilityEventMask & PROPERTY_EVENTS) != 0) {
                Object o = e.getSource();
                AccessibleContext ac;

                if (o instanceof AccessibleContext) {
                    ac = (AccessibleContext) o;
                } else {
                    Accessible a = Translator.getAccessible(e.getSource());
                    if (a == null)
                        return;
                    else
                        ac = a.getAccessibleContext();
                }
                if (ac != null) {
                    InvocationUtils.registerAccessibleContext(ac, AppContext.getAppContext());

                    accessBridge.debugString("[INFO]: AccessibleContext: " + ac);
                    String propertyName = e.getPropertyName();

                    if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_CARET_PROPERTY) == 0) {
                        int oldValue = 0;
                        int newValue = 0;

                        if (e.getOldValue() instanceof Integer) {
                            oldValue = ((Integer) e.getOldValue()).intValue();
                        }
                        if (e.getNewValue() instanceof Integer) {
                            newValue = ((Integer) e.getNewValue()).intValue();
                        }
                        accessBridge.debugString("[INFO]:  - about to call propertyCaretChange()   old value: " + oldValue + "new value: " + newValue);
                        accessBridge.propertyCaretChange(e, ac, oldValue, newValue);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY) == 0) {
                        String oldValue = null;
                        String newValue = null;

                        if (e.getOldValue() != null) {
                            oldValue = e.getOldValue().toString();
                        }
                        if (e.getNewValue() != null) {
                            newValue = e.getNewValue().toString();
                        }
                        accessBridge.debugString("[INFO]:  - about to call propertyDescriptionChange()   old value: " + oldValue + "new value: " + newValue);
                        accessBridge.propertyDescriptionChange(e, ac, oldValue, newValue);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_NAME_PROPERTY) == 0) {
                        String oldValue = null;
                        String newValue = null;

                        if (e.getOldValue() != null) {
                            oldValue = e.getOldValue().toString();
                        }
                        if (e.getNewValue() != null) {
                            newValue = e.getNewValue().toString();
                        }
                        accessBridge.debugString("[INFO]:  - about to call propertyNameChange()   old value: " + oldValue + " new value: " + newValue);
                        accessBridge.propertyNameChange(e, ac, oldValue, newValue);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY) == 0) {
                        accessBridge.debugString("[INFO]:  - about to call propertySelectionChange() " + ac +  "   " + Thread.currentThread() + "   " + e.getSource());

                        accessBridge.propertySelectionChange(e, ac);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_STATE_PROPERTY) == 0) {
                        String oldValue = null;
                        String newValue = null;

                        // Localization fix requested by Oliver for EA-1
                        if (e.getOldValue() != null) {
                            AccessibleState oldState = (AccessibleState) e.getOldValue();
                            oldValue = oldState.toDisplayString(Locale.US);
                        }
                        if (e.getNewValue() != null) {
                            AccessibleState newState = (AccessibleState) e.getNewValue();
                            newValue = newState.toDisplayString(Locale.US);
                        }

                        accessBridge.debugString("[INFO]:  - about to call propertyStateChange()");
                        accessBridge.propertyStateChange(e, ac, oldValue, newValue);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY) == 0) {
                        accessBridge.debugString("[INFO]:  - about to call propertyTextChange()");
                        accessBridge.propertyTextChange(e, ac);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_VALUE_PROPERTY) == 0) {  // strings 'cause of floating point, etc.
                        String oldValue = null;
                        String newValue = null;

                        if (e.getOldValue() != null) {
                            oldValue = e.getOldValue().toString();
                        }
                        if (e.getNewValue() != null) {
                            newValue = e.getNewValue().toString();
                        }
                        accessBridge.debugString("[INFO]:  - about to call propertyDescriptionChange()");
                        accessBridge.propertyValueChange(e, ac, oldValue, newValue);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY) == 0) {
                        accessBridge.propertyVisibleDataChange(e, ac);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_CHILD_PROPERTY) == 0) {
                        AccessibleContext oldAC = null;
                        AccessibleContext newAC = null;
                        Accessible a;

                        if (e.getOldValue() instanceof AccessibleContext) {
                            oldAC = (AccessibleContext) e.getOldValue();
                            InvocationUtils.registerAccessibleContext(oldAC, AppContext.getAppContext());
                        }
                        if (e.getNewValue() instanceof AccessibleContext) {
                            newAC = (AccessibleContext) e.getNewValue();
                            InvocationUtils.registerAccessibleContext(newAC, AppContext.getAppContext());
                        }
                        accessBridge.debugString("[INFO]:  - about to call propertyChildChange()   old AC: " + oldAC + "new AC: " + newAC);
                        accessBridge.propertyChildChange(e, ac, oldAC, newAC);

                    } else if (propertyName.compareTo(AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY) == 0) {
                        handleActiveDescendentEvent(e, ac);
                    }
                }
            }
        }

        /*
        * Handle an ActiveDescendent PropertyChangeEvent.  This
        * method works around a JTree bug where ActiveDescendent
        * PropertyChangeEvents have the wrong parent.
        */
        private AccessibleContext prevAC = null; // previous AccessibleContext

        private void handleActiveDescendentEvent(PropertyChangeEvent e,
                                                 AccessibleContext ac) {
            if (e == null || ac == null)
                return;
            AccessibleContext oldAC = null;
            AccessibleContext newAC = null;
            Accessible a;

            // get the old active descendent
            if (e.getOldValue() instanceof Accessible) {
                oldAC = ((Accessible) e.getOldValue()).getAccessibleContext();
            } else if (e.getOldValue() instanceof Component) {
                a = Translator.getAccessible(e.getOldValue());
                if (a != null) {
                    oldAC = a.getAccessibleContext();
                }
            }
            if (oldAC != null) {
                Accessible parent = oldAC.getAccessibleParent();
                if (parent instanceof JTree) {
                    // use the previous AccessibleJTreeNode
                    oldAC = prevAC;
                }
            }

            // get the new active descendent
            if (e.getNewValue() instanceof Accessible) {
                newAC = ((Accessible) e.getNewValue()).getAccessibleContext();
            } else if (e.getNewValue() instanceof Component) {
                a = Translator.getAccessible(e.getNewValue());
                if (a != null) {
                    newAC = a.getAccessibleContext();
                }
            }
            if (newAC != null) {
                Accessible parent = newAC.getAccessibleParent();
                if (parent instanceof JTree) {
                    // use a new AccessibleJTreeNode with the right parent
                    JTree tree = (JTree)parent;
                    newAC = new AccessibleJTreeNode(tree,
                                                    tree.getSelectionPath(),
                                                    null);
                }
            }
            prevAC = newAC;

            accessBridge.debugString("[INFO]:   - about to call propertyActiveDescendentChange()   AC: " + ac + "   old AC: " + oldAC + "new AC: " + newAC);
            InvocationUtils.registerAccessibleContext(oldAC, AppContext.getAppContext());
            InvocationUtils.registerAccessibleContext(newAC, AppContext.getAppContext());
            accessBridge.propertyActiveDescendentChange(e, ac, oldAC, newAC);
        }

        /**
        *  ------- focus event glue
        */
        private boolean stateChangeListenerAdded = false;

        public void focusGained(FocusEvent e) {
            processFocusGained();
        }

        public void stateChanged(ChangeEvent e) {
            processFocusGained();
        }

        private void processFocusGained() {
            Component focusOwner = KeyboardFocusManager.
            getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner == null) {
                return;
            }

            // Only menus and popup selections are handled by the JRootPane.
            if (focusOwner instanceof JRootPane) {
                MenuElement [] path =
                MenuSelectionManager.defaultManager().getSelectedPath();
                if (path.length > 1) {
                    Component penult = path[path.length-2].getComponent();
                    Component last = path[path.length-1].getComponent();

                    if (last instanceof JPopupMenu) {
                        // This is a popup with nothing in the popup
                        // selected. The menu itself is selected.
                        FocusEvent e = new FocusEvent(penult, FocusEvent.FOCUS_GAINED);
                        AccessibleContext context = penult.getAccessibleContext();
                        InvocationUtils.registerAccessibleContext(context, SunToolkit.targetToAppContext(penult));
                        accessBridge.focusGained(e, context);
                    } else if (penult instanceof JPopupMenu) {
                        // This is a popup with an item selected
                        FocusEvent e =
                        new FocusEvent(last, FocusEvent.FOCUS_GAINED);
                        AccessibleContext focusedAC = last.getAccessibleContext();
                        InvocationUtils.registerAccessibleContext(focusedAC, SunToolkit.targetToAppContext(last));
                        accessBridge.debugString("[INFO]:  - about to call focusGained()   AC: " + focusedAC);
                        accessBridge.focusGained(e, focusedAC);
                    }
                }
            } else {
                // The focus owner has the selection.
                if (focusOwner instanceof Accessible) {
                    FocusEvent e = new FocusEvent(focusOwner,
                                                  FocusEvent.FOCUS_GAINED);
                    AccessibleContext focusedAC = focusOwner.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(focusedAC, SunToolkit.targetToAppContext(focusOwner));
                    accessBridge.debugString("[INFO]:  - about to call focusGained()   AC: " + focusedAC);
                    accessBridge.focusGained(e, focusedAC);
                }
            }
        }

        public void focusLost(FocusEvent e) {
            if (e != null && (javaEventMask & FOCUS_LOST_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    accessBridge.debugString("[INFO]:  - about to call focusLost()   AC: " + a.getAccessibleContext());
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.focusLost(e, context);
                }
            }
        }

        /**
         *  ------- caret event glue
         */
        public void caretUpdate(CaretEvent e) {
            if (e != null && (javaEventMask & CARET_UPATE_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.caretUpdate(e, context);
                }
            }
        }

    /**
     *  ------- mouse event glue
     */

        public void mouseClicked(MouseEvent e) {
            if (e != null && (javaEventMask & MOUSE_CLICKED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.mouseClicked(e, context);
                }
            }
        }

        public void mouseEntered(MouseEvent e) {
            if (e != null && (javaEventMask & MOUSE_ENTERED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.mouseEntered(e, context);
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            if (e != null && (javaEventMask & MOUSE_EXITED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.mouseExited(e, context);
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e != null && (javaEventMask & MOUSE_PRESSED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.mousePressed(e, context);
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e != null && (javaEventMask & MOUSE_RELEASED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.mouseReleased(e, context);
                }
            }
        }

        /**
         *  ------- menu event glue
         */
        public void menuCanceled(MenuEvent e) {
            if (e != null && (javaEventMask & MENU_CANCELED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.menuCanceled(e, context);
                }
            }
        }

        public void menuDeselected(MenuEvent e) {
            if (e != null && (javaEventMask & MENU_DESELECTED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.menuDeselected(e, context);
                }
            }
        }

        public void menuSelected(MenuEvent e) {
            if (e != null && (javaEventMask & MENU_SELECTED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.menuSelected(e, context);
                }
            }
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            if (e != null && (javaEventMask & POPUPMENU_CANCELED_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.popupMenuCanceled(e, context);
                }
            }
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            if (e != null && (javaEventMask & POPUPMENU_WILL_BECOME_INVISIBLE_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.popupMenuWillBecomeInvisible(e, context);
                }
            }
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            if (e != null && (javaEventMask & POPUPMENU_WILL_BECOME_VISIBLE_EVENTS) != 0) {
                Accessible a = Translator.getAccessible(e.getSource());
                if (a != null) {
                    AccessibleContext context = a.getAccessibleContext();
                    InvocationUtils.registerAccessibleContext(context, AppContext.getAppContext());
                    accessBridge.popupMenuWillBecomeVisible(e, context);
                }
            }
        }

    } // End of EventHandler Class

    // --------- Event Notification Registration methods

    /**
     *  Wrapper method around eventHandler.addJavaEventNotification()
     */
    private void addJavaEventNotification(final long type) {
        EventQueue.invokeLater(new Runnable() {
            public void run(){
                eventHandler.addJavaEventNotification(type);
            }
        });
    }

    /**
     *  Wrapper method around eventHandler.removeJavaEventNotification()
     */
    private void removeJavaEventNotification(final long type) {
        EventQueue.invokeLater(new Runnable() {
            public void run(){
                eventHandler.removeJavaEventNotification(type);
            }
        });
    }


    /**
     *  Wrapper method around eventHandler.addAccessibilityEventNotification()
     */
    private void addAccessibilityEventNotification(final long type) {
        EventQueue.invokeLater(new Runnable() {
            public void run(){
                eventHandler.addAccessibilityEventNotification(type);
            }
        });
    }

    /**
     *  Wrapper method around eventHandler.removeAccessibilityEventNotification()
     */
    private void removeAccessibilityEventNotification(final long type) {
        EventQueue.invokeLater(new Runnable() {
            public void run(){
                eventHandler.removeAccessibilityEventNotification(type);
            }
        });
    }

    /**
     ******************************************************
     * All AccessibleRoles
     *
     * We shouldn't have to do this since it requires us
     * to synchronize the allAccessibleRoles array when
     * the AccessibleRoles class interface changes. However,
     * there is no Accessibility API method to get all
     * AccessibleRoles
     ******************************************************
     */
    private AccessibleRole [] allAccessibleRoles = {
    /**
     * Object is used to alert the user about something.
     */
    AccessibleRole.ALERT,

    /**
     * The header for a column of data.
     */
    AccessibleRole.COLUMN_HEADER,

    /**
     * Object that can be drawn into and is used to trap
     * events.
     * @see #FRAME
     * @see #GLASS_PANE
     * @see #LAYERED_PANE
     */
    AccessibleRole.CANVAS,

    /**
     * A list of choices the user can select from.  Also optionally
     * allows the user to enter a choice of their own.
     */
    AccessibleRole.COMBO_BOX,

    /**
     * An iconified internal frame in a DESKTOP_PANE.
     * @see #DESKTOP_PANE
     * @see #INTERNAL_FRAME
     */
    AccessibleRole.DESKTOP_ICON,

    /**
     * A frame-like object that is clipped by a desktop pane.  The
     * desktop pane, internal frame, and desktop icon objects are
     * often used to create multiple document interfaces within an
     * application.
     * @see #DESKTOP_ICON
     * @see #DESKTOP_PANE
     * @see #FRAME
     */
    AccessibleRole.INTERNAL_FRAME,

    /**
     * A pane that supports internal frames and
     * iconified versions of those internal frames.
     * @see #DESKTOP_ICON
     * @see #INTERNAL_FRAME
     */
    AccessibleRole.DESKTOP_PANE,

    /**
     * A specialized pane whose primary use is inside a DIALOG
     * @see #DIALOG
     */
    AccessibleRole.OPTION_PANE,

    /**
     * A top level window with no title or border.
     * @see #FRAME
     * @see #DIALOG
     */
    AccessibleRole.WINDOW,

    /**
     * A top level window with a title bar, border, menu bar, etc.  It is
     * often used as the primary window for an application.
     * @see #DIALOG
     * @see #CANVAS
     * @see #WINDOW
     */
    AccessibleRole.FRAME,

    /**
     * A top level window with title bar and a border.  A dialog is similar
     * to a frame, but it has fewer properties and is often used as a
     * secondary window for an application.
     * @see #FRAME
     * @see #WINDOW
     */
    AccessibleRole.DIALOG,

    /**
     * A specialized dialog that lets the user choose a color.
     */
    AccessibleRole.COLOR_CHOOSER,


    /**
     * A pane that allows the user to navigate through
     * and select the contents of a directory.  May be used
     * by a file chooser.
     * @see #FILE_CHOOSER
     */
    AccessibleRole.DIRECTORY_PANE,

    /**
     * A specialized dialog that displays the files in the directory
     * and lets the user select a file, browse a different directory,
     * or specify a filename.  May use the directory pane to show the
     * contents of a directory.
     * @see #DIRECTORY_PANE
     */
    AccessibleRole.FILE_CHOOSER,

    /**
     * An object that fills up space in a user interface.  It is often
     * used in interfaces to tweak the spacing between components,
     * but serves no other purpose.
     */
    AccessibleRole.FILLER,

    /**
     * A hypertext anchor
     */
    // AccessibleRole.HYPERLINK,

    /**
     * A small fixed size picture, typically used to decorate components.
     */
    AccessibleRole.ICON,

    /**
     * An object used to present an icon or short string in an interface.
     */
    AccessibleRole.LABEL,

    /**
     * A specialized pane that has a glass pane and a layered pane as its
     * children.
     * @see #GLASS_PANE
     * @see #LAYERED_PANE
     */
    AccessibleRole.ROOT_PANE,

    /**
     * A pane that is guaranteed to be painted on top
     * of all panes beneath it.
     * @see #ROOT_PANE
     * @see #CANVAS
     */
    AccessibleRole.GLASS_PANE,

    /**
     * A specialized pane that allows its children to be drawn in layers,
     * providing a form of stacking order.  This is usually the pane that
     * holds the menu bar as well as the pane that contains most of the
     * visual components in a window.
     * @see #GLASS_PANE
     * @see #ROOT_PANE
     */
    AccessibleRole.LAYERED_PANE,

    /**
     * An object that presents a list of objects to the user and allows the
     * user to select one or more of them.  A list is usually contained
     * within a scroll pane.
     * @see #SCROLL_PANE
     * @see #LIST_ITEM
     */
    AccessibleRole.LIST,

    /**
     * An object that presents an element in a list.  A list is usually
     * contained within a scroll pane.
     * @see #SCROLL_PANE
     * @see #LIST
     */
    AccessibleRole.LIST_ITEM,

    /**
     * An object usually drawn at the top of the primary dialog box of
     * an application that contains a list of menus the user can choose
     * from.  For example, a menu bar might contain menus for "File,"
     * "Edit," and "Help."
     * @see #MENU
     * @see #POPUP_MENU
     * @see #LAYERED_PANE
     */
    AccessibleRole.MENU_BAR,

    /**
     * A temporary window that is usually used to offer the user a
     * list of choices, and then hides when the user selects one of
     * those choices.
     * @see #MENU
     * @see #MENU_ITEM
     */
    AccessibleRole.POPUP_MENU,

    /**
     * An object usually found inside a menu bar that contains a list
     * of actions the user can choose from.  A menu can have any object
     * as its children, but most often they are menu items, other menus,
     * or rudimentary objects such as radio buttons, check boxes, or
     * separators.  For example, an application may have an "Edit" menu
     * that contains menu items for "Cut" and "Paste."
     * @see #MENU_BAR
     * @see #MENU_ITEM
     * @see #SEPARATOR
     * @see #RADIO_BUTTON
     * @see #CHECK_BOX
     * @see #POPUP_MENU
     */
    AccessibleRole.MENU,

    /**
     * An object usually contained in a menu that presents an action
     * the user can choose.  For example, the "Cut" menu item in an
     * "Edit" menu would be an action the user can select to cut the
     * selected area of text in a document.
     * @see #MENU_BAR
     * @see #SEPARATOR
     * @see #POPUP_MENU
     */
    AccessibleRole.MENU_ITEM,

    /**
     * An object usually contained in a menu to provide a visual
     * and logical separation of the contents in a menu.  For example,
     * the "File" menu of an application might contain menu items for
     * "Open," "Close," and "Exit," and will place a separator between
     * "Close" and "Exit" menu items.
     * @see #MENU
     * @see #MENU_ITEM
     */
    AccessibleRole.SEPARATOR,

    /**
     * An object that presents a series of panels (or page tabs), one at a
     * time, through some mechanism provided by the object.  The most common
     * mechanism is a list of tabs at the top of the panel.  The children of
     * a page tab list are all page tabs.
     * @see #PAGE_TAB
     */
    AccessibleRole.PAGE_TAB_LIST,

    /**
     * An object that is a child of a page tab list.  Its sole child is
     * the panel that is to be presented to the user when the user
     * selects the page tab from the list of tabs in the page tab list.
     * @see #PAGE_TAB_LIST
     */
    AccessibleRole.PAGE_TAB,

    /**
     * A generic container that is often used to group objects.
     */
    AccessibleRole.PANEL,

    /**
     * An object used to indicate how much of a task has been completed.
     */
    AccessibleRole.PROGRESS_BAR,

    /**
     * A text object used for passwords, or other places where the
     * text contents is not shown visibly to the user
     */
    AccessibleRole.PASSWORD_TEXT,

    /**
     * An object the user can manipulate to tell the application to do
     * something.
     * @see #CHECK_BOX
     * @see #TOGGLE_BUTTON
     * @see #RADIO_BUTTON
     */
    AccessibleRole.PUSH_BUTTON,

    /**
     * A specialized push button that can be checked or unchecked, but
     * does not provide a separate indicator for the current state.
     * @see #PUSH_BUTTON
     * @see #CHECK_BOX
     * @see #RADIO_BUTTON
     */
    AccessibleRole.TOGGLE_BUTTON,

    /**
     * A choice that can be checked or unchecked and provides a
     * separate indicator for the current state.
     * @see #PUSH_BUTTON
     * @see #TOGGLE_BUTTON
     * @see #RADIO_BUTTON
     */
    AccessibleRole.CHECK_BOX,

    /**
     * A specialized check box that will cause other radio buttons in the
     * same group to become unchecked when this one is checked.
     * @see #PUSH_BUTTON
     * @see #TOGGLE_BUTTON
     * @see #CHECK_BOX
     */
    AccessibleRole.RADIO_BUTTON,

    /**
     * The header for a row of data.
     */
    AccessibleRole.ROW_HEADER,

    /**
     * An object that allows a user to incrementally view a large amount
     * of information.  Its children can include scroll bars and a viewport.
     * @see #SCROLL_BAR
     * @see #VIEWPORT
     */
    AccessibleRole.SCROLL_PANE,

    /**
     * An object usually used to allow a user to incrementally view a
     * large amount of data.  Usually used only by a scroll pane.
     * @see #SCROLL_PANE
     */
    AccessibleRole.SCROLL_BAR,

    /**
     * An object usually used in a scroll pane.  It represents the portion
     * of the entire data that the user can see.  As the user manipulates
     * the scroll bars, the contents of the viewport can change.
     * @see #SCROLL_PANE
     */
    AccessibleRole.VIEWPORT,

    /**
     * An object that allows the user to select from a bounded range.  For
     * example, a slider might be used to select a number between 0 and 100.
     */
    AccessibleRole.SLIDER,

    /**
     * A specialized panel that presents two other panels at the same time.
     * Between the two panels is a divider the user can manipulate to make
     * one panel larger and the other panel smaller.
     */
    AccessibleRole.SPLIT_PANE,

    /**
     * An object used to present information in terms of rows and columns.
     * An example might include a spreadsheet application.
     */
    AccessibleRole.TABLE,

    /**
     * An object that presents text to the user.  The text is usually
     * editable by the user as opposed to a label.
     * @see #LABEL
     */
    AccessibleRole.TEXT,

    /**
     * An object used to present hierarchical information to the user.
     * The individual nodes in the tree can be collapsed and expanded
     * to provide selective disclosure of the tree's contents.
     */
    AccessibleRole.TREE,

    /**
     * A bar or palette usually composed of push buttons or toggle buttons.
     * It is often used to provide the most frequently used functions for an
     * application.
     */
    AccessibleRole.TOOL_BAR,

    /**
     * An object that provides information about another object.  The
     * accessibleDescription property of the tool tip is often displayed
     * to the user in a small "help bubble" when the user causes the
     * mouse to hover over the object associated with the tool tip.
     */
    AccessibleRole.TOOL_TIP,

    /**
     * An AWT component, but nothing else is known about it.
     * @see #SWING_COMPONENT
     * @see #UNKNOWN
     */
    AccessibleRole.AWT_COMPONENT,

    /**
     * A Swing component, but nothing else is known about it.
     * @see #AWT_COMPONENT
     * @see #UNKNOWN
     */
    AccessibleRole.SWING_COMPONENT,

    /**
     * The object contains some Accessible information, but its role is
     * not known.
     * @see #AWT_COMPONENT
     * @see #SWING_COMPONENT
     */
    AccessibleRole.UNKNOWN,

    // These roles are available since JDK 1.4

    /**
     * A STATUS_BAR is an simple component that can contain
     * multiple labels of status information to the user.
     AccessibleRole.STATUS_BAR,

     /**
     * A DATE_EDITOR is a component that allows users to edit
     * java.util.Date and java.util.Time objects
     AccessibleRole.DATE_EDITOR,

     /**
     * A SPIN_BOX is a simple spinner component and its main use
     * is for simple numbers.
     AccessibleRole.SPIN_BOX,

     /**
     * A FONT_CHOOSER is a component that lets the user pick various
     * attributes for fonts.
     AccessibleRole.FONT_CHOOSER,

     /**
     * A GROUP_BOX is a simple container that contains a border
     * around it and contains components inside it.
     AccessibleRole.GROUP_BOX

     /**
     * Since JDK 1.5
     *
     * A text header

     AccessibleRole.HEADER,

     /**
     * A text footer

     AccessibleRole.FOOTER,

     /**
     * A text paragraph

     AccessibleRole.PARAGRAPH,

     /**
     * A ruler is an object used to measure distance

     AccessibleRole.RULER,

     /**
     * A role indicating the object acts as a formula for
     * calculating a value.  An example is a formula in
     * a spreadsheet cell.
     AccessibleRole.EDITBAR
    */
    };

    /**
     * This class implements accessibility support for the
     * <code>JTree</code> child.  It provides an implementation of the
     * Java Accessibility API appropriate to tree nodes.
     *
     * Copied from JTree.java to work around a JTree bug where
     * ActiveDescendent PropertyChangeEvents contain the wrong
     * parent.
     */
    /**
     * This class in invoked on the EDT as its part of ActiveDescendant,
     * hence the calls do not need to be specifically made on the EDT
     */
    private class AccessibleJTreeNode extends AccessibleContext
        implements Accessible, AccessibleComponent, AccessibleSelection,
                   AccessibleAction {

        private JTree tree = null;
        private TreeModel treeModel = null;
        private Object obj = null;
        private TreePath path = null;
        private Accessible accessibleParent = null;
        private int index = 0;
        private boolean isLeaf = false;

        /**
         *  Constructs an AccessibleJTreeNode
         */
        AccessibleJTreeNode(JTree t, TreePath p, Accessible ap) {
            tree = t;
            path = p;
            accessibleParent = ap;
            if (t != null)
                treeModel = t.getModel();
            if (p != null) {
                obj = p.getLastPathComponent();
                if (treeModel != null && obj != null) {
                    isLeaf = treeModel.isLeaf(obj);
                }
            }
            debugString("[INFO]: AccessibleJTreeNode: name = "+getAccessibleName()+"; TreePath = "+p+"; parent = "+ap);
        }

        private TreePath getChildTreePath(int i) {
            // Tree nodes can't be so complex that they have
            // two sets of children -> we're ignoring that case
            if (i < 0 || i >= getAccessibleChildrenCount() || path == null || treeModel == null) {
                return null;
            } else {
                Object childObj = treeModel.getChild(obj, i);
                Object[] objPath = path.getPath();
                Object[] objChildPath = new Object[objPath.length+1];
                java.lang.System.arraycopy(objPath, 0, objChildPath, 0, objPath.length);
                objChildPath[objChildPath.length-1] = childObj;
                return new TreePath(objChildPath);
            }
        }

        /**
         * Get the AccessibleContext associated with this tree node.
         * In the implementation of the Java Accessibility API for
         * this class, return this object, which is its own
         * AccessibleContext.
         *
         * @return this object
        */
        public AccessibleContext getAccessibleContext() {
            return this;
        }

        private AccessibleContext getCurrentAccessibleContext() {
            Component c = getCurrentComponent();
            if (c instanceof Accessible) {
               return (c.getAccessibleContext());
            } else {
                return null;
            }
        }

        private Component getCurrentComponent() {
            debugString("[INFO]: AccessibleJTreeNode: getCurrentComponent");
            // is the object visible?
            // if so, get row, selected, focus & leaf state,
            // and then get the renderer component and return it
            if (tree != null && tree.isVisible(path)) {
                TreeCellRenderer r = tree.getCellRenderer();
                if (r == null) {
                    debugString("[WARN]:  returning null 1");
                    return null;
                }
                TreeUI ui = tree.getUI();
                if (ui != null) {
                    int row = ui.getRowForPath(tree, path);
                    boolean selected = tree.isPathSelected(path);
                    boolean expanded = tree.isExpanded(path);
                    boolean hasFocus = false; // how to tell?? -PK
                    Component retval = r.getTreeCellRendererComponent(tree, obj,
                                                                      selected, expanded,
                                                                      isLeaf, row, hasFocus);
                    debugString("[INFO]:   returning = "+retval.getClass());
                    return retval;
                }
            }
            debugString("[WARN]:  returning null 2");
            return null;
        }

        // AccessibleContext methods

        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object; null if this
         * object does not have a name
         */
        public String getAccessibleName() {
            debugString("[INFO]: AccessibleJTreeNode: getAccessibleName");
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                String name = ac.getAccessibleName();
                if ((name != null) && (!name.isEmpty())) {
                    String retval = ac.getAccessibleName();
                    debugString("[INFO]:     returning "+retval);
                    return retval;
                } else {
                    return null;
                }
            }
            if ((accessibleName != null) && (accessibleName.isEmpty())) {
                return accessibleName;
            } else {
                return null;
            }
        }

        /**
         * Set the localized accessible name of this object.
         *
         * @param s the new localized name of the object.
         */
        public void setAccessibleName(String s) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                ac.setAccessibleName(s);
            } else {
                super.setAccessibleName(s);
            }
        }

        //
        // *** should check tooltip text for desc. (needs MouseEvent)
        //
        /**
         * Get the accessible description of this object.
         *
         * @return the localized description of the object; null if
         * this object does not have a description
         */
        public String getAccessibleDescription() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                return ac.getAccessibleDescription();
            } else {
                return super.getAccessibleDescription();
            }
        }

        /**
         * Set the accessible description of this object.
         *
         * @param s the new localized description of the object
         */
        public void setAccessibleDescription(String s) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                ac.setAccessibleDescription(s);
            } else {
                super.setAccessibleDescription(s);
            }
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                return ac.getAccessibleRole();
            } else {
                return AccessibleRole.UNKNOWN;
            }
        }

        /**
         * Get the state set of this object.
         *
         * @return an instance of AccessibleStateSet containing the
         * current state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            if (tree == null)
                return null;
            AccessibleContext ac = getCurrentAccessibleContext();
            AccessibleStateSet states;
            int row = tree.getUI().getRowForPath(tree,path);
            int lsr = tree.getLeadSelectionRow();
            if (ac != null) {
                states = ac.getAccessibleStateSet();
            } else {
                states = new AccessibleStateSet();
            }
            // need to test here, 'cause the underlying component
            // is a cellRenderer, which is never showing...
            if (isShowing()) {
                states.add(AccessibleState.SHOWING);
            } else if (states.contains(AccessibleState.SHOWING)) {
                states.remove(AccessibleState.SHOWING);
            }
            if (isVisible()) {
                states.add(AccessibleState.VISIBLE);
            } else if (states.contains(AccessibleState.VISIBLE)) {
                states.remove(AccessibleState.VISIBLE);
            }
            if (tree.isPathSelected(path)){
                states.add(AccessibleState.SELECTED);
            }
            if (lsr == row) {
                states.add(AccessibleState.ACTIVE);
            }
            if (!isLeaf) {
                states.add(AccessibleState.EXPANDABLE);
            }
            if (tree.isExpanded(path)) {
                states.add(AccessibleState.EXPANDED);
            } else {
                states.add(AccessibleState.COLLAPSED);
            }
            if (tree.isEditable()) {
                states.add(AccessibleState.EDITABLE);
            }
            return states;
        }

        /**
         * Get the Accessible parent of this object.
         *
         * @return the Accessible parent of this object; null if this
         * object does not have an Accessible parent
         */
        public Accessible getAccessibleParent() {
            // someone wants to know, so we need to create our parent
            // if we don't have one (hey, we're a talented kid!)
            if (accessibleParent == null && path != null) {
                Object[] objPath = path.getPath();
                if (objPath.length > 1) {
                    Object objParent = objPath[objPath.length-2];
                    if (treeModel != null) {
                        index = treeModel.getIndexOfChild(objParent, obj);
                    }
                    Object[] objParentPath = new Object[objPath.length-1];
                    java.lang.System.arraycopy(objPath, 0, objParentPath,
                                               0, objPath.length-1);
                    TreePath parentPath = new TreePath(objParentPath);
                    accessibleParent = new AccessibleJTreeNode(tree,
                                                               parentPath,
                                                               null);
                    this.setAccessibleParent(accessibleParent);
                } else if (treeModel != null) {
                    accessibleParent = tree; // we're the top!
                    index = 0; // we're an only child!
                    this.setAccessibleParent(accessibleParent);
                }
            }
            return accessibleParent;
        }

        /**
         * Get the index of this object in its accessible parent.
         *
         * @return the index of this object in its parent; -1 if this
         * object does not have an accessible parent.
         * @see #getAccessibleParent
         */
        public int getAccessibleIndexInParent() {
            // index is invalid 'till we have an accessibleParent...
            if (accessibleParent == null) {
                getAccessibleParent();
            }
            if (path != null) {
                Object[] objPath = path.getPath();
                if (objPath.length > 1) {
                    Object objParent = objPath[objPath.length-2];
                    if (treeModel != null) {
                        index = treeModel.getIndexOfChild(objParent, obj);
                    }
                }
            }
            return index;
        }

        /**
         * Returns the number of accessible children in the object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            // Tree nodes can't be so complex that they have
            // two sets of children -> we're ignoring that case
            if (obj != null && treeModel != null) {
                return treeModel.getChildCount(obj);
            }
            return 0;
        }

        /**
         * Return the specified Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            // Tree nodes can't be so complex that they have
            // two sets of children -> we're ignoring that case
            if (i < 0 || i >= getAccessibleChildrenCount() || path == null || treeModel == null) {
                return null;
            } else {
                Object childObj = treeModel.getChild(obj, i);
                Object[] objPath = path.getPath();
                Object[] objChildPath = new Object[objPath.length+1];
                java.lang.System.arraycopy(objPath, 0, objChildPath, 0, objPath.length);
                objChildPath[objChildPath.length-1] = childObj;
                TreePath childPath = new TreePath(objChildPath);
                return new AccessibleJTreeNode(tree, childPath, this);
            }
        }

        /**
         * Gets the locale of the component. If the component does not have
         * a locale, then the locale of its parent is returned.
         *
         * @return This component's locale. If this component does not have
         * a locale, the locale of its parent is returned.
         * @exception IllegalComponentStateException
         * If the Component does not have its own locale and has not yet
         * been added to a containment hierarchy such that the locale can be
         * determined from the containing parent.
         * @see #setLocale
         */
        public Locale getLocale() {
            if (tree == null)
                return null;
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                return ac.getLocale();
            } else {
                return tree.getLocale();
            }
        }

        /**
         * Add a PropertyChangeListener to the listener list.
         * The listener is registered for all properties.
         *
         * @param l  The PropertyChangeListener to be added
         */
        public void addPropertyChangeListener(PropertyChangeListener l) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                ac.addPropertyChangeListener(l);
            } else {
                super.addPropertyChangeListener(l);
            }
        }

        /**
         * Remove a PropertyChangeListener from the listener list.
         * This removes a PropertyChangeListener that was registered
         * for all properties.
         *
         * @param l  The PropertyChangeListener to be removed
         */
        public void removePropertyChangeListener(PropertyChangeListener l) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                ac.removePropertyChangeListener(l);
            } else {
                super.removePropertyChangeListener(l);
            }
        }

        /**
         * Get the AccessibleAction associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleAction interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        /**
         * Get the AccessibleComponent associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleComponent interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleComponent getAccessibleComponent() {
            return this; // to override getBounds()
        }

        /**
         * Get the AccessibleSelection associated with this object if one
         * exists.  Otherwise return null.
         *
         * @return the AccessibleSelection, or null
         */
        public AccessibleSelection getAccessibleSelection() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null && isLeaf) {
                return getCurrentAccessibleContext().getAccessibleSelection();
            } else {
                return this;
            }
        }

        /**
         * Get the AccessibleText associated with this object if one
         * exists.  Otherwise return null.
         *
         * @return the AccessibleText, or null
         */
        public AccessibleText getAccessibleText() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                return getCurrentAccessibleContext().getAccessibleText();
            } else {
                return null;
            }
        }

        /**
         * Get the AccessibleValue associated with this object if one
         * exists.  Otherwise return null.
         *
         * @return the AccessibleValue, or null
         */
        public AccessibleValue getAccessibleValue() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                return getCurrentAccessibleContext().getAccessibleValue();
            } else {
                return null;
            }
        }


            // AccessibleComponent methods

        /**
         * Get the background color of this object.
         *
         * @return the background color, if supported, of the object;
         * otherwise, null
         */
        public Color getBackground() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getBackground();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.getBackground();
                } else {
                    return null;
                }
            }
        }

        /**
         * Set the background color of this object.
         *
         * @param c the new Color for the background
         */
        public void setBackground(Color c) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setBackground(c);
            } else {
                Component cp = getCurrentComponent();
                if (    cp != null) {
                    cp.setBackground(c);
                }
            }
        }


        /**
         * Get the foreground color of this object.
         *
         * @return the foreground color, if supported, of the object;
         * otherwise, null
         */
        public Color getForeground() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getForeground();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.getForeground();
                } else {
                    return null;
                }
            }
        }

        public void setForeground(Color c) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setForeground(c);
            } else {
                Component cp = getCurrentComponent();
                if (cp != null) {
                    cp.setForeground(c);
                }
            }
        }

        public Cursor getCursor() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getCursor();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.getCursor();
                } else {
                    Accessible ap = getAccessibleParent();
                    if (ap instanceof AccessibleComponent) {
                        return ((AccessibleComponent) ap).getCursor();
                    } else {
                        return null;
                    }
                }
            }
        }

        public void setCursor(Cursor c) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setCursor(c);
            } else {
                Component cp = getCurrentComponent();
                if (cp != null) {
                    cp.setCursor(c);
                }
            }
        }

        public Font getFont() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getFont();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.getFont();
                } else {
                    return null;
                }
            }
        }

        public void setFont(Font f) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setFont(f);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.setFont(f);
                }
            }
        }

        public FontMetrics getFontMetrics(Font f) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getFontMetrics(f);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.getFontMetrics(f);
                } else {
                    return null;
                }
            }
        }

        public boolean isEnabled() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).isEnabled();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.isEnabled();
                } else {
                    return false;
                }
            }
        }

        public void setEnabled(boolean b) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setEnabled(b);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.setEnabled(b);
                }
            }
        }

        public boolean isVisible() {
            if (tree == null)
                return false;
            Rectangle pathBounds = tree.getPathBounds(path);
            Rectangle parentBounds = tree.getVisibleRect();
            if ( pathBounds != null && parentBounds != null &&
                 parentBounds.intersects(pathBounds) ) {
                return true;
            } else {
                return false;
            }
        }

        public void setVisible(boolean b) {
        }

        public boolean isShowing() {
            return (tree.isShowing() && isVisible());
        }

        public boolean contains(Point p) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                Rectangle r = ((AccessibleComponent) ac).getBounds();
                return r.contains(p);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    Rectangle r = c.getBounds();
                    return r.contains(p);
                } else {
                    return getBounds().contains(p);
                }
            }
        }

        public Point getLocationOnScreen() {
            if (tree != null) {
                Point treeLocation = tree.getLocationOnScreen();
                Rectangle pathBounds = tree.getPathBounds(path);
                if (treeLocation != null && pathBounds != null) {
                    Point nodeLocation = new Point(pathBounds.x,
                                                   pathBounds.y);
                    nodeLocation.translate(treeLocation.x, treeLocation.y);
                    return nodeLocation;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        private Point getLocationInJTree() {
            Rectangle r = tree.getPathBounds(path);
            if (r != null) {
                return r.getLocation();
            } else {
                return null;
            }
        }

        public Point getLocation() {
            Rectangle r = getBounds();
            if (r != null) {
                return r.getLocation();
            } else {
                return null;
            }
        }

        public void setLocation(Point p) {
        }

        public Rectangle getBounds() {
            if (tree == null)
                return null;
            Rectangle r = tree.getPathBounds(path);
            Accessible parent = getAccessibleParent();
            if (parent instanceof AccessibleJTreeNode) {
                Point parentLoc = ((AccessibleJTreeNode) parent).getLocationInJTree();
                if (parentLoc != null && r != null) {
                    r.translate(-parentLoc.x, -parentLoc.y);
                } else {
                    return null;        // not visible!
                }
            }
            return r;
        }

        public void setBounds(Rectangle r) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setBounds(r);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.setBounds(r);
                }
            }
        }

        public Dimension getSize() {
            return getBounds().getSize();
        }

        public void setSize (Dimension d) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).setSize(d);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.setSize(d);
                }
            }
        }

        /**
        * Returns the <code>Accessible</code> child, if one exists,
        * contained at the local coordinate <code>Point</code>.
        * Otherwise returns <code>null</code>.
        *
        * @param p point in local coordinates of this
        *    <code>Accessible</code>
        * @return the <code>Accessible</code>, if it exists,
        *    at the specified location; else <code>null</code>
        */
        public Accessible getAccessibleAt(Point p) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).getAccessibleAt(p);
            } else {
                return null;
            }
        }

        public boolean isFocusTraversable() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                return ((AccessibleComponent) ac).isFocusTraversable();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    return c.isFocusable();
                } else {
                    return false;
                }
            }
        }

        public void requestFocus() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).requestFocus();
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.requestFocus();
                }
            }
        }

        public void addFocusListener(FocusListener l) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).addFocusListener(l);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.addFocusListener(l);
                }
            }
        }

        public void removeFocusListener(FocusListener l) {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac instanceof AccessibleComponent) {
                ((AccessibleComponent) ac).removeFocusListener(l);
            } else {
                Component c = getCurrentComponent();
                if (c != null) {
                    c.removeFocusListener(l);
                }
            }
        }

            // AccessibleSelection methods

        /**
         * Returns the number of items currently selected.
         * If no items are selected, the return value will be 0.
         *
         * @return the number of items currently selected.
         */
        public int getAccessibleSelectionCount() {
            int count = 0;
            int childCount = getAccessibleChildrenCount();
            for (int i = 0; i < childCount; i++) {
                TreePath childPath = getChildTreePath(i);
                if (tree.isPathSelected(childPath)) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Returns an Accessible representing the specified selected item
         * in the object.  If there isn't a selection, or there are
         * fewer items selected than the integer passed in, the return
         * value will be null.
         *
         * @param i the zero-based index of selected items
         * @return an Accessible containing the selected item
         */
        public Accessible getAccessibleSelection(int i) {
            int childCount = getAccessibleChildrenCount();
            if (i < 0 || i >= childCount) {
                return null;        // out of range
            }
            int count = 0;
            for (int j = 0; j < childCount && i >= count; j++) {
                TreePath childPath = getChildTreePath(j);
                if (tree.isPathSelected(childPath)) {
                    if (count == i) {
                        return new AccessibleJTreeNode(tree, childPath, this);
                    } else {
                        count++;
                    }
                }
            }
            return null;
        }

        /**
         * Returns true if the current child of this object is selected.
         *
         * @param i the zero-based index of the child in this Accessible
         * object.
         * @see AccessibleContext#getAccessibleChild
         */
        public boolean isAccessibleChildSelected(int i) {
            int childCount = getAccessibleChildrenCount();
            if (i < 0 || i >= childCount) {
                return false;       // out of range
            } else {
                TreePath childPath = getChildTreePath(i);
                return tree.isPathSelected(childPath);
            }
        }

         /**
         * Adds the specified selected item in the object to the object's
         * selection.  If the object supports multiple selections,
         * the specified item is added to any existing selection, otherwise
         * it replaces any existing selection in the object.  If the
         * specified item is already selected, this method has no effect.
         *
         * @param i the zero-based index of selectable items
         */
        public void addAccessibleSelection(int i) {
            if (tree == null)
                return;
            TreeModel model = tree.getModel();
            if (model != null) {
                if (i >= 0 && i < getAccessibleChildrenCount()) {
                    TreePath path = getChildTreePath(i);
                    tree.addSelectionPath(path);
                }
            }
        }

        /**
         * Removes the specified selected item in the object from the
         * object's
         * selection.  If the specified item isn't currently selected, this
         * method has no effect.
         *
         * @param i the zero-based index of selectable items
         */
        public void removeAccessibleSelection(int i) {
            if (tree == null)
                return;
            TreeModel model = tree.getModel();
            if (model != null) {
                if (i >= 0 && i < getAccessibleChildrenCount()) {
                    TreePath path = getChildTreePath(i);
                    tree.removeSelectionPath(path);
                }
            }
        }

        /**
         * Clears the selection in the object, so that nothing in the
         * object is selected.
         */
        public void clearAccessibleSelection() {
            int childCount = getAccessibleChildrenCount();
            for (int i = 0; i < childCount; i++) {
                removeAccessibleSelection(i);
            }
        }

        /**
         * Causes every selected item in the object to be selected
         * if the object supports multiple selections.
         */
        public void selectAllAccessibleSelection() {
            if (tree == null)
                return;
            TreeModel model = tree.getModel();
            if (model != null) {
                int childCount = getAccessibleChildrenCount();
                TreePath path;
                for (int i = 0; i < childCount; i++) {
                    path = getChildTreePath(i);
                    tree.addSelectionPath(path);
                }
            }
        }

            // AccessibleAction methods

        /**
         * Returns the number of accessible actions available in this
         * tree node.  If this node is not a leaf, there is at least
         * one action (toggle expand), in addition to any available
         * on the object behind the TreeCellRenderer.
         *
         * @return the number of Actions in this object
         */
        public int getAccessibleActionCount() {
            AccessibleContext ac = getCurrentAccessibleContext();
            if (ac != null) {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa != null) {
                    return (aa.getAccessibleActionCount() + (isLeaf ? 0 : 1));
                }
            }
            return isLeaf ? 0 : 1;
        }

        /**
         * Return a description of the specified action of the tree node.
         * If this node is not a leaf, there is at least one action
         * description (toggle expand), in addition to any available
         * on the object behind the TreeCellRenderer.
         *
         * @param i zero-based index of the actions
         * @return a description of the action
         */
        public String getAccessibleActionDescription(int i) {
            if (i < 0 || i >= getAccessibleActionCount()) {
                return null;
            }
            AccessibleContext ac = getCurrentAccessibleContext();
            if (i == 0) {
                // TIGER - 4766636
                // return AccessibleAction.TOGGLE_EXPAND;
                return "toggle expand";
            } else if (ac != null) {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa != null) {
                    return aa.getAccessibleActionDescription(i - 1);
                }
            }
            return null;
        }

        /**
         * Perform the specified Action on the tree node.  If this node
         * is not a leaf, there is at least one action which can be
         * done (toggle expand), in addition to any available on the
         * object behind the TreeCellRenderer.
         *
         * @param i zero-based index of actions
         * @return true if the the action was performed; else false.
         */
        public boolean doAccessibleAction(int i) {
            if (i < 0 || i >= getAccessibleActionCount()) {
                return false;
            }
            AccessibleContext ac = getCurrentAccessibleContext();
            if (i == 0) {
                if (tree.isExpanded(path)) {
                    tree.collapsePath(path);
                } else {
                    tree.expandPath(path);
                }
                return true;
            } else if (ac != null) {
                AccessibleAction aa = ac.getAccessibleAction();
                if (aa != null) {
                    return aa.doAccessibleAction(i - 1);
                }
            }
            return false;
        }

    } // inner class AccessibleJTreeNode

    /**
     * A helper class to perform {@code Callable} objects on the event dispatch thread appropriate
     * for the provided {@code AccessibleContext}.
     */
    private static class InvocationUtils {

        /**
         * Invokes a {@code Callable} in the {@code AppContext} of the given {@code Accessible}
         * and waits for it to finish blocking the caller thread.
         *
         * @param callable   the {@code Callable} to invoke
         * @param accessibleTable the {@code AccessibleExtendedTable} which would be used to find the right context
         *                   for the task execution
         * @param <T> type parameter for the result value
         *
         * @return the result of the {@code Callable} execution
         */
        public static <T> T invokeAndWait(final Callable<T> callable,
                                          final AccessibleExtendedTable accessibleTable) {
            if (accessibleTable instanceof AccessibleContext) {
                return invokeAndWait(callable, (AccessibleContext)accessibleTable);
            }
            throw new RuntimeException("Unmapped AccessibleContext used to dispatch event: " + accessibleTable);
        }

        /**
         * Invokes a {@code Callable} in the {@code AppContext} of the given {@code Accessible}
         * and waits for it to finish blocking the caller thread.
         *
         * @param callable   the {@code Callable} to invoke
         * @param accessible the {@code Accessible} which would be used to find the right context
         *                   for the task execution
         * @param <T> type parameter for the result value
         *
         * @return the result of the {@code Callable} execution
         */
        public static <T> T invokeAndWait(final Callable<T> callable,
                                          final Accessible accessible) {
            if (accessible instanceof Component) {
                return invokeAndWait(callable, (Component)accessible);
            }
            if (accessible instanceof AccessibleContext) {
                // This case also covers the Translator
                return invokeAndWait(callable, (AccessibleContext)accessible);
            }
            throw new RuntimeException("Unmapped Accessible used to dispatch event: " + accessible);
        }

        /**
         * Invokes a {@code Callable} in the {@code AppContext} of the given {@code Component}
         * and waits for it to finish blocking the caller thread.
         *
         * @param callable  the {@code Callable} to invoke
         * @param component the {@code Component} which would be used to find the right context
         *                  for the task execution
         * @param <T> type parameter for the result value
         *
         * @return the result of the {@code Callable} execution
         */
        public static <T> T invokeAndWait(final Callable<T> callable,
                                          final Component component) {
            return invokeAndWait(callable, SunToolkit.targetToAppContext(component));
        }

        /**
         * Invokes a {@code Callable} in the {@code AppContext} mapped to the given {@code AccessibleContext}
         * and waits for it to finish blocking the caller thread.
         *
         * @param callable the {@code Callable} to invoke
         * @param accessibleContext the {@code AccessibleContext} which would be used to determine the right
         *                          context for the task execution.
         * @param <T> type parameter for the result value
         *
         * @return the result of the {@code Callable} execution
         */
        public static <T> T invokeAndWait(final Callable<T> callable,
                                          final AccessibleContext accessibleContext) {
            AppContext targetContext = AWTAccessor.getAccessibleContextAccessor()
                    .getAppContext(accessibleContext);
            if (targetContext != null) {
                return invokeAndWait(callable, targetContext);
            } else {
                // Normally this should not happen, unmapped context provided and
                // the target AppContext is unknown.

                // Try to recover in case the context is a translator.
                if (accessibleContext instanceof Translator) {
                    Object source = ((Translator)accessibleContext).getSource();
                    if (source instanceof Component) {
                        return invokeAndWait(callable, (Component)source);
                    }
                }
            }
            throw new RuntimeException("Unmapped AccessibleContext used to dispatch event: " + accessibleContext);
        }

        private static <T> T invokeAndWait(final Callable<T> callable,
                                           final AppContext targetAppContext) {
            final CallableWrapper<T> wrapper = new CallableWrapper<T>(callable);
            try {
                invokeAndWait(wrapper, targetAppContext);
                T result = wrapper.getResult();
                updateAppContextMap(result, targetAppContext);
                return result;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static void invokeAndWait(final Runnable runnable,
                                        final AppContext appContext)
                throws InterruptedException, InvocationTargetException {

            EventQueue eq = SunToolkit.getSystemEventQueueImplPP(appContext);
            Object lock = new Object();
            Toolkit source = Toolkit.getDefaultToolkit();
            InvocationEvent event =
                    new InvocationEvent(source, runnable, lock, true);
            synchronized (lock) {
                eq.postEvent(event);
                lock.wait();
            }

            Throwable eventThrowable = event.getThrowable();
            if (eventThrowable != null) {
                throw new InvocationTargetException(eventThrowable);
            }
        }

        /**
         * Maps the {@code AccessibleContext} to the {@code AppContext} which should be used
         * to dispatch events related to the {@code AccessibleContext}
         * @param accessibleContext the {@code AccessibleContext} for the mapping
         * @param targetContext the {@code AppContext} for the mapping
         */
        public static void registerAccessibleContext(final AccessibleContext accessibleContext,
                                                     final AppContext targetContext) {
            if (accessibleContext != null) {
                AWTAccessor.getAccessibleContextAccessor().setAppContext(accessibleContext, targetContext);
            }
        }

        private static <T> void updateAppContextMap(final T accessibleContext,
                                                    final AppContext targetContext) {
            if (accessibleContext instanceof AccessibleContext) {
                registerAccessibleContext((AccessibleContext)accessibleContext, targetContext);
            }
        }

        private static class CallableWrapper<T> implements Runnable {
            private final Callable<T> callable;
            private volatile T object;
            private Exception e;

            CallableWrapper(final Callable<T> callable) {
                this.callable = callable;
            }

            public void run() {
                try {
                    if (callable != null) {
                        object = callable.call();
                    }
                } catch (final Exception e) {
                    this.e = e;
                }
            }

            T getResult() throws Exception {
                if (e != null)
                    throw e;
                return object;
            }
        }
    }
}
