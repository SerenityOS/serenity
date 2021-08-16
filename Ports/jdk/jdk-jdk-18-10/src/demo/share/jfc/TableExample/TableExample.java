/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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



/**
 * A a UI around the JDBCAdaptor, allowing database data to be interactively
 * fetched, sorted and displayed using Swing.
 *
 * NOTE: This example uses a modal dialog via the static convenience methods in
 * the JOptionPane. Use of modal dialogs requires JDK 1.1.4 or greater.
 *
 * @author Philip Milne
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;


public final class TableExample implements LayoutManager {

    static String[] ConnectOptionNames = { "Connect" };
    static String ConnectTitle = "Connection Information";
    Dimension origin = new Dimension(0, 0);
    JButton fetchButton;
    JButton showConnectionInfoButton;
    JPanel connectionPanel;
    JFrame frame; // The query/results window.
    JLabel userNameLabel;
    JTextField userNameField;
    JLabel passwordLabel;
    JTextField passwordField;
    // JLabel      queryLabel;
    JTextArea queryTextArea;
    JComponent queryAggregate;
    JLabel serverLabel;
    JTextField serverField;
    JLabel driverLabel;
    JTextField driverField;
    JPanel mainPanel;
    TableSorter sorter;
    JDBCAdapter dataBase;
    JScrollPane tableAggregate;

    /**
     * Brigs up a JDialog using JOptionPane containing the connectionPanel.
     * If the user clicks on the 'Connect' button the connection is reset.
     */
    void activateConnectionDialog() {
        if (JOptionPane.showOptionDialog(tableAggregate, connectionPanel,
                ConnectTitle,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, ConnectOptionNames, ConnectOptionNames[0]) == 0) {
            connect();
            frame.setVisible(true);
        } else if (!frame.isVisible()) {
            System.exit(0);
        }
    }

    /**
     * Creates the connectionPanel, which will contain all the fields for
     * the connection information.
     */
    public void createConnectionDialog() {
        // Create the labels and text fields.
        userNameLabel = new JLabel("User name: ", JLabel.RIGHT);
        userNameField = new JTextField("app");

        passwordLabel = new JLabel("Password: ", JLabel.RIGHT);
        passwordField = new JTextField("app");

        serverLabel = new JLabel("Database URL: ", JLabel.RIGHT);
        serverField = new JTextField("jdbc:derby://localhost:1527/sample");

        driverLabel = new JLabel("Driver: ", JLabel.RIGHT);
        driverField = new JTextField("org.apache.derby.jdbc.ClientDriver");


        connectionPanel = new JPanel(false);
        connectionPanel.setLayout(new BoxLayout(connectionPanel,
                BoxLayout.X_AXIS));

        JPanel namePanel = new JPanel(false);
        namePanel.setLayout(new GridLayout(0, 1));
        namePanel.add(userNameLabel);
        namePanel.add(passwordLabel);
        namePanel.add(serverLabel);
        namePanel.add(driverLabel);

        JPanel fieldPanel = new JPanel(false);
        fieldPanel.setLayout(new GridLayout(0, 1));
        fieldPanel.add(userNameField);
        fieldPanel.add(passwordField);
        fieldPanel.add(serverField);
        fieldPanel.add(driverField);

        connectionPanel.add(namePanel);
        connectionPanel.add(fieldPanel);
    }

    public TableExample() {
        mainPanel = new JPanel();

        // Create the panel for the connection information
        createConnectionDialog();

        // Create the buttons.
        showConnectionInfoButton = new JButton("Configuration");
        showConnectionInfoButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                activateConnectionDialog();
            }
        });

        fetchButton = new JButton("Fetch");
        fetchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fetch();
            }
        });

        // Create the query text area and label.
        queryTextArea = new JTextArea("SELECT * FROM APP.CUSTOMER", 25, 25);
        queryAggregate = new JScrollPane(queryTextArea);
        queryAggregate.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // Create the table.
        tableAggregate = createTable();
        tableAggregate.setBorder(new BevelBorder(BevelBorder.LOWERED));

        // Add all the components to the main panel.
        mainPanel.add(fetchButton);
        mainPanel.add(showConnectionInfoButton);
        mainPanel.add(queryAggregate);
        mainPanel.add(tableAggregate);
        mainPanel.setLayout(this);

        // Create a Frame and put the main panel in it.
        frame = new JFrame("TableExample");
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setBackground(Color.lightGray);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(false);
        frame.setBounds(200, 200, 640, 480);

        activateConnectionDialog();
    }

    public void connect() {
        dataBase = new JDBCAdapter(
                serverField.getText(),
                driverField.getText(),
                userNameField.getText(),
                passwordField.getText());
        sorter.setModel(dataBase);
    }

    public void fetch() {
        dataBase.executeQuery(queryTextArea.getText());
    }

    public JScrollPane createTable() {
        sorter = new TableSorter();

        //connect();
        //fetch();

        // Create the table
        JTable table = new JTable(sorter);
        // Use a scrollbar, in case there are many columns.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Install a mouse listener in the TableHeader as the sorter UI.
        sorter.addMouseListenerToHeaderInTable(table);

        JScrollPane scrollpane = new JScrollPane(table);

        return scrollpane;
    }

    public static void main(String[] s) {
        // Trying to set Nimbus look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TableExample.class.getName()).log(Level.SEVERE,
                    "Failed to apply Nimbus look and feel", ex);
        }

        new TableExample();
    }

    public Dimension preferredLayoutSize(Container c) {
        return origin;
    }

    public Dimension minimumLayoutSize(Container c) {
        return origin;
    }

    public void addLayoutComponent(String s, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

    public void layoutContainer(Container c) {
        Rectangle b = c.getBounds();
        int topHeight = 90;
        int inset = 4;
        showConnectionInfoButton.setBounds(b.width - 2 * inset - 120, inset, 120,
                25);
        fetchButton.setBounds(b.width - 2 * inset - 120, 60, 120, 25);
        // queryLabel.setBounds(10, 10, 100, 25);
        queryAggregate.setBounds(inset, inset, b.width - 2 * inset - 150, 80);
        tableAggregate.setBounds(new Rectangle(inset,
                inset + topHeight,
                b.width - 2 * inset,
                b.height - 2 * inset - topHeight));
    }
}
