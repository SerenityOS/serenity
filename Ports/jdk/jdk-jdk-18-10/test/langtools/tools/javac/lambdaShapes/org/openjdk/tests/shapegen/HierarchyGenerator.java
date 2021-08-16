/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package org.openjdk.tests.shapegen;

import org.openjdk.tests.shapegen.ClassCase.Kind;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.tests.shapegen.ClassCase.Kind.*;

import static java.lang.Math.pow;

/**
 *
 * @author Robert Field
 */
public final class HierarchyGenerator {

    private int okcnt = 0;
    private int errcnt = 0;
    private Set<Hierarchy> uniqueOK = new HashSet<>();
    private Set<Hierarchy> uniqueErr = new HashSet<>();

    /**
     * @param args the command line arguments
     */
    public HierarchyGenerator() {
        organize("exhaustive interface", iExhaustive(2));
        organize("exhaustive class", cExhaustive());
        organize("shapes interface", iShapes());
        organize("shapes class/interface", ciShapes());

        System.out.printf("\nExpect OK:    %d -- unique %d",   okcnt,  uniqueOK.size());
        System.out.printf("\nExpect Error: %d -- unique %d\n", errcnt, uniqueErr.size());
    }

    public Collection<Hierarchy> getOK() {
        return uniqueOK;
    }

    public Collection<Hierarchy> getErr() {
        return uniqueErr;
    }

    private void organize(String tname, List<Hierarchy> totest) {
        System.out.printf("\nGenerating %s....\n", tname);
        int nodefault = 0;
        List<Hierarchy> ok = new ArrayList<>();
        List<Hierarchy> err = new ArrayList<>();
        for (Hierarchy cc : totest) {
            if (cc.anyDefaults()) {
                //System.out.printf("  %s\n", cc);
                if (cc.get_OK()) {
                    ok.add(cc);
                } else {
                    err.add(cc);
                }
            } else {
                ++nodefault;
            }
        }

        errcnt += err.size();
        okcnt += ok.size();
        uniqueErr.addAll(err);
        uniqueOK.addAll(ok);

        System.out.printf("  %5d No default\n  %5d Error\n  %5d OK\n  %5d Total\n",
                nodefault, err.size(), ok.size(), totest.size());
    }

    public List<Hierarchy> iExhaustive(int idepth) {
        List<ClassCase> current = new ArrayList<>();
        for (int i = 0; i < idepth; ++i) {
            current = ilayer(current);
        }
        return wrapInClassAndHierarchy(current);
    }

    private List<ClassCase> ilayer(List<ClassCase> srcLayer) {
        List<ClassCase> lay = new ArrayList<>();
        for (int i = (int) pow(2, srcLayer.size()) - 1; i >= 0; --i) {
            List<ClassCase> itfs = new ArrayList<>();
            for (int b = srcLayer.size() - 1; b >= 0; --b) {
                if ((i & (1<<b)) != 0) {
                    itfs.add(srcLayer.get(b));
                }
            }
            lay.add(new ClassCase(IVAC, null, itfs));
            lay.add(new ClassCase(IPRESENT, null, itfs));
            lay.add(new ClassCase(IDEFAULT, null, itfs));
            lay.add(new ClassCase(IDEFAULT, null, itfs));
        }
        return lay;
    }

    public List<Hierarchy> cExhaustive() {
        final Kind[] iKinds = new Kind[]{IDEFAULT, IVAC, IPRESENT, null};
        final Kind[] cKinds = new Kind[]{CNONE, CABSTRACT, CCONCRETE};
        List<Hierarchy> totest = new ArrayList<>();
        for (int i1 = 0; i1 < iKinds.length; ++i1) {
            for (int i2 = 0; i2 < iKinds.length; ++i2) {
                for (int i3 = 0; i3 < iKinds.length; ++i3) {
                    for (int c1 = 0; c1 < cKinds.length; ++c1) {
                        for (int c2 = 0; c2 < cKinds.length; ++c2) {
                            for (int c3 = 0; c3 < cKinds.length; ++c3) {
                                totest.add( new Hierarchy(
                                        new ClassCase(cKinds[c1],
                                            new ClassCase(cKinds[c2],
                                                new ClassCase(cKinds[c3],
                                                    null,
                                                    iList(iKinds[i1])
                                                ),
                                                iList(iKinds[i2])
                                            ),
                                            iList(iKinds[i3])
                                        )));
                            }
                        }
                    }
                }
            }
        }
        return totest;
    }

    public static final List<ClassCase> EMPTY_LIST = new ArrayList<>();

    private List<ClassCase> iList(Kind kind) {
        if (kind == null) {
            return EMPTY_LIST;
        } else {
            List<ClassCase> itfs = new ArrayList<>();
            itfs.add(new ClassCase(kind, null, EMPTY_LIST));
            return itfs;
        }
    }

    public List<Hierarchy> ciShapes() {
        return wrapInHierarchy(TTShape.allCases(true));
    }

    public List<Hierarchy> iShapes() {
        return wrapInClassAndHierarchy(TTShape.allCases(false));
    }

    public List<Hierarchy> wrapInClassAndHierarchy(List<ClassCase> ihs) {
        List<Hierarchy> totest = new ArrayList<>();
        for (ClassCase cc : ihs) {
            List<ClassCase> interfaces = new ArrayList<>();
            interfaces.add(cc);
            totest.add(new Hierarchy(new ClassCase(CNONE, null, interfaces)));
        }
        return totest;
    }

    public List<Hierarchy> wrapInHierarchy(List<ClassCase> ihs) {
        List<Hierarchy> totest = new ArrayList<>();
        for (ClassCase cc : ihs) {
            totest.add(new Hierarchy(cc));
        }
        return totest;
    }
}
