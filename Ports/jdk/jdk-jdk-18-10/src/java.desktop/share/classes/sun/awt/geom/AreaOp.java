/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.geom;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Comparator;
import java.util.Arrays;

public abstract class AreaOp {
    public abstract static class CAGOp extends AreaOp {
        boolean inLeft;
        boolean inRight;
        boolean inResult;

        public void newRow() {
            inLeft = false;
            inRight = false;
            inResult = false;
        }

        public int classify(Edge e) {
            if (e.getCurveTag() == CTAG_LEFT) {
                inLeft = !inLeft;
            } else {
                inRight = !inRight;
            }
            boolean newClass = newClassification(inLeft, inRight);
            if (inResult == newClass) {
                return ETAG_IGNORE;
            }
            inResult = newClass;
            return (newClass ? ETAG_ENTER : ETAG_EXIT);
        }

        public int getState() {
            return (inResult ? RSTAG_INSIDE : RSTAG_OUTSIDE);
        }

        public abstract boolean newClassification(boolean inLeft,
                                                  boolean inRight);
    }

    public static class AddOp extends CAGOp {
        public boolean newClassification(boolean inLeft, boolean inRight) {
            return (inLeft || inRight);
        }
    }

    public static class SubOp extends CAGOp {
        public boolean newClassification(boolean inLeft, boolean inRight) {
            return (inLeft && !inRight);
        }
    }

    public static class IntOp extends CAGOp {
        public boolean newClassification(boolean inLeft, boolean inRight) {
            return (inLeft && inRight);
        }
    }

    public static class XorOp extends CAGOp {
        public boolean newClassification(boolean inLeft, boolean inRight) {
            return (inLeft != inRight);
        }
    }

    public static class NZWindOp extends AreaOp {
        private int count;

        public void newRow() {
            count = 0;
        }

        public int classify(Edge e) {
            // Note: the right curves should be an empty set with this op...
            // assert(e.getCurveTag() == CTAG_LEFT);
            int newCount = count;
            int type = (newCount == 0 ? ETAG_ENTER : ETAG_IGNORE);
            newCount += e.getCurve().getDirection();
            count = newCount;
            return (newCount == 0 ? ETAG_EXIT : type);
        }

        public int getState() {
            return ((count == 0) ? RSTAG_OUTSIDE : RSTAG_INSIDE);
        }
    }

    public static class EOWindOp extends AreaOp {
        private boolean inside;

        public void newRow() {
            inside = false;
        }

        public int classify(Edge e) {
            // Note: the right curves should be an empty set with this op...
            // assert(e.getCurveTag() == CTAG_LEFT);
            boolean newInside = !inside;
            inside = newInside;
            return (newInside ? ETAG_ENTER : ETAG_EXIT);
        }

        public int getState() {
            return (inside ? RSTAG_INSIDE : RSTAG_OUTSIDE);
        }
    }

    private AreaOp() {
    }

    /* Constants to tag the left and right curves in the edge list */
    public static final int CTAG_LEFT = 0;
    public static final int CTAG_RIGHT = 1;

    /* Constants to classify edges */
    public static final int ETAG_IGNORE = 0;
    public static final int ETAG_ENTER = 1;
    public static final int ETAG_EXIT = -1;

    /* Constants used to classify result state */
    public static final int RSTAG_INSIDE = 1;
    public static final int RSTAG_OUTSIDE = -1;

    public abstract void newRow();

    public abstract int classify(Edge e);

    public abstract int getState();

    public Vector<Curve> calculate(Vector<Curve> left, Vector<Curve> right) {
        Vector<Edge> edges = new Vector<>();
        addEdges(edges, left, AreaOp.CTAG_LEFT);
        addEdges(edges, right, AreaOp.CTAG_RIGHT);
        Vector<Curve> curves = pruneEdges(edges);
        if (false) {
            System.out.println("result: ");
            int numcurves = curves.size();
            Curve[] curvelist = curves.toArray(new Curve[numcurves]);
            for (int i = 0; i < numcurves; i++) {
                System.out.println("curvelist["+i+"] = "+curvelist[i]);
            }
        }
        return curves;
    }

