/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.NamingManager;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * This class discovers the location of LDAP services by querying DNS.
 * See http://www.ietf.org/internet-drafts/draft-ietf-ldapext-locate-07.txt
 */

class ServiceLocator {

    private static final String SRV_RR = "SRV";

    private static final String[] SRV_RR_ATTR = new String[]{SRV_RR};

    private static final Random random = new Random();

    private ServiceLocator() {
    }

    /**
     * Maps a distinguished name (RFC 2253) to a fully qualified domain name.
     * Processes a sequence of RDNs having a DC attribute.
     * The special RDN "DC=." denotes the root of the domain tree.
     * Multi-valued RDNs, non-DC attributes, binary-valued attributes and the
     * RDN "DC=." all reset the domain name and processing continues.
     *
     * @param dn A string distinguished name (RFC 2253).
     * @return A domain name or null if none can be derived.
     * @throws InvalidNameException If the distinguished name is invalid.
     */
    static String mapDnToDomainName(String dn) throws InvalidNameException {
        if (dn == null) {
            return null;
        }
        StringBuilder domain = new StringBuilder();
        LdapName ldapName = new LdapName(dn);

        // process RDNs left-to-right
        //List<Rdn> rdnList = ldapName.getRdns();

        List<Rdn> rdnList = ldapName.getRdns();
        for (int i = rdnList.size() - 1; i >= 0; i--) {
            //Rdn rdn = rdnList.get(i);
            Rdn rdn = rdnList.get(i);

            // single-valued RDN with a DC attribute
            if ((rdn.size() == 1) &&
                ("dc".equalsIgnoreCase(rdn.getType()) )) {
                Object attrval = rdn.getValue();
                if (attrval instanceof String) {
                    if (attrval.equals(".") ||
                        (domain.length() == 1 && domain.charAt(0) == '.')) {
                        domain.setLength(0); // reset (when current or previous
                                             //        RDN value is "DC=.")
                    }
                    if (domain.length() > 0) {
                        domain.append('.');
                    }
                    domain.append(attrval);
                } else {
                    domain.setLength(0); // reset (when binary-valued attribute)
                }
            } else {
                domain.setLength(0); // reset (when multi-valued RDN or non-DC)
            }
        }
        return (domain.length() != 0) ? domain.toString() : null;
    }

    /**
     * Locates the LDAP service for a given domain.
     * Queries DNS for a list of LDAP Service Location Records (SRV) for a
     * given domain name.
     *
     * @param domainName A string domain name.
     * @param environment The possibly null environment of the context.
     * @return An ordered list of hostports for the LDAP service or null if
     *         the service has not been located.
     */
    static String[] getLdapService(String domainName, Map<?,?> environment) {
        if (environment instanceof Hashtable) {
            return getLdapService(domainName, (Hashtable)environment);
        }
        return getLdapService(domainName, new Hashtable<>(environment));
    }

    /**
     * Locates the LDAP service for a given domain.
     * Queries DNS for a list of LDAP Service Location Records (SRV) for a
     * given domain name.
     *
     * @param domainName A string domain name.
     * @param environment The possibly null environment of the context.
     * @return An ordered list of hostports for the LDAP service or null if
     *         the service has not been located.
     */
    static String[] getLdapService(String domainName, Hashtable<?,?> environment) {

        if (domainName == null || domainName.length() == 0) {
            return null;
        }

        String dnsUrl = "dns:///_ldap._tcp." + domainName;
        String[] hostports = null;

        try {
            // Create the DNS context using NamingManager rather than using
            // the initial context constructor. This avoids having the initial
            // context constructor call itself (when processing the URL
            // argument in the getAttributes call).
            Context ctx = NamingManager.getURLContext("dns", environment);
            if (!(ctx instanceof DirContext)) {
                return null; // cannot create a DNS context
            }
            Attributes attrs =
                ((DirContext)ctx).getAttributes(dnsUrl, SRV_RR_ATTR);
            Attribute attr;

            if (attrs != null && ((attr = attrs.get(SRV_RR)) != null)) {
                int numValues = attr.size();
                int numRecords = 0;
                SrvRecord[] srvRecords = new SrvRecord[numValues];

                // create the service records
                int i = 0;
                int j = 0;
                while (i < numValues) {
                    try {
                        srvRecords[j] = new SrvRecord((String) attr.get(i));
                        j++;
                    } catch (Exception e) {
                        // ignore bad value
                    }
                    i++;
                }
                numRecords = j;

                // trim
                if (numRecords < numValues) {
                    SrvRecord[] trimmed = new SrvRecord[numRecords];
                    System.arraycopy(srvRecords, 0, trimmed, 0, numRecords);
                    srvRecords = trimmed;
                }

                // Sort the service records in ascending order of their
                // priority value. For records with equal priority, move
                // those with weight 0 to the top of the list.
                if (numRecords > 1) {
                    Arrays.sort(srvRecords);
                }

                // extract the host and port number from each service record
                hostports = extractHostports(srvRecords);
            }
        } catch (NamingException e) {
            // ignore
        }
        return hostports;
    }

