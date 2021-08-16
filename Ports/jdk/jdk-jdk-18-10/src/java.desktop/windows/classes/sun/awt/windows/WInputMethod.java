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


package sun.awt.windows;

import java.awt.*;
import java.awt.peer.*;
import java.awt.event.*;
import java.awt.im.*;
import java.awt.im.spi.InputMethodContext;
import java.awt.font.*;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.lang.Character.Subset;
import java.lang.Character.UnicodeBlock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.im.InputMethodAdapter;

final class WInputMethod extends InputMethodAdapter
{
    /**
     * The input method context, which is used to dispatch input method
     * events to the client component and to request information from
     * the client component.
     */
    private InputMethodContext inputContext;

    private Component awtFocussedComponent;
    private WComponentPeer awtFocussedComponentPeer = null;
    private WComponentPeer lastFocussedComponentPeer = null;
    private boolean isLastFocussedActiveClient = false;
    private boolean isActive;
    private int context;
    private boolean open; //default open status;
    private int cmode;    //default conversion mode;
    private Locale currentLocale;
    // indicate whether status window is hidden or not.
    private boolean statusWindowHidden = false;
    private boolean hasCompositionString = false;

    // attribute definition in Win32 (in IMM.H)
    public static final byte ATTR_INPUT                 = 0x00;
    public static final byte ATTR_TARGET_CONVERTED      = 0x01;
    public static final byte ATTR_CONVERTED             = 0x02;
    public static final byte ATTR_TARGET_NOTCONVERTED   = 0x03;
    public static final byte ATTR_INPUT_ERROR           = 0x04;
    // cmode definition in Win32 (in IMM.H)
    public static final int  IME_CMODE_ALPHANUMERIC     = 0x0000;
    public static final int  IME_CMODE_NATIVE           = 0x0001;
    public static final int  IME_CMODE_KATAKANA         = 0x0002;
    public static final int  IME_CMODE_LANGUAGE         = 0x0003;
    public static final int  IME_CMODE_FULLSHAPE        = 0x0008;
    public static final int  IME_CMODE_HANJACONVERT     = 0x0040;
    public static final int  IME_CMODE_ROMAN            = 0x0010;

    // flag values for endCompositionNative() behavior
    private static final boolean COMMIT_INPUT           = true;
    private static final boolean DISCARD_INPUT          = false;

    private static Map<TextAttribute,Object> [] highlightStyles;

    // Initialize highlight mapping table
    static {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Map<TextAttribute,Object>[] styles = new Map[4];
        HashMap<TextAttribute,Object> map;

        // UNSELECTED_RAW_TEXT_HIGHLIGHT
        map = new HashMap<>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
        styles[0] = Collections.unmodifiableMap(map);

        // SELECTED_RAW_TEXT_HIGHLIGHT
        map = new HashMap<>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_GRAY);
        styles[1] = Collections.unmodifiableMap(map);

        // UNSELECTED_CONVERTED_TEXT_HIGHLIGHT
        map = new HashMap<>(1);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
        styles[2] = Collections.unmodifiableMap(map);

        // SELECTED_CONVERTED_TEXT_HIGHLIGHT
        map = new HashMap<>(4);
        Color navyBlue = new Color(0, 0, 128);
        map.put(TextAttribute.FOREGROUND, navyBlue);
        map.put(TextAttribute.BACKGROUND, Color.white);
        map.put(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON);
        map.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
        styles[3] = Collections.unmodifiableMap(map);

