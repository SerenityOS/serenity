/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */

package com.sun.inputmethods.internal.codepointim;


import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodContext;
import java.io.IOException;
import java.text.AttributedString;
import java.util.Locale;


/**
 * The Code Point Input Method is a simple input method that allows Unicode
 * characters to be entered using their code point or code unit values. See the
 * accompanying file README.txt for more information.
 *
 * @author Brian Beck
 */
public class CodePointInputMethod implements InputMethod {

    private static final int UNSET = 0;
    private static final int ESCAPE = 1; // \u0000       - \uFFFF
    private static final int SPECIAL_ESCAPE = 2; // \U000000     - \U10FFFF
    private static final int SURROGATE_PAIR = 3; // \uD800\uDC00 - \uDBFF\uDFFF
    private InputMethodContext context;
    private Locale locale;
    private StringBuffer buffer;
    private int insertionPoint;
    private int format = UNSET;

    public CodePointInputMethod() throws IOException {
    }

    /**
     * This is the input method's main routine.  The composed text is stored
     * in buffer.
     */
    public void dispatchEvent(AWTEvent event) {
        // This input method handles KeyEvent only.
        if (!(event instanceof KeyEvent)) {
            return;
        }

        KeyEvent e = (KeyEvent) event;
        int eventID = event.getID();
        boolean notInCompositionMode = buffer.length() == 0;

        if (eventID == KeyEvent.KEY_PRESSED) {
            // If we are not in composition mode, pass through
            if (notInCompositionMode) {
                return;
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    moveCaretLeft();
                    break;
                case KeyEvent.VK_RIGHT:
                    moveCaretRight();
                    break;
            }
        } else if (eventID == KeyEvent.KEY_TYPED) {
            char c = e.getKeyChar();

            // If we are not in composition mode, wait a back slash
            if (notInCompositionMode) {
                // If the type character is not a back slash, pass through
                if (c != '\\') {
                    return;
                }

                startComposition();     // Enter to composition mode
            } else {
                switch (c) {
                    case ' ':       // Exit from composition mode
                        finishComposition();
                        break;
                    case '\u007f':  // Delete
                        deleteCharacter();
                        break;
                    case '\b':      // BackSpace
                        deletePreviousCharacter();
                        break;
                    case '\u001b':  // Escape
                        cancelComposition();
                        break;
                    case '\n':      // Return
                    case '\t':      // Tab
                        sendCommittedText();
                        break;
                    default:
                        composeUnicodeEscape(c);
                        break;
                }
            }
        } else {  // KeyEvent.KEY_RELEASED
            // If we are not in composition mode, pass through
            if (notInCompositionMode) {
                return;
            }
        }

        e.consume();
    }

    private void composeUnicodeEscape(char c) {
        switch (buffer.length()) {
            case 1:  // \\
                waitEscapeCharacter(c);
                break;
            case 2:  // \\u or \\U
            case 3:  // \\ux or \\Ux
            case 4:  // \\uxx or \\Uxx
                waitDigit(c);
                break;
            case 5:  // \\uxxx or \\Uxxx
                if (format == SPECIAL_ESCAPE) {
                    waitDigit(c);
                } else {
                    waitDigit2(c);
                }
                break;
            case 6:  // \\uxxxx or \\Uxxxx
                if (format == SPECIAL_ESCAPE) {
                    waitDigit(c);
                } else if (format == SURROGATE_PAIR) {
                    waitBackSlashOrLowSurrogate(c);
                } else {
                    beep();
                }
                break;
            case 7:  // \\Uxxxxx
                // Only SPECIAL_ESCAPE format uses this state.
                // Since the second "\\u" of SURROGATE_PAIR format is inserted
                // automatically, users don't have to type these keys.
                waitDigit(c);
                break;
            case 8:  // \\uxxxx\\u
            case 9:  // \\uxxxx\\ux
            case 10: // \\uxxxx\\uxx
            case 11: // \\uxxxx\\uxxx
                if (format == SURROGATE_PAIR) {
                    waitDigit(c);
                } else {
                    beep();
                }
                break;
            default:
                beep();
                break;
        }
    }

