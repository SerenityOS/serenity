/*
 * Copyright (c) 1997, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.im.spi;

import java.util.Locale;
import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.lang.Character.Subset;


/**
 * Defines the interface for an input method that supports complex text input.
 * Input methods traditionally support text input for languages that have
 * more characters than can be represented on a standard-size keyboard,
 * such as Chinese, Japanese, and Korean. However, they may also be used to
 * support phonetic text input for English or character reordering for Thai.
 * <p>
 * Subclasses of InputMethod can be loaded by the input method framework; they
 * can then be selected either through the API
 * ({@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod})
 * or the user interface (the input method selection menu).
 *
 * @since 1.3
 *
 * @author JavaSoft International
 */

public interface InputMethod {

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
    public void setInputMethodContext(InputMethodContext context);

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
     * @param locale locale to input
     * @return whether the specified locale is supported
     * @exception NullPointerException if {@code locale} is null
     */
    public boolean setLocale(Locale locale);

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
    public Locale getLocale();

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
    public void setCharacterSubsets(Subset[] subsets);

    /**
     * Enables or disables this input method for composition,
     * depending on the value of the parameter {@code enable}.
     * <p>
     * An input method that is enabled for composition interprets incoming
     * events for both composition and control purposes, while a
     * disabled input method does not interpret events for composition.
     * Note however that events are passed on to the input method regardless
     * whether it is enabled or not, and that an input method that is disabled
     * for composition may still interpret events for control purposes,
     * including to enable or disable itself for composition.
     * <p>
     * For input methods provided by host operating systems, it is not always possible to
     * determine whether this operation is supported. For example, an input method may enable
     * composition only for some locales, and do nothing for other locales. For such input
     * methods, it is possible that this method does not throw
     * {@link java.lang.UnsupportedOperationException UnsupportedOperationException},
     * but also does not affect whether composition is enabled.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#setCompositionEnabled InputContext.setCompositionEnabled},
     * <li>when switching to this input method from a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod},
     *     if the previously selected input method's
     *     {@link java.awt.im.spi.InputMethod#isCompositionEnabled isCompositionEnabled}
     *     method returns without throwing an exception.
     * </ul>
     *
     * @param enable whether to enable the input method for composition
     * @throws UnsupportedOperationException if this input method does not
     * support the enabling/disabling operation
     * @see #isCompositionEnabled
     */
    public void setCompositionEnabled(boolean enable);

    /**
     * Determines whether this input method is enabled.
     * An input method that is enabled for composition interprets incoming
     * events for both composition and control purposes, while a
     * disabled input method does not interpret events for composition.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#isCompositionEnabled InputContext.isCompositionEnabled} and
     * <li>when switching from this input method to a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod}.
     * </ul>
     *
     * @return {@code true} if this input method is enabled for
     * composition; {@code false} otherwise.
     * @throws UnsupportedOperationException if this input method does not
     * support checking whether it is enabled for composition
     * @see #setCompositionEnabled
     */
    public boolean isCompositionEnabled();

    /**
     * Starts the reconversion operation. The input method obtains the
     * text to be reconverted from the current client component using the
     * {@link java.awt.im.InputMethodRequests#getSelectedText InputMethodRequests.getSelectedText}
     * method. It can use other {@code InputMethodRequests}
     * methods to request additional information required for the
     * reconversion operation. The composed and committed text
     * produced by the operation is sent to the client component as a
     * sequence of {@code InputMethodEvent}s. If the given text
     * cannot be reconverted, the same text should be sent to the
     * client component as committed text.
     * <p>
     * This method is called by
     * {@link java.awt.im.InputContext#reconvert() InputContext.reconvert}.
     *
     * @throws UnsupportedOperationException if the input method does not
     * support the reconversion operation.
     */
    public void reconvert();

