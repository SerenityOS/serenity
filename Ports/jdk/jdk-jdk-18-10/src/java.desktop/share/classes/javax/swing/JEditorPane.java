/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleHyperlink;
import javax.accessibility.AccessibleHypertext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Caret;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.CompositeView;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.GlyphView;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.WrappedPlainView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import sun.reflect.misc.ReflectUtil;

/**
 * A text component to edit various kinds of content.
 * You can find how-to information and examples of using editor panes in
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/text.html">Using Text Components</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * <p>
 * This component uses implementations of the
 * <code>EditorKit</code> to accomplish its behavior. It effectively
 * morphs into the proper kind of text editor for the kind
 * of content it is given.  The content type that editor is bound
 * to at any given time is determined by the <code>EditorKit</code> currently
 * installed.  If the content is set to a new URL, its type is used
 * to determine the <code>EditorKit</code> that should be used to
 * load the content.
 * <p>
 * By default, the following types of content are known:
 * <dl>
 * <dt><b>text/plain</b>
 * <dd>Plain text, which is the default the type given isn't
 * recognized.  The kit used in this case is an extension of
 * <code>DefaultEditorKit</code> that produces a wrapped plain text view.
 * <dt><b>text/html</b>
 * <dd>HTML text.  The kit used in this case is the class
 * <code>javax.swing.text.html.HTMLEditorKit</code>
 * which provides HTML 3.2 support.
 * <dt><b>text/rtf</b>
 * <dd>RTF text.  The kit used in this case is the class
 * <code>javax.swing.text.rtf.RTFEditorKit</code>
 * which provides a limited support of the Rich Text Format.
 * </dl>
 * <p>
 * There are several ways to load content into this component.
 * <ol>
 * <li>
 * The {@link #setText setText} method can be used to initialize
 * the component from a string.  In this case the current
 * <code>EditorKit</code> will be used, and the content type will be
 * expected to be of this type.
 * <li>
 * The {@link #read read} method can be used to initialize the
 * component from a <code>Reader</code>.  Note that if the content type is HTML,
 * relative references (e.g. for things like images) can't be resolved
 * unless the &lt;base&gt; tag is used or the <em>Base</em> property
 * on <code>HTMLDocument</code> is set.
 * In this case the current <code>EditorKit</code> will be used,
 * and the content type will be expected to be of this type.
 * <li>
 * The {@link #setPage setPage} method can be used to initialize
 * the component from a URL.  In this case, the content type will be
 * determined from the URL, and the registered <code>EditorKit</code>
 * for that content type will be set.
 * </ol>
 * <p>
 * Some kinds of content may provide hyperlink support by generating
 * hyperlink events.  The HTML <code>EditorKit</code> will generate
 * hyperlink events if the <code>JEditorPane</code> is <em>not editable</em>
 * (<code>JEditorPane.setEditable(false);</code> has been called).
 * If HTML frames are embedded in the document, the typical response would be
 * to change a portion of the current document.  The following code
 * fragment is a possible hyperlink listener implementation, that treats
 * HTML frame events specially, and simply displays any other activated
 * hyperlinks.
 * <pre>

&nbsp;    class Hyperactive implements HyperlinkListener {
&nbsp;
&nbsp;        public void hyperlinkUpdate(HyperlinkEvent e) {
&nbsp;            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
&nbsp;                JEditorPane pane = (JEditorPane) e.getSource();
&nbsp;                if (e instanceof HTMLFrameHyperlinkEvent) {
&nbsp;                    HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
&nbsp;                    HTMLDocument doc = (HTMLDocument)pane.getDocument();
&nbsp;                    doc.processHTMLFrameHyperlinkEvent(evt);
&nbsp;                } else {
&nbsp;                    try {
&nbsp;                        pane.setPage(e.getURL());
&nbsp;                    } catch (Throwable t) {
&nbsp;                        t.printStackTrace();
&nbsp;                    }
&nbsp;                }
&nbsp;            }
&nbsp;        }
&nbsp;    }

 * </pre>
 * <p>
 * For information on customizing how <b>text/html</b> is rendered please see
 * {@link #W3C_LENGTH_UNITS} and {@link #HONOR_DISPLAY_PROPERTIES}
 * <p>
 * Culturally dependent information in some documents is handled through
 * a mechanism called character encoding.  Character encoding is an
 * unambiguous mapping of the members of a character set (letters, ideographs,
 * digits, symbols, or control functions) to specific numeric code values. It
 * represents the way the file is stored. Example character encodings are
 * ISO-8859-1, ISO-8859-5, Shift-jis, Euc-jp, and UTF-8. When the file is
 * passed to an user agent (<code>JEditorPane</code>) it is converted to
 * the document character set (ISO-10646 aka Unicode).
 * <p>
 * There are multiple ways to get a character set mapping to happen
 * with <code>JEditorPane</code>.
 * <ol>
 * <li>
 * One way is to specify the character set as a parameter of the MIME
 * type.  This will be established by a call to the
 * {@link #setContentType setContentType} method.  If the content
 * is loaded by the {@link #setPage setPage} method the content
 * type will have been set according to the specification of the URL.
 * It the file is loaded directly, the content type would be expected to
 * have been set prior to loading.
 * <li>
 * Another way the character set can be specified is in the document itself.
 * This requires reading the document prior to determining the character set
 * that is desired.  To handle this, it is expected that the
 * <code>EditorKit</code>.read operation throw a
 * <code>ChangedCharSetException</code> which will
 * be caught.  The read is then restarted with a new Reader that uses
 * the character set specified in the <code>ChangedCharSetException</code>
 * (which is an <code>IOException</code>).
 * </ol>
 *
 * <dl>
 * <dt><b>Newlines</b>
 * <dd>
 * For a discussion on how newlines are handled, see
 * <a href="text/DefaultEditorKit.html">DefaultEditorKit</a>.
 * </dl>
 *
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author  Timothy Prinzing
 * @since 1.2
 */