    private static void addEdges(Vector<Edge> edges, Vector<Curve> curves, int curvetag) {
        Enumeration<Curve> enum_ = curves.elements();
        while (enum_.hasMoreElements()) {
            Curve c = enum_.nextElement();
            if (c.getOrder() > 0) {
                edges.add(new Edge(c, curvetag));
            }
        }
    }

    private static Comparator<Edge> YXTopComparator = new Comparator<Edge>() {
        public int compare(Edge o1, Edge o2) {
            Curve c1 = o1.getCurve();
            Curve c2 = o2.getCurve();
            double v1, v2;
            if ((v1 = c1.getYTop()) == (v2 = c2.getYTop())) {
                if ((v1 = c1.getXTop()) == (v2 = c2.getXTop())) {
                    return 0;
                }
            }
            if (v1 < v2) {
                return -1;
            }
            return 1;
        }
    };

    private Vector<Curve> pruneEdges(Vector<Edge> edges) {
        int numedges = edges.size();
        if (numedges < 2) {
            // empty vector is expected with less than 2 edges
            return new Vector<>();
        }
        Edge[] edgelist = edges.toArray(new Edge[numedges]);
        Arrays.sort(edgelist, YXTopComparator);
        if (false) {
            System.out.println("pruning: ");
            for (int i = 0; i < numedges; i++) {
                System.out.println("edgelist["+i+"] = "+edgelist[i]);
            }
        }
        Edge e;
        int left = 0;
        int right = 0;
        int cur = 0;
        int next = 0;
        double[] yrange = new double[2];
        Vector<CurveLink> subcurves = new Vector<>();
        Vector<ChainEnd> chains = new Vector<>();
        Vector<CurveLink> links = new Vector<>();
        // Active edges are between left (inclusive) and right (exclusive)
        while (left < numedges) {
            double y = yrange[0];
            // Prune active edges that fall off the top of the active y range
            for (cur = next = right - 1; cur >= left; cur--) {
                e = edgelist[cur];
                if (e.getCurve().getYBot() > y) {
                    if (next > cur) {
                        edgelist[next] = e;
                    }
                    next--;
                }
            }
            left = next + 1;
            // Grab a new "top of Y range" if the active edges are empty
            if (left >= right) {
                if (right >= numedges) {
                    break;
                }
                y = edgelist[right].getCurve().getYTop();
                if (y > yrange[0]) {
                    finalizeSubCurves(subcurves, chains);
                }
                yrange[0] = y;
            }
            // Incorporate new active edges that enter the active y range
            while (right < numedges) {
                e = edgelist[right];
                if (e.getCurve().getYTop() > y) {
                    break;
                }
                right++;
            }
            // Sort the current active edges by their X values and
            // determine the maximum valid Y range where the X ordering
            // is correct
            yrange[1] = edgelist[left].getCurve().getYBot();
            if (right < numedges) {
                y = edgelist[right].getCurve().getYTop();
                if (yrange[1] > y) {
                    yrange[1] = y;
                }
            }
            if (false) {
                System.out.println("current line: y = ["+
                                   yrange[0]+", "+yrange[1]+"]");
                for (cur = left; cur < right; cur++) {
                    System.out.println("  "+edgelist[cur]);
                }
            }
            // Note: We could start at left+1, but we need to make
            // sure that edgelist[left] has its equivalence set to 0.
            int nexteq = 1;
            for (cur = left; cur < right; cur++) {
                e = edgelist[cur];
                e.setEquivalence(0);
                for (next = cur; next > left; next--) {
                    Edge prevedge = edgelist[next-1];
                    int ordering = e.compareTo(prevedge, yrange);
                    if (yrange[1] <= yrange[0]) {
                        throw new InternalError("backstepping to "+yrange[1]+
                                                " from "+yrange[0]);
                    }
                    if (ordering >= 0) {
                        if (ordering == 0) {
                            // If the curves are equal, mark them to be
                            // deleted later if they cancel each other
                            // out so that we avoid having extraneous
                            // curve segments.
                            int eq = prevedge.getEquivalence();
                            if (eq == 0) {
                                eq = nexteq++;
                                prevedge.setEquivalence(eq);
                            }
                            e.setEquivalence(eq);
                        }
                        break;
                    }
                    edgelist[next] = prevedge;
                }
                edgelist[next] = e;
            }
            if (false) {
                System.out.println("current sorted line: y = ["+
                                   yrange[0]+", "+yrange[1]+"]");
                for (cur = left; cur < right; cur++) {
                    System.out.println("  "+edgelist[cur]);
                }
            }
            // Now prune the active edge list.
            // For each edge in the list, determine its classification
            // (entering shape, exiting shape, ignore - no change) and
            // record the current Y range and its classification in the
            // Edge object for use later in constructing the new outline.
            newRow();
            double ystart = yrange[0];
            double yend = yrange[1];
            for (cur = left; cur < right; cur++) {
                e = edgelist[cur];
                int etag;
                int eq = e.getEquivalence();
                if (eq != 0) {
                    // Find one of the segments in the "equal" range
                    // with the right transition state and prefer an
                    // edge that was either active up until ystart
                    // or the edge that extends the furthest downward
                    // (i.e. has the most potential for continuation)
                    int origstate = getState();
                    etag = (origstate == AreaOp.RSTAG_INSIDE
                            ? AreaOp.ETAG_EXIT
                            : AreaOp.ETAG_ENTER);
                    Edge activematch = null;
                    Edge longestmatch = e;
                    double furthesty = yend;
                    do {
                        // Note: classify() must be called
                        // on every edge we consume here.
                        classify(e);
                        if (activematch == null &&
                            e.isActiveFor(ystart, etag))
                        {
                            activematch = e;
                        }
                        y = e.getCurve().getYBot();
                        if (y > furthesty) {
                            longestmatch = e;
                            furthesty = y;
                        }
                    } while (++cur < right &&
                             (e = edgelist[cur]).getEquivalence() == eq);
                    --cur;
                    if (getState() == origstate) {
                        etag = AreaOp.ETAG_IGNORE;
                    } else {
                        e = (activematch != null ? activematch : longestmatch);
                    }
                } else {
                    etag = classify(e);
                }
                if (etag != AreaOp.ETAG_IGNORE) {
                    e.record(yend, etag);
                    links.add(new CurveLink(e.getCurve(), ystart, yend, etag));
                }
            }
            // assert(getState() == AreaOp.RSTAG_OUTSIDE);
            if (getState() != AreaOp.RSTAG_OUTSIDE) {
                System.out.println("Still inside at end of active edge list!");
                System.out.println("num curves = "+(right-left));
                System.out.println("num links = "+links.size());
                System.out.println("y top = "+yrange[0]);
                if (right < numedges) {
                    System.out.println("y top of next curve = "+
                                       edgelist[right].getCurve().getYTop());
                } else {
                    System.out.println("no more curves");
                }
                for (cur = left; cur < right; cur++) {
                    e = edgelist[cur];
                    System.out.println(e);
                    int eq = e.getEquivalence();
                    if (eq != 0) {
                        System.out.println("  was equal to "+eq+"...");
                    }
                }
            }
            if (false) {
                System.out.println("new links:");
                for (int i = 0; i < links.size(); i++) {
                    CurveLink link = links.elementAt(i);
                    System.out.println("  "+link.getSubCurve());
                }
            }
            resolveLinks(subcurves, chains, links);
            links.clear();
            // Finally capture the bottom of the valid Y range as the top
            // of the next Y range.
            yrange[0] = yend;
        }
        finalizeSubCurves(subcurves, chains);
        Vector<Curve> ret = new Vector<>();
        Enumeration<CurveLink> enum_ = subcurves.elements();
        while (enum_.hasMoreElements()) {
            CurveLink link = enum_.nextElement();
            ret.add(link.getMoveto());
            CurveLink nextlink = link;
            while ((nextlink = nextlink.getNext()) != null) {
                if (!link.absorb(nextlink)) {
                    ret.add(link.getSubCurve());
                    link = nextlink;
                }
            }
            ret.add(link.getSubCurve());
        }
        return ret;
    }

