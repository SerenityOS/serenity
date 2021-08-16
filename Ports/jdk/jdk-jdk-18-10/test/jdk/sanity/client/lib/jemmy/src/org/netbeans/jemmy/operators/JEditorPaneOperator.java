/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Container;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 * Class provides basic functions to operate with JEditorPane (selection,
 * typing, deleting)
 *
 * <BR><BR>Timeouts used: <BR>
 * JTextComponentOperator.PushKeyTimeout - time between key pressing and
 * releasing during text typing <BR>
 * JTextComponentOperator.BetweenKeysTimeout - time to sleep between two chars
 * typing <BR>
 * JTextComponentOperator.ChangeCaretPositionTimeout - maximum time to change
 * caret position <BR>
 * JTextComponentOperator.TypeTextTimeout - maximum time to type text <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitFocusTimeout - time to wait component focus <BR>
 * JScrollBarOperator.OneScrollClickTimeout - time for one scroll click <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class JEditorPaneOperator extends JTextComponentOperator {

    /**
     * Identifier for a "content type" property.
     *
     * @see #getDump
     */
    public static final String CONTENT_TYPE_DPROP = "Content type";

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JEditorPaneOperator(JEditorPane b) {
        super(b);
    }

    /**
     * Constructs a JEditorPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JEditorPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JEditorPane) cont.
                waitSubComponent(new JEditorPaneFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JEditorPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JEditorPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JEditorPaneOperator(ContainerOperator<?> cont, String text, int index) {
        this((JEditorPane) waitComponent(cont,
                new JEditorPaneFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        cont.getComparator())),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JEditorPaneOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public JEditorPaneOperator(ContainerOperator<?> cont, int index) {
        this((JEditorPane) waitComponent(cont,
                new JEditorPaneFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @throws TimeoutExpiredException
     */
    public JEditorPaneOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JEditorPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JEditorPane instance or null if component was not found.
     */
    public static JEditorPane findJEditorPane(Container cont, ComponentChooser chooser, int index) {
        return (JEditorPane) findJTextComponent(cont, new JEditorPaneFinder(chooser), index);
    }

    /**
     * Searches JEditorPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JEditorPane instance or null if component was not found.
     */
    public static JEditorPane findJEditorPane(Container cont, ComponentChooser chooser) {
        return findJEditorPane(cont, chooser, 0);
    }

    /**
     * Searches JEditorPane by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JEditorPane instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JEditorPane findJEditorPane(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findJEditorPane(cont,
                new JEditorPaneFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Searches JEditorPane by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JEditorPane instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JEditorPane findJEditorPane(Container cont, String text, boolean ce, boolean ccs) {
        return findJEditorPane(cont, text, ce, ccs, 0);
    }

    /**
     * Waits JEditorPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JEditorPane instance.
     * @throws TimeoutExpiredException
     */
    public static JEditorPane waitJEditorPane(Container cont, ComponentChooser chooser, int index) {
        return (JEditorPane) waitJTextComponent(cont, new JEditorPaneFinder(chooser), index);
    }

    /**
     * Waits JEditorPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JEditorPane instance.
     * @throws TimeoutExpiredException
     */
    public static JEditorPane waitJEditorPane(Container cont, ComponentChooser chooser) {
        return waitJEditorPane(cont, chooser, 0);
    }

    /**
     * Waits JEditorPane by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JEditorPane instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JEditorPane waitJEditorPane(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitJEditorPane(cont,
                new JEditorPaneFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Waits JEditorPane by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JEditorPane instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JEditorPane waitJEditorPane(Container cont, String text, boolean ce, boolean ccs) {
        return waitJEditorPane(cont, text, ce, ccs, 0);
    }

    /**
     * Notifies whether "PageUp" and "PageDown" should be used to change caret
     * position. If can be useful if text takes some pages.
     *
     * @param yesOrNo whether to use "PageUp" and "PageDown"
     * @deprecated vlue set by this method is not used anymore: all navigating
     * is performed by TextDriver.
     */
    @Deprecated
    public void usePageNavigationKeys(boolean yesOrNo) {
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(CONTENT_TYPE_DPROP, ((JEditorPane) getSource()).getContentType());
        return result;
    }

    /**
     * Clicks on a named reference location
     *
     * @param reference the named location to click
     */
    public void clickOnReference(String reference) {
        int expectedCaretPos = getCaretPositionOfReference(reference);
        Rectangle viewBounds = modelToView(expectedCaretPos);
        Point expectedCaretPosLoc = new Point(viewBounds.x, viewBounds.y);
        //TODO Extend DefaultVisualizer to show a portion of component and use
        // that in here
        JScrollPane scroll = (JScrollPane) getContainer(
                new JScrollPaneOperator.JScrollPaneFinder(
                        ComponentSearcher.getTrueChooser("JScrollPane")));
        if (scroll != null) {
            JScrollPaneOperator scroller = new JScrollPaneOperator(scroll);
            scroller.copyEnvironment(this);
            scroller.setVisualizer(new EmptyVisualizer());
            scroller.scrollToComponentRectangle(getSource(),
                    (int) viewBounds.getX(), (int) viewBounds.getY(),
                    (int) viewBounds.getWidth(), (int) viewBounds.getHeight());
            setCaretPosition(expectedCaretPos);
        } else if (getVisibleRect().contains(expectedCaretPosLoc)) {
            scrollToReference(reference);
        } else {
            throw new IllegalComponentStateException("Component doesn't "
                    + "contain JScrollPane and Reference is out of"
                    + " visible area");
        }

        waitStateOnQueue(comp -> expectedCaretPosLoc.equals(
                ((JEditorPane)comp).getCaret().getMagicCaretPosition()));
        waitCaretPosition(expectedCaretPos);
        clickMouse(viewBounds.x, viewBounds.y, 1);
    }

    /**
     * Gets the starting caret position of a named reference location
     *
     * @param reference the named location
     * @return starting caret position of the named reference location
     * @throws IllegalArgumentException if the named reference doesn't
     *  exist in the document
     */
    private int getCaretPositionOfReference(String reference)
            throws IllegalArgumentException {
        int pos = -1;
        Document doc = getDocument();
        if (doc instanceof HTMLDocument) {
            HTMLDocument.Iterator iter =
                    ((HTMLDocument) doc).getIterator(HTML.Tag.A);
            for (;iter.isValid();iter.next()) {
                String nameAttr = (String) iter.getAttributes().
                        getAttribute(HTML.Attribute.NAME);
                if (reference.equals(nameAttr)) {
                    pos = iter.getStartOffset();
                }
            }
        }
        if(pos ==-1) {
            throw new IllegalArgumentException("Reference " + reference +
                    " doesn't exist in the document " + doc + ".");
        }
        return pos;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JEditorPane.addHyperlinkListener(HyperlinkListener)}
     * through queue
     */
    public void addHyperlinkListener(final HyperlinkListener hyperlinkListener) {
        runMapping(new MapVoidAction("addHyperlinkListener") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).addHyperlinkListener(hyperlinkListener);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.fireHyperlinkUpdate(HyperlinkEvent)} through queue
     */
    public void fireHyperlinkUpdate(final HyperlinkEvent hyperlinkEvent) {
        runMapping(new MapVoidAction("fireHyperlinkUpdate") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).fireHyperlinkUpdate(hyperlinkEvent);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.getContentType()} through queue
     */
    public String getContentType() {
        return (runMapping(new MapAction<String>("getContentType") {
            @Override
            public String map() {
                return ((JEditorPane) getSource()).getContentType();
            }
        }));
    }

    /**
     * Maps {@code JEditorPane.getEditorKit()} through queue
     */
    public EditorKit getEditorKit() {
        return (runMapping(new MapAction<EditorKit>("getEditorKit") {
            @Override
            public EditorKit map() {
                return ((JEditorPane) getSource()).getEditorKit();
            }
        }));
    }

    /**
     * Maps {@code JEditorPane.getEditorKitForContentType(String)} through queue
     */
    public EditorKit getEditorKitForContentType(final String string) {
        return (runMapping(new MapAction<EditorKit>("getEditorKitForContentType") {
            @Override
            public EditorKit map() {
                return ((JEditorPane) getSource()).getEditorKitForContentType(string);
            }
        }));
    }

    /**
     * Maps {@code JEditorPane.getPage()} through queue
     */
    public URL getPage() {
        return (runMapping(new MapAction<URL>("getPage") {
            @Override
            public URL map() {
                return ((JEditorPane) getSource()).getPage();
            }
        }));
    }

    /**
     * Maps {@code JEditorPane.read(InputStream, Object)} through queue
     */
    public void read(final InputStream inputStream, final Object object) {
        runMapping(new MapVoidAction("read") {
            @Override
            public void map() throws IOException {
                ((JEditorPane) getSource()).read(inputStream, object);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.removeHyperlinkListener(HyperlinkListener)}
     * through queue
     */
    public void removeHyperlinkListener(final HyperlinkListener hyperlinkListener) {
        runMapping(new MapVoidAction("removeHyperlinkListener") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).removeHyperlinkListener(hyperlinkListener);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.setContentType(String)} through queue
     */
    public void setContentType(final String string) {
        runMapping(new MapVoidAction("setContentType") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).setContentType(string);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.setEditorKit(EditorKit)} through queue
     */
    public void setEditorKit(final EditorKit editorKit) {
        runMapping(new MapVoidAction("setEditorKit") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).setEditorKit(editorKit);
            }
        });
    }

    /**
     * Maps
     * {@code JEditorPane.setEditorKitForContentType(String, EditorKit)}
     * through queue
     */
    public void setEditorKitForContentType(final String string, final EditorKit editorKit) {
        runMapping(new MapVoidAction("setEditorKitForContentType") {
            @Override
            public void map() {
                ((JEditorPane) getSource()).setEditorKitForContentType(string, editorKit);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.setPage(String)} through queue
     */
    public void setPage(final String string) {
        runMapping(new MapVoidAction("setPage") {
            @Override
            public void map() throws IOException {
                ((JEditorPane) getSource()).setPage(string);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.setPage(URL)} through queue
     */
    public void setPage(final URL uRL) {
        runMapping(new MapVoidAction("setPage") {
            @Override
            public void map() throws IOException {
                ((JEditorPane) getSource()).setPage(uRL);
            }
        });
    }

    /**
     * Maps {@code JEditorPane.scrollToReference(String)} through queue
     */
    public void scrollToReference(String reference) {
        runMapping(new MapVoidAction("scrollToReference") {
            @Override
            public void map() throws IOException {
                ((JEditorPane) getSource()).scrollToReference(reference);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Checks component type.
     */
    public static class JEditorPaneFinder extends Finder {

        /**
         * Constructs JEditorPaneFinder.
         *
         * @param sf other searching criteria.
         */
        public JEditorPaneFinder(ComponentChooser sf) {
            super(JEditorPane.class, sf);
        }

        /**
         * Constructs JEditorPaneFinder.
         */
        public JEditorPaneFinder() {
            super(JEditorPane.class);
        }
    }
}