@JavaBean(defaultProperty = "UIClassID", description = "A text component to edit various types of content.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JEditorPane extends JTextComponent {

    /**
     * Creates a new <code>JEditorPane</code>.
     * The document model is set to <code>null</code>.
     */
    public JEditorPane() {
        super();
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
                public Component getComponentAfter(Container focusCycleRoot,
                                                   Component aComponent) {
                    if (focusCycleRoot != JEditorPane.this ||
                        (!isEditable() && getComponentCount() > 0)) {
                        return super.getComponentAfter(focusCycleRoot,
                                                       aComponent);
                    } else {
                        Container rootAncestor = getFocusCycleRootAncestor();
                        return (rootAncestor != null)
                            ? rootAncestor.getFocusTraversalPolicy().
                                  getComponentAfter(rootAncestor,
                                                    JEditorPane.this)
                            : null;
                    }
                }
                public Component getComponentBefore(Container focusCycleRoot,
                                                    Component aComponent) {
                    if (focusCycleRoot != JEditorPane.this ||
                        (!isEditable() && getComponentCount() > 0)) {
                        return super.getComponentBefore(focusCycleRoot,
                                                        aComponent);
                    } else {
                        Container rootAncestor = getFocusCycleRootAncestor();
                        return (rootAncestor != null)
                            ? rootAncestor.getFocusTraversalPolicy().
                                  getComponentBefore(rootAncestor,
                                                     JEditorPane.this)
                            : null;
                    }
                }
                public Component getDefaultComponent(Container focusCycleRoot)
                {
                    return (focusCycleRoot != JEditorPane.this ||
                            (!isEditable() && getComponentCount() > 0))
                        ? super.getDefaultComponent(focusCycleRoot)
                        : null;
                }
                protected boolean accept(Component aComponent) {
                    return (aComponent != JEditorPane.this)
                        ? super.accept(aComponent)
                        : false;
                }
            });
        LookAndFeel.installProperty(this,
                                    "focusTraversalKeysForward",
                                    JComponent.
                                    getManagingFocusForwardTraversalKeys());
        LookAndFeel.installProperty(this,
                                    "focusTraversalKeysBackward",
                                    JComponent.
                                    getManagingFocusBackwardTraversalKeys());
    }

    /**
     * Creates a <code>JEditorPane</code> based on a specified URL for input.
     *
     * @param initialPage the URL
     * @exception IOException if the URL is <code>null</code>
     *          or cannot be accessed
     */
    public JEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    /**
     * Creates a <code>JEditorPane</code> based on a string containing
     * a URL specification.
     *
     * @param url the URL
     * @exception IOException if the URL is <code>null</code> or
     *          cannot be accessed
     */
    public JEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    /**
     * Creates a <code>JEditorPane</code> that has been initialized
     * to the given text.  This is a convenience constructor that calls the
     * <code>setContentType</code> and <code>setText</code> methods.
     *
     * @param type mime type of the given text
     * @param text the text to initialize with; may be <code>null</code>
     * @exception NullPointerException if the <code>type</code> parameter
     *          is <code>null</code>
     */
    public JEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }

    /**
     * Adds a hyperlink listener for notification of any changes, for example
     * when a link is selected and entered.
     *
     * @param listener the listener
     */
    public synchronized void addHyperlinkListener(HyperlinkListener listener) {
        listenerList.add(HyperlinkListener.class, listener);
    }

    /**
     * Removes a hyperlink listener.
     *
     * @param listener the listener
     */
    public synchronized void removeHyperlinkListener(HyperlinkListener listener) {
        listenerList.remove(HyperlinkListener.class, listener);
    }

    /**
     * Returns an array of all the <code>HyperLinkListener</code>s added
     * to this JEditorPane with addHyperlinkListener().
     *
     * @return all of the <code>HyperLinkListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public synchronized HyperlinkListener[] getHyperlinkListeners() {
        return listenerList.getListeners(javax.swing.event.HyperlinkListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  This is normally called
     * by the currently installed <code>EditorKit</code> if a content type
     * that supports hyperlinks is currently active and there
     * was activity with a link.  The listener list is processed
     * last to first.
     *
     * @param e the event
     * @see EventListenerList
     */
    public void fireHyperlinkUpdate(HyperlinkEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==HyperlinkListener.class) {
                ((HyperlinkListener)listeners[i+1]).hyperlinkUpdate(e);
            }
        }
    }


    /**
     * Sets the current URL being displayed.  The content type of the
     * pane is set, and if the editor kit for the pane is
     * non-<code>null</code>, then
     * a new default document is created and the URL is read into it.
     * If the URL contains and reference location, the location will
     * be scrolled to by calling the <code>scrollToReference</code>
     * method. If the desired URL is the one currently being displayed,
     * the document will not be reloaded. To force a document
     * reload it is necessary to clear the stream description property
     * of the document. The following code shows how this can be done:
     *
     * <pre>
     *   Document doc = jEditorPane.getDocument();
     *   doc.putProperty(Document.StreamDescriptionProperty, null);
     * </pre>
     *
     * If the desired URL is not the one currently being
     * displayed, the <code>getStream</code> method is called to
     * give subclasses control over the stream provided.
     * <p>
     * This may load either synchronously or asynchronously
     * depending upon the document returned by the <code>EditorKit</code>.
     * If the <code>Document</code> is of type
     * <code>AbstractDocument</code> and has a value returned by
     * <code>AbstractDocument.getAsynchronousLoadPriority</code>
     * that is greater than or equal to zero, the page will be
     * loaded on a separate thread using that priority.
     * <p>
     * If the document is loaded synchronously, it will be
     * filled in with the stream prior to being installed into
     * the editor with a call to <code>setDocument</code>, which
     * is bound and will fire a property change event.  If an
     * <code>IOException</code> is thrown the partially loaded
     * document will
     * be discarded and neither the document or page property
     * change events will be fired.  If the document is
     * successfully loaded and installed, a view will be
     * built for it by the UI which will then be scrolled if
     * necessary, and then the page property change event
     * will be fired.
     * <p>
     * If the document is loaded asynchronously, the document
     * will be installed into the editor immediately using a
     * call to <code>setDocument</code> which will fire a
     * document property change event, then a thread will be
     * created which will begin doing the actual loading.
     * In this case, the page property change event will not be
     * fired by the call to this method directly, but rather will be
     * fired when the thread doing the loading has finished.
     * It will also be fired on the event-dispatch thread.
     * Since the calling thread can not throw an <code>IOException</code>
     * in the event of failure on the other thread, the page
     * property change event will be fired when the other
     * thread is done whether the load was successful or not.
     *
     * @param page the URL of the page
     * @exception IOException for a <code>null</code> or invalid
     *          page specification, or exception from the stream being read
     * @see #getPage
     */
    @BeanProperty(expert = true, description
            = "the URL used to set content")
    public void setPage(URL page) throws IOException {
        if (page == null) {
            throw new IOException("invalid url");
        }
        URL loaded = getPage();


        // reset scrollbar
        if (!page.equals(loaded) && page.getRef() == null) {
            scrollRectToVisible(new Rectangle(0,0,1,1));
        }
        boolean reloaded = false;
        Object postData = getPostData();
        if ((loaded == null) || !loaded.sameFile(page) || (postData != null)) {
            // different url or POST method, load the new content

            int p = getAsynchronousLoadPriority(getDocument());
            if (p < 0) {
                // open stream synchronously
                InputStream in = getStream(page);
                if (kit != null) {
                    Document doc = initializeModel(kit, page);

                    // At this point, one could either load up the model with no
                    // view notifications slowing it down (i.e. best synchronous
                    // behavior) or set the model and start to feed it on a separate
                    // thread (best asynchronous behavior).
                    p = getAsynchronousLoadPriority(doc);
                    if (p >= 0) {
                        // load asynchronously
                        setDocument(doc);
                        synchronized(this) {
                            pageLoader = new PageLoader(doc, in, loaded, page);
                            pageLoader.execute();
                        }
                        return;
                    }
                    read(in, doc);
                    setDocument(doc);
                    reloaded = true;
                }
            } else {
                // we may need to cancel background loading
                if (pageLoader != null) {
                    pageLoader.cancel(true);
                }

                // Do everything in a background thread.
                // Model initialization is deferred to that thread, too.
                pageLoader = new PageLoader(null, null, loaded, page);
                pageLoader.execute();
                return;
            }
        }
        final String reference = page.getRef();
        if (reference != null) {
            if (!reloaded) {
                scrollToReference(reference);
            }
            else {
                // Have to scroll after painted.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        scrollToReference(reference);
                    }
                });
            }
            getDocument().putProperty(Document.StreamDescriptionProperty, page);
        }
        firePropertyChange("page", loaded, page);
    }

    /**
     * Create model and initialize document properties from page properties.
     */
    private Document initializeModel(EditorKit kit, URL page) {
        Document doc = kit.createDefaultDocument();
        if (pageProperties != null) {
            // transfer properties discovered in stream to the
            // document property collection.
            for (Enumeration<String> e = pageProperties.keys(); e.hasMoreElements() ;) {
                String key = e.nextElement();
                doc.putProperty(key, pageProperties.get(key));
            }
            pageProperties.clear();
        }
        if (doc.getProperty(Document.StreamDescriptionProperty) == null) {
            doc.putProperty(Document.StreamDescriptionProperty, page);
        }
        return doc;
    }

    /**
     * Return load priority for the document or -1 if priority not supported.
     */
    private int getAsynchronousLoadPriority(Document doc) {
        return (doc instanceof AbstractDocument ?
            ((AbstractDocument) doc).getAsynchronousLoadPriority() : -1);
    }

    /**
     * This method initializes from a stream.  If the kit is
     * set to be of type <code>HTMLEditorKit</code>, and the
     * <code>desc</code> parameter is an <code>HTMLDocument</code>,
     * then it invokes the <code>HTMLEditorKit</code> to initiate
     * the read. Otherwise it calls the superclass
     * method which loads the model as plain text.
     *
     * @param in the stream from which to read
     * @param desc an object describing the stream
     * @exception IOException as thrown by the stream being
     *          used to initialize
     * @see JTextComponent#read
     * @see #setDocument
     */
    public void read(InputStream in, Object desc) throws IOException {

        if (desc instanceof HTMLDocument &&
            kit instanceof HTMLEditorKit) {
            HTMLDocument hdoc = (HTMLDocument) desc;
            setDocument(hdoc);
            read(in, hdoc);
        } else {
            String charset = (String) getClientProperty("charset");
            Reader r = (charset != null) ? new InputStreamReader(in, charset) :
                new InputStreamReader(in);
            super.read(r, desc);
        }
    }


    /**
     * This method invokes the <code>EditorKit</code> to initiate a
     * read.  In the case where a <code>ChangedCharSetException</code>
     * is thrown this exception will contain the new CharSet.
     * Therefore the <code>read</code> operation
     * is then restarted after building a new Reader with the new charset.
     *
     * @param in the inputstream to use
     * @param doc the document to load
     *
     */
    void read(InputStream in, Document doc) throws IOException {
        if (! Boolean.TRUE.equals(doc.getProperty("IgnoreCharsetDirective"))) {
            final int READ_LIMIT = 1024 * 10;
            in = new BufferedInputStream(in, READ_LIMIT);
            in.mark(READ_LIMIT);
        }
        String charset = (String) getClientProperty("charset");
        try(Reader r = (charset != null) ? new InputStreamReader(in, charset) :
                new InputStreamReader(in)) {
            kit.read(r, doc, 0);
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        } catch (ChangedCharSetException changedCharSetException) {
            String charSetSpec = changedCharSetException.getCharSetSpec();
            if (changedCharSetException.keyEqualsCharSet()) {
                putClientProperty("charset", charSetSpec);
            } else {
                setCharsetFromContentTypeParameters(charSetSpec);
            }
            try {
                in.reset();
            } catch (IOException exception) {
                //mark was invalidated
                in.close();
                URL url = (URL)doc.getProperty(Document.StreamDescriptionProperty);
                if (url != null) {
                    URLConnection conn = url.openConnection();
                    in = conn.getInputStream();
                } else {
                    //there is nothing we can do to recover stream
                    throw changedCharSetException;
                }
            }
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException e) {}
            doc.putProperty("IgnoreCharsetDirective", Boolean.valueOf(true));
            read(in, doc);
        }
    }


    /**
     * Loads a stream into the text document model.
     */
    class PageLoader extends SwingWorker<URL, Object> {

        /**
         * Construct an asynchronous page loader.
         */
        PageLoader(Document doc, InputStream in, URL old, URL page) {
            this.in = in;
            this.old = old;
            this.page = page;
            this.doc = doc;
        }

        /**
         * Try to load the document, then scroll the view
         * to the reference (if specified).  When done, fire
         * a page property change event.
         */
        protected URL doInBackground() {
            boolean pageLoaded = false;
            try {
                if (in == null) {
                    in = getStream(page);
                    if (kit == null) {
                        // We received document of unknown content type.
                        UIManager.getLookAndFeel().
                                provideErrorFeedback(JEditorPane.this);
                        return old;
                    }
                }

                if (doc == null) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                doc = initializeModel(kit, page);
                                setDocument(doc);
                            }
                        });
                    } catch (InvocationTargetException ex) {
                        UIManager.getLookAndFeel().provideErrorFeedback(
                                                            JEditorPane.this);
                        return old;
                    } catch (InterruptedException ex) {
                        UIManager.getLookAndFeel().provideErrorFeedback(
                                                            JEditorPane.this);
                        return old;
                    }
                }

                read(in, doc);
                URL page = (URL) doc.getProperty(Document.StreamDescriptionProperty);
                String reference = page.getRef();
                if (reference != null) {
                    // scroll the page if necessary, but do it on the
                    // event thread... that is the only guarantee that
                    // modelToView can be safely called.
                    Runnable callScrollToReference = new Runnable() {
                        public void run() {
                            URL u = (URL) getDocument().getProperty
                                (Document.StreamDescriptionProperty);
                            String ref = u.getRef();
                            scrollToReference(ref);
                        }
                    };
                    SwingUtilities.invokeLater(callScrollToReference);
                }
                pageLoaded = true;
            } catch (IOException ioe) {
                UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
            } finally {
                if (pageLoaded) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JEditorPane.this.firePropertyChange("page", old, page);
                        }
                    });
                }
            }
            return (pageLoaded ? page : old);
        }

        /**
         * The stream to load the document with
         */
        InputStream in;

        /**
         * URL of the old page that was replaced (for the property change event)
         */
        URL old;

        /**
         * URL of the page being loaded (for the property change event)
         */
        URL page;

        /**
         * The Document instance to load into. This is cached in case a
         * new Document is created between the time the thread this is created
         * and run.
         */
        Document doc;
    }

    /**
     * Fetches a stream for the given URL, which is about to
     * be loaded by the <code>setPage</code> method.  By
     * default, this simply opens the URL and returns the
     * stream.  This can be reimplemented to do useful things
     * like fetch the stream from a cache, monitor the progress
     * of the stream, etc.
     * <p>
     * This method is expected to have the side effect of
     * establishing the content type, and therefore setting the
     * appropriate <code>EditorKit</code> to use for loading the stream.
     * <p>
     * If this the stream was an http connection, redirects
     * will be followed and the resulting URL will be set as
     * the <code>Document.StreamDescriptionProperty</code> so that relative
     * URL's can be properly resolved.
     *
     * @param page  the URL of the page
     * @return a stream for the URL which is about to be loaded
     * @throws IOException if an I/O problem occurs
     */
    protected InputStream getStream(URL page) throws IOException {
        final URLConnection conn = page.openConnection();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setInstanceFollowRedirects(false);
            Object postData = getPostData();
            if (postData != null) {
                handlePostData(hconn, postData);
            }
            int response = hconn.getResponseCode();
            boolean redirect = (response >= 300 && response <= 399);

            /*
             * In the case of a redirect, we want to actually change the URL
             * that was input to the new, redirected URL
             */
            if (redirect) {
                String loc = conn.getHeaderField("Location");
                if (loc.startsWith("http", 0)) {
                    page = new URL(loc);
                } else {
                    page = new URL(page, loc);
                }
                return getStream(page);
            }
        }

        // Connection properties handler should be forced to run on EDT,
        // as it instantiates the EditorKit.
        if (SwingUtilities.isEventDispatchThread()) {
            handleConnectionProperties(conn);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        handleConnectionProperties(conn);
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return conn.getInputStream();
    }

    /**
     * Handle URL connection properties (most notably, content type).
     */
    private void handleConnectionProperties(URLConnection conn) {
        if (pageProperties == null) {
            pageProperties = new Hashtable<String, Object>();
        }
        String type = conn.getContentType();
        if (type != null) {
            setContentType(type);
            pageProperties.put("content-type", type);
        }
        pageProperties.put(Document.StreamDescriptionProperty, conn.getURL());
        String enc = conn.getContentEncoding();
        if (enc != null) {
            pageProperties.put("content-encoding", enc);
        }
    }

    private Object getPostData() {
        return getDocument().getProperty(PostDataProperty);
    }

    private void handlePostData(HttpURLConnection conn, Object postData)
                                                            throws IOException {
        conn.setDoOutput(true);
        DataOutputStream os = null;
        try {
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes((String) postData);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }


    /**
     * Scrolls the view to the given reference location
     * (that is, the value returned by the <code>URL.getRef</code>
     * method for the URL being displayed).  By default, this
     * method only knows how to locate a reference in an
     * HTMLDocument.  The implementation calls the
     * <code>scrollRectToVisible</code> method to
     * accomplish the actual scrolling.  If scrolling to a
     * reference location is needed for document types other
     * than HTML, this method should be reimplemented.
     * This method will have no effect if the component
     * is not visible.
     *
     * @param reference the named location to scroll to
     */
    @SuppressWarnings("deprecation")
    public void scrollToReference(String reference) {
        Document d = getDocument();
        if (d instanceof HTMLDocument) {
            HTMLDocument doc = (HTMLDocument) d;
            HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
            for (; iter.isValid(); iter.next()) {
                AttributeSet a = iter.getAttributes();
                String nm = (String) a.getAttribute(HTML.Attribute.NAME);
                if ((nm != null) && nm.equals(reference)) {
                    // found a matching reference in the document.
                    try {
                        int pos = iter.getStartOffset();
                        Rectangle r = modelToView(pos);
                        if (r != null) {
                            // the view is visible, scroll it to the
                            // center of the current visible area.
                            Rectangle vis = getVisibleRect();
                            //r.y -= (vis.height / 2);
                            r.height = vis.height;
                            scrollRectToVisible(r);
                            setCaretPosition(pos);
                        }
                    } catch (BadLocationException ble) {
                        UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
                    }
                }
            }
        }
    }

    /**
     * Gets the current URL being displayed.  If a URL was
     * not specified in the creation of the document, this
     * will return <code>null</code>, and relative URL's will not be
     * resolved.
     *
     * @return the URL, or <code>null</code> if none
     */
    public URL getPage() {
        return (URL) getDocument().getProperty(Document.StreamDescriptionProperty);
    }

    /**
     * Sets the current URL being displayed.
     *
     * @param url the URL for display
     * @exception IOException for a <code>null</code> or invalid URL
     *          specification
     */
    public void setPage(String url) throws IOException {
        if (url == null) {
            throw new IOException("invalid url");
        }
        URL page = new URL(url);
        setPage(page);
    }

    /**
     * Gets the class ID for the UI.
     *
     * @return the string "EditorPaneUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Creates the default editor kit (<code>PlainEditorKit</code>) for when
     * the component is first created.
     *
     * @return the editor kit
     */
    protected EditorKit createDefaultEditorKit() {
        return new PlainEditorKit();
    }

    /**
     * Fetches the currently installed kit for handling content.
     * <code>createDefaultEditorKit</code> is called to set up a default
     * if necessary.
     *
     * @return the editor kit
     */
    public EditorKit getEditorKit() {
        if (kit == null) {
            kit = createDefaultEditorKit();
            isUserSetEditorKit = false;
        }
        return kit;
    }

    /**
     * Gets the type of content that this editor
     * is currently set to deal with.  This is
     * defined to be the type associated with the
     * currently installed <code>EditorKit</code>.
     *
     * @return the content type, <code>null</code> if no editor kit set
     */
    public final String getContentType() {
        return (kit != null) ? kit.getContentType() : null;
    }

    /**
     * Sets the type of content that this editor
     * handles.  This calls <code>getEditorKitForContentType</code>,
     * and then <code>setEditorKit</code> if an editor kit can
     * be successfully located.  This is mostly convenience method
     * that can be used as an alternative to calling
     * <code>setEditorKit</code> directly.
     * <p>
     * If there is a charset definition specified as a parameter
     * of the content type specification, it will be used when
     * loading input streams using the associated <code>EditorKit</code>.
     * For example if the type is specified as
     * <code>text/html; charset=EUC-JP</code> the content
     * will be loaded using the <code>EditorKit</code> registered for
     * <code>text/html</code> and the Reader provided to
     * the <code>EditorKit</code> to load unicode into the document will
     * use the <code>EUC-JP</code> charset for translating
     * to unicode.  If the type is not recognized, the content
     * will be loaded using the <code>EditorKit</code> registered
     * for plain text, <code>text/plain</code>.
     *
     * @param type the non-<code>null</code> mime type for the content editing
     *   support
     * @see #getContentType
     * @throws NullPointerException if the <code>type</code> parameter
     *          is <code>null</code>
     */
    @BeanProperty(bound = false, description
            = "the type of content")
    public final void setContentType(String type) {
        // The type could have optional info is part of it,
        // for example some charset info.  We need to strip that
        // of and save it.
        int parm = type.indexOf(';');
        if (parm > -1) {
            // Save the paramList.
            String paramList = type.substring(parm);
            // update the content type string.
            type = type.substring(0, parm).trim();
            if (type.toLowerCase().startsWith("text/")) {
                setCharsetFromContentTypeParameters(paramList);
            }
        }
        if ((kit == null) || (! type.equals(kit.getContentType()))
                || !isUserSetEditorKit) {
            EditorKit k = getEditorKitForContentType(type);
            if (k != null && k != kit) {
                setEditorKit(k);
                isUserSetEditorKit = false;
            }
        }

    }

    /**
     * This method gets the charset information specified as part
     * of the content type in the http header information.
     */
    private void setCharsetFromContentTypeParameters(String paramlist) {
        String charset;
        try {
            // paramlist is handed to us with a leading ';', strip it.
            int semi = paramlist.indexOf(';');
            if (semi > -1 && semi < paramlist.length()-1) {
                paramlist = paramlist.substring(semi + 1);
            }

            if (paramlist.length() > 0) {
                // parse the paramlist into attr-value pairs & get the
                // charset pair's value
                HeaderParser hdrParser = new HeaderParser(paramlist);
                charset = hdrParser.findValue("charset");
                if (charset != null) {
                    putClientProperty("charset", charset);
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            // malformed parameter list, use charset we have
        }
        catch (NullPointerException e) {
            // malformed parameter list, use charset we have
        }
        catch (Exception e) {
            // malformed parameter list, use charset we have; but complain
            System.err.println("JEditorPane.getCharsetFromContentTypeParameters failed on: " + paramlist);
            e.printStackTrace();
        }
    }


    /**
     * Sets the currently installed kit for handling
     * content.  This is the bound property that
     * establishes the content type of the editor.
     * Any old kit is first deinstalled, then if kit is
     * non-<code>null</code>,
     * the new kit is installed, and a default document created for it.
     * A <code>PropertyChange</code> event ("editorKit") is always fired when
     * <code>setEditorKit</code> is called.
     * <p>
     * <em>NOTE: This has the side effect of changing the model,
     * because the <code>EditorKit</code> is the source of how a
     * particular type
     * of content is modeled.  This method will cause <code>setDocument</code>
     * to be called on behalf of the caller to ensure integrity
     * of the internal state.</em>
     *
     * @param kit the desired editor behavior
     * @see #getEditorKit
     */
    @BeanProperty(expert = true, description
            = "the currently installed kit for handling content")
    public void setEditorKit(EditorKit kit) {
        EditorKit old = this.kit;
        isUserSetEditorKit = true;
        if (old != null) {
            old.deinstall(this);
        }
        this.kit = kit;
        if (this.kit != null) {
            this.kit.install(this);
            setDocument(this.kit.createDefaultDocument());
        }
        firePropertyChange("editorKit", old, kit);
    }

    /**
     * Fetches the editor kit to use for the given type
     * of content.  This is called when a type is requested
     * that doesn't match the currently installed type.
     * If the component doesn't have an <code>EditorKit</code> registered
     * for the given type, it will try to create an
     * <code>EditorKit</code> from the default <code>EditorKit</code> registry.
     * If that fails, a <code>PlainEditorKit</code> is used on the
     * assumption that all text documents can be represented
     * as plain text.
     * <p>
     * This method can be reimplemented to use some
     * other kind of type registry.  This can
     * be reimplemented to use the Java Activation
     * Framework, for example.
     *
     * @param type the non-<code>null</code> content type
     * @return the editor kit
     */
    public EditorKit getEditorKitForContentType(String type) {
        if (typeHandlers == null) {
            typeHandlers = new Hashtable<String, EditorKit>(3);
        }
        EditorKit k = typeHandlers.get(type);
        if (k == null) {
            k = createEditorKitForContentType(type);
            if (k != null) {
                setEditorKitForContentType(type, k);
            }
        }
        if (k == null) {
            k = createDefaultEditorKit();
        }
        return k;
    }

    /**
     * Directly sets the editor kit to use for the given type.  A
     * look-and-feel implementation might use this in conjunction
     * with <code>createEditorKitForContentType</code> to install handlers for
     * content types with a look-and-feel bias.
     *
     * @param type the non-<code>null</code> content type
     * @param k the editor kit to be set
     */
    public void setEditorKitForContentType(String type, EditorKit k) {
        if (typeHandlers == null) {
            typeHandlers = new Hashtable<String, EditorKit>(3);
        }
        typeHandlers.put(type, k);
    }

    /**
     * Replaces the currently selected content with new content
     * represented by the given string.  If there is no selection
     * this amounts to an insert of the given text.  If there
     * is no replacement text (i.e. the content string is empty
     * or <code>null</code>) this amounts to a removal of the
     * current selection.  The replacement text will have the
     * attributes currently defined for input.  If the component is not
     * editable, beep and return.
     *
     * @param content  the content to replace the selection with.  This
     *   value can be <code>null</code>
     */
    @Override
    public void replaceSelection(String content) {
        if (! isEditable()) {
            UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
            return;
        }
        EditorKit kit = getEditorKit();
        if(kit instanceof StyledEditorKit) {
            try {
                Document doc = getDocument();
                Caret caret = getCaret();
                boolean composedTextSaved = saveComposedText(caret.getDot());
                int p0 = Math.min(caret.getDot(), caret.getMark());
                int p1 = Math.max(caret.getDot(), caret.getMark());
                if (doc instanceof AbstractDocument) {
                    ((AbstractDocument)doc).replace(p0, p1 - p0, content,
                              ((StyledEditorKit)kit).getInputAttributes());
                }
                else {
                    if (p0 != p1) {
                        doc.remove(p0, p1 - p0);
                    }
                    if (content != null && content.length() > 0) {
                        doc.insertString(p0, content, ((StyledEditorKit)kit).
                                         getInputAttributes());
                    }
                }
                if (composedTextSaved) {
                    restoreComposedText();
                }
            } catch (BadLocationException e) {
                UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
            }
        }
        else {
            super.replaceSelection(content);
        }
    }

    /**
     * Creates a handler for the given type from the default registry
     * of editor kits.  The registry is created if necessary.  If the
     * registered class has not yet been loaded, an attempt
     * is made to dynamically load the prototype of the kit for the
     * given type.  If the type was registered with a <code>ClassLoader</code>,
     * that <code>ClassLoader</code> will be used to load the prototype.
     * If there was no registered <code>ClassLoader</code>,
     * <code>Class.forName</code> will be used to load the prototype.
     * <p>
     * Once a prototype <code>EditorKit</code> instance is successfully
     * located, it is cloned and the clone is returned.
     *
     * @param type the content type
     * @return the editor kit, or <code>null</code> if there is nothing
     *   registered for the given type
     */
    @SuppressWarnings("deprecation")
    public static EditorKit createEditorKitForContentType(String type) {
        Hashtable<String, EditorKit> kitRegistry = getKitRegisty();
        EditorKit k = kitRegistry.get(type);
        if (k == null) {
            // try to dynamically load the support
            String classname = getKitTypeRegistry().get(type);
            ClassLoader loader = getKitLoaderRegistry().get(type);
            try {
                Class<?> c;
                if (loader != null) {
                    ReflectUtil.checkPackageAccess(classname);
                    c = loader.loadClass(classname);
                } else {
                    // Will only happen if developer has invoked
                    // registerEditorKitForContentType(type, class, null).
                    c = SwingUtilities.loadSystemClass(classname);
                }
                k = (EditorKit) c.newInstance();
                kitRegistry.put(type, k);
            } catch (Throwable e) {
                k = null;
            }
        }

        // create a copy of the prototype or null if there
        // is no prototype.
        if (k != null) {
            return (EditorKit) k.clone();
        }
        return null;
    }

    /**
     * Establishes the default bindings of <code>type</code> to
     * <code>classname</code>.
     * The class will be dynamically loaded later when actually
     * needed, and can be safely changed before attempted uses
     * to avoid loading unwanted classes.  The prototype
     * <code>EditorKit</code> will be loaded with <code>Class.forName</code>
     * when registered with this method.
     *
     * @param type the non-<code>null</code> content type
     * @param classname the class to load later
     */
    public static void registerEditorKitForContentType(String type, String classname) {
        registerEditorKitForContentType(type, classname,Thread.currentThread().
                                        getContextClassLoader());
    }

    /**
     * Establishes the default bindings of <code>type</code> to
     * <code>classname</code>.
     * The class will be dynamically loaded later when actually
     * needed using the given <code>ClassLoader</code>,
     * and can be safely changed
     * before attempted uses to avoid loading unwanted classes.
     *
     * @param type the non-<code>null</code> content type
     * @param classname the class to load later
     * @param loader the <code>ClassLoader</code> to use to load the name
     */
    public static void registerEditorKitForContentType(String type, String classname, ClassLoader loader) {
        getKitTypeRegistry().put(type, classname);
        if (loader != null) {
            getKitLoaderRegistry().put(type, loader);
        } else {
            getKitLoaderRegistry().remove(type);
        }
        getKitRegisty().remove(type);
    }

    /**
     * Returns the currently registered {@code EditorKit} class name for the
     * type {@code type}.
     *
     * @param type  the non-{@code null} content type
     * @return a {@code String} containing the {@code EditorKit} class name
     *         for {@code type}
     * @since 1.3
     */
    public static String getEditorKitClassNameForContentType(String type) {
        return getKitTypeRegistry().get(type);
    }

    private static Hashtable<String, String> getKitTypeRegistry() {
        loadDefaultKitsIfNecessary();
        @SuppressWarnings("unchecked")
        Hashtable<String, String> tmp =
            (Hashtable)SwingUtilities.appContextGet(kitTypeRegistryKey);
        return tmp;
    }

    private static Hashtable<String, ClassLoader> getKitLoaderRegistry() {
        loadDefaultKitsIfNecessary();
        @SuppressWarnings("unchecked")
        Hashtable<String,  ClassLoader> tmp =
            (Hashtable)SwingUtilities.appContextGet(kitLoaderRegistryKey);
        return tmp;
    }

    private static Hashtable<String, EditorKit> getKitRegisty() {
        @SuppressWarnings("unchecked")
        Hashtable<String, EditorKit> ht =
            (Hashtable)SwingUtilities.appContextGet(kitRegistryKey);
        if (ht == null) {
            ht = new Hashtable<>(3);
            SwingUtilities.appContextPut(kitRegistryKey, ht);
        }
        return ht;
    }

    /**
     * This is invoked every time the registries are accessed. Loading
     * is done this way instead of via a static as the static is only
     * called once when running in plugin resulting in the entries only
     * appearing in the first applet.
     */
    private static void loadDefaultKitsIfNecessary() {
        if (SwingUtilities.appContextGet(kitTypeRegistryKey) == null) {
            synchronized(defaultEditorKitMap) {
                if (defaultEditorKitMap.size() == 0) {
                    defaultEditorKitMap.put("text/plain",
                                            "javax.swing.JEditorPane$PlainEditorKit");
                    defaultEditorKitMap.put("text/html",
                                            "javax.swing.text.html.HTMLEditorKit");
                    defaultEditorKitMap.put("text/rtf",
                                            "javax.swing.text.rtf.RTFEditorKit");
                    defaultEditorKitMap.put("application/rtf",
                                            "javax.swing.text.rtf.RTFEditorKit");
                }
            }
            Hashtable<Object, Object> ht = new Hashtable<>();
            SwingUtilities.appContextPut(kitTypeRegistryKey, ht);
            ht = new Hashtable<>();
            SwingUtilities.appContextPut(kitLoaderRegistryKey, ht);
            for (String key : defaultEditorKitMap.keySet()) {
                registerEditorKitForContentType(key,defaultEditorKitMap.get(key));
            }

        }
    }

    // --- java.awt.Component methods --------------------------

    /**
     * Returns the preferred size for the <code>JEditorPane</code>.
     * The preferred size for <code>JEditorPane</code> is slightly altered
     * from the preferred size of the superclass.  If the size
     * of the viewport has become smaller than the minimum size
     * of the component, the scrollable definition for tracking
     * width or height will turn to false.  The default viewport
     * layout will give the preferred size, and that is not desired
     * in the case where the scrollable is tracking.  In that case
     * the <em>normal</em> preferred size is adjusted to the
     * minimum size.  This allows things like HTML tables to
     * shrink down to their minimum size and then be laid out at
     * their minimum size, refusing to shrink any further.
     *
     * @return a <code>Dimension</code> containing the preferred size
     */
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            TextUI ui = getUI();
            int prefWidth = d.width;
            int prefHeight = d.height;
            if (! getScrollableTracksViewportWidth()) {
                int w = port.getWidth();
                Dimension min = ui.getMinimumSize(this);
                if (w != 0 && w < min.width) {
                    // Only adjust to min if we have a valid size
                    prefWidth = min.width;
                }
            }
            if (! getScrollableTracksViewportHeight()) {
                int h = port.getHeight();
                Dimension min = ui.getMinimumSize(this);
                if (h != 0 && h < min.height) {
                    // Only adjust to min if we have a valid size
                    prefHeight = min.height;
                }
            }
            if (prefWidth != d.width || prefHeight != d.height) {
                d = new Dimension(prefWidth, prefHeight);
            }
        }
        return d;
    }

    // --- JTextComponent methods -----------------------------

    /**
     * Sets the text of this <code>TextComponent</code> to the specified
     * content,
     * which is expected to be in the format of the content type of
     * this editor.  For example, if the type is set to <code>text/html</code>
     * the string should be specified in terms of HTML.
     * <p>
     * This is implemented to remove the contents of the current document,
     * and replace them by parsing the given string using the current
     * <code>EditorKit</code>.  This gives the semantics of the
     * superclass by not changing
     * out the model, while supporting the content type currently set on
     * this component.  The assumption is that the previous content is
     * relatively
     * small, and that the previous content doesn't have side effects.
     * Both of those assumptions can be violated and cause undesirable results.
     * To avoid this, create a new document,
     * <code>getEditorKit().createDefaultDocument()</code>, and replace the
     * existing <code>Document</code> with the new one. You are then assured the
     * previous <code>Document</code> won't have any lingering state.
     * <ol>
     * <li>
     * Leaving the existing model in place means that the old view will be
     * torn down, and a new view created, where replacing the document would
     * avoid the tear down of the old view.
     * <li>
     * Some formats (such as HTML) can install things into the document that
     * can influence future contents.  HTML can have style information embedded
     * that would influence the next content installed unexpectedly.
     * </ol>
     * <p>
     * An alternative way to load this component with a string would be to
     * create a StringReader and call the read method.  In this case the model
     * would be replaced after it was initialized with the contents of the
     * string.
     *
     * @param t the new text to be set; if <code>null</code> the old
     *    text will be deleted
     * @see #getText
     */
    @BeanProperty(bound = false, description
            = "the text of this component")
    public void setText(String t) {
        try {
            Document doc = getDocument();
            doc.remove(0, doc.getLength());
            if (t == null || t.isEmpty()) {
                return;
            }
            Reader r = new StringReader(t);
            EditorKit kit = getEditorKit();
            kit.read(r, doc, 0);
        } catch (IOException ioe) {
            UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
        } catch (BadLocationException ble) {
            UIManager.getLookAndFeel().provideErrorFeedback(JEditorPane.this);
        }
    }

    /**
     * Returns the text contained in this <code>TextComponent</code>
     * in terms of the
     * content type of this editor.  If an exception is thrown while
     * attempting to retrieve the text, <code>null</code> will be returned.
     * This is implemented to call <code>JTextComponent.write</code> with
     * a <code>StringWriter</code>.
     *
     * @return the text
     * @see #setText
     */
    public String getText() {
        String txt;
        try {
            StringWriter buf = new StringWriter();
            write(buf);
            txt = buf.toString();
        } catch (IOException ioe) {
            txt = null;
        }
        return txt;
    }

    // --- Scrollable  ----------------------------------------

    /**
     * Returns true if a viewport should always force the width of this
     * <code>Scrollable</code> to match the width of the viewport.
     *
     * @return true if a viewport should force the Scrollables width to
     * match its own, false otherwise
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportWidth() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            TextUI ui = getUI();
            int w = port.getWidth();
            Dimension min = ui.getMinimumSize(this);
            Dimension max = ui.getMaximumSize(this);
            if ((w >= min.width) && (w <= max.width)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if a viewport should always force the height of this
     * <code>Scrollable</code> to match the height of the viewport.
     *
     * @return true if a viewport should force the
     *          <code>Scrollable</code>'s height to match its own,
     *          false otherwise
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportHeight() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            TextUI ui = getUI();
            int h = port.getHeight();
            Dimension min = ui.getMinimumSize(this);
            if (h >= min.height) {
                Dimension max = ui.getMaximumSize(this);
                if (h <= max.height) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Serialization ------------------------------------

    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }

    // --- variables ---------------------------------------

    private SwingWorker<URL, Object> pageLoader;

    /**
     * Current content binding of the editor.
     */
    private EditorKit kit;
    private boolean isUserSetEditorKit;

    private Hashtable<String, Object> pageProperties;

    /** Should be kept in sync with javax.swing.text.html.FormView counterpart. */
    static final String PostDataProperty = "javax.swing.JEditorPane.postdata";

    /**
     * Table of registered type handlers for this editor.
     */
    private Hashtable<String, EditorKit> typeHandlers;

    /*
     * Private AppContext keys for this class's static variables.
     */
    private static final Object kitRegistryKey =
        new StringBuffer("JEditorPane.kitRegistry");
    private static final Object kitTypeRegistryKey =
        new StringBuffer("JEditorPane.kitTypeRegistry");
    private static final Object kitLoaderRegistryKey =
        new StringBuffer("JEditorPane.kitLoaderRegistry");

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "EditorPaneUI";


    /**
     * Key for a client property used to indicate whether
     * <a href="https://www.w3.org/TR/CSS22/syndata.html#length-units">
     * w3c compliant</a> length units are used for html rendering.
     * <p>
     * By default this is not enabled; to enable
     * it set the client {@link #putClientProperty property} with this name
     * to <code>Boolean.TRUE</code>.
     *
     * @since 1.5
     */
    public static final String W3C_LENGTH_UNITS = "JEditorPane.w3cLengthUnits";

    /**
     * Key for a client property used to indicate whether
     * the default font and foreground color from the component are
     * used if a font or foreground color is not specified in the styled
     * text.
     * <p>
     * The default varies based on the look and feel;
     * to enable it set the client {@link #putClientProperty property} with
     * this name to <code>Boolean.TRUE</code>.
     *
     * @since 1.5
     */
    public static final String HONOR_DISPLAY_PROPERTIES = "JEditorPane.honorDisplayProperties";

    static final Map<String, String> defaultEditorKitMap = new HashMap<String, String>(0);

    /**
     * Returns a string representation of this <code>JEditorPane</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JEditorPane</code>
     */
    protected String paramString() {
        String kitString = (kit != null ?
                            kit.toString() : "");
        String typeHandlersString = (typeHandlers != null ?
                                     typeHandlers.toString() : "");

        return super.paramString() +
        ",kit=" + kitString +
        ",typeHandlers=" + typeHandlersString;
    }


/////////////////
// Accessibility support
////////////////


    /**
     * Gets the AccessibleContext associated with this JEditorPane.
     * For editor panes, the AccessibleContext takes the form of an
     * AccessibleJEditorPane.
     * A new AccessibleJEditorPane instance is created if necessary.
     *
     * @return an AccessibleJEditorPane that serves as the
     *         AccessibleContext of this JEditorPane
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (getEditorKit() instanceof HTMLEditorKit) {
            if (accessibleContext == null || accessibleContext.getClass() !=
                    AccessibleJEditorPaneHTML.class) {
                accessibleContext = new AccessibleJEditorPaneHTML();
            }
        } else if (accessibleContext == null || accessibleContext.getClass() !=
                       AccessibleJEditorPane.class) {
            accessibleContext = new AccessibleJEditorPane();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JEditorPane</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to editor pane user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJEditorPane extends AccessibleJTextComponent {

        /**
         * Constructs an {@code AccessibleJEditorPane}.
         */
        protected AccessibleJEditorPane() {}

        /**
         * Gets the accessibleDescription property of this object.  If this
         * property isn't set, returns the content type of this
         * <code>JEditorPane</code> instead (e.g. "plain/text", "html/text").
         *
         * @return the localized description of the object; <code>null</code>
         *      if this object does not have a description
         *
         * @see #setAccessibleName
         */
        public String getAccessibleDescription() {
            String description = accessibleDescription;

            // fallback to client property
            if (description == null) {
                description = (String)getClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY);
            }
            if (description == null) {
                description = JEditorPane.this.getContentType();
            }
            return description;
        }

        /**
         * Gets the state set of this object.
         *
         * @return an instance of AccessibleStateSet describing the states
         * of the object
         * @see AccessibleStateSet
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            states.add(AccessibleState.MULTI_LINE);
            return states;
        }
    }

    /**
     * This class provides support for <code>AccessibleHypertext</code>,
     * and is used in instances where the <code>EditorKit</code>
     * installed in this <code>JEditorPane</code> is an instance of
     * <code>HTMLEditorKit</code>.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJEditorPaneHTML extends AccessibleJEditorPane {

        private AccessibleContext accessibleContext;

        /**
         * Returns the accessible text.
         * @return the accessible text
         */
        public AccessibleText getAccessibleText() {
            return new JEditorPaneAccessibleHypertextSupport();
        }

        /**
         * Constructs an {@code AccessibleJEditorPaneHTML}.
         */
        protected AccessibleJEditorPaneHTML () {
            HTMLEditorKit kit = (HTMLEditorKit)JEditorPane.this.getEditorKit();
            accessibleContext = kit.getAccessibleContext();
        }

        /**
         * Returns the number of accessible children of the object.
         *
         * @return the number of accessible children of the object.
         */
        public int getAccessibleChildrenCount() {
            if (accessibleContext != null) {
                return accessibleContext.getAccessibleChildrenCount();
            } else {
                return 0;
            }
        }

        /**
         * Returns the specified Accessible child of the object.  The Accessible
         * children of an Accessible object are zero-based, so the first child
         * of an Accessible child is at index 0, the second child is at index 1,
         * and so on.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         * @see #getAccessibleChildrenCount
         */
        public Accessible getAccessibleChild(int i) {
            if (accessibleContext != null) {
                return accessibleContext.getAccessibleChild(i);
            } else {
                return null;
            }
        }

        /**
         * Returns the Accessible child, if one exists, contained at the local
         * coordinate Point.
         *
         * @param p The point relative to the coordinate system of this object.
         * @return the Accessible, if it exists, at the specified location;
         * otherwise null
         */
        public Accessible getAccessibleAt(Point p) {
            if (accessibleContext != null && p != null) {
                try {
                    AccessibleComponent acomp =
                        accessibleContext.getAccessibleComponent();
                    if (acomp != null) {
                        return acomp.getAccessibleAt(p);
                    } else {
                        return null;
                    }
                } catch (IllegalComponentStateException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * What's returned by
     * <code>AccessibleJEditorPaneHTML.getAccessibleText</code>.
     *
     * Provides support for <code>AccessibleHypertext</code> in case
     * there is an HTML document being displayed in this
     * <code>JEditorPane</code>.
     *
     */
    protected class JEditorPaneAccessibleHypertextSupport
    extends AccessibleJEditorPane implements AccessibleHypertext {

        /**
         * An HTML link.
         */
        public class HTMLLink extends AccessibleHyperlink {
            Element element;

            /**
             * Constructs a {@code HTMLLink}.
             * @param e the element
             */
            public HTMLLink(Element e) {
                element = e;
            }

            /**
             * Since the document a link is associated with may have
             * changed, this method returns whether this Link is valid
             * anymore (with respect to the document it references).
             *
             * @return a flag indicating whether this link is still valid with
             *         respect to the AccessibleHypertext it belongs to
             */
            public boolean isValid() {
                return JEditorPaneAccessibleHypertextSupport.this.linksValid;
            }

            /**
             * Returns the number of accessible actions available in this Link
             * If there are more than one, the first one is NOT considered the
             * "default" action of this LINK object (e.g. in an HTML imagemap).
             * In general, links will have only one AccessibleAction in them.
             *
             * @return the zero-based number of Actions in this object
             */
            public int getAccessibleActionCount() {
                return 1;
            }

            /**
             * Perform the specified Action on the object
             *
             * @param i zero-based index of actions
             * @return true if the action was performed; else false.
             * @see #getAccessibleActionCount
             */
            public boolean doAccessibleAction(int i) {
                if (i == 0 && isValid() == true) {
                    URL u = (URL) getAccessibleActionObject(i);
                    if (u != null) {
                        HyperlinkEvent linkEvent =
                            new HyperlinkEvent(JEditorPane.this, HyperlinkEvent.EventType.ACTIVATED, u);
                        JEditorPane.this.fireHyperlinkUpdate(linkEvent);
                        return true;
                    }
                }
                return false;  // link invalid or i != 0
            }

            /**
             * Return a String description of this particular
             * link action.  The string returned is the text
             * within the document associated with the element
             * which contains this link.
             *
             * @param i zero-based index of the actions
             * @return a String description of the action
             * @see #getAccessibleActionCount
             */
            public String getAccessibleActionDescription(int i) {
                if (i == 0 && isValid() == true) {
                    Document d = JEditorPane.this.getDocument();
                    if (d != null) {
                        try {
                            return d.getText(getStartIndex(),
                                             getEndIndex() - getStartIndex());
                        } catch (BadLocationException exception) {
                            return null;
                        }
                    }
                }
                return null;
            }

            /**
             * Returns a URL object that represents the link.
             *
             * @param i zero-based index of the actions
             * @return an URL representing the HTML link itself
             * @see #getAccessibleActionCount
             */
            public Object getAccessibleActionObject(int i) {
                if (i == 0 && isValid() == true) {
                    AttributeSet as = element.getAttributes();
                    AttributeSet anchor =
                        (AttributeSet) as.getAttribute(HTML.Tag.A);
                    String href = (anchor != null) ?
                        (String) anchor.getAttribute(HTML.Attribute.HREF) : null;
                    if (href != null) {
                        URL u;
                        try {
                            u = new URL(JEditorPane.this.getPage(), href);
                        } catch (MalformedURLException m) {
                            u = null;
                        }
                        return u;
                    }
                }
                return null;  // link invalid or i != 0
            }

            /**
             * Return an object that represents the link anchor,
             * as appropriate for that link.
             * <p>
             * E.g. from HTML:
             *   &lt;a href="http://openjdk.java.net"&gt;OpenJDK&lt;/a&gt;
             * this method would return a String containing the text:
             * 'OpenJDK'.
             * <p>
             * Similarly, from this HTML:
             *   &lt;a HREF="#top"&gt;&lt;img src="top-hat.gif" alt="top hat"&gt;&lt;/a&gt;
             * this might return the object ImageIcon("top-hat.gif", "top hat");
             *
             * @param i zero-based index of the actions
             * @return an Object representing the hypertext anchor
             * @see #getAccessibleActionCount
             */
            public Object getAccessibleActionAnchor(int i) {
                return getAccessibleActionDescription(i);
            }


            /**
             * Get the index with the hypertext document at which this
             * link begins
             *
             * @return index of start of link
             */
            public int getStartIndex() {
                return element.getStartOffset();
            }

            /**
             * Get the index with the hypertext document at which this
             * link ends
             *
             * @return index of end of link
             */
            public int getEndIndex() {
                return element.getEndOffset();
            }
        }

        private class LinkVector extends Vector<HTMLLink> {
            public int baseElementIndex(Element e) {
                HTMLLink l;
                for (int i = 0; i < elementCount; i++) {
                    l = elementAt(i);
                    if (l.element == e) {
                        return i;
                    }
                }
                return -1;
            }
        }

        LinkVector hyperlinks;
        boolean linksValid = false;

        /**
         * Build the private table mapping links to locations in the text
         */
        private void buildLinkTable() {
            hyperlinks.removeAllElements();
            Document d = JEditorPane.this.getDocument();
            if (d != null) {
                ElementIterator ei = new ElementIterator(d);
                Element e;
                AttributeSet as;
                AttributeSet anchor;
                String href;
                while ((e = ei.next()) != null) {
                    if (e.isLeaf()) {
                        as = e.getAttributes();
                    anchor = (AttributeSet) as.getAttribute(HTML.Tag.A);
                    href = (anchor != null) ?
                        (String) anchor.getAttribute(HTML.Attribute.HREF) : null;
                        if (href != null) {
                            hyperlinks.addElement(new HTMLLink(e));
                        }
                    }
                }
            }
            linksValid = true;
        }

        /**
         * Constructs a {@code JEditorPaneAccessibleHypertextSupport}.
         */
        public JEditorPaneAccessibleHypertextSupport() {
            hyperlinks = new LinkVector();
            Document d = JEditorPane.this.getDocument();
            if (d != null) {
                d.addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent theEvent) {
                        linksValid = false;
                    }
                    public void insertUpdate(DocumentEvent theEvent) {
                        linksValid = false;
                    }
                    public void removeUpdate(DocumentEvent theEvent) {
                        linksValid = false;
                    }
                });
            }
        }

        /**
         * Returns the number of links within this hypertext doc.
         *
         * @return number of links in this hypertext doc.
         */
        public int getLinkCount() {
            if (linksValid == false) {
                buildLinkTable();
            }
            return hyperlinks.size();
        }

        /**
         * Returns the index into an array of hyperlinks that
         * is associated with this character index, or -1 if there
         * is no hyperlink associated with this index.
         *
         * @param  charIndex index within the text
         * @return index into the set of hyperlinks for this hypertext doc.
         */
        public int getLinkIndex(int charIndex) {
            if (linksValid == false) {
                buildLinkTable();
            }
            Element e = null;
            Document doc = JEditorPane.this.getDocument();
            if (doc != null) {
                for (e = doc.getDefaultRootElement(); ! e.isLeaf(); ) {
                    int index = e.getElementIndex(charIndex);
                    e = e.getElement(index);
                }
            }

            // don't need to verify that it's an HREF element; if
            // not, then it won't be in the hyperlinks Vector, and
            // so indexOf will return -1 in any case
            return hyperlinks.baseElementIndex(e);
        }

        /**
         * Returns the index into an array of hyperlinks that
         * index.  If there is no hyperlink at this index, it returns
         * null.
         *
         * @param linkIndex into the set of hyperlinks for this hypertext doc.
         * @return string representation of the hyperlink
         */
        public AccessibleHyperlink getLink(int linkIndex) {
            if (linksValid == false) {
                buildLinkTable();
            }
            if (linkIndex >= 0 && linkIndex < hyperlinks.size()) {
                return hyperlinks.elementAt(linkIndex);
            } else {
                return null;
            }
        }

        /**
         * Returns the contiguous text within the document that
         * is associated with this hyperlink.
         *
         * @param linkIndex into the set of hyperlinks for this hypertext doc.
         * @return the contiguous text sharing the link at this index
         */
        public String getLinkText(int linkIndex) {
            if (linksValid == false) {
                buildLinkTable();
            }
            Element e = (Element) hyperlinks.elementAt(linkIndex);
            if (e != null) {
                Document d = JEditorPane.this.getDocument();
                if (d != null) {
                    try {
                        return d.getText(e.getStartOffset(),
                                         e.getEndOffset() - e.getStartOffset());
                    } catch (BadLocationException exception) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    static class PlainEditorKit extends DefaultEditorKit implements ViewFactory {

        /**
         * Fetches a factory that is suitable for producing
         * views of any models that are produced by this
         * kit.  The default is to have the UI produce the
         * factory, so this method has no implementation.
         *
         * @return the view factory
         */
        public ViewFactory getViewFactory() {
            return this;
        }

        /**
         * Creates a view from the given structural element of a
         * document.
         *
         * @param elem  the piece of the document to build a view of
         * @return the view
         * @see View
         */
        public View create(Element elem) {
            Document doc = elem.getDocument();
            Object i18nFlag
                = doc.getProperty("i18n"/*AbstractDocument.I18NProperty*/);
            if ((i18nFlag != null) && i18nFlag.equals(Boolean.TRUE)) {
                // build a view that support bidi
                return createI18N(elem);
            } else {
                return new WrappedPlainView(elem);
            }
        }

        View createI18N(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new PlainParagraph(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)){
                    return new BoxView(elem, View.Y_AXIS);
                }
            }
            return null;
        }

        /**
         * Paragraph for representing plain-text lines that support
         * bidirectional text.
         */
        static class PlainParagraph extends javax.swing.text.ParagraphView {

            PlainParagraph(Element elem) {
                super(elem);
                layoutPool = new LogicalView(elem);
                layoutPool.setParent(this);
            }

            protected void setPropertiesFromAttributes() {
                Component c = getContainer();
                if ((c != null)
                    && (! c.getComponentOrientation().isLeftToRight()))
                {
                    setJustification(StyleConstants.ALIGN_RIGHT);
                } else {
                    setJustification(StyleConstants.ALIGN_LEFT);
                }
            }

            /**
             * Fetch the constraining span to flow against for
             * the given child index.
             */
            public int getFlowSpan(int index) {
                Component c = getContainer();
                if (c instanceof JTextArea) {
                    JTextArea area = (JTextArea) c;
                    if (! area.getLineWrap()) {
                        // no limit if unwrapped
                        return Integer.MAX_VALUE;
                    }
                }
                return super.getFlowSpan(index);
            }

            protected SizeRequirements calculateMinorAxisRequirements(int axis,
                                                            SizeRequirements r)
            {
                SizeRequirements req
                    = super.calculateMinorAxisRequirements(axis, r);
                Component c = getContainer();
                if (c instanceof JTextArea) {
                    JTextArea area = (JTextArea) c;
                    if (! area.getLineWrap()) {
                        // min is pref if unwrapped
                        req.minimum = req.preferred;
                    }
                }
                return req;
            }

            /**
             * This class can be used to represent a logical view for
             * a flow.  It keeps the children updated to reflect the state
             * of the model, gives the logical child views access to the
             * view hierarchy, and calculates a preferred span.  It doesn't
             * do any rendering, layout, or model/view translation.
             */
            static class LogicalView extends CompositeView {

                LogicalView(Element elem) {
                    super(elem);
                }

                protected int getViewIndexAtPosition(int pos) {
                    Element elem = getElement();
                    if (elem.getElementCount() > 0) {
                        return elem.getElementIndex(pos);
                    }
                    return 0;
                }

                protected boolean
                updateChildren(DocumentEvent.ElementChange ec,
                               DocumentEvent e, ViewFactory f)
                {
                    return false;
                }

                protected void loadChildren(ViewFactory f) {
                    Element elem = getElement();
                    if (elem.getElementCount() > 0) {
                        super.loadChildren(f);
                    } else {
                        View v = new GlyphView(elem);
                        append(v);
                    }
                }

                public float getPreferredSpan(int axis) {
                    if( getViewCount() != 1 )
                        throw new Error("One child view is assumed.");

                    View v = getView(0);
                    //((GlyphView)v).setGlyphPainter(null);
                    return v.getPreferredSpan(axis);
                }

                /**
                 * Forward the DocumentEvent to the given child view.  This
                 * is implemented to reparent the child to the logical view
                 * (the children may have been parented by a row in the flow
                 * if they fit without breaking) and then execute the
                 * superclass behavior.
                 *
                 * @param v the child view to forward the event to.
                 * @param e the change information from the associated document
                 * @param a the current allocation of the view
                 * @param f the factory to use to rebuild if the view has
                 *          children
                 * @see #forwardUpdate
                 * @since 1.3
                 */
                protected void forwardUpdateToView(View v, DocumentEvent e,
                                                   Shape a, ViewFactory f) {
                    v.setParent(this);
                    super.forwardUpdateToView(v, e, a, f);
                }

                // The following methods don't do anything useful, they
                // simply keep the class from being abstract.

                public void paint(Graphics g, Shape allocation) {
                }

                protected boolean isBefore(int x, int y, Rectangle alloc) {
                    return false;
                }

                protected boolean isAfter(int x, int y, Rectangle alloc) {
                    return false;
                }

                protected View getViewAtPoint(int x, int y, Rectangle alloc) {
                    return null;
                }

                protected void childAllocation(int index, Rectangle a) {
                }
            }
        }
    }

/* This is useful for the nightmare of parsing multi-part HTTP/RFC822 headers
 * sensibly:
 * From a String like: 'timeout=15, max=5'
 * create an array of Strings:
 * { {"timeout", "15"},
 *   {"max", "5"}
 * }
 * From one like: 'Basic Realm="FuzzFace" Foo="Biz Bar Baz"'
 * create one like (no quotes in literal):
 * { {"basic", null},
 *   {"realm", "FuzzFace"}
 *   {"foo", "Biz Bar Baz"}
 * }
 * keys are converted to lower case, vals are left as is....
 *
 * author Dave Brown
 */


static class HeaderParser {

    /* table of key/val pairs - maxes out at 10!!!!*/
    String raw;
    String[][] tab;

    public HeaderParser(String raw) {
        this.raw = raw;
        tab = new String[10][2];
        parse();
    }

    private void parse() {

        if (raw != null) {
            raw = raw.trim();
            char[] ca = raw.toCharArray();
            int beg = 0, end = 0, i = 0;
            boolean inKey = true;
            boolean inQuote = false;
            int len = ca.length;
            while (end < len) {
                char c = ca[end];
                if (c == '=') { // end of a key
                    tab[i][0] = new String(ca, beg, end-beg).toLowerCase();
                    inKey = false;
                    end++;
                    beg = end;
                } else if (c == '\"') {
                    if (inQuote) {
                        tab[i++][1]= new String(ca, beg, end-beg);
                        inQuote=false;
                        do {
                            end++;
                        } while (end < len && (ca[end] == ' ' || ca[end] == ','));
                        inKey=true;
                        beg=end;
                    } else {
                        inQuote=true;
                        end++;
                        beg=end;
                    }
                } else if (c == ' ' || c == ',') { // end key/val, of whatever we're in
                    if (inQuote) {
                        end++;
                        continue;
                    } else if (inKey) {
                        tab[i++][0] = (new String(ca, beg, end-beg)).toLowerCase();
                    } else {
                        tab[i++][1] = (new String(ca, beg, end-beg));
                    }
                    while (end < len && (ca[end] == ' ' || ca[end] == ',')) {
                        end++;
                    }
                    inKey = true;
                    beg = end;
                } else {
                    end++;
                }
            }
            // get last key/val, if any
            if (--end > beg) {
                if (!inKey) {
                    if (ca[end] == '\"') {
                        tab[i++][1] = (new String(ca, beg, end-beg));
                    } else {
                        tab[i++][1] = (new String(ca, beg, end-beg+1));
                    }
                } else {
                    tab[i][0] = (new String(ca, beg, end-beg+1)).toLowerCase();
                }
            } else if (end == beg) {
                if (!inKey) {
                    if (ca[end] == '\"') {
                        tab[i++][1] = String.valueOf(ca[end-1]);
                    } else {
                        tab[i++][1] = String.valueOf(ca[end]);
                    }
                } else {
                    tab[i][0] = String.valueOf(ca[end]).toLowerCase();
                }
            }
        }

    }

    public String findKey(int i) {
        if (i < 0 || i > 10)
            return null;
        return tab[i][0];
    }

    public String findValue(int i) {
        if (i < 0 || i > 10)
            return null;
        return tab[i][1];
    }

    public String findValue(String key) {
        return findValue(key, null);
    }

    public String findValue(String k, String Default) {
        if (k == null)
            return Default;
        k = k.toLowerCase();
        for (int i = 0; i < 10; ++i) {
            if (tab[i][0] == null) {
                return Default;
            } else if (k.equals(tab[i][0])) {
                return tab[i][1];
            }
        }
        return Default;
    }

    public int findInt(String k, int Default) {
        try {
            return Integer.parseInt(findValue(k, String.valueOf(Default)));
        } catch (Throwable t) {
            return Default;
        }
    }
 }

}