    /**
     * Dispatches the event to the input method. If input method support is
     * enabled for the focused component, incoming events of certain types
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
    public void dispatchEvent(AWTEvent event);

    /**
     * Notifies this input method of changes in the client window
     * location or state. This method is called while this input
     * method is the current input method of its input context and
     * notifications for it are enabled (see {@link
     * InputMethodContext#enableClientWindowNotification
     * InputMethodContext.enableClientWindowNotification}). Calls
     * to this method are temporarily suspended if the input context's
     * {@link java.awt.im.InputContext#removeNotify removeNotify}
     * method is called, and resume when the input method is activated
     * for a new client component. It is called in the following
     * situations:
     * <ul>
     * <li>
     * when the window containing the current client component changes
     * in location, size, visibility, iconification state, or when the
     * window is closed.</li>
     * <li>
     * from {@code enableClientWindowNotification(inputMethod, true)}
     * if the current client component exists,</li>
     * <li>
     * when activating the input method for the first time after it
     * called
     * {@code enableClientWindowNotification(inputMethod, true)}
     * if during the call no current client component was
     * available,</li>
     * <li>
     * when activating the input method for a new client component
     * after the input context's removeNotify method has been
     * called.</li>
     * </ul>
     * @param bounds client window's {@link
     * java.awt.Component#getBounds bounds} on the screen; or null if
     * the client window is iconified or invisible
     */
    public void notifyClientWindowChange(Rectangle bounds);

    /**
     * Activates the input method for immediate input processing.
     * <p>
     * If an input method provides its own windows, it should make sure
     * at this point that all necessary windows are open and visible.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#dispatchEvent InputContext.dispatchEvent}
     *     when a client component receives a FOCUS_GAINED event,
     * <li>when switching to this input method from a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod}.
     * </ul>
     * The method is only called when the input method is inactive.
     * A newly instantiated input method is assumed to be inactive.
     */
    public void activate();

    /**
     * Deactivates the input method.
     * The isTemporary argument has the same meaning as in
     * {@link java.awt.event.FocusEvent#isTemporary FocusEvent.isTemporary}.
     * <p>
     * If an input method provides its own windows, only windows that relate
     * to the current composition (such as a lookup choice window) should be
     * closed at this point.
     * It is possible that the input method will be immediately activated again
     * for a different client component, and closing and reopening more
     * persistent windows (such as a control panel) would create unnecessary
     * screen flicker.
     * Before an instance of a different input method class is activated,
     * {@link #hideWindows} is called on the current input method.
     * <p>
     * This method is called
     * <ul>
     * <li>by {@link java.awt.im.InputContext#dispatchEvent InputContext.dispatchEvent}
     *     when a client component receives a FOCUS_LOST event,
     * <li>when switching from this input method to a different one using the
     *     user interface or
     *     {@link java.awt.im.InputContext#selectInputMethod InputContext.selectInputMethod},
     * <li>before {@link #removeNotify removeNotify} if the current client component is
     *     removed.
     * </ul>
     * The method is only called when the input method is active.
     *
     * @param isTemporary whether the focus change is temporary
     */
    public void deactivate(boolean isTemporary);

    /**
     * Closes or hides all windows opened by this input method instance or
     * its class.
     * <p>
     * This method is called
     * <ul>
     * <li>before calling {@link #activate activate} on an instance of a different input
     *     method class,
     * <li>before calling {@link #dispose dispose} on this input method.
     * </ul>
     * The method is only called when the input method is inactive.
     */
    public void hideWindows();

    /**
     * Notifies the input method that a client component has been
     * removed from its containment hierarchy, or that input method
     * support has been disabled for the component.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#removeNotify InputContext.removeNotify}.
     * <p>
     * The method is only called when the input method is inactive.
     */
    public void removeNotify();

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
    public void endComposition();

    /**
     * Releases the resources used by this input method.
     * In particular, the input method should dispose windows and close files that are no
     * longer needed.
     * <p>
     * This method is called by {@link java.awt.im.InputContext#dispose InputContext.dispose}.
     * <p>
     * The method is only called when the input method is inactive.
     * No method of this interface is called on this instance after dispose.
     */
    public void dispose();

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
    public Object getControlObject();

}
