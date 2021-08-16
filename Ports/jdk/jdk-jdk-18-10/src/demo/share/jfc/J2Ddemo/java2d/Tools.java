/*
 *
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java2d;


import static java.awt.Color.BLACK;
import static java.awt.Color.GREEN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Tools to control individual demo graphic attributes.  Also, control for
 * start & stop on animated demos; control for cloning the demo; control for
 * printing the demo.  Expand and collapse the Tools panel with ToggleIcon.
 */
@SuppressWarnings("serial")
public final class Tools extends JPanel implements ActionListener,
        ChangeListener, Runnable {
    private final DemoInstVarsAccessor demoInstVars;
    private ImageIcon stopIcon, startIcon;
    private Font font = new Font(Font.SERIF, Font.PLAIN, 10);
    private Color roColor = new Color(187, 213, 238);
    private Surface surface;
    private Thread thread;
    private JPanel toolbarPanel;
    private JPanel sliderPanel;
    private JLabel label;
    private ToggleIcon bumpyIcon, rolloverIcon;
    private DecimalFormat decimalFormat = new DecimalFormat("000");
    protected boolean focus;
    public JToggleButton toggleB;
    public JButton printB;
    public JComboBox<String> screenCombo;
    public JToggleButton renderB, aliasB;
    public JToggleButton textureB, compositeB;
    public JButton startStopB;
    public JButton cloneB;
    public boolean issueRepaint = true;
    public JToolBar toolbar;
    public JSlider slider;
    public boolean doSlider;
    public boolean isExpanded;

    @SuppressWarnings("LeakingThisInConstructor")
    public Tools(Surface surface, DemoInstVarsAccessor demoInstVars) {
        this.surface = surface;
        this.demoInstVars = demoInstVars;

        setLayout(new BorderLayout());

        stopIcon = new ImageIcon(DemoImages.getImage("stop.gif", this));
        startIcon = new ImageIcon(DemoImages.getImage("start.gif", this));
        bumpyIcon = new ToggleIcon(this, LIGHT_GRAY);
        rolloverIcon = new ToggleIcon(this, roColor);
        toggleB = new JToggleButton(bumpyIcon);
        toggleB.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                focus = true;
                bumpyIcon.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                focus = false;
                bumpyIcon.stop();
            }
        });
        isExpanded = false;
        toggleB.addActionListener(this);
        toggleB.setMargin(new Insets(0, 0, -4, 0));
        toggleB.setBorderPainted(false);
        toggleB.setFocusPainted(false);
        toggleB.setContentAreaFilled(false);
        toggleB.setRolloverIcon(rolloverIcon);
        add("North", toggleB);

        toolbar = new JToolBar();
        toolbar.setPreferredSize(new Dimension(5*25, 26));
        toolbar.setFloatable(false);

        String s = surface.AntiAlias == RenderingHints.VALUE_ANTIALIAS_ON
                ? "On" : "Off";
        aliasB = addTool("A", "Antialiasing " + s, this);

        s = surface.Rendering == RenderingHints.VALUE_RENDER_SPEED
                ? "Speed" : "Quality";
        renderB = addTool("R", "Rendering " + s, this);

        s = surface.texture != null ? "On" : "Off";
        textureB = addTool("T", "Texture " + s, this);

        s = surface.composite != null ? "On" : "Off";
        compositeB = addTool("C", "Composite " + s, this);

        Image printBImg = DemoImages.getImage("print.gif", this);
        printB = addTool(printBImg, "Print the Surface", this);

        if (surface instanceof AnimatingSurface) {
            Image stopImg = DemoImages.getImage("stop.gif", this);
            startStopB = addTool(stopImg, "Stop Animation", this);
            toolbar.setPreferredSize(new Dimension(6*25, 26));
        }

        screenCombo = new JComboBox<>();
        screenCombo.setPreferredSize(new Dimension(100, 18));
        screenCombo.setFont(font);
        for (String name : GlobalControls.screenNames) {
            screenCombo.addItem(name);
        }
        screenCombo.addActionListener(this);
        toolbarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        toolbarPanel.setLocation(0, 6);
        toolbarPanel.setVisible(false);
        toolbarPanel.add(toolbar);
        toolbarPanel.add(screenCombo);
        toolbarPanel.setBorder(new EtchedBorder());
        add(toolbarPanel);

        setPreferredSize(new Dimension(200, 8));

        if (surface instanceof AnimatingSurface) {
            sliderPanel = new JPanel(new BorderLayout());
            label = new JLabel(" Sleep = 030 ms");
            label.setForeground(BLACK);
            sliderPanel.add(label, BorderLayout.WEST);
            slider = new JSlider(SwingConstants.HORIZONTAL, 0, 200, 30);
            slider.addChangeListener(this);
            sliderPanel.setBorder(new EtchedBorder());
            sliderPanel.add(slider);

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (toolbarPanel.isVisible()) {
                        invalidate();
                        if ((doSlider = !doSlider)) {
                            remove(toolbarPanel);
                            add(sliderPanel);
                        } else {
                            remove(sliderPanel);
                            add(toolbarPanel);
                        }
                        validate();
                        repaint();
                    }
                }
            });
        }
    }

    public JButton addTool(Image img,
            String toolTip,
            ActionListener al) {
        JButton b = new JButton(new ImageIcon(img)) {

            Dimension prefSize = new Dimension(25, 22);

            @Override
            public Dimension getPreferredSize() {
                return prefSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return prefSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return prefSize;
            }
        };
        toolbar.add(b);
        b.setFocusPainted(false);
        b.setSelected(true);
        b.setToolTipText(toolTip);
        b.addActionListener(al);
        return b;
    }

    public JToggleButton addTool(String name,
            String toolTip,
            ActionListener al) {
        JToggleButton b = new JToggleButton(name) {

            Dimension prefSize = new Dimension(25, 22);

            @Override
            public Dimension getPreferredSize() {
                return prefSize;
            }

            @Override
            public Dimension getMaximumSize() {
                return prefSize;
            }

            @Override
            public Dimension getMinimumSize() {
                return prefSize;
            }
        };
        toolbar.add(b);
        b.setFocusPainted(false);
        if (toolTip.equals("Rendering Quality") || toolTip.equals(
                "Antialiasing On") || toolTip.equals("Texture On") || toolTip.
                equals("Composite On")) {
            b.setSelected(true);
        } else {
            b.setSelected(false);
        }
        b.setToolTipText(toolTip);
        b.addActionListener(al);
        return b;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object obj = e.getSource();
        if (obj instanceof JButton) {
            JButton b = (JButton) obj;
            b.setSelected(!b.isSelected());
            if (b.getIcon() == null) {
                b.setBackground(b.isSelected() ? GREEN : LIGHT_GRAY);
            }
        }
        if (obj.equals(toggleB)) {
            isExpanded = !isExpanded;
            if (isExpanded) {
                setPreferredSize(new Dimension(200, 38));
            } else {
                setPreferredSize(new Dimension(200, 6));
            }
            toolbarPanel.setVisible(isExpanded);
            if (sliderPanel != null) {
                sliderPanel.setVisible(isExpanded);
            }
            getParent().validate();
            toggleB.getModel().setRollover(false);
            return;
        }
        if (obj.equals(printB)) {
            start();
            return;
        }

        if (obj.equals(startStopB)) {
            if (startStopB.getToolTipText().equals("Stop Animation")) {
                startStopB.setIcon(startIcon);
                startStopB.setToolTipText("Start Animation");
                surface.animating.stop();
            } else {
                startStopB.setIcon(stopIcon);
                startStopB.setToolTipText("Stop Animation");
                surface.animating.start();
            }
        } else if (obj.equals(aliasB)) {
            if (aliasB.getToolTipText().equals("Antialiasing On")) {
                aliasB.setToolTipText("Antialiasing Off");
            } else {
                aliasB.setToolTipText("Antialiasing On");
            }
            surface.setAntiAlias(aliasB.isSelected());
        } else if (obj.equals(renderB)) {
            if (renderB.getToolTipText().equals("Rendering Quality")) {
                renderB.setToolTipText("Rendering Speed");
            } else {
                renderB.setToolTipText("Rendering Quality");
            }
            surface.setRendering(renderB.isSelected());
        } else if (obj.equals(textureB)) {
            if (textureB.getToolTipText().equals("Texture On")) {
                textureB.setToolTipText("Texture Off");
                surface.setTexture(null);
                surface.clearSurface = true;
            } else {
                textureB.setToolTipText("Texture On");
                surface.setTexture(demoInstVars.getControls().texturechooser.texture);
            }
        } else if (obj.equals(compositeB)) {
            if (compositeB.getToolTipText().equals("Composite On")) {
                compositeB.setToolTipText("Composite Off");
            } else {
                compositeB.setToolTipText("Composite On");
            }
            surface.setComposite(compositeB.isSelected());
        } else if (obj.equals(screenCombo)) {
            surface.setImageType(screenCombo.getSelectedIndex());
        }

        if (issueRepaint && surface.animating != null) {
            if (surface.getSleepAmount() != 0) {
                if (surface.animating.running()) {
                    surface.animating.doRepaint();
                }
            }
        } else if (issueRepaint) {
            surface.repaint();
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int value = slider.getValue();
        label.setText(" Sleep = " + decimalFormat.format(value) + " ms");
        label.repaint();
        surface.setSleepAmount(value);
    }

    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setName("Printing " + surface.name);
        thread.start();
    }

    public synchronized void stop() {
        thread = null;
        notifyAll();
    }

    @Override
    public void run() {
        boolean stopped = false;
        if (surface.animating != null && surface.animating.running()) {
            stopped = true;
            startStopB.doClick();
        }

        try {
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(surface);
            boolean pDialogState = true;
            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

            if (!demoInstVars.getPrintCB().isSelected()) {
                pDialogState = printJob.printDialog(aset);
            }
            if (pDialogState) {
                printJob.print(aset);
            }
        } catch (@SuppressWarnings("removal") java.security.AccessControlException ace) {
            String errmsg = "Applet access control exception; to allow "
                    + "access to printer, set\n"
                    + "permission for \"queuePrintJob\" in "
                    + "RuntimePermission.";
            JOptionPane.showMessageDialog(this, errmsg, "Printer Access Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(Tools.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        if (stopped) {
            startStopB.doClick();
        }
        thread = null;
    }


    /**
     * Expand and Collapse the Tools Panel with this bumpy button.
     */
    static class ToggleIcon implements Icon, Runnable {

        private Color shadowColor = new Color(102, 102, 153);
        private Color fillColor;
        private Tools tools;
        private Thread thread;

        public ToggleIcon(Tools tools, Color fillColor) {
            this.tools = tools;
            this.fillColor = fillColor;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int w = getIconWidth();
            int h = getIconHeight();
            g.setColor(fillColor);
            g.fillRect(0, 0, w, h);
            for (; x < w - 2; x += 4) {
                g.setColor(WHITE);
                g.fillRect(x, 1, 1, 1);
                g.fillRect(x + 2, 3, 1, 1);
                g.setColor(shadowColor);
                g.fillRect(x + 1, 2, 1, 1);
                g.fillRect(x + 3, 4, 1, 1);
            }
        }

        @Override
        public int getIconWidth() {
            return tools.getSize().width;
        }

        @Override
        public int getIconHeight() {
            return 6;
        }

        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("ToggleIcon");
            thread.start();
        }

        public synchronized void stop() {
            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
            }
            if (tools.focus && thread != null) {
                tools.toggleB.doClick();
            }
            thread = null;
        }
    }
} // End Tools class

