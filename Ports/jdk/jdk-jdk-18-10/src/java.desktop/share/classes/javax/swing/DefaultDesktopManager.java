/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyVetoException;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

/** This is an implementation of the <code>DesktopManager</code>.
  * It currently implements the basic behaviors for managing
  * <code>JInternalFrame</code>s in an arbitrary parent.
  * <code>JInternalFrame</code>s that are not children of a
  * <code>JDesktop</code> will use this component
  * to handle their desktop-like actions.
  * <p>This class provides a policy for the various JInternalFrame methods,
  * it is not meant to be called directly rather the various JInternalFrame
  * methods will call into the DesktopManager.</p>
  * @see JDesktopPane
  * @see JInternalFrame
  * @author David Kloba
  * @author Steve Wilson
  * @since 1.2
  */
@SuppressWarnings("serial") // No Interesting Non-Transient State
public class DefaultDesktopManager implements DesktopManager, java.io.Serializable {
    static final String HAS_BEEN_ICONIFIED_PROPERTY = "wasIconOnce";

    static final int DEFAULT_DRAG_MODE = 0;
    static final int OUTLINE_DRAG_MODE = 1;
    static final int FASTER_DRAG_MODE = 2;

    int dragMode = DEFAULT_DRAG_MODE;

    private transient Rectangle currentBounds = null;
    private transient Graphics desktopGraphics = null;
    private transient Rectangle desktopBounds = null;
    private transient Rectangle[] floatingItems = {};

    /**
     * Set to true when the user actually drags a frame vs clicks on it
     * to start the drag operation.  This is only used when dragging with
     * FASTER_DRAG_MODE.
     */
    private transient boolean didDrag;

    /**
     * Constructs a {@code DefaultDesktopManager}.
     */
    public DefaultDesktopManager() {}

    /** Normally this method will not be called. If it is, it
      * tries to determine the appropriate parent from the desktopIcon of the frame.
      * Will remove the desktopIcon from its parent if it successfully adds the frame.
      */
    public void openFrame(JInternalFrame f) {
        if(f.getDesktopIcon().getParent() != null) {
            f.getDesktopIcon().getParent().add(f);
            removeIconFor(f);
        }
    }

    /**
     * Removes the frame, and, if necessary, the
     * <code>desktopIcon</code>, from its parent.
     * @param f the <code>JInternalFrame</code> to be removed
     */
    public void closeFrame(JInternalFrame f) {
        JDesktopPane d = f.getDesktopPane();
        if (d == null) {
            return;
        }
        boolean findNext = f.isSelected();
        Container c = f.getParent();
        JInternalFrame nextFrame = null;
        if (findNext) {
            nextFrame = d.getNextFrame(f);
            try { f.setSelected(false); } catch (PropertyVetoException e2) { }
        }
        if(c != null) {
            c.remove(f); // Removes the focus.
            c.repaint(f.getX(), f.getY(), f.getWidth(), f.getHeight());
        }
        removeIconFor(f);
        if(f.getNormalBounds() != null)
            f.setNormalBounds(null);
        if(wasIcon(f))
            setWasIcon(f, null);
        if (nextFrame != null) {
            try { nextFrame.setSelected(true); }
            catch (PropertyVetoException e2) { }
        } else if (findNext && d.getComponentCount() == 0) {
            // It was selected and was the last component on the desktop.
            d.requestFocus();
        }
    }

    /**
     * Resizes the frame to fill its parents bounds.
     * @param f the frame to be resized
     */
    public void maximizeFrame(JInternalFrame f) {
        if (f.isIcon()) {
            try {
                // In turn calls deiconifyFrame in the desktop manager.
                // That method will handle the maximization of the frame.
                f.setIcon(false);
            } catch (PropertyVetoException e2) {
            }
        } else {
            Container c = f.getParent();
            if (c == null) {
                return;
            }
            f.setNormalBounds(f.getBounds());
            Rectangle desktopBounds = c.getBounds();
            setBoundsForFrame(f, 0, 0,
                desktopBounds.width, desktopBounds.height);
        }

        // Set the maximized frame as selected.
        try {
            f.setSelected(true);
        } catch (PropertyVetoException e2) {
        }
    }

