/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6296064 6521533
 * @summary A simple manual test for visual verification of GradientPaint
 * LinearGradientPaint, and RadialGradientPaint.
 * @run main/manual MultiGradientTest
 * @author campbelc
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MultiGradientTest extends JPanel {

    private static final Color[] COLORS = {
        new Color(0, 0, 0),
        new Color(128, 128, 128),
        new Color(255, 0, 0),
        new Color(255, 255, 0),
        new Color(0, 255, 0),
        new Color(0, 255, 255),
        new Color(128, 0, 255),
        new Color(128, 128, 128),
    };

    private static enum PaintType {BASIC, LINEAR, RADIAL};
    private static enum ShapeType {RECT, ELLIPSE, MULTIPLE};
    private static enum XformType {IDENTITY, TRANSLATE, SCALE, SHEAR, ROTATE};

    private PaintType paintType = PaintType.LINEAR;
    private ShapeType shapeType = ShapeType.RECT;
    private XformType xformType = XformType.IDENTITY;
    private CycleMethod cycleMethod = CycleMethod.NO_CYCLE;
    private ColorSpaceType colorSpace = ColorSpaceType.SRGB;
    private Object antialiasHint = RenderingHints.VALUE_ANTIALIAS_OFF;
    private Object renderHint = RenderingHints.VALUE_RENDER_SPEED;
    private AffineTransform transform = new AffineTransform();

    private int numColors;

    private GradientPanel gradientPanel;
    private ControlsPanel controlsPanel;

    private MultiGradientTest() {
        numColors = COLORS.length;

        setLayout(new BorderLayout());
        gradientPanel = new GradientPanel();
        add(gradientPanel, BorderLayout.CENTER);
        controlsPanel = new ControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);
    }

    private class GradientPanel extends JPanel {
        private int startX, startY, endX, endY;
        private int ctrX, ctrY, focusX, focusY;
        private float radius;
        private Paint paint;

        private GradientPanel() {
            startX = 20;
            startY = 20;
            endX   = 100;
            endY   = 100;
            ctrX   = 100;
            ctrY   = 100;
            focusX = 100;
            focusY = 100;
            radius = 100.0f;

            makeNewPaint();

            MouseAdapter l = new MyMouseAdapter();
            addMouseListener(l);
            addMouseMotionListener(l);
        }

        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g.create();

            int w = getWidth();
            int h = getHeight();
            g2d.setColor(Color.black);
            g2d.fillRect(0, 0, w, h);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 antialiasHint);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                 renderHint);

            g2d.transform(transform);
            g2d.setPaint(paint);

            switch (shapeType) {
            default:
            case RECT:
                g2d.fillRect(0, 0, w, h);
                break;

            case ELLIPSE:
                g2d.fillOval(0, 0, w, h);
                break;

            case MULTIPLE:
                g2d.fillRect(0, 0, w/2, h/2);
                g2d.fillOval(w/2, 0, w/2, h/2);
                g2d.drawOval(0, h/2, w/2, h/2);
                g2d.drawLine(0, h/2, w/2, h);
                g2d.drawLine(0, h, w/2, h/2);
                Polygon p = new Polygon();
                p.addPoint(w/2, h);
                p.addPoint(w, h);
                p.addPoint(3*w/4, h/2);
                g2d.fillPolygon(p);
                break;
            }

            switch (paintType) {
            default:
            case BASIC:
            case LINEAR:
                g2d.setColor(Color.white);
                g2d.fillRect(startX-1, startY-1, 2, 2);
                g2d.drawString("1", startX, startY + 12);
                g2d.fillRect(endX-1, endY-1, 2, 2);
                g2d.drawString("2", endX, endY + 12);
                break;

            case RADIAL:
                g2d.setColor(Color.white);
                g2d.fillRect(ctrX-1, ctrY-1, 2, 2);
                g2d.drawString("C", ctrX, ctrY + 12);
                g2d.fillRect(focusX-1, focusY-1, 2, 2);
                g2d.drawString("F", focusX, focusY + 12);
                break;
            }

            g2d.dispose();
        }

        private void updatePoints(int x, int y) {
            Point2D inv = new Point2D.Double(x, y);

            try {
                inv = transform.inverseTransform(inv, null);
            } catch (NoninvertibleTransformException e) {
                e.printStackTrace();
            }

            x = (int)inv.getX();
            y = (int)inv.getY();

            switch (paintType) {
            default:
            case BASIC:
            case LINEAR:
                // pick the closest point to move
                if (inv.distance(startX, startY) < inv.distance(endX, endY)) {
                    startX = x;
                    startY = y;
                } else {
                    endX = x;
                    endY = y;
                }
                break;

            case RADIAL:
                // pick the closest point to move
                if (inv.distance(ctrX, ctrY) < inv.distance(focusX, focusY)) {
                    ctrX = x;
                    ctrY = y;
                } else {
                    focusX = x;
                    focusY = y;
                }
                break;
            }

            updatePaint();
        }

        private void makeNewPaint() {
            Color[] colors = Arrays.copyOf(COLORS, numColors);
            float[] fractions = new float[colors.length];
            for (int i = 0; i < fractions.length; i++) {
                fractions[i] = ((float)i) / (fractions.length-1);
            }

            switch (paintType) {
            case BASIC:
                boolean cyclic = (cycleMethod != CycleMethod.NO_CYCLE);
                paint = new GradientPaint(startX, startY, Color.RED,
                                          endX, endY, Color.BLUE, cyclic);
                break;

            default:
            case LINEAR:
                paint =
                    new LinearGradientPaint(new Point2D.Float(startX, startY),
                                            new Point2D.Float(endX, endY),
                                            fractions, colors,
                                            cycleMethod, colorSpace,
                                            new AffineTransform());
                break;

            case RADIAL:
                paint =
                    new RadialGradientPaint(new Point2D.Float(ctrX, ctrY),
                                            radius,
                                            new Point2D.Float(focusX, focusY),
                                            fractions, colors,
                                            cycleMethod, colorSpace,
                                            new AffineTransform());
                break;
            }

            switch (xformType) {
            default:
            case IDENTITY:
                transform = new AffineTransform();
                break;
            case TRANSLATE:
                transform = AffineTransform.getTranslateInstance(2, 2);
                break;
            case SCALE:
                transform = AffineTransform.getScaleInstance(1.2, 1.4);
                break;
            case SHEAR:
                transform = AffineTransform.getShearInstance(0.1, 0.1);
                break;
            case ROTATE:
                transform = AffineTransform.getRotateInstance(Math.PI / 4,
                                                              getWidth()/2,
                                                              getHeight()/2);
                break;
            }
        }

        public void updatePaint() {
            makeNewPaint();
            repaint();
        }

        private class MyMouseAdapter extends MouseAdapter {
            @Override
            public void mouseClicked(MouseEvent e) {
                updatePoints(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePoints(e.getX(), e.getY());
            }
        }

        public Dimension getPreferredSize() {
            return new Dimension(400, 400);
        }
    }

    private class ControlsPanel extends JPanel implements ActionListener {
        private JComboBox cmbPaint, cmbCycle, cmbSpace, cmbShape, cmbXform;
        private JCheckBox cbAntialias, cbRender;
        private JSpinner spinNumColors;

        private ControlsPanel() {
            cmbPaint = createCombo(this, paintType);
            cmbPaint.setSelectedIndex(1);
            cmbCycle = createCombo(this, cycleMethod);
            cmbSpace = createCombo(this, colorSpace);
            cmbShape = createCombo(this, shapeType);
            cmbXform = createCombo(this, xformType);

            int max = COLORS.length;
            SpinnerNumberModel model = new SpinnerNumberModel(max, 2, max, 1);
            spinNumColors = new JSpinner(model);
            spinNumColors.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    numColors = ((Integer)spinNumColors.getValue()).intValue();
                    gradientPanel.updatePaint();
                }
            });
            add(spinNumColors);

            cbAntialias = createCheck(this, "Antialiasing");
            cbRender = createCheck(this, "Render Quality");
        }

        private JComboBox createCombo(JPanel panel, Enum e) {
            JComboBox cmb = new JComboBox();
            cmb.setModel(new EnumComboBoxModel(e.getClass()));
            cmb.addActionListener(this);
            panel.add(cmb);
            return cmb;
        }

        private JCheckBox createCheck(JPanel panel, String name) {
            JCheckBox cb = new JCheckBox(name);
            cb.addActionListener(this);
            panel.add(cb);
            return cb;
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();

            if (source == cmbPaint) {
                paintType = (PaintType)cmbPaint.getSelectedItem();
            } else if (source == cmbCycle) {
                cycleMethod = (CycleMethod)cmbCycle.getSelectedItem();
            } else if (source == cmbSpace) {
                colorSpace = (ColorSpaceType)cmbSpace.getSelectedItem();
            } else if (source == cmbShape) {
                shapeType = (ShapeType)cmbShape.getSelectedItem();
            } else if (source == cmbXform) {
                xformType = (XformType)cmbXform.getSelectedItem();
            } else if (source == cbAntialias) {
                antialiasHint = cbAntialias.isSelected() ?
                    RenderingHints.VALUE_ANTIALIAS_ON :
                    RenderingHints.VALUE_ANTIALIAS_OFF;
            } else if (source == cbRender) {
                renderHint = cbRender.isSelected() ?
                    RenderingHints.VALUE_RENDER_QUALITY :
                    RenderingHints.VALUE_RENDER_SPEED;
            }

            gradientPanel.updatePaint();
        }
    }

    private static class EnumComboBoxModel<E extends Enum<E>>
        extends AbstractListModel
        implements ComboBoxModel
    {
        private E selected = null;
        private List<E> list;

        public EnumComboBoxModel(Class<E> en) {
            EnumSet<E> ens = EnumSet.allOf(en);
            list = new ArrayList<E>(ens);
            selected = list.get(0);
        }

        public int getSize() {
            return list.size();
        }

        public E getElementAt(int index) {
            return list.get(index);
        }

        public void setSelectedItem(Object anItem) {
            selected = (E)anItem;
            this.fireContentsChanged(this, 0, getSize());
        }

        public E getSelectedItem() {
            return selected;
        }
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("Multistop Gradient Demo");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });
        frame.add(new MultiGradientTest());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
