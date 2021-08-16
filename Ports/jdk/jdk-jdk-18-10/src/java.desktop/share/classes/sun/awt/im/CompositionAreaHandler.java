/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.lang.ref.WeakReference;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;

/**
 * A composition area handler handles events and input method requests for
 * the composition area. Typically each input method context has its own
 * composition area handler if it supports passive clients or below-the-spot
 * input, but all handlers share a single composition area.
 *
 * @author JavaSoft International
 */

class CompositionAreaHandler implements InputMethodListener,
                                                 InputMethodRequests {

    private static CompositionArea compositionArea;
    private static Object compositionAreaLock = new Object();
    private static CompositionAreaHandler compositionAreaOwner; // synchronized through compositionArea

    private AttributedCharacterIterator composedText;
    private TextHitInfo caret = null;
    private WeakReference<Component> clientComponent = new WeakReference<>(null);
    private InputMethodContext inputMethodContext;

    /**
     * Constructs the composition area handler.
     */
    CompositionAreaHandler(InputMethodContext context) {
        inputMethodContext = context;
    }

    /**
     * Creates the composition area.
     */
    private void createCompositionArea() {
        synchronized(compositionAreaLock) {
            compositionArea = new CompositionArea();
            if (compositionAreaOwner != null) {
                compositionArea.setHandlerInfo(compositionAreaOwner, inputMethodContext);
            }
            // If the client component is an active client using below-the-spot style, then
            // make the composition window undecorated without a title bar.
            Component client = clientComponent.get();
            if(client != null){
                InputMethodRequests req = client.getInputMethodRequests();
                if (req != null && inputMethodContext.useBelowTheSpotInput()) {
                    setCompositionAreaUndecorated(true);
                }
            }
        }
    }

    void setClientComponent(Component clientComponent) {
        this.clientComponent = new WeakReference<>(clientComponent);
    }

    /**
     * Grabs the composition area, makes this handler its owner, and installs
     * the handler and its input context into the composition area for event
     * and input method request handling.
     * If doUpdate is true, updates the composition area with previously sent
     * composed text.
     */

    void grabCompositionArea(boolean doUpdate) {
        synchronized (compositionAreaLock) {
            if (compositionAreaOwner != this) {
                compositionAreaOwner = this;
                if (compositionArea != null) {
                    compositionArea.setHandlerInfo(this, inputMethodContext);
                }
                if (doUpdate) {
                    // Create the composition area if necessary
                    if ((composedText != null) && (compositionArea == null)) {
                        createCompositionArea();
                    }
                    if (compositionArea != null) {
                        compositionArea.setText(composedText, caret);
                    }
                }
            }
        }
    }

    /**
     * Releases and closes the composition area if it is currently owned by
     * this composition area handler.
     */
    void releaseCompositionArea() {
        synchronized (compositionAreaLock) {
            if (compositionAreaOwner == this) {
                compositionAreaOwner = null;
                if (compositionArea != null) {
                    compositionArea.setHandlerInfo(null, null);
                    compositionArea.setText(null, null);
                }
            }
        }
    }

    /**
     * Releases and closes the composition area if it has been created,
     * independent of the current owner.
     */
    static void closeCompositionArea() {
        if (compositionArea != null) {
            synchronized (compositionAreaLock) {
                compositionAreaOwner = null;
                compositionArea.setHandlerInfo(null, null);
                compositionArea.setText(null, null);
            }
        }
    }

    /**
     * Returns whether the composition area is currently visible
     */
    boolean isCompositionAreaVisible() {
        if (compositionArea != null) {
            return compositionArea.isCompositionAreaVisible();
        }

        return false;
    }


    /**
     * Shows or hides the composition Area
     */
    void setCompositionAreaVisible(boolean visible) {
        if (compositionArea != null) {
            compositionArea.setCompositionAreaVisible(visible);
        }
    }

    void processInputMethodEvent(InputMethodEvent event) {
        if (event.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            inputMethodTextChanged(event);
        } else {
            caretPositionChanged(event);
        }
    }

    /**
     * set the compositionArea frame decoration
     */
    void setCompositionAreaUndecorated(boolean undecorated) {
        if (compositionArea != null) {
            compositionArea.setCompositionAreaUndecorated(undecorated);
        }
    }

    //
    // InputMethodListener methods
    //

    private static final Attribute[] IM_ATTRIBUTES =
            { TextAttribute.INPUT_METHOD_HIGHLIGHT };

    public void inputMethodTextChanged(InputMethodEvent event) {
        AttributedCharacterIterator text = event.getText();
        int committedCharacterCount = event.getCommittedCharacterCount();

        // extract composed text and prepare it for display
        composedText = null;
        caret = null;
        if (text != null
                && committedCharacterCount < text.getEndIndex() - text.getBeginIndex()) {

            // Create the composition area if necessary
            if (compositionArea == null) {
                 createCompositionArea();
            }

            // copy the composed text
            AttributedString composedTextString;
            composedTextString = new AttributedString(text,
                    text.getBeginIndex() + committedCharacterCount, // skip over committed text
                    text.getEndIndex(), IM_ATTRIBUTES);
            composedTextString.addAttribute(TextAttribute.FONT, compositionArea.getFont());
            composedText = composedTextString.getIterator();
            caret = event.getCaret();
        }

        if (compositionArea != null) {
            compositionArea.setText(composedText, caret);
        }

        // send any committed text to the text component
        if (committedCharacterCount > 0) {
            inputMethodContext.dispatchCommittedText(((Component) event.getSource()),
                                                     text, committedCharacterCount);

            // this may have changed the text location, so reposition the window
            if (isCompositionAreaVisible()) {
                compositionArea.updateWindowLocation();
            }
        }

        // event has been handled, so consume it
        event.consume();
    }

    public void caretPositionChanged(InputMethodEvent event) {
        if (compositionArea != null) {
            compositionArea.setCaret(event.getCaret());
        }

        // event has been handled, so consume it
        event.consume();
    }

    //
    // InputMethodRequests methods
    //

    /**
     * Returns the input method request handler of the client component.
     * When using the composition window for an active client (below-the-spot
     * input), input method requests that do not relate to the display of
     * the composed text are forwarded to the client component.
     */
    InputMethodRequests getClientInputMethodRequests() {
        Component client = clientComponent.get();
        if (client != null) {
            return client.getInputMethodRequests();
        }

        return null;
    }

    public Rectangle getTextLocation(TextHitInfo offset) {
        synchronized (compositionAreaLock) {
            if (compositionAreaOwner == this && isCompositionAreaVisible()) {
                return compositionArea.getTextLocation(offset);
            } else if (composedText != null) {
                // there's composed text, but it's not displayed, so fake a rectangle
                return new Rectangle(0, 0, 0, 10);
            } else {
                InputMethodRequests requests = getClientInputMethodRequests();
                if (requests != null) {
                    return requests.getTextLocation(offset);
                } else {
                    // passive client, no composed text, so fake a rectangle
                    return new Rectangle(0, 0, 0, 10);
                }
            }
        }
    }

    public TextHitInfo getLocationOffset(int x, int y) {
        synchronized (compositionAreaLock) {
            if (compositionAreaOwner == this && isCompositionAreaVisible()) {
                return compositionArea.getLocationOffset(x, y);
            } else {
                return null;
            }
        }
    }

    public int getInsertPositionOffset() {
        InputMethodRequests req = getClientInputMethodRequests();
        if (req != null) {
            return req.getInsertPositionOffset();
        }

        // we don't have access to the client component's text.
        return 0;
    }

    private static final AttributedCharacterIterator EMPTY_TEXT =
            (new AttributedString("")).getIterator();

    public AttributedCharacterIterator getCommittedText(int beginIndex,
                                                       int endIndex,
                                                       Attribute[] attributes) {
        InputMethodRequests req = getClientInputMethodRequests();
        if(req != null) {
            return req.getCommittedText(beginIndex, endIndex, attributes);
        }

        // we don't have access to the client component's text.
        return EMPTY_TEXT;
    }

    public int getCommittedTextLength() {
        InputMethodRequests req = getClientInputMethodRequests();
        if(req != null) {
            return req.getCommittedTextLength();
        }

        // we don't have access to the client component's text.
        return 0;
    }


    public AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes) {
        InputMethodRequests req = getClientInputMethodRequests();
        if(req != null) {
            return req.cancelLatestCommittedText(attributes);
        }

        // we don't have access to the client component's text.
        return null;
    }

    public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
        InputMethodRequests req = getClientInputMethodRequests();
        if(req != null) {
            return req.getSelectedText(attributes);
        }

        // we don't have access to the client component's text.
        return EMPTY_TEXT;
    }

}
