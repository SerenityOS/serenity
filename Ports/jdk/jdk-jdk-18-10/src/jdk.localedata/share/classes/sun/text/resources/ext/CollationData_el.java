/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text.resources.ext;

import java.util.ListResourceBundle;

public class CollationData_el extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                "& \u0361 = \u0387 = \u03f3 " // ?? \u03f3 is letter yot
                // punctuations
                + "& \u00b5 "
                + "< \u0374 "        // upper numeral sign
                + "< \u0375 "        // lower numeral sign
                + "< \u037a "        // ypogegrammeni
                + "< \u037e "        // question mark
                + "< \u0384 "        // tonos
                + "< \u0385 "        // dialytika tonos
                // Greek letters sorts after Z's
                + "& Z < \u03b1 , \u0391 "  // alpha
                + "; \u03ac , \u0386 "  // alpha-tonos
                + "< \u03b2 , \u0392 "  // beta
                + "; \u03d0 "           // beta symbol
                + "< \u03b3 , \u0393 "  // gamma
                + "< \u03b4 , \u0394 "  // delta
                + "< \u03b5 , \u0395 "  // epsilon
                + "; \u03ad , \u0388 "  // epsilon-tonos
                + "< \u03b6 , \u0396 "  // zeta
                + "< \u03b7 , \u0397 "  // eta
                + "; \u03ae , \u0389 "  // eta-tonos
                + "< \u03b8 , \u0398 "  // theta
                + "; \u03d1 "           // theta-symbol
                + "< \u03b9 , \u0399 "  // iota
                + "; \u03af , \u038a "  // iota-tonos
                + "; \u03ca , \u03aa "  // iota-dialytika
                + "; \u0390 "           // iota-dialytika
                + "< \u03ba , \u039a "  // kappa
                + "; \u03f0 "           // kappa symbol
                + "< \u03bb , \u039b "  // lamda
                + "< \u03bc , \u039c "  // mu
                + "< \u03bd , \u039d "  // nu
                + "< \u03be , \u039e "  // xi
                + "< \u03bf , \u039f "  // omicron
                + "; \u03cc , \u038c "  // omicron-tonos
                + "< \u03c0 , \u03a0 "  // pi
                + "; \u03d6 < \u03c1 "  // pi-symbol
                + ", \u03a1 "           // rho
                + "; \u03f1 "           // rho-symbol
                + "< \u03c3 , \u03c2 "  // sigma(final)
                + ", \u03a3 "           // sigma
                + "; \u03f2 "           // sigma-symbol
                + "< \u03c4 , \u03a4 "  // tau
                + "< \u03c5 , \u03a5 "  // upsilon
                + "; \u03cd , \u038e "  // upsilon-tonos
                + "; \u03cb , \u03ab "  // upsilon-dialytika
                + "= \u03d4 "           // upsilon-diaeresis-hook
                + "; \u03b0 "           // upsilon-dialytika-tonos
                + "; \u03d2 "           // upsilon-hook symbol
                + "; \u03d3 "           // upsilon-acute-hook
                + "< \u03c6 , \u03a6 "  // phi
                + "; \u03d5 "           // phi-symbol
                + "< \u03c7 , \u03a7 "  // chi
                + "< \u03c8 , \u03a8 "  // psi
                + "< \u03c9 , \u03a9 "  // omega
                + "; \u03ce , \u038f "  // omega-tonos
                + ", \u03da , \u03dc "  // stigma, digamma
                + ", \u03de , \u03e0 "  // koppa, sampi
                + "< \u03e3 , \u03e2 "  // shei
                + "< \u03e5 , \u03e4 "  // fei
                + "< \u03e7 , \u03e6 "  // khei
                + "< \u03e9 , \u03e8 "  // hori
                + "< \u03eb , \u03ea "  // gangia
                + "< \u03ed , \u03ec "  // shima
                + "< \u03ef , \u03ee "  // dei

                + "& \u03bc = \u00b5 "  // Micro symbol sorts with mu
            }
        };
    }
}
