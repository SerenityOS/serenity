/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.cmm;

import java.awt.color.CMMException;
import java.awt.color.ICC_Profile;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;

public final class CMSManager {

    private static volatile PCMM cmmImpl;

    public static PCMM getModule() {
        PCMM loc = cmmImpl;
        return loc != null ? loc : createModule();
    }

    private static synchronized PCMM createModule() {
        if (cmmImpl != null) {
            return cmmImpl;
        }

        GetPropertyAction gpa = new GetPropertyAction("sun.java2d.cmm");
        @SuppressWarnings("removal")
        String cmmProviderClass = AccessController.doPrivileged(gpa);
        CMMServiceProvider provider = null;
        if (cmmProviderClass != null) {
            try {
                Class<?> cls = Class.forName(cmmProviderClass);
                provider = (CMMServiceProvider)cls.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
            }
        }
        if (provider == null) {
            provider = new sun.java2d.cmm.lcms.LcmsServiceProvider();
        }

        cmmImpl = provider.getColorManagementModule();

        if (cmmImpl == null) {
            throw new CMMException("Cannot initialize Color Management System."+
                                   "No CM module found");
        }

        gpa = new GetPropertyAction("sun.java2d.cmm.trace");
        @SuppressWarnings("removal")
        String cmmTrace = AccessController.doPrivileged(gpa);
        if (cmmTrace != null) {
            cmmImpl = new CMMTracer(cmmImpl);
        }

        return cmmImpl;
    }

    static synchronized boolean canCreateModule() {
        return (cmmImpl == null);
    }

    /* CMM trace routines */

    public static class CMMTracer implements PCMM {
        PCMM tcmm;
        String cName ;

        public CMMTracer(PCMM tcmm) {
            this.tcmm = tcmm;
            cName = tcmm.getClass().getName();
        }

        public Profile loadProfile(byte[] data) {
            System.err.print(cName + ".loadProfile");
            Profile p = tcmm.loadProfile(data);
            System.err.printf("(ID=%s)\n", p.toString());
            return p;
        }

        public byte[] getProfileData(Profile p) {
            System.err.print(cName + ".getProfileData(ID=" + p + ") ");
            byte[] data = tcmm.getProfileData(p);
            System.err.println("requested " + data.length + " byte(s)");
            return data;
        }

        public byte[] getTagData(Profile p, int tagSignature) {
            System.err.printf(cName + ".getTagData(ID=%x, TagSig=%s)",
                              p, signatureToString(tagSignature));
            byte[] data = tcmm.getTagData(p, tagSignature);
            System.err.println(" requested " + data.length + " byte(s)");
            return data;
        }

        public void setTagData(Profile p, int tagSignature,
                               byte[] data) {
            System.err.print(cName + ".setTagData(ID=" + p +
                             ", TagSig=" + tagSignature + ")");
            System.err.println(" sending " + data.length + " byte(s)");
            tcmm.setTagData(p, tagSignature, data);
        }

        /* methods for creating ColorTransforms */
        public ColorTransform createTransform(ICC_Profile profile,
                                              int renderType,
                                              int transformType) {
            System.err.println(cName + ".createTransform(ICC_Profile,int,int)");
            return tcmm.createTransform(profile, renderType, transformType);
        }

        public ColorTransform createTransform(ColorTransform[] transforms) {
            System.err.println(cName + ".createTransform(ColorTransform[])");
            return tcmm.createTransform(transforms);
        }

        private static String signatureToString(int sig) {
            return String.format("%c%c%c%c",
                                 (char)(0xff & (sig >> 24)),
                                 (char)(0xff & (sig >> 16)),
                                 (char)(0xff & (sig >>  8)),
                                 (char)(0xff & (sig      )));
        }
    }
}
