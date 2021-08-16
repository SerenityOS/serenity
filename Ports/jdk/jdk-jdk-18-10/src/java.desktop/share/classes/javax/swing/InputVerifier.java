/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.util.*;

/**
 * This class provides the validation mechanism for Swing components. GUIs often
 * need to ensure that the components are in a valid state before allowing the
 * user to navigate the input focus. To do this, clients create a subclass of
 * {@code InputVerifier} and, using {@code JComponent}'s
 * {@code setInputVerifier} method, attach an instance of their subclass to
 * the {@code JComponent} which is the source of the focus transfer operation.
 * The {@code InputVerifier} also provides the possibility to validate against
 * the target of the focus transfer which may reject the focus.
 * Before focus is transferred from the source Swing component to the target
 * Swing component, the input verifier's
 * {@code shouldYieldFocus(source, target)} method is called. Focus is
 * transferred only if that method returns
 * {@code true}.
 * <p>
 * The following example has two text fields, with the first one expecting
 * the string "pass" to be entered by the user. If either that string is entered
 * in the first text field or the second text field contains "accept" string,
 * then the user can advance focus to the second text field by clicking in it or
 * by pressing TAB.
 * However, if another string is entered in the first text field and the second
 * text field does not contain "accept", then the user will be unable to
 * transfer focus to the second text field.
 *
 * <pre>
 * import java.awt.*;
 * import javax.swing.*;
 *
 * // This program demonstrates the use of the Swing InputVerifier class.
 * // It creates two text fields; the first of the text fields expects the
 * // string "pass" as input, and will allow focus to advance to the second text
 * // field if either that string is typed in by the user or the second
 * // field contains "accept" string.
 *
 * public class VerifierTest extends JFrame {
 *
 *     public VerifierTest() {
 *         JTextField field1 = new JTextField("Type \"pass\" here");
 *         JTextField field2 = new JTextField("or \"accept\" here");
 *         getContentPane().add(field1, BorderLayout.NORTH);
 *         getContentPane().add(field2, BorderLayout.SOUTH);
 *
 *         field1.setInputVerifier(new InputVerifier() {
 *             public boolean verify(JComponent input) {
 *                return "pass".equals(((JTextField) input).getText());
 *             }
 *
 *             public boolean verifyTarget(JComponent input) {
 *                 return "accept".equals(((JTextField) input).getText());
 *             }
 *
 *             public boolean shouldYieldFocus(JComponent source,
 *                                                          JComponent target) {
 *                 return verify(source) || verifyTarget(target);
 *             }
 *         });
 *
 *         pack();
 *         setVisible(true);
 *     }
 *
 *     public static void main(String[] args) {
 *         SwingUtilities.invokeLater(VerifierTest::new);
 *     }
 * }
 * </pre>
 *
 * @since 1.3
 */
public abstract class InputVerifier {

    /**
     * Constructor for subclasses to call.
     */
    protected InputVerifier() {}

    /**
     * Checks whether the JComponent's input is valid. This method should
     * have no side effects. It returns a boolean indicating the status
     * of the argument's input.
     *
     * @param input the JComponent to verify
     * @return {@code true} when valid, {@code false} when invalid
     * @see JComponent#setInputVerifier
     * @see JComponent#getInputVerifier
     */
    public abstract boolean verify(JComponent input);

    /**
     * Calls {@code verify(input)} to ensure that the input is valid.
     * This method can have side effects. In particular, this method
     * is called when the user attempts to advance focus out of the
     * argument component into another Swing component in this window.
     * If this method returns {@code true}, then the focus is transferred
     * normally; if it returns {@code false}, then the focus remains in
     * the argument component.
     *
     * @param input the JComponent to verify
     * @return {@code true} when valid, {@code false} when invalid
     * @see JComponent#setInputVerifier
     * @see JComponent#getInputVerifier
     *
     * @deprecated use {@link #shouldYieldFocus(JComponent, JComponent)}
     * instead.
     */
    @Deprecated(since = "9")
    public boolean shouldYieldFocus(JComponent input) {
        return verify(input);
    }

    /**
     * Checks whether the target JComponent that will be receiving the focus
     * is ready to accept it. This method should be over-ridden only if it is
     * necessary to validate the target of the focus transfer.
     * This method should have no side effects. It returns a boolean
     * indicating the status of the argument's input.
     *
     * @implSpec By default this method returns {@code true}.
     *
     * @param target the target JComponent to verify
     * @return {@code true} when valid, {@code false} when invalid
     * @see JComponent#setInputVerifier
     * @see JComponent#getInputVerifier
     * @since 9
     */
    public boolean verifyTarget(JComponent target) {
        return true;
    }

    /**
     * Is called by Swing if this {@code InputVerifier} is assigned to the
     * {@code source} Swing component to check whether the requested focus
     * transfer from the {@code source} to {@code target} is allowed.
     * This method can have side effects.
     * If this method returns {@code true}, then the focus is transferred
     * normally; if it returns {@code false}, then the focus remains in
     * the first argument component.
     *
     * @implSpec The basic implementation of this method returns the conjunction
     * of results obtained from {@code verify(input)} and
     * {@code verifyTarget(input)} to ensure that both the source and the target
     * components are in valid state.
     *
     * @param source the source JComponent of the focus transfer
     * @param target the target JComponent of the focus transfer
     * @return {@code true} when valid, {@code false} when invalid
     * @see JComponent#setInputVerifier
     * @see JComponent#getInputVerifier
     * @since 9
     */
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        return shouldYieldFocus(source) && verifyTarget(target);
    }
}
