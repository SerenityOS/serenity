/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Encapsulate Namespace logic for use by applications using SAX,
 * or internally by SAX drivers.
 *
 * <p>This class encapsulates the logic of Namespace processing: it
 * tracks the declarations currently in force for each context and
 * automatically processes qualified XML names into their Namespace
 * parts; it can also be used in reverse for generating XML qnames
 * from Namespaces.</p>
 *
 * <p>Namespace support objects are reusable, but the reset method
 * must be invoked between each session.</p>
 *
 * <p>Here is a simple session:</p>
 *
 * <pre>
 * String parts[] = new String[3];
 * NamespaceSupport support = new NamespaceSupport();
 *
 * support.pushContext();
 * support.declarePrefix("", "http://www.w3.org/1999/xhtml");
 * support.declarePrefix("dc", "http://www.purl.org/dc#");
 *
 * parts = support.processName("p", parts, false);
 * System.out.println("Namespace URI: " + parts[0]);
 * System.out.println("Local name: " + parts[1]);
 * System.out.println("Raw name: " + parts[2]);
 *
 * parts = support.processName("dc:title", parts, false);
 * System.out.println("Namespace URI: " + parts[0]);
 * System.out.println("Local name: " + parts[1]);
 * System.out.println("Raw name: " + parts[2]);
 *
 * support.popContext();
 * </pre>
 *
 * <p>Note that this class is optimized for the use case where most
 * elements do not contain Namespace declarations: if the same
 * prefix/URI mapping is repeated for each context (for example), this
 * class will be somewhat less efficient.</p>
 *
 * <p>Although SAX drivers (parsers) may choose to use this class to
 * implement namespace handling, they are not required to do so.
 * Applications must track namespace information themselves if they
 * want to use namespace information.
 *
 * @since 1.4, SAX 2.0
 * @author David Megginson
 */
public class NamespaceSupport
{


    ////////////////////////////////////////////////////////////////////
    // Constants.
    ////////////////////////////////////////////////////////////////////


    /**
     * The XML Namespace URI as a constant.
     * The value is <code>http://www.w3.org/XML/1998/namespace</code>
     * as defined in the "Namespaces in XML" * recommendation.
     *
     * <p>This is the Namespace URI that is automatically mapped
     * to the "xml" prefix.</p>
     */
    public final static String XMLNS =
        "http://www.w3.org/XML/1998/namespace";


    /**
     * The namespace declaration URI as a constant.
     * The value is <code>http://www.w3.org/xmlns/2000/</code>, as defined
     * in a backwards-incompatible erratum to the "Namespaces in XML"
     * recommendation.  Because that erratum postdated SAX2, SAX2 defaults
     * to the original recommendation, and does not normally use this URI.
     *
     *
     * <p>This is the Namespace URI that is optionally applied to
     * <em>xmlns</em> and <em>xmlns:*</em> attributes, which are used to
     * declare namespaces.  </p>
     *
     * @since 1.5, SAX 2.1alpha
     * @see #setNamespaceDeclUris
     * @see #isNamespaceDeclUris
     */
    public final static String NSDECL =
        "http://www.w3.org/xmlns/2000/";


    /**
     * An empty enumeration.
     */
    private final static Enumeration<String> EMPTY_ENUMERATION =
            Collections.enumeration(new ArrayList<String>());


    ////////////////////////////////////////////////////////////////////
    // Constructor.
    ////////////////////////////////////////////////////////////////////


    /**
     * Create a new Namespace support object.
     */
    public NamespaceSupport ()
    {
        reset();
    }



    ////////////////////////////////////////////////////////////////////
    // Context management.
    ////////////////////////////////////////////////////////////////////


    /**
     * Reset this Namespace support object for reuse.
     *
     * <p>It is necessary to invoke this method before reusing the
     * Namespace support object for a new session.  If namespace
     * declaration URIs are to be supported, that flag must also
     * be set to a non-default value.
     * </p>
     *
     * @see #setNamespaceDeclUris
     */
    public void reset ()
    {
        contexts = new Context[32];
        namespaceDeclUris = false;
        contextPos = 0;
        contexts[contextPos] = currentContext = new Context();
        currentContext.declarePrefix("xml", XMLNS);
    }