    public static void finalizeSubCurves(Vector<CurveLink> subcurves,
                                         Vector<ChainEnd> chains) {
        int numchains = chains.size();
        if (numchains == 0) {
            return;
        }
        if ((numchains & 1) != 0) {
            throw new InternalError("Odd number of chains!");
        }
        ChainEnd[] endlist = new ChainEnd[numchains];
        chains.toArray(endlist);
        for (int i = 1; i < numchains; i += 2) {
            ChainEnd open = endlist[i - 1];
            ChainEnd close = endlist[i];
            CurveLink subcurve = open.linkTo(close);
            if (subcurve != null) {
                subcurves.add(subcurve);
            }
        }
        chains.clear();
    }

    private static CurveLink[] EmptyLinkList = new CurveLink[2];
    private static ChainEnd[] EmptyChainList = new ChainEnd[2];

    public static void resolveLinks(Vector<CurveLink> subcurves,
                                    Vector<ChainEnd> chains,
                                    Vector<CurveLink> links)
    {
        int numlinks = links.size();
        CurveLink[] linklist;
        if (numlinks == 0) {
            linklist = EmptyLinkList;
        } else {
            if ((numlinks & 1) != 0) {
                throw new InternalError("Odd number of new curves!");
            }
            linklist = new CurveLink[numlinks+2];
            links.toArray(linklist);
        }
        int numchains = chains.size();
        ChainEnd[] endlist;
        if (numchains == 0) {
            endlist = EmptyChainList;
        } else {
            if ((numchains & 1) != 0) {
                throw new InternalError("Odd number of chains!");
            }
            endlist = new ChainEnd[numchains+2];
            chains.toArray(endlist);
        }
        int curchain = 0;
        int curlink = 0;
        chains.clear();
        ChainEnd chain = endlist[0];
        ChainEnd nextchain = endlist[1];
        CurveLink link = linklist[0];
        CurveLink nextlink = linklist[1];
        while (chain != null || link != null) {
            /*
             * Strategy 1:
             * Connect chains or links if they are the only things left...
             */
            boolean connectchains = (link == null);
            boolean connectlinks = (chain == null);

            if (!connectchains && !connectlinks) {
                // assert(link != null && chain != null);
                /*
                 * Strategy 2:
                 * Connect chains or links if they close off an open area...
                 */
                connectchains = ((curchain & 1) == 0 &&
                                 chain.getX() == nextchain.getX());
                connectlinks = ((curlink & 1) == 0 &&
                                link.getX() == nextlink.getX());

                if (!connectchains && !connectlinks) {
                    /*
                     * Strategy 3:
                     * Connect chains or links if their successor is
                     * between them and their potential connectee...
                     */
                    double cx = chain.getX();
                    double lx = link.getX();
                    connectchains =
                        (nextchain != null && cx < lx &&
                         obstructs(nextchain.getX(), lx, curchain));
                    connectlinks =
                        (nextlink != null && lx < cx &&
                         obstructs(nextlink.getX(), cx, curlink));
                }
            }
            if (connectchains) {
                CurveLink subcurve = chain.linkTo(nextchain);
                if (subcurve != null) {
                    subcurves.add(subcurve);
                }
                curchain += 2;
                chain = endlist[curchain];
                nextchain = endlist[curchain+1];
            }
            if (connectlinks) {
                ChainEnd openend = new ChainEnd(link, null);
                ChainEnd closeend = new ChainEnd(nextlink, openend);
                openend.setOtherEnd(closeend);
                chains.add(openend);
                chains.add(closeend);
                curlink += 2;
                link = linklist[curlink];
                nextlink = linklist[curlink+1];
            }
            if (!connectchains && !connectlinks) {
                // assert(link != null);
                // assert(chain != null);
                // assert(chain.getEtag() == link.getEtag());
                chain.addLink(link);
                chains.add(chain);
                curchain++;
                chain = nextchain;
                nextchain = endlist[curchain+1];
                curlink++;
                link = nextlink;
                nextlink = linklist[curlink+1];
            }
        }
        if ((chains.size() & 1) != 0) {
            System.out.println("Odd number of chains!");
        }
    }

    /*
     * Does the position of the next edge at v1 "obstruct" the
     * connectivity between current edge and the potential
     * partner edge which is positioned at v2?
     *
     * Phase tells us whether we are testing for a transition
     * into or out of the interior part of the resulting area.
     *
     * Require 4-connected continuity if this edge and the partner
     * edge are both "entering into" type edges
     * Allow 8-connected continuity for "exiting from" type edges
     */
    public static boolean obstructs(double v1, double v2, int phase) {
        return (((phase & 1) == 0) ? (v1 <= v2) : (v1 < v2));
    }
}