    /**
     * Restores the frame back to its size and position prior
     * to a <code>maximizeFrame</code> call.
     * @param f the <code>JInternalFrame</code> to be restored
     */
    public void minimizeFrame(JInternalFrame f) {
        // If the frame was an icon restore it back to an icon.
        if (f.isIcon()) {
            iconifyFrame(f);
            return;
        }

        if ((f.getNormalBounds()) != null) {
            Rectangle r = f.getNormalBounds();
            f.setNormalBounds(null);
            try { f.setSelected(true); } catch (PropertyVetoException e2) { }
            setBoundsForFrame(f, r.x, r.y, r.width, r.height);
        }
    }

    /**
     * Removes the frame from its parent and adds its
     * <code>desktopIcon</code> to the parent.
     * @param f the <code>JInternalFrame</code> to be iconified
     */
    public void iconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon desktopIcon;
        Container c = f.getParent();
        JDesktopPane d = f.getDesktopPane();
        boolean findNext = f.isSelected();
        desktopIcon = f.getDesktopIcon();
        if(!wasIcon(f)) {
            Rectangle r = getBoundsForIconOf(f);
            desktopIcon.setBounds(r.x, r.y, r.width, r.height);
            // we must validate the hierarchy to not break the hw/lw mixing
            desktopIcon.revalidate();
            setWasIcon(f, Boolean.TRUE);
        }

        if (c == null || d == null) {
            return;
        }