    /**
     * Start a new Namespace context.
     * The new context will automatically inherit
     * the declarations of its parent context, but it will also keep
     * track of which declarations were made within this context.
     *
     * <p>Event callback code should start a new context once per element.
     * This means being ready to call this in either of two places.
     * For elements that don't include namespace declarations, the
     * <em>ContentHandler.startElement()</em> callback is the right place.
     * For elements with such a declaration, it'd done in the first
     * <em>ContentHandler.startPrefixMapping()</em> callback.
     * A boolean flag can be used to
     * track whether a context has been started yet.  When either of
     * those methods is called, it checks the flag to see if a new context
     * needs to be started.  If so, it starts the context and sets the
     * flag.  After <em>ContentHandler.startElement()</em>
     * does that, it always clears the flag.
     *
     * <p>Normally, SAX drivers would push a new context at the beginning
     * of each XML element.  Then they perform a first pass over the
     * attributes to process all namespace declarations, making
     * <em>ContentHandler.startPrefixMapping()</em> callbacks.
     * Then a second pass is made, to determine the namespace-qualified
     * names for all attributes and for the element name.
     * Finally all the information for the
     * <em>ContentHandler.startElement()</em> callback is available,
     * so it can then be made.
     *
     * <p>The Namespace support object always starts with a base context
     * already in force: in this context, only the "xml" prefix is
     * declared.</p>
     *
     * @see org.xml.sax.ContentHandler
     * @see #popContext
     */
    public void pushContext ()
    {
        int max = contexts.length;

        contextPos++;

                                // Extend the array if necessary
        if (contextPos >= max) {
            Context newContexts[] = new Context[max*2];
            System.arraycopy(contexts, 0, newContexts, 0, max);
            max *= 2;
            contexts = newContexts;
        }

                                // Allocate the context if necessary.
        currentContext = contexts[contextPos];
        if (currentContext == null) {
            contexts[contextPos] = currentContext = new Context();
        }

                                // Set the parent, if any.
        if (contextPos > 0) {
            currentContext.setParent(contexts[contextPos - 1]);
        }
    }


    /**
     * Revert to the previous Namespace context.
     *
     * <p>Normally, you should pop the context at the end of each
     * XML element.  After popping the context, all Namespace prefix
     * mappings that were previously in force are restored.</p>
     *
     * <p>You must not attempt to declare additional Namespace
     * prefixes after popping a context, unless you push another
     * context first.</p>
     *
     * @see #pushContext
     */
    public void popContext ()
    {
        contexts[contextPos].clear();
        contextPos--;
        if (contextPos < 0) {
            throw new EmptyStackException();
        }
        currentContext = contexts[contextPos];
    }



    ////////////////////////////////////////////////////////////////////
    // Operations within a context.
    ////////////////////////////////////////////////////////////////////


    /**
     * Declare a Namespace prefix.  All prefixes must be declared
     * before they are referenced.  For example, a SAX driver (parser)
     * would scan an element's attributes
     * in two passes:  first for namespace declarations,
     * then a second pass using {@link #processName processName()} to
     * interpret prefixes against (potentially redefined) prefixes.
     *
     * <p>This method declares a prefix in the current Namespace
     * context; the prefix will remain in force until this context
     * is popped, unless it is shadowed in a descendant context.</p>
     *
     * <p>To declare the default element Namespace, use the empty string as
     * the prefix.</p>
     *
     * <p>Note that there is an asymmetry in this library: {@link
     * #getPrefix getPrefix} will not return the "" prefix,
     * even if you have declared a default element namespace.
     * To check for a default namespace,
     * you have to look it up explicitly using {@link #getURI getURI}.
     * This asymmetry exists to make it easier to look up prefixes
     * for attribute names, where the default prefix is not allowed.</p>
     *
     * @param prefix The prefix to declare, or the empty string to
     *  indicate the default element namespace.  This may never have
     *  the value "xml" or "xmlns".
     * @param uri The Namespace URI to associate with the prefix.
     * @return true if the prefix was legal, false otherwise
     *
     * @see #processName
     * @see #getURI
     * @see #getPrefix
     */
    public boolean declarePrefix (String prefix, String uri)
    {
        if (prefix.equals("xml") || prefix.equals("xmlns")) {
            return false;
        } else {
            currentContext.declarePrefix(prefix, uri);
            return true;
        }
    }


