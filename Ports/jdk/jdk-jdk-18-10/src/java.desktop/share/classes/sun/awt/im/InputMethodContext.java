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
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.InputMethodEvent;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.awt.im.spi.InputMethod;
import java.security.AccessController;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import javax.swing.JFrame;
import sun.awt.InputMethodSupport;
import sun.security.action.GetPropertyAction;

/**
 * The InputMethodContext class provides methods that input methods
 * can use to communicate with their client components.
 * It is a subclass of InputContext, which provides methods for use by
 * components.
 *
 * @author JavaSoft International
 */

public class InputMethodContext
       extends sun.awt.im.InputContext
       implements java.awt.im.spi.InputMethodContext {

    private boolean dispatchingCommittedText;

    // Creation of the context's composition area handler is
    // delayed until we really need a composition area.
    private CompositionAreaHandler compositionAreaHandler;
    private Object compositionAreaHandlerLock = new Object();

    private static boolean belowTheSpotInputRequested;
    private boolean inputMethodSupportsBelowTheSpot;

    static {
        // check whether we should use below-the-spot input
        // get property from command line
        @SuppressWarnings("removal")
        String inputStyle = AccessController.doPrivileged
                (new GetPropertyAction("java.awt.im.style", null));
        // get property from awt.properties file
        if (inputStyle == null) {
            inputStyle = Toolkit.getProperty("java.awt.im.style", null);
        }
        belowTheSpotInputRequested = "below-the-spot".equals(inputStyle);
    }

    /**
     * Constructs an InputMethodContext.
     */
    public InputMethodContext() {
        super();
    }

    void setInputMethodSupportsBelowTheSpot(boolean supported) {
        inputMethodSupportsBelowTheSpot = supported;
    }

   boolean useBelowTheSpotInput() {
        return belowTheSpotInputRequested && inputMethodSupportsBelowTheSpot;
    }

    private boolean haveActiveClient() {
        Component client = getClientComponent();
        return client != null
               && client.getInputMethodRequests() != null;
    }

    // implements java.awt.im.spi.InputMethodContext.dispatchInputMethodEvent
    public void dispatchInputMethodEvent(int id,
                AttributedCharacterIterator text, int committedCharacterCount,
                TextHitInfo caret, TextHitInfo visiblePosition) {
        // We need to record the client component as the source so
        // that we have correct information if we later have to break up this
        // event into key events.
        Component source;

        source = getClientComponent();
        if (source != null) {
            InputMethodEvent event = new InputMethodEvent(source,
                    id, text, committedCharacterCount, caret, visiblePosition);

            if (haveActiveClient() && !useBelowTheSpotInput()) {
                source.dispatchEvent(event);
            } else {
                getCompositionAreaHandler(true).processInputMethodEvent(event);
            }
        }
    }

    /**
     * Dispatches committed text to a client component.
     * Called by composition window.
     *
     * @param client The component that the text should get dispatched to.
     * @param text The iterator providing access to the committed
     *        (and possible composed) text.
     * @param committedCharacterCount The number of committed characters in the text.
     */
    synchronized void dispatchCommittedText(Component client,
                 AttributedCharacterIterator text,
                 int committedCharacterCount) {
        // note that the client is not always the current client component -
        // some host input method adapters may dispatch input method events
        // through the Java event queue, and we may have switched clients while
        // the event was in the queue.
        if (committedCharacterCount == 0
                || text.getEndIndex() <= text.getBeginIndex()) {
            return;
        }
        long time = System.currentTimeMillis();
        dispatchingCommittedText = true;
        try {
            InputMethodRequests req = client.getInputMethodRequests();
            if (req != null) {
                // active client -> send text as InputMethodEvent
                int beginIndex = text.getBeginIndex();
                AttributedCharacterIterator toBeCommitted =
                    (new AttributedString(text, beginIndex, beginIndex + committedCharacterCount)).getIterator();

                InputMethodEvent inputEvent = new InputMethodEvent(
                        client,
                        InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                        toBeCommitted,
                        committedCharacterCount,
                        null, null);

                client.dispatchEvent(inputEvent);
            } else {
                // passive client -> send text as KeyEvents
                char keyChar = text.first();
                while (committedCharacterCount-- > 0 && keyChar != CharacterIterator.DONE) {
                    KeyEvent keyEvent = new KeyEvent(client, KeyEvent.KEY_TYPED,
                                                 time, 0, KeyEvent.VK_UNDEFINED, keyChar);
                    client.dispatchEvent(keyEvent);
                    keyChar = text.next();
                }
            }
        } finally {
            dispatchingCommittedText = false;
        }
    }

    public void dispatchEvent(AWTEvent event) {
        // some host input method adapters may dispatch input method events
        // through the Java event queue. If the component that the event is
        // intended for isn't an active client, or if we're using below-the-spot
        // input, we need to dispatch this event
        // to the input window. Note that that component is not necessarily the
        // current client component, since we may have switched clients while
        // the event was in the queue.
        if (event instanceof InputMethodEvent) {
            if (((Component) event.getSource()).getInputMethodRequests() == null
                    || (useBelowTheSpotInput() && !dispatchingCommittedText)) {
                getCompositionAreaHandler(true).processInputMethodEvent((InputMethodEvent) event);
            }
        } else {
            // make sure we don't dispatch our own key events back to the input method
            if (!dispatchingCommittedText) {
                super.dispatchEvent(event);
            }
        }
    }

    /**
     * Gets this context's composition area handler, creating it if necessary.
     * If requested, it grabs the composition area for use by this context.
     * The composition area's text is not updated.
     */
    private CompositionAreaHandler getCompositionAreaHandler(boolean grab) {
        synchronized(compositionAreaHandlerLock) {
            if (compositionAreaHandler == null) {
                compositionAreaHandler = new CompositionAreaHandler(this);
            }
            compositionAreaHandler.setClientComponent(getClientComponent());
            if (grab) {
                compositionAreaHandler.grabCompositionArea(false);
            }

            return compositionAreaHandler;
        }
    }

    /**
     * Grabs the composition area for use by this context.
     * If doUpdate is true, updates the composition area with previously sent
     * composed text.
     */
    void grabCompositionArea(boolean doUpdate) {
        synchronized(compositionAreaHandlerLock) {
            if (compositionAreaHandler != null) {
                compositionAreaHandler.grabCompositionArea(doUpdate);
            } else {
                // if this context hasn't seen a need for a composition area yet,
                // just close it without creating the machinery
                CompositionAreaHandler.closeCompositionArea();
            }
        }
    }

    /**
     * Releases and closes the composition area if it is currently owned by
     * this context's composition area handler.
     */
    void releaseCompositionArea() {
        synchronized(compositionAreaHandlerLock) {
            if (compositionAreaHandler != null) {
                compositionAreaHandler.releaseCompositionArea();
            }
        }
    }

    /**
     * Calls CompositionAreaHandler.isCompositionAreaVisible() to see
     * whether the composition area is visible or not.
     * Notice that this method is always called on the AWT event dispatch
     * thread.
     */
    boolean isCompositionAreaVisible() {
        if (compositionAreaHandler != null) {
            return compositionAreaHandler.isCompositionAreaVisible();
        }

        return false;
    }
    /**
     * Calls CompositionAreaHandler.setCompositionAreaVisible to
     * show or hide the composition area.
     * As isCompositionAreaVisible method, it is always called
     * on AWT event dispatch thread.
     */
    void setCompositionAreaVisible(boolean visible) {
        if (compositionAreaHandler != null) {
            compositionAreaHandler.setCompositionAreaVisible(visible);
        }
    }

    /**
     * Calls the current client component's implementation of getTextLocation.
     */
    public Rectangle getTextLocation(TextHitInfo offset) {
        return getReq().getTextLocation(offset);
    }

    /**
     * Calls the current client component's implementation of getLocationOffset.
     */
    public TextHitInfo getLocationOffset(int x, int y) {
        return getReq().getLocationOffset(x, y);
    }

    /**
     * Calls the current client component's implementation of getInsertPositionOffset.
     */
    public int getInsertPositionOffset() {
        return getReq().getInsertPositionOffset();
    }

    /**
     * Calls the current client component's implementation of getCommittedText.
     */
    public AttributedCharacterIterator getCommittedText(int beginIndex,
                                                       int endIndex,
                                                       Attribute[] attributes) {
        return getReq().getCommittedText(beginIndex, endIndex, attributes);
    }

    /**
     * Calls the current client component's implementation of getCommittedTextLength.
     */
    public int getCommittedTextLength() {
        return getReq().getCommittedTextLength();
    }


    /**
     * Calls the current client component's implementation of cancelLatestCommittedText.
     */
    public AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes) {
        return getReq().cancelLatestCommittedText(attributes);
    }

    /**
     * Calls the current client component's implementation of getSelectedText.
     */
    public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
        return getReq().getSelectedText(attributes);
    }

    private InputMethodRequests getReq() {
        if (haveActiveClient() && !useBelowTheSpotInput()) {
            return getClientComponent().getInputMethodRequests();
        } else {
            return getCompositionAreaHandler(false);
        }
    }

    // implements java.awt.im.spi.InputMethodContext.createInputMethodWindow
    public Window createInputMethodWindow(String title, boolean attachToInputContext) {
        InputContext context = attachToInputContext ? this : null;
        return createInputMethodWindow(title, context, false);
    }

    // implements java.awt.im.spi.InputMethodContext.createInputMethodJFrame
    public JFrame createInputMethodJFrame(String title, boolean attachToInputContext) {
        InputContext context = attachToInputContext ? this : null;
        return (JFrame)createInputMethodWindow(title, context, true);
    }

    static Window createInputMethodWindow(String title, InputContext context, boolean isSwing) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        if (isSwing) {
            return new InputMethodJFrame(title, context);
        } else {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit instanceof InputMethodSupport) {
                return ((InputMethodSupport)toolkit).createInputMethodWindow(
                    title, context);
            }
        }
        throw new InternalError("Input methods must be supported");
    }

    /**
     * @see java.awt.im.spi.InputMethodContext#enableClientWindowNotification
     */
    public void enableClientWindowNotification(InputMethod inputMethod, boolean enable) {
        super.enableClientWindowNotification(inputMethod, enable);
    }

  /**
   * Disables or enables decorations for the composition window.
   */
   void setCompositionAreaUndecorated(boolean undecorated) {
        if (compositionAreaHandler != null) {
            compositionAreaHandler.setCompositionAreaUndecorated(undecorated);
        }
   }
}
