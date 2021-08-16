/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package com.sun.swingset3.demos.gridbaglayout;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;

/**
 * Calculator
 *
 * @author Pavel Porvatov
 */
public class Calculator extends JComponent {

    private static final String ZERO = "0";

    private static final char DECIMAL_SEPARATOR = ',';

    private final JTextField tfScreen = new JTextField();

    private enum State {

        INPUT_X,
        INPUT_X_FINISHED,
        INPUT_Y,
        INPUT_Y_FINISHED
    }

    private enum Operator {

        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        SQRT,
        INVERSE,
        EQUALS
    }

    private final Map<Character, Operator> keyMapping = new HashMap<Character, Operator>();

    private String operand_x;

    private Operator operator;

    private State state;

    public static final String ZERO_BUTTON_TITLE = "0";
    public static final String ONE_BUTTON_TITLE = "1";
    public static final String TWO_BUTTON_TITLE = "2";
    public static final String THREE_BUTTON_TITLE = "3";
    public static final String FOUR_BUTTON_TITLE = "4";
    public static final String FIVE_BUTTON_TITLE = "5";
    public static final String SIX_BUTTON_TITLE = "6";
    public static final String SEVEN_BUTTON_TITLE = "7";
    public static final String EIGHT_BUTTON_TITLE = "8";
    public static final String NINE_BUTTON_TITLE = "9";
    public static final String PLUS_BUTTON_TITLE = "+";
    public static final String MINUS_BUTTON_TITLE = "-";
    public static final String DIVIDE_BUTTON_TITLE = "/";
    public static final String MULTIPLY_BUTTON_TITLE = "*";
    public static final String BACKSPACE_BUTTON_TITLE = "Backspace";
    public static final String C_BUTTON_TITLE = "C";
    public static final String EQUALS_BUTTON_TITLE = "=";
    public static final String SWAPSIGN_BUTTON_TITLE = "+/-";
    public static final String INVERSE_BUTTON_TITLE = "1/x";
    public static final String SQRT_BUTTON_TITLE = "sqrt";
    public static final String COMMA_BUTTON_TITLE = ",";

    private static DecimalFormatSymbols decimalFormatSymbols;
    private static DecimalFormat decimalFormat;

    public Calculator() {
        keyMapping.put('/', Operator.DIVISION);
        keyMapping.put('*', Operator.MULTIPLICATION);
        keyMapping.put('+', Operator.ADDITION);
        keyMapping.put('-', Operator.SUBTRACTION);
        keyMapping.put('\n', Operator.EQUALS);

        decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
        decimalFormatSymbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("##0.###", decimalFormatSymbols);

        initUI();

        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (Character.isDigit(c)) {
                    doProcessChar(c);
                } else if (c == '.' || c == ',') {
                    doProcessChar(DECIMAL_SEPARATOR);
                } else {
                    Operator operator = keyMapping.get(c);

                    if (operator != null) {
                        doProcessOperator(operator);
                    }
                }
            }

            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE:
                        doProcessBackspace();

                        break;