        highlightStyles = styles;
    }

    public WInputMethod()
    {
        context = createNativeContext();
        cmode = getConversionStatus(context);
        open = getOpenStatus(context);
        currentLocale = getNativeLocale();
        if (currentLocale == null) {
            currentLocale = Locale.getDefault();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable
    {
        // Release the resources used by the native input context.
        if (context!=0) {
            destroyNativeContext(context);
            context=0;
        }
        super.finalize();
    }

    @Override
    public synchronized void setInputMethodContext(InputMethodContext context) {
        inputContext = context;
    }

    @Override
    public void dispose() {
        // Due to a memory management problem in Windows 98, we should retain
        // the native input context until this object is finalized. So do
        // nothing here.
    }

    /**
     * Returns null.
     *
     * @see java.awt.im.spi.InputMethod#getControlObject
     */
    @Override
    public Object getControlObject() {
        return null;
    }

    @Override
    public boolean setLocale(Locale lang) {
        return setLocale(lang, false);
    }

    private boolean setLocale(Locale lang, boolean onActivate) {
        Locale[] available = WInputMethodDescriptor.getAvailableLocalesInternal();
        for (int i = 0; i < available.length; i++) {
            Locale locale = available[i];
            if (lang.equals(locale) ||
                    // special compatibility rule for Japanese and Korean
                    locale.equals(Locale.JAPAN) && lang.equals(Locale.JAPANESE) ||
                    locale.equals(Locale.KOREA) && lang.equals(Locale.KOREAN)) {
                if (isActive) {
                    setNativeLocale(locale.toLanguageTag(), onActivate);
                }
                currentLocale = locale;
                return true;
            }
        }
        return false;
    }

    @Override
    public Locale getLocale() {
        if (isActive) {
            currentLocale = getNativeLocale();
            if (currentLocale == null) {
                currentLocale = Locale.getDefault();
            }
        }
        return currentLocale;
    }

    /**
     * Implements InputMethod.setCharacterSubsets for Windows.
     *
     * @see java.awt.im.spi.InputMethod#setCharacterSubsets
     */
    @Override
    public void setCharacterSubsets(Subset[] subsets) {
        if (subsets == null){
            setConversionStatus(context, cmode);
            setOpenStatus(context, open);
            return;
        }

        // Use first subset only. Other subsets in array is ignored.
        // This is restriction of Win32 implementation.
        Subset subset1 = subsets[0];

        Locale locale = getNativeLocale();
        int newmode;

        if (locale == null) {
            return;
        }

        if (locale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
            if (subset1 == UnicodeBlock.BASIC_LATIN || subset1 == InputSubset.LATIN_DIGITS) {
                setOpenStatus(context, false);
            } else {
                if (subset1 == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || subset1 == InputSubset.KANJI
                    || subset1 == UnicodeBlock.HIRAGANA)
                    newmode = IME_CMODE_NATIVE | IME_CMODE_FULLSHAPE;
                else if (subset1 == UnicodeBlock.KATAKANA)
                    newmode = IME_CMODE_NATIVE | IME_CMODE_KATAKANA| IME_CMODE_FULLSHAPE;
                else if (subset1 == InputSubset.HALFWIDTH_KATAKANA)
                    newmode = IME_CMODE_NATIVE | IME_CMODE_KATAKANA;
                else if (subset1 == InputSubset.FULLWIDTH_LATIN)
                    newmode = IME_CMODE_FULLSHAPE;
                else
                    return;
                setOpenStatus(context, true);
                newmode |= (getConversionStatus(context)&IME_CMODE_ROMAN);   // reserve ROMAN input mode
                setConversionStatus(context, newmode);
            }
        } else if (locale.getLanguage().equals(Locale.KOREAN.getLanguage())) {
            if (subset1 == UnicodeBlock.BASIC_LATIN || subset1 == InputSubset.LATIN_DIGITS) {
                setOpenStatus(context, false);
                setConversionStatus(context, IME_CMODE_ALPHANUMERIC);
            } else {
                if (subset1 == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || subset1 == InputSubset.HANJA
                    || subset1 == UnicodeBlock.HANGUL_SYLLABLES
                    || subset1 == UnicodeBlock.HANGUL_JAMO
                    || subset1 == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO)
                    newmode = IME_CMODE_NATIVE;
                else if (subset1 == InputSubset.FULLWIDTH_LATIN)
                    newmode = IME_CMODE_FULLSHAPE;
                else
                    return;
                setOpenStatus(context, true);
                setConversionStatus(context, newmode);
            }
        } else if (locale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
            if (subset1 == UnicodeBlock.BASIC_LATIN || subset1 == InputSubset.LATIN_DIGITS) {
                setOpenStatus(context, false);
                newmode = getConversionStatus(context);
                newmode &= ~IME_CMODE_FULLSHAPE;
                setConversionStatus(context, newmode);
            } else {
                if (subset1 == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || subset1 == InputSubset.TRADITIONAL_HANZI
                    || subset1 == InputSubset.SIMPLIFIED_HANZI)
                    newmode = IME_CMODE_NATIVE | IME_CMODE_FULLSHAPE;
                else if (subset1 == InputSubset.FULLWIDTH_LATIN)
                    newmode = IME_CMODE_FULLSHAPE;
                else
                    return;
                setOpenStatus(context, true);
                setConversionStatus(context, newmode);
            }
        }
    }

    @Override
    public void dispatchEvent(AWTEvent e) {
        if (e instanceof ComponentEvent) {
            Component comp = ((ComponentEvent) e).getComponent();
            if (comp == awtFocussedComponent) {
                if (awtFocussedComponentPeer == null ||
                    awtFocussedComponentPeer.isDisposed()) {
                    awtFocussedComponentPeer = getNearestNativePeer(comp);
                }
                if (awtFocussedComponentPeer != null) {
                    handleNativeIMEEvent(awtFocussedComponentPeer, e);
                }
            }
        }
    }

    @Override
    public void activate() {
        boolean isAc = haveActiveClient();

        // When the last focussed component peer is different from the
        // current focussed component or if they are different client
        // (active or passive), disable native IME for the old focussed
        // component and enable for the new one.
        if (lastFocussedComponentPeer != awtFocussedComponentPeer ||
            isLastFocussedActiveClient != isAc) {
            if (lastFocussedComponentPeer != null) {
                disableNativeIME(lastFocussedComponentPeer);
            }
            if (awtFocussedComponentPeer != null) {
                enableNativeIME(awtFocussedComponentPeer, context, !isAc);
            }
            lastFocussedComponentPeer = awtFocussedComponentPeer;
            isLastFocussedActiveClient = isAc;
        }
        isActive = true;
        if (currentLocale != null) {
            setLocale(currentLocale, true);
        }

        // Compare IM's composition string with Java's composition string
        if (hasCompositionString && !isCompositionStringAvailable(context)) {
            endCompositionNative(context, DISCARD_INPUT);
            sendInputMethodEvent(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                EventQueue.getMostRecentEventTime(),
                null, null, null, null, null, 0, 0, 0);
            hasCompositionString = false;
        }

        /* If the status window or Windows language bar is turned off due to
           native input method was switched to java input method, we
           have to turn it on otherwise it is gone for good until next time
           the user turns it on through Windows Control Panel. See details
           from bug 6252674.
        */
        if (statusWindowHidden) {
            setStatusWindowVisible(awtFocussedComponentPeer, true);
            statusWindowHidden = false;
        }

    }

    @Override
    public void deactivate(boolean isTemporary)
    {
        // Sync currentLocale with the Windows keyboard layout which might be changed
        // by hot key
        getLocale();

        // Delay calling disableNativeIME until activate is called and the newly
        // focussed component has a different peer as the last focussed component.
        if (awtFocussedComponentPeer != null) {
            lastFocussedComponentPeer = awtFocussedComponentPeer;
            isLastFocussedActiveClient = haveActiveClient();
        }
        isActive = false;
        hasCompositionString = isCompositionStringAvailable(context);

        // IME is going to be disabled commit the composition string
        if (hasCompositionString) {
            endComposition();
        }
    }

    /**
     * Explicitly disable the native IME. Native IME is not disabled when
     * deactivate is called.
     */
    @Override
    public void disableInputMethod() {
        if (lastFocussedComponentPeer != null) {
            disableNativeIME(lastFocussedComponentPeer);
            lastFocussedComponentPeer = null;
            isLastFocussedActiveClient = false;
        }
    }

    /**
     * Returns a string with information about the windows input method,
     * or null.
     */
    @Override
    public String getNativeInputMethodInfo() {
        return getNativeIMMDescription();
    }

     /**
     * @see sun.awt.im.InputMethodAdapter#stopListening
     * This method is called when the input method is swapped out.
     * Calling stopListening to give other input method the keybaord input
     * focus.
     */
    @Override
    protected void stopListening() {
        // Since the native input method is not disabled when deactivate is
        // called, we need to call disableInputMethod to explicitly turn off the
        // native IME.
        disableInputMethod();
    }

    // implements sun.awt.im.InputMethodAdapter.setAWTFocussedComponent
    @Override
    protected void setAWTFocussedComponent(Component component) {
        if (component == null) {
            return;
        }
        WComponentPeer peer = getNearestNativePeer(component);
        if (isActive) {
            // deactivate/activate are being suppressed during a focus change -
            // this may happen when an input method window is made visible
            if (awtFocussedComponentPeer != null) {
                disableNativeIME(awtFocussedComponentPeer);
            }
            if (peer != null) {
                enableNativeIME(peer, context, !haveActiveClient());
            }
        }
        awtFocussedComponent = component;
        awtFocussedComponentPeer = peer;
    }

    // implements java.awt.im.spi.InputMethod.hideWindows
    @Override
    public void hideWindows() {
        if (awtFocussedComponentPeer != null) {
            /* Hide the native status window including the Windows language
               bar if it is on. One typical senario this method
               gets called is when the native input method is
               switched to java input method, for example.
            */
            setStatusWindowVisible(awtFocussedComponentPeer, false);
            statusWindowHidden = true;
        }
    }

    /**
     * @see java.awt.im.spi.InputMethod#removeNotify
     */
    @Override
    public void removeNotify() {
        endCompositionNative(context, DISCARD_INPUT);
        awtFocussedComponent = null;
        awtFocussedComponentPeer = null;
    }

    /**
     * @see java.awt.Toolkit#mapInputMethodHighlight
     */
    static Map<TextAttribute,?> mapInputMethodHighlight(InputMethodHighlight highlight) {
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
        return highlightStyles[index];
    }

    // see sun.awt.im.InputMethodAdapter.supportsBelowTheSpot
    @Override
    protected boolean supportsBelowTheSpot() {
        return true;
    }

    @Override
    public void endComposition()
    {
        //right now the native endCompositionNative() just cancel
        //the composition string, maybe a commtting is desired
        endCompositionNative(context,
            (haveActiveClient() ? COMMIT_INPUT : DISCARD_INPUT));
    }

    /**
     * @see java.awt.im.spi.InputMethod#setCompositionEnabled(boolean)
     */
    @Override
    public void setCompositionEnabled(boolean enable) {
        setOpenStatus(context, enable);
    }

    /**
     * @see java.awt.im.spi.InputMethod#isCompositionEnabled
     */
    @Override
    public boolean isCompositionEnabled() {
        return getOpenStatus(context);
    }

    public void sendInputMethodEvent(int id, long when, String text,
                                     int[] clauseBoundary, String[] clauseReading,
                                     int[] attributeBoundary, byte[] attributeValue,
                                     int commitedTextLength, int caretPos, int visiblePos)
    {

        AttributedCharacterIterator iterator = null;

        if (text!=null) {

            // construct AttributedString
            AttributedString attrStr = new AttributedString(text);

            // set Language Information
            attrStr.addAttribute(Attribute.LANGUAGE,
                                            Locale.getDefault(), 0, text.length());

            // set Clause and Reading Information
            if (clauseBoundary!=null && clauseReading!=null &&
                clauseReading.length!=0 && clauseBoundary.length==clauseReading.length+1 &&
                clauseBoundary[0]==0 && clauseBoundary[clauseReading.length]<=text.length() )
            {
                for (int i=0; i<clauseBoundary.length-1; i++) {
                    attrStr.addAttribute(Attribute.INPUT_METHOD_SEGMENT,
                                            new Annotation(null), clauseBoundary[i], clauseBoundary[i+1]);
                    attrStr.addAttribute(Attribute.READING,
                                            new Annotation(clauseReading[i]), clauseBoundary[i], clauseBoundary[i+1]);
                }
            } else {
                // if (clauseBoundary != null)
                //    System.out.println("Invalid clause information!");

                attrStr.addAttribute(Attribute.INPUT_METHOD_SEGMENT,
                                        new Annotation(null), 0, text.length());
                attrStr.addAttribute(Attribute.READING,
                                     new Annotation(""), 0, text.length());
            }

            // set Hilight Information
            if (attributeBoundary!=null && attributeValue!=null &&
                attributeValue.length!=0 && attributeBoundary.length==attributeValue.length+1 &&
                attributeBoundary[0]==0 && attributeBoundary[attributeValue.length]==text.length() )
            {
                for (int i=0; i<attributeBoundary.length-1; i++) {
                    InputMethodHighlight highlight;
                    switch (attributeValue[i]) {
                        case ATTR_TARGET_CONVERTED:
                            highlight = InputMethodHighlight.SELECTED_CONVERTED_TEXT_HIGHLIGHT;
                            break;
                        case ATTR_CONVERTED:
                            highlight = InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT;
                            break;
                        case ATTR_TARGET_NOTCONVERTED:
                            highlight = InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT;
                            break;
                        case ATTR_INPUT:
                        case ATTR_INPUT_ERROR:
                        default:
                            highlight = InputMethodHighlight.UNSELECTED_RAW_TEXT_HIGHLIGHT;
                            break;
                    }
                    attrStr.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT,
                                         highlight,
                                         attributeBoundary[i], attributeBoundary[i+1]);
                }
            } else {
                // if (attributeBoundary != null)
                //    System.out.println("Invalid attribute information!");

                attrStr.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT,
                             InputMethodHighlight.UNSELECTED_CONVERTED_TEXT_HIGHLIGHT,
                             0, text.length());
            }

            // get iterator
            iterator = attrStr.getIterator();

        }

        Component source = getClientComponent();
        if (source == null)
            return;

        InputMethodEvent event = new InputMethodEvent(source,
                                                      id,
                                                      when,
                                                      iterator,
                                                      commitedTextLength,
                                                      TextHitInfo.leading(caretPos),
                                                      TextHitInfo.leading(visiblePos));
        WToolkit.postEvent(WToolkit.targetToAppContext(source), event);
    }

    public void inquireCandidatePosition()
    {
        Component source = getClientComponent();
        if (source == null) {
            return;
        }
        // This call should return immediately just to cause
        // InputMethodRequests.getTextLocation be called within
        // AWT Event thread.  Otherwise, a potential deadlock
        // could happen.
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int x = 0;
                int y = 0;
                Component client = getClientComponent();

                if (client != null) {
                    if (!client.isShowing()) {
                        return;
                    }
                    if (haveActiveClient()) {
                            Rectangle rc = inputContext.getTextLocation(TextHitInfo.leading(0));
                            x = rc.x;
                            y = rc.y + rc.height;
                    } else {
                            Point pt = client.getLocationOnScreen();
                            Dimension size = client.getSize();
                            x = pt.x;
                            y = pt.y + size.height;
                    }
                }

                openCandidateWindow(awtFocussedComponentPeer, x, y);
            }
        };
        WToolkit.postEvent(WToolkit.targetToAppContext(source),
                           new InvocationEvent(source, r));
    }

    // java.awt.Toolkit#getNativeContainer() is not available
    //  from this package
    private WComponentPeer getNearestNativePeer(Component comp)
    {
        if (comp==null)     return null;
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(comp);
        if (peer==null)     return null;

        while (peer instanceof java.awt.peer.LightweightPeer) {
            comp = comp.getParent();
            if (comp==null) return null;
            peer = acc.getPeer(comp);
            if (peer==null) return null;
        }

        if (peer instanceof WComponentPeer)
            return (WComponentPeer)peer;
        else
            return null;

    }

    private native int createNativeContext();
    private native void destroyNativeContext(int context);
    private native void enableNativeIME(WComponentPeer peer, int context, boolean useNativeCompWindow);
    private native void disableNativeIME(WComponentPeer peer);
    private native void handleNativeIMEEvent(WComponentPeer peer, AWTEvent e);
    private native void endCompositionNative(int context, boolean flag);
    private native void setConversionStatus(int context, int cmode);
    private native int  getConversionStatus(int context);
    private native void setOpenStatus(int context, boolean flag);
    private native boolean getOpenStatus(int context);
    private native void setStatusWindowVisible(WComponentPeer peer, boolean visible);
    private native String getNativeIMMDescription();
    static native Locale getNativeLocale();
    static native boolean setNativeLocale(String localeName, boolean onActivate);
    private native void openCandidateWindow(WComponentPeer peer, int x, int y);
    private native boolean isCompositionStringAvailable(int context);
}
