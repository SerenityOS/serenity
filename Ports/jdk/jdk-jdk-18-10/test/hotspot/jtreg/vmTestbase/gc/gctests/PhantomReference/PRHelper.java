/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.gctests.PhantomReference;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Helper class that extends PhantomReference, that we can use
 * to match the hash code for the referent object.
 */
public final class PRHelper extends PhantomReference {

    private int referentHashCode;

    /**
     * Constructor for extended PhantomReference class.
     *
     * @param o              Referred object
     * @param referenceQueue Reference queue to attach the PR to
     */
    public PRHelper(Object o, ReferenceQueue referenceQueue) {
        super(o, referenceQueue);
        referentHashCode = -1;
    }

    /**
     * Get the referred objects hash code.
     *
     * @return Hash code for referred object
     */
    public int getReferentHashCode() {
        return referentHashCode;
    }

    /**
     * Set the original referred objects hash code for tracking.
     *
     * @param referentHashCode New hash code
     */
    public void setReferentHashCode(int referentHashCode) {
        this.referentHashCode = referentHashCode;
    }
}
