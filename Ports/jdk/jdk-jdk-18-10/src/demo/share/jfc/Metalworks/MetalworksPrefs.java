/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;


/**
 * This is dialog which allows users to choose preferences
 *
 * @author Steve Wilson
 * @author Alexander Kouznetsov
 */
@SuppressWarnings("serial")
public final class MetalworksPrefs extends JDialog {

    public MetalworksPrefs(JFrame f) {
        super(f, "Preferences", true);
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        JPanel filters = buildFilterPanel();
        JPanel conn = buildConnectingPanel();
        tabs.addTab("Filters", null, filters);
        tabs.addTab("Connecting", null, conn);


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                CancelPressed();
            }
        });
        buttonPanel.add(cancel);
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                OKPressed();
            }
        });
        buttonPanel.add(ok);
        getRootPane().setDefaultButton(ok);

        container.add(tabs, BorderLayout.CENTER);
        container.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(container);
        pack();
        centerDialog();
        UIManager.addPropertyChangeListener(new UISwitchListener(container));
    }

    public JPanel buildFilterPanel() {
        JPanel filters = new JPanel();
        filters.setLayout(new GridLayout(1, 0));

        JPanel spamPanel = new JPanel();

        spamPanel.setLayout(new ColumnLayout());
        spamPanel.setBorder(new TitledBorder("Spam"));
        ButtonGroup spamGroup = new ButtonGroup();
        JRadioButton file = new JRadioButton("File in Spam Folder");
        JRadioButton delete = new JRadioButton("Auto Delete");
        JRadioButton bomb = new JRadioButton("Reverse Mail-Bomb");
        spamGroup.add(file);
        spamGroup.add(delete);
        spamGroup.add(bomb);
        spamPanel.add(file);
        spamPanel.add(delete);
        spamPanel.add(bomb);
        file.setSelected(true);
        filters.add(spamPanel);

        JPanel autoRespond = new JPanel();
        autoRespond.setLayout(new ColumnLayout());
        autoRespond.setBorder(new TitledBorder("Auto Response"));

        ButtonGroup respondGroup = new ButtonGroup();
        JRadioButton none = new JRadioButton("None");
        JRadioButton vaca = new JRadioButton("Send Vacation Message");
        JRadioButton thx = new JRadioButton("Send Thank You Message");

        respondGroup.add(none);
        respondGroup.add(vaca);
        respondGroup.add(thx);

        autoRespond.add(none);
        autoRespond.add(vaca);
        autoRespond.add(thx);

        none.setSelected(true);
        filters.add(autoRespond);

        return filters;
    }

    public JPanel buildConnectingPanel() {
        JPanel connectPanel = new JPanel();
        connectPanel.setLayout(new ColumnLayout());

        JPanel protoPanel = new JPanel();
        JLabel protoLabel = new JLabel("Protocol");
        JComboBox<String> protocol = new JComboBox<>();
        protocol.addItem("SMTP");
        protocol.addItem("IMAP");
        protocol.addItem("Other...");
        protoPanel.add(protoLabel);
        protoPanel.add(protocol);

        JPanel attachmentPanel = new JPanel();
        JLabel attachmentLabel = new JLabel("Attachments");
        JComboBox<String> attach = new JComboBox<>();
        attach.addItem("Download Always");
        attach.addItem("Ask size > 1 Meg");
        attach.addItem("Ask size > 5 Meg");
        attach.addItem("Ask Always");
        attachmentPanel.add(attachmentLabel);
        attachmentPanel.add(attach);

        JCheckBox autoConn = new JCheckBox("Auto Connect");
        JCheckBox compress = new JCheckBox("Use Compression");
        autoConn.setSelected(true);

        connectPanel.add(protoPanel);
        connectPanel.add(attachmentPanel);
        connectPanel.add(autoConn);
        connectPanel.add(compress);
        return connectPanel;
    }

    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height / 2;
        screenSize.width = screenSize.width / 2;
        size.height = size.height / 2;
        size.width = size.width / 2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x, y);
    }

    public void CancelPressed() {
        this.setVisible(false);
    }

    public void OKPressed() {
        this.setVisible(false);
    }
}


class ColumnLayout implements LayoutManager {

    int xInset = 5;
    int yInset = 5;
    int yGap = 2;

    public void addLayoutComponent(String s, Component c) {
    }

    public void layoutContainer(Container c) {
        Insets insets = c.getInsets();
        int height = yInset + insets.top;

        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (Component child : children) {
            compSize = child.getPreferredSize();
            child.setSize(compSize.width, compSize.height);
            child.setLocation(xInset + insets.left, height);
            height += compSize.height + yGap;
        }

    }

    public Dimension minimumLayoutSize(Container c) {
        Insets insets = c.getInsets();
        int height = yInset + insets.top;
        int width = 0 + insets.left + insets.right;

        Component[] children = c.getComponents();
        Dimension compSize = null;
        for (Component child : children) {
            compSize = child.getPreferredSize();
            height += compSize.height + yGap;
            width = Math.max(width, compSize.width + insets.left + insets.right + xInset
                    * 2);
        }
        height += insets.bottom;
        return new Dimension(width, height);
    }

    public Dimension preferredLayoutSize(Container c) {
        return minimumLayoutSize(c);
    }

    public void removeLayoutComponent(Component c) {
    }
}
