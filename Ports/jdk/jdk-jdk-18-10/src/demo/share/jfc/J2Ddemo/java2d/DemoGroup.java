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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * DemoGroup handles multiple demos inside of a panel.  Demos are loaded
 * from the demos[][] string as listed in J2Ddemo.java.
 * Demo groups can be loaded individually, for example :
 *      java DemoGroup Fonts
 * Loads all the demos found in the demos/Fonts directory.
 */
@SuppressWarnings("serial")
public class DemoGroup extends JPanel
        implements ChangeListener, ActionListener {
    private final DemoInstVarsAccessor demoInstVars;
    public int columns = 2;
    private static final Font font = new Font(Font.SERIF, Font.PLAIN, 10);
    private final EmptyBorder emptyB = new EmptyBorder(5, 5, 5, 5);
    private final BevelBorder bevelB = new BevelBorder(BevelBorder.LOWERED);
    private String groupName;
    public JPanel[] clonePanels;
    public JTabbedPane tabbedPane;

    public DemoGroup(String name, DemoInstVarsAccessor demoInstVars) {

        groupName = name;
        this.demoInstVars = demoInstVars;

        setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(0, 2));
        p.setBorder(new CompoundBorder(emptyB, bevelB));

        // Find the named demo group in J2Ddemo.demos[].
        int ind = -1;
        while (!name.equals(J2Ddemo.demos[++ind][0])) {
        }
        String[] demos = J2Ddemo.demos[ind];

        // If there are an odd number of demos, use GridBagLayout.
        // Note that we don't use the first entry.
        int numDemos = demos.length - 1;
        if (numDemos % 2 == 1) {
            p.setLayout(new GridBagLayout());
        }

        MouseAdapter mouseListener = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                DemoGroup.this.mouseClicked(e.getComponent());
            }
        };

        // For each demo in the group, prepare a DemoPanel.
        for (int i = 1; i <= numDemos; i++) {
            DemoPanel dp =
                    new DemoPanel("java2d.demos." + name + "." + demos[i], demoInstVars);
            dp.setDemoBorder(p);
            if (dp.surface != null) {
                dp.surface.addMouseListener(mouseListener);
                dp.surface.setMonitor(demoInstVars.getPerformanceMonitor() != null);
            }
            if (p.getLayout() instanceof GridBagLayout) {
                int x = p.getComponentCount() % 2;
                int y = p.getComponentCount() / 2;
                int w = (i == numDemos) ? 2 : 1;
                J2Ddemo.addToGridBag(p, dp, x, y, w, 1, 1, 1);
            } else {
                p.add(dp);
            }
        }

        add(p);
    }

    public void mouseClicked(Component component) {
        String className = component.toString();

        if (tabbedPane == null) {
            shutDown(getPanel());
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(new CompoundBorder(emptyB, bevelB));

            tabbedPane = new JTabbedPane();
            tabbedPane.setFont(font);

            JPanel tmpP = (JPanel) getComponent(0);
            tabbedPane.addTab(groupName, tmpP);

            clonePanels = new JPanel[tmpP.getComponentCount()];
            for (int i = 0; i < clonePanels.length; i++) {
                clonePanels[i] = new JPanel(new BorderLayout());
                DemoPanel dp = (DemoPanel) tmpP.getComponent(i);
                DemoPanel c = new DemoPanel(dp.className, demoInstVars);
                c.setDemoBorder(clonePanels[i]);
                if (c.surface != null) {
                    c.surface.setMonitor(demoInstVars.getPerformanceMonitor() != null);
                    Image cloneImg = DemoImages.getImage("clone.gif", this);
                    c.tools.cloneB = c.tools.addTool(cloneImg,
                            "Clone the Surface", this);
                    Dimension d = c.tools.toolbar.getPreferredSize();
                    c.tools.toolbar.setPreferredSize(
                            new Dimension(d.width + 27, d.height));
                    if (demoInstVars.getBackgroundColor() != null) {
                        c.surface.setBackground(demoInstVars.getBackgroundColor());
                    }
                }
                clonePanels[i].add(c);
                String s = dp.className.substring(dp.className.indexOf('.') + 1);
                tabbedPane.addTab(s, clonePanels[i]);
            }
            p.add(tabbedPane);
            remove(tmpP);
            add(p);

            tabbedPane.addChangeListener(this);
            revalidate();
        }

        className = className.substring(0, className.indexOf('['));

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String s1 = className.substring(className.indexOf('.') + 1);
            if (tabbedPane.getTitleAt(i).equals(s1)) {
                tabbedPane.setSelectedIndex(i);
                break;
            }
        }

        revalidate();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        if (b.getToolTipText().startsWith("Clone")) {
            cloneDemo();
        } else {
            removeClone(b.getParent().getParent().getParent().getParent());
        }
    }
    private int index;

    @Override
    public void stateChanged(ChangeEvent e) {
        shutDown((JPanel) tabbedPane.getComponentAt(index));
        index = tabbedPane.getSelectedIndex();
        setup(false);
    }

    public JPanel getPanel() {
        if (tabbedPane != null) {
            return (JPanel) tabbedPane.getSelectedComponent();
        } else {
            return (JPanel) getComponent(0);
        }
    }

    public void setup(boolean issueRepaint) {

        JPanel p = getPanel();

        // Let PerformanceMonitor know which demos are running
        if (demoInstVars.getPerformanceMonitor() != null) {
            demoInstVars.getPerformanceMonitor().surf.setPanel(p);
            demoInstVars.getPerformanceMonitor().surf.setSurfaceState();
        }

        GlobalControls c = demoInstVars.getControls();
        // .. tools check against global controls settings ..
        // .. & start demo & custom control thread if need be ..
        for (int i = 0; i < p.getComponentCount(); i++) {
            DemoPanel dp = (DemoPanel) p.getComponent(i);
            if (dp.surface != null && c != null) {
                Tools t = dp.tools;
                t.setVisible(isValid());
                t.issueRepaint = issueRepaint;
                JToggleButton[] b = { t.toggleB, t.aliasB, t.renderB,
                    t.textureB, t.compositeB };
                JCheckBox[] cb = { c.toolBarCB, c.aliasCB, c.renderCB,
                    c.textureCB, c.compositeCB };
                for (int j = 0; j < b.length; j++) {
                    if (c.obj != null && c.obj.equals(cb[j])) {
                        if (b[j].isSelected() != cb[j].isSelected()) {
                            b[j].doClick();
                        }
                    } else if (c.obj == null) {
                        if (b[j].isSelected() != cb[j].isSelected()) {
                            b[j].doClick();
                        }
                    }
                }
                t.setVisible(true);
                if (c.screenCombo.getSelectedIndex()
                        != t.screenCombo.getSelectedIndex()) {
                    t.screenCombo.setSelectedIndex(c.screenCombo.
                            getSelectedIndex());
                }
                if (demoInstVars.getVerboseCB().isSelected()) {
                    dp.surface.verbose(c);
                }
                dp.surface.setSleepAmount(c.slider.getValue());
                if (demoInstVars.getBackgroundColor() != null) {
                    dp.surface.setBackground(demoInstVars.getBackgroundColor());
                }
                t.issueRepaint = true;
            }
            dp.start();
        }
        revalidate();
    }

    public void shutDown(JPanel p) {
        for (int i = 0; i < p.getComponentCount(); i++) {
            ((DemoPanel) p.getComponent(i)).stop();
        }
        System.gc();
    }

    public void cloneDemo() {
        JPanel panel = clonePanels[tabbedPane.getSelectedIndex() - 1];
        if (panel.getComponentCount() == 1) {
            panel.invalidate();
            panel.setLayout(new GridLayout(0, columns, 5, 5));
            panel.revalidate();
        }
        DemoPanel original = (DemoPanel) getPanel().getComponent(0);
        DemoPanel clone = new DemoPanel(original.className, demoInstVars);
        if (columns == 2) {
            clone.setDemoBorder(panel);
        }
        Image removeImg = DemoImages.getImage("remove.gif", this);
        clone.tools.cloneB =
                clone.tools.addTool(removeImg, "Remove the Surface", this);
        Dimension d = clone.tools.toolbar.getPreferredSize();
        clone.tools.toolbar.setPreferredSize(
                new Dimension(d.width + 27, d.height));
        if (demoInstVars.getBackgroundColor() != null) {
            clone.surface.setBackground(demoInstVars.getBackgroundColor());
        }
        if (demoInstVars.getControls() != null) {
            if (clone.tools.isExpanded
                    != demoInstVars.getControls().toolBarCB.isSelected()) {
                clone.tools.toggleB.doClick();
            }
        }
        clone.start();
        clone.surface.setMonitor(demoInstVars.getPerformanceMonitor() != null);
        panel.add(clone);
        panel.repaint();
        panel.revalidate();
    }

    public void removeClone(Component theClone) {
        JPanel panel = clonePanels[tabbedPane.getSelectedIndex() - 1];
        if (panel.getComponentCount() == 2) {
            Component cmp = panel.getComponent(0);
            panel.removeAll();
            panel.setLayout(new BorderLayout());
            panel.revalidate();
            panel.add(cmp);
        } else {
            panel.remove(theClone);
            int cmpCount = panel.getComponentCount();
            for (int j = 1; j < cmpCount; j++) {
                int top = (j + 1 >= 3) ? 0 : 5;
                int left = ((j + 1) % 2) == 0 ? 0 : 5;
                EmptyBorder eb = new EmptyBorder(top, left, 5, 5);
                SoftBevelBorder sbb = new SoftBevelBorder(BevelBorder.RAISED);
                JPanel p = (JPanel) panel.getComponent(j);
                p.setBorder(new CompoundBorder(eb, sbb));
            }
        }
        panel.repaint();
        panel.revalidate();
    }

    public static void main(String[] args) {
        class DemoInstVarsAccessorImpl extends DemoInstVarsAccessorImplBase {
            private volatile JCheckBoxMenuItem ccthreadCB;

            public void setCcthreadCB(JCheckBoxMenuItem ccthreadCB) {
                this.ccthreadCB = ccthreadCB;
            }

            @Override
            public JCheckBoxMenuItem getCcthreadCB() {
                return ccthreadCB;
            }
        }
        DemoInstVarsAccessorImpl demoInstVars = new DemoInstVarsAccessorImpl();
        final DemoGroup group = new DemoGroup(args[0], demoInstVars);
        JFrame f = new JFrame("Java2D(TM) Demo - DemoGroup");
        f.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                group.setup(false);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                group.shutDown(group.getPanel());
            }
        });
        f.getContentPane().add("Center", group);
        f.pack();
        int FRAME_WIDTH = 620;
        int FRAME_HEIGHT = 530;
        f.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        f.setLocationRelativeTo(null);  // centers f on screen
        f.setVisible(true);
        for (String arg : args) {
            if (arg.startsWith("-ccthread")) {
                demoInstVars.setCcthreadCB(new JCheckBoxMenuItem("CCThread", true));
            }
        }
        group.setup(false);
    }
}