    private void waitEscapeCharacter(char c) {
        if (c == 'u' || c == 'U') {
            buffer.append(c);
            insertionPoint++;
            sendComposedText();
            format = (c == 'u') ? ESCAPE : SPECIAL_ESCAPE;
        } else {
            if (c != '\\') {
                buffer.append(c);
                insertionPoint++;
            }
            sendCommittedText();
        }
    }

    private void waitDigit(char c) {
        if (Character.digit(c, 16) != -1) {
            buffer.insert(insertionPoint++, c);
            sendComposedText();
        } else {
            beep();
        }
    }

    private void waitDigit2(char c) {
        if (Character.digit(c, 16) != -1) {
            buffer.insert(insertionPoint++, c);
            char codePoint = (char) getCodePoint(buffer, 2, 5);
            if (Character.isHighSurrogate(codePoint)) {
                format = SURROGATE_PAIR;
                buffer.append("\\u");
                insertionPoint = 8;
            } else {
                format = ESCAPE;
            }
            sendComposedText();
        } else {
            beep();
        }
    }

    private void waitBackSlashOrLowSurrogate(char c) {
        if (insertionPoint == 6) {
            if (c == '\\') {
                buffer.append(c);
                buffer.append('u');
                insertionPoint = 8;
                sendComposedText();
            } else if (Character.digit(c, 16) != -1) {
                buffer.append("\\u");
                buffer.append(c);
                insertionPoint = 9;
                sendComposedText();
            } else {
                beep();
            }
        } else {
            beep();
        }
    }

    /**
     * Send the composed text to the client.
     */
    private void sendComposedText() {
        AttributedString as = new AttributedString(buffer.toString());
        as.addAttribute(TextAttribute.INPUT_METHOD_HIGHLIGHT,
                InputMethodHighlight.SELECTED_RAW_TEXT_HIGHLIGHT);
        context.dispatchInputMethodEvent(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                as.getIterator(), 0,
                TextHitInfo.leading(insertionPoint), null);
    }

    /**
     * Send the committed text to the client.
     */
    private void sendCommittedText() {
        AttributedString as = new AttributedString(buffer.toString());
        context.dispatchInputMethodEvent(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                as.getIterator(), buffer.length(),
                TextHitInfo.leading(insertionPoint), null);

        buffer.setLength(0);
        insertionPoint = 0;
        format = UNSET;
    }

    /**
     * Move the insertion point one position to the left in the composed text.
     * Do not let the caret move to the left of the "\\u" or "\\U".
     */
    private void moveCaretLeft() {
        int len = buffer.length();
        if (--insertionPoint < 2) {
            insertionPoint++;
            beep();
        } else if (format == SURROGATE_PAIR && insertionPoint == 7) {
            insertionPoint = 8;
            beep();
        }

        context.dispatchInputMethodEvent(
                InputMethodEvent.CARET_POSITION_CHANGED,
                null, 0,
                TextHitInfo.leading(insertionPoint), null);
    }

    /**
     * Move the insertion point one position to the right in the composed text.
     */
    private void moveCaretRight() {
        int len = buffer.length();
        if (++insertionPoint > len) {
            insertionPoint = len;
            beep();
        }

        context.dispatchInputMethodEvent(
                InputMethodEvent.CARET_POSITION_CHANGED,
                null, 0,
                TextHitInfo.leading(insertionPoint), null);
    }

