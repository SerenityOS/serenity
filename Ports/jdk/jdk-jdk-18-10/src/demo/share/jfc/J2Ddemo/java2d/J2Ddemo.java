/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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


import static java2d.CustomControlsContext.State.START;
import static java2d.CustomControlsContext.State.STOP;
import static java2d.DemoImages.newDemoImages;
import static java2d.DemoFonts.newDemoFonts;
import static java2d.RunWindow.RunWindowSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;


/**
 * A demo that shows Java 2D(TM) API features.
 */
@SuppressWarnings("serial")
public class J2Ddemo extends JPanel implements ItemListener, ActionListener, DemoInstVarsAccessor {
    private final GlobalControls controls;
    private final MemoryMonitor memorymonitor;
    private final PerformanceMonitor performancemonitor;
    private final JTabbedPane tabbedPane;
    private final DemoGroup[] group;
    private JCheckBoxMenuItem verboseCB;
    private JCheckBoxMenuItem ccthreadCB;
    private JCheckBoxMenuItem printCB = new JCheckBoxMenuItem("Default Printer");
    private Color backgroundColor;
    private JCheckBoxMenuItem memoryCB, perfCB;
    private final Intro intro;
    public static final String[][] demos = {
        { "Arcs_Curves", "Arcs", "BezierAnim", "Curves", "Ellipses" },
        { "Clipping", "Areas", "ClipAnim", "Intersection", "Text" },
        { "Colors", "BullsEye", "ColorConvert", "Rotator3D" },
        { "Composite", "ACimages", "ACrules", "FadeAnim" },
        { "Fonts", "AttributedStr", "Highlighting", "Outline", "Tree" },
        { "Images", "DukeAnim", "ImageOps", "JPEGFlip", "WarpImage" },
        { "Lines", "Caps", "Dash", "Joins", "LineAnim" },
        { "Mix", "Balls", "BezierScroller", "Stars3D" },
        { "Paint", "GradAnim", "Gradient", "Texture", "TextureAnim" },
        { "Paths", "Append", "CurveQuadTo", "FillStroke", "WindingRule" },
        { "Transforms", "Rotate", "SelectTx", "TransformAnim" }
    };
    private final boolean demoIsInApplet;
    private JCheckBoxMenuItem controlsCB;
    private JMenuItem runMI, cloneMI, fileMI, backgMI;
//    private JMenuItem ccthreadMI, verboseMI;
    private RunWindow runwindow;
    private RunWindowSettings runWndSetts;
    private CloningFeature cloningfeature;
    private JFrame rf, cf;
//    private GlobalPanel gp;

