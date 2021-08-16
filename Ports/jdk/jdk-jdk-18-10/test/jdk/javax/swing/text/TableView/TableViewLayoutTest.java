/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8133864 8158209
 * @summary  Wrong display, when the document I18n properties is true.
 */
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class TableViewLayoutTest extends JFrame {

    private static double yCaret;
    private static double xCaret;

    // Number of iteration to verify the stability of the test with different robot delays :
    // Work well with robot.delay(50) in hitKey method.
    // But if the robot delay is too low, the test is not stable.
    // Put this to 100, and reduce robot delay sometimes answers may be different.
    private static int tn = 2;

    // The four caret positions to test.
    private static double yCarFLTab;
    private static double yCarLLTab;
    private static double xCarBTab;
    private static double xCarETab;

    // The caret coordonate differences along axis after the insertion and the removing cycle.
    // 0 if the table layout is right.
    private static double dyCarFLTab;
    private static double dyCarLLTab;
    private static double dxCarBTab;
    private static double dxCarETab;

    private static JEditorPane edit = new JEditorPane();
    private static TableViewLayoutTest frame;

    private static String Prop = "\n";
    private static boolean isTabWrong = Boolean.FALSE;

    private static Boolean isI18n = false;

    public TableViewLayoutTest() {

        super("Code example for a TableView bug");
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        edit.setEditorKit(new CodeBugEditorKit());
        initCodeBug();
        this.getContentPane().add(new JScrollPane(edit));
        this.pack();
        this.setLocationRelativeTo(null);

        edit.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                JTextComponent textComp = (JTextComponent) e.getSource();
                try {
                    Rectangle rect = textComp.getUI().modelToView(textComp, e.getDot());
                    yCaret = rect.getY();
                    xCaret = rect.getX();
                } catch (BadLocationException ex) {
                    throw new RuntimeException("Failed to get pixel position of caret", ex);
                }
            }
        });
    }

    private void initCodeBug() {
        CodeBugDocument doc = (CodeBugDocument) edit.getDocument();
        try {
            doc.insertString(0, "TextB  TextE", null);
        } catch (BadLocationException ex) {
        }
        doc.insertTable(6, 4, 3);
        try {
            doc.insertString(7, "Cell11", null);
            doc.insertString(14, "Cell12", null);
            doc.insertString(21, "Cell13", null);
            doc.insertString(28, "Cell21", null);
            doc.insertString(35, "Cell22", null);
            doc.insertString(42, "Cell23", null);
            doc.insertString(49, "Cell31", null);
            doc.insertString(56, "Cell32", null);
            doc.insertString(63, "Cell33", null);
            doc.insertString(70, "Cell41", null);
            doc.insertString(77, "Cell42", null);
            doc.insertString(84, "Cell43", null);
        } catch (BadLocationException ex) {
        }
    }

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < tn; i++) {
            Robot rob = new Robot();

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame = new TableViewLayoutTest();
                    frame.setVisible(true);
                }
            });

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    //Enable or disable i18n.
                    isI18n = !isI18n;
                    edit.getDocument().putProperty("i18n", isI18n);

                    //Made a change to update table layout.
                    //Without any change the table i18n property change is not take in account.
                    edit.select(11, 12);
                    edit.replaceSelection("1");

                    //Catch the four caret positions to test before insertions.
                    edit.setCaretPosition(6);
                    xCarBTab = xCaret;
                    edit.setCaretPosition(91);
                    xCarETab = xCaret;

                    edit.setCaretPosition(74);
                    yCarLLTab = yCaret;
                    edit.setCaretPosition(11);
                    yCarFLTab = yCaret;
                }
            });

            hitKey(rob, KeyEvent.VK_T);
            hitKey(rob, KeyEvent.VK_E);
            hitKey(rob, KeyEvent.VK_S);
            hitKey(rob, KeyEvent.VK_T);
            hitKey(rob, KeyEvent.VK_BACK_SPACE);
            hitKey(rob, KeyEvent.VK_BACK_SPACE);
            hitKey(rob, KeyEvent.VK_BACK_SPACE);
            hitKey(rob, KeyEvent.VK_BACK_SPACE);

            rob.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    //Calculate caret coordinate differences and catch caret positions after insertions.
                    edit.setCaretPosition(6);
                    dxCarBTab = Math.abs(xCarBTab - xCaret);
                    edit.setCaretPosition(91);
                    dxCarETab = Math.abs(xCarETab - xCaret);

                    edit.setCaretPosition(74);
                    dyCarLLTab = Math.abs(yCarLLTab - yCaret);
                    edit.setCaretPosition(11);
                    dyCarFLTab = Math.abs(yCarFLTab - yCaret);

                    edit.setCaretPosition(74);
                    yCarLLTab = yCaret;
                    edit.setCaretPosition(11);
                    yCarFLTab = yCaret;
                }
            });

            Object dp = edit.getDocument().getProperty("i18n");
            Boolean isI18n = dp instanceof Boolean ? (Boolean) dp : Boolean.FALSE;
            String i18n = isI18n ? "\nWhen i18n enable, " : "\nWhen i18n disable, ";

            if (Math.abs(yCarFLTab - yCarLLTab) < 10) {
                isTabWrong = Boolean.TRUE;
                Prop = Prop + i18n + "test can't be completed : TableView layout wrong, lines overlap, see JDK-8133864.";
            } else {
                if (dyCarFLTab != 0 || dyCarLLTab != 0) {
                    isTabWrong = Boolean.TRUE;
                    Prop = Prop + i18n + "TableView layout wrong : Table high change when inserts and removes caracters, bug never reported yet. First Line dy=" + dyCarFLTab + " Last Line dy=" + dyCarLLTab;
                }
                if (dxCarBTab != 0 || dxCarETab != 0) {
                    isTabWrong = Boolean.TRUE;
                    Prop = Prop + i18n + "TableView layout wrong : Table width change when inserts and removes caracters, see JDK-8158209 and JDK-7169915. Before Table dx=" + dxCarBTab + " After Table dx=" + dxCarETab;
                }
            }
            rob.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.dispose();
                }
            });
        }
        if (isTabWrong) {
            throw new RuntimeException(Prop);
        }

        System.out.println("ok");
    }

    private static void hitKey(Robot robot, int k) throws Exception {
        robot.delay(50);
        robot.keyPress(k);
        robot.keyRelease(k);
        robot.delay(50);
    }
}