    /**
     * Process a raw XML qualified name, after all declarations in the
     * current context have been handled by {@link #declarePrefix
     * declarePrefix()}.
     *
     * <p>This method processes a raw XML qualified name in the
     * current context by removing the prefix and looking it up among
     * the prefixes currently declared.  The return value will be the
     * array supplied by the caller, filled in as follows:</p>
     *
     * <dl>
     * <dt>parts[0]</dt>
     * <dd>The Namespace URI, or an empty string if none is
     *  in use.</dd>
     * <dt>parts[1]</dt>
     * <dd>The local name (without prefix).</dd>
     * <dt>parts[2]</dt>
     * <dd>The original raw name.</dd>
     * </dl>
     *
     * <p>All of the strings in the array will be internalized.  If
     * the raw name has a prefix that has not been declared, then
     * the return value will be null.</p>
     *
     * <p>Note that attribute names are processed differently than
     * element names: an unprefixed element name will receive the
     * default Namespace (if any), while an unprefixed attribute name
     * will not.</p>
     *
     * @param qName The XML qualified name to be processed.
     * @param parts An array supplied by the caller, capable of
     *        holding at least three members.
     * @param isAttribute A flag indicating whether this is an
     *        attribute name (true) or an element name (false).
     * @return The supplied array holding three internalized strings
     *        representing the Namespace URI (or empty string), the
     *        local name, and the XML qualified name; or null if there
     *        is an undeclared prefix.
     * @see #declarePrefix
     * @see java.lang.String#intern */
    public String [] processName (String qName, String parts[],
                                  boolean isAttribute)
    {
        String myParts[] = currentContext.processName(qName, isAttribute);
        if (myParts == null) {
            return null;
        } else {
            parts[0] = myParts[0];
            parts[1] = myParts[1];
            parts[2] = myParts[2];
            return parts;
        }
    }


    /**
     * Look up a prefix and get the currently-mapped Namespace URI.
     *
     * <p>This method looks up the prefix in the current context.
     * Use the empty string ("") for the default Namespace.</p>
     *
     * @param prefix The prefix to look up.
     * @return The associated Namespace URI, or null if the prefix
     *         is undeclared in this context.
     * @see #getPrefix
     * @see #getPrefixes
     */
    public String getURI (String prefix)
    {
        return currentContext.getURI(prefix);
    }


    /**
     * Return an enumeration of all prefixes whose declarations are
     * active in the current context.
     * This includes declarations from parent contexts that have
     * not been overridden.
     *
     * <p><strong>Note:</strong> if there is a default prefix, it will not be
     * returned in this enumeration; check for the default prefix
     * using the {@link #getURI getURI} with an argument of "".</p>
     *
     * @return An enumeration of prefixes (never empty).
     * @see #getDeclaredPrefixes
     * @see #getURI
     */
    public Enumeration<String> getPrefixes ()
    {
        return currentContext.getPrefixes();
    }


    /**
     * Return one of the prefixes mapped to a Namespace URI.
     *
     * <p>If more than one prefix is currently mapped to the same
     * URI, this method will make an arbitrary selection; if you
     * want all of the prefixes, use the {@link #getPrefixes}
     * method instead.</p>
     *
     * <p><strong>Note:</strong> this will never return the empty (default) prefix;
     * to check for a default prefix, use the {@link #getURI getURI}
     * method with an argument of "".</p>
     *
     * @param uri the namespace URI
     * @return one of the prefixes currently mapped to the URI supplied,
     *         or null if none is mapped or if the URI is assigned to
     *         the default namespace
     * @see #getPrefixes(java.lang.String)
     * @see #getURI
     */
    public String getPrefix (String uri)
    {
        return currentContext.getPrefix(uri);
    }


    /**
     * Return an enumeration of all prefixes for a given URI whose
     * declarations are active in the current context.
     * This includes declarations from parent contexts that have
     * not been overridden.
     *
     * <p>This method returns prefixes mapped to a specific Namespace
     * URI.  The xml: prefix will be included.  If you want only one
     * prefix that's mapped to the Namespace URI, and you don't care
     * which one you get, use the {@link #getPrefix getPrefix}
     *  method instead.</p>
     *
     * <p><strong>Note:</strong> the empty (default) prefix is <em>never</em> included
     * in this enumeration; to check for the presence of a default
     * Namespace, use the {@link #getURI getURI} method with an
     * argument of "".</p>
     *
     * @param uri The Namespace URI.
     * @return An enumeration of prefixes (never empty).
     * @see #getPrefix
     * @see #getDeclaredPrefixes
     * @see #getURI
     */
    public Enumeration<String> getPrefixes (String uri)
    {
        List<String> prefixes = new ArrayList<>();
        Enumeration<String> allPrefixes = getPrefixes();
        while (allPrefixes.hasMoreElements()) {
            String prefix = allPrefixes.nextElement();
            if (uri.equals(getURI(prefix))) {
                prefixes.add(prefix);
            }
        }
        return Collections.enumeration(prefixes);
    }


