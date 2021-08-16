/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui;

import java.math.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.dummy.*;
import sun.jvm.hotspot.utilities.*;

/** A subclass of JPanel which displays a hex dump of memory along
    with annotations describing the significance of various
    pieces. This can be used to implement either a stack or heap
    inspector. */

public class AnnotatedMemoryPanel extends JPanel {
  private boolean is64Bit;
  private Debugger debugger;
  private long    addressSize;
  private HighPrecisionJScrollBar scrollBar;
  private Font font;
  private int bytesPerLine;
  private int paintCount;
  private String unmappedAddrString;
  // Type of this is an IntervalTree indexed by Interval<Address> and
  // with user data of type Annotation
  private IntervalTree annotations =
    new IntervalTree(new Comparator<>() {
        public int compare(Object o1, Object o2) {
          Address a1 = (Address) o1;
          Address a2 = (Address) o2;

          if ((a1 == null) && (a2 == null)) {
            return 0;
          } else if (a1 == null) {
            return -1;
          } else if (a2 == null) {
            return 1;
          }

          if (a1.equals(a2)) {
            return 0;
          } else if (a1.lessThan(a2)) {
            return -1;
          }
          return 1;
        }
      });
  // Keep track of the last start address at which we painted, so we
  // can scroll annotations
  private Address lastStartAddr;
  // This contains the list of currently-visible IntervalNodes, in
  // sorted order by their low endpoint, in the form of a
  // List<Annotation>. These annotations have already been laid out.
  private java.util.List<Annotation> visibleAnnotations;
  // Darker colors than defaults for better readability
  private static Color[] colors = {
    new Color(0.0f, 0.0f, 0.6f), // blue
    new Color(0.6f, 0.0f, 0.6f), // magenta
    new Color(0.0f, 0.8f, 0.0f), // green
    new Color(0.8f, 0.3f, 0.0f), // orange
    new Color(0.0f, 0.6f, 0.8f), // cyan
    new Color(0.2f, 0.2f, 0.2f), // dark gray
  };

  /** Default is 32-bit mode */
  public AnnotatedMemoryPanel(Debugger debugger) {
    this(debugger, false);
  }

  public AnnotatedMemoryPanel(Debugger debugger, boolean is64Bit, Address addrValue, Address addrLow, Address addrHigh) {
    super();
    init(debugger, is64Bit, addressToBigInt(addrValue), addressToBigInt(addrLow), addressToBigInt(addrHigh));
  }

  public AnnotatedMemoryPanel(Debugger debugger, boolean is64Bit ) {
    super();
    init(debugger, is64Bit, defaultMemoryLocation(is64Bit), defaultMemoryLow(is64Bit), defaultMemoryHigh(is64Bit));
  }

  static class AnnoX {
    int     lineX;
    Address highBound;

    public AnnoX(int lineX, Address highBound) {
      this.lineX = lineX;
      this.highBound = highBound;
    }
  }