//------------------------------------------------------------------------------
class CodeBugEditorKit extends StyledEditorKit {

    ViewFactory defaultFactory = new TableFactory();

    @Override
    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    @Override
    public Document createDefaultDocument() {
        return new CodeBugDocument();
    }
}
//------------------------------------------------------------------------------

class TableFactory implements ViewFactory {

    @Override
    public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
            if (kind.equals(AbstractDocument.ContentElementName)) {
                return new LabelView(elem);
            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                return new ParagraphView(elem);
            } else if (kind.equals(AbstractDocument.SectionElementName)) {
                return new BoxView(elem, View.Y_AXIS);
            } else if (kind.equals(StyleConstants.ComponentElementName)) {
                return new ComponentView(elem);
            } else if (kind.equals(CodeBugDocument.ELEMENT_TABLE)) {
                return new tableView(elem);
            } else if (kind.equals(StyleConstants.IconElementName)) {
                return new IconView(elem);
            }
        }
        // default to text display
        return new LabelView(elem);

    }
}
//------------------------------------------------------------------------------

//------------------------------------------------------------------------------
class tableView extends TableView implements ViewFactory {

    public tableView(Element elem) {
        super(elem);
    }

    @Override
    public void setParent(View parent) {
        super.setParent(parent);
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
    }

    @Override
    public ViewFactory getViewFactory() {
        return this;
    }

    @Override
    public float getMinimumSpan(int axis) {
        return getPreferredSpan(axis);
    }

    @Override
    public float getMaximumSpan(int axis) {
        return getPreferredSpan(axis);
    }

    @Override
    public float getAlignment(int axis) {
        return 0.5f;
    }

    @Override
    public float getPreferredSpan(int axis) {
        if (axis == 0) {
            return super.getPreferredSpan(0);
        }
        float preferredSpan = super.getPreferredSpan(axis);
        return preferredSpan;
    }

    @Override
    public void paint(Graphics g, Shape allocation) {
        super.paint(g, allocation);
        Rectangle alloc = allocation.getBounds();
        int lastY = alloc.y + alloc.height - 1;
        g.drawLine(alloc.x, lastY, alloc.x + alloc.width, lastY);
    }