    /**
     * Return an enumeration of all prefixes declared in this context.
     *
     * <p>The empty (default) prefix will be included in this
     * enumeration; note that this behaviour differs from that of
     * {@link #getPrefix} and {@link #getPrefixes}.</p>
     *
     * @return An enumeration of all prefixes declared in this
     *         context.
     * @see #getPrefixes
     * @see #getURI
     */
    public Enumeration<String> getDeclaredPrefixes ()
    {
        return currentContext.getDeclaredPrefixes();
    }

    /**
     * Controls whether namespace declaration attributes are placed
     * into the {@link #NSDECL NSDECL} namespace
     * by {@link #processName processName()}.  This may only be
     * changed before any contexts have been pushed.
     *
     * @param value a flag indicating whether namespace declaration attributes
     * are placed into the {@link #NSDECL NSDECL} namespace
     * @since 1.5, SAX 2.1alpha
     *
     * @throws IllegalStateException when attempting to set this
     *  after any context has been pushed.
     */
    public void setNamespaceDeclUris (boolean value)
    {
        if (contextPos != 0)
            throw new IllegalStateException ();
        if (value == namespaceDeclUris)
            return;
        namespaceDeclUris = value;
        if (value)
            currentContext.declarePrefix ("xmlns", NSDECL);
        else {
            contexts[contextPos] = currentContext = new Context();
            currentContext.declarePrefix("xml", XMLNS);
        }
    }

    /**
     * Returns true if namespace declaration attributes are placed into
     * a namespace.  This behavior is not the default.
     *
     * @return true if namespace declaration attributes are placed into a namespace,
     * false otherwise
     * @since 1.5, SAX 2.1alpha
     */
    public boolean isNamespaceDeclUris ()
        { return namespaceDeclUris; }



    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    private Context contexts[];
    private Context currentContext;
    private int contextPos;
    private boolean namespaceDeclUris;


    ////////////////////////////////////////////////////////////////////
    // Internal classes.
    ////////////////////////////////////////////////////////////////////

    /**
     * Internal class for a single Namespace context.
     *
     * <p>This module caches and reuses Namespace contexts,
     * so the number allocated
     * will be equal to the element depth of the document, not to the total
     * number of elements (i.e. 5-10 rather than tens of thousands).
     * Also, data structures used to represent contexts are shared when
     * possible (child contexts without declarations) to further reduce
     * the amount of memory that's consumed.
     * </p>
     */
    final class Context {

        /**
         * Create the root-level Namespace context.
         */
        Context ()
        {
            copyTables();
        }


        /**
         * (Re)set the parent of this Namespace context.
         * The context must either have been freshly constructed,
         * or must have been cleared.
         *
         * @param context The parent Namespace context object.
         */
        void setParent (Context parent)
        {
            this.parent = parent;
            declarations = null;
            prefixTable = parent.prefixTable;
            uriTable = parent.uriTable;
            elementNameTable = parent.elementNameTable;
            attributeNameTable = parent.attributeNameTable;
            defaultNS = parent.defaultNS;
            declSeen = false;
        }

        /**
         * Makes associated state become collectible,
         * invalidating this context.
         * {@link #setParent} must be called before
         * this context may be used again.
         */
        void clear ()
        {
            parent = null;
            prefixTable = null;
            uriTable = null;
            elementNameTable = null;
            attributeNameTable = null;
            defaultNS = null;
        }


        /**
         * Declare a Namespace prefix for this context.
         *
         * @param prefix The prefix to declare.
         * @param uri The associated Namespace URI.
         * @see org.xml.sax.helpers.NamespaceSupport#declarePrefix
         */
        void declarePrefix (String prefix, String uri)
        {
                                // Lazy processing...
//          if (!declsOK)
//              throw new IllegalStateException (
//                  "can't declare any more prefixes in this context");
            if (!declSeen) {
                copyTables();
            }
            if (declarations == null) {
                declarations = new ArrayList<>();
            }

            prefix = prefix.intern();
            uri = uri.intern();
            if ("".equals(prefix)) {
                if ("".equals(uri)) {
                    defaultNS = null;
                } else {
                    defaultNS = uri;
                }
            } else {
                prefixTable.put(prefix, uri);
                uriTable.put(uri, prefix); // may wipe out another prefix
            }
            declarations.add(prefix);
        }