  public synchronized void paintComponent(Graphics g) {
    //    System.err.println("AnnotatedMemoryPanel.paintComponent() " + ++paintCount);
    super.paintComponent(g);

    // Clone the Graphics so we don't screw up its state for Swing
    // drawing (as this code otherwise does)
    g = g.create();

    g.setFont(font);
    g.setColor(Color.black);
    Rectangle rect = new Rectangle();
    getBounds(rect);
    String firstAddressString = null;
    int lineHeight;
    int addrWidth;
    {
      Rectangle2D bounds = GraphicsUtilities.getStringBounds(unmappedAddrString, g);
      lineHeight = (int) bounds.getHeight();
      addrWidth  = (int) bounds.getWidth();
    }
    int addrX = (int) (0.25 * addrWidth);
    int dataX = (int) (addrX + (1.5 * addrWidth));
    int lineStartX = dataX + addrWidth + 5;
    int annoStartX = (int) (lineStartX + (0.75 * addrWidth));

    int numLines = rect.height / lineHeight;

    BigInteger startVal  = scrollBar.getValueHP();
    BigInteger perLine = new BigInteger(Integer.toString((int) addressSize));
    // lineCount and maxLines are both 1 less than expected
    BigInteger lineCount = new BigInteger(Integer.toString((int) (numLines - 1)));
    BigInteger maxLines = scrollBar.getMaximumHP().subtract(scrollBar.getMinimumHP()).divide(perLine);
    if (lineCount.compareTo(maxLines) > 0) {
      lineCount = maxLines;
    }
    BigInteger offsetVal = lineCount.multiply(perLine);
    BigInteger endVal    = startVal.add(offsetVal);
    if (endVal.compareTo(scrollBar.getMaximumHP()) > 0) {
      startVal = scrollBar.getMaximumHP().subtract(offsetVal);
      endVal   = scrollBar.getMaximumHP();
      // Sure seems like this call will cause us to immediately redraw...
      scrollBar.setValueHP(startVal);
    }
    scrollBar.setVisibleAmountHP(offsetVal.add(perLine));
    scrollBar.setBlockIncrementHP(offsetVal);

    Address startAddr = bigIntToAddress(startVal);
    Address endAddr   = bigIntToAddress(endVal);

    // Scroll last-known annotations
    int scrollOffset = 0;
    if (lastStartAddr != null) {
      scrollOffset = (int) lastStartAddr.minus(startAddr);
    } else {
      if (startAddr != null) {
        scrollOffset = (int) (-1 * startAddr.minus(lastStartAddr));
      }
    }
    scrollOffset = scrollOffset * lineHeight / (int) addressSize;
    scrollAnnotations(scrollOffset);
    lastStartAddr = startAddr;

    int curY = lineHeight;
    Address curAddr = startAddr;
    for (int i = 0; i < numLines; i++) {
      String s = bigIntToHexString(startVal);
      g.drawString(s, addrX, curY);
      try {
        s = addressToString(startAddr.getAddressAt(i * addressSize));
      }
      catch (UnmappedAddressException e) {
        s = unmappedAddrString;
      }
      g.drawString(s, dataX, curY);
      curY += lineHeight;
      startVal = startVal.add(perLine);
    }

    // Query for visible annotations (little slop to ensure we get the
    // top and bottom)
    // FIXME: it would be nice to have a more static layout; that is,
    // if something scrolls off the bottom of the screen, other
    // annotations still visible shouldn't change position
    java.util.List<IntervalNode> va =
      annotations.findAllNodesIntersecting(new Interval(startAddr.addOffsetTo(-addressSize),
                                                        endAddr.addOffsetTo(2 * addressSize)));

    // Render them
    int curLineX = lineStartX;
    int curTextX = annoStartX;
    int curColorIdx = 0;
    if (g instanceof Graphics2D) {
      Stroke stroke = new BasicStroke(3.0f);
      ((Graphics2D) g).setStroke(stroke);
    }

    Stack<AnnoX> drawStack = new Stack<>();

    layoutAnnotations(va, g, curTextX, startAddr, lineHeight);

    for (Iterator<Annotation> iter = visibleAnnotations.iterator(); iter.hasNext(); ) {
      Annotation anno   = iter.next();
      Interval interval = anno.getInterval();

      if (!drawStack.empty()) {
        // See whether we can pop any items off the stack
        boolean shouldContinue = true;
        do {
          AnnoX annoX = (AnnoX) drawStack.peek();
          if (annoX.highBound.lessThanOrEqual((Address) interval.getLowEndpoint())) {
            curLineX = annoX.lineX;
            drawStack.pop();
            shouldContinue = !drawStack.empty();
          } else {
            shouldContinue = false;
          }
        } while (shouldContinue);
      }

      // Draw a line covering the interval
      Address lineStartAddr = (Address) interval.getLowEndpoint();
      // Give it a little slop
      int lineStartY = (int) (lineStartAddr.minus(startAddr) * lineHeight / addressSize) +
        (lineHeight / 3);
      Address lineEndAddr = (Address) interval.getHighEndpoint();
      drawStack.push(new AnnoX(curLineX, lineEndAddr));
      int lineEndY = (int) (lineEndAddr.minus(startAddr) * lineHeight / addressSize);
      g.setColor(anno.getColor());
      g.drawLine(curLineX, lineStartY, curLineX, lineEndY);
      // Draw line to text
      g.drawLine(curLineX, lineStartY, curTextX - 10, anno.getY() - (lineHeight / 2));
      curLineX += 8;
      anno.draw(g);
      ++curColorIdx;
    }
  }