    @Override
    protected void paintChild(Graphics g, Rectangle alloc, int index) {
        super.paintChild(g, alloc, index);
        int lastX = alloc.x + alloc.width;
        g.drawLine(alloc.x, alloc.y, lastX, alloc.y);
    }

    @Override
    public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
            if (kind.equals(CodeBugDocument.ELEMENT_TR)) {
                return new trView(elem);
            } else if (kind.equals(CodeBugDocument.ELEMENT_TD)) {
                return new BoxView(elem, View.Y_AXIS);

            }
        }

        // default is to delegate to the normal factory
        View p = getParent();
        if (p != null) {
            ViewFactory f = p.getViewFactory();
            if (f != null) {
                return f.create(elem);
            }
        }

        return null;
    }

    public class trView extends TableRow {

        @Override
        public void setParent(View parent) {
            super.setParent(parent);
        }

        public trView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        public float getMaximumSpan(int axis) {
            return getPreferredSpan(axis);
        }

        public float getAlignment(int axis) {
            return 0f;
        }

        @Override
        protected void paintChild(Graphics g, Rectangle alloc, int index) {
            super.paintChild(g, alloc, index);
            int lastY = alloc.y + alloc.height - 1;
            g.drawLine(alloc.x, alloc.y, alloc.x, lastY);
            int lastX = alloc.x + alloc.width;
            g.drawLine(lastX, alloc.y, lastX, lastY);
        }
    };
}

//------------------------------------------------------------------------------
class CodeBugDocument extends DefaultStyledDocument {

    public static final String ELEMENT_TABLE = "table";
    public static final String ELEMENT_TR = "table cells row";
    public static final String ELEMENT_TD = "table data cell";

    public CodeBugDocument() {
        //putProperty("i18n", Boolean.TRUE);
    }

    protected void insertTable(int offset, int rowCount, int colCount) {
        try {
            ArrayList Specs = new ArrayList();
            ElementSpec gapTag = new ElementSpec(new SimpleAttributeSet(),
                    ElementSpec.ContentType, "\n".toCharArray(), 0, 1);
            Specs.add(gapTag);

            SimpleAttributeSet tableAttrs = new SimpleAttributeSet();
            tableAttrs.addAttribute(ElementNameAttribute, ELEMENT_TABLE);
            ElementSpec tableStart
                    = new ElementSpec(tableAttrs, ElementSpec.StartTagType);
            Specs.add(tableStart); //start table tag

            fillRowSpecs(Specs, rowCount, colCount);

            ElementSpec[] spec = new ElementSpec[Specs.size()];
            Specs.toArray(spec);

            this.insert(offset, spec);
        } catch (BadLocationException ex) {
        }
    }

    protected void fillRowSpecs(ArrayList Specs, int rowCount, int colCount) {
        SimpleAttributeSet rowAttrs = new SimpleAttributeSet();
        rowAttrs.addAttribute(ElementNameAttribute, ELEMENT_TR);
        for (int i = 0; i < rowCount; i++) {
            ElementSpec rowStart
                    = new ElementSpec(rowAttrs, ElementSpec.StartTagType);
            Specs.add(rowStart);

            fillCellSpecs(Specs, colCount);

            ElementSpec rowEnd
                    = new ElementSpec(rowAttrs, ElementSpec.EndTagType);
            Specs.add(rowEnd);
        }

    }

    protected void fillCellSpecs(ArrayList Specs, int colCount) {
        for (int i = 0; i < colCount; i++) {
            SimpleAttributeSet cellAttrs = new SimpleAttributeSet();
            cellAttrs.addAttribute(ElementNameAttribute, ELEMENT_TD);

            ElementSpec cellStart
                    = new ElementSpec(cellAttrs, ElementSpec.StartTagType);
            Specs.add(cellStart);

            ElementSpec parStart = new ElementSpec(new SimpleAttributeSet(),
                    ElementSpec.StartTagType);
            Specs.add(parStart);
            ElementSpec parContent = new ElementSpec(new SimpleAttributeSet(),
                    ElementSpec.ContentType, "\n".toCharArray(), 0, 1);
            Specs.add(parContent);
            ElementSpec parEnd = new ElementSpec(new SimpleAttributeSet(),
                    ElementSpec.EndTagType);
            Specs.add(parEnd);
            ElementSpec cellEnd
                    = new ElementSpec(cellAttrs, ElementSpec.EndTagType);
            Specs.add(cellEnd);
        }
    }
}