        /**
         * Process an XML qualified name in this context.
         *
         * @param qName The XML qualified name.
         * @param isAttribute true if this is an attribute name.
         * @return An array of three strings containing the
         *         URI part (or empty string), the local part,
         *         and the raw name, all internalized, or null
         *         if there is an undeclared prefix.
         * @see org.xml.sax.helpers.NamespaceSupport#processName
         */
        String [] processName (String qName, boolean isAttribute)
        {
            String name[];
            Map<String, String[]> table;

            // Select the appropriate table.
            if (isAttribute) {
                table = attributeNameTable;
            } else {
                table = elementNameTable;
            }

            // Start by looking in the cache, and
            // return immediately if the name
            // is already known in this content
            name = table.get(qName);
            if (name != null) {
                return name;
            }

            // We haven't seen this name in this
            // context before.  Maybe in the parent
            // context, but we can't assume prefix
            // bindings are the same.
            name = new String[3];
            name[2] = qName.intern();
            int index = qName.indexOf(':');


            // No prefix.
            if (index == -1) {
                if (isAttribute) {
                    if (qName == "xmlns" && namespaceDeclUris)
                        name[0] = NSDECL;
                    else
                        name[0] = "";
                } else if (defaultNS == null) {
                    name[0] = "";
                } else {
                    name[0] = defaultNS;
                }
                name[1] = name[2];
            }

            // Prefix
            else {
                String prefix = qName.substring(0, index);
                String local = qName.substring(index+1);
                String uri;
                if ("".equals(prefix)) {
                    uri = defaultNS;
                } else {
                    uri = prefixTable.get(prefix);
                }
                if (uri == null
                        || (!isAttribute && "xmlns".equals (prefix))) {
                    return null;
                }
                name[0] = uri;
                name[1] = local.intern();
            }

            // Save in the cache for future use.
            // (Could be shared with parent context...)
            table.put(name[2], name);
            return name;
        }


        /**
         * Look up the URI associated with a prefix in this context.
         *
         * @param prefix The prefix to look up.
         * @return The associated Namespace URI, or null if none is
         *         declared.
         * @see org.xml.sax.helpers.NamespaceSupport#getURI
         */
        String getURI (String prefix)
        {
            if ("".equals(prefix)) {
                return defaultNS;
            } else if (prefixTable == null) {
                return null;
            } else {
                return prefixTable.get(prefix);
            }
        }


        /**
         * Look up one of the prefixes associated with a URI in this context.
         *
         * <p>Since many prefixes may be mapped to the same URI,
         * the return value may be unreliable.</p>
         *
         * @param uri The URI to look up.
         * @return The associated prefix, or null if none is declared.
         * @see org.xml.sax.helpers.NamespaceSupport#getPrefix
         */
        String getPrefix (String uri)
        {
            if (uriTable == null) {
                return null;
            } else {
                return uriTable.get(uri);
            }
        }


        /**
         * Return an enumeration of prefixes declared in this context.
         *
         * @return An enumeration of prefixes (possibly empty).
         * @see org.xml.sax.helpers.NamespaceSupport#getDeclaredPrefixes
         */
        Enumeration<String> getDeclaredPrefixes ()
        {
            if (declarations == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(declarations);
            }
        }

        /**
         * Return an enumeration of all prefixes currently in force.
         *
         * <p>The default prefix, if in force, is <em>not</em>
         * returned, and will have to be checked for separately.</p>
         *
         * @return An enumeration of prefixes (never empty).
         * @see org.xml.sax.helpers.NamespaceSupport#getPrefixes
         */
        Enumeration<String> getPrefixes ()
        {
            if (prefixTable == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(prefixTable.keySet());
            }
        }



        ////////////////////////////////////////////////////////////////
        // Internal methods.
        ////////////////////////////////////////////////////////////////


        /**
         * Copy on write for the internal tables in this context.
         *
         * <p>This class is optimized for the normal case where most
         * elements do not contain Namespace declarations.</p>
         */
        private void copyTables ()
        {
            if (prefixTable != null) {
                prefixTable = new HashMap<>(prefixTable);
            } else {
                prefixTable = new HashMap<>();
            }
            if (uriTable != null) {
                uriTable = new HashMap<>(uriTable);
            } else {
                uriTable = new HashMap<>();
            }
            elementNameTable = new HashMap<>();
            attributeNameTable = new HashMap<>();
            declSeen = true;
        }



        ////////////////////////////////////////////////////////////////
        // Protected state.
        ////////////////////////////////////////////////////////////////

        Map<String, String> prefixTable;
        Map<String, String> uriTable;
        Map<String, String[]> elementNameTable;
        Map<String, String[]> attributeNameTable;
        String defaultNS = null;



        ////////////////////////////////////////////////////////////////
        // Internal state.
        ////////////////////////////////////////////////////////////////

        private List<String> declarations = null;
        private boolean declSeen = false;
        private Context parent = null;
    }
}

// end of NamespaceSupport.java