    /**
     * Construct the J2D Demo.
     */
    public J2Ddemo(boolean demoIsInApplet, DemoProgress progress, RunWindowSettings runWndSetts) {
        this.demoIsInApplet = demoIsInApplet;
        this.runWndSetts = runWndSetts;

        setLayout(new BorderLayout());
        setBorder(new EtchedBorder());

        add(createMenuBar(), BorderLayout.NORTH);

        // hard coding 14 = 11 demo dirs + images + fonts + Intro
        progress.setMaximum(13);
        progress.setText("Loading images");
        newDemoImages();
        progress.setValue(progress.getValue() + 1);
        progress.setText("Loading fonts");
        newDemoFonts();
        progress.setValue(progress.getValue() + 1);
        progress.setText("Loading Intro");
        intro = new Intro();
        progress.setValue(progress.getValue() + 1);
        UIManager.put("Button.margin", new Insets(0, 0, 0, 0));

        controls = new GlobalControls(this);
        memorymonitor = new MemoryMonitor();
        performancemonitor = new PerformanceMonitor();

        GlobalPanel gp = new GlobalPanel(this);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
        tabbedPane.addTab("", new J2DIcon(this), gp);
        tabbedPane.addChangeListener(gp);

        group = new DemoGroup[demos.length];
        for (int i = 0; i < demos.length; i++) {
            progress.setText("Loading demos." + demos[i][0]);
            group[i] = new DemoGroup(demos[i][0], this);
            tabbedPane.addTab(demos[i][0], null);
            progress.setValue(progress.getValue() + 1);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        JMenuBar menuBar = new JMenuBar();

        if (!demoIsInApplet) {
            JMenu file = menuBar.add(new JMenu("File"));
            fileMI = file.add(new JMenuItem("Exit"));
            fileMI.addActionListener(this);
        }

        JMenu options = menuBar.add(new JMenu("Options"));

        controlsCB = (JCheckBoxMenuItem) options.add(
                new JCheckBoxMenuItem("Global Controls", true));
        controlsCB.addItemListener(this);

        memoryCB = (JCheckBoxMenuItem) options.add(
                new JCheckBoxMenuItem("Memory Monitor", true));
        memoryCB.addItemListener(this);

        perfCB = (JCheckBoxMenuItem) options.add(
                new JCheckBoxMenuItem("Performance Monitor", true));
        perfCB.addItemListener(this);

        options.add(new JSeparator());

        ccthreadCB = (JCheckBoxMenuItem) options.add(
                new JCheckBoxMenuItem("Custom Controls Thread"));
        ccthreadCB.addItemListener(this);

        printCB = (JCheckBoxMenuItem) options.add(printCB);

        verboseCB = (JCheckBoxMenuItem) options.add(
                new JCheckBoxMenuItem("Verbose"));

        options.add(new JSeparator());

        backgMI = options.add(new JMenuItem("Background Color"));
        backgMI.addActionListener(this);

        runMI = options.add(new JMenuItem("Run Window"));
        runMI.addActionListener(this);

        cloneMI = options.add(new JMenuItem("Cloning Feature"));
        cloneMI.addActionListener(this);

        return menuBar;
    }

    public void createRunWindow() {
        if (rf != null) {
            rf.toFront();
            return;
        }
        runwindow = new RunWindow(this, runWndSetts);
        WindowListener l = new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                runwindow.stop();
                rf.dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                rf = null;
            }
        };
        rf = new JFrame("Run");
        rf.addWindowListener(l);
        rf.getContentPane().add("Center", runwindow);
        rf.pack();
        if (!demoIsInApplet) {
            rf.setSize(new Dimension(200, 125));
        } else {
            rf.setSize(new Dimension(200, 150));
        }
        rf.setVisible(true);
    }

    public void startRunWindow() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                runwindow.doRunAction();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(fileMI)) {
            System.exit(0);
        } else if (e.getSource().equals(runMI)) {
            createRunWindow();
        } else if (e.getSource().equals(cloneMI)) {
            if (cloningfeature == null) {
                cloningfeature = new CloningFeature(this);
                WindowListener l = new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        cloningfeature.stop();
                        cf.dispose();
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        cloningfeature = null;
                    }
                };
                cf = new JFrame("Cloning Demo");
                cf.addWindowListener(l);
                cf.getContentPane().add("Center", cloningfeature);
                cf.pack();
                cf.setSize(new Dimension(320, 330));
                cf.setVisible(true);
            } else {
                cf.toFront();
            }
        } else if (e.getSource().equals(backgMI)) {
            backgroundColor =
                    JColorChooser.showDialog(this, "Background Color",
                    Color.white);
            for (int i = 1; i < tabbedPane.getTabCount(); i++) {
                JPanel p = group[i - 1].getPanel();
                for (int j = 0; j < p.getComponentCount(); j++) {
                    DemoPanel dp = (DemoPanel) p.getComponent(j);
                    if (dp.surface != null) {
                        dp.surface.setBackground(backgroundColor);
                    }
                }
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource().equals(controlsCB)) {
            boolean newVisibility = !controls.isVisible();
            controls.setVisible(newVisibility);
            for (Component cmp : controls.texturechooser.getComponents()) {
                cmp.setVisible(newVisibility);
            }
        } else if (e.getSource().equals(memoryCB)) {
            if (memorymonitor.isVisible()) {
                memorymonitor.setVisible(false);
                memorymonitor.surf.setVisible(false);
                memorymonitor.surf.stop();
            } else {
                memorymonitor.setVisible(true);
                memorymonitor.surf.setVisible(true);
                memorymonitor.surf.start();
            }
        } else if (e.getSource().equals(perfCB)) {
            if (performancemonitor.isVisible()) {
                performancemonitor.setVisible(false);
                performancemonitor.surf.setVisible(false);
                performancemonitor.surf.stop();
            } else {
                performancemonitor.setVisible(true);
                performancemonitor.surf.setVisible(true);
                performancemonitor.surf.start();
            }
        } else if (e.getSource().equals(ccthreadCB)) {
            CustomControlsContext.State state =
                    ccthreadCB.isSelected() ? START : STOP;
            if (tabbedPane.getSelectedIndex() != 0) {
                JPanel p = group[tabbedPane.getSelectedIndex() - 1].getPanel();
                for (int i = 0; i < p.getComponentCount(); i++) {
                    DemoPanel dp = (DemoPanel) p.getComponent(i);
                    if (dp.ccc != null) {
                        dp.ccc.handleThread(state);
                    }
                }
            }
        }
        revalidate();
    }

    public void start() {
        if (tabbedPane.getSelectedIndex() == 0) {
            intro.start();
        } else {
            group[tabbedPane.getSelectedIndex() - 1].setup(false);
            if (memorymonitor.surf.thread == null && memoryCB.getState()) {
                memorymonitor.surf.start();
            }
            if (performancemonitor.surf.thread == null && perfCB.getState()) {
                performancemonitor.surf.start();
            }
        }
    }

    public void stop() {
        if (tabbedPane.getSelectedIndex() == 0) {
            intro.stop();
        } else {
            memorymonitor.surf.stop();
            performancemonitor.surf.stop();
            int i = tabbedPane.getSelectedIndex() - 1;
            group[i].shutDown(group[i].getPanel());
        }
    }

    /**
     * Start of 'DemoInstVarsAccessor' implementation.
     */
    @Override
    public GlobalControls getControls() {
        return controls;
    }

    @Override
    public MemoryMonitor getMemoryMonitor() {
        return memorymonitor;
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return performancemonitor;
    }

    @Override
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    @Override
    public DemoGroup[] getGroup() {
        return group;
    }

    @Override
    public void setGroupColumns(int columns) {
        for (DemoGroup dg : group) {
            if (dg != null) {
                dg.columns = columns;
            }
        }
    }

    @Override
    public JCheckBoxMenuItem getVerboseCB() {
        return verboseCB;
    }

    @Override
    public JCheckBoxMenuItem getCcthreadCB() {
        return ccthreadCB;
    }

    @Override
    public JCheckBoxMenuItem getPrintCB() {
        return printCB;
    }

    @Override
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public JCheckBoxMenuItem getMemoryCB() {
        return memoryCB;
    }

    @Override
    public JCheckBoxMenuItem getPerfCB() {
        return perfCB;
    }

    @Override
    public Intro getIntro() {
        return intro;
    }
    /**
     * End of 'DemoInstVarsAccessor' implementation.
     */

    static void addToGridBag(JPanel panel, Component comp,
            int x, int y, int w, int h, double weightx, double weighty) {

        GridBagLayout gbl = (GridBagLayout) panel.getLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = weightx;
        c.weighty = weighty;
        panel.add(comp);
        gbl.setConstraints(comp, c);
    }


    /**
     * The Icon for the Intro tab.
     */
    static class J2DIcon implements Icon {
        private final DemoInstVarsAccessor demoInstVars;
        private static final Color myBlue = new Color(94, 105, 176);
        private static final Color myBlack = new Color(20, 20, 20);
        private static final Font font = new Font(Font.SERIF, Font.BOLD, 12);
        private FontRenderContext frc = new FontRenderContext(null, true, true);
        private TextLayout tl = new TextLayout("J2D demo", font, frc);

        public J2DIcon(DemoInstVarsAccessor demoInstVars) {
            this.demoInstVars = demoInstVars;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(font);
            if (demoInstVars.getTabbedPane().getSelectedIndex() == 0) {
                g2.setColor(myBlue);
            } else {
                g2.setColor(myBlack);
            }
            tl.draw(g2, x, y + (float)(tl.getBounds().getHeight()));
        }

        @Override
        public int getIconWidth() {
            return (int)(tl.getAdvance())+5;
        }

        @Override
        public int getIconHeight() {
            return (int)(tl.getBounds().getHeight());
        }
    }

    /**
     * This class eliminates the need in presence of static 'JLabel', 'JProgressBar'
     * variables in 'J2Ddemo' class. It is a part of the fix which changed static
     * variables for instance variables in certain demo classes.
     */
    public static class DemoProgress {
        private final JLabel progressLabel;
        private final JProgressBar progressBar;

        public DemoProgress(JLabel progressLabel, JProgressBar progressBar) {
            if (progressLabel == null) {
                throw new IllegalArgumentException("null was transferred as 'progressLabel' argument");
            }
            if (progressBar == null) {
                throw new IllegalArgumentException("null was transferred as 'progressBar' argument");
            }

            this.progressLabel = progressLabel;
            this.progressBar = progressBar;
        }

        public void setText(String text) {
            progressLabel.setText(text);
        }

        public void setMaximum(int n) {
            progressBar.setMaximum(n);
        }

        public int getValue() {
            return progressBar.getValue();
        }

        public void setValue(int n) {
            progressBar.setValue(n);
        }
    }

    private static void initFrame(String[] args, RunWindowSettings runWndSetts) {
        final J2Ddemo[] demoOneInstArr = new J2Ddemo[1];

        JFrame frame = new JFrame("Java 2D(TM) Demo");
        frame.getAccessibleContext().setAccessibleDescription(
                "A sample application to demonstrate Java2D(TM) features");
        int FRAME_WIDTH = 400, FRAME_HEIGHT = 200;
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(d.width / 2 - FRAME_WIDTH / 2, d.height / 2 - FRAME_HEIGHT
                / 2);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                J2Ddemo demo = demoOneInstArr[0];
                if (demo != null) {
                    demo.start();
                }
            }

            @Override
            public void windowIconified(WindowEvent e) {
                J2Ddemo demo = demoOneInstArr[0];
                if (demo != null) {
                    demo.stop();
                }
            }
        });
        JOptionPane.setRootFrame(frame);

        JPanel progressPanel = new JPanel() {

            @Override
            public Insets getInsets() {
                return new Insets(40, 30, 20, 30);
            }
        };
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        frame.getContentPane().add(progressPanel, BorderLayout.CENTER);

        Dimension labelSize = new Dimension(400, 20);
        JLabel progressLabel = new JLabel("Loading, please wait...");
        progressLabel.setAlignmentX(CENTER_ALIGNMENT);
        progressLabel.setMaximumSize(labelSize);
        progressLabel.setPreferredSize(labelSize);
        progressPanel.add(progressLabel);
        progressPanel.add(Box.createRigidArea(new Dimension(1, 20)));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressLabel.setLabelFor(progressBar);
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        progressBar.setMinimum(0);
        progressBar.setValue(0);
        progressBar.getAccessibleContext().setAccessibleName(
                                  "J2D demo loading progress");
        progressPanel.add(progressBar);
        DemoProgress demoProgress = new DemoProgress(progressLabel, progressBar);

        frame.setVisible(true);

        J2Ddemo demo = new J2Ddemo(false, demoProgress, runWndSetts);
        demoOneInstArr[0] = demo;

        frame.getContentPane().removeAll();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(demo, BorderLayout.CENTER);
        FRAME_WIDTH = 850;
        FRAME_HEIGHT = 600;
        frame.setLocation(d.width / 2 - FRAME_WIDTH / 2, d.height / 2 - FRAME_HEIGHT
                / 2);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String s = arg.substring(arg.indexOf('=') + 1);
            if (arg.startsWith("-runs=")) {
                runWndSetts.setNumRuns(Integer.parseInt(s));
                runWndSetts.setExit(true);
                demo.createRunWindow();
            } else if (arg.startsWith("-screen=")) {
                demo.getControls().screenCombo.setSelectedIndex(Integer.parseInt(s));
            } else if (arg.startsWith("-antialias=")) {
                demo.controls.aliasCB.setSelected(s.endsWith("true"));
            } else if (arg.startsWith("-rendering=")) {
                demo.controls.renderCB.setSelected(s.endsWith("true"));
            } else if (arg.startsWith("-texture=")) {
                demo.controls.textureCB.setSelected(s.endsWith("true"));
            } else if (arg.startsWith("-composite=")) {
                demo.controls.compositeCB.setSelected(s.endsWith("true"));
            } else if (arg.startsWith("-verbose")) {
                demo.verboseCB.setSelected(true);
            } else if (arg.startsWith("-print")) {
                demo.printCB.setSelected(true);
                runWndSetts.setPrintCBIsSelected(true);
            } else if (arg.startsWith("-columns=")) {
                demo.setGroupColumns(Integer.parseInt(s));
            } else if (arg.startsWith("-buffers=")) {
                // usage -buffers=3,10
                runWndSetts.setBuffersFlag(true);
                int i1 = arg.indexOf('=') + 1;
                int i2 = arg.indexOf(',');
                String s1 = arg.substring(i1, i2);
                runWndSetts.setBufBeg(Integer.parseInt(s1));
                s1 = arg.substring(i2 + 1, arg.length());
                runWndSetts.setBufEnd(Integer.parseInt(s1));
            } else if (arg.startsWith("-ccthread")) {
                demo.ccthreadCB.setSelected(true);
            } else if (arg.startsWith("-zoom")) {
                runWndSetts.setZoomCBSelected(true);
            } else if (arg.startsWith("-maxscreen")) {
                frame.setLocation(0, 0);
                frame.setSize(d.width, d.height);
            }
        }

        frame.validate();
        frame.repaint();
        frame.getFocusTraversalPolicy().getDefaultComponent(frame).requestFocus();
        demo.start();

        if (runWndSetts.getExit()) {
            demo.startRunWindow();
        }

    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RunWindowSettings runWndSetts = new RunWindowSettings();
                for (int i = 0; i < args.length; i++) {
                    if (args[i].startsWith("-h") || args[i].startsWith("-help")) {
                        String s = "\njava -jar J2Ddemo.jar -runs=5 -delay=5 -screen=5 "
                                + "-antialias=true -rendering=true -texture=true "
                                + "-composite=true -verbose -print -columns=3 "
                                + "-buffers=5,10 -ccthread -zoom -maxscreen \n";
                        System.out.println(s);
                        s =
                                "    -runs=5       Number of runs to execute\n"
                                + "    -delay=5      Sleep amount between tabs\n"
                                + "    -antialias=   true or false for antialiasing\n"
                                + "    -rendering=   true or false for quality or speed\n"
                                + "    -texture=     true or false for texturing\n"
                                + "    -composite=   true or false for compositing\n"
                                + "    -verbose      output Surface graphic states \n"
                                + "    -print        during run print the Surface, "
                                + "use the Default Printer\n"
                                + "    -columns=3    # of columns to use in clone layout \n"
                                + "    -screen=3     demos all use this screen type \n"
                                + "    -buffers=5,10 during run - clone to see screens "
                                + "five through ten\n"
                                + "    -ccthread     Invoke the Custom Controls Thread \n"
                                + "    -zoom         mouseClick on surface for zoom in  \n"
                                + "    -maxscreen    take up the entire monitor screen \n";
                        System.out.println(s);
                        s =
                                "Examples : \n" + "    Print all of the demos : \n"
                                + "        java -jar J2Ddemo.jar -runs=1 -delay=60 -print \n"
                                + "    Run zoomed in with custom control thread \n"
                                + "        java -jar J2Ddemo.jar -runs=10 -zoom -ccthread\n";
                        System.out.println(s);
                        System.exit(0);
                    } else if (args[i].startsWith("-delay=")) {
                        String s = args[i].substring(args[i].indexOf('=') + 1);
                        runWndSetts.setDelay(Integer.parseInt(s));
                    }
                }

                initFrame(args, runWndSetts);
            }
        });
    }
}
