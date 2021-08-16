/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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


import javax.naming.*;


/**
 * The Resolver class performs DNS client operations in support of DnsContext.
 *
 * <p> Every DnsName instance passed to or returned from a method of
 * this class should be fully-qualified and contain a root label (an
 * empty component at position 0).
 *
 * @author Scott Seligman
 */

class Resolver {

    private DnsClient dnsClient;
    private int timeout;                // initial timeout on UDP queries in ms
    private int retries;                // number of UDP retries


    /*
     * Constructs a new Resolver given its servers and timeout parameters.
     * Each server is of the form "server[:port]".
     * IPv6 literal host names include delimiting brackets.
     * There must be at least one server.
     * "timeout" is the initial timeout interval (in ms) for UDP queries,
     * and "retries" gives the number of retries per server.
     */
    Resolver(String[] servers, int timeout, int retries)
            throws NamingException {
        this.timeout = timeout;
        this.retries = retries;
        dnsClient = new DnsClient(servers, timeout, retries);
    }

    public void close() {
        dnsClient.close();
        dnsClient = null;
    }


    /*
     * Queries resource records of a particular class and type for a
     * given domain name.
     * Useful values of rrclass are ResourceRecord.[Q]CLASS_xxx.
     * Useful values of rrtype are ResourceRecord.[Q]TYPE_xxx.
     * If recursion is true, recursion is requested on the query.
     * If auth is true, only authoritative responses are accepted.
     */
    ResourceRecords query(DnsName fqdn, int rrclass, int rrtype,
                          boolean recursion, boolean auth)
            throws NamingException {
        return dnsClient.query(fqdn, rrclass, rrtype, recursion, auth);
    }

    /*
     * Queries all resource records of a zone given its domain name and class.
     * If recursion is true, recursion is requested on the query to find
     * the name server (and also on the zone transfer, but it won't matter).
     */
    ResourceRecords queryZone(DnsName zone, int rrclass, boolean recursion)
            throws NamingException {

        DnsClient cl =
            new DnsClient(findNameServers(zone, recursion), timeout, retries);
        try {
            return cl.queryZone(zone, rrclass, recursion);
        } finally {
            cl.close();
        }
    }

    /*
     * Finds the zone of a given domain name.  The method is to look
     * for the first SOA record on the path from the given domain to
     * the root.  This search may be partially bypassed if the zone's
     * SOA record is received in the authority section of a response.
     * If recursion is true, recursion is requested on any queries.
     */
    DnsName findZoneName(DnsName fqdn, int rrclass, boolean recursion)
            throws NamingException {

        fqdn = (DnsName) fqdn.clone();
        while (fqdn.size() > 1) {       // while below root
            ResourceRecords rrs = null;
            try {
                rrs = query(fqdn, rrclass, ResourceRecord.TYPE_SOA,
                            recursion, false);
            } catch (NameNotFoundException e) {
                throw e;
            } catch (NamingException e) {
                // Ignore error and keep searching up the tree.
            }
            if (rrs != null) {
                if (rrs.answer.size() > 0) {    // found zone's SOA
                    return fqdn;
                }
                // Look for an SOA record giving the zone's top node.
                for (int i = 0; i < rrs.authority.size(); i++) {
                    ResourceRecord rr = rrs.authority.elementAt(i);
                    if (rr.getType() == ResourceRecord.TYPE_SOA) {
                        DnsName zone = rr.getName();
                        if (fqdn.endsWith(zone)) {
                            return zone;
                        }
                    }
                }
            }
            fqdn.remove(fqdn.size() - 1);       // one step rootward
        }
        return fqdn;                    // no SOA found below root, so
                                        // return root
    }

    /*
     * Finds a zone's SOA record.  Returns null if no SOA is found (in
     * which case "zone" is not actually a zone).
     * If recursion is true, recursion is requested on the query.
     */
     ResourceRecord findSoa(DnsName zone, int rrclass, boolean recursion)
            throws NamingException {

        ResourceRecords rrs = query(zone, rrclass, ResourceRecord.TYPE_SOA,
                                    recursion, false);
        for (int i = 0; i < rrs.answer.size(); i++) {
            ResourceRecord rr = rrs.answer.elementAt(i);
            if (rr.getType() == ResourceRecord.TYPE_SOA) {
                return rr;
            }
        }
        return null;
    }

    /*
     * Finds the name servers of a zone.  {@code zone} is a fully-qualified
     * domain name at the top of a zone.
     * If recursion is true, recursion is requested on the query.
     */
    private String[] findNameServers(DnsName zone, boolean recursion)
            throws NamingException {

        // %%% As an optimization, could look in authority section of
        // findZoneName() response first.
        ResourceRecords rrs =
            query(zone, ResourceRecord.CLASS_INTERNET, ResourceRecord.TYPE_NS,
                  recursion, false);
        String[] ns = new String[rrs.answer.size()];
        for (int i = 0; i < ns.length; i++) {
            ResourceRecord rr = rrs.answer.elementAt(i);
            if (rr.getType() != ResourceRecord.TYPE_NS) {
                throw new CommunicationException("Corrupted DNS message");
            }
            ns[i] = (String) rr.getRdata();

            // Server name will be passed to InetAddress.getByName(), which
            // may not be able to handle a trailing dot.
            // assert ns[i].endsWith(".");
            ns[i] = ns[i].substring(0, ns[i].length() - 1);
        }
        return ns;
    }
}
