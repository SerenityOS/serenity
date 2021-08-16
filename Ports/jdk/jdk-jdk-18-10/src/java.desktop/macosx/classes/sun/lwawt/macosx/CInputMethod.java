/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.im.spi.*;
import java.util.*;
import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import java.awt.im.*;
import java.awt.font.*;
import java.lang.Character.Subset;
import java.lang.reflect.InvocationTargetException;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.*;
import javax.swing.text.JTextComponent;

import sun.awt.AWTAccessor;
import sun.awt.im.InputMethodAdapter;
import sun.lwawt.*;

import static sun.awt.AWTAccessor.ComponentAccessor;

public class CInputMethod extends InputMethodAdapter {
    private InputMethodContext fIMContext;
    private Component fAwtFocussedComponent;
    private LWComponentPeer<?, ?> fAwtFocussedComponentPeer;
    private boolean isActive;

    private static Map<TextAttribute, Integer>[] sHighlightStyles;

    // Intitalize highlight mapping table and its mapper.
    static {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Map<TextAttribute, Integer>[] styles = new Map[4];
        HashMap<TextAttribute, Integer> map;

        // UNSELECTED_RAW_TEXT_HIGHLIGHT
        map = new HashMap<TextAttribute, Integer>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE,
                TextAttribute.UNDERLINE_LOW_GRAY);
        styles[0] = Collections.unmodifiableMap(map);

