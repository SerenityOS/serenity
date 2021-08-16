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

public class CollationData_uk extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                /* for uk, default plus the following */
                "& 9 < \u0482 " +       // thousand sign
                "& Z" +                 // Arabic script sorts after Z's
                "< \u0430 , \u0410" +   // a
                "< \u0431 , \u0411" +   // be
                "< \u0432 , \u0412" +   // ve
                "< \u0433 , \u0413" +   // ghe
                "; \u0491 , \u0490" +   // ghe-upturn
                "; \u0495 , \u0494" +   // ghe-mid-hook
                "; \u0453 , \u0403" +   // gje
                "; \u0493 , \u0492" +   // ghe-stroke
                "< \u0434 , \u0414" +   // de
                "< \u0452 , \u0402" +   // dje
                "< \u0435 , \u0415" +   // ie
                "; \u04bd , \u04bc" +   // che
                "; \u0451 , \u0401" +   // io
                "; \u04bf , \u04be" +   // che-descender
                "< \u0454 , \u0404" +   // uk ie
                "< \u0436 , \u0416" +   // zhe
                "; \u0497 , \u0496" +   // zhe-descender
                "; \u04c2 , \u04c1" +   // zhe-breve
                "< \u0437 , \u0417" +   // ze
                "; \u0499 , \u0498" +   // zh-descender
                "< \u0455 , \u0405" +   // dze
                "< \u0438 , \u0418" +   // i
                "< \u0456 , \u0406" +   // uk/bg i
                "; \u04c0 " +           // palochka
                "< \u0457 , \u0407" +   // uk yi
                "< \u0439 , \u0419" +   // short i
                "< \u0458 , \u0408" +   // je
                "< \u043a , \u041a" +   // ka
                "; \u049f , \u049e" +   // ka-stroke
                "; \u04c4 , \u04c3" +   // ka-hook
                "; \u049d , \u049c" +   // ka-vt-stroke
                "; \u04a1 , \u04a0" +   // bashkir-ka
                "; \u045c , \u040c" +   // kje
                "; \u049b , \u049a" +   // ka-descender
                "< \u043b , \u041b" +   // el
                "< \u0459 , \u0409" +   // lje
                "< \u043c , \u041c" +   // em
                "< \u043d , \u041d" +   // en
                "; \u0463 " +           // yat
                "; \u04a3 , \u04a2" +   // en-descender
                "; \u04a5 , \u04a4" +   // en-ghe
                "; \u04bb , \u04ba" +   // shha
                "; \u04c8 , \u04c7" +   // en-hook
                "< \u045a , \u040a" +   // nje
                "< \u043e , \u041e" +   // o
                "; \u04a9 , \u04a8" +   // ha
                "< \u043f , \u041f" +   // pe
                "; \u04a7 , \u04a6" +   // pe-mid-hook
                "< \u0440 , \u0420" +   // er
                "< \u0441 , \u0421" +   // es
                "; \u04ab , \u04aa" +   // es-descender
                "< \u0442 , \u0422" +   // te
                "; \u04ad , \u04ac" +   // te-descender
                "< \u045b , \u040b" +   // tshe
                "< \u0443 , \u0423" +   // u
                "; \u04af , \u04ae" +   // straight u
                "< \u045e , \u040e" +   // short u
                "< \u04b1 , \u04b0" +   // straight u-stroke
                "< \u0444 , \u0424" +   // ef
                "< \u0445 , \u0425" +   // ha
                "; \u04b3 , \u04b2" +   // ha-descender
                "< \u0446 , \u0426" +   // tse
                "; \u04b5 , \u04b4" +   // te tse
                "< \u0447 , \u0427" +   // che
                "; \u04b7 ; \u04b6" +   // che-descender
                "; \u04b9 , \u04b8" +   // che-vt-stroke
                "; \u04cc , \u04cb" +   // che
                "< \u045f , \u040f" +   // dzhe
                "< \u0448 , \u0428" +   // sha
                "< \u0449 , \u0429" +   // shcha
                "< \u044a , \u042a" +   // hard sign
                "< \u044b , \u042b" +   // yeru
                "< \u044d , \u042d" +   // e
                "< \u044e , \u042e" +   // yu
                "< \u044f , \u042f" +   // ya
                "< \u044c , \u042c" +   // soft sign
                "< \u0461 , \u0460" +   // omega
                "< \u0462 " +           // yat
                "< \u0465 , \u0464" +   // iotified e
                "< \u0467 , \u0466" +   // little yus
                "< \u0469 , \u0468" +   // iotified little yus
                "< \u046b , \u046a" +   // big yus
                "< \u046d , \u046c" +   // iotified big yus
                "< \u046f , \u046e" +   // ksi
                "< \u0471 , \u0470" +   // psi
                "< \u0473 , \u0472" +   // fita
                "< \u0475 , \u0474" +   // izhitsa
                "; \u0477 , \u0476" +   // izhitsa-double-grave
                "< \u0479 , \u0478" +   // uk
                "< \u047b , \u047a" +   // round omega
                "< \u047d , \u047c" +   // omega-titlo
                "< \u047f , \u047e" +   // ot
                "< \u0481 , \u0480"     // koppa
            }
        };
    }
}