        if (c instanceof JLayeredPane) {
            JLayeredPane lp = (JLayeredPane)c;
            int layer = JLayeredPane.getLayer(f);
            JLayeredPane.putLayer(desktopIcon, layer);
        }
        d.setComponentOrderCheckingEnabled(true);
        c.remove(f);
        c.add(desktopIcon);
        if (findNext) {
            if (d.selectFrame(true) == null) {
                // The icon is the last frame.
                f.restoreSubcomponentFocus();
            }
        }
        c.repaint(f.getX(), f.getY(), f.getWidth(), f.getHeight());
    }

    /**
     * Removes the desktopIcon from its parent and adds its frame
     * to the parent.
     * @param f the <code>JInternalFrame</code> to be de-iconified
     */
    public void deiconifyFrame(JInternalFrame f) {
        JInternalFrame.JDesktopIcon desktopIcon = f.getDesktopIcon();
        Container c = desktopIcon.getParent();
        JDesktopPane d = f.getDesktopPane();
        if (c != null && d != null) {
            c.add(f);
            // If the frame is to be restored to a maximized state make
            // sure it still fills the whole desktop.
            if (f.isMaximum()) {
                Rectangle desktopBounds = c.getBounds();
                if (f.getWidth() != desktopBounds.width ||
                        f.getHeight() != desktopBounds.height) {
                    setBoundsForFrame(f, 0, 0,
                        desktopBounds.width, desktopBounds.height);
                }
            }
            removeIconFor(f);
            if (f.isSelected()) {
                f.moveToFront();
                f.restoreSubcomponentFocus();
            }
            else {
                try {
                    f.setSelected(true);
                } catch (PropertyVetoException e2) {}

            }
        }
    }

    /** This will activate <b>f</b> moving it to the front. It will
      * set the current active frame's (if any)
      * <code>IS_SELECTED_PROPERTY</code> to <code>false</code>.
      * There can be only one active frame across all Layers.
      * @param f the <code>JInternalFrame</code> to be activated
      */
    public void activateFrame(JInternalFrame f) {
        Container p = f.getParent();
        Component[] c;
        JDesktopPane d = f.getDesktopPane();
        JInternalFrame currentlyActiveFrame =
          (d == null) ? null : d.getSelectedFrame();
        // fix for bug: 4162443
        if(p == null) {
            // If the frame is not in parent, its icon maybe, check it
            p = f.getDesktopIcon().getParent();
            if(p == null)
                return;
        }
        // we only need to keep track of the currentActive InternalFrame, if any
        if (currentlyActiveFrame == null){
          if (d != null) { d.setSelectedFrame(f);}
        } else if (currentlyActiveFrame != f) {
          // if not the same frame as the current active
          // we deactivate the current
          if (currentlyActiveFrame.isSelected()) {
            try {
              currentlyActiveFrame.setSelected(false);
            }
            catch(PropertyVetoException e2) {}
          }
          if (d != null) { d.setSelectedFrame(f);}
        }
        f.moveToFront();
    }

    // implements javax.swing.DesktopManager
    public void deactivateFrame(JInternalFrame f) {
      JDesktopPane d = f.getDesktopPane();
      JInternalFrame currentlyActiveFrame =
          (d == null) ? null : d.getSelectedFrame();
      if (currentlyActiveFrame == f)
        d.setSelectedFrame(null);
    }

    // implements javax.swing.DesktopManager
    public void beginDraggingFrame(JComponent f) {
        setupDragMode(f);

        if (dragMode == FASTER_DRAG_MODE) {
          Component desktop = f.getParent();
          floatingItems = findFloatingItems(f);
          currentBounds = f.getBounds();
          if (desktop instanceof JComponent) {
              desktopBounds = ((JComponent)desktop).getVisibleRect();
          }
          else {
              desktopBounds = desktop.getBounds();
              desktopBounds.x = desktopBounds.y = 0;
          }
          desktopGraphics = JComponent.safelyGetGraphics(desktop);
          ((JInternalFrame)f).isDragging = true;
          didDrag = false;
        }

    }

    private void setupDragMode(JComponent f) {
        JDesktopPane p = getDesktopPane(f);
        Container parent = f.getParent();
        dragMode = DEFAULT_DRAG_MODE;
        if (p != null) {
            String mode = (String)p.getClientProperty("JDesktopPane.dragMode");
            Window window = SwingUtilities.getWindowAncestor(f);
            if (window != null && !window.isOpaque()) {
                dragMode = DEFAULT_DRAG_MODE;
            } else if (mode != null && mode.equals("outline")) {
                dragMode = OUTLINE_DRAG_MODE;
            } else if (mode != null && mode.equals("faster")
                    && f instanceof JInternalFrame
                    && ((JInternalFrame)f).isOpaque() &&
                       (parent == null || parent.isOpaque())) {
                dragMode = FASTER_DRAG_MODE;
            } else {
                if (p.getDragMode() == JDesktopPane.OUTLINE_DRAG_MODE ) {
                    dragMode = OUTLINE_DRAG_MODE;
                } else if ( p.getDragMode() == JDesktopPane.LIVE_DRAG_MODE
                        && f instanceof JInternalFrame
                        && ((JInternalFrame)f).isOpaque()) {
                    dragMode = FASTER_DRAG_MODE;
                } else {
                    dragMode = DEFAULT_DRAG_MODE;
                }
            }
        }
    }

    private transient Point currentLoc = null;

    /**
      * Moves the visible location of the frame being dragged
      * to the location specified.  The means by which this occurs can vary depending
      * on the dragging algorithm being used.  The actual logical location of the frame
      * might not change until <code>endDraggingFrame</code> is called.
      */
    public void dragFrame(JComponent f, int newX, int newY) {

        if (dragMode == OUTLINE_DRAG_MODE) {
            JDesktopPane desktopPane = getDesktopPane(f);
            if (desktopPane != null){
              Graphics g = JComponent.safelyGetGraphics(desktopPane);

              g.setXORMode(Color.white);
              if (currentLoc != null) {
                g.drawRect(currentLoc.x, currentLoc.y,
                        f.getWidth()-1, f.getHeight()-1);
              }
              g.drawRect( newX, newY, f.getWidth()-1, f.getHeight()-1);
              /* Work around for 6635462: XOR mode may cause a SurfaceLost on first use.
              * Swing doesn't expect that its XOR drawRect did
              * not complete, so believes that on re-entering at
              * the next update location, that there is an XOR rect
              * to draw out at "currentLoc". But in fact
              * its now got a new clean surface without that rect,
              * so drawing it "out" in fact draws it on, leaving garbage.
              * So only update/set currentLoc if the draw completed.
              */
              sun.java2d.SurfaceData sData =
                  ((sun.java2d.SunGraphics2D)g).getSurfaceData();

              if (!sData.isSurfaceLost()) {
                  currentLoc = new Point (newX, newY);
              }
;
              g.dispose();
            }
        } else if (dragMode == FASTER_DRAG_MODE) {
            dragFrameFaster(f, newX, newY);
        } else {
            setBoundsForFrame(f, newX, newY, f.getWidth(), f.getHeight());
        }
    }

    // implements javax.swing.DesktopManager
    public void endDraggingFrame(JComponent f) {
        if ( dragMode == OUTLINE_DRAG_MODE && currentLoc != null) {
            setBoundsForFrame(f, currentLoc.x, currentLoc.y, f.getWidth(), f.getHeight() );
            currentLoc = null;
        } else if (dragMode == FASTER_DRAG_MODE) {
            currentBounds = null;
            if (desktopGraphics != null) {
                desktopGraphics.dispose();
                desktopGraphics = null;
            }
            desktopBounds = null;
            ((JInternalFrame)f).isDragging = false;
        }
    }

    // implements javax.swing.DesktopManager
    public void beginResizingFrame(JComponent f, int direction) {
        setupDragMode(f);
    }

    /**
     * Calls <code>setBoundsForFrame</code> with the new values.
     * @param f the component to be resized
     * @param newX the new x-coordinate
     * @param newY the new y-coordinate
     * @param newWidth the new width
     * @param newHeight the new height
     */
    public void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {

        if ( dragMode == DEFAULT_DRAG_MODE || dragMode == FASTER_DRAG_MODE ) {
            setBoundsForFrame(f, newX, newY, newWidth, newHeight);
        } else {
            JDesktopPane desktopPane = getDesktopPane(f);
            if (desktopPane != null){
              Graphics g = JComponent.safelyGetGraphics(desktopPane);

              g.setXORMode(Color.white);
              if (currentBounds != null) {
                g.drawRect( currentBounds.x, currentBounds.y, currentBounds.width-1, currentBounds.height-1);
              }
              g.drawRect( newX, newY, newWidth-1, newHeight-1);

              // Work around for 6635462, see comment in dragFrame()
              sun.java2d.SurfaceData sData =
                  ((sun.java2d.SunGraphics2D)g).getSurfaceData();
              if (!sData.isSurfaceLost()) {
                  currentBounds = new Rectangle (newX, newY, newWidth, newHeight);
              }

              g.setPaintMode();
              g.dispose();
            }
        }

    }

    // implements javax.swing.DesktopManager
    public void endResizingFrame(JComponent f) {
        if ( dragMode == OUTLINE_DRAG_MODE && currentBounds != null) {
            setBoundsForFrame(f, currentBounds.x, currentBounds.y, currentBounds.width, currentBounds.height );
            currentBounds = null;
        }
    }


    /** This moves the <code>JComponent</code> and repaints the damaged areas. */
    public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
        f.setBounds(newX, newY, newWidth, newHeight);
        // we must validate the hierarchy to not break the hw/lw mixing
        f.revalidate();
    }

    /**
     * Convenience method to remove the desktopIcon of <b>f</b> is necessary.
     *
     * @param f the {@code JInternalFrame} for which to remove the
     *          {@code desktopIcon}
     */
    protected void removeIconFor(JInternalFrame f) {
        JInternalFrame.JDesktopIcon di = f.getDesktopIcon();
        Container c = di.getParent();
        if(c != null) {
            c.remove(di);
            c.repaint(di.getX(), di.getY(), di.getWidth(), di.getHeight());
        }
    }

    /**
     * The {@code iconifyFrame()} code calls this to determine the proper bounds
     * for the desktopIcon.
     *
     * @param f the {@code JInternalFrame} of interest
     * @return a {@code Rectangle} containing bounds for the {@code desktopIcon}
     */
    protected Rectangle getBoundsForIconOf(JInternalFrame f) {
      //
      // Get the icon for this internal frame and its preferred size
      //

      JInternalFrame.JDesktopIcon icon = f.getDesktopIcon();
      Dimension prefSize = icon.getPreferredSize();
      //
      // Get the parent bounds and child components.
      //

      Container c = f.getParent();
      if (c == null) {
          c = f.getDesktopIcon().getParent();
      }

      if (c == null) {
        /* the frame has not yet been added to the parent; how about (0,0) ?*/
        return new Rectangle(0, 0, prefSize.width, prefSize.height);
      }

      Rectangle parentBounds = c.getBounds();
      Component [] components = c.getComponents();


      //
      // Iterate through valid default icon locations and return the
      // first one that does not intersect any other icons.
      //

      Rectangle availableRectangle = null;
      JInternalFrame.JDesktopIcon currentIcon = null;

      int x = 0;
      int y = parentBounds.height - prefSize.height;
      int w = prefSize.width;
      int h = prefSize.height;

      boolean found = false;

      while (!found) {

        availableRectangle = new Rectangle(x,y,w,h);

        found = true;

        for ( int i=0; i<components.length; i++ ) {

          //
          // Get the icon for this component
          //

          if ( components[i] instanceof JInternalFrame ) {
            currentIcon = ((JInternalFrame)components[i]).getDesktopIcon();
          }
          else if ( components[i] instanceof JInternalFrame.JDesktopIcon ){
            currentIcon = (JInternalFrame.JDesktopIcon)components[i];
          } else
            /* found a child that's neither an internal frame nor
               an icon. I don't believe this should happen, but at
               present it does and causes a null pointer exception.
               Even when that gets fixed, this code protects against
               the npe. hania */
            continue;

          //
          // If this icon intersects the current location, get next location.
          //

          if ( !currentIcon.equals(icon) ) {
            if ( availableRectangle.intersects(currentIcon.getBounds()) ) {
              found = false;
              break;
            }
          }
        }

        if (currentIcon == null)
          /* didn't find any useful children above. This probably shouldn't
           happen, but this check protects against an npe if it ever does
           (and it's happening now) */
          return availableRectangle;

        x += currentIcon.getBounds().width;

        if ( x + w > parentBounds.width ) {
          x = 0;
          y -= h;
        }
      }

      return(availableRectangle);
    }

    /**
     * Stores the bounds of the component just before a maximize call.
     * @param f the component about to be resized
     * @param r the normal bounds to be saved away
     */
    protected void setPreviousBounds(JInternalFrame f, Rectangle r)     {
        f.setNormalBounds(r);
    }

    /**
     * Gets the normal bounds of the component prior to the component
     * being maximized.
     * @param f the <code>JInternalFrame</code> of interest
     * @return the normal bounds of the component
     */
    protected Rectangle getPreviousBounds(JInternalFrame f)     {
        return f.getNormalBounds();
    }

    /**
     * Sets that the component has been iconized and the bounds of the
     * <code>desktopIcon</code> are valid.
     *
     * @param f     the {@code JInternalFrame} of interest
     * @param value a {@code Boolean} signifying if component has been iconized
     */
    protected void setWasIcon(JInternalFrame f, Boolean value)  {
        if (value != null) {
            f.putClientProperty(HAS_BEEN_ICONIFIED_PROPERTY, value);
        }
    }

    /**
     * Returns <code>true</code> if the component has been iconized
     * and the bounds of the <code>desktopIcon</code> are valid,
     * otherwise returns <code>false</code>.
     *
     * @param f the <code>JInternalFrame</code> of interest
     * @return <code>true</code> if the component has been iconized;
     *    otherwise returns <code>false</code>
     */
    protected boolean wasIcon(JInternalFrame f) {
        return (f.getClientProperty(HAS_BEEN_ICONIFIED_PROPERTY) == Boolean.TRUE);
    }


    JDesktopPane getDesktopPane( JComponent frame ) {
        JDesktopPane pane = null;
        Component c = frame.getParent();

        // Find the JDesktopPane
        while ( pane == null ) {
            if ( c instanceof JDesktopPane ) {
                pane = (JDesktopPane)c;
            }
            else if ( c == null ) {
                break;
            }
            else {
                c = c.getParent();
            }
        }

        return pane;
    }


  // =========== stuff for faster frame dragging ===================

   private void dragFrameFaster(JComponent f, int newX, int newY) {

      Rectangle previousBounds = new Rectangle(currentBounds.x,
                                               currentBounds.y,
                                               currentBounds.width,
                                               currentBounds.height);

   // move the frame
      currentBounds.x = newX;
      currentBounds.y = newY;

      if (didDrag) {
          // Only initiate cleanup if we have actually done a drag.
          emergencyCleanup(f);
      }
      else {
          didDrag = true;
          // We reset the danger field as until now we haven't actually
          // moved the internal frame so we don't need to initiate repaint.
          ((JInternalFrame)f).danger = false;
      }

      boolean floaterCollision = isFloaterCollision(previousBounds, currentBounds);

      JComponent parent = (JComponent)f.getParent();
      Rectangle visBounds = previousBounds.intersection(desktopBounds);

      RepaintManager currentManager = RepaintManager.currentManager(f);

      currentManager.beginPaint();
      try {
          if(!floaterCollision) {
              currentManager.copyArea(parent, desktopGraphics, visBounds.x,
                                      visBounds.y,
                                      visBounds.width,
                                      visBounds.height,
                                      newX - previousBounds.x,
                                      newY - previousBounds.y,
                                      true);
          }

          f.setBounds(currentBounds);

          if (!floaterCollision) {
              Rectangle r = currentBounds;
              currentManager.notifyRepaintPerformed(parent, r.x, r.y, r.width, r.height);
          }

          if(floaterCollision) {
              // since we couldn't blit we just redraw as fast as possible
              // the isDragging mucking is to avoid activating emergency
              // cleanup
              ((JInternalFrame)f).isDragging = false;
              parent.paintImmediately(currentBounds);
              ((JInternalFrame)f).isDragging = true;
          }

          // fake out the repaint manager.  We'll take care of everything

          currentManager.markCompletelyClean(parent);
          currentManager.markCompletelyClean(f);

          // compute the minimal newly exposed area
          // if the rects intersect then we use computeDifference.  Otherwise
          // we'll repaint the entire previous bounds
          Rectangle[] dirtyRects = null;
          if ( previousBounds.intersects(currentBounds) ) {
              dirtyRects = SwingUtilities.computeDifference(previousBounds,
                                                            currentBounds);
          } else {
              dirtyRects = new Rectangle[1];
              dirtyRects[0] = previousBounds;
          };

          // Fix the damage
          for (int i = 0; i < dirtyRects.length; i++) {
              parent.paintImmediately(dirtyRects[i]);
              Rectangle r = dirtyRects[i];
              currentManager.notifyRepaintPerformed(parent, r.x, r.y, r.width, r.height);
          }

          // new areas of blit were exposed
          if ( !(visBounds.equals(previousBounds)) ) {
              dirtyRects = SwingUtilities.computeDifference(previousBounds,
                                                            desktopBounds);
              for (int i = 0; i < dirtyRects.length; i++) {
                  dirtyRects[i].x += newX - previousBounds.x;
                  dirtyRects[i].y += newY - previousBounds.y;
                  ((JInternalFrame)f).isDragging = false;
                  parent.paintImmediately(dirtyRects[i]);
                  ((JInternalFrame)f).isDragging = true;
                  Rectangle r = dirtyRects[i];
                  currentManager.notifyRepaintPerformed(parent, r.x, r.y, r.width, r.height);
              }

          }
      } finally {
          currentManager.endPaint();
      }

      // update window if it's non-opaque
      Window topLevel = SwingUtilities.getWindowAncestor(f);
      Toolkit tk = Toolkit.getDefaultToolkit();
      if (!topLevel.isOpaque() &&
          (tk instanceof SunToolkit) &&
          ((SunToolkit)tk).needUpdateWindow())
      {
          AWTAccessor.getWindowAccessor().updateWindow(topLevel);
      }
   }

   private boolean isFloaterCollision(Rectangle moveFrom, Rectangle moveTo) {
      if (floatingItems.length == 0) {
        // System.out.println("no floaters");
         return false;
      }

      for (int i = 0; i < floatingItems.length; i++) {
         boolean intersectsFrom = moveFrom.intersects(floatingItems[i]);
         if (intersectsFrom) {
            return true;
         }
         boolean intersectsTo = moveTo.intersects(floatingItems[i]);
         if (intersectsTo) {
            return true;
         }
      }

      return false;
   }

   private Rectangle[] findFloatingItems(JComponent f) {
      Container desktop = f.getParent();
      Component[] children = desktop.getComponents();
      int i = 0;
      for (i = 0; i < children.length; i++) {
         if (children[i] == f) {
            break;
         }
      }
      // System.out.println(i);
      Rectangle[] floaters = new Rectangle[i];
      for (i = 0; i < floaters.length; i++) {
         floaters[i] = children[i].getBounds();
      }

      return floaters;
   }

   /**
     * This method is here to clean up problems associated
     * with a race condition which can occur when the full contents
     * of a copyArea's source argument is not available onscreen.
     * This uses brute force to clean up in case of possible damage
     */
   private void emergencyCleanup(final JComponent f) {

        if ( ((JInternalFrame)f).danger ) {

           SwingUtilities.invokeLater( new Runnable(){
                                       public void run(){

                                       ((JInternalFrame)f).isDragging = false;
                                       f.paintImmediately(0,0,
                                                          f.getWidth(),
                                                          f.getHeight());

                                        //finalFrame.repaint();
                                        ((JInternalFrame)f).isDragging = true;
                                        // System.out.println("repair complete");
                                       }});

             ((JInternalFrame)f).danger = false;
        }

   }


}