    /**
     * Extract hosts and port numbers from a list of SRV records.
     * An array of hostports is returned or null if none were found.
     */
    private static String[] extractHostports(SrvRecord[] srvRecords) {
        String[] hostports = null;

        int head = 0;
        int tail = 0;
        int sublistLength = 0;
        int k = 0;
        for (int i = 0; i < srvRecords.length; i++) {
            if (hostports == null) {
                hostports = new String[srvRecords.length];
            }
            // find the head and tail of the list of records having the same
            // priority value.
            head = i;
            while (i < srvRecords.length - 1 &&
                srvRecords[i].priority == srvRecords[i + 1].priority) {
                i++;
            }
            tail = i;

            // select hostports from the sublist
            sublistLength = (tail - head) + 1;
            for (int j = 0; j < sublistLength; j++) {
                hostports[k++] = selectHostport(srvRecords, head, tail);
            }
        }
        return hostports;
    }

    /*
     * Randomly select a service record in the range [head, tail] and return
     * its hostport value. Follows the algorithm in RFC 2782.
     */
    private static String selectHostport(SrvRecord[] srvRecords, int head,
            int tail) {
        if (head == tail) {
            return srvRecords[head].hostport;
        }

        // compute the running sum for records between head and tail
        int sum = 0;
        for (int i = head; i <= tail; i++) {
            if (srvRecords[i] != null) {
                sum += srvRecords[i].weight;
                srvRecords[i].sum = sum;
            }
        }
        String hostport = null;

        // If all records have zero weight, select first available one;
        // otherwise, randomly select a record according to its weight
        int target = (sum == 0 ? 0 : random.nextInt(sum + 1));
        for (int i = head; i <= tail; i++) {
            if (srvRecords[i] != null && srvRecords[i].sum >= target) {
                hostport = srvRecords[i].hostport;
                srvRecords[i] = null; // make this record unavailable
                break;
            }
        }
        return hostport;
    }

/**
 * This class holds a DNS service (SRV) record.
 * See http://www.ietf.org/rfc/rfc2782.txt
 */

static class SrvRecord implements Comparable<SrvRecord> {

    int priority;
    int weight;
    int sum;
    String hostport;

    /**
     * Creates a service record object from a string record.
     * DNS supplies the string record in the following format:
     * <pre>
     *     <Priority> " " <Weight> " " <Port> " " <Host>
     * </pre>
     */
    SrvRecord(String srvRecord) throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(srvRecord, " ");
        String port;

        if (tokenizer.countTokens() == 4) {
            priority = Integer.parseInt(tokenizer.nextToken());
            weight = Integer.parseInt(tokenizer.nextToken());
            port = tokenizer.nextToken();
            hostport = tokenizer.nextToken() + ":" + port;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /*
     * Sort records in ascending order of priority value. For records with
     * equal priority move those with weight 0 to the top of the list.
     */
    public int compareTo(SrvRecord that) {
        if (priority > that.priority) {
            return 1; // this > that
        } else if (priority < that.priority) {
            return -1; // this < that
        } else if (weight == 0 && that.weight != 0) {
            return -1; // this < that
        } else if (weight != 0 && that.weight == 0) {
            return 1; // this > that
        } else {
            return 0; // this == that
        }
    }
}
}
