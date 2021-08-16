/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.dns;


import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.DirectoryManager;

import com.sun.jndi.toolkit.ctx.*;


/**
 * A DnsContext is a directory context representing a DNS node.
 *
 * @author Scott Seligman
 */


public class DnsContext extends ComponentDirContext {

    DnsName domain;             // fully-qualified domain name of this context,
                                // with a root (empty) label at position 0
    Hashtable<Object,Object> environment;
    private boolean envShared;  // true if environment is possibly shared
                                // and so must be copied on write
    private boolean parentIsDns;        // was this DnsContext created by
                                        // another?  see composeName()
    private String[] servers;
    private Resolver resolver;

    private boolean authoritative;      // must all responses be authoritative?
    private boolean recursion;          // request recursion on queries?
    private int timeout;                // initial timeout on UDP queries in ms
    private int retries;                // number of UDP retries

    static final NameParser nameParser = new DnsNameParser();

    // Timeouts for UDP queries use exponential backoff:  each retry
    // is for twice as long as the last.  The following constants set
    // the defaults for the initial timeout (in ms) and the number of
    // retries, and name the environment properties used to override
    // these defaults.
    private static final int DEFAULT_INIT_TIMEOUT = 1000;
    private static final int DEFAULT_RETRIES = 4;
    private static final String INIT_TIMEOUT =
                                          "com.sun.jndi.dns.timeout.initial";
    private static final String RETRIES = "com.sun.jndi.dns.timeout.retries";

    // The resource record type and class to use for lookups, and the
    // property used to modify them
    private CT lookupCT;
    private static final String LOOKUP_ATTR = "com.sun.jndi.dns.lookup.attr";

    // Property used to disallow recursion on queries
    private static final String RECURSION = "com.sun.jndi.dns.recursion";

    // ANY == ResourceRecord.QCLASS_STAR == ResourceRecord.QTYPE_STAR
    private static final int ANY = ResourceRecord.QTYPE_STAR;

    // The zone tree used for list operations
    private static final ZoneNode zoneTree = new ZoneNode(null);


    /**
     * Returns a DNS context for a given domain and servers.
     * Each server is of the form "server[:port]".
     * IPv6 literal host names include delimiting brackets.
     * There must be at least one server.
     * The environment must not be null; it is cloned before being stored.
     */
    @SuppressWarnings("unchecked")
    public DnsContext(String domain, String[] servers, Hashtable<?,?> environment)
            throws NamingException {

        this.domain = new DnsName(domain.endsWith(".")
                                  ? domain
                                  : domain + ".");
        this.servers = (servers == null) ? null : servers.clone();
        this.environment = (Hashtable<Object,Object>) environment.clone();
        envShared = false;
        parentIsDns = false;
        resolver = null;

        initFromEnvironment();
    }

    /*
     * Returns a clone of a DNS context, just like DnsContext(DnsContext)
     * but with a different domain name and with parentIsDns set to true.
     */
    DnsContext(DnsContext ctx, DnsName domain) {
        this(ctx);
        this.domain = domain;
        parentIsDns = true;
    }

    /*
     * Returns a clone of a DNS context.  The context's modifiable
     * private state is independent of the original's (so closing one
     * context, for example, won't close the other).  The two contexts
     * share {@code environment}, but it's copy-on-write so there's
     * no conflict.
     */
    private DnsContext(DnsContext ctx) {
        environment = ctx.environment;  // shared environment, copy-on-write
        envShared = ctx.envShared = true;
        parentIsDns = ctx.parentIsDns;
        domain = ctx.domain;
        servers = ctx.servers;          // shared servers, no write operation
        resolver = ctx.resolver;
        authoritative = ctx.authoritative;
        recursion = ctx.recursion;
        timeout = ctx.timeout;
        retries = ctx.retries;
        lookupCT = ctx.lookupCT;
    }

    public void close() {
        if (resolver != null) {
            resolver.close();
            resolver = null;
        }
    }


    //---------- Environment operations

    /*
     * Override default with a noncloning version.
     */
    protected Hashtable<?,?> p_getEnvironment() {
        return environment;
    }

    public Hashtable<?,?> getEnvironment() throws NamingException {
        return (Hashtable<?,?>) environment.clone();
    }

