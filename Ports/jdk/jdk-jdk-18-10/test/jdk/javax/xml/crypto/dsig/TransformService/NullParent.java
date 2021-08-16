/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8022120
 * @summary check that the init and marshalParams methods throw
 *          NullPointerException when the parent parameter is null
 * @run main/othervm/java.security.policy==test.policy NullParent
 */

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.TransformService;

public class NullParent {

    public static void main(String[] args) throws Exception {
        String[] transforms = new String[]
            { Transform.BASE64, Transform.ENVELOPED, Transform.XPATH,
              Transform.XPATH2, Transform.XSLT,
              CanonicalizationMethod.EXCLUSIVE,
              CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
              CanonicalizationMethod.INCLUSIVE,
              CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS };

        for (String transform : transforms) {
            System.out.println("Testing " + transform);
            TransformService ts = TransformService.getInstance(transform,
                                                               "DOM");
            try {
                ts.init(null, null);
                throw new Exception("init must throw NullPointerException " +
                                    "when the parent parameter is null");
            } catch (NullPointerException npe) { }
            try {
                ts.marshalParams(null, null);
                throw new Exception("marshalParams must throw " +
                                    "NullPointerException when the parent " +
                                    "parameter is null");
            } catch (NullPointerException npe) { }
        }
    }
}
