/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import sun.swing.DefaultLookup;
import sun.swing.UIAction;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.OptionPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;


/**
 * Provides the basic look and feel for a <code>JOptionPane</code>.
 * <code>BasicMessagePaneUI</code> provides a means to place an icon,
 * message and buttons into a <code>Container</code>.
 * Generally, the layout will look like:
 * <pre>
 *        ------------------
 *        | i | message    |
 *        | c | message    |
 *        | o | message    |
 *        | n | message    |
 *        ------------------
 *        |     buttons    |
 *        |________________|
 * </pre>
 * icon is an instance of <code>Icon</code> that is wrapped inside a
 * <code>JLabel</code>.  The message is an opaque object and is tested
 * for the following: if the message is a <code>Component</code> it is
 * added to the <code>Container</code>, if it is an <code>Icon</code>
 * it is wrapped inside a <code>JLabel</code> and added to the
 * <code>Container</code> otherwise it is wrapped inside a <code>JLabel</code>.
 * <p>
 * The above layout is used when the option pane's
 * <code>ComponentOrientation</code> property is horizontal, left-to-right.
 * The layout will be adjusted appropriately for other orientations.
 * <p>
 * The <code>Container</code>, message, icon, and buttons are all
 * determined from abstract methods.
 *
 * @author James Gosling
 * @author Scott Violet
 * @author Amy Fowler
 */
public class BasicOptionPaneUI extends OptionPaneUI {

    /**
     * The mininum width of {@code JOptionPane}.
     */
    public static final int MinimumWidth = 262;
    /**
     * The mininum height of {@code JOptionPane}.
     */
    public static final int MinimumHeight = 90;

    private static String newline;

    /**
     * {@code JOptionPane} that the receiver is providing the
     * look and feel for.
     */
    protected JOptionPane         optionPane;

    /**
     * The size of {@code JOptionPane}.
     */
    protected Dimension minimumSize;

    /** JComponent provide for input if optionPane.getWantsInput() returns
     * true. */
    protected JComponent          inputComponent;

    /** Component to receive focus when messaged with selectInitialValue. */
    protected Component           initialFocusComponent;

    /** This is set to true in validateComponent if a Component is contained
     * in either the message or the buttons. */
    protected boolean             hasCustomComponents;

    /**
     * The instance of {@code PropertyChangeListener}.
     */
    protected PropertyChangeListener propertyChangeListener;

    private Handler handler;


    static {
        newline = System.lineSeparator();
        if (newline == null) {
            newline = "\n";
        }
    }

