/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.dom;

import com.sun.org.apache.xerces.internal.dom.events.EventImpl;
import com.sun.org.apache.xerces.internal.dom.events.MutationEventImpl;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.DocumentEvent;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventException;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MutationEvent;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;


/**
 * The Document interface represents the entire HTML or XML document.
 * Conceptually, it is the root of the document tree, and provides the
 * primary access to the document's data.
 * <P>
 * Since elements, text nodes, comments, processing instructions,
 * etc. cannot exist outside the context of a Document, the Document
 * interface also contains the factory methods needed to create these
 * objects. The Node objects created have a ownerDocument attribute
 * which associates them with the Document within whose context they
 * were created.
 * <p>
 * The DocumentImpl class also implements the DOM Level 2 DocumentTraversal
 * interface. This interface is comprised of factory methods needed to
 * create NodeIterators and TreeWalkers. The process of creating NodeIterator
 * objects also adds these references to this document.
 * After finishing with an iterator it is important to remove the object
 * using the remove methods in this implementation. This allows the release of
 * the references from the iterator objects to the DOM Nodes.
 * <p>
 * <b>Note:</b> When any node in the document is serialized, the
 * entire document is serialized along with it.
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @author Andy Clark, IBM
 * @author Ralf Pfeiffer, IBM
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: Nov 2017
 */
