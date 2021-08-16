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

public class CollationData_ar extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                // for ar, the following additions are needed:
               "& \u0361 = \u0640"
               + "= \u064b"
               + "= \u064c"
               + "= \u064d"
               + "= \u064e"
               + "= \u064f"
               + "= \u0650"
               + "= \u0652"
               + "= \u066d"
               + "= \u06d6"
               + "= \u06d7"
               + "= \u06d8"
               + "= \u06d9"
               + "= \u06da"
               + "= \u06db"
               + "= \u06dc"
               + "= \u06dd"
               + "= \u06de"
               + "= \u06df"
               + "= \u06e0"
               + "= \u06e1"
               + "= \u06e2"
               + "= \u06e3"
               + "= \u06e4"
               + "= \u06e5"
               + "= \u06e6"
               + "= \u06e7"
               + "= \u06e8"
               + "= \u06e9"
               + "= \u06ea"
               + "= \u06eb"
               + "= \u06ec"
               + "= \u06ed"
                // Numerics
               + "& 0 < \u0660 < \u06f0"       // 0
               + "& 1 < \u0661 < \u06f1"       // 1
               + "& 2 < \u0662 < \u06f2"       // 2
               + "& 3 < \u0663 < \u06f3"       // 3
               + "& 4 < \u0664 < \u06f4"       // 4
               + "& 5 < \u0665 < \u06f5"       // 5
               + "& 6 < \u0666 < \u06f6"       // 6
               + "& 7 < \u0667 < \u06f7"       // 7
               + "& 8 < \u0668 < \u06f8"       // 8
               + "& 9 < \u0669 < \u06f9"       // 9
                // Punctuations
               + "& \u00b5 < \u060c"  // retroflex click < arabic comma
               + "< \u061b"           // ar semicolon
               + "< \u061f"           // ar question mark
               + "< \u066a"           // ar percent sign
               + "< \u066b"           // ar decimal separator
               + "< \u066c"           // ar thousand separator
               + "< \u06d4"           // ar full stop
                // Arabic script sorts after Z's
               + "&  Z <  \u0621"
               + "; \u0622"
               + "; \u0623"
               + "; \u0624"
               + "; \u0625"
               + "; \u0626"
               + "< \u0627"
               + "< \u0628"
               + "< \u067e"
               + "< \u0629"
               + "= \u062a"
               + "< \u062b"
               + "< \u062c"
               + "< \u0686"
               + "< \u062d"
               + "< \u062e"
               + "< \u062f"
               + "< \u0630"
               + "< \u0631"
               + "< \u0632"
               + "< \u0698"
               + "< \u0633"
               + "< \u0634"
               + "< \u0635"
               + "< \u0636"
               + "< \u0637"
               + "< \u0638"
               + "< \u0639"
               + "< \u063a"
               + "< \u0641"
               + "< \u0642"
               + "< \u0643"
               + "< \u06af"
               + "< \u0644"
               + "< \u0645"
               + "< \u0646"
               + "< \u0647"
               + "< \u0648"
               + "< \u0649"
               + "; \u064a"
               + "< \u0670"
               + "< \u0671"
               + "< \u0672"
               + "< \u0673"
               + "< \u0674"
               + "< \u0675"
               + "< \u0676"
               + "< \u0677"
               + "< \u0678"
               + "< \u0679"
               + "< \u067a"
               + "< \u067b"
               + "< \u067c"
               + "< \u067d"
               + "< \u067f"
               + "< \u0680"
               + "< \u0681"
               + "< \u0682"
               + "< \u0683"
               + "< \u0684"
               + "< \u0685"
               + "< \u0687"
               + "< \u0688"
               + "< \u0689"
               + "< \u068a"
               + "< \u068b"
               + "< \u068c"
               + "< \u068d"
               + "< \u068e"
               + "< \u068f"
               + "< \u0690"
               + "< \u0691"
               + "< \u0692"
               + "< \u0693"
               + "< \u0694"
               + "< \u0695"
               + "< \u0696"
               + "< \u0697"
               + "< \u0699"
               + "< \u069a"
               + "< \u069b"
               + "< \u069c"
               + "< \u069d"
               + "< \u069e"
               + "< \u069f"
               + "< \u06a0"
               + "< \u06a1"
               + "< \u06a2"
               + "< \u06a3"
               + "< \u06a4"
               + "< \u06a5"
               + "< \u06a6"
               + "< \u06a7"
               + "< \u06a8"
               + "< \u06a9"
               + "< \u06aa"
               + "< \u06ab"
               + "< \u06ac"
               + "< \u06ad"
               + "< \u06ae"
               + "< \u06b0"
               + "< \u06b1"
               + "< \u06b2"
               + "< \u06b3"
               + "< \u06b4"
               + "< \u06b5"
               + "< \u06b6"
               + "< \u06b7"
               + "< \u06ba"
               + "< \u06bb"
               + "< \u06bc"
               + "< \u06bd"
               + "< \u06be"
               + "< \u06c0"
               + "< \u06c1"
               + "< \u06c2"
               + "< \u06c3"
               + "< \u06c4"
               + "< \u06c5"
               + "< \u06c6"
               + "< \u06c7"
               + "< \u06c8"
               + "< \u06c9"
               + "< \u06ca"
               + "< \u06cb"
               + "< \u06cc"
               + "< \u06cd"
               + "< \u06ce"
               + "< \u06d0"
               + "< \u06d1"
               + "< \u06d2"
               + "< \u06d3"
               + "< \u06d5"
               + "< \u0651"
            },
        };
    }
}
