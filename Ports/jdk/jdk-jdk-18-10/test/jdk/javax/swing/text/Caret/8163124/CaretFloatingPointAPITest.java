/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;

/*
 * @test
 * @bug 8163175
 * @summary PlainView.modelToView() method should return Rectangle2D
 * @run main/manual CaretFloatingPointAPITest
 */
public class CaretFloatingPointAPITest {

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n"
            + "Verify that cursor position is not rounded on HiDPI display.\n\n"
            + "If the display does not support HiDPI mode press PASS.\n\n"
            + "1. Press the Right-Arrow key several times to move the red caret"
            + " in the text field.\n"
            + "2. Check that the caret has the same position between chars"
            + " in diffrent locations.\n\n"
            + "If so, press PASS, else press FAIL.\n";

    public static void main(String args[]) throws Exception {
        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(CaretFloatingPointAPITest::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);

        if (!testResult) {
            throw new RuntimeException("Test fails!");
        }
    }

    private static void createUI() {

        final JFrame mainFrame = new JFrame("Metal L&F icons test");
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        JTextField textField = new JTextField("aaaaaaaaaaaaaaaaaaaaaaa");
        Dimension size = new Dimension(400, 100);
        textField.setPreferredSize(size);
        textField.setFont(textField.getFont().deriveFont(28.0f));
        textField.setCaretColor(Color.RED);
        textField.setCaret(new CustomCaret());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(textField, gbc);

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        JButton passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            mainFrame.dispose();
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.dispose();
                countDownLatch.countDown();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;

        resultButtonPanel.add(passButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        mainControlPanel.add(resultButtonPanel, gbc);

        mainFrame.add(mainControlPanel);
        mainFrame.pack();

        mainFrame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.dispose();
                countDownLatch.countDown();
            }
        });
        mainFrame.setVisible(true);
    }

    static class CustomCaret implements Caret {

        private JTextComponent component;
        private boolean visible;
        private boolean selectionVisible = true;
        int blinkRate;
        int dot;
        int mark;
        Position.Bias dotBias;
        Position.Bias markBias;
        Object selectionTag;
        Point2D magicCaretPosition;

        private MouseListener mouseListener = new CaretMouseListener();

        @Override
        public void install(JTextComponent c) {
            this.component = c;
            c.addMouseListener(mouseListener);
        }

        @Override
        public void deinstall(JTextComponent c) {
            c.removeMouseListener(mouseListener);
            this.component = null;
        }

        @Override
        public void paint(Graphics g) {

            if (component == null) {
                return;
            }

            int dot = getDot();
            Rectangle2D r = null;
            try {
                r = component.modelToView2D(dot);
            } catch (BadLocationException e) {
                return;
            }

            if (r == null) {
                return;
            }

            Rectangle2D cr = getCaretRectangle(r);
            repaint(cr.getBounds());

            g.setColor(component.getCaretColor());
            float cx = (float) cr.getX();
            float cy = (float) cr.getY();
            float cw = (float) cr.getWidth();
            float ch = (float) cr.getHeight();
            float c = cx + cw / 2;

            Graphics2D g2d = (Graphics2D) g;
            g2d.draw(new Line2D.Float(c, cy, c, cy + ch));
            g2d.draw(new Line2D.Float(cx, cy, cx + cw, cy));
            g2d.draw(new Line2D.Float(cx, cy + ch, cx + cw, cy + ch));
        }

        void repaint(Rectangle r) {
            component.repaint(r);
        }

        Rectangle2D getCaretRectangle(Rectangle2D r) {
            int d = 3;
            double cx = r.getX() - d;
            double cy = r.getY();
            double cw = 2 * d;
            double ch = r.getHeight();
            return new Rectangle2D.Double(cx, cy, cw, ch);
        }

        @Override
        public void addChangeListener(ChangeListener l) {
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void setVisible(boolean v) {
            this.visible = true;
        }

        @Override
        public boolean isSelectionVisible() {
            return selectionVisible;
        }

        @Override
        public void setSelectionVisible(boolean v) {
            this.selectionVisible = v;
            updateSelection();
        }

        @Override
        public void setMagicCaretPosition(Point p) {
            magicCaretPosition = p;
        }

        @Override
        public Point getMagicCaretPosition() {
            if (magicCaretPosition != null) {
                return new Point((int) magicCaretPosition.getX(),
                                 (int) magicCaretPosition.getY());
            }
            return null;
        }

        @Override
        public void setBlinkRate(int rate) {
            this.blinkRate = rate;
        }

        @Override
        public int getBlinkRate() {
            return blinkRate;
        }

        @Override
        public int getDot() {
            return dot;
        }

        @Override
        public int getMark() {
            return mark;
        }

        @Override
        public void setDot(int dot) {
            setDot(dot, Position.Bias.Forward);
        }

        private void setDot(int dot, Position.Bias bias) {
            handleSetDot(dot, bias);
            updateSelection();
        }

        @Override
        public void moveDot(int dot) {
            moveDot(dot, Position.Bias.Forward);
        }

        private void moveDot(int dot, Position.Bias bias) {
            changeCaretPosition(dot, bias);
            updateSelection();
        }

        void handleSetDot(int dot, Position.Bias dotBias) {

            if (component == null) {
                return;
            }

            Document doc = component.getDocument();
            if (doc != null) {
                dot = Math.min(dot, doc.getLength());
            }

            dot = Math.max(dot, 0);

            if (dot == 0) {
                dotBias = Position.Bias.Forward;
            }

            mark = dot;

            if (this.dot != dot || this.dotBias != dotBias) {
                changeCaretPosition(dot, dotBias);
                updateSelection();
            }

            this.markBias = this.dotBias;
        }

        void changeCaretPosition(int dot, Position.Bias dotBias) {
            this.dot = dot;
            this.dotBias = dotBias;
            setMagicCaretPosition(null);
            SwingUtilities.invokeLater(this::repaintNewCaret);
        }

        private void updateSelection() {
            Highlighter h = component.getHighlighter();
            if (h != null) {
                int p0 = Math.min(dot, mark);
                int p1 = Math.max(dot, mark);

                if (p0 == p1 || !selectionVisible) {
                    if (selectionTag != null) {
                        h.removeHighlight(selectionTag);
                        selectionTag = null;
                    }
                } else {
                    try {
                        if (selectionTag != null) {
                            h.changeHighlight(selectionTag, p0, p1);
                        } else {
                            Highlighter.HighlightPainter p = getSelectionPainter();
                            selectionTag = h.addHighlight(p0, p1, p);
                        }
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        void repaintNewCaret() {
            if (component != null) {
                TextUI mapper = component.getUI();
                Document doc = component.getDocument();
                if ((mapper != null) && (doc != null)) {
                    Rectangle2D newLoc;
                    try {
                        newLoc = mapper.modelToView2D(component, this.dot, this.dotBias);
                    } catch (BadLocationException e) {
                        newLoc = null;
                    }
                    if (newLoc != null) {
                        adjustVisibility(newLoc.getBounds());
                        if (getMagicCaretPosition() == null) {
                            setMagicCaretPosition(new Point((int) newLoc.getX(),
                                                            (int) newLoc.getY()));
                        }
                    }
                    damage(newLoc.getBounds());
                }
            }
        }

        protected Highlighter.HighlightPainter getSelectionPainter() {
            return DefaultHighlighter.DefaultPainter;
        }

        protected void adjustVisibility(Rectangle nloc) {
            if (component == null) {
                return;
            }
            if (SwingUtilities.isEventDispatchThread()) {
                component.scrollRectToVisible(nloc);
            } else {
                SwingUtilities.invokeLater(() -> {
                    component.scrollRectToVisible(nloc);
                });
            }
        }

        protected synchronized void damage(Rectangle r) {
            if (r != null && component != null) {
                component.repaint(r);
            }
        }

        private class CaretMouseListener extends MouseAdapter {

            @Override
            public void mousePressed(MouseEvent e) {
                Point pt = new Point(e.getX(), e.getY());
                Position.Bias[] biasRet = new Position.Bias[1];
                int pos = component.getUI().viewToModel(component, pt, biasRet);
                if (biasRet[0] == null) {
                    biasRet[0] = Position.Bias.Forward;
                }
                if (pos >= 0) {
                    setDot(pos);
                }
            }
        }
    }
}