    @SuppressWarnings("unchecked")
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {

        if (propName.equals(LOOKUP_ATTR)) {
            lookupCT = getLookupCT((String) propVal);
        } else if (propName.equals(Context.AUTHORITATIVE)) {
            authoritative = "true".equalsIgnoreCase((String) propVal);
        } else if (propName.equals(RECURSION)) {
            recursion = "true".equalsIgnoreCase((String) propVal);
        } else if (propName.equals(INIT_TIMEOUT)) {
            int val = Integer.parseInt((String) propVal);
            if (timeout != val) {
                timeout = val;
                resolver = null;
            }
        } else if (propName.equals(RETRIES)) {
            int val = Integer.parseInt((String) propVal);
            if (retries != val) {
                retries = val;
                resolver = null;
            }
        }

        if (!envShared) {
            return environment.put(propName, propVal);
        } else if (environment.get(propName) != propVal) {
            // copy on write
            environment = (Hashtable<Object,Object>) environment.clone();
            envShared = false;
            return environment.put(propName, propVal);
        } else {
            return propVal;
        }
    }

    @SuppressWarnings("unchecked")
    public Object removeFromEnvironment(String propName)
            throws NamingException {

        if (propName.equals(LOOKUP_ATTR)) {
            lookupCT = getLookupCT(null);
        } else if (propName.equals(Context.AUTHORITATIVE)) {
            authoritative = false;
        } else if (propName.equals(RECURSION)) {
            recursion = true;
        } else if (propName.equals(INIT_TIMEOUT)) {
            if (timeout != DEFAULT_INIT_TIMEOUT) {
                timeout = DEFAULT_INIT_TIMEOUT;
                resolver = null;
            }
        } else if (propName.equals(RETRIES)) {
            if (retries != DEFAULT_RETRIES) {
                retries = DEFAULT_RETRIES;
                resolver = null;
            }
        }

        if (!envShared) {
            return environment.remove(propName);
        } else if (environment.get(propName) != null) {
            // copy-on-write
            environment = (Hashtable<Object,Object>) environment.clone();
            envShared = false;
            return environment.remove(propName);
        } else {
            return null;
        }
    }

    /*
     * Update PROVIDER_URL property.  Call this only when environment
     * is not being shared.
     */
    void setProviderUrl(String url) {
        // assert !envShared;
        environment.put(Context.PROVIDER_URL, url);
    }

    /*
     * Read environment properties and set parameters.
     */
    private void initFromEnvironment()
            throws InvalidAttributeIdentifierException {

        lookupCT = getLookupCT((String) environment.get(LOOKUP_ATTR));
        authoritative = "true".equalsIgnoreCase((String)
                                       environment.get(Context.AUTHORITATIVE));
        String val = (String) environment.get(RECURSION);
        recursion = ((val == null) ||
                     "true".equalsIgnoreCase(val));
        val = (String) environment.get(INIT_TIMEOUT);
        timeout = (val == null)
            ? DEFAULT_INIT_TIMEOUT
            : Integer.parseInt(val);
        val = (String) environment.get(RETRIES);
        retries = (val == null)
            ? DEFAULT_RETRIES
            : Integer.parseInt(val);
    }

    private CT getLookupCT(String attrId)
            throws InvalidAttributeIdentifierException {
        return (attrId == null)
            ? new CT(ResourceRecord.CLASS_INTERNET, ResourceRecord.TYPE_TXT)
            : fromAttrId(attrId);
    }


    //---------- Naming operations

    public Object c_lookup(Name name, Continuation cont)
            throws NamingException {

        cont.setSuccess();
        if (name.isEmpty()) {
            DnsContext ctx = new DnsContext(this);
            ctx.resolver = new Resolver(servers, timeout, retries);
                                                // clone for parallelism
            return ctx;
        }
        try {
            DnsName fqdn = fullyQualify(name);
            ResourceRecords rrs =
                getResolver().query(fqdn, lookupCT.rrclass, lookupCT.rrtype,
                                    recursion, authoritative);
            Attributes attrs = rrsToAttrs(rrs, null);
            DnsContext ctx = new DnsContext(this, fqdn);
            return DirectoryManager.getObjectInstance(ctx, name, this,
                                                      environment, attrs);
        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        } catch (Exception e) {
            cont.setError(this, name);
            NamingException ne = new NamingException(
                    "Problem generating object using object factory");
            ne.setRootCause(e);
            throw cont.fillInException(ne);
        }
    }

