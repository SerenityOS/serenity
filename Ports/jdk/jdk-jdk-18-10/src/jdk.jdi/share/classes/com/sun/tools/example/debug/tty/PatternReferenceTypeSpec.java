/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

import com.sun.jdi.*;
import com.sun.jdi.request.ClassPrepareRequest;
import java.util.StringTokenizer;


class PatternReferenceTypeSpec implements ReferenceTypeSpec {
    final String classId;
    String stem;

    PatternReferenceTypeSpec(String classId) throws ClassNotFoundException {
        this.classId = classId;
        stem = classId;
        if (classId.startsWith("*")) {
            stem = stem.substring(1);
        } else if (classId.endsWith("*")) {
            stem = stem.substring(0, classId.length() - 1);
        }
        checkClassName(stem);
    }

    /**
     * Is this spec unique or is it a class pattern?
     */
    public boolean isUnique() {
        return classId.equals(stem);
    }

    /**
     * Does the specified ReferenceType match this spec.
     */
    @Override
    public boolean matches(ReferenceType refType) {
        if (classId.startsWith("*")) {
            return refType.name().endsWith(stem);
        } else if (classId.endsWith("*")) {
            return refType.name().startsWith(stem);
        } else {
            return refType.name().equals(classId);
        }
    }

    @Override
    public ClassPrepareRequest createPrepareRequest() {
        ClassPrepareRequest request =
            Env.vm().eventRequestManager().createClassPrepareRequest();
        request.addClassFilter(classId);
        request.addCountFilter(1);
        return request;
    }

    @Override
    public int hashCode() {
        return classId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PatternReferenceTypeSpec) {
            PatternReferenceTypeSpec spec = (PatternReferenceTypeSpec)obj;

            return classId.equals(spec.classId);
        } else {
            return false;
        }
    }

    private void checkClassName(String className) throws ClassNotFoundException {
        int slashIdx = className.indexOf("/");

        // Slash is present in hidden class names only. It looks like p.Foo/0x1234.
        if (slashIdx != -1) {
            // A hidden class name is ending with a slash following by a suffix.
            int lastSlashIdx = className.lastIndexOf("/");
            int lastDotIdx = className.lastIndexOf(".");

            // There must be just one slash with a following suffix but no dots.
            if (slashIdx != lastSlashIdx || lastDotIdx > slashIdx || slashIdx + 1 == className.length()) {
                throw new ClassNotFoundException();
            }
            // Check prefix and suffix separately.
            String[] parts = className.split("/");
            assert parts.length == 2;
            className = parts[0];
            String hcSuffix = parts[1];
            if (!isUnqualifiedName(hcSuffix)) {
                throw new ClassNotFoundException();
            }
        }

        // Do stricter checking of class name validity on deferred
        //  because if the name is invalid, it will
        // never match a future loaded class, and we'll be silent
        // about it.
        StringTokenizer tokenizer = new StringTokenizer(className, ".");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            // Each dot-separated piece must be a valid identifier
            // and the first token can also be "*". (Note that
            // numeric class ids are not permitted. They must
            // match a loaded class.)
            if (!isJavaIdentifier(token)) {
                throw new ClassNotFoundException();
            }
        }
    }

    private boolean isUnqualifiedName(String s) {
        if (s.length() == 0) {
            return false;
        }
        // unqualified names should have no characters: ".;/["
        return !s.matches("[.;/\091]*"); // \091 is '['
    }

    private boolean isJavaIdentifier(String s) {
        if (s.length() == 0) {
            return false;
        }

        int cp = s.codePointAt(0);
        if (! Character.isJavaIdentifierStart(cp)) {
            return false;
        }

        for (int i = Character.charCount(cp); i < s.length(); i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            if (! Character.isJavaIdentifierPart(cp)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return classId;
    }
}