  /** Add an annotation covering the address range [annotation.lowAddress,
      annotation.highAddress); that is, it includes the low address and does not
      include the high address. */
  public synchronized void addAnnotation(Annotation annotation) {
    annotations.insert(annotation.getInterval(), annotation);
  }

  /** Makes the given address visible somewhere in the window */
  public synchronized void makeVisible(Address addr) {
    BigInteger bi = addressToBigInt(addr);
    scrollBar.setValueHP(bi);
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    annotations.printOn(tty);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void init(Debugger debugger, boolean is64Bit, BigInteger addrValue, BigInteger addrLow, BigInteger addrHigh) {
    this.is64Bit = is64Bit;
    this.debugger = debugger;
    if (is64Bit) {
      addressSize = 8;
      unmappedAddrString = "??????????????????";
    } else {
      addressSize = 4;
      unmappedAddrString = "??????????";
    }
    setLayout(new BorderLayout());
    setupScrollBar(addrValue, addrLow, addrHigh);
    add(scrollBar, BorderLayout.EAST);
    visibleAnnotations = new ArrayList<>();
    setBackground(Color.white);
    addHierarchyBoundsListener(new HierarchyBoundsListener() {
        public void ancestorMoved(HierarchyEvent e) {
        }

        public void ancestorResized(HierarchyEvent e) {
          // FIXME: should perform incremental layout
          //          System.err.println("Ancestor resized");
        }
      });

    if (font == null) {
      font = GraphicsUtilities.getMonospacedFont();
    }
    if (font == null) {
      throw new RuntimeException("Error looking up monospace font Courier");
    }
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "PageDown");
    getActionMap().put("PageDown", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          scrollBar.setValueHP(scrollBar.getValueHP().add(scrollBar.getBlockIncrementHP()));
        }
      });
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "PageUp");
    getActionMap().put("PageUp", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          scrollBar.setValueHP(scrollBar.getValueHP().subtract(scrollBar.getBlockIncrementHP()));
        }
      });
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down");
    getActionMap().put("Down", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          scrollBar.setValueHP(scrollBar.getValueHP().add(scrollBar.getUnitIncrementHP()));
        }
      });
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up");
    getActionMap().put("Up", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          scrollBar.setValueHP(scrollBar.getValueHP().subtract(scrollBar.getUnitIncrementHP()));
        }
      });
    setEnabled(true);
  }

  private void setupScrollBar(BigInteger value, BigInteger min, BigInteger max) {
    scrollBar = new HighPrecisionJScrollBar( Scrollbar.VERTICAL, value, min, max);
    if (is64Bit) {
      bytesPerLine = 8;
      // 64-bit mode
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40}));
    } else {
      // 32-bit mode
      bytesPerLine = 4;
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20}));
    }
    scrollBar.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          HighPrecisionJScrollBar h = (HighPrecisionJScrollBar) e.getSource();
          repaint();
        }
      });
  }

  private static BigInteger defaultMemoryLocation(boolean is64Bit) {
    if (is64Bit) {
      return new BigInteger(1, new byte[] {
                           (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                           (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    } else {
      return new BigInteger(1, new byte[] { (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    }
  }

  private static BigInteger defaultMemoryLow(boolean is64Bit) {
    if (is64Bit) {
      return new BigInteger(1, new byte[] {
                 (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                 (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    } else {
      return new BigInteger(1, new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
    }
  }

  private static BigInteger defaultMemoryHigh(boolean is64Bit) {
    if (is64Bit) {
      return new BigInteger(1, new byte[] {
                 (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                 (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC});
    } else {
      return new BigInteger(1, new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC});
    }
  }

  private void setupScrollBar() {
    setupScrollBar(defaultMemoryLocation(is64Bit),
                   defaultMemoryLow(is64Bit),
                   defaultMemoryHigh(is64Bit));
  }

  private String bigIntToHexString(BigInteger bi) {
    StringBuilder buf = new StringBuilder();
    buf.append("0x");
    String val = bi.toString(16);
    for (int i = 0; i < ((2 * addressSize) - val.length()); i++) {
      buf.append('0');
    }
    buf.append(val);
    return buf.toString();
  }

  private Address bigIntToAddress(BigInteger i) {
    String s = bigIntToHexString(i);
    return debugger.parseAddress(s);
  }

  private BigInteger addressToBigInt(Address a) {
    String s = addressToString(a);
    if (!s.startsWith("0x")) {
      throw new NumberFormatException(s);
    }
    return new BigInteger(s.substring(2), 16);
  }

  private String addressToString(Address a) {
    if (a == null) {
      if (is64Bit) {
        return "0x0000000000000000";
      } else {
        return "0x00000000";
      }
    }
    return a.toString();
  }

  /** Scrolls the visible annotations by the given Y amount */
  private void scrollAnnotations(int y) {
    for (Iterator<Annotation> iter = visibleAnnotations.iterator(); iter.hasNext(); ) {
      Annotation anno = iter.next();
      anno.setY(anno.getY() + y);
    }
  }

  /** Takes the list of currently-visible annotations (in the form of
      a List<IntervalNode>) and lays them out given the current
      visible position and the already-visible annotations. Does not
      perturb the layouts of the currently-visible annotations. */
  private void layoutAnnotations(java.util.List<IntervalNode> va,
                                 Graphics g,
                                 int x,
                                 Address startAddr,
                                 int lineHeight) {
    // Handle degenerate case early: no visible annotations.
    if (va.size() == 0) {
      visibleAnnotations.clear();
      return;
    }

    // We have two ranges of visible annotations: the one from the
    // last repaint and the currently visible set. We want to preserve
    // the layouts of the previously-visible annotations that are
    // currently visible (to avoid color flashing, jumping, etc.)
    // while making the coloring of the new annotations fit as well as
    // possible. Note that annotations may appear and disappear from
    // any point in the previously-visible list, but the ordering of
    // the visible annotations is always the same.

    // This is really a constrained graph-coloring problem. This
    // simple algorithm only takes into account half of the
    // constraints (for example, the layout of the previous
    // annotation, where it should be taking into account the layout
    // of the previous and next annotations that were in the
    // previously-visible list). There are situations where it can
    // generate overlapping annotations and adjacent annotations with
    // the same color; generally visible when scrolling line-by-line
    // rather than page-by-page. In some of these situations, will
    // have to move previously laid-out annotations. FIXME: revisit
    // this.

    // Index of last annotation which we didn't know how to lay out
    int deferredIndex = -1;
    // We "lay out after" this one
    Annotation constraintAnnotation = null;
    // The first constraint annotation
    Annotation firstConstraintAnnotation = null;
    // The index from which we search forward in the
    // visibleAnnotations list. This reduces the amount of work we do.
    int searchIndex = 0;
    // The new set of annotations
    java.util.List<Annotation> newAnnos = new ArrayList<>();

    for (Iterator<IntervalNode> iter = va.iterator(); iter.hasNext(); ) {
      Annotation anno = (Annotation) ((IntervalNode) iter.next()).getData();

      // Search forward for this one
      boolean found = false;
      for (int i = searchIndex; i < visibleAnnotations.size(); i++) {
        Annotation el = visibleAnnotations.get(i);
        // See whether we can abort the search unsuccessfully because
        // we went forward too far
        if (el.getLowAddress().greaterThan(anno.getLowAddress())) {
          break;
        }
        if (el == anno) {
          // Found successfully.
          found = true;
          searchIndex = i;
          constraintAnnotation = anno;
          if (firstConstraintAnnotation == null) {
            firstConstraintAnnotation = constraintAnnotation;
          }
          break;
        }
      }

      if (!found) {
        if (constraintAnnotation != null) {
          layoutAfter(anno, constraintAnnotation, g, x, startAddr, lineHeight);
          constraintAnnotation = anno;
        } else {
          // Defer layout of this annotation until later
          ++deferredIndex;
        }
      }

      newAnnos.add(anno);
    }

    if (firstConstraintAnnotation != null) {
      // Go back and lay out deferred annotations
      for (int i = deferredIndex; i >= 0; i--) {
        Annotation anno = (Annotation) newAnnos.get(i);
        layoutBefore(anno, firstConstraintAnnotation, g, x, startAddr, lineHeight);
        firstConstraintAnnotation = anno;
      }
    } else {
      // Didn't find any overlap between the old and new annotations.
      // Lay out in a feed-forward fashion.
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(constraintAnnotation == null, "logic error in layout code");
      }
      for (Iterator iter = newAnnos.iterator(); iter.hasNext(); ) {
        Annotation anno = (Annotation) iter.next();
        layoutAfter(anno, constraintAnnotation, g, x, startAddr, lineHeight);
        constraintAnnotation = anno;
      }
    }

    visibleAnnotations = newAnnos;
  }

  /** Lays out the given annotation before the optional constraint
      annotation, obeying constraints imposed by that annotation if it
      is specified. */
  private void layoutBefore(Annotation anno, Annotation constraintAnno,
                            Graphics g, int x,
                            Address startAddr, int lineHeight) {
    anno.computeWidthAndHeight(g);
    // Color
    if (constraintAnno != null) {
      anno.setColor(prevColor(constraintAnno.getColor()));
    } else {
      anno.setColor(colors[0]);
    }
    // X
    anno.setX(x);
    // Tentative Y
    anno.setY((int) (((Address) anno.getInterval().getLowEndpoint()).minus(startAddr) * lineHeight / addressSize) +
              (5 * lineHeight / 6));
    // See whether Y overlaps with last anno's Y; if so, move this one up
    if ((constraintAnno != null) && (anno.getY() + anno.getHeight() > constraintAnno.getY())) {
      anno.setY(constraintAnno.getY() - anno.getHeight());
    }
  }

  /** Lays out the given annotation after the optional constraint
      annotation, obeying constraints imposed by that annotation if it
      is specified. */
  private void layoutAfter(Annotation anno, Annotation constraintAnno,
                           Graphics g, int x,
                           Address startAddr, int lineHeight) {
    anno.computeWidthAndHeight(g);
    // Color
    if (constraintAnno != null) {
      anno.setColor(nextColor(constraintAnno.getColor()));
    } else {
      anno.setColor(colors[0]);
    }
    // X
    anno.setX(x);
    // Tentative Y
    anno.setY((int) (((Address) anno.getInterval().getLowEndpoint()).minus(startAddr) * lineHeight / addressSize) +
              (5 * lineHeight / 6));
    // See whether Y overlaps with last anno's Y; if so, move this one down
    if ((constraintAnno != null) && (anno.getY() < (constraintAnno.getY() + constraintAnno.getHeight()))) {
      anno.setY(constraintAnno.getY() + constraintAnno.getHeight());
    }
  }

  /** Returns previous color in our color palette */
  private Color prevColor(Color c) {
    int i = findColorIndex(c);
    if (i == 0) {
      return colors[colors.length - 1];
    } else {
      return colors[i - 1];
    }
  }

  /** Returns next color in our color palette */
  private Color nextColor(Color c) {
    return colors[(findColorIndex(c) + 1) % colors.length];
  }

  private int findColorIndex(Color c) {
    for (int i = 0; i < colors.length; i++) {
      if (colors[i] == c) {
        return i;
      }
    }
    throw new IllegalArgumentException();
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    DummyDebugger debugger = new DummyDebugger(new MachineDescriptionIntelX86());
    AnnotatedMemoryPanel anno = new AnnotatedMemoryPanel(debugger);
    frame.getContentPane().add(anno);
    anno.addAnnotation(new Annotation(debugger.parseAddress("0x80000000"),
                                      debugger.parseAddress("0x80000040"),
                                      "Stack Frame for \"foo\""));
    anno.addAnnotation(new Annotation(debugger.parseAddress("0x80000010"),
                                      debugger.parseAddress("0x80000020"),
                                      "Locals for \"foo\""));
    anno.addAnnotation(new Annotation(debugger.parseAddress("0x80000020"),
                                      debugger.parseAddress("0x80000030"),
                                      "Expression stack for \"foo\""));

    frame.setSize(400, 300);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosed(WindowEvent e) {
          System.exit(0);
        }
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });
    frame.setVisible(true);
  }
}