public class DocumentImpl
    extends CoreDocumentImpl
    implements DocumentTraversal, DocumentEvent, DocumentRange {

    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = 515687835542616694L;

    //
    // Data
    //

    /** Iterators */
    // REVISIT: Should this be transient? -Ac
    protected List<NodeIterator> iterators;

     /** Ranges */
    // REVISIT: Should this be transient? -Ac
    protected List<Range> ranges;

    /** Table for event listeners registered to this document nodes. */
    protected Map<NodeImpl, List<LEntry>> eventListeners;

    /** Bypass mutation events firing. */
    protected boolean mutationEvents = false;


    /**
     * @serialField iterators Vector Node iterators
     * @serialField ranges Vector ranges
     * @serialField eventListeners Hashtable Event listeners
     * @serialField mutationEvents boolean Bypass mutation events firing
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("iterators", Vector.class),
            new ObjectStreamField("ranges", Vector.class),
            new ObjectStreamField("eventListeners", Hashtable.class),
            new ObjectStreamField("mutationEvents", boolean.class),
        };

    //
    // Constructors
    //

    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public DocumentImpl() {
        super();
    }

    /** Constructor. */
    public DocumentImpl(boolean grammarAccess) {
        super(grammarAccess);
    }

    /**
     * For DOM2 support.
     * The createDocument factory method is in DOMImplementation.
     */
    public DocumentImpl(DocumentType doctype)
    {
        super(doctype);
    }

    /** For DOM2 support. */
    public DocumentImpl(DocumentType doctype, boolean grammarAccess) {
        super(doctype, grammarAccess);
    }

    //
    // Node methods
    //

    /**
     * Deep-clone a document, including fixing ownerDoc for the cloned
     * children. Note that this requires bypassing the WRONG_DOCUMENT_ERR
     * protection. I've chosen to implement it by calling importNode
     * which is DOM Level 2.
     *
     * @return org.w3c.dom.Node
     * @param deep boolean, iff true replicate children
     */
    public Node cloneNode(boolean deep) {

        DocumentImpl newdoc = new DocumentImpl();
        callUserDataHandlers(this, newdoc, UserDataHandler.NODE_CLONED);
        cloneNode(newdoc, deep);

        // experimental
        newdoc.mutationEvents = mutationEvents;

        return newdoc;

    } // cloneNode(boolean):Node

    /**
     * Retrieve information describing the abilities of this particular
     * DOM implementation. Intended to support applications that may be
     * using DOMs retrieved from several different sources, potentially
     * with different underlying representations.
     */
    public DOMImplementation getImplementation() {
        // Currently implemented as a singleton, since it's hardcoded
        // information anyway.
        return DOMImplementationImpl.getDOMImplementation();
    }

    //
    // DocumentTraversal methods
    //

    /**
     * NON-DOM extension:
     * Create and return a NodeIterator. The NodeIterator is
     * added to a list of NodeIterators so that it can be
     * removed to free up the DOM Nodes it references.
     *
     * @param root The root of the iterator.
     * @param whatToShow The whatToShow mask.
     * @param filter The NodeFilter installed. Null means no filter.
     */
    public NodeIterator createNodeIterator(Node root,
                                           short whatToShow,
                                           NodeFilter filter)
    {
        return createNodeIterator(root, whatToShow, filter, true);
    }

    /**
     * Create and return a NodeIterator. The NodeIterator is
     * added to a list of NodeIterators so that it can be
     * removed to free up the DOM Nodes it references.
     *
     * @param root The root of the iterator.
     * @param whatToShow The whatToShow mask.
     * @param filter The NodeFilter installed. Null means no filter.
     * @param entityReferenceExpansion true to expand the contents of
     *                                 EntityReference nodes
     * @since WD-DOM-Level-2-19990923
     */
    public NodeIterator createNodeIterator(Node root,
                                           int whatToShow,
                                           NodeFilter filter,
                                           boolean entityReferenceExpansion)
    {

        if (root == null) {
                  String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                  throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }

        NodeIterator iterator = new NodeIteratorImpl(this,
                                                     root,
                                                     whatToShow,
                                                     filter,
                                                     entityReferenceExpansion);
        if (iterators == null) {
            iterators = new ArrayList<>();
        }

        iterators.add(iterator);

        return iterator;
    }

    /**
     * NON-DOM extension:
     * Create and return a TreeWalker.
     *
     * @param root The root of the iterator.
     * @param whatToShow The whatToShow mask.
     * @param filter The NodeFilter installed. Null means no filter.
     */
    public TreeWalker createTreeWalker(Node root,
                                       short whatToShow,
                                       NodeFilter filter)
    {
        return createTreeWalker(root, whatToShow, filter, true);
    }
    /**
     * Create and return a TreeWalker.
     *
     * @param root The root of the iterator.
     * @param whatToShow The whatToShow mask.
     * @param filter The NodeFilter installed. Null means no filter.
     * @param entityReferenceExpansion true to expand the contents of
     *                                 EntityReference nodes
     * @since WD-DOM-Level-2-19990923
     */
    public TreeWalker createTreeWalker(Node root,
                                       int whatToShow,
                                       NodeFilter filter,
                                       boolean entityReferenceExpansion)
    {
        if (root == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }
        return new TreeWalkerImpl(root, whatToShow, filter,
                                  entityReferenceExpansion);
    }

    //
    // Not DOM Level 2. Support DocumentTraversal methods.
    //

    /** This is not called by the developer client. The
     *  developer client uses the detach() function on the
     *  NodeIterator itself. <p>
     *
     *  This function is called from the NodeIterator#detach().
     */
     void removeNodeIterator(NodeIterator nodeIterator) {

        if (nodeIterator == null) return;
        if (iterators == null) return;

        iterators.remove(nodeIterator);
    }

    //
    // DocumentRange methods
    //
    /**
     */
    public Range createRange() {

        if (ranges == null) {
            ranges = new ArrayList<>();
        }

        Range range = new RangeImpl(this);
        ranges.add(range);

        return range;

    }

    /** Not a client function. Called by Range.detach(),
     *  so a Range can remove itself from the list of
     *  Ranges.
     */
    void removeRange(Range range) {

        if (range == null) return;
        if (ranges == null) return;

        ranges.remove(range);
    }

    /**
     * A method to be called when some text was changed in a text node,
     * so that live objects can be notified.
     */
    void replacedText(NodeImpl node) {
        // notify ranges
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).receiveReplacedText(node);
            }
        }
    }

    /**
     * A method to be called when some text was deleted from a text node,
     * so that live objects can be notified.
     */
    void deletedText(NodeImpl node, int offset, int count) {
        // notify ranges
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).receiveDeletedText(node,
                                                                offset, count);
            }
        }
    }

    /**
     * A method to be called when some text was inserted into a text node,
     * so that live objects can be notified.
     */
    void insertedText(NodeImpl node, int offset, int count) {
        // notify ranges
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).receiveInsertedText(node,
                                                                offset, count);
            }
        }
    }

    /**
     * A method to be called when a text node has been split,
     * so that live objects can be notified.
     */
    void splitData(Node node, Node newNode, int offset) {
        // notify ranges
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).receiveSplitData(node,
                                                              newNode, offset);
            }
        }
    }

    //
    // DocumentEvent methods
    //

    /**
     * Introduced in DOM Level 2. Optional. <p>
     * Create and return Event objects.
     *
     * @param type The eventType parameter specifies the type of Event
     * interface to be created.  If the Event interface specified is supported
     * by the implementation this method will return a new Event of the
     * interface type requested. If the Event is to be dispatched via the
     * dispatchEvent method the appropriate event init method must be called
     * after creation in order to initialize the Event's values.  As an
     * example, a user wishing to synthesize some kind of Event would call
     * createEvent with the parameter "Events". The initEvent method could then
     * be called on the newly created Event to set the specific type of Event
     * to be dispatched and set its context information.
     * @return Newly created Event
     * @exception DOMException NOT_SUPPORTED_ERR: Raised if the implementation
     * does not support the type of Event interface requested
     * @since WD-DOM-Level-2-19990923
     */
    public Event createEvent(String type)
        throws DOMException {
            if (type.equalsIgnoreCase("Events") || "Event".equals(type))
                return new EventImpl();
            if (type.equalsIgnoreCase("MutationEvents") ||
                "MutationEvent".equals(type))
                return new MutationEventImpl();
            else {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }
        }

    /**
     * Sets whether the DOM implementation generates mutation events
     * upon operations.
     */
    void setMutationEvents(boolean set) {
        mutationEvents = set;
    }

    /**
     * Returns true if the DOM implementation generates mutation events.
     */
    boolean getMutationEvents() {
        return mutationEvents;
    }

    /**
     * Store event listener registered on a given node
     * This is another place where we could use weak references! Indeed, the
     * node here won't be GC'ed as long as some listener is registered on it,
     * since the eventsListeners table will have a reference to the node.
     */
    protected void setEventListeners(NodeImpl n, List<LEntry> listeners) {
        if (eventListeners == null) {
            eventListeners = new HashMap<>();
        }
        if (listeners == null) {
            eventListeners.remove(n);
            if (eventListeners.isEmpty()) {
                // stop firing events when there isn't any listener
                mutationEvents = false;
            }
        } else {
            eventListeners.put(n, listeners);
            // turn mutation events on
            mutationEvents = true;
        }
    }

    /**
     * Retreive event listener registered on a given node
     */
    protected List<LEntry> getEventListeners(NodeImpl n) {
        if (eventListeners == null) {
            return null;
        }
        return eventListeners.get(n);
    }

    //
    // EventTarget support (public and internal)
    //

    //
    // Constants
    //

    /*
     * NON-DOM INTERNAL: Class LEntry is just a struct used to represent
     * event listeners registered with this node. Copies of this object
     * are hung from the nodeListeners Vector.
     * <p>
     * I considered using two vectors -- one for capture,
     * one for bubble -- but decided that since the list of listeners
     * is probably short in most cases, it might not be worth spending
     * the space. ***** REVISIT WHEN WE HAVE MORE EXPERIENCE.
     */
    class LEntry implements Serializable {

        private static final long serialVersionUID = -8426757059492421631L;
        String type;
        EventListener listener;
        boolean useCapture;

        /** NON-DOM INTERNAL: Constructor for Listener list Entry
         * @param type Event name (NOT event group!) to listen for.
         * @param listener Who gets called when event is dispatched
         * @param useCaptue True iff listener is registered on
         *  capturing phase rather than at-target or bubbling
         */
        LEntry(String type, EventListener listener, boolean useCapture)
        {
            this.type = type;
            this.listener = listener;
            this.useCapture = useCapture;
        }

    } // LEntry

    /**
     * Introduced in DOM Level 2. <p> Register an event listener with this
     * Node. A listener may be independently registered as both Capturing and
     * Bubbling, but may only be registered once per role; redundant
     * registrations are ignored.
     * @param node node to add listener to
     * @param type Event name (NOT event group!) to listen for.
     * @param listener Who gets called when event is dispatched
     * @param useCapture True iff listener is registered on
     *  capturing phase rather than at-target or bubbling
     */
    @Override
    protected void addEventListener(NodeImpl node, String type,
                                    EventListener listener, boolean useCapture)
    {
        // We can't dispatch to blank type-name, and of course we need
        // a listener to dispatch to
        if (type == null || type.equals("") || listener == null)
            return;

        // Each listener may be registered only once per type per phase.
        // Simplest way to code that is to zap the previous entry, if any.
        removeEventListener(node, type, listener, useCapture);

        List<LEntry> nodeListeners = getEventListeners(node);
        if(nodeListeners == null) {
            nodeListeners = new ArrayList<>();
            setEventListeners(node, nodeListeners);
        }
        nodeListeners.add(new LEntry(type, listener, useCapture));

        // Record active listener
        LCount lc = LCount.lookup(type);
        if (useCapture) {
            ++lc.captures;
            ++lc.total;
        }
        else {
            ++lc.bubbles;
            ++lc.total;
        }

    } // addEventListener(NodeImpl,String,EventListener,boolean) :void

    /**
     * Introduced in DOM Level 2. <p> Deregister an event listener previously
     * registered with this Node.  A listener must be independently removed
     * from the Capturing and Bubbling roles. Redundant removals (of listeners
     * not currently registered for this role) are ignored.
     * @param node node to remove listener from
     * @param type Event name (NOT event group!) to listen for.
     * @param listener Who gets called when event is dispatched
     * @param useCapture True iff listener is registered on
     *  capturing phase rather than at-target or bubbling
     */
    @Override
    protected void removeEventListener(NodeImpl node, String type,
                                       EventListener listener,
                                       boolean useCapture)
    {
        // If this couldn't be a valid listener registration, ignore request
        if (type == null || type.equals("") || listener == null)
            return;
        List<LEntry> nodeListeners = getEventListeners(node);
        if (nodeListeners == null)
            return;

        // Note that addListener has previously ensured that
        // each listener may be registered only once per type per phase.
        // count-down is OK for deletions!
        for (int i = nodeListeners.size() - 1; i >= 0; --i) {
            LEntry le = nodeListeners.get(i);
            if (le.useCapture == useCapture && le.listener == listener &&
                le.type.equals(type)) {
                nodeListeners.remove(i);
                // Storage management: Discard empty listener lists
                if (nodeListeners.isEmpty())
                    setEventListeners(node, null);

                // Remove active listener
                LCount lc = LCount.lookup(type);
                if (useCapture) {
                    --lc.captures;
                    --lc.total;
                }
                else {
                    --lc.bubbles;
                    --lc.total;
                }

                break;  // Found it; no need to loop farther.
            }
        }
    } // removeEventListener(NodeImpl,String,EventListener,boolean) :void

    @Override
    protected void copyEventListeners(NodeImpl src, NodeImpl tgt) {
        List<LEntry> nodeListeners = getEventListeners(src);
        if (nodeListeners == null) {
            return;
        }
        setEventListeners(tgt, new ArrayList<>(nodeListeners));
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Distribution engine for DOM Level 2 Events.
     * <p>
     * Event propagation runs as follows:
     * <ol>
     * <li>Event is dispatched to a particular target node, which invokes
     *   this code. Note that the event's stopPropagation flag is
     *   cleared when dispatch begins; thereafter, if it has
     *   been set before processing of a node commences, we instead
     *   immediately advance to the DEFAULT phase.
     * <li>The node's ancestors are established as destinations for events.
     *   For capture and bubble purposes, node ancestry is determined at
     *   the time dispatch starts. If an event handler alters the document
     *   tree, that does not change which nodes will be informed of the event.
     * <li>CAPTURING_PHASE: Ancestors are scanned, root to target, for
     *   Capturing listeners. If found, they are invoked (see below).
     * <li>AT_TARGET:
     *   Event is dispatched to NON-CAPTURING listeners on the
     *   target node. Note that capturing listeners on this node are _not_
     *   invoked.
     * <li>BUBBLING_PHASE: Ancestors are scanned, target to root, for
     *   non-capturing listeners.
     * <li>Default processing: Some DOMs have default behaviors bound to
     *   specific nodes. If this DOM does, and if the event's preventDefault
     *   flag has not been set, we now return to the target node and process
     *   its default handler for this event, if any.
     * </ol>
     * <p>
     * Note that registration of handlers during processing of an event does
     * not take effect during this phase of this event; they will not be called
     * until the next time this node is visited by dispatchEvent. On the other
     * hand, removals take effect immediately.
     * <p>
     * If an event handler itself causes events to be dispatched, they are
     * processed synchronously, before processing resumes
     * on the event which triggered them. Please be aware that this may
     * result in events arriving at listeners "out of order" relative
     * to the actual sequence of requests.
     * <p>
     * Note that our implementation resets the event's stop/prevent flags
     * when dispatch begins.
     * I believe the DOM's intent is that event objects be redispatchable,
     * though it isn't stated in those terms.
     * @param node node to dispatch to
     * @param event the event object to be dispatched to
     *              registered EventListeners
     * @return true if the event's <code>preventDefault()</code>
     *              method was invoked by an EventListener; otherwise false.
    */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean dispatchEvent(NodeImpl node, Event event) {
        if (event == null) return false;

        // Can't use anyone else's implementation, since there's no public
        // API for setting the event's processing-state fields.
        EventImpl evt = (EventImpl)event;

        // VALIDATE -- must have been initialized at least once, must have
        // a non-null non-blank name.
        if(!evt.initialized || evt.type == null || evt.type.equals("")) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "UNSPECIFIED_EVENT_TYPE_ERR", null);
            throw new EventException(EventException.UNSPECIFIED_EVENT_TYPE_ERR, msg);
        }

        // If nobody is listening for this event, discard immediately
        LCount lc = LCount.lookup(evt.getType());
        if (lc.total == 0)
            return evt.preventDefault;

        // INITIALIZE THE EVENT'S DISPATCH STATUS
        // (Note that Event objects are reusable in our implementation;
        // that doesn't seem to be explicitly guaranteed in the DOM, but
        // I believe it is the intent.)
        evt.target = node;
        evt.stopPropagation = false;
        evt.preventDefault = false;

        // Capture pre-event parentage chain, not including target;
        // use pre-event-dispatch ancestors even if event handlers mutate
        // document and change the target's context.
        // Note that this is parents ONLY; events do not
        // cross the Attr/Element "blood/brain barrier".
        // DOMAttrModified. which looks like an exception,
        // is issued to the Element rather than the Attr
        // and causes a _second_ DOMSubtreeModified in the Element's
        // tree.
        List<Node> pv = new ArrayList<>(10);
        Node p = node;
        Node n = p.getParentNode();
        while (n != null) {
            pv.add(n);
            p = n;
            n = n.getParentNode();
        }

        // CAPTURING_PHASE:
        if (lc.captures > 0) {
            evt.eventPhase = Event.CAPTURING_PHASE;
            // Ancestors are scanned, root to target, for
            // Capturing listeners.
            for (int j = pv.size() - 1; j >= 0; --j) {
                if (evt.stopPropagation)
                    break;  // Someone set the flag. Phase ends.

                // Handle all capturing listeners on this node
                NodeImpl nn = (NodeImpl) pv.get(j);
                evt.currentTarget = nn;
                ArrayList<LEntry> nodeListeners = (ArrayList<LEntry>)getEventListeners(nn);
                if (nodeListeners != null) {
                    List<LEntry> nl = (ArrayList<LEntry>)nodeListeners.clone();
                    // call listeners in the order in which they got registered
                    int nlsize = nl.size();
                    for (int i = 0; i < nlsize; i++) {
                        LEntry le = nl.get(i);
                        if (le.useCapture && le.type.equals(evt.type) &&
                            nodeListeners.contains(le)) {
                            try {
                                le.listener.handleEvent(evt);
                            }
                            catch (Exception e) {
                                // All exceptions are ignored.
                            }
                        }
                    }
                }
            }
        }


        // Both AT_TARGET and BUBBLE use non-capturing listeners.
        if (lc.bubbles > 0) {
            // AT_TARGET PHASE: Event is dispatched to NON-CAPTURING listeners
            // on the target node. Note that capturing listeners on the target
            // node are _not_ invoked, even during the capture phase.
            evt.eventPhase = Event.AT_TARGET;
            evt.currentTarget = node;
            ArrayList<LEntry> nodeListeners = (ArrayList<LEntry>)getEventListeners(node);
            if (!evt.stopPropagation && nodeListeners != null) {
                List<LEntry> nl = (ArrayList<LEntry>)nodeListeners.clone();
                // call listeners in the order in which they got registered
                int nlsize = nl.size();
                for (int i = 0; i < nlsize; i++) {
                    LEntry le = nl.get(i);
                    if (!le.useCapture && le.type.equals(evt.type) &&
                        nodeListeners.contains(le)) {
                        try {
                            le.listener.handleEvent(evt);
                        }
                        catch (Exception e) {
                            // All exceptions are ignored.
                        }
                    }
                }
            }
            // BUBBLING_PHASE: Ancestors are scanned, target to root, for
            // non-capturing listeners. If the event's preventBubbling flag
            // has been set before processing of a node commences, we
            // instead immediately advance to the default phase.
            // Note that not all events bubble.
            if (evt.bubbles) {
                evt.eventPhase = Event.BUBBLING_PHASE;
                int pvsize = pv.size();
                for (int j = 0; j < pvsize; j++) {
                    if (evt.stopPropagation)
                        break;  // Someone set the flag. Phase ends.

                    // Handle all bubbling listeners on this node
                    NodeImpl nn = (NodeImpl) pv.get(j);
                    evt.currentTarget = nn;
                    nodeListeners = (ArrayList<LEntry>)getEventListeners(nn);
                    if (nodeListeners != null) {
                        List<LEntry> nl = (ArrayList<LEntry>)nodeListeners.clone();
                        // call listeners in the order in which they got
                        // registered
                        int nlsize = nl.size();
                        for (int i = 0; i < nlsize; i++) {
                            LEntry le = nl.get(i);
                            if (!le.useCapture && le.type.equals(evt.type) &&
                                nodeListeners.contains(le)) {
                                try {
                                    le.listener.handleEvent(evt);
                                }
                                catch (Exception e) {
                                    // All exceptions are ignored.
                                }
                            }
                        }
                    }
                }
            }
        }

        // DEFAULT PHASE: Some DOMs have default behaviors bound to specific
        // nodes. If this DOM does, and if the event's preventDefault flag has
        // not been set, we now return to the target node and process its
        // default handler for this event, if any.
        // No specific phase value defined, since this is DOM-internal
        if (lc.defaults > 0 && (!evt.cancelable || !evt.preventDefault)) {
            // evt.eventPhase = Event.DEFAULT_PHASE;
            // evt.currentTarget = node;
            // DO_DEFAULT_OPERATION
        }

        return evt.preventDefault;
    } // dispatchEvent(NodeImpl,Event) :boolean

    /**
     * NON-DOM INTERNAL: DOMNodeInsertedIntoDocument and ...RemovedFrom...
     * are dispatched to an entire subtree. This is the distribution code
     * therefor. They DO NOT bubble, thanks be, but may be captured.
     * <p>
     * Similar to code in dispatchingEventToSubtree however this method
     * is only used on the target node and does not start a dispatching chain
     * on the sibling of the target node as this is not part of the subtree
     * ***** At the moment I'm being sloppy and using the normal
     * capture dispatcher on every node. This could be optimized hugely
     * by writing a capture engine that tracks our position in the tree to
     * update the capture chain without repeated chases up to root.
     * @param n target node (that was directly inserted or removed)
     * @param e event to be sent to that node and its subtree
     */
    protected void dispatchEventToSubtree(Node n, Event e) {

        ((NodeImpl) n).dispatchEvent(e);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap a = n.getAttributes();
            for (int i = a.getLength() - 1; i >= 0; --i)
                dispatchingEventToSubtree(a.item(i), e);
        }
        dispatchingEventToSubtree(n.getFirstChild(), e);

    } // dispatchEventToSubtree(NodeImpl,Node,Event) :void


    /**
     * Dispatches event to the target node's descendents recursively
     *
     * @param n node to dispatch to
     * @param e event to be sent to that node and its subtree
     */
    protected void dispatchingEventToSubtree(Node n, Event e) {
        if (n==null)
                return;

        // ***** Recursive implementation. This is excessively expensive,
        // and should be replaced in conjunction with optimization
        // mentioned above.
        ((NodeImpl) n).dispatchEvent(e);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap a = n.getAttributes();
            for (int i = a.getLength() - 1; i >= 0; --i)
                dispatchingEventToSubtree(a.item(i), e);
        }
        dispatchingEventToSubtree(n.getFirstChild(), e);
        dispatchingEventToSubtree(n.getNextSibling(), e);
    }

    /**
     * NON-DOM INTERNAL: Return object for getEnclosingAttr. Carries
     * (two values, the Attr node affected (if any) and its previous
     * string value. Simple struct, no methods.
     */
    class EnclosingAttr implements Serializable {
        private static final long serialVersionUID = 5208387723391647216L;
        AttrImpl node;
        String oldvalue;
    }

    EnclosingAttr savedEnclosingAttr;

    /**
     * NON-DOM INTERNAL: Convenience wrapper for calling
     * dispatchAggregateEvents when the context was established
     * by <code>savedEnclosingAttr</code>.
     * @param node node to dispatch to
     * @param ea description of Attr affected by current operation
     */
    protected void dispatchAggregateEvents(NodeImpl node, EnclosingAttr ea) {
        if (ea != null)
            dispatchAggregateEvents(node, ea.node, ea.oldvalue,
                                    MutationEvent.MODIFICATION);
        else
            dispatchAggregateEvents(node, null, null, (short) 0);

    } // dispatchAggregateEvents(NodeImpl,EnclosingAttr) :void

    /**
     * NON-DOM INTERNAL: Generate the "aggregated" post-mutation events
     * DOMAttrModified and DOMSubtreeModified.
     * Both of these should be issued only once for each user-requested
     * mutation operation, even if that involves multiple changes to
     * the DOM.
     * For example, if a DOM operation makes multiple changes to a single
     * Attr before returning, it would be nice to generate only one
     * DOMAttrModified, and multiple changes over larger scope but within
     * a recognizable single subtree might want to generate only one
     * DOMSubtreeModified, sent to their lowest common ancestor.
     * <p>
     * To manage this, use the "internal" versions of insert and remove
     * with MUTATION_LOCAL, then make an explicit call to this routine
     * at the higher level. Some examples now exist in our code.
     *
     * @param node The node to dispatch to
     * @param enclosingAttr The Attr node (if any) whose value has been changed
     * as a result of the DOM operation. Null if none such.
     * @param oldValue The String value previously held by the
     * enclosingAttr. Ignored if none such.
     * @param change Type of modification to the attr. See
     * MutationEvent.attrChange
     */
    protected void dispatchAggregateEvents(NodeImpl node,
                                           AttrImpl enclosingAttr,
                                           String oldvalue, short change) {
        // We have to send DOMAttrModified.
        NodeImpl owner = null;
        if (enclosingAttr != null) {
            LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
            owner = (NodeImpl) enclosingAttr.getOwnerElement();
            if (lc.total > 0) {
                if (owner != null) {
                    MutationEventImpl me =  new MutationEventImpl();
                    me.initMutationEvent(MutationEventImpl.DOM_ATTR_MODIFIED,
                                         true, false, enclosingAttr,
                                         oldvalue,
                                         enclosingAttr.getNodeValue(),
                                         enclosingAttr.getNodeName(),
                                         change);
                    owner.dispatchEvent(me);
                }
            }
        }
        // DOMSubtreeModified gets sent to the lowest common root of a
        // set of changes.
        // "This event is dispatched after all other events caused by the
        // mutation have been fired."
        LCount lc = LCount.lookup(MutationEventImpl.DOM_SUBTREE_MODIFIED);
        if (lc.total > 0) {
            MutationEvent me =  new MutationEventImpl();
            me.initMutationEvent(MutationEventImpl.DOM_SUBTREE_MODIFIED,
                                 true, false, null, null,
                                 null, null, (short) 0);

            // If we're within an Attr, DStM gets sent to the Attr
            // and to its owningElement. Otherwise we dispatch it
            // locally.
            if (enclosingAttr != null) {
                dispatchEvent(enclosingAttr, me);
                if (owner != null)
                    dispatchEvent(owner, me);
            }
            else
                dispatchEvent(node, me);
        }
    } // dispatchAggregateEvents(NodeImpl, AttrImpl,String) :void

    /**
     * NON-DOM INTERNAL: Pre-mutation context check, in
     * preparation for later generating DOMAttrModified events.
     * Determines whether this node is within an Attr
     * @param node node to get enclosing attribute for
     * @return either a description of that Attr, or null if none such.
     */
    protected void saveEnclosingAttr(NodeImpl node) {
        savedEnclosingAttr = null;
        // MUTATION PREPROCESSING AND PRE-EVENTS:
        // If we're within the scope of an Attr and DOMAttrModified
        // was requested, we need to preserve its previous value for
        // that event.
        LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
        if (lc.total > 0) {
            NodeImpl eventAncestor = node;
            while (true) {
                if (eventAncestor == null)
                    return;
                int type = eventAncestor.getNodeType();
                if (type == Node.ATTRIBUTE_NODE) {
                    EnclosingAttr retval = new EnclosingAttr();
                    retval.node = (AttrImpl) eventAncestor;
                    retval.oldvalue = retval.node.getNodeValue();
                    savedEnclosingAttr = retval;
                    return;
                }
                else if (type == Node.ENTITY_REFERENCE_NODE)
                    eventAncestor = eventAncestor.parentNode();
                else if (type == Node.TEXT_NODE)
                    eventAncestor = eventAncestor.parentNode();
                else
                    return;
                // Any other parent means we're not in an Attr
            }
        }
    } // saveEnclosingAttr(NodeImpl) :void

    /**
     * A method to be called when a character data node has been modified
     */
    void modifyingCharacterData(NodeImpl node, boolean replace) {
        if (mutationEvents) {
                if (!replace) {
                        saveEnclosingAttr(node);
                }
        }
    }

    /**
     * A method to be called when a character data node has been modified
     */
    void modifiedCharacterData(NodeImpl node, String oldvalue, String value, boolean replace) {
        if (mutationEvents) {
                if (!replace) {
                        // MUTATION POST-EVENTS:
                        LCount lc =
                                LCount.lookup(MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED);
                        if (lc.total > 0) {
                                MutationEvent me = new MutationEventImpl();
                                me.initMutationEvent(
                                        MutationEventImpl.DOM_CHARACTER_DATA_MODIFIED,
                                        true, false, null,
                                                                                oldvalue, value, null, (short) 0);
                                dispatchEvent(node, me);
                        }

                        // Subroutine: Transmit DOMAttrModified and DOMSubtreeModified,
                        // if required. (Common to most kinds of mutation)
                        dispatchAggregateEvents(node, savedEnclosingAttr);
                } // End mutation postprocessing
        }
    }

    /**
     * A method to be called when a character data node has been replaced
     */
    void replacedCharacterData(NodeImpl node, String oldvalue, String value) {
        //now that we have finished replacing data, we need to perform the same actions
        //that are required after a character data node has been modified
        //send the value of false for replace parameter so that mutation
        //events if appropriate will be initiated
        modifiedCharacterData(node, oldvalue, value, false);
    }



    /**
     * A method to be called when a node is about to be inserted in the tree.
     */
    void insertingNode(NodeImpl node, boolean replace) {
        if (mutationEvents) {
            if (!replace) {
                saveEnclosingAttr(node);
            }
        }
    }

    /**
     * A method to be called when a node has been inserted in the tree.
     */
    void insertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
        if (mutationEvents) {
            // MUTATION POST-EVENTS:
            // "Local" events (non-aggregated)
            // New child is told it was inserted, and where
            LCount lc = LCount.lookup(MutationEventImpl.DOM_NODE_INSERTED);
            if (lc.total > 0) {
                MutationEventImpl me = new MutationEventImpl();
                me.initMutationEvent(MutationEventImpl.DOM_NODE_INSERTED,
                                     true, false, node,
                                     null, null, null, (short) 0);
                dispatchEvent(newInternal, me);
            }

            // If within the Document, tell the subtree it's been added
            // to the Doc.
            lc = LCount.lookup(
                            MutationEventImpl.DOM_NODE_INSERTED_INTO_DOCUMENT);
            if (lc.total > 0) {
                NodeImpl eventAncestor = node;
                if (savedEnclosingAttr != null)
                    eventAncestor = (NodeImpl)
                        savedEnclosingAttr.node.getOwnerElement();
                if (eventAncestor != null) { // Might have been orphan Attr
                    NodeImpl p = eventAncestor;
                    while (p != null) {
                        eventAncestor = p; // Last non-null ancestor
                        // In this context, ancestry includes
                        // walking back from Attr to Element
                        if (p.getNodeType() == ATTRIBUTE_NODE) {
                            p = (NodeImpl) ((AttrImpl)p).getOwnerElement();
                        }
                        else {
                            p = p.parentNode();
                        }
                    }
                    if (eventAncestor.getNodeType() == Node.DOCUMENT_NODE){
                        MutationEventImpl me = new MutationEventImpl();
                        me.initMutationEvent(MutationEventImpl
                                             .DOM_NODE_INSERTED_INTO_DOCUMENT,
                                             false,false,null,null,
                                             null,null,(short)0);
                        dispatchEventToSubtree(newInternal, me);
                    }
                }
            }
            if (!replace) {
                // Subroutine: Transmit DOMAttrModified and DOMSubtreeModified
                // (Common to most kinds of mutation)
                dispatchAggregateEvents(node, savedEnclosingAttr);
            }
        }

        // notify the range of insertions
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).insertedNodeFromDOM(newInternal);
            }
        }
    }

    /**
     * A method to be called when a node is about to be removed from the tree.
     */
    void removingNode(NodeImpl node, NodeImpl oldChild, boolean replace) {

        // notify iterators
        if (iterators != null) {
            int size = iterators.size();
            for (int i = 0; i != size; i++) {
               ((NodeIteratorImpl)iterators.get(i)).removeNode(oldChild);
            }
        }

        // notify ranges
        if (ranges != null) {
            int size = ranges.size();
            for (int i = 0; i != size; i++) {
                ((RangeImpl)ranges.get(i)).removeNode(oldChild);
            }
        }

        // mutation events
        if (mutationEvents) {
            // MUTATION PREPROCESSING AND PRE-EVENTS:
            // If we're within the scope of an Attr and DOMAttrModified
            // was requested, we need to preserve its previous value for
            // that event.
            if (!replace) {
                saveEnclosingAttr(node);
            }
            // Child is told that it is about to be removed
            LCount lc = LCount.lookup(MutationEventImpl.DOM_NODE_REMOVED);
            if (lc.total > 0) {
                MutationEventImpl me= new MutationEventImpl();
                me.initMutationEvent(MutationEventImpl.DOM_NODE_REMOVED,
                                     true, false, node, null,
                                     null, null, (short) 0);
                dispatchEvent(oldChild, me);
            }

            // If within Document, child's subtree is informed that it's
            // losing that status
            lc = LCount.lookup(
                             MutationEventImpl.DOM_NODE_REMOVED_FROM_DOCUMENT);
            if (lc.total > 0) {
                NodeImpl eventAncestor = this;
                if(savedEnclosingAttr != null)
                    eventAncestor = (NodeImpl)
                        savedEnclosingAttr.node.getOwnerElement();
                if (eventAncestor != null) { // Might have been orphan Attr
                    for (NodeImpl p = eventAncestor.parentNode();
                         p != null; p = p.parentNode()) {
                        eventAncestor = p; // Last non-null ancestor
                    }
                    if (eventAncestor.getNodeType() == Node.DOCUMENT_NODE){
                        MutationEventImpl me = new MutationEventImpl();
                        me.initMutationEvent(
                              MutationEventImpl.DOM_NODE_REMOVED_FROM_DOCUMENT,
                                             false, false, null,
                                             null, null, null, (short) 0);
                        dispatchEventToSubtree(oldChild, me);
                    }
                }
            }
        } // End mutation preprocessing
    }

    /**
     * A method to be called when a node has been removed from the tree.
     */
    void removedNode(NodeImpl node, boolean replace) {
        if (mutationEvents) {
            // MUTATION POST-EVENTS:
            // Subroutine: Transmit DOMAttrModified and DOMSubtreeModified,
            // if required. (Common to most kinds of mutation)
            if (!replace) {
                dispatchAggregateEvents(node, savedEnclosingAttr);
            }
        } // End mutation postprocessing
    }

    /**
     * A method to be called when a node is about to be replaced in the tree.
     */
    void replacingNode(NodeImpl node) {
        if (mutationEvents) {
            saveEnclosingAttr(node);
        }
    }

    /**
     * A method to be called when character data is about to be replaced in the tree.
     */
    void replacingData (NodeImpl node) {
        if (mutationEvents) {
                        saveEnclosingAttr(node);
        }
    }

    /**
     * A method to be called when a node has been replaced in the tree.
     */
    void replacedNode(NodeImpl node) {
        if (mutationEvents) {
            dispatchAggregateEvents(node, savedEnclosingAttr);
        }
    }

    /**
     * A method to be called when an attribute value has been modified
     */
    void modifiedAttrValue(AttrImpl attr, String oldvalue) {
        if (mutationEvents) {
            // MUTATION POST-EVENTS:
            dispatchAggregateEvents(attr, attr, oldvalue,
                                    MutationEvent.MODIFICATION);
        }
    }

    /**
     * A method to be called when an attribute node has been set
     */
    void setAttrNode(AttrImpl attr, AttrImpl previous) {
        if (mutationEvents) {
            // MUTATION POST-EVENTS:
            if (previous == null) {
                dispatchAggregateEvents(attr.ownerNode, attr, null,
                                        MutationEvent.ADDITION);
            }
            else {
                dispatchAggregateEvents(attr.ownerNode, attr,
                                        previous.getNodeValue(),
                                        MutationEvent.MODIFICATION);
            }
        }
    }

    /**
     * A method to be called when an attribute node has been removed
     */
    void removedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
        // We can't use the standard dispatchAggregate, since it assumes
        // that the Attr is still attached to an owner. This code is
        // similar but dispatches to the previous owner, "element".
        if (mutationEvents) {
            // If we have to send DOMAttrModified (determined earlier),
            // do so.
            LCount lc = LCount.lookup(MutationEventImpl.DOM_ATTR_MODIFIED);
            if (lc.total > 0) {
                MutationEventImpl me= new MutationEventImpl();
                me.initMutationEvent(MutationEventImpl.DOM_ATTR_MODIFIED,
                                     true, false, attr,
                                     attr.getNodeValue(), null, name,
                                     MutationEvent.REMOVAL);
                dispatchEvent(oldOwner, me);
            }

            // We can hand off to process DOMSubtreeModified, though.
            // Note that only the Element needs to be informed; the
            // Attr's subtree has not been changed by this operation.
            dispatchAggregateEvents(oldOwner, null, null, (short) 0);
        }
    }


    /**
     * A method to be called when an attribute node has been renamed
     */
    void renamedAttrNode(Attr oldAt, Attr newAt) {
        // REVISIT: To be implemented!!!
    }

    /**
     * A method to be called when an element has been renamed
     */
    void renamedElement(Element oldEl, Element newEl) {
        // REVISIT: To be implemented!!!
    }


    /**
     * @serialData Serialized fields. Convert Maps to Hashtables and Lists
     * to Vectors for backward compatibility.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Convert Maps to Hashtables, Lists to Vectors
        Vector<NodeIterator> it = (iterators == null)? null : new Vector<>(iterators);
        Vector<Range> r = (ranges == null)? null : new Vector<>(ranges);

        Hashtable<NodeImpl, Vector<LEntry>> el = null;
        if (eventListeners != null) {
            el = new Hashtable<>();
            for (Map.Entry<NodeImpl, List<LEntry>> e : eventListeners.entrySet()) {
                 el.put(e.getKey(), new Vector<>(e.getValue()));
            }
        }

        // Write serialized fields
        ObjectOutputStream.PutField pf = out.putFields();
        pf.put("iterators", it);
        pf.put("ranges", r);
        pf.put("eventListeners", el);
        pf.put("mutationEvents", mutationEvents);
        out.writeFields();
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
        // We have to read serialized fields first.
        ObjectInputStream.GetField gf = in.readFields();
        Vector<NodeIterator> it = (Vector<NodeIterator>)gf.get("iterators", null);
        Vector<Range> r = (Vector<Range>)gf.get("ranges", null);
        Hashtable<NodeImpl, Vector<LEntry>> el =
                (Hashtable<NodeImpl, Vector<LEntry>>)gf.get("eventListeners", null);

        mutationEvents = gf.get("mutationEvents", false);

        //convert Hashtables back to HashMaps and Vectors to Lists
        if (it != null) iterators = new ArrayList<>(it);
        if (r != null) ranges = new ArrayList<>(r);
        if (el != null) {
            eventListeners = new HashMap<>();
            for (Map.Entry<NodeImpl, Vector<LEntry>> e : el.entrySet()) {
                 eventListeners.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
    }
} // class DocumentImpl