                    case KeyEvent.VK_DELETE:
                        doReset();
                }
            }
        });

        doReset();
    }

    private void initUI() {
        tfScreen.setHorizontalAlignment(JTextField.RIGHT);

        JButton btnBackspace = new JButton(BACKSPACE_BUTTON_TITLE);

        btnBackspace.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doProcessBackspace();
            }
        });

        JButton btnReset = new JButton(C_BUTTON_TITLE);

        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doReset();
            }
        });

        JPanel pnGridPanel = new JPanel(new GridLayout(1, 2, 8, 8));

        pnGridPanel.add(btnBackspace);
        pnGridPanel.add(btnReset);

        setLayout(new GridBagLayout());

        JButton btnSwapSign = new SquareButton(SWAPSIGN_BUTTON_TITLE);

        btnSwapSign.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSwapSign();
            }
        });

        addComp(tfScreen, 0, 0, 5, 1);
        addComp(pnGridPanel, 0, 1, 5, 1);

        addComp(new CharButton(SEVEN_BUTTON_TITLE.charAt(0)), 0, 2, 1, 1);
        addComp(new CharButton(EIGHT_BUTTON_TITLE.charAt(0)), 1, 2, 1, 1);
        addComp(new CharButton(NINE_BUTTON_TITLE.charAt(0)), 2, 2, 1, 1);
        addComp(new OperatorButton(Operator.DIVISION, DIVIDE_BUTTON_TITLE), 3, 2, 1, 1);
        addComp(new OperatorButton(Operator.INVERSE, INVERSE_BUTTON_TITLE), 4, 2, 1, 1);

        addComp(new CharButton(FOUR_BUTTON_TITLE.charAt(0)), 0, 3, 1, 1);
        addComp(new CharButton(FIVE_BUTTON_TITLE.charAt(0)), 1, 3, 1, 1);
        addComp(new CharButton(SIX_BUTTON_TITLE.charAt(0)), 2, 3, 1, 1);
        addComp(new OperatorButton(Operator.MULTIPLICATION, MULTIPLY_BUTTON_TITLE), 3, 3, 1, 1);
        addComp(new OperatorButton(Operator.SQRT, SQRT_BUTTON_TITLE), 4, 3, 1, 1);

        addComp(new CharButton(ONE_BUTTON_TITLE.charAt(0)), 0, 4, 1, 1);
        addComp(new CharButton(TWO_BUTTON_TITLE.charAt(0)), 1, 4, 1, 1);
        addComp(new CharButton(THREE_BUTTON_TITLE.charAt(0)), 2, 4, 1, 1);
        addComp(new OperatorButton(Operator.SUBTRACTION, MINUS_BUTTON_TITLE), 3, 4, 1, 1);

        addComp(new CharButton(ZERO_BUTTON_TITLE.charAt(0)), 0, 5, 1, 1);
        addComp(btnSwapSign, 1, 5, 1, 1);
        addComp(new CharButton(COMMA_BUTTON_TITLE.charAt(0)), 2, 5, 1, 1);
        addComp(new OperatorButton(Operator.ADDITION, PLUS_BUTTON_TITLE), 3, 5, 1, 1);
        addComp(new OperatorButton(Operator.EQUALS, EQUALS_BUTTON_TITLE), 4, 5, 1, 1);

        // Set focusable false
        resetFocusable(this);

        setFocusable(true);
    }

    private static void resetFocusable(Component component) {
        component.setFocusable(false);

        if (component instanceof Container) {
            for (Component c : ((Container) component).getComponents()) {
                resetFocusable(c);
            }
        }
    }

    private void doReset() {
        synchronized (this) {
            operand_x = null;
            operator = null;
            state = State.INPUT_X;
        }
        tfScreen.setText(ZERO);
    }

    private void doProcessChar(char c) {
        String text = tfScreen.getText();

        String newValue;

        synchronized (this) {
            if (state == State.INPUT_X || state == State.INPUT_Y) {
                newValue = attachChar(text, c);

                if (stringToValue(newValue) == null) {
                    return;
                }
            } else {
                newValue = attachChar("0", c);

                if (stringToValue(newValue) == null) {
                    return;
                }

                if (operator == null) {
                    operand_x = null;
                    state = State.INPUT_X;
                } else {
                    operand_x = text;
                    state = State.INPUT_Y;
                }
            }
        }
        tfScreen.setText(newValue);
    }

    private static String attachChar(String s, char c) {
        if (Character.isDigit(c)) {
            if (s.equals(ZERO)) {
                return Character.toString(c);
            }

            if (s.equals("-" + ZERO)) {
                return "-" + Character.toString(c);
            }

            return s + Character.toString(c);
        } else {
            return s + Character.toString(c);
        }
    }

    private void doSwapSign() {
        String text = tfScreen.getText();

        tfScreen.setText(text.startsWith("-") ? text.substring(1) : "-" + text);
    }

    private void doProcessBackspace() {
        String text = tfScreen.getText();

        if (text.length() > 0) {
            text = text.substring(0, text.length() - 1);
        }

        if (text.length() == 0 || text.equals("-")) {
            text = ZERO;
        }

        if (stringToValue(text) != null) {
            tfScreen.setText(text);
        }
    }

    private void doProcessOperator(Operator operator) {
        double y = stringToValue(tfScreen.getText());

        // Process unary operators
        boolean isUnary;

        switch (operator) {
            case SQRT:
                tfScreen.setText(valueToString(Math.sqrt(y)));

                isUnary = true;

                break;

            case INVERSE:
                tfScreen.setText(valueToString(1 / y));

                isUnary = true;

                break;

            default:
                isUnary = false;
        }

        synchronized (this) {
            if (isUnary) {
                if (state == State.INPUT_X) {
                    state = State.INPUT_X_FINISHED;
                }

                if (state == State.INPUT_Y) {
                    state = State.INPUT_Y_FINISHED;
                }

                return;
            }

            // Process binary operators
            if (state == State.INPUT_Y || state == State.INPUT_Y_FINISHED) {
                double x = stringToValue(operand_x);
                double result;

                switch (this.operator) {
                    case ADDITION:
                        result = x + y;

                        break;

                    case SUBTRACTION:
                        result = x - y;

                        break;

                    case MULTIPLICATION:
                        result = x * y;

                        break;

                    case DIVISION:
                        result = x / y;

                        break;

                    default:
                        throw new IllegalStateException("Unsupported operation " + operator);
                }

                tfScreen.setText(valueToString(result));
            }

            this.operator = operator == Operator.EQUALS ? null : operator;
            operand_x = null;

            state = State.INPUT_X_FINISHED;
        }
    }

    private static Double stringToValue(String value) {
        try {
            return new Double(value.replace(DECIMAL_SEPARATOR, '.'));
        } catch (NumberFormatException e) {
            // Continue convertion
        }

        if (value.endsWith(String.valueOf(DECIMAL_SEPARATOR))) {
            try {
                // Try convert uncompleted value
                return new Double(value.substring(0, value.length() - 1));
            } catch (NumberFormatException e) {
                // Continue convertion
            }
        }

        return null;
    }

    private static String valueToString(Double value) {
        if (value == null) {
            return ZERO;
        } else {
            String result = decimalFormat.format(value);

            if (result.endsWith(".0")) {
                result = result.substring(0, result.length() - 2);
            }

            if (result.equals("-0")) {
                result = ZERO;
            }

            return result;
        }
    }

    private void addComp(Component comp, int gridx, int gridy,
            int gridwidth, int gridheight) {
        add(comp, new GridBagConstraints(gridx, gridy, gridwidth, gridheight,
                1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(4, 4, 4, 4), 0, 0));
    }

    private static class SquareButton extends JButton {

        private SquareButton(String text) {
            super(text);

            setMargin(new Insets(2, 0, 2, 0));
        }

        public Dimension getMinimumSize() {
            Dimension result = super.getMinimumSize();

            if (result.width < result.height) {
                result.width = result.height;
            }

            return result;
        }

        public Dimension getPreferredSize() {
            return getMinimumSize();
        }
    }

    private class CharButton extends SquareButton {

        private CharButton(final char c) {
            super(String.valueOf(c));

            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doProcessChar(c);
                }
            });
        }
    }

    private class OperatorButton extends SquareButton {

        private OperatorButton(final Operator operator, String text) {
            super(text);

            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doProcessOperator(operator);
                }
            });
        }
    }
}