    /**
     * Delete the character preceding the insertion point in the composed text.
     * If the insertion point is not at the end of the composed text and the
     * preceding text is "\\u" or "\\U", ring the bell.
     */
    private void deletePreviousCharacter() {
        if (insertionPoint == 2) {
            if (buffer.length() == 2) {
                cancelComposition();
            } else {
                // Do not allow deletion of the leading "\\u" or "\\U" if there
                // are other digits in the composed text.
                beep();
            }
        } else if (insertionPoint == 8) {
            if (buffer.length() == 8) {
                if (format == SURROGATE_PAIR) {
                    buffer.deleteCharAt(--insertionPoint);
                }
                buffer.deleteCharAt(--insertionPoint);
                sendComposedText();
            } else {
                // Do not allow deletion of the second "\\u" if there are other
                // digits in the composed text.
                beep();
            }
        } else {
            buffer.deleteCharAt(--insertionPoint);
            if (buffer.length() == 0) {
                sendCommittedText();
            } else {
                sendComposedText();
            }
        }
    }

    /**
     * Delete the character following the insertion point in the composed text.
     * If the insertion point is at the end of the composed text, ring the bell.
     */
    private void deleteCharacter() {
        if (insertionPoint < buffer.length()) {
            buffer.deleteCharAt(insertionPoint);
            sendComposedText();
        } else {
            beep();
        }
    }

    private void startComposition() {
        buffer.append('\\');
        insertionPoint = 1;
        sendComposedText();
    }

    private void cancelComposition() {
        buffer.setLength(0);
        insertionPoint = 0;
        sendCommittedText();
    }

    private void finishComposition() {
        int len = buffer.length();
        if (len == 6 && format != SPECIAL_ESCAPE) {
            char codePoint = (char) getCodePoint(buffer, 2, 5);
            if (Character.isValidCodePoint(codePoint) && codePoint != 0xFFFF) {
                buffer.setLength(0);
                buffer.append(codePoint);
                sendCommittedText();
                return;
            }
        } else if (len == 8 && format == SPECIAL_ESCAPE) {
            int codePoint = getCodePoint(buffer, 2, 7);
            if (Character.isValidCodePoint(codePoint) && codePoint != 0xFFFF) {
                buffer.setLength(0);
                buffer.appendCodePoint(codePoint);
                sendCommittedText();
                return;
            }
        } else if (len == 12 && format == SURROGATE_PAIR) {
            char[] codePoint = {
                (char) getCodePoint(buffer, 2, 5),
                (char) getCodePoint(buffer, 8, 11)
            };
            if (Character.isHighSurrogate(codePoint[0]) && Character.
                    isLowSurrogate(codePoint[1])) {
                buffer.setLength(0);
                buffer.append(codePoint);
                sendCommittedText();
                return;
            }
        }

        beep();
    }

    private int getCodePoint(StringBuffer sb, int from, int to) {
        int value = 0;
        for (int i = from; i <= to; i++) {
            value = (value << 4) + Character.digit(sb.charAt(i), 16);
        }
        return value;
    }

    private static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public void activate() {
        if (buffer == null) {
            buffer = new StringBuffer(12);
            insertionPoint = 0;
        }
    }

    public void deactivate(boolean isTemporary) {
        if (!isTemporary) {
            buffer = null;
        }
    }

    public void dispose() {
    }

    public Object getControlObject() {
        return null;
    }

    public void endComposition() {
        sendCommittedText();
    }

    public Locale getLocale() {
        return locale;
    }

    public void hideWindows() {
    }

    public boolean isCompositionEnabled() {
        // always enabled
        return true;
    }

    public void notifyClientWindowChange(Rectangle location) {
    }

    public void reconvert() {
        // not supported yet
        throw new UnsupportedOperationException();
    }

    public void removeNotify() {
    }

    public void setCharacterSubsets(Character.Subset[] subsets) {
    }

    public void setCompositionEnabled(boolean enable) {
        // not supported yet
        throw new UnsupportedOperationException();
    }

    public void setInputMethodContext(InputMethodContext context) {
        this.context = context;
    }

    /*
     * The Code Point Input Method supports all locales.
     */
    public boolean setLocale(Locale locale) {
        this.locale = locale;
        return true;
    }
}
