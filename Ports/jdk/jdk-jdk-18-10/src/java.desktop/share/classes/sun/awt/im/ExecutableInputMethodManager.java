/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.im;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.PopupMenu;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Toolkit;
import sun.awt.AppContext;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InvocationEvent;
import java.awt.im.spi.InputMethodDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import sun.awt.InputMethodSupport;
import sun.awt.SunToolkit;

/**
 * {@code ExecutableInputMethodManager} is the implementation of the
 * {@code InputMethodManager} class. It is runnable as a separate
 * thread in the AWT environment.&nbsp;
 * {@code InputMethodManager.getInstance()} creates an instance of
 * {@code ExecutableInputMethodManager} and executes it as a deamon
 * thread.
 *
 * @see InputMethodManager
 */
class ExecutableInputMethodManager extends InputMethodManager
                                   implements Runnable
{
    // the input context that's informed about selections from the user interface
    private InputContext currentInputContext;

    // Menu item string for the trigger menu.
    private String triggerMenuString;

    // popup menu for selecting an input method
    private InputMethodPopupMenu selectionMenu;
    private static String selectInputMethodMenuTitle;

    // locator and name of host adapter
    private InputMethodLocator hostAdapterLocator;

    // locators for Java input methods
    private int javaInputMethodCount;         // number of Java input methods found
    private Vector<InputMethodLocator> javaInputMethodLocatorList;

    // component that is requesting input method switch
    // must be Frame or Dialog
    private Component requestComponent;

    // input context that is requesting input method switch
    private InputContext requestInputContext;

    // IM preference stuff
    private static final String preferredIMNode = "/sun/awt/im/preferredInputMethod";
    private static final String descriptorKey = "descriptor";
    private Hashtable<String, InputMethodLocator> preferredLocatorCache = new Hashtable<>();
    private Preferences userRoot;

    ExecutableInputMethodManager() {

        // set up host adapter locator
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        try {
            if (toolkit instanceof InputMethodSupport) {
                InputMethodDescriptor hostAdapterDescriptor =
                    ((InputMethodSupport)toolkit)
                    .getInputMethodAdapterDescriptor();
                if (hostAdapterDescriptor != null) {
                    hostAdapterLocator = new InputMethodLocator(hostAdapterDescriptor, null, null);
                }
            }
        } catch (AWTException e) {
            // if we can't get a descriptor, we'll just have to do without native input methods
        }

        javaInputMethodLocatorList = new Vector<InputMethodLocator>();
        initializeInputMethodLocatorList();
    }

    synchronized void initialize() {
        selectInputMethodMenuTitle = Toolkit.getProperty("AWT.InputMethodSelectionMenu", "Select Input Method");

        triggerMenuString = selectInputMethodMenuTitle;
    }

    public void run() {
        // If there are no multiple input methods to choose from, wait forever
        while (!hasMultipleInputMethods()) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
            }
        }

        // Loop for processing input method change requests
        while (true) {
            waitForChangeRequest();
            initializeInputMethodLocatorList();
            try {
                if (requestComponent != null) {
                    showInputMethodMenuOnRequesterEDT(requestComponent);
                } else {
                    // show the popup menu within the event thread
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            showInputMethodMenu();
                        }
                    });
                }
            } catch (InterruptedException ie) {
            } catch (InvocationTargetException ite) {
                // should we do anything under these exceptions?
            }
        }
    }

    // Shows Input Method Menu on the EDT of requester component
    // to avoid side effects. See 6544309.
    private void showInputMethodMenuOnRequesterEDT(Component requester)
        throws InterruptedException, InvocationTargetException {

        if (requester == null){
            return;
        }

        class AWTInvocationLock {}
        Object lock = new AWTInvocationLock();

        InvocationEvent event =
                new InvocationEvent(requester,
                                    new Runnable() {
                                        public void run() {
                                            showInputMethodMenu();
                                        }
                                    },
                                    lock,
                                    true);

        AppContext requesterAppContext = SunToolkit.targetToAppContext(requester);
        synchronized (lock) {
            SunToolkit.postEvent(requesterAppContext, event);
            while (!event.isDispatched()) {
                lock.wait();
            }
        }

        Throwable eventThrowable = event.getThrowable();
        if (eventThrowable != null) {
            throw new InvocationTargetException(eventThrowable);
        }
    }

    void setInputContext(InputContext inputContext) {
        if (currentInputContext != null && inputContext != null) {
            // don't throw this exception until 4237852 is fixed
            // throw new IllegalStateException("Can't have two active InputContext at the same time");
        }
        currentInputContext = inputContext;
    }

    public synchronized void notifyChangeRequest(Component comp) {
        if (!(comp instanceof Frame || comp instanceof Dialog))
            return;

        // if busy with the current request, ignore this request.
        if (requestComponent != null)
            return;

        requestComponent = comp;
        notify();
    }

    public synchronized void notifyChangeRequestByHotKey(Component comp) {
        while (!(comp instanceof Frame || comp instanceof Dialog)) {
            if (comp == null) {
                // no Frame or Dialog found in containment hierarchy.
                return;
            }
            comp = comp.getParent();
        }

        notifyChangeRequest(comp);
    }

    public String getTriggerMenuString() {
        return triggerMenuString;
    }

    /*
     * Returns true if the environment indicates there are multiple input methods
     */
    boolean hasMultipleInputMethods() {
        return ((hostAdapterLocator != null) && (javaInputMethodCount > 0)
                || (javaInputMethodCount > 1));
    }

    private synchronized void waitForChangeRequest() {
        try {
            while (requestComponent == null) {
                wait();
            }
        } catch (InterruptedException e) {
        }
    }

    /*
     * initializes the input method locator list for all
     * installed input method descriptors.
     */
    @SuppressWarnings("removal")
    private void initializeInputMethodLocatorList() {
        synchronized (javaInputMethodLocatorList) {
            javaInputMethodLocatorList.clear();
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public Object run() {
                        for (InputMethodDescriptor descriptor :
                            ServiceLoader.load(InputMethodDescriptor.class,
                                               ClassLoader.getSystemClassLoader())) {
                            ClassLoader cl = descriptor.getClass().getClassLoader();
                            javaInputMethodLocatorList.add(new InputMethodLocator(descriptor, cl, null));
                        }
                        return null;
                    }
                });
            }  catch (PrivilegedActionException e) {
                e.printStackTrace();
            }
            javaInputMethodCount = javaInputMethodLocatorList.size();
        }

        if (hasMultipleInputMethods()) {
            // initialize preferences
            if (userRoot == null) {
                userRoot = getUserRoot();
            }
        } else {
            // indicate to clients not to offer the menu
            triggerMenuString = null;
        }
    }

    private void showInputMethodMenu() {

        if (!hasMultipleInputMethods()) {
            requestComponent = null;
            return;
        }

        // initialize pop-up menu
        selectionMenu = InputMethodPopupMenu.getInstance(requestComponent, selectInputMethodMenuTitle);

        // we have to rebuild the menu each time because
        // some input methods (such as IIIMP) may change
        // their list of supported locales dynamically
        selectionMenu.removeAll();

        // get information about the currently selected input method
        // ??? if there's no current input context, what's the point
        // of showing the menu?
        String currentSelection = getCurrentSelection();

        // Add menu item for host adapter
        if (hostAdapterLocator != null) {
            selectionMenu.addOneInputMethodToMenu(hostAdapterLocator, currentSelection);
            selectionMenu.addSeparator();
        }

        // Add menu items for other input methods
        for (int i = 0; i < javaInputMethodLocatorList.size(); i++) {
            InputMethodLocator locator = javaInputMethodLocatorList.get(i);
            selectionMenu.addOneInputMethodToMenu(locator, currentSelection);
        }

        synchronized (this) {
            selectionMenu.addToComponent(requestComponent);
            requestInputContext = currentInputContext;
            selectionMenu.show(requestComponent, 60, 80); // TODO: get proper x, y...
            requestComponent = null;
        }
    }

    private String getCurrentSelection() {
        InputContext inputContext = currentInputContext;
        if (inputContext != null) {
            InputMethodLocator locator = inputContext.getInputMethodLocator();
            if (locator != null) {
                return locator.getActionCommandString();
            }
        }
        return null;
    }

    synchronized void changeInputMethod(String choice) {
        InputMethodLocator locator = null;

        String inputMethodName = choice;
        String localeString = null;
        int index = choice.indexOf('\n');
        if (index != -1) {
            localeString = choice.substring(index + 1);
            inputMethodName = choice.substring(0, index);
        }
        if (hostAdapterLocator.getActionCommandString().equals(inputMethodName)) {
            locator = hostAdapterLocator;
        } else {
            for (int i = 0; i < javaInputMethodLocatorList.size(); i++) {
                InputMethodLocator candidate = javaInputMethodLocatorList.get(i);
                String name = candidate.getActionCommandString();
                if (name.equals(inputMethodName)) {
                    locator = candidate;
                    break;
                }
            }
        }

        if (locator != null && localeString != null) {
            String language = "", country = "", variant = "";
            int postIndex = localeString.indexOf('_');
            if (postIndex == -1) {
                language = localeString;
            } else {
                language = localeString.substring(0, postIndex);
                int preIndex = postIndex + 1;
                postIndex = localeString.indexOf('_', preIndex);
                if (postIndex == -1) {
                    country = localeString.substring(preIndex);
                } else {
                    country = localeString.substring(preIndex, postIndex);
                    variant = localeString.substring(postIndex + 1);
                }
            }
            Locale locale = new Locale(language, country, variant);
            locator = locator.deriveLocator(locale);
        }

        if (locator == null)
            return;

        // tell the input context about the change
        if (requestInputContext != null) {
            requestInputContext.changeInputMethod(locator);
            requestInputContext = null;

            // remember the selection
            putPreferredInputMethod(locator);
        }
    }

    InputMethodLocator findInputMethod(Locale locale) {
        // look for preferred input method first
        InputMethodLocator locator = getPreferredInputMethod(locale);
        if (locator != null) {
            return locator;
        }

        if (hostAdapterLocator != null && hostAdapterLocator.isLocaleAvailable(locale)) {
            return hostAdapterLocator.deriveLocator(locale);
        }

        // Update the locator list
        initializeInputMethodLocatorList();

        for (int i = 0; i < javaInputMethodLocatorList.size(); i++) {
            InputMethodLocator candidate = javaInputMethodLocatorList.get(i);
            if (candidate.isLocaleAvailable(locale)) {
                return candidate.deriveLocator(locale);
            }
        }
        return null;
    }

    Locale getDefaultKeyboardLocale() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof InputMethodSupport) {
            return ((InputMethodSupport)toolkit).getDefaultKeyboardLocale();
        } else {
            return Locale.getDefault();
        }
    }

    /**
     * Returns a InputMethodLocator object that the
     * user prefers for the given locale.
     *
     * @param locale Locale for which the user prefers the input method.
     */
    private synchronized InputMethodLocator getPreferredInputMethod(Locale locale) {
        InputMethodLocator preferredLocator = null;

        if (!hasMultipleInputMethods()) {
            // No need to look for a preferred Java input method
            return null;
        }

        // look for the cached preference first.
        preferredLocator = preferredLocatorCache.get(locale.toString().intern());
        if (preferredLocator != null) {
            return preferredLocator;
        }

        // look for the preference in the user preference tree
        String nodePath = findPreferredInputMethodNode(locale);
        String descriptorName = readPreferredInputMethod(nodePath);
        Locale advertised;

        // get the locator object
        if (descriptorName != null) {
            // check for the host adapter first
            if (hostAdapterLocator != null &&
                hostAdapterLocator.getDescriptor().getClass().getName().equals(descriptorName)) {
                advertised = getAdvertisedLocale(hostAdapterLocator, locale);
                if (advertised != null) {
                    preferredLocator = hostAdapterLocator.deriveLocator(advertised);
                    preferredLocatorCache.put(locale.toString().intern(), preferredLocator);
                }
                return preferredLocator;
            }
            // look for Java input methods
            for (int i = 0; i < javaInputMethodLocatorList.size(); i++) {
                InputMethodLocator locator = javaInputMethodLocatorList.get(i);
                InputMethodDescriptor descriptor = locator.getDescriptor();
                if (descriptor.getClass().getName().equals(descriptorName)) {
                    advertised = getAdvertisedLocale(locator, locale);
                    if (advertised != null) {
                        preferredLocator = locator.deriveLocator(advertised);
                        preferredLocatorCache.put(locale.toString().intern(), preferredLocator);
                    }
                    return preferredLocator;
                }
            }

            // maybe preferred input method information is bogus.
            writePreferredInputMethod(nodePath, null);
        }

        return null;
    }

    private String findPreferredInputMethodNode(Locale locale) {
        if (userRoot == null) {
            return null;
        }

        // create locale node relative path
        String nodePath = preferredIMNode + "/" + createLocalePath(locale);

        // look for the descriptor
        while (!nodePath.equals(preferredIMNode)) {
            try {
                if (userRoot.nodeExists(nodePath)) {
                    if (readPreferredInputMethod(nodePath) != null) {
                        return nodePath;
                    }
                }
            } catch (BackingStoreException bse) {
            }

            // search at parent's node
            nodePath = nodePath.substring(0, nodePath.lastIndexOf('/'));
        }

        return null;
    }

    private String readPreferredInputMethod(String nodePath) {
        if ((userRoot == null) || (nodePath == null)) {
            return null;
        }

        return userRoot.node(nodePath).get(descriptorKey, null);
    }

    /**
     * Writes the preferred input method descriptor class name into
     * the user's Preferences tree in accordance with the given locale.
     *
     * @param locator input method locator to remember.
     */
    private synchronized void putPreferredInputMethod(InputMethodLocator locator) {
        InputMethodDescriptor descriptor = locator.getDescriptor();
        Locale preferredLocale = locator.getLocale();

        if (preferredLocale == null) {
            // check available locales of the input method
            try {
                Locale[] availableLocales = descriptor.getAvailableLocales();
                if (availableLocales.length == 1) {
                    preferredLocale = availableLocales[0];
                } else {
                    // there is no way to know which locale is the preferred one, so do nothing.
                    return;
                }
            } catch (AWTException ae) {
                // do nothing here, either.
                return;
            }
        }

        // for regions that have only one language, we need to regard
        // "xx_YY" as "xx" when putting the preference into tree
        if (preferredLocale.equals(Locale.JAPAN)) {
            preferredLocale = Locale.JAPANESE;
        }
        if (preferredLocale.equals(Locale.KOREA)) {
            preferredLocale = Locale.KOREAN;
        }
        if (preferredLocale.equals(new Locale("th", "TH"))) {
            preferredLocale = new Locale("th");
        }

        // obtain node
        String path = preferredIMNode + "/" + createLocalePath(preferredLocale);

        // write in the preference tree
        writePreferredInputMethod(path, descriptor.getClass().getName());
        preferredLocatorCache.put(preferredLocale.toString().intern(),
            locator.deriveLocator(preferredLocale));

        return;
    }

    private String createLocalePath(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        String localePath = null;
        if (!variant.isEmpty()) {
            localePath = "_" + language + "/_" + country + "/_" + variant;
        } else if (!country.isEmpty()) {
            localePath = "_" + language + "/_" + country;
        } else {
            localePath = "_" + language;
        }

        return localePath;
    }

    private void writePreferredInputMethod(String path, String descriptorName) {
        if (userRoot != null) {
            Preferences node = userRoot.node(path);

            // record it
            if (descriptorName != null) {
                node.put(descriptorKey, descriptorName);
            } else {
                node.remove(descriptorKey);
            }
        }
    }

    @SuppressWarnings("removal")
    private Preferences getUserRoot() {
        return AccessController.doPrivileged(new PrivilegedAction<Preferences>() {
            public Preferences run() {
                return Preferences.userRoot();
            }
        });
    }

    private Locale getAdvertisedLocale(InputMethodLocator locator, Locale locale) {
        Locale advertised = null;

        if (locator.isLocaleAvailable(locale)) {
            advertised = locale;
        } else if (locale.getLanguage().equals("ja")) {
            // for Japanese, Korean, and Thai, check whether the input method supports
            // language or language_COUNTRY.
            if (locator.isLocaleAvailable(Locale.JAPAN)) {
                advertised = Locale.JAPAN;
            } else if (locator.isLocaleAvailable(Locale.JAPANESE)) {
                advertised = Locale.JAPANESE;
            }
        } else if (locale.getLanguage().equals("ko")) {
            if (locator.isLocaleAvailable(Locale.KOREA)) {
                advertised = Locale.KOREA;
            } else if (locator.isLocaleAvailable(Locale.KOREAN)) {
                advertised = Locale.KOREAN;
            }
        } else if (locale.getLanguage().equals("th")) {
            if (locator.isLocaleAvailable(new Locale("th", "TH"))) {
                advertised = new Locale("th", "TH");
            } else if (locator.isLocaleAvailable(new Locale("th"))) {
                advertised = new Locale("th");
            }
        }

        return advertised;
    }
}