    public Object c_lookupLink(Name name, Continuation cont)
            throws NamingException {
        return c_lookup(name, cont);
    }

    public NamingEnumeration<NameClassPair> c_list(Name name, Continuation cont)
            throws NamingException {
        cont.setSuccess();
        try {
            DnsName fqdn = fullyQualify(name);
            NameNode nnode = getNameNode(fqdn);
            DnsContext ctx = new DnsContext(this, fqdn);
            return new NameClassPairEnumeration(ctx, nnode.getChildren());

        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
    }

    public NamingEnumeration<Binding> c_listBindings(Name name, Continuation cont)
            throws NamingException {
        cont.setSuccess();
        try {
            DnsName fqdn = fullyQualify(name);
            NameNode nnode = getNameNode(fqdn);
            DnsContext ctx = new DnsContext(this, fqdn);
            return new BindingEnumeration(ctx, nnode.getChildren());

        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
    }

    public void c_bind(Name name, Object obj, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_rebind(Name name, Object obj, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_unbind(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_rename(Name oldname, Name newname, Continuation cont)
            throws NamingException {
        cont.setError(this, oldname);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public Context c_createSubcontext(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_destroySubcontext(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public NameParser c_getNameParser(Name name, Continuation cont)
            throws NamingException {
        cont.setSuccess();
        return nameParser;
    }


    //---------- Directory operations

    public void c_bind(Name name,
                       Object obj,
                       Attributes attrs,
                       Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_rebind(Name name,
                         Object obj,
                         Attributes attrs,
                         Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public DirContext c_createSubcontext(Name name,
                                         Attributes attrs,
                                         Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public Attributes c_getAttributes(Name name,
                                      String[] attrIds,
                                      Continuation cont)
            throws NamingException {

        cont.setSuccess();
        try {
            DnsName fqdn = fullyQualify(name);
            CT[] cts = attrIdsToClassesAndTypes(attrIds);
            CT ct = getClassAndTypeToQuery(cts);
            ResourceRecords rrs =
                getResolver().query(fqdn, ct.rrclass, ct.rrtype,
                                    recursion, authoritative);
            return rrsToAttrs(rrs, cts);

        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
    }

    public void c_modifyAttributes(Name name,
                                   int mod_op,
                                   Attributes attrs,
                                   Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public void c_modifyAttributes(Name name,
                                   ModificationItem[] mods,
                                   Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public NamingEnumeration<SearchResult> c_search(Name name,
                                      Attributes matchingAttributes,
                                      String[] attributesToReturn,
                                      Continuation cont)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<SearchResult> c_search(Name name,
                                      String filter,
                                      SearchControls cons,
                                      Continuation cont)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    public NamingEnumeration<SearchResult> c_search(Name name,
                                      String filterExpr,
                                      Object[] filterArgs,
                                      SearchControls cons,
                                      Continuation cont)
            throws NamingException {
        throw new OperationNotSupportedException();
    }

    public DirContext c_getSchema(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }

    public DirContext c_getSchemaClassDefinition(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        throw cont.fillInException(
                new OperationNotSupportedException());
    }


    //---------- Name-related operations

    public String getNameInNamespace() {
        return domain.toString();
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result;

        // Any name that's not a CompositeName is assumed to be a DNS
        // compound name.  Convert each to a DnsName for syntax checking.
        if (!(prefix instanceof DnsName || prefix instanceof CompositeName)) {
            prefix = (new DnsName()).addAll(prefix);
        }
        if (!(name instanceof DnsName || name instanceof CompositeName)) {
            name = (new DnsName()).addAll(name);
        }

        // Each of prefix and name is now either a DnsName or a CompositeName.

        // If we have two DnsNames, simply join them together.
        if ((prefix instanceof DnsName) && (name instanceof DnsName)) {
            result = (DnsName) (prefix.clone());
            result.addAll(name);
            return new CompositeName().add(result.toString());
        }

        // Wrap compound names in composite names.
        Name prefixC = (prefix instanceof CompositeName)
            ? prefix
            : new CompositeName().add(prefix.toString());
        Name nameC = (name instanceof CompositeName)
            ? name
            : new CompositeName().add(name.toString());
        int prefixLast = prefixC.size() - 1;

        // Let toolkit do the work at namespace boundaries.
        if (nameC.isEmpty() || nameC.get(0).isEmpty() ||
                prefixC.isEmpty() || prefixC.get(prefixLast).isEmpty()) {
            return super.composeName(nameC, prefixC);
        }

        result = (prefix == prefixC)
            ? (CompositeName) prefixC.clone()
            : prefixC;                  // prefixC is already a clone
        result.addAll(nameC);

        if (parentIsDns) {
            DnsName dnsComp = (prefix instanceof DnsName)
                           ? (DnsName) prefix.clone()
                           : new DnsName(prefixC.get(prefixLast));
            dnsComp.addAll((name instanceof DnsName)
                           ? name
                           : new DnsName(nameC.get(0)));
            result.remove(prefixLast + 1);
            result.remove(prefixLast);
            result.add(prefixLast, dnsComp.toString());
        }
        return result;
    }


    //---------- Helper methods

    /*
     * Resolver is not created until needed, to allow time for updates
     * to the environment.
     */
    private synchronized Resolver getResolver() throws NamingException {
        if (resolver == null) {
            resolver = new Resolver(servers, timeout, retries);
        }
        return resolver;
    }

    /*
     * Returns the fully-qualified domain name of a name given
     * relative to this context.  Result includes a root label (an
     * empty component at position 0).
     */
    DnsName fullyQualify(Name name) throws NamingException {
        if (name.isEmpty()) {
            return domain;
        }
        DnsName dnsName = (name instanceof CompositeName)
            ? new DnsName(name.get(0))                  // parse name
            : (DnsName) (new DnsName()).addAll(name);   // clone & check syntax

        if (dnsName.hasRootLabel()) {
            // Be overly generous and allow root label if we're in root domain.
            if (domain.size() == 1) {
                return dnsName;
            } else {
                throw new InvalidNameException(
                       "DNS name " + dnsName + " not relative to " + domain);
            }
        }
        return (DnsName) dnsName.addAll(0, domain);
    }

    /*
     * Converts resource records to an attribute set.  Only resource
     * records in the answer section are used, and only those that
     * match the classes and types in cts (see classAndTypeMatch()
     * for matching rules).
     */
    private static Attributes rrsToAttrs(ResourceRecords rrs, CT[] cts) {

        BasicAttributes attrs = new BasicAttributes(true);

        for (int i = 0; i < rrs.answer.size(); i++) {
            ResourceRecord rr = rrs.answer.elementAt(i);
            int rrtype  = rr.getType();
            int rrclass = rr.getRrclass();

            if (!classAndTypeMatch(rrclass, rrtype, cts)) {
                continue;
            }

            String attrId = toAttrId(rrclass, rrtype);
            Attribute attr = attrs.get(attrId);
            if (attr == null) {
                attr = new BasicAttribute(attrId);
                attrs.put(attr);
            }
            attr.add(rr.getRdata());
        }
        return attrs;
    }

    /*
     * Returns true if rrclass and rrtype match some element of cts.
     * A match occurs if corresponding classes and types are equal,
     * or if the array value is ANY.  If cts is null, then any class
     * and type match.
     */
    private static boolean classAndTypeMatch(int rrclass, int rrtype,
                                             CT[] cts) {
        if (cts == null) {
            return true;
        }
        for (int i = 0; i < cts.length; i++) {
            CT ct = cts[i];
            boolean classMatch = (ct.rrclass == ANY) ||
                                 (ct.rrclass == rrclass);
            boolean typeMatch  = (ct.rrtype == ANY) ||
                                 (ct.rrtype == rrtype);
            if (classMatch && typeMatch) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns the attribute ID for a resource record given its class
     * and type.  If the record is in the internet class, the
     * corresponding attribute ID is the record's type name (or the
     * integer type value if the name is not known).  If the record is
     * not in the internet class, the class name (or integer class
     * value) is prepended to the attribute ID, separated by a space.
     *
     * A class or type value of ANY represents an indeterminate class
     * or type, and is represented within the attribute ID by "*".
     * For example, the attribute ID "IN *" represents
     * any type in the internet class, and "* NS" represents an NS
     * record of any class.
     */
    private static String toAttrId(int rrclass, int rrtype) {
        String attrId = ResourceRecord.getTypeName(rrtype);
        if (rrclass != ResourceRecord.CLASS_INTERNET) {
            attrId = ResourceRecord.getRrclassName(rrclass) + " " + attrId;
        }
        return attrId;
    }

    /*
     * Returns the class and type values corresponding to an attribute
     * ID.  An indeterminate class or type is represented by ANY.  See
     * toAttrId() for the format of attribute IDs.
     *
     * @throws InvalidAttributeIdentifierException
     *          if class or type is unknown
     */
    private static CT fromAttrId(String attrId)
            throws InvalidAttributeIdentifierException {

        if (attrId.isEmpty()) {
            throw new InvalidAttributeIdentifierException(
                    "Attribute ID cannot be empty");
        }
        int rrclass;
        int rrtype;
        int space = attrId.indexOf(' ');

        // class
        if (space < 0) {
            rrclass = ResourceRecord.CLASS_INTERNET;
        } else {
            String className = attrId.substring(0, space);
            rrclass = ResourceRecord.getRrclass(className);
            if (rrclass < 0) {
                throw new InvalidAttributeIdentifierException(
                        "Unknown resource record class '" + className + '\'');
            }
        }

        // type
        String typeName = attrId.substring(space + 1);
        rrtype = ResourceRecord.getType(typeName);
        if (rrtype < 0) {
            throw new InvalidAttributeIdentifierException(
                    "Unknown resource record type '" + typeName + '\'');
        }

        return new CT(rrclass, rrtype);
    }

    /*
     * Returns an array of the classes and types corresponding to a
     * set of attribute IDs.  See toAttrId() for the format of
     * attribute IDs, and classAndTypeMatch() for the format of the
     * array returned.
     */
    private static CT[] attrIdsToClassesAndTypes(String[] attrIds)
            throws InvalidAttributeIdentifierException {
        if (attrIds == null) {
            return null;
        }
        CT[] cts = new CT[attrIds.length];

        for (int i = 0; i < attrIds.length; i++) {
            cts[i] = fromAttrId(attrIds[i]);
        }
        return cts;
    }

    /*
     * Returns the most restrictive resource record class and type
     * that may be used to query for records matching cts.
     * See classAndTypeMatch() for matching rules.
     */
    private static CT getClassAndTypeToQuery(CT[] cts) {
        int rrclass;
        int rrtype;

        if (cts == null) {
            // Query all records.
            rrclass = ANY;
            rrtype  = ANY;
        } else if (cts.length == 0) {
            // No records are requested, but we need to ask for something.
            rrclass = ResourceRecord.CLASS_INTERNET;
            rrtype  = ANY;
        } else {
            rrclass = cts[0].rrclass;
            rrtype  = cts[0].rrtype;
            for (int i = 1; i < cts.length; i++) {
                if (rrclass != cts[i].rrclass) {
                    rrclass = ANY;
                }
                if (rrtype != cts[i].rrtype) {
                    rrtype = ANY;
                }
            }
        }
        return new CT(rrclass, rrtype);
    }


    //---------- Support for list operations

    /*
     * Synchronization notes:
     *
     * Any access to zoneTree that walks the tree, whether it modifies
     * the tree or not, is synchronized on zoneTree.
     * [%%% Note:  a read/write lock would allow increased concurrency.]
     * The depth of a ZoneNode can thereafter be accessed without
     * further synchronization.  Access to other fields and methods
     * should be synchronized on the node itself.
     *
     * A zone's contents is a NameNode tree that, once created, is never
     * modified.  The only synchronization needed is to ensure that it
     * gets flushed into shared memory after being created, which is
     * accomplished by ZoneNode.populate().  The contents are accessed
     * via a soft reference, so a ZoneNode may be seen to be populated
     * one moment and unpopulated the next.
     */

    /*
     * Returns the node in the zone tree corresponding to a
     * fully-qualified domain name.  If the desired portion of the
     * tree has not yet been populated or has been outdated, a zone
     * transfer is done to populate the tree.
     */
    private NameNode getNameNode(DnsName fqdn) throws NamingException {
        dprint("getNameNode(" + fqdn + ")");

        // Find deepest related zone in zone tree.
        ZoneNode znode;
        DnsName zone;
        synchronized (zoneTree) {
            znode = zoneTree.getDeepestPopulated(fqdn);
        }
        dprint("Deepest related zone in zone tree: " +
               ((znode != null) ? znode.getLabel() : "[none]"));

        NameNode topOfZone;
        NameNode nnode;

        if (znode != null) {
            synchronized (znode) {
                topOfZone = znode.getContents();
            }
            // If fqdn is in znode's zone, is not at a zone cut, and
            // is current, we're done.
            if (topOfZone != null) {
                nnode = topOfZone.get(fqdn, znode.depth() + 1); // +1 for root

                if ((nnode != null) && !nnode.isZoneCut()) {
                    dprint("Found node " + fqdn + " in zone tree");
                    zone = (DnsName)
                        fqdn.getPrefix(znode.depth() + 1);      // +1 for root
                    boolean current = isZoneCurrent(znode, zone);
                    boolean restart = false;

                    synchronized (znode) {
                        if (topOfZone != znode.getContents()) {
                            // Zone was modified while we were examining it.
                            // All bets are off.
                            restart = true;
                        } else if (!current) {
                            znode.depopulate();
                        } else {
                            return nnode;                       // cache hit!
                        }
                    }
                    dprint("Zone not current; discarding node");
                    if (restart) {
                        return getNameNode(fqdn);
                    }
                }
            }
        }

        // Cache miss...  do it the expensive way.
        dprint("Adding node " + fqdn + " to zone tree");

        // Find fqdn's zone and add it to the tree.
        zone = getResolver().findZoneName(fqdn, ResourceRecord.CLASS_INTERNET,
                                          recursion);
        dprint("Node's zone is " + zone);
        synchronized (zoneTree) {
            znode = (ZoneNode) zoneTree.add(zone, 1);   // "1" to skip root
        }

        // If znode is now populated we know -- because the first half of
        // getNodeName() didn't find it -- that it was populated by another
        // thread during this method call.  Assume then that it's current.

        synchronized (znode) {
            topOfZone = znode.isPopulated()
                ? znode.getContents()
                : populateZone(znode, zone);
        }
        // Desired node should now be in znode's populated zone.  Find it.
        nnode = topOfZone.get(fqdn, zone.size());
        if (nnode == null) {
            throw new ConfigurationException(
                    "DNS error: node not found in its own zone");
        }
        dprint("Found node in newly-populated zone");
        return nnode;
    }

    /*
     * Does a zone transfer to [re]populate a zone in the zone tree.
     * Returns the zone's new contents.
     */
    private NameNode populateZone(ZoneNode znode, DnsName zone)
            throws NamingException {
        dprint("Populating zone " + zone);
        // assert Thread.holdsLock(znode);
        ResourceRecords rrs =
            getResolver().queryZone(zone,
                                    ResourceRecord.CLASS_INTERNET, recursion);
        dprint("zone xfer complete: " + rrs.answer.size() + " records");
        return znode.populate(zone, rrs);
    }

    /*
     * Determine if a ZoneNode's data is current.
     * We base this on a comparison between the cached serial
     * number and the latest SOA record.
     *
     * If there is no SOA record, znode is not (or is no longer) a zone:
     * depopulate znode and return false.
     *
     * Since this method may perform a network operation, it is best
     * to call it with znode unlocked.  Caller must then note that the
     * result may be outdated by the time this method returns.
     */
    private boolean isZoneCurrent(ZoneNode znode, DnsName zone)
            throws NamingException {
        // former version:  return !znode.isExpired();

        if (!znode.isPopulated()) {
            return false;
        }
        ResourceRecord soa =
            getResolver().findSoa(zone, ResourceRecord.CLASS_INTERNET,
                                  recursion);
        synchronized (znode) {
            if (soa == null) {
                znode.depopulate();
            }
            return (znode.isPopulated() &&
                    znode.compareSerialNumberTo(soa) >= 0);
        }
    }


    //---------- Debugging

    private static final boolean debug = false;

    private static final void dprint(String msg) {
        if (debug) {
            System.err.println("** " + msg);
        }
    }
}


//----------

/*
 * A pairing of a resource record class and a resource record type.
 * A value of ANY in either field represents an indeterminate value.
 */
class CT {
    int rrclass;
    int rrtype;

    CT(int rrclass, int rrtype) {
        this.rrclass = rrclass;
        this.rrtype = rrtype;
    }
}


//----------

/*
 * Common base class for NameClassPairEnumeration and BindingEnumeration.
 */
abstract class BaseNameClassPairEnumeration<T> implements NamingEnumeration<T> {

    protected Enumeration<NameNode> nodes;    // nodes to be enumerated, or null if none
    protected DnsContext ctx;       // context being enumerated

    BaseNameClassPairEnumeration(DnsContext ctx, Hashtable<String,NameNode> nodes) {
        this.ctx = ctx;
        this.nodes = (nodes != null)
            ? nodes.elements()
            : null;
    }

    /*
     * ctx will be set to null when no longer needed by the enumeration.
     */
    public final void close() {
        nodes = null;
        ctx = null;
    }

    public final boolean hasMore() {
        boolean more = ((nodes != null) && nodes.hasMoreElements());
        if (!more) {
            close();
        }
        return more;
    }

    public final boolean hasMoreElements() {
        return hasMore();
    }

    abstract public T next() throws NamingException;

    public final T nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            java.util.NoSuchElementException nsee =
                    new java.util.NoSuchElementException();
            nsee.initCause(e);
            throw nsee;
        }
    }
}

/*
 * An enumeration of name/classname pairs.
 *
 * Nodes that have children or that are zone cuts are returned with
 * classname DirContext.  Other nodes are returned with classname
 * Object even though they are DirContexts as well, since this might
 * make the namespace easier to browse.
 */
final class NameClassPairEnumeration
        extends BaseNameClassPairEnumeration<NameClassPair>
        implements NamingEnumeration<NameClassPair> {

    NameClassPairEnumeration(DnsContext ctx, Hashtable<String,NameNode> nodes) {
        super(ctx, nodes);
    }

    @Override
    public NameClassPair next() throws NamingException {
        if (!hasMore()) {
            throw new java.util.NoSuchElementException();
        }
        NameNode nnode = nodes.nextElement();
        String className = (nnode.isZoneCut() ||
                            (nnode.getChildren() != null))
            ? "javax.naming.directory.DirContext"
            : "java.lang.Object";

        String label = nnode.getLabel();
        Name compName = (new DnsName()).add(label);
        Name cname = (new CompositeName()).add(compName.toString());

        NameClassPair ncp = new NameClassPair(cname.toString(), className);
        ncp.setNameInNamespace(ctx.fullyQualify(cname).toString());
        return ncp;
    }
}

/*
 * An enumeration of Bindings.
 */
final class BindingEnumeration extends BaseNameClassPairEnumeration<Binding>
                         implements NamingEnumeration<Binding> {

    BindingEnumeration(DnsContext ctx, Hashtable<String,NameNode> nodes) {
        super(ctx, nodes);
    }

    // Finalizer not needed since it's safe to leave ctx unclosed.
//  protected void finalize() {
//      close();
//  }

    @Override
    public Binding next() throws NamingException {
        if (!hasMore()) {
            throw (new java.util.NoSuchElementException());
        }
        NameNode nnode = nodes.nextElement();

        String label = nnode.getLabel();
        Name compName = (new DnsName()).add(label);
        String compNameStr = compName.toString();
        Name cname = (new CompositeName()).add(compNameStr);
        String cnameStr = cname.toString();

        DnsName fqdn = ctx.fullyQualify(compName);

        // Clone ctx to create the child context.
        DnsContext child = new DnsContext(ctx, fqdn);

        try {
            Object obj = DirectoryManager.getObjectInstance(
                    child, cname, ctx, child.environment, null);
            Binding binding = new Binding(cnameStr, obj);
            binding.setNameInNamespace(ctx.fullyQualify(cname).toString());
            return binding;
        } catch (Exception e) {
            NamingException ne = new NamingException(
                    "Problem generating object using object factory");
            ne.setRootCause(e);
            throw ne;
        }
    }
}
