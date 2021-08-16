/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.im.InputMethodRequests;
import java.awt.im.spi.InputMethod;
import java.lang.Character.Subset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import sun.util.logging.PlatformLogger;
import sun.awt.SunToolkit;

/**
 * This InputContext class contains parts of the implementation of
 * java.text.im.InputContext. These parts have been moved
 * here to avoid exposing protected members that are needed by the
 * subclass InputMethodContext.
 *
 * @see java.awt.im.InputContext
 * @author JavaSoft Asia/Pacific
 */

public class InputContext extends java.awt.im.InputContext
                          implements ComponentListener, WindowListener {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.im.InputContext");
    // The current input method is represented by two objects:
    // a locator is used to keep information about the selected
    // input method and locale until we actually need a real input
    // method; only then the input method itself is created.
    // Once there is an input method, the input method's locale
    // takes precedence over locale information in the locator.
    private InputMethodLocator inputMethodLocator;
    private InputMethod inputMethod;
    private boolean inputMethodCreationFailed;

    // holding bin for previously used input method instances, but not the current one
    private HashMap<InputMethodLocator, InputMethod> usedInputMethods;

    // the current client component is kept until the user focusses on a different
    // client component served by the same input context. When that happens, we call
    // endComposition so that text doesn't jump from one component to another.
    private Component currentClientComponent;
    private Component awtFocussedComponent;
    private boolean   isInputMethodActive;
    private Subset[]  characterSubsets = null;

    // true if composition area has been set to invisible when focus was lost
    private boolean compositionAreaHidden = false;

    // The input context for whose input method we may have to call hideWindows
    private static InputContext inputMethodWindowContext;

    // Previously active input method to decide whether we need to call
    // InputMethodAdapter.stopListening() on activateInputMethod()
    private static InputMethod previousInputMethod = null;

    // true if the current input method requires client window change notification
    private boolean clientWindowNotificationEnabled = false;
    // client window to which this input context is listening
    private Window clientWindowListened;
    // cache location notification
    private Rectangle clientWindowLocation = null;
    // holding the state of clientWindowNotificationEnabled of only non-current input methods
    private HashMap<InputMethod, Boolean> perInputMethodState;

    // Input Method selection hot key stuff
    private static AWTKeyStroke inputMethodSelectionKey;
    private static boolean inputMethodSelectionKeyInitialized = false;
    private static final String inputMethodSelectionKeyPath = "/java/awt/im/selectionKey";
    private static final String inputMethodSelectionKeyCodeName = "keyCode";
    private static final String inputMethodSelectionKeyModifiersName = "modifiers";

    /**
     * Constructs an InputContext.
     */
    protected InputContext() {
        InputMethodManager imm = InputMethodManager.getInstance();
        synchronized (InputContext.class) {
            if (!inputMethodSelectionKeyInitialized) {
                inputMethodSelectionKeyInitialized = true;
                if (imm.hasMultipleInputMethods()) {
                    initializeInputMethodSelectionKey();
                }
            }
        }
        selectInputMethod(imm.getDefaultKeyboardLocale());
    }

    /**
     * @see java.awt.im.InputContext#selectInputMethod
     * @exception NullPointerException when the locale is null.
     */
    public synchronized boolean selectInputMethod(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }

        // see whether the current input method supports the locale
        if (inputMethod != null) {
            if (inputMethod.setLocale(locale)) {
                return true;
            }
        } else if (inputMethodLocator != null) {
            // This is not 100% correct, since the input method
            // may support the locale without advertising it.
            // But before we try instantiations and setLocale,
            // we look for an input method that's more confident.
            if (inputMethodLocator.isLocaleAvailable(locale)) {
                inputMethodLocator = inputMethodLocator.deriveLocator(locale);
                return true;
            }
        }

        // see whether there's some other input method that supports the locale
        InputMethodLocator newLocator = InputMethodManager.getInstance().findInputMethod(locale);
        if (newLocator != null) {
            changeInputMethod(newLocator);
            return true;
        }

        // make one last desperate effort with the current input method
        // ??? is this good? This is pretty high cost for something that's likely to fail.
        if (inputMethod == null && inputMethodLocator != null) {
            inputMethod = getInputMethod();
            if (inputMethod != null) {
                return inputMethod.setLocale(locale);
            }
        }
        return false;
    }

    /**
     * @see java.awt.im.InputContext#getLocale
     */
    public Locale getLocale() {
        if (inputMethod != null) {
            return inputMethod.getLocale();
        } else if (inputMethodLocator != null) {
            return inputMethodLocator.getLocale();
        } else {
            return null;
        }
    }

    /**
     * @see java.awt.im.InputContext#setCharacterSubsets
     */
    public void setCharacterSubsets(Subset[] subsets) {
        if (subsets == null) {
            characterSubsets = null;
        } else {
            characterSubsets = new Subset[subsets.length];
            System.arraycopy(subsets, 0,
                             characterSubsets, 0, characterSubsets.length);
        }
        if (inputMethod != null) {
            inputMethod.setCharacterSubsets(subsets);
        }
    }

    /**
     * @see java.awt.im.InputContext#reconvert
     * @since 1.3
     * @exception UnsupportedOperationException when input method is null
     */
    public synchronized void reconvert() {
        InputMethod inputMethod = getInputMethod();
        if (inputMethod == null) {
            throw new UnsupportedOperationException();
        }
        inputMethod.reconvert();
    }

    /**
     * @see java.awt.im.InputContext#dispatchEvent
     */
    @SuppressWarnings("fallthrough")
    public void dispatchEvent(AWTEvent event) {

        if (event instanceof InputMethodEvent) {
            return;
        }

        // Ignore focus events that relate to the InputMethodWindow of this context.
        // This is a workaround.  Should be removed after 4452384 is fixed.
        if (event instanceof FocusEvent) {
            Component opposite = ((FocusEvent)event).getOppositeComponent();
            if ((opposite != null) &&
                (getComponentWindow(opposite) instanceof InputMethodWindow) &&
                (opposite.getInputContext() == this)) {
                return;
            }
        }

        InputMethod inputMethod = getInputMethod();
        int id = event.getID();

        switch (id) {
        case FocusEvent.FOCUS_GAINED:
            focusGained((Component) event.getSource());
            break;

        case FocusEvent.FOCUS_LOST:
            focusLost((Component) event.getSource(), ((FocusEvent) event).isTemporary());
            break;

        case KeyEvent.KEY_PRESSED:
            if (checkInputMethodSelectionKey((KeyEvent)event)) {
                // pop up the input method selection menu
                InputMethodManager.getInstance().notifyChangeRequestByHotKey((Component)event.getSource());
                break;
            }

            // fall through

        default:
            if ((inputMethod != null) && (event instanceof InputEvent)) {
                inputMethod.dispatchEvent(event);
            }
        }
    }

    /**
     * Handles focus gained events for any component that's using
     * this input context.
     * These events are generated by AWT when the keyboard focus
     * moves to a component.
     * Besides actual client components, the source components
     * may also be the composition area or any component in an
     * input method window.
     * <p>
     * When handling the focus event for a client component, this
     * method checks whether the input context was previously
     * active for a different client component, and if so, calls
     * endComposition for the previous client component.
     *
     * @param source the component gaining the focus
     */
    private void focusGained(Component source) {

        /*
         * NOTE: When a Container is removing its Component which
         * invokes this.removeNotify(), the Container has the global
         * Component lock. It is possible to happen that an
         * application thread is calling this.removeNotify() while an
         * AWT event queue thread is dispatching a focus event via
         * this.dispatchEvent(). If an input method uses AWT
         * components (e.g., IIIMP status window), it causes deadlock,
         * for example, Component.show()/hide() in this situation
         * because hide/show tried to obtain the lock.  Therefore,
         * it's necessary to obtain the global Component lock before
         * activating or deactivating an input method.
         */
        synchronized (source.getTreeLock()) {
            synchronized (this) {
                if ("sun.awt.im.CompositionArea".equals(source.getClass().getName())) {
                    // no special handling for this one
                } else if (getComponentWindow(source) instanceof InputMethodWindow) {
                    // no special handling for this one either
                } else {
                    if (!source.isDisplayable()) {
                        // Component is being disposed
                        return;
                    }

                    // Focus went to a real client component.
                    // Check whether we're switching between client components
                    // that share an input context. We can't do that earlier
                    // than here because we don't want to end composition
                    // until we really know we're switching to a different component
                    if (inputMethod != null) {
                        if (currentClientComponent != null && currentClientComponent != source) {
                            if (!isInputMethodActive) {
                                activateInputMethod(false);
                            }
                            endComposition();
                            deactivateInputMethod(false);
                        }
                    }

                    currentClientComponent = source;
                }

                awtFocussedComponent = source;
                if (inputMethod instanceof InputMethodAdapter) {
                    ((InputMethodAdapter) inputMethod).setAWTFocussedComponent(source);
                }

                // it's possible that the input method is still active because
                // we suppressed a deactivate cause by an input method window
                // coming up
                if (!isInputMethodActive) {
                    activateInputMethod(true);
                }


                // If the client component is an active client with the below-the-spot
                // input style, then make the composition window undecorated without a title bar.
                InputMethodContext inputContext = ((InputMethodContext)this);
                if (!inputContext.isCompositionAreaVisible()) {
                      InputMethodRequests req = source.getInputMethodRequests();
                      if (req != null && inputContext.useBelowTheSpotInput()) {
                          inputContext.setCompositionAreaUndecorated(true);
                      } else {
                          inputContext.setCompositionAreaUndecorated(false);
                      }
                }
                // restores the composition area if it was set to invisible
                // when focus got lost
                if (compositionAreaHidden == true) {
                    ((InputMethodContext)this).setCompositionAreaVisible(true);
                    compositionAreaHidden = false;
                }
            }
        }
    }

    /**
     * Activates the current input method of this input context, and grabs
     * the composition area for use by this input context.
     * If updateCompositionArea is true, the text in the composition area
     * is updated (set to false if the text is going to change immediately
     * to avoid screen flicker).
     */
    private void activateInputMethod(boolean updateCompositionArea) {
        // call hideWindows() if this input context uses a different
        // input method than the previously activated one
        if (inputMethodWindowContext != null && inputMethodWindowContext != this &&
                inputMethodWindowContext.inputMethodLocator != null &&
                !inputMethodWindowContext.inputMethodLocator.sameInputMethod(inputMethodLocator) &&
                inputMethodWindowContext.inputMethod != null) {
            inputMethodWindowContext.inputMethod.hideWindows();
        }
        inputMethodWindowContext = this;

        if (inputMethod != null) {
            if (previousInputMethod != inputMethod &&
                    previousInputMethod instanceof InputMethodAdapter) {
                // let the host adapter pass through the input events for the
                // new input method
                ((InputMethodAdapter) previousInputMethod).stopListening();
            }
            previousInputMethod = null;

            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("Current client component " + currentClientComponent);
            }
            if (inputMethod instanceof InputMethodAdapter) {
                ((InputMethodAdapter) inputMethod).setClientComponent(currentClientComponent);
            }
            inputMethod.activate();
            isInputMethodActive = true;

            if (perInputMethodState != null) {
                Boolean state = perInputMethodState.remove(inputMethod);
                if (state != null) {
                    clientWindowNotificationEnabled = state.booleanValue();
                }
            }
            if (clientWindowNotificationEnabled) {
                if (!addedClientWindowListeners()) {
                    addClientWindowListeners();
                }
                synchronized(this) {
                    if (clientWindowListened != null) {
                        notifyClientWindowChange(clientWindowListened);
                    }
                }
            } else {
                if (addedClientWindowListeners()) {
                    removeClientWindowListeners();
                }
            }
        }
        InputMethodManager.getInstance().setInputContext(this);

        ((InputMethodContext) this).grabCompositionArea(updateCompositionArea);
    }

    static Window getComponentWindow(Component component) {
        while (true) {
            if (component == null) {
                return null;
            } else if (component instanceof Window) {
                return (Window) component;
            } else {
                component = component.getParent();
            }
        }
    }

    /**
     * Handles focus lost events for any component that's using
     * this input context.
     * These events are generated by AWT when the keyboard focus
     * moves away from a component.
     * Besides actual client components, the source components
     * may also be the composition area or any component in an
     * input method window.
     *
     * @param source the component losing the focus
     * @isTemporary whether the focus change is temporary
     */
    private void focusLost(Component source, boolean isTemporary) {

        // see the note on synchronization in focusGained
        synchronized (source.getTreeLock()) {
            synchronized (this) {

                // We need to suppress deactivation if removeNotify has been called earlier.
                // This is indicated by isInputMethodActive == false.
                if (isInputMethodActive) {
                    deactivateInputMethod(isTemporary);
                }

                awtFocussedComponent = null;
                if (inputMethod instanceof InputMethodAdapter) {
                    ((InputMethodAdapter) inputMethod).setAWTFocussedComponent(null);
                }

                // hides the composition area if currently it is visible
                InputMethodContext inputContext = ((InputMethodContext)this);
                if (inputContext.isCompositionAreaVisible()) {
                    inputContext.setCompositionAreaVisible(false);
                    compositionAreaHidden = true;
                }
            }
        }
    }

    /**
     * Checks the key event is the input method selection key or not.
     */
    private boolean checkInputMethodSelectionKey(KeyEvent event) {
        if (inputMethodSelectionKey != null) {
            AWTKeyStroke aKeyStroke = AWTKeyStroke.getAWTKeyStrokeForEvent(event);
            return inputMethodSelectionKey.equals(aKeyStroke);
        } else {
            return false;
        }
    }

    private void deactivateInputMethod(boolean isTemporary) {
        InputMethodManager.getInstance().setInputContext(null);
        if (inputMethod != null) {
            isInputMethodActive = false;
            inputMethod.deactivate(isTemporary);
            previousInputMethod = inputMethod;
        }
    }

    /**
     * Switches from the current input method to the one described by newLocator.
     * The current input method, if any, is asked to end composition, deactivated,
     * and saved for future use. The newLocator is made the current locator. If
     * the input context is active, an input method instance for the new locator
     * is obtained; otherwise this is deferred until required.
     */
    synchronized void changeInputMethod(InputMethodLocator newLocator) {
        // If we don't have a locator yet, this must be a new input context.
        // If we created a new input method here, we might get into an
        // infinite loop: create input method -> create some input method window ->
        // create new input context -> add input context to input method manager's context list ->
        // call changeInputMethod on it.
        // So, just record the locator. dispatchEvent will create the input method when needed.
        if (inputMethodLocator == null) {
            inputMethodLocator = newLocator;
            inputMethodCreationFailed = false;
            return;
        }

        // If the same input method is specified, just keep it.
        // Adjust the locale if necessary.
        if (inputMethodLocator.sameInputMethod(newLocator)) {
            Locale newLocale = newLocator.getLocale();
            if (newLocale != null && inputMethodLocator.getLocale() != newLocale) {
                if (inputMethod != null) {
                    inputMethod.setLocale(newLocale);
                }
                inputMethodLocator = newLocator;
            }
            return;
        }

        // Switch out the old input method
        Locale savedLocale = inputMethodLocator.getLocale();
        boolean wasInputMethodActive = isInputMethodActive;
        boolean wasCompositionEnabledSupported = false;
        boolean wasCompositionEnabled = false;
        if (inputMethod != null) {
            try {
                wasCompositionEnabled = inputMethod.isCompositionEnabled();
                wasCompositionEnabledSupported = true;
            } catch (UnsupportedOperationException e) { }

            if (currentClientComponent != null) {
                if (!isInputMethodActive) {
                    activateInputMethod(false);
                }
                endComposition();
                deactivateInputMethod(false);
                if (inputMethod instanceof InputMethodAdapter) {
                    ((InputMethodAdapter) inputMethod).setClientComponent(null);
                }
                if (null == currentClientComponent.getInputMethodRequests())
                    wasCompositionEnabledSupported = false;
            }
            savedLocale = inputMethod.getLocale();

            // keep the input method instance around for future use
            if (usedInputMethods == null) {
                usedInputMethods = new HashMap<>(5);
            }
            if (perInputMethodState == null) {
                perInputMethodState = new HashMap<>(5);
            }
            usedInputMethods.put(inputMethodLocator.deriveLocator(null), inputMethod);
            perInputMethodState.put(inputMethod,
                                    Boolean.valueOf(clientWindowNotificationEnabled));
            enableClientWindowNotification(inputMethod, false);
            if (this == inputMethodWindowContext) {
                inputMethod.hideWindows();
                inputMethod.removeNotify();
                inputMethodWindowContext = null;
            }
            inputMethodLocator = null;
            inputMethod = null;
            inputMethodCreationFailed = false;
        }

        // Switch in the new input method
        if (newLocator.getLocale() == null && savedLocale != null &&
                newLocator.isLocaleAvailable(savedLocale)) {
            newLocator = newLocator.deriveLocator(savedLocale);
        }
        inputMethodLocator = newLocator;
        inputMethodCreationFailed = false;

        // activate the new input method if the old one was active
        if (wasInputMethodActive) {
            inputMethod = getInputMethodInstance();
            if (inputMethod instanceof InputMethodAdapter) {
                ((InputMethodAdapter) inputMethod).setAWTFocussedComponent(awtFocussedComponent);
            }
            activateInputMethod(true);
        }

        // enable/disable composition if the old one supports querying enable/disable
        if (wasCompositionEnabledSupported) {
            inputMethod = getInputMethod();
            if (inputMethod != null) {
                try {
                    inputMethod.setCompositionEnabled(wasCompositionEnabled);
                } catch (UnsupportedOperationException e) { }
            }
        }
    }

    /**
     * Returns the client component.
     */
    Component getClientComponent() {
        return currentClientComponent;
    }

    /**
     * @see java.awt.im.InputContext#removeNotify
     * @exception NullPointerException when the component is null.
     */
    public synchronized void removeNotify(Component component) {
        if (component == null) {
            throw new NullPointerException();
        }

        if (inputMethod == null) {
            if (component == currentClientComponent) {
                currentClientComponent = null;
            }
            return;
        }

        // We may or may not get a FOCUS_LOST event for this component,
        // so do the deactivation stuff here too.
        if (component == awtFocussedComponent) {
            focusLost(component, false);
        }

        if (component == currentClientComponent) {
            if (isInputMethodActive) {
                // component wasn't the one that had the focus
                deactivateInputMethod(false);
            }
            inputMethod.removeNotify();
            if (clientWindowNotificationEnabled && addedClientWindowListeners()) {
                removeClientWindowListeners();
            }
            currentClientComponent = null;
            if (inputMethod instanceof InputMethodAdapter) {
                ((InputMethodAdapter) inputMethod).setClientComponent(null);
            }

            // removeNotify() can be issued from a thread other than the event dispatch
            // thread.  In that case, avoid possible deadlock between Component.AWTTreeLock
            // and InputMethodContext.compositionAreaHandlerLock by releasing the composition
            // area on the event dispatch thread.
            if (EventQueue.isDispatchThread()) {
                ((InputMethodContext)this).releaseCompositionArea();
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        ((InputMethodContext)InputContext.this).releaseCompositionArea();
                    }
                });
            }
        }
    }

    /**
     * @see java.awt.im.InputContext#dispose
     * @exception IllegalStateException when the currentClientComponent is not null
     */
    public synchronized void dispose() {
        if (currentClientComponent != null) {
            throw new IllegalStateException("Can't dispose InputContext while it's active");
        }
        if (inputMethod != null) {
            if (this == inputMethodWindowContext) {
                inputMethod.hideWindows();
                inputMethodWindowContext = null;
            }
            if (inputMethod == previousInputMethod) {
                previousInputMethod = null;
            }
            if (clientWindowNotificationEnabled) {
                if (addedClientWindowListeners()) {
                    removeClientWindowListeners();
                }
                clientWindowNotificationEnabled = false;
            }
            inputMethod.dispose();

            // in case the input method enabled the client window
            // notification in dispose(), which shouldn't happen, it
            // needs to be cleaned up again.
            if (clientWindowNotificationEnabled) {
                enableClientWindowNotification(inputMethod, false);
            }

            inputMethod = null;
        }
        inputMethodLocator = null;
        if (usedInputMethods != null && !usedInputMethods.isEmpty()) {
            Iterator<InputMethod> iterator = usedInputMethods.values().iterator();
            usedInputMethods = null;
            while (iterator.hasNext()) {
                iterator.next().dispose();
            }
        }

        // cleanup client window notification variables
        clientWindowNotificationEnabled = false;
        clientWindowListened = null;
        perInputMethodState = null;
    }

    /**
     * @see java.awt.im.InputContext#getInputMethodControlObject
     */
    public synchronized Object getInputMethodControlObject() {
        InputMethod inputMethod = getInputMethod();

        if (inputMethod != null) {
            return inputMethod.getControlObject();
        } else {
            return null;
        }
    }

    /**
     * @see java.awt.im.InputContext#setCompositionEnabled(boolean)
     * @exception UnsupportedOperationException when input method is null
     */
    public void setCompositionEnabled(boolean enable) {
        InputMethod inputMethod = getInputMethod();

        if (inputMethod == null) {
            throw new UnsupportedOperationException();
        }
        inputMethod.setCompositionEnabled(enable);
    }

    /**
     * @see java.awt.im.InputContext#isCompositionEnabled
     * @exception UnsupportedOperationException when input method is null
     */
    public boolean isCompositionEnabled() {
        InputMethod inputMethod = getInputMethod();

        if (inputMethod == null) {
            throw new UnsupportedOperationException();
        }
        return inputMethod.isCompositionEnabled();
    }

    /**
     * @return a string with information about the current input method.
     * @exception UnsupportedOperationException when input method is null
     */
    public String getInputMethodInfo() {
        InputMethod inputMethod = getInputMethod();

        if (inputMethod == null) {
            throw new UnsupportedOperationException("Null input method");
        }

        String inputMethodInfo = null;
        if (inputMethod instanceof InputMethodAdapter) {
            // returns the information about the host native input method.
            inputMethodInfo = ((InputMethodAdapter)inputMethod).
                getNativeInputMethodInfo();
        }

        // extracts the information from the InputMethodDescriptor
        // associated with the current java input method.
        if (inputMethodInfo == null && inputMethodLocator != null) {
            inputMethodInfo = inputMethodLocator.getDescriptor().
                getInputMethodDisplayName(getLocale(), SunToolkit.
                                          getStartupLocale());
        }

        if (inputMethodInfo != null && !inputMethodInfo.isEmpty()) {
            return inputMethodInfo;
        }

        // do our best to return something useful.
        return inputMethod.toString() + "-" + inputMethod.getLocale().toString();
    }

    /**
     * Turns off the native IM. The native IM is diabled when
     * the deactive method of InputMethod is called. It is
     * delayed until the active method is called on a different
     * peer component. This method is provided to explicitly disable
     * the native IM.
     */
    public void disableNativeIM() {
        InputMethod inputMethod = getInputMethod();
        if (inputMethod != null && inputMethod instanceof InputMethodAdapter) {
            ((InputMethodAdapter)inputMethod).stopListening();
        }
    }


    private synchronized InputMethod getInputMethod() {
        if (inputMethod != null) {
            return inputMethod;
        }

        if (inputMethodCreationFailed) {
            return null;
        }

        inputMethod = getInputMethodInstance();
        return inputMethod;
    }

    /**
     * Returns an instance of the input method described by
     * the current input method locator. This may be an input
     * method that was previously used and switched out of,
     * or a new instance. The locale, character subsets, and
     * input method context of the input method are set.
     *
     * The inputMethodCreationFailed field is set to true if the
     * instantiation failed.
     *
     * @return an InputMethod instance
     * @see java.awt.im.spi.InputMethod#setInputMethodContext
     * @see java.awt.im.spi.InputMethod#setLocale
     * @see java.awt.im.spi.InputMethod#setCharacterSubsets
     */
    private InputMethod getInputMethodInstance() {
        InputMethodLocator locator = inputMethodLocator;
        if (locator == null) {
            inputMethodCreationFailed = true;
            return null;
        }

        Locale locale = locator.getLocale();
        InputMethod inputMethodInstance = null;

        // see whether we have a previously used input method
        if (usedInputMethods != null) {
            inputMethodInstance = usedInputMethods.remove(locator.deriveLocator(null));
            if (inputMethodInstance != null) {
                if (locale != null) {
                    inputMethodInstance.setLocale(locale);
                }
                inputMethodInstance.setCharacterSubsets(characterSubsets);
                Boolean state = perInputMethodState.remove(inputMethodInstance);
                if (state != null) {
                    enableClientWindowNotification(inputMethodInstance, state.booleanValue());
                }
                ((InputMethodContext) this).setInputMethodSupportsBelowTheSpot(
                        (!(inputMethodInstance instanceof InputMethodAdapter)) ||
                        ((InputMethodAdapter) inputMethodInstance).supportsBelowTheSpot());
                return inputMethodInstance;
            }
        }

        // need to create new instance
        try {
            inputMethodInstance = locator.getDescriptor().createInputMethod();

            if (locale != null) {
                inputMethodInstance.setLocale(locale);
            }
            inputMethodInstance.setInputMethodContext((InputMethodContext) this);
            inputMethodInstance.setCharacterSubsets(characterSubsets);

        } catch (Exception e) {
            logCreationFailed(e);

            // there are a number of bad things that can happen while creating
            // the input method. In any case, we just continue without an
            // input method.
            inputMethodCreationFailed = true;

            // if the instance has been created, then it means either
            // setLocale() or setInputMethodContext() failed.
            if (inputMethodInstance != null) {
                inputMethodInstance = null;
            }
        } catch (LinkageError e) {
            logCreationFailed(e);

            // same as above
            inputMethodCreationFailed = true;
        }
        ((InputMethodContext) this).setInputMethodSupportsBelowTheSpot(
                (!(inputMethodInstance instanceof InputMethodAdapter)) ||
                ((InputMethodAdapter) inputMethodInstance).supportsBelowTheSpot());
        return inputMethodInstance;
    }

    private void logCreationFailed(Throwable throwable) {
        PlatformLogger logger = PlatformLogger.getLogger("sun.awt.im");
        if (logger.isLoggable(PlatformLogger.Level.CONFIG)) {
            String errorTextFormat = Toolkit.getProperty("AWT.InputMethodCreationFailed",
                                                         "Could not create {0}. Reason: {1}");
            Object[] args =
                {inputMethodLocator.getDescriptor().getInputMethodDisplayName(null, Locale.getDefault()),
                 throwable.getLocalizedMessage()};
            MessageFormat mf = new MessageFormat(errorTextFormat);
            logger.config(mf.format(args));
        }
    }

    InputMethodLocator getInputMethodLocator() {
        if (inputMethod != null) {
            return inputMethodLocator.deriveLocator(inputMethod.getLocale());
        }
        return inputMethodLocator;
    }

    /**
     * @see java.awt.im.InputContext#endComposition
     */
    public synchronized void endComposition() {
        if (inputMethod != null) {
            inputMethod.endComposition();
        }
    }

    /**
     * @see java.awt.im.spi.InputMethodContext#enableClientWindowNotification
     */
    synchronized void enableClientWindowNotification(InputMethod requester,
                                                     boolean enable) {
        // in case this request is not from the current input method,
        // store the request and handle it when this requesting input
        // method becomes the current one.
        if (requester != inputMethod) {
            if (perInputMethodState == null) {
                perInputMethodState = new HashMap<>(5);
            }
            perInputMethodState.put(requester, Boolean.valueOf(enable));
            return;
        }

        if (clientWindowNotificationEnabled != enable) {
            clientWindowLocation = null;
            clientWindowNotificationEnabled = enable;
        }
        if (clientWindowNotificationEnabled) {
            if (!addedClientWindowListeners()) {
                addClientWindowListeners();
            }
            if (clientWindowListened != null) {
                clientWindowLocation = null;
                notifyClientWindowChange(clientWindowListened);
            }
        } else {
            if (addedClientWindowListeners()) {
                removeClientWindowListeners();
            }
        }
    }

    private synchronized void notifyClientWindowChange(Window window) {
        if (inputMethod == null) {
            return;
        }

        // if the window is invisible or iconified, send null to the input method.
        if (!window.isVisible() ||
            ((window instanceof Frame) && ((Frame)window).getState() == Frame.ICONIFIED)) {
            clientWindowLocation = null;
            inputMethod.notifyClientWindowChange(null);
            return;
        }
        Rectangle location = window.getBounds();
        if (clientWindowLocation == null || !clientWindowLocation.equals(location)) {
            clientWindowLocation = location;
            inputMethod.notifyClientWindowChange(clientWindowLocation);
        }
    }

    private synchronized void addClientWindowListeners() {
        Component client = getClientComponent();
        if (client == null) {
            return;
        }
        Window window = getComponentWindow(client);
        if (window == null) {
            return;
        }
        window.addComponentListener(this);
        window.addWindowListener(this);
        clientWindowListened = window;
    }

    private synchronized void removeClientWindowListeners() {
        clientWindowListened.removeComponentListener(this);
        clientWindowListened.removeWindowListener(this);
        clientWindowListened = null;
    }

    /**
     * Returns true if listeners have been set up for client window
     * change notification.
     */
    private boolean addedClientWindowListeners() {
        return clientWindowListened != null;
    }

    /*
     * ComponentListener and WindowListener implementation
     */
    public void componentResized(ComponentEvent e) {
        notifyClientWindowChange((Window)e.getComponent());
    }

    public void componentMoved(ComponentEvent e) {
        notifyClientWindowChange((Window)e.getComponent());
    }

    public void componentShown(ComponentEvent e) {
        notifyClientWindowChange((Window)e.getComponent());
    }

    public void componentHidden(ComponentEvent e) {
        notifyClientWindowChange((Window)e.getComponent());
    }

    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {
        notifyClientWindowChange(e.getWindow());
    }

    public void windowDeiconified(WindowEvent e) {
        notifyClientWindowChange(e.getWindow());
    }

    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    /**
     * Initializes the input method selection key definition in preference trees
     */
    @SuppressWarnings("removal")
    private void initializeInputMethodSelectionKey() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                // Look in user's tree
                Preferences root = Preferences.userRoot();
                inputMethodSelectionKey = getInputMethodSelectionKeyStroke(root);

                if (inputMethodSelectionKey == null) {
                    // Look in system's tree
                    root = Preferences.systemRoot();
                    inputMethodSelectionKey = getInputMethodSelectionKeyStroke(root);
                }
                return null;
            }
        });
    }

    private AWTKeyStroke getInputMethodSelectionKeyStroke(Preferences root) {
        try {
            if (root.nodeExists(inputMethodSelectionKeyPath)) {
                Preferences node = root.node(inputMethodSelectionKeyPath);
                int keyCode = node.getInt(inputMethodSelectionKeyCodeName, KeyEvent.VK_UNDEFINED);
                if (keyCode != KeyEvent.VK_UNDEFINED) {
                    int modifiers = node.getInt(inputMethodSelectionKeyModifiersName, 0);
                    return AWTKeyStroke.getAWTKeyStroke(keyCode, modifiers);
                }
            }
        } catch (BackingStoreException bse) {
        }

        return null;
    }
}
