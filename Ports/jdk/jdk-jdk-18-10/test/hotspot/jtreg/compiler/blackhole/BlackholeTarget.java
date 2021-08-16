/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

package compiler.blackhole;

import java.lang.reflect.*;

public class BlackholeTarget {
    public void call_for_null_check() {}

    public        void    bh_i_boolean_0()           {}
    public        void    bh_i_byte_0()              {}
    public        void    bh_i_short_0()             {}
    public        void    bh_i_char_0()              {}
    public        void    bh_i_int_0()               {}
    public        void    bh_i_float_0()             {}
    public        void    bh_i_long_0()              {}
    public        void    bh_i_double_0()            {}
    public        void    bh_i_Object_0()            {}

    public static void    bh_s_boolean_0()           {}
    public static void    bh_s_byte_0()              {}
    public static void    bh_s_short_0()             {}
    public static void    bh_s_char_0()              {}
    public static void    bh_s_int_0()               {}
    public static void    bh_s_float_0()             {}
    public static void    bh_s_long_0()              {}
    public static void    bh_s_double_0()            {}
    public static void    bh_s_Object_0()            {}

    public static void    bh_s_boolean_1(boolean v)  {}
    public static void    bh_s_byte_1(byte v)        {}
    public static void    bh_s_short_1(short v)      {}
    public static void    bh_s_char_1(char v)        {}
    public static void    bh_s_int_1(int v)          {}
    public static void    bh_s_float_1(float v)      {}
    public static void    bh_s_long_1(long v)        {}
    public static void    bh_s_double_1(double v)    {}
    public static void    bh_s_Object_1(Object v)    {}

    public static void    bh_s_boolean_1_delegate(boolean v)  { bh_s_boolean_1(v); }
    public static void    bh_s_byte_1_delegate(byte v)        { bh_s_byte_1(v);    }
    public static void    bh_s_short_1_delegate(short v)      { bh_s_short_1(v);   }
    public static void    bh_s_char_1_delegate(char v)        { bh_s_char_1(v);    }
    public static void    bh_s_int_1_delegate(int v)          { bh_s_int_1(v);     }
    public static void    bh_s_float_1_delegate(float v)      { bh_s_float_1(v);   }
    public static void    bh_s_long_1_delegate(long v)        { bh_s_long_1(v);    }
    public static void    bh_s_double_1_delegate(double v)    { bh_s_double_1(v);  }
    public static void    bh_s_Object_1_delegate(Object v)    { bh_s_Object_1(v);  }

    public static void    bh_s_boolean_2(boolean v1, boolean v2) {}
    public static void    bh_s_byte_2(byte v1, byte v2)          {}
    public static void    bh_s_short_2(short v1, short v2)       {}
    public static void    bh_s_char_2(char v1, char v2)          {}
    public static void    bh_s_int_2(int v1, int v2)             {}
    public static void    bh_s_float_2(float v1, float v2)       {}
    public static void    bh_s_long_2(long v1, long v2)          {}
    public static void    bh_s_double_2(double v1, double v2)    {}
    public static void    bh_s_Object_2(Object v1, Object v2)    {}

    public static boolean bh_sr_boolean(boolean v) { return false; }
    public static byte    bh_sr_byte(byte v)       { return 0;     }
    public static short   bh_sr_short(short v)     { return 0;     }
    public static char    bh_sr_char(char v)       { return 0;     }
    public static int     bh_sr_int(int v)         { return 0;     }
    public static float   bh_sr_float(float v)     { return 0;     }
    public static long    bh_sr_long(long v)       { return 0;     }
    public static double  bh_sr_double(double v)   { return 0;     }
    public static Object  bh_sr_Object(Object v)   { return null;  }
}
