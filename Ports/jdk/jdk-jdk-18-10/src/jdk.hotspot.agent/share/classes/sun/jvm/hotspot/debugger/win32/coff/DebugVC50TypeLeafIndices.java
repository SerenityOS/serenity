/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.debugger.win32.coff;

/** <P> Enumerates the leaf indices referenced in type strings
    contained in the {@link
    sun.jvm.hotspot.debugger.win32.coff.DebugVC50SSGlobalTypes}
    subsection. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.) </P>

    <P> NOTE that these indices are specified as integers rather than
    short integers; this is to make comparisons and switches simpler
    because of Java's automatic sign extension. </P> */

public interface DebugVC50TypeLeafIndices {

  //
  // Leaf indices for type records that can be referenced from symbols:
  //

  public static final int LF_MODIFIER   = 0x1001;
  public static final int LF_POINTER    = 0x1002;
  public static final int LF_ARRAY      = 0x1003;
  public static final int LF_CLASS      = 0x1004;
  public static final int LF_STRUCTURE  = 0x1005;
  public static final int LF_UNION      = 0x1006;
  public static final int LF_ENUM       = 0x1007;
  public static final int LF_PROCEDURE  = 0x1008;
  public static final int LF_MFUNCTION  = 0x1009;
  public static final int LF_VTSHAPE    = 0x000a;
  public static final int LF_COBOL0     = 0x100a;
  public static final int LF_COBOL1     = 0x000c;
  public static final int LF_BARRAY     = 0x100b;
  public static final int LF_LABEL      = 0x000e;
  public static final int LF_NULL       = 0x000f;
  public static final int LF_NOTTRAN    = 0x0010;
  public static final int LF_DIMARRAY   = 0x100c;
  public static final int LF_VFTPATH    = 0x100d;
  public static final int LF_PRECOMP    = 0x100e;
  public static final int LF_ENDPRECOMP = 0x0014;
  public static final int LF_OEM        = 0x100f;
  public static final int LF_TYPESERVER = 0x0016;

  //
  // Leaf indices for type records that can be referenced from other type records:
  //

  public static final int LF_SKIP       = 0x1200;
  public static final int LF_ARGLIST    = 0x1201;
  public static final int LF_DEFARG     = 0x1202;
  public static final int LF_FIELDLIST  = 0x1203;
  public static final int LF_DERIVED    = 0x1204;
  public static final int LF_BITFIELD   = 0x1205;
  public static final int LF_METHODLIST = 0x1206;
  public static final int LF_DIMCONU    = 0x1207;
  public static final int LF_DIMCONLU   = 0x1208;
  public static final int LF_DIMVARU    = 0x1209;
  public static final int LF_DIMVARLU   = 0x120a;
  public static final int LF_REFSYM     = 0x020c;

  //
  // Leaf indices for fields of complex lists:
  //

  public static final int LF_BCLASS       = 0x1400;
  public static final int LF_VBCLASS      = 0x1401;
  public static final int LF_IVBCLASS     = 0x1402;
  public static final int LF_ENUMERATE    = 0x0403;
  public static final int LF_FRIENDFCN    = 0x1403;
  public static final int LF_INDEX        = 0x1404;
  public static final int LF_MEMBER       = 0x1405;
  public static final int LF_STMEMBER     = 0x1406;
  public static final int LF_METHOD       = 0x1407;
  public static final int LF_NESTTYPE     = 0x1408;
  public static final int LF_VFUNCTAB     = 0x1409;
  public static final int LF_FRIENDCLS    = 0x140a;
  public static final int LF_ONEMETHOD    = 0x140b;
  public static final int LF_VFUNCOFF     = 0x140c;
  public static final int LF_NESTTYPEEX   = 0x140d;
  public static final int LF_MEMBERMODIFY = 0x140e;

  //
  // Leaf indices for numeric fields of symbols and type records:
  //

  public static final int LF_NUMERIC    = 0x8000;
  public static final int LF_CHAR       = 0x8000;
  public static final int LF_SHORT      = 0x8001;
  public static final int LF_USHORT     = 0x8002;
  public static final int LF_LONG       = 0x8003;
  public static final int LF_ULONG      = 0x8004;
  public static final int LF_REAL32     = 0x8005;
  public static final int LF_REAL64     = 0x8006;
  public static final int LF_REAL80     = 0x8007;
  public static final int LF_REAL128    = 0x8008;
  public static final int LF_QUADWORD   = 0x8009;
  public static final int LF_UQUADWORD  = 0x800a;
  public static final int LF_REAL48     = 0x800b;
  public static final int LF_COMPLEX32  = 0x800c;
  public static final int LF_COMPLEX64  = 0x800d;
  public static final int LF_COMPLEX80  = 0x800e;
  public static final int LF_COMPLEX128 = 0x800f;
  public static final int LF_VARSTRING  = 0x8010;

  public static final int LF_PAD0       = 0xf0;
  public static final int LF_PAD1       = 0xf1;
  public static final int LF_PAD2       = 0xf2;
  public static final int LF_PAD3       = 0xf3;
  public static final int LF_PAD4       = 0xf4;
  public static final int LF_PAD5       = 0xf5;
  public static final int LF_PAD6       = 0xf6;
  public static final int LF_PAD7       = 0xf7;
  public static final int LF_PAD8       = 0xf8;
  public static final int LF_PAD9       = 0xf9;
  public static final int LF_PAD10      = 0xfa;
  public static final int LF_PAD11      = 0xfb;
  public static final int LF_PAD12      = 0xfc;
  public static final int LF_PAD13      = 0xfd;
  public static final int LF_PAD14      = 0xfe;
  public static final int LF_PAD15      = 0xff;
}