        // SELECTED_RAW_TEXT_HIGHLIGHT
        map = new HashMap<TextAttribute, Integer>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE,
                TextAttribute.UNDERLINE_LOW_GRAY);
        styles[1] = Collections.unmodifiableMap(map);

        // UNSELECTED_CONVERTED_TEXT_HIGHLIGHT
        map = new HashMap<TextAttribute, Integer>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE,
                TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
        styles[2] = Collections.unmodifiableMap(map);

        // SELECTED_CONVERTED_TEXT_HIGHLIGHT
        map = new HashMap<TextAttribute, Integer>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE,
                TextAttribute.UNDERLINE_LOW_TWO_PIXEL);
        styles[3] = Collections.unmodifiableMap(map);

        sHighlightStyles = styles;

        nativeInit();

    }

    public CInputMethod() {
    }


    /**
        * Sets the input method context, which is used to dispatch input method
     * events to the client component and to request information from
     * the client component.
     * <p>
     * This method is called once immediately after instantiating this input
     * method.
     *
     * @param context the input method context for this input method
     * @exception NullPointerException if {@code context} is null
     */
    public void setInputMethodContext(InputMethodContext context) {
        fIMContext = context;
    }

    /**
        * Attempts to set the input locale. If the input method supports the
     * desired locale, it changes its behavior to support input for the locale
     * and returns true.
     * Otherwise, it returns false and does not change its behavior.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod},
     * <li>when switching to this input method through the user interface if the user
     *     specified a locale or if the previously selected input method's
     *     {@link java.awt.im.spi.InputMethod#getLocale getLocale} method
     *     returns a non-null value.
     * </ul>
     *
     * @param lang locale to input
     * @return whether the specified locale is supported
     * @exception NullPointerException if {@code locale} is null
     */
    public boolean setLocale(Locale lang) {
        return setLocale(lang, false);
    }

    private boolean setLocale(Locale lang, boolean onActivate) {
        Object[] available = CInputMethodDescriptor.getAvailableLocalesInternal();
        for (int i = 0; i < available.length; i++) {
            Locale locale = (Locale)available[i];
            if (lang.equals(locale) ||
                // special compatibility rule for Japanese and Korean
                locale.equals(Locale.JAPAN) && lang.equals(Locale.JAPANESE) ||
                locale.equals(Locale.KOREA) && lang.equals(Locale.KOREAN)) {
                if (isActive) {
                    setNativeLocale(locale.toString(), onActivate);
                }
                return true;
            }
        }
        return false;
    }

    /**
        * Returns the current input locale. Might return null in exceptional cases.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#getLocale InputContext.getLocale} and
     * <li>when switching from this input method to a different one through the
     *     user interface.
     * </ul>
     *
     * @return the current input locale, or null
     */
    public Locale getLocale() {
        // On Mac OS X we'll ask the currently active input method what its locale is.
        Locale returnValue = getNativeLocale();
        if (returnValue == null) {
            returnValue = Locale.getDefault();
        }

        return returnValue;
    }

    /**
        * Sets the subsets of the Unicode character set that this input method
     * is allowed to input. Null may be passed in to indicate that all
     * characters are allowed.
     * <p>
     * This method is called
     * <ul>
     * <li>immediately after instantiating this input method,
     * <li>when switching to this input method from a different one, and
     * <li>by {@link java.awt.im.InputContext#setCharacterSubsets InputContext.setCharacterSubsets}.
     * </ul>
     *
     * @param subsets the subsets of the Unicode character set from which
     * characters may be input
     */
    public void setCharacterSubsets(Subset[] subsets) {
        // -- SAK: Does mac OS X support this?
    }

    /**
        * Composition cannot be set on Mac OS X -- the input method remembers this
     */
    public void setCompositionEnabled(boolean enable) {
        throw new UnsupportedOperationException("Can't adjust composition mode on Mac OS X.");
    }

    public boolean isCompositionEnabled() {
        throw new UnsupportedOperationException("Can't adjust composition mode on Mac OS X.");
    }

    /**
     * Dispatches the event to the input method. If input method support is
     * enabled for the focussed component, incoming events of certain types
     * are dispatched to the current input method for this component before
     * they are dispatched to the component's methods or event listeners.
     * The input method decides whether it needs to handle the event. If it
     * does, it also calls the event's {@code consume} method; this
     * causes the event to not get dispatched to the component's event
     * processing methods or event listeners.
     * <p>
     * Events are dispatched if they are instances of InputEvent or its
     * subclasses.
     * This includes instances of the AWT classes KeyEvent and MouseEvent.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#dispatchEvent InputContext.dispatchEvent}.
     *
     * @param event the event being dispatched to the input method
     * @exception NullPointerException if {@code event} is null
     */
    public void dispatchEvent(final AWTEvent event) {
        // No-op for Mac OS X.
    }


    /**
     * Activate and deactivate are no-ops on Mac OS X.
     * A non-US keyboard layout is an 'input method' in that it generates events the same way as
     * a CJK input method. A component that doesn't want input method events still wants the dead-key
     * events.
     *
     *
     */
    public void activate() {
        isActive = true;
    }

    public void deactivate(boolean isTemporary) {
        isActive = false;
    }

    /**
     * Closes or hides all windows opened by this input method instance or
     * its class.  Deactivate hides windows for us on Mac OS X.
     */
    public void hideWindows() {
    }

    long getNativeViewPtr(LWComponentPeer<?, ?> peer) {
        if (peer.getPlatformWindow() instanceof CPlatformWindow) {
            CPlatformWindow platformWindow = (CPlatformWindow) peer.getPlatformWindow();
            CPlatformView platformView = platformWindow.getContentView();
            return platformView.getAWTView();
        } else {
            return 0;
        }
    }

    /**
        * Notifies the input method that a client component has been
     * removed from its containment hierarchy, or that input method
     * support has been disabled for the component.
     */
    public void removeNotify() {
        if (fAwtFocussedComponentPeer != null) {
            nativeEndComposition(getNativeViewPtr(fAwtFocussedComponentPeer));
        }

        fAwtFocussedComponentPeer = null;
    }

    /**
     * Informs the input method adapter about the component that has the AWT
     * focus if it's using the input context owning this adapter instance.
     * We also take the opportunity to tell the native side that we are the input method
     * to talk to when responding to key events.
     */
    protected void setAWTFocussedComponent(Component component) {
        LWComponentPeer<?, ?> peer = null;
        long modelPtr = 0;
        CInputMethod imInstance = this;

        // component will be null when we are told there's no focused component.
        // When that happens we need to notify the native architecture to stop generating IMEs
        if (component == null) {
            peer = fAwtFocussedComponentPeer;
            imInstance = null;
        } else {
            peer = getNearestNativePeer(component);

            // If we have a passive client, don't pass input method events to it.
            if (component.getInputMethodRequests() == null) {
                imInstance = null;
            }
        }

        if (peer != null) {
            modelPtr = getNativeViewPtr(peer);

            // modelPtr refers to the ControlModel that either got or lost focus.
            nativeNotifyPeer(modelPtr, imInstance);
        }

        // Track the focused component and its nearest peer.
        fAwtFocussedComponent = component;
        fAwtFocussedComponentPeer = getNearestNativePeer(component);
    }

    /**
        * @see java.awt.Toolkit#mapInputMethodHighlight
     */
    public static Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight) {
        int index;
        int state = highlight.getState();
        if (state == InputMethodHighlight.RAW_TEXT) {
            index = 0;
        } else if (state == InputMethodHighlight.CONVERTED_TEXT) {
            index = 2;
        } else {
            return null;
        }
        if (highlight.isSelected()) {
            index += 1;
        }
        return sHighlightStyles[index];
    }

    /**
        * Ends any input composition that may currently be going on in this
     * context. Depending on the platform and possibly user preferences,
     * this may commit or delete uncommitted text. Any changes to the text
     * are communicated to the active component using an input method event.
     *
     * <p>
     * A text editing component may call this in a variety of situations,
     * for example, when the user moves the insertion point within the text
     * (but outside the composed text), or when the component's text is
     * saved to a file or copied to the clipboard.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#endComposition InputContext.endComposition},
     * <li>by {@link java.awt.im.InputContext#dispatchEvent InputContext.dispatchEvent}
     *     when switching to a different client component
     * <li>when switching from this input method to a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod}.
     * </ul>
     */
    public void endComposition() {
        if (fAwtFocussedComponentPeer != null)
            nativeEndComposition(getNativeViewPtr(fAwtFocussedComponentPeer));
    }

    /**
        * Disposes of the input method and releases the resources used by it.
     * In particular, the input method should dispose windows and close files that are no
     * longer needed.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#dispose InputContext.dispose}.
     * <p>
     * The method is only called when the input method is inactive.
     * No method of this interface is called on this instance after dispose.
     */
    public void dispose() {
        fIMContext = null;
        fAwtFocussedComponent = null;
        fAwtFocussedComponentPeer = null;
    }

    /**
        * Returns a control object from this input method, or null. A
     * control object provides methods that control the behavior of the
     * input method or obtain information from the input method. The type
     * of the object is an input method specific class. Clients have to
     * compare the result against known input method control object
     * classes and cast to the appropriate class to invoke the methods
     * provided.
     * <p>
     * This method is called by
     * {@link java.awt.im.InputContext#getInputMethodControlObject InputContext.getInputMethodControlObject}.
     *
     * @return a control object from this input method, or null
     */
    public Object getControlObject() {
        return null;
    }

    // java.awt.Toolkit#getNativeContainer() is not available
    //    from this package
    private LWComponentPeer<?, ?> getNearestNativePeer(Component comp) {
        if (comp==null)
            return null;
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(comp);
        if (peer==null)
            return null;

        while (peer instanceof java.awt.peer.LightweightPeer) {
            comp = comp.getParent();
            if (comp==null)
                return null;
            peer = acc.getPeer(comp);
            if (peer==null)
                return null;
        }

        if (peer instanceof LWComponentPeer)
            return (LWComponentPeer)peer;

        return null;
    }

    // =========================== NSTextInput callbacks ===========================
    // The 'marked text' that we get from Cocoa.  We need to track this separately, since
    // Java doesn't let us ask the IM context for it.
    private AttributedString fCurrentText = null;
    private String fCurrentTextAsString = null;
    private int fCurrentTextLength = 0;

    /**
     * Tell the component to commit all of the characters in the string to the current
     * text view. This effectively wipes out any text in progress.
     */
    private synchronized void insertText(String aString) {
        AttributedString attribString = new AttributedString(aString);

        // Set locale information on the new string.
        attribString.addAttribute(Attribute.LANGUAGE, getLocale(), 0, aString.length());

        TextHitInfo theCaret = TextHitInfo.afterOffset(aString.length() - 1);
        InputMethodEvent event = new InputMethodEvent(fAwtFocussedComponent,
                                                      InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                                                      attribString.getIterator(),
                                                      aString.length(),
                                                      theCaret,
                                                      theCaret);
        LWCToolkit.postEvent(LWCToolkit.targetToAppContext(fAwtFocussedComponent), event);
        fCurrentText = null;
        fCurrentTextAsString = null;
        fCurrentTextLength = 0;
    }

    private void startIMUpdate (String rawText) {
        fCurrentTextAsString = new String(rawText);
        fCurrentText = new AttributedString(fCurrentTextAsString);
        fCurrentTextLength = rawText.length();
    }

    private static final int kCaretPosition = 0;
    private static final int kRawText = 1;
    private static final int kSelectedRawText = 2;
    private static final int kConvertedText = 3;
    private static final int kSelectedConvertedText = 4;

    /**
     * Convert Cocoa text highlight attributes into Java input method highlighting.
     */
    private void addAttribute (boolean isThickUnderline, boolean isGray, int start, int length) {
        int begin = start;
        int end = start + length;
        int markupType = kRawText;

        if (isThickUnderline && isGray) {
            markupType = kRawText;
        } else if (!isThickUnderline && isGray) {
            markupType = kRawText;
        } else if (isThickUnderline && !isGray) {
            markupType = kSelectedConvertedText;
        } else if (!isThickUnderline && !isGray) {
            markupType = kConvertedText;
        }

        InputMethodHighlight theHighlight;

        switch (markupType) {
            case kSelectedRawText:
                theHighlight = InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT;
                break;
            case kConvertedText:
                theHighlight = InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT;
                break;
            case kSelectedConvertedText:
                theHighlight = InputMethodHighlight.SELECTED_CONVERTED_TEXT_HIGHLIGHT;
                break;
            case kRawText:
            default:
                theHighlight = InputMethodHighlight.UNSELECTED_RAW_TEXT_HIGHLIGHT;
                break;
        }

        fCurrentText.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT, theHighlight, begin, end);
    }

   /* Called from JNI to select the previously typed glyph during press and hold */
    private void selectPreviousGlyph() {
        if (fIMContext == null) return; // ???
        try {
            LWCToolkit.invokeLater(new Runnable() {
                public void run() {
                    final int offset = fIMContext.getInsertPositionOffset();
                    if (offset < 1) return; // ???

                    if (fAwtFocussedComponent instanceof JTextComponent) {
                        ((JTextComponent) fAwtFocussedComponent).select(offset - 1, offset);
                        return;
                    }

                    if (fAwtFocussedComponent instanceof TextComponent) {
                        ((TextComponent) fAwtFocussedComponent).select(offset - 1, offset);
                        return;
                    }
                    // TODO: Ideally we want to disable press-and-hold in this case
                }
            }, fAwtFocussedComponent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectNextGlyph() {
        if (fIMContext == null || !(fAwtFocussedComponent instanceof JTextComponent)) return;
        try {
            LWCToolkit.invokeLater(new Runnable() {
                public void run() {
                    final int offset = fIMContext.getInsertPositionOffset();
                    if (offset < 0) return;
                    ((JTextComponent) fAwtFocussedComponent).select(offset, offset + 1);
                    return;
                }
            }, fAwtFocussedComponent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dispatchText(int selectStart, int selectLength, boolean pressAndHold) {
        // Nothing to do if we have no text.
        if (fCurrentText == null)
            return;

        TextHitInfo theCaret = (selectLength == 0 ? TextHitInfo.beforeOffset(selectStart) : null);
        TextHitInfo visiblePosition = TextHitInfo.beforeOffset(0);

        InputMethodEvent event = new InputMethodEvent(fAwtFocussedComponent,
                                                      InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                                                      fCurrentText.getIterator(),
                                                      0,
                                                      theCaret,
                                                      visiblePosition);
        LWCToolkit.postEvent(LWCToolkit.targetToAppContext(fAwtFocussedComponent), event);

        if (pressAndHold) selectNextGlyph();
    }

    /**
     * Frequent callbacks from NSTextInput.  I think we're supposed to commit it here?
     */
    private synchronized void unmarkText() {
        if (fCurrentText == null)
            return;

        TextHitInfo theCaret = TextHitInfo.afterOffset(fCurrentTextLength);
        TextHitInfo visiblePosition = theCaret;
        InputMethodEvent event = new InputMethodEvent(fAwtFocussedComponent,
                                                      InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                                                      fCurrentText.getIterator(),
                                                      fCurrentTextLength,
                                                      theCaret,
                                                      visiblePosition);
        LWCToolkit.postEvent(LWCToolkit.targetToAppContext(fAwtFocussedComponent), event);
        fCurrentText = null;
        fCurrentTextAsString = null;
        fCurrentTextLength = 0;
    }

    private synchronized boolean hasMarkedText() {
        return fCurrentText != null;
    }

    /**
        * Cocoa assumes the marked text and committed text is all stored in the same storage, but
     * Java does not.  So, we have to see where the request is and based on that return the right
     * substring.
     */
    private synchronized String attributedSubstringFromRange(final int locationIn, final int lengthIn) {
        final String[] retString = new String[1];

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() { synchronized(retString) {
                    int location = locationIn;
                    int length = lengthIn;

                    if ((location + length) > (fIMContext.getCommittedTextLength() + fCurrentTextLength)) {
                        length = fIMContext.getCommittedTextLength() - location;
                    }

                    AttributedCharacterIterator theIterator = null;

                    if (fCurrentText == null) {
                        theIterator = fIMContext.getCommittedText(location, location + length, null);
                    } else {
                        int insertSpot = fIMContext.getInsertPositionOffset();

                        if (location < insertSpot) {
                            theIterator = fIMContext.getCommittedText(location, location + length, null);
                        } else if (location >= insertSpot && location < insertSpot + fCurrentTextLength) {
                            theIterator = fCurrentText.getIterator(null, location - insertSpot, location - insertSpot +length);
                        } else  {
                            theIterator = fIMContext.getCommittedText(location - fCurrentTextLength, location - fCurrentTextLength + length, null);
                        }
                    }

                    // Get the characters from the iterator
                    char[] selectedText = new char[theIterator.getEndIndex() - theIterator.getBeginIndex()];
                    char current = theIterator.first();
                    int index = 0;
                    while (current != CharacterIterator.DONE) {
                        selectedText[index++] = current;
                        current = theIterator.next();
                    }

                    retString[0] = new String(selectedText);
                }}
            }, fAwtFocussedComponent);
        } catch (InvocationTargetException ite) { ite.printStackTrace(); }

        synchronized(retString) { return retString[0]; }
    }

    /**
     * Cocoa wants the range of characters that are currently selected.  We have to synthesize this
     * by getting the insert location and the length of the selected text. NB:  This does NOT allow
     * for the fact that the insert point in Swing can come AFTER the selected text, making this
     * potentially incorrect.
     */
    private synchronized int[] selectedRange() {
        final int[] returnValue = new int[2];

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() { synchronized(returnValue) {
                    AttributedCharacterIterator theIterator = fIMContext.getSelectedText(null);
                    if (theIterator == null) {
                        returnValue[0] = fIMContext.getInsertPositionOffset();
                        returnValue[1] = 0;
                        return;
                    }

                    int startLocation;

                    if (fAwtFocussedComponent instanceof JTextComponent) {
                        JTextComponent theComponent = (JTextComponent)fAwtFocussedComponent;
                        startLocation = theComponent.getSelectionStart();
                    } else if (fAwtFocussedComponent instanceof TextComponent) {
                        TextComponent theComponent = (TextComponent)fAwtFocussedComponent;
                        startLocation = theComponent.getSelectionStart();
                    } else {
                        // If we don't have a Swing or AWT component, we have to guess whether the selection is before or after the input spot.
                        startLocation = fIMContext.getInsertPositionOffset() - (theIterator.getEndIndex() - theIterator.getBeginIndex());

                        // If the calculated spot is negative the insert spot must be at the beginning of
                        // the selection.
                        if (startLocation <  0) {
                            startLocation = fIMContext.getInsertPositionOffset() + (theIterator.getEndIndex() - theIterator.getBeginIndex());
                        }
                    }

                    returnValue[0] = startLocation;
                    returnValue[1] = theIterator.getEndIndex() - theIterator.getBeginIndex();

                }}
            }, fAwtFocussedComponent);
        } catch (InvocationTargetException ite) { ite.printStackTrace(); }

        synchronized(returnValue) { return returnValue; }
    }

    /**
     * Cocoa wants the range of characters that are currently marked.  Since Java doesn't store committed and
     * text in progress (composed text) together, we have to synthesize it.  We know where the text will be
     * inserted, so we can return that position, and the length of the text in progress.  If there is no marked text
     * return null.
     */
    private synchronized int[] markedRange() {
        if (fCurrentText == null)
            return null;

        final int[] returnValue = new int[2];

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() { synchronized(returnValue) {
                    // The insert position is always after the composed text, so the range start is the
                    // insert spot less the length of the composed text.
                    returnValue[0] = fIMContext.getInsertPositionOffset();
                }}
            }, fAwtFocussedComponent);
        } catch (InvocationTargetException ite) { ite.printStackTrace(); }

        returnValue[1] = fCurrentTextLength;
        synchronized(returnValue) { return returnValue; }
    }

    /**
     * Cocoa wants a rectangle that describes where a particular range is on screen, but only cares about the
     * location of that rectangle.  We are given the index of the character for which we want the location on
     * screen, which will be a character in the in-progress text.  By subtracting the current insert position,
     * which is always in front of the in-progress text, we get the offset into the composed text, and we get
     * that location from the input method context.
     */
    private synchronized int[] firstRectForCharacterRange(final int absoluteTextOffset) {
        final int[] rect = new int[4];

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() { synchronized(rect) {
                    int insertOffset = fIMContext.getInsertPositionOffset();
                    int composedTextOffset = absoluteTextOffset - insertOffset;
                    if (composedTextOffset < 0) composedTextOffset = 0;
                    Rectangle r = fIMContext.getTextLocation(TextHitInfo.beforeOffset(composedTextOffset));
                    rect[0] = r.x;
                    rect[1] = r.y;
                    rect[2] = r.width;
                    rect[3] = r.height;

                    // This next if-block is a hack to work around a bug in JTextComponent. getTextLocation ignores
                    // the TextHitInfo passed to it and always returns the location of the insertion point, which is
                    // at the start of the composed text.  We'll do some calculation so the candidate window for Kotoeri
                    // follows the requested offset into the composed text.
                    if (composedTextOffset > 0 && (fAwtFocussedComponent instanceof JTextComponent)) {
                        Rectangle r2 = fIMContext.getTextLocation(TextHitInfo.beforeOffset(0));

                        if (r.equals(r2)) {
                            // FIXME: (SAK) If the candidate text wraps over two lines, this calculation pushes the candidate
                            // window off the right edge of the component.
                            String inProgressSubstring = fCurrentTextAsString.substring(0, composedTextOffset);
                            Graphics g = fAwtFocussedComponent.getGraphics();
                            int xOffset = g.getFontMetrics().stringWidth(inProgressSubstring);
                            rect[0] += xOffset;
                            g.dispose();
                        }
                    }
                }}
            }, fAwtFocussedComponent);
        } catch (InvocationTargetException ite) { ite.printStackTrace(); }

        synchronized(rect) { return rect; }
    }

    /* This method returns the index for the character that is nearest to the point described by screenX and screenY.
     * The coordinates are in Java screen coordinates.  If no character in the composed text was hit, we return -1, indicating
     * not found.
     */
    private synchronized int characterIndexForPoint(final int screenX, final int screenY) {
        final TextHitInfo[] offsetInfo = new TextHitInfo[1];
        final int[] insertPositionOffset = new int[1];

        try {
            LWCToolkit.invokeAndWait(new Runnable() {
                public void run() { synchronized(offsetInfo) {
                    offsetInfo[0] = fIMContext.getLocationOffset(screenX, screenY);
                    insertPositionOffset[0] = fIMContext.getInsertPositionOffset();
                }}
            }, fAwtFocussedComponent);
        } catch (InvocationTargetException ite) { ite.printStackTrace(); }

        // This bit of gymnastics ensures that the returned location is within the composed text.
        // If it falls outside that region, the input method will commit the text, which is inconsistent with native
        // Cocoa apps (see TextEdit, for example.)  Clicking to the left of or above the selected text moves the
        // cursor to the start of the composed text, and to the right or below moves it to one character before the end.
        if (offsetInfo[0] == null) {
            return insertPositionOffset[0];
        }

        int returnValue = offsetInfo[0].getCharIndex() + insertPositionOffset[0];

        if (offsetInfo[0].getCharIndex() == fCurrentTextLength)
            returnValue --;

        return returnValue;
    }

    // On Mac OS X we effectively disabled the input method when focus was lost, so
    // this call can be ignored.
    public void disableInputMethod()
    {
        // Deliberately ignored. See setAWTFocussedComponent above.
    }

    public String getNativeInputMethodInfo()
    {
        return nativeGetCurrentInputMethodInfo();
    }


    // =========================== Native methods ===========================
    // Note that if nativePeer isn't something that normally accepts keystrokes (i.e., a CPanel)
    // these calls will be ignored.
    private native void nativeNotifyPeer(long nativePeer, CInputMethod imInstance);
    private native void nativeEndComposition(long nativePeer);
    private native void nativeHandleEvent(LWComponentPeer<?, ?> peer, AWTEvent event);

    // Returns the locale of the active input method.
    static native Locale getNativeLocale();

    // Switches to the input method with language indicated in localeName
    static native boolean setNativeLocale(String localeName, boolean onActivate);

    // Returns information about the currently selected input method.
    static native String nativeGetCurrentInputMethodInfo();

    // Initialize toolbox routines
    static native void nativeInit();
}
