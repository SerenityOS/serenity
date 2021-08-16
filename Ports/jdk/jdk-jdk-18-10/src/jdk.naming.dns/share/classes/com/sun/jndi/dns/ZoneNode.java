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


import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Vector;


/**
 * ZoneNode extends NameNode to represent a tree of the zones in the
 * DNS namespace, along with any intermediate nodes between zones.
 * A ZoneNode that represents a zone may be "populated" with a
 * NameNode tree containing the zone's contents.
 *
 * <p> A populated zone's contents will be flagged as having expired after
 * the time specified by the minimum TTL value in the zone's SOA record.
 *
 * <p> Since zone cuts aren't directly modeled by a tree of ZoneNodes,
 * ZoneNode.isZoneCut() always returns false.
 *
 * <p> The synchronization strategy is documented in DnsContext.java.
 *
 * <p> The zone's contents are accessed via a soft reference, so its
 * heap space may be reclaimed when necessary.  The zone may be
 * repopulated later.
 *
 * @author Scott Seligman
 */


class ZoneNode extends NameNode {

    private SoftReference<NameNode> contentsRef = null;   // the zone's namespace
    private long serialNumber = -1;     // the zone data's serial number
    private Date expiration = null;     // time when the zone's data expires

    ZoneNode(String label) {
        super(label);
    }

    protected NameNode newNameNode(String label) {
        return new ZoneNode(label);
    }

    /*
     * Clears the contents of this node.  If the node was flagged as
     * expired, it remains so.
     */
    synchronized void depopulate() {
        contentsRef = null;
        serialNumber = -1;
    }

    /*
     * Is this node currently populated?
     */
    synchronized boolean isPopulated() {
        return (getContents() != null);
    }

    /*
     * Returns the zone's contents, or null if the zone is not populated.
     */
    synchronized NameNode getContents() {
        return (contentsRef != null)
                ? contentsRef.get()
                : null;
    }

    /*
     * Has this zone's data expired?
     */
    synchronized boolean isExpired() {
        return ((expiration != null) && expiration.before(new Date()));
    }

    /*
     * Returns the deepest populated zone on the path specified by a
     * fully-qualified domain name, or null if there is no populated
     * zone on that path.  Note that a node may be depopulated after
     * being returned.
     */
    ZoneNode getDeepestPopulated(DnsName fqdn) {
        ZoneNode znode = this;
        ZoneNode popNode = isPopulated() ? this : null;
        for (int i = 1; i < fqdn.size(); i++) { //     "i=1" to skip root label
            znode = (ZoneNode) znode.get(fqdn.getKey(i));
            if (znode == null) {
                break;
            } else if (znode.isPopulated()) {
                popNode = znode;
            }
        }
        return popNode;
    }

    /*
     * Populates (or repopulates) a zone given its own fully-qualified
     * name and its resource records.  Returns the zone's new contents.
     */
    NameNode populate(DnsName zone, ResourceRecords rrs) {
        // assert zone.get(0).isEmpty();               // zone has root label
        // assert (zone.size() == (depth() + 1));       // +1 due to root label

        NameNode newContents = new NameNode(null);

        for (int i = 0; i < rrs.answer.size(); i++) {
            ResourceRecord rr = rrs.answer.elementAt(i);
            DnsName n = rr.getName();

            // Ignore resource records whose names aren't within the zone's
            // domain.  Also skip records of the zone's top node, since
            // the zone's root NameNode is already in place.
            if ((n.size() > zone.size()) && n.startsWith(zone)) {
                NameNode nnode = newContents.add(n, zone.size());
                if (rr.getType() == ResourceRecord.TYPE_NS) {
                    nnode.setZoneCut(true);
                }
            }
        }
        // The zone's SOA record is the first record in the answer section.
        ResourceRecord soa = rrs.answer.firstElement();
        synchronized (this) {
            contentsRef = new SoftReference<NameNode>(newContents);
            serialNumber = getSerialNumber(soa);
            setExpiration(getMinimumTtl(soa));
            return newContents;
        }
    }

    /*
     * Set this zone's data to expire in {@code secsToExpiration} seconds.
     */
    private void setExpiration(long secsToExpiration) {
        expiration = new Date(System.currentTimeMillis() +
                              1000 * secsToExpiration);
    }

    /*
     * Returns an SOA record's minimum TTL field.
     */
    private static long getMinimumTtl(ResourceRecord soa) {
        String rdata = (String) soa.getRdata();
        int pos = rdata.lastIndexOf(' ') + 1;
        return Long.parseLong(rdata.substring(pos));
    }

    /*
     * Compares this zone's serial number with that of an SOA record.
     * Zone must be populated.
     * Returns a negative, zero, or positive integer as this zone's
     * serial number is less than, equal to, or greater than the SOA
     * record's.
     * See ResourceRecord.compareSerialNumbers() for a description of
     * serial number arithmetic.
     */
    int compareSerialNumberTo(ResourceRecord soa) {
        // assert isPopulated();
        return ResourceRecord.compareSerialNumbers(serialNumber,
                                                   getSerialNumber(soa));
    }

    /*
     * Returns an SOA record's serial number.
     */
    private static long getSerialNumber(ResourceRecord soa) {
        String rdata = (String) soa.getRdata();

        // An SOA record ends with:  serial refresh retry expire minimum.
        // Set "beg" to the space before serial, and "end" to the space after.
        // We go "backward" to avoid dealing with escaped spaces in names.
        int beg = rdata.length();
        int end = -1;
        for (int i = 0; i < 5; i++) {
            end = beg;
            beg = rdata.lastIndexOf(' ', end - 1);
        }
        return Long.parseLong(rdata.substring(beg + 1, end));
    }
}