    /**
     * Constructs a {@code BasicOptionPaneUI}.
     */
    public BasicOptionPaneUI() {}

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.CLOSE));
        BasicLookAndFeel.installAudioActionMap(map);
    }



    /**
     * Creates a new {@code BasicOptionPaneUI} instance.
     * @param x the component
     * @return a new {@code BasicOptionPaneUI} instance
     */
    public static ComponentUI createUI(JComponent x) {
        return new BasicOptionPaneUI();
    }

    /**
      * Installs the receiver as the L&amp;F for the passed in
      * <code>JOptionPane</code>.
      */
    public void installUI(JComponent c) {
        optionPane = (JOptionPane)c;
        installDefaults();
        optionPane.setLayout(createLayoutManager());
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    /**
      * Removes the receiver from the L&amp;F controller of the passed in split
      * pane.
      */
    public void uninstallUI(JComponent c) {
        uninstallComponents();
        optionPane.setLayout(null);
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        optionPane = null;
    }

    /**
     * Installs default properties.
     */
    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(optionPane, "OptionPane.background",
                                         "OptionPane.foreground", "OptionPane.font");
        LookAndFeel.installBorder(optionPane, "OptionPane.border");
        minimumSize = UIManager.getDimension("OptionPane.minimumSize");
        LookAndFeel.installProperty(optionPane, "opaque", Boolean.TRUE);
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(optionPane);
    }

    /**
     * Registers components.
     */
    protected void installComponents() {
        optionPane.add(createMessageArea());

        Container separator = createSeparator();
        if (separator != null) {
            optionPane.add(separator);
        }
        optionPane.add(createButtonArea());
        optionPane.applyComponentOrientation(optionPane.getComponentOrientation());
    }

    /**
     * Unregisters components.
     */
    protected void uninstallComponents() {
        hasCustomComponents = false;
        inputComponent = null;
        initialFocusComponent = null;
        optionPane.removeAll();
    }

    /**
     * Returns a layout manager.
     *
     * @return a layout manager
     */
    protected LayoutManager createLayoutManager() {
        return new BoxLayout(optionPane, BoxLayout.Y_AXIS);
    }

    /**
     * Registers listeners.
     */
    protected void installListeners() {
        if ((propertyChangeListener = createPropertyChangeListener()) != null) {
            optionPane.addPropertyChangeListener(propertyChangeListener);
        }
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners() {
        if (propertyChangeListener != null) {
            optionPane.removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }
        handler = null;
    }

    /**
     * Returns an instance of {@code PropertyChangeListener}.
     *
     * @return an instance of {@code PropertyChangeListener}
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Registers keyboard actions.
     */
    protected void installKeyboardActions() {
        InputMap map = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        SwingUtilities.replaceUIInputMap(optionPane, JComponent.
                                       WHEN_IN_FOCUSED_WINDOW, map);

        LazyActionMap.installLazyActionMap(optionPane, BasicOptionPaneUI.class,
                                           "OptionPane.actionMap");
    }

    /**
     * Unregisters keyboard actions.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(optionPane, JComponent.
                                       WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(optionPane, null);
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_IN_FOCUSED_WINDOW) {
            Object[] bindings = (Object[])DefaultLookup.get(
                             optionPane, this, "OptionPane.windowBindings");
            if (bindings != null) {
                return LookAndFeel.makeComponentInputMap(optionPane, bindings);
            }
        }
        return null;
    }

    /**
     * Returns the minimum size the option pane should be. Primarily
     * provided for subclassers wishing to offer a different minimum size.
     *
     * @return the minimum size of the option pane
     */
    public Dimension getMinimumOptionPaneSize() {
        if (minimumSize == null) {
            return new Dimension(MinimumWidth, MinimumHeight);
        }
        return new Dimension(minimumSize.width,
                             minimumSize.height);
    }

    /**
     * If <code>c</code> is the <code>JOptionPane</code> the receiver
     * is contained in, the preferred
     * size that is returned is the maximum of the preferred size of
     * the <code>LayoutManager</code> for the <code>JOptionPane</code>, and
     * <code>getMinimumOptionPaneSize</code>.
     */
    public Dimension getPreferredSize(JComponent c) {
        if (c == optionPane) {
            Dimension            ourMin = getMinimumOptionPaneSize();
            LayoutManager        lm = c.getLayout();

            if (lm != null) {
                Dimension         lmSize = lm.preferredLayoutSize(c);

                if (ourMin != null)
                    return new Dimension
                        (Math.max(lmSize.width, ourMin.width),
                         Math.max(lmSize.height, ourMin.height));
                return lmSize;
            }
            return ourMin;
        }
        return null;
    }

    /**
     * Messaged from {@code installComponents} to create a {@code Container}
     * containing the body of the message. The icon is the created
     * by calling {@code addIcon}.
     *
     * @return a instance of {@code Container}
     */
    protected Container createMessageArea() {
        JPanel top = new JPanel();
        Border topBorder = (Border)DefaultLookup.get(optionPane, this,
                                             "OptionPane.messageAreaBorder");
        if (topBorder != null) {
            top.setBorder(topBorder);
        }
        top.setLayout(new BorderLayout());

        /* Fill the body. */
        Container          body = new JPanel(new GridBagLayout());
        Container          realBody = new JPanel(new BorderLayout());

        body.setName("OptionPane.body");
        realBody.setName("OptionPane.realBody");

        if (getIcon() != null) {
            JPanel sep = new JPanel();
            sep.setName("OptionPane.separator");
            sep.setPreferredSize(new Dimension(15, 1));
            realBody.add(sep, BorderLayout.BEFORE_LINE_BEGINS);
        }
        realBody.add(body, BorderLayout.CENTER);

        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = cons.gridy = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.gridheight = 1;
        cons.anchor = DefaultLookup.getInt(optionPane, this,
                      "OptionPane.messageAnchor", GridBagConstraints.CENTER);
        cons.insets = new Insets(0,0,3,0);

        addMessageComponents(body, cons, getMessage(),
                          getMaxCharactersPerLineCount(), false);
        top.add(realBody, BorderLayout.CENTER);

        addIcon(top);
        return top;
    }

    /**
     * Creates the appropriate object to represent {@code msg} and places it
     * into {@code container}. If {@code msg} is an instance of
     * {@code Component}, it is added directly; if it is an {@code Icon}, a
     * {@code JLabel} is created to represent it; otherwise, a {@code JLabel}
     * is created for the string. If {@code msg} is an Object[], this method
     * will be recursively invoked for the children. {@code internallyCreated}
     * is {@code true} if {@code msg} is an instance of {@code Component} and
     * was created internally by this method (this is used to correctly set
     * {@code hasCustomComponents} only if {@code internallyCreated} is
     * {@code false}).
     *
     * @param container a container
     * @param cons an instance of {@code GridBagConstraints}
     * @param msg a message
     * @param maxll a maximum length
     * @param internallyCreated {@code true} if the component was internally created
     */
    protected void addMessageComponents(Container container,
                                     GridBagConstraints cons,
                                     Object msg, int maxll,
                                     boolean internallyCreated) {
        if (msg == null) {
            return;
        }
        if (msg instanceof Component) {
            // To workaround problem where Gridbad will set child
            // to its minimum size if its preferred size will not fit
            // within allocated cells
            if (msg instanceof JScrollPane || msg instanceof JPanel) {
                cons.fill = GridBagConstraints.BOTH;
                cons.weighty = 1;
            } else {
                cons.fill = GridBagConstraints.HORIZONTAL;
            }
            cons.weightx = 1;

            container.add((Component) msg, cons);
            cons.weightx = 0;
            cons.weighty = 0;
            cons.fill = GridBagConstraints.NONE;
            cons.gridy++;
            if (!internallyCreated) {
                hasCustomComponents = true;
            }

        } else if (msg instanceof Object[]) {
            Object [] msgs = (Object[]) msg;
            for (Object o : msgs) {
                addMessageComponents(container, cons, o, maxll, false);
            }

        } else if (msg instanceof Icon) {
            JLabel label = new JLabel( (Icon)msg, SwingConstants.CENTER );
            configureMessageLabel(label);
            addMessageComponents(container, cons, label, maxll, true);

        } else {
            String s = msg.toString();
            int len = s.length();
            if (len <= 0) {
                return;
            }
            int nl;
            int nll = 0;

            if ((nl = s.indexOf(newline)) >= 0) {
                nll = newline.length();
            } else if ((nl = s.indexOf("\r\n")) >= 0) {
                nll = 2;
            } else if ((nl = s.indexOf('\n')) >= 0) {
                nll = 1;
            }
            if (nl >= 0) {
                // break up newlines
                if (nl == 0) {
                    @SuppressWarnings("serial") // anonymous class
                    JPanel breakPanel = new JPanel() {
                        public Dimension getPreferredSize() {
                            Font       f = getFont();

                            if (f != null) {
                                return new Dimension(1, f.getSize() + 2);
                            }
                            return new Dimension(0, 0);
                        }
                    };
                    breakPanel.setName("OptionPane.break");
                    addMessageComponents(container, cons, breakPanel, maxll,
                                         true);
                } else {
                    addMessageComponents(container, cons, s.substring(0, nl),
                                      maxll, false);
                }
                addMessageComponents(container, cons, s.substring(nl + nll), maxll,
                                  false);

            } else if (len > maxll) {
                Container c = Box.createVerticalBox();
                c.setName("OptionPane.verticalBox");
                burstStringInto(c, s, maxll);
                addMessageComponents(container, cons, c, maxll, true );

            } else {
                JLabel label;
                label = new JLabel( s, JLabel.LEADING );
                label.setName("OptionPane.label");
                configureMessageLabel(label);
                addMessageComponents(container, cons, label, maxll, true);
            }
        }
    }

    /**
     * Returns the message to display from the {@code JOptionPane} the receiver is
     * providing the look and feel for.
     *
     * @return the message to display
     */
    protected Object getMessage() {
        inputComponent = null;
        if (optionPane != null) {
            if (optionPane.getWantsInput()) {
                /* Create a user component to capture the input. If the
                   selectionValues are non null the component and there
                   are < 20 values it'll be a combobox, if non null and
                   >= 20, it'll be a list, otherwise it'll be a textfield. */
                Object             message = optionPane.getMessage();
                Object[]           sValues = optionPane.getSelectionValues();
                Object             inputValue = optionPane
                                           .getInitialSelectionValue();
                JComponent         toAdd;

                if (sValues != null) {
                    if (sValues.length < 20) {
                        JComboBox<Object> cBox = new JComboBox<>();

                        cBox.setName("OptionPane.comboBox");
                        for(int counter = 0, maxCounter = sValues.length;
                            counter < maxCounter; counter++) {
                            cBox.addItem(sValues[counter]);
                        }
                        if (inputValue != null) {
                            cBox.setSelectedItem(inputValue);
                        }
                        inputComponent = cBox;
                        toAdd = cBox;

                    } else {
                        JList<Object>      list = new JList<>(sValues);
                        JScrollPane          sp = new JScrollPane(list);

                        sp.setName("OptionPane.scrollPane");
                        list.setName("OptionPane.list");
                        list.setVisibleRowCount(10);
                        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        if(inputValue != null)
                            list.setSelectedValue(inputValue, true);
                        list.addMouseListener(getHandler());
                        toAdd = sp;
                        inputComponent = list;
                    }

                } else {
                    MultiplexingTextField   tf = new MultiplexingTextField(20);

                    tf.setName("OptionPane.textField");
                    tf.setKeyStrokes(new KeyStroke[] {
                                     KeyStroke.getKeyStroke("ENTER") } );
                    if (inputValue != null) {
                        String inputString = inputValue.toString();
                        tf.setText(inputString);
                        tf.setSelectionStart(0);
                        tf.setSelectionEnd(inputString.length());
                    }
                    tf.addActionListener(getHandler());
                    toAdd = inputComponent = tf;
                }

                Object[]           newMessage;

                if (message == null) {
                    newMessage = new Object[1];
                    newMessage[0] = toAdd;

                } else {
                    newMessage = new Object[2];
                    newMessage[0] = message;
                    newMessage[1] = toAdd;
                }
                return newMessage;
            }
            return optionPane.getMessage();
        }
        return null;
    }

    /**
     * Creates and adds a JLabel representing the icon returned from
     * {@code getIcon} to {@code top}. This is messaged from
     * {@code createMessageArea}.
     *
     * @param top a container
     */
    protected void addIcon(Container top) {
        /* Create the icon. */
        Icon                  sideIcon = getIcon();

        if (sideIcon != null) {
            JLabel            iconLabel = new JLabel(sideIcon);

            iconLabel.setName("OptionPane.iconLabel");
            iconLabel.setVerticalAlignment(SwingConstants.TOP);
            top.add(iconLabel, BorderLayout.BEFORE_LINE_BEGINS);
        }
    }

    /**
     * Returns the icon from the {@code JOptionPane} the receiver is providing
     * the look and feel for, or the default icon as returned from
     * {@code getDefaultIcon}.
     *
     * @return the icon
     */
    protected Icon getIcon() {
        Icon      mIcon = (optionPane == null ? null : optionPane.getIcon());

        if(mIcon == null && optionPane != null)
            mIcon = getIconForType(optionPane.getMessageType());
        return mIcon;
    }

    /**
     * Returns the icon to use for the passed in type.
     *
     * @param messageType a type of message
     * @return the icon to use for the passed in type
     */
    protected Icon getIconForType(int messageType) {
        if(messageType < 0 || messageType > 3)
            return null;
        String propertyName = null;
        switch(messageType) {
        case 0:
            propertyName = "OptionPane.errorIcon";
            break;
        case 1:
            propertyName = "OptionPane.informationIcon";
            break;
        case 2:
            propertyName = "OptionPane.warningIcon";
            break;
        case 3:
            propertyName = "OptionPane.questionIcon";
            break;
        }
        if (propertyName != null) {
            return (Icon)DefaultLookup.get(optionPane, this, propertyName);
        }
        return null;
    }

    /**
     * Returns the maximum number of characters to place on a line.
     *
     * @return the maximum number of characters to place on a line
     */
    protected int getMaxCharactersPerLineCount() {
        return optionPane.getMaxCharactersPerLineCount();
    }

    /**
     * Recursively creates new {@code JLabel} instances to represent {@code d}.
     * Each {@code JLabel} instance is added to {@code c}.
     *
     * @param c a container
     * @param d a text
     * @param maxll a maximum length of a text
     */
    protected void burstStringInto(Container c, String d, int maxll) {
        // Primitive line wrapping
        int len = d.length();
        if (len <= 0)
            return;
        if (len > maxll) {
            int p = d.lastIndexOf(' ', maxll);
            if (p <= 0)
                p = d.indexOf(' ', maxll);
            if (p > 0 && p < len) {
                burstStringInto(c, d.substring(0, p), maxll);
                burstStringInto(c, d.substring(p + 1), maxll);
                return;
            }
        }
        JLabel label = new JLabel(d, JLabel.LEFT);
        label.setName("OptionPane.label");
        configureMessageLabel(label);
        c.add(label);
    }

    /**
     * Returns a separator.
     *
     * @return a separator
     */
    protected Container createSeparator() {
        return null;
    }

    /**
     * Creates and returns a {@code Container} containing the buttons.
     * The buttons are created by calling {@code getButtons}.
     *
     * @return a {@code Container} containing the buttons
     */
    protected Container createButtonArea() {
        JPanel bottom = new JPanel();
        Border border = (Border)DefaultLookup.get(optionPane, this,
                                          "OptionPane.buttonAreaBorder");
        bottom.setName("OptionPane.buttonArea");
        if (border != null) {
            bottom.setBorder(border);
        }
        bottom.setLayout(new ButtonAreaLayout(
           DefaultLookup.getBoolean(optionPane, this,
                                    "OptionPane.sameSizeButtons", true),
           DefaultLookup.getInt(optionPane, this, "OptionPane.buttonPadding",
                                6),
           DefaultLookup.getInt(optionPane, this,
                        "OptionPane.buttonOrientation", SwingConstants.CENTER),
           DefaultLookup.getBoolean(optionPane, this, "OptionPane.isYesLast",
                                    false)));
        addButtonComponents(bottom, getButtons(), getInitialValueIndex());
        return bottom;
    }

    /**
     * Creates the appropriate object to represent each of the objects in
     * {@code buttons} and adds it to {@code container}. This
     * differs from addMessageComponents in that it will recurse on
     * {@code buttons} and that if button is not a Component
     * it will create an instance of JButton.
     *
     * @param container a container
     * @param buttons an array of buttons
     * @param initialIndex an initial index
     */
    protected void addButtonComponents(Container container, Object[] buttons,
                                 int initialIndex) {
        if (buttons != null && buttons.length > 0) {
            boolean            sizeButtonsToSame = getSizeButtonsToSameWidth();
            boolean            createdAll = true;
            int                numButtons = buttons.length;
            JButton[]          createdButtons = null;
            int                maxWidth = 0;

            if (sizeButtonsToSame) {
                createdButtons = new JButton[numButtons];
            }

            for(int counter = 0; counter < numButtons; counter++) {
                Object       button = buttons[counter];
                Component    newComponent;

                if (button instanceof Component) {
                    createdAll = false;
                    newComponent = (Component)button;
                    container.add(newComponent);
                    hasCustomComponents = true;

                } else {
                    JButton      aButton;

                    if (button instanceof ButtonFactory) {
                        aButton = ((ButtonFactory)button).createButton();
                    }
                    else if (button instanceof Icon)
                        aButton = new JButton((Icon)button);
                    else
                        aButton = new JButton(button.toString());

                    aButton.setName("OptionPane.button");
                    aButton.setMultiClickThreshhold(DefaultLookup.getInt(
                          optionPane, this, "OptionPane.buttonClickThreshhold",
                          0));
                    configureButton(aButton);

                    container.add(aButton);

                    ActionListener buttonListener = createButtonActionListener(counter);
                    if (buttonListener != null) {
                        aButton.addActionListener(buttonListener);
                    }
                    newComponent = aButton;
                }
                if (sizeButtonsToSame && createdAll &&
                   (newComponent instanceof JButton)) {
                    createdButtons[counter] = (JButton)newComponent;
                    maxWidth = Math.max(maxWidth,
                                        newComponent.getMinimumSize().width);
                }
                if (counter == initialIndex) {
                    initialFocusComponent = newComponent;
                    if (initialFocusComponent instanceof JButton) {
                        JButton defaultB = (JButton)initialFocusComponent;
                        defaultB.addHierarchyListener(new HierarchyListener() {
                            public void hierarchyChanged(HierarchyEvent e) {
                                if ((e.getChangeFlags() &
                                        HierarchyEvent.PARENT_CHANGED) != 0) {
                                    JButton defaultButton = (JButton) e.getComponent();
                                    JRootPane root =
                                            SwingUtilities.getRootPane(defaultButton);
                                    if (root != null) {
                                        root.setDefaultButton(defaultButton);
                                    }
                                }
                            }
                        });
                    }
                }
            }
            ((ButtonAreaLayout)container.getLayout()).
                              setSyncAllWidths((sizeButtonsToSame && createdAll));
            /* Set the padding, windows seems to use 8 if <= 2 components,
               otherwise 4 is used. It may actually just be the size of the
               buttons is always the same, not sure. */
            if (DefaultLookup.getBoolean(optionPane, this,
                   "OptionPane.setButtonMargin", true) && sizeButtonsToSame &&
                   createdAll) {
                JButton               aButton;
                int                   padSize;

                padSize = (numButtons <= 2? 8 : 4);

                for(int counter = 0; counter < numButtons; counter++) {
                    aButton = createdButtons[counter];
                    aButton.setMargin(new Insets(2, padSize, 2, padSize));
                }
            }
        }
    }

    /**
     * Constructs a new instance of a {@code ButtonActionListener}.
     *
     * @param buttonIndex an index of the button
     * @return a new instance of a {@code ButtonActionListener}
     */
    protected ActionListener createButtonActionListener(int buttonIndex) {
        return new ButtonActionListener(buttonIndex);
    }

    /**
     * Returns the buttons to display from the {@code JOptionPane} the receiver is
     * providing the look and feel for. If the {@code JOptionPane} has options
     * set, they will be provided, otherwise if the optionType is
     * {@code YES_NO_OPTION}, {@code yesNoOptions} is returned, if the type is
     * {@code YES_NO_CANCEL_OPTION} {@code yesNoCancelOptions} is returned, otherwise
     * {@code defaultButtons} are returned.
     *
     * @return the buttons to display from the JOptionPane
     */
    protected Object[] getButtons() {
        if (optionPane != null) {
            Object[] suppliedOptions = optionPane.getOptions();

            if (suppliedOptions == null) {
                Object[] defaultOptions;
                int type = optionPane.getOptionType();
                Locale l = optionPane.getLocale();
                int minimumWidth =
                    DefaultLookup.getInt(optionPane, this,
                                        "OptionPane.buttonMinimumWidth",-1);
                if (type == JOptionPane.YES_NO_OPTION) {
                    defaultOptions = new ButtonFactory[2];
                    defaultOptions[0] = new ButtonFactory(
                        UIManager.getString("OptionPane.yesButtonText", l),
                        getMnemonic("OptionPane.yesButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.yesIcon"), minimumWidth);
                    defaultOptions[1] = new ButtonFactory(
                        UIManager.getString("OptionPane.noButtonText", l),
                        getMnemonic("OptionPane.noButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.noIcon"), minimumWidth);
                } else if (type == JOptionPane.YES_NO_CANCEL_OPTION) {
                    defaultOptions = new ButtonFactory[3];
                    defaultOptions[0] = new ButtonFactory(
                        UIManager.getString("OptionPane.yesButtonText", l),
                        getMnemonic("OptionPane.yesButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.yesIcon"), minimumWidth);
                    defaultOptions[1] = new ButtonFactory(
                        UIManager.getString("OptionPane.noButtonText",l),
                        getMnemonic("OptionPane.noButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.noIcon"), minimumWidth);
                    defaultOptions[2] = new ButtonFactory(
                        UIManager.getString("OptionPane.cancelButtonText",l),
                        getMnemonic("OptionPane.cancelButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.cancelIcon"), minimumWidth);
                } else if (type == JOptionPane.OK_CANCEL_OPTION) {
                    defaultOptions = new ButtonFactory[2];
                    defaultOptions[0] = new ButtonFactory(
                        UIManager.getString("OptionPane.okButtonText",l),
                        getMnemonic("OptionPane.okButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.okIcon"), minimumWidth);
                    defaultOptions[1] = new ButtonFactory(
                        UIManager.getString("OptionPane.cancelButtonText",l),
                        getMnemonic("OptionPane.cancelButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.cancelIcon"), minimumWidth);
                } else {
                    defaultOptions = new ButtonFactory[1];
                    defaultOptions[0] = new ButtonFactory(
                        UIManager.getString("OptionPane.okButtonText",l),
                        getMnemonic("OptionPane.okButtonMnemonic", l),
                        (Icon)DefaultLookup.get(optionPane, this,
                                          "OptionPane.okIcon"), minimumWidth);
                }
                return defaultOptions;

            }
            return suppliedOptions;
        }
        return null;
    }

    private int getMnemonic(String key, Locale l) {
        String value = (String)UIManager.get(key, l);

        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException nfe) { }
        return 0;
    }

    /**
     * Returns {@code true}, basic L&amp;F wants all the buttons to have the same
     * width.
     *
     * @return {@code true} if all the buttons should have the same width
     */
    protected boolean getSizeButtonsToSameWidth() {
        return true;
    }

    /**
     * Returns the initial index into the buttons to select. The index
     * is calculated from the initial value from the JOptionPane and
     * options of the JOptionPane or 0.
     *
     * @return the initial index into the buttons to select
     */
    protected int getInitialValueIndex() {
        if (optionPane != null) {
            Object             iv = optionPane.getInitialValue();
            Object[]           options = optionPane.getOptions();

            if(options == null) {
                return 0;
            }
            else if(iv != null) {
                for(int counter = options.length - 1; counter >= 0; counter--){
                    if(options[counter].equals(iv))
                        return counter;
                }
            }
        }
        return -1;
    }

    /**
     * Sets the input value in the option pane the receiver is providing
     * the look and feel for based on the value in the inputComponent.
     */
    protected void resetInputValue() {
        if(inputComponent != null && (inputComponent instanceof JTextField)) {
            optionPane.setInputValue(((JTextField)inputComponent).getText());

        } else if(inputComponent != null &&
                  (inputComponent instanceof JComboBox)) {
            optionPane.setInputValue(((JComboBox)inputComponent)
                                     .getSelectedItem());
        } else if(inputComponent != null) {
            optionPane.setInputValue(((JList)inputComponent)
                                     .getSelectedValue());
        }
    }


    /**
     * If inputComponent is non-null, the focus is requested on that,
     * otherwise request focus on the default value
     */
    public void selectInitialValue(JOptionPane op) {
        if (inputComponent != null)
            inputComponent.requestFocus();
        else {
            if (initialFocusComponent != null)
                initialFocusComponent.requestFocus();

            if (initialFocusComponent instanceof JButton) {
                JRootPane root = SwingUtilities.getRootPane(initialFocusComponent);
                if (root != null) {
                    root.setDefaultButton((JButton)initialFocusComponent);
                }
            }
        }
    }

    /**
     * Returns true if in the last call to validateComponent the message
     * or buttons contained a subclass of Component.
     */
    public boolean containsCustomComponents(JOptionPane op) {
        return hasCustomComponents;
    }


    /**
     * <code>ButtonAreaLayout</code> behaves in a similar manner to
     * <code>FlowLayout</code>. It lays out all components from left to
     * right. If <code>syncAllWidths</code> is true, the widths of each
     * component will be set to the largest preferred size width.
     *
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of {@code BasicOptionPaneUI}.
     */
    public static class ButtonAreaLayout implements LayoutManager {
        /**
         * The value represents if the width of children should be synchronized.
         */
        protected boolean           syncAllWidths;
        /**
         * The padding value.
         */
        protected int               padding;
        /** If true, children are lumped together in parent. */
        protected boolean           centersChildren;
        private int orientation;
        private boolean reverseButtons;
        /**
         * Indicates whether or not centersChildren should be used vs
         * the orientation. This is done for backward compatibility
         * for subclassers.
         */
        private boolean useOrientation;

        /**
         * Constructs a new instance of {@code ButtonAreaLayout}.
         *
         * @param syncAllWidths if the width of children should be synchronized
         * @param padding the padding value
         */
        public ButtonAreaLayout(boolean syncAllWidths, int padding) {
            this.syncAllWidths = syncAllWidths;
            this.padding = padding;
            centersChildren = true;
            useOrientation = false;
        }

        ButtonAreaLayout(boolean syncAllSizes, int padding, int orientation,
                         boolean reverseButtons) {
            this(syncAllSizes, padding);
            useOrientation = true;
            this.orientation = orientation;
            this.reverseButtons = reverseButtons;
        }

        /**
         * Sets if the width of children should be synchronized.
         *
         * @param newValue if the width of children should be synchronized
         */
        public void setSyncAllWidths(boolean newValue) {
            syncAllWidths = newValue;
        }

        /**
         * Returns if the width of children should be synchronized.
         *
         * @return if the width of children should be synchronized
         */
        public boolean getSyncAllWidths() {
            return syncAllWidths;
        }

        /**
         * Sets the padding value.
         *
         * @param newPadding the new padding
         */
        public void setPadding(int newPadding) {
            this.padding = newPadding;
        }

        /**
         * Returns the padding.
         *
         * @return the padding
         */
        public int getPadding() {
            return padding;
        }

        /**
         * Sets whether or not center children should be used.
         *
         * @param newValue a new value
         */
        public void setCentersChildren(boolean newValue) {
            centersChildren = newValue;
            useOrientation = false;
        }

        /**
         * Returns whether or not center children should be used.
         *
         * @return whether or not center children should be used
         */
        public boolean getCentersChildren() {
            return centersChildren;
        }

        private int getOrientation(Container container) {
            if (!useOrientation) {
                return SwingConstants.CENTER;
            }
            if (container.getComponentOrientation().isLeftToRight()) {
                return orientation;
            }
            switch (orientation) {
            case SwingConstants.LEFT:
                return SwingConstants.RIGHT;
            case SwingConstants.RIGHT:
                return SwingConstants.LEFT;
            case SwingConstants.CENTER:
                return SwingConstants.CENTER;
            }
            return SwingConstants.LEFT;
        }

        public void addLayoutComponent(String string, Component comp) {
        }

        public void layoutContainer(Container container) {
            Component[]      children = container.getComponents();

            if(children != null && children.length > 0) {
                int               numChildren = children.length;
                Insets            insets = container.getInsets();
                int maxWidth = 0;
                int maxHeight = 0;
                int totalButtonWidth = 0;
                int x = 0;
                int xOffset = 0;
                boolean ltr = container.getComponentOrientation().
                                        isLeftToRight();
                boolean reverse = (ltr) ? reverseButtons : !reverseButtons;

                for(int counter = 0; counter < numChildren; counter++) {
                    Dimension pref = children[counter].getPreferredSize();
                    maxWidth = Math.max(maxWidth, pref.width);
                    maxHeight = Math.max(maxHeight, pref.height);
                    totalButtonWidth += pref.width;
                }
                if (getSyncAllWidths()) {
                    totalButtonWidth = maxWidth * numChildren;
                }
                totalButtonWidth += (numChildren - 1) * padding;

                switch (getOrientation(container)) {
                case SwingConstants.LEFT:
                    x = insets.left;
                    break;
                case SwingConstants.RIGHT:
                    x = container.getWidth() - insets.right - totalButtonWidth;
                    break;
                case SwingConstants.CENTER:
                    if (getCentersChildren() || numChildren < 2) {
                        x = (container.getWidth() - totalButtonWidth) / 2;
                    }
                    else {
                        x = insets.left;
                        if (getSyncAllWidths()) {
                            xOffset = (container.getWidth() - insets.left -
                                       insets.right - totalButtonWidth) /
                                (numChildren - 1) + maxWidth;
                        }
                        else {
                            xOffset = (container.getWidth() - insets.left -
                                       insets.right - totalButtonWidth) /
                                      (numChildren - 1);
                        }
                    }
                    break;
                }

                for (int counter = 0; counter < numChildren; counter++) {
                    int index = (reverse) ? numChildren - counter - 1 :
                                counter;
                    Dimension pref = children[index].getPreferredSize();

                    if (getSyncAllWidths()) {
                        children[index].setBounds(x, insets.top,
                                                  maxWidth, maxHeight);
                    }
                    else {
                        children[index].setBounds(x, insets.top, pref.width,
                                                  pref.height);
                    }
                    if (xOffset != 0) {
                        x += xOffset;
                    }
                    else {
                        x += children[index].getWidth() + padding;
                    }
                }
            }
        }

        public Dimension minimumLayoutSize(Container c) {
            if(c != null) {
                Component[]       children = c.getComponents();

                if(children != null && children.length > 0) {
                    Dimension     aSize;
                    int           numChildren = children.length;
                    int           height = 0;
                    Insets        cInsets = c.getInsets();
                    int           extraHeight = cInsets.top + cInsets.bottom;
                    int           extraWidth = cInsets.left + cInsets.right;

                    if (syncAllWidths) {
                        int              maxWidth = 0;

                        for(int counter = 0; counter < numChildren; counter++){
                            aSize = children[counter].getPreferredSize();
                            height = Math.max(height, aSize.height);
                            maxWidth = Math.max(maxWidth, aSize.width);
                        }
                        return new Dimension(extraWidth + (maxWidth * numChildren) +
                                             (numChildren - 1) * padding,
                                             extraHeight + height);
                    }
                    else {
                        int        totalWidth = 0;

                        for(int counter = 0; counter < numChildren; counter++){
                            aSize = children[counter].getPreferredSize();
                            height = Math.max(height, aSize.height);
                            totalWidth += aSize.width;
                        }
                        totalWidth += ((numChildren - 1) * padding);
                        return new Dimension(extraWidth + totalWidth, extraHeight + height);
                    }
                }
            }
            return new Dimension(0, 0);
        }

        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }

        public void removeLayoutComponent(Component c) { }
    }


    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of {@code BasicOptionPaneUI}.
     */
    public class PropertyChangeHandler implements PropertyChangeListener {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        /**
         * If the source of the PropertyChangeEvent <code>e</code> equals the
         * optionPane and is one of the ICON_PROPERTY, MESSAGE_PROPERTY,
         * OPTIONS_PROPERTY or INITIAL_VALUE_PROPERTY,
         * validateComponent is invoked.
         */
        public void propertyChange(PropertyChangeEvent e) {
            getHandler().propertyChange(e);
        }
    }

    /**
     * Configures any necessary colors/fonts for the specified label
     * used representing the message.
     */
    private void configureMessageLabel(JLabel label) {
        Color color = (Color)DefaultLookup.get(optionPane, this,
                                               "OptionPane.messageForeground");
        if (color != null) {
            label.setForeground(color);
        }
        Font messageFont = (Font)DefaultLookup.get(optionPane, this,
                                                   "OptionPane.messageFont");
        if (messageFont != null) {
            label.setFont(messageFont);
        }
    }

    /**
     * Configures any necessary colors/fonts for the specified button
     * used representing the button portion of the optionpane.
     */
    private void configureButton(JButton button) {
        Font buttonFont = (Font)DefaultLookup.get(optionPane, this,
                                            "OptionPane.buttonFont");
        if (buttonFont != null) {
            button.setFont(buttonFont);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of {@code BasicOptionPaneUI}.
     */
    public class ButtonActionListener implements ActionListener {
        /**
         * The index of the button.
         */
        protected int buttonIndex;

        /**
         * Constructs a new instance of {@code ButtonActionListener}.
         *
         * @param buttonIndex an index of the button
         */
        public ButtonActionListener(int buttonIndex) {
            this.buttonIndex = buttonIndex;
        }

        public void actionPerformed(ActionEvent e) {
            if (optionPane != null) {
                int optionType = optionPane.getOptionType();
                Object[] options = optionPane.getOptions();

                /* If the option pane takes input, then store the input value
                 * if custom options were specified, if the option type is
                 * DEFAULT_OPTION, OR if option type is set to a predefined
                 * one and the user chose the affirmative answer.
                 */
                if (inputComponent != null) {
                    if (options != null ||
                        optionType == JOptionPane.DEFAULT_OPTION ||
                        ((optionType == JOptionPane.YES_NO_OPTION ||
                         optionType == JOptionPane.YES_NO_CANCEL_OPTION ||
                         optionType == JOptionPane.OK_CANCEL_OPTION) &&
                         buttonIndex == 0)) {
                        resetInputValue();
                    }
                }
                if (options == null) {
                    if (optionType == JOptionPane.OK_CANCEL_OPTION &&
                        buttonIndex == 1) {
                        optionPane.setValue(Integer.valueOf(2));

                    } else {
                        optionPane.setValue(Integer.valueOf(buttonIndex));
                    }
                } else {
                    optionPane.setValue(options[buttonIndex]);
                }
            }
        }
    }


    private class Handler implements ActionListener, MouseListener,
                                     PropertyChangeListener {
        //
        // ActionListener
        //
        public void actionPerformed(ActionEvent e) {
            optionPane.setInputValue(((JTextField)e.getSource()).getText());
        }


        //
        // MouseListener
        //
        public void mouseClicked(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getClickCount() == 2) {
                JList<?>  list = (JList)e.getSource();
                int       index = list.locationToIndex(e.getPoint());

                optionPane.setInputValue(list.getModel().getElementAt(index));
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        }

        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e) {
            if(e.getSource() == optionPane) {
                // Option Pane Auditory Cue Activation
                // only respond to "ancestor" changes
                // the idea being that a JOptionPane gets a JDialog when it is
                // set to appear and loses it's JDialog when it is dismissed.
                if ("ancestor" == e.getPropertyName()) {
                    JOptionPane op = (JOptionPane)e.getSource();
                    boolean isComingUp;

                    // if the old value is null, then the JOptionPane is being
                    // created since it didn't previously have an ancestor.
                    if (e.getOldValue() == null) {
                        isComingUp = true;
                    } else {
                        isComingUp = false;
                    }

                    // figure out what to do based on the message type
                    switch (op.getMessageType()) {
                    case JOptionPane.PLAIN_MESSAGE:
                        if (isComingUp) {
                            BasicLookAndFeel.playSound(optionPane,
                                               "OptionPane.informationSound");
                        }
                        break;
                    case JOptionPane.QUESTION_MESSAGE:
                        if (isComingUp) {
                            BasicLookAndFeel.playSound(optionPane,
                                             "OptionPane.questionSound");
                        }
                        break;
                    case JOptionPane.INFORMATION_MESSAGE:
                        if (isComingUp) {
                            BasicLookAndFeel.playSound(optionPane,
                                             "OptionPane.informationSound");
                        }
                        break;
                    case JOptionPane.WARNING_MESSAGE:
                        if (isComingUp) {
                            BasicLookAndFeel.playSound(optionPane,
                                             "OptionPane.warningSound");
                        }
                        break;
                    case JOptionPane.ERROR_MESSAGE:
                        if (isComingUp) {
                            BasicLookAndFeel.playSound(optionPane,
                                             "OptionPane.errorSound");
                        }
                        break;
                    default:
                        System.err.println("Undefined JOptionPane type: " +
                                           op.getMessageType());
                        break;
                    }
                }
                // Visual activity
                String         changeName = e.getPropertyName();

                if(changeName == JOptionPane.OPTIONS_PROPERTY ||
                   changeName == JOptionPane.INITIAL_VALUE_PROPERTY ||
                   changeName == JOptionPane.ICON_PROPERTY ||
                   changeName == JOptionPane.MESSAGE_TYPE_PROPERTY ||
                   changeName == JOptionPane.OPTION_TYPE_PROPERTY ||
                   changeName == JOptionPane.MESSAGE_PROPERTY ||
                   changeName == JOptionPane.SELECTION_VALUES_PROPERTY ||
                   changeName == JOptionPane.INITIAL_SELECTION_VALUE_PROPERTY ||
                   changeName == JOptionPane.WANTS_INPUT_PROPERTY) {
                   uninstallComponents();
                   installComponents();
                   optionPane.validate();
                }
                else if (changeName == "componentOrientation") {
                    ComponentOrientation o = (ComponentOrientation)e.getNewValue();
                    JOptionPane op = (JOptionPane)e.getSource();
                    if (o != e.getOldValue()) {
                        op.applyComponentOrientation(o);
                    }
                }
            }
        }
    }


    //
    // Classes used when optionPane.getWantsInput returns true.
    //

    /**
     * A JTextField that allows you to specify an array of KeyStrokes that
     * that will have their bindings processed regardless of whether or
     * not they are registered on the JTextField. This is used as we really
     * want the ActionListener to be notified so that we can push the
     * change to the JOptionPane, but we also want additional bindings
     * (those of the JRootPane) to be processed as well.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class MultiplexingTextField extends JTextField {
        private KeyStroke[] strokes;

        MultiplexingTextField(int cols) {
            super(cols);
        }

        /**
         * Sets the KeyStrokes that will be additional processed for
         * ancestor bindings.
         */
        void setKeyStrokes(KeyStroke[] strokes) {
            this.strokes = strokes;
        }

        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                            int condition, boolean pressed) {
            boolean processed = super.processKeyBinding(ks, e, condition,
                                                        pressed);

            if (processed && condition != JComponent.WHEN_IN_FOCUSED_WINDOW) {
                for (int counter = strokes.length - 1; counter >= 0;
                         counter--) {
                    if (strokes[counter].equals(ks)) {
                        // Returning false will allow further processing
                        // of the bindings, eg our parent Containers will get a
                        // crack at them.
                        return false;
                    }
                }
            }
            return processed;
        }
    }



    /**
     * Registered in the ActionMap. Sets the value of the option pane
     * to <code>JOptionPane.CLOSED_OPTION</code>.
     */
    private static class Actions extends UIAction {
        private static final String CLOSE = "close";

        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            if (getName() == CLOSE) {
                JOptionPane optionPane = (JOptionPane)e.getSource();

                optionPane.setValue(Integer.valueOf(JOptionPane.CLOSED_OPTION));
            }
        }
    }


    /**
     * This class is used to create the default buttons. This indirection is
     * used so that addButtonComponents can tell which Buttons were created
     * by us vs subclassers or from the JOptionPane itself.
     */
    private static class ButtonFactory {
        private String text;
        private int mnemonic;
        private Icon icon;
        private int minimumWidth = -1;

        ButtonFactory(String text, int mnemonic, Icon icon, int minimumWidth) {
            this.text = text;
            this.mnemonic = mnemonic;
            this.icon = icon;
            this.minimumWidth = minimumWidth;
        }

        JButton createButton() {
            JButton button;

            if (minimumWidth > 0) {
                button = new ConstrainedButton(text, minimumWidth);
            } else {
                button = new JButton(text);
            }
            if (icon != null) {
                button.setIcon(icon);
            }
            if (mnemonic != 0) {
                button.setMnemonic(mnemonic);
            }
            return button;
        }

        @SuppressWarnings("serial") // Superclass is not serializable across versions
        private static class ConstrainedButton extends JButton {
            int minimumWidth;

            ConstrainedButton(String text, int minimumWidth) {
                super(text);
                this.minimumWidth = minimumWidth;
            }

            public Dimension getMinimumSize() {
                Dimension min = super.getMinimumSize();
                min.width = Math.max(min.width, minimumWidth);
                return min;
            }

            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                pref.width = Math.max(pref.width, minimumWidth);
                return pref;
            }
        }
    }
}
