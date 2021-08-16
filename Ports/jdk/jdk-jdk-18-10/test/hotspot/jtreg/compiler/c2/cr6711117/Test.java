/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6711117
 * @summary Assertion in 64bit server vm (flat != TypePtr::BOTTOM,"cannot alias-analyze an untyped ptr")
 *
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000
 *                   -XX:+UseCompressedOops compiler.c2.cr6711117.Test
 */

package compiler.c2.cr6711117;

final class Test_Class_0 {
    final static char var_1 = 'E';
    short var_2 = 16213;
    final static String var_3 = "jiiibmmsk";


    public Test_Class_0()
    {
        var_2 ^= 'M';
        final String var_18 = var_3;
        var_2--;
        var_2 |= (byte)('D' / (byte)var_2) - ((byte)1.6680514E38F << + ((byte)'O') & 7320241275829036032L);
        func_2(((!false & false | false ? true : false) ? true : true | !true) ? var_2 : 834513107);
        var_2 >>>= var_1;
        "smiosoebk".codePointCount(true ^ (false ^ ! !false) ? (byte)- ((byte)430513598) : + ((byte)'_'), ~ (true ? (byte)']' : (byte)-2.8272547997066827E307));
        var_2 -= true ? var_1 : var_1;
        var_2 ^= var_1;
        var_2 &= (var_2 |= ~ ((byte)(var_2 *= var_2)));
        long var_19 = 0L;
        short var_20 = var_2 += 'P';
        while (var_19 < 1)
        {
            var_2 ^= true ? (byte)- +1.2219539475209E308 : (byte)1.2748408476894178E308;
            var_19++;
            var_2 = (byte)((1489358000 == (var_20 | 7816908224315289600L) ? var_1 : var_1) ^ var_19);
            var_20--;
        }
        var_20 -= 'f';
        var_20 <<= (((new Test_Class_0[(byte)var_20])[(byte)var_2]).var_2 *= false ? 'g' : 'x');
    }




    static float func_0()
    {
        ((new Test_Class_0[(byte)7.774490796987995E307])[(byte)'v']).var_2 <<= false ^ !false ? (short)'v' : "".codePointCount(594464985, 579036736);
        ((new Test_Class_0[(byte)(((new Test_Class_0[(byte)1361657519])[(byte)2.3703713E38F]).var_2-- - (short)3.5589388134844986E307)])[((true ? !true : false) ^ (!false ? true : !true) ? !false : false) ? (byte)7.047289E37F : (byte)- ((byte)2.6620062118475144E307)]).var_2 *= 3273943364390983680L;
        --((new Test_Class_0[false ? (byte)(short)1.4965069E36F : (byte)286322022])[(byte)- ((byte)2.742619E38F)]).var_2;
        long var_4;
        {
            double var_5;
        }
        var_4 = (byte)1.3509231E38F;
        ((new Test_Class_0[(byte)'_'])[('g' | 1427123046096105472L) < var_1 >> (byte)(int)(byte)7697616672011068416L ? (byte)var_1 : (byte)1251856579]).var_2--;
        switch (--((new Test_Class_0[(byte)5.0656327E37F])[(byte)'e']).var_2 != ++((new Test_Class_0[(byte)(int)1.3728667270920175E308])[(byte)+ + -1.6338179407381788E308]).var_2 | !var_3.equalsIgnoreCase("iiwwwln") ? (false ? (byte)1.8291216E38F : (byte)4.778575546584698E307) : (byte)1048254181)
        {
            case 99:

        }
        {
            byte var_6 = 13;
        }
        var_4 = --((new Test_Class_0[!var_3.endsWith("qaoioore") ^ false ? (byte)2.827362738392923E307 : (byte)~4890175967151316992L])[(byte)(short)var_1]).var_2;
        ++((new Test_Class_0[(byte)(1.0075552E38F + (short)2083553541)])[(byte)(short)(byte)(short)1.6872205E38F]).var_2;
        return ((new Test_Class_0[(byte)var_1])[(byte)+ +5760973323384750080L]).var_2 - (false ? (byte)'i' : (var_4 = (short)1.2458781351126844E308) + 2.131006E38F);
    }

    public static long func_1(String arg_0, Object arg_1, final long arg_2)
    {
        arg_0 = false ? arg_0 : "fgbrpgsq";
        ((new Test_Class_0[(byte)- ((byte)']')])[false ? (byte)757239006 : (byte)1866002020]).var_2 ^= (short)(true ? (byte)(((new Test_Class_0[(byte)1416194866])[(byte)1.2309887362692395E308]).var_2 >>= (int)~ ~ ~arg_2) : (byte)5804970709284726784L);
        final long var_7 = (long)(- + ((long)+ - + - -2.5396583E38F) - - +1.8770165E38F % 2472404173160781824L < --((new Test_Class_0[(byte)5.569360482341752E307])[(byte)(double)(byte)8131142397821553664L]).var_2 ^ true ? (false ? (byte)- -1.163275451591927E308 : (byte)var_1) : (false ? (byte)1843746036 : (byte)1.0209668642291047E308));
        arg_0 = (arg_0 = arg_0.substring(699480935));
        switch (((new Test_Class_0[(byte)(5415649243316856832L >> 861936806)])[true | true & !false ? (byte)(short)- -7.785169683394908E307 : (byte)+ ((byte)arg_2)]).var_2++)
        {
            case 42:

            case 102:

        }
        arg_1 = (true || false ? false : true) ? (arg_0 = (arg_0 = "jbfaru")) : arg_0;
        arg_1 = new byte[(byte)2.669957E38F];
        boolean var_8 = ! ((false ? (short)1.4259420861834744E308 : (short)7.352115508157158E307) != 1.7635658130722812E308);
        arg_1 = new Object[(byte)- ((byte)(short)1.8950693E38F)];
        arg_0 = arg_0;
        return (byte)1.4762239057269886E308 & 4923938844759802880L;
    }

    double[][] func_2(final int arg_0)
    {
        var_2 >>>= (var_2 >>= var_2++);
        float var_9 = 0F;
        var_2 %= var_2;
        do
        {
            ++var_2;
            var_9++;
            var_2++;
        } while (true && (var_9 < 1 && false));
        double var_10 = 0;
        final int var_11 = 11903395;
        do
        {
            --var_2;
            var_10++;
            ++var_2;
        } while ((false & true || false) && (var_10 < 2 && ~ ((byte)'[') == (byte)(1.1943192E38F % ('c' << var_1) % (byte)((var_2 |= var_2) + 591679039 / ~5932100696448264192L))));
        String var_12 = "jkwnk";
        var_12 = var_3;
        var_12 = (var_12 = (var_12 = var_3));
        var_12 = "qrhdwx";
        var_12 = var_12;
        short var_13 = (true && true) ^ true | ! (!true || 1646418779 <= (byte)var_1) ? var_2 : var_2;
        return new double[(byte)var_1][true || false ^ !true ^ true ? (byte)arg_0 : (byte)var_10];
    }

    private final int func_3()
    {
        long var_14 = 's' * (~ ~6656240461354863616L * 3151744928387344384L) << ~ (((var_2 >>>= 6600935261424147456L) % 1798503219359364096L | - ~3832249967647077376L / - ((byte)~1529201870915276800L)) / var_2);
        {
            var_14 |= !false | (byte)1078230528 >= (byte)1.3972878565417081E308 | (true | !true & !true & !false) ? var_1 : '_';
        }
        long var_15 = 7589204885152164864L;
        var_2 ^= (var_1 < (byte)'r' ? 475314139 : 'Z') <= 1943074698 ? 'h' : var_1;
        return 'V' * (false ? (byte)5.498204E37F : (byte)1.0137001669765466E308);
    }

    protected static boolean func_4(boolean arg_0, byte arg_1, boolean arg_2)
    {
        arg_1++;
        arg_1 &= (((((new Test_Class_0[arg_1][arg_1][arg_1])[arg_1])[arg_1])[arg_1]).var_2 |= arg_2 ? (short)~3038084056596854784L : (short)+ (arg_1 = arg_1));
        arg_0 |= true;
        arg_1 %= (arg_1 |= ((new Test_Class_0[arg_1])[arg_1]).var_2--);
        if (false)
        {
            arg_0 |= arg_2;
        }
        else
        {
            ++(((new Test_Class_0[arg_1][arg_1][arg_1])[arg_1 += var_1])[(!arg_2 | (arg_0 &= false)) ^ (arg_0 | arg_0) ? arg_1 : (arg_1 <<= 3192041751921364992L)][arg_1 /= arg_1]).var_2;
        }
        arg_1 &= +(new byte[arg_1])[arg_1];
        arg_1 <<= 3632133838014908416L;
        byte[] var_16 = (new byte[arg_1][arg_1--])[arg_1];
        long var_17;
        arg_1 ^= ~ arg_1--;
        arg_0 ^= (arg_2 ^= 1186877294 >= ((new Test_Class_0[arg_1][arg_1])[arg_1][arg_1]).var_2) & arg_2;
        return var_3.startsWith(var_3);
    }

    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_0.var_2 = "; result += Test.Printer.print(var_2);
        result += "\n";
        result += "Test_Class_0.var_1 = "; result += Test.Printer.print(var_1);
        result += "\n";
        result += "Test_Class_0.var_3 = "; result += Test.Printer.print(var_3);
        result += "";
        result += "\n]";
        return result;
    }
}


class Test_Class_1 {
    static int var_21 = 670918363;
    final float var_22 = 8.650798E37F;
    static int var_23 = 1774228457;
    final int var_24 = 1282736974;
    final byte var_25 = !false & false | true ? (byte)7.677121016144275E307 : (byte)'r';
    static long var_26 = 2939310115459338240L;
    final long var_27 = var_25 - 7555453173456381952L;
    double var_28;
    static String var_29;


    public Test_Class_1()
    {
        var_29 = Test_Class_0.var_3;
        ((false ? false || ! !true : ! (! !true & !true)) ? new Test_Class_0() : new Test_Class_0()).var_2++;
        var_23 -= 2.963694E38F;
    }




    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_1.var_21 = "; result += Test.Printer.print(var_21);
        result += "\n";
        result += "Test_Class_1.var_23 = "; result += Test.Printer.print(var_23);
        result += "\n";
        result += "Test_Class_1.var_24 = "; result += Test.Printer.print(var_24);
        result += "\n";
        result += "Test_Class_1.var_26 = "; result += Test.Printer.print(var_26);
        result += "\n";
        result += "Test_Class_1.var_27 = "; result += Test.Printer.print(var_27);
        result += "\n";
        result += "Test_Class_1.var_28 = "; result += Test.Printer.print(var_28);
        result += "\n";
        result += "Test_Class_1.var_22 = "; result += Test.Printer.print(var_22);
        result += "\n";
        result += "Test_Class_1.var_25 = "; result += Test.Printer.print(var_25);
        result += "\n";
        result += "Test_Class_1.var_29 = "; result += Test.Printer.print(var_29);
        result += "";
        result += "\n]";
        return result;
    }
}


class Test_Class_2 {
    double var_30;
    static byte var_31;
    static char var_32;
    float var_33;
    double var_34 = !false & (true ? true : ! !true && false) ? 'q' - 4789231433793305600L - (var_33 = -1.0677024E38F) : 2.65473560313378E307;
    final double var_35 = ~Test_Class_1.var_26 == 5.145660681364723E307 | false ? 1.4134775E38F : 1.77223030708671E308;
    final int var_36 = Test_Class_1.var_23 |= Test_Class_1.var_21++;


    public Test_Class_2()
    {
        Test_Class_0.var_3.replace(Test_Class_0.var_1, 'Q');
        var_32 = (var_32 = (var_32 = '_'));
        Test_Class_1.var_26 |= Test_Class_0.var_1;
        Test_Class_1.var_29 = (Test_Class_1.var_29 = Test_Class_0.var_3);
        var_32 = Test_Class_0.var_1;
        var_33 = ((new Test_Class_0[(byte)851412948463452160L])[var_31 = new Test_Class_1().var_25]).var_2;
        var_33 = ! (((!false | false) & (false || !true) ? false : ! !false) | false) ? new Test_Class_1().var_25 : (var_31 = new Test_Class_1().var_25);
        float var_38 = 0F;
        var_34 /= 5336005797857974272L;
        for ("ccnyq".endsWith((new String[(byte)Test_Class_1.var_26])[var_31 = (var_31 = (var_31 = (byte)4.7927775E37F))]); var_38 < 2; var_32 = '^' <= Test_Class_0.var_1 ^ true ? (var_32 = Test_Class_0.var_1) : (var_32 = 'V'))
        {
            var_32 = true ? 'a' : (var_32 = Test_Class_0.var_1);
            var_38++;
            var_33 = new Test_Class_1().var_24;
            var_32 = ! (true || true ? !false : (short)3.2844383E37F < 2.1400662E38F) ? (char)1.2691096999143248E308 : (! !false ^ true ? 's' : 'q');
        }
        var_32 = 'B';
        {
            var_32 = Test_Class_0.var_1;
        }
        var_32 = Test_Class_0.var_1;
        Test_Class_1.var_29 = "ov";
        Test_Class_1.var_29 = "smtolghw";
    }





    protected final static String func_0(final long[][] arg_0, byte arg_1, char arg_2)
    {
        arg_1 <<= (((new Test_Class_2[arg_1])[arg_1]).var_34 > new Test_Class_0().var_2 | true ? new Test_Class_0() : (new Test_Class_0[arg_1][arg_1])[new Test_Class_1().var_25][new Test_Class_1().var_25]).var_2;
        Test_Class_1.var_26 >>>= (!true | !true | (new boolean[arg_1])[arg_1] || true ? (new Test_Class_1[arg_1])[arg_1] : new Test_Class_1()).var_27;
        float var_37 = 0F;
        arg_2 >>= ((new Test_Class_1[arg_1][arg_1])[arg_1][arg_1]).var_25;
        do
        {
            ((new Test_Class_2[arg_1 /= 2055714081])[arg_1]).var_34 = 'l';
            var_37++;
            Test_Class_1.var_29 = Test_Class_0.var_3;
        } while ((false ? false : false) && var_37 < 7);
        Test_Class_1.var_29 = Test_Class_0.var_3 + "";
        ((new Test_Class_2[new Test_Class_1().var_25][new Test_Class_1().var_25])[new Test_Class_1().var_25][arg_1 |= new Test_Class_0().var_2]).var_34 += Test_Class_0.var_1;
        return "esb";
    }

    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_2.var_32 = "; result += Test.Printer.print(var_32);
        result += "\n";
        result += "Test_Class_2.var_36 = "; result += Test.Printer.print(var_36);
        result += "\n";
        result += "Test_Class_2.var_30 = "; result += Test.Printer.print(var_30);
        result += "\n";
        result += "Test_Class_2.var_34 = "; result += Test.Printer.print(var_34);
        result += "\n";
        result += "Test_Class_2.var_35 = "; result += Test.Printer.print(var_35);
        result += "\n";
        result += "Test_Class_2.var_33 = "; result += Test.Printer.print(var_33);
        result += "\n";
        result += "Test_Class_2.var_31 = "; result += Test.Printer.print(var_31);
        result += "";
        result += "\n]";
        return result;
    }
}


final class Test_Class_3 extends Test_Class_2 {
    byte var_39 = 23;
    static boolean var_40 = false;


    public Test_Class_3()
    {
        if (true)
        {
            Test_Class_1.var_21 |= new Test_Class_1().var_27;
        }
        else
        {
            final float var_46 = 7.9266674E37F;
            ++Test_Class_1.var_26;
        }
        {
            Test_Class_1.var_23++;
        }
        var_30 = ((new Test_Class_1[var_39][var_39])[var_39][var_39]).var_25;
        if (var_40 &= (var_40 |= (var_40 |= var_40)))
        {
            Test_Class_0.var_3.indexOf(Test_Class_1.var_29 = "xfgyblg", 'X' >>> ((Test_Class_1)(new Object[var_39])[((new Test_Class_1[var_39])[var_39]).var_25]).var_27);
        }
        else
        {
            var_40 &= var_40 && var_40;
        }
        ((Test_Class_2)(((new boolean[var_39])[var_39++] ? (var_40 &= var_40) : (var_40 &= false)) ? (new Test_Class_2[var_39][var_39])[var_39][var_39] : (new Object[var_39][var_39])[var_39][var_39])).var_33 = (var_40 ? new Test_Class_1() : new Test_Class_1()).var_25;
        switch (var_39)
        {
            case 24:

        }
        var_39 += (((var_40 ^= true) ? new Test_Class_0() : new Test_Class_0()).var_2 ^= var_40 & (var_40 | false) ? var_39-- : var_36);
        new Test_Class_0().var_2 %= (new Test_Class_0().var_2 += (var_39 ^= Test_Class_1.var_26));
    }




    private static String func_0()
    {
        --Test_Class_1.var_26;
        {
            Test_Class_1.var_29 = var_40 ? Test_Class_0.var_3 : "rahqjhqf";
        }
        if (var_40 ^= var_40)
        {
            Test_Class_1.var_26 >>= (Test_Class_2.var_32 = Test_Class_0.var_1) / new Test_Class_0().var_2;
        }
        else
        {
            ++Test_Class_1.var_21;
        }
        ++Test_Class_1.var_26;
        int var_41 = 0;
        ++Test_Class_1.var_26;
        do
        {
            var_40 = (var_40 = true);
            var_41++;
            Test_Class_0 var_42 = new Test_Class_0();
        } while (var_41 < 1);
        Test_Class_1.var_29 = "f";
        Test_Class_1 var_43;
        var_43 = (var_43 = new Test_Class_1());
        Test_Class_2.var_32 = 'V';
        long var_44 = 0L;
        Test_Class_1.var_23--;
        while (var_40 && (var_44 < 1 && var_40))
        {
            Test_Class_1.var_29 = "bsgewkmk";
            var_44++;
            Test_Class_1.var_29 = "ktegattny";
            var_40 &= var_40 ^ (var_40 |= (short)4.4487427E37F < 'n') & true;
        }
        Test_Class_1.var_23 %= (((var_40 |= true & (var_40 &= var_40)) ^ true ? new Test_Class_0() : new Test_Class_0()).var_2 -= 1.6638270827800162E308);
        float var_45;
        var_32 = (Test_Class_2.var_32 = Test_Class_0.var_1);
        return false ? "fluk" : "wt";
    }

    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_3.var_32 = "; result += Test.Printer.print(var_32);
        result += "\n";
        result += "Test_Class_3.var_36 = "; result += Test.Printer.print(var_36);
        result += "\n";
        result += "Test_Class_3.var_30 = "; result += Test.Printer.print(var_30);
        result += "\n";
        result += "Test_Class_3.var_34 = "; result += Test.Printer.print(var_34);
        result += "\n";
        result += "Test_Class_3.var_35 = "; result += Test.Printer.print(var_35);
        result += "\n";
        result += "Test_Class_3.var_33 = "; result += Test.Printer.print(var_33);
        result += "\n";
        result += "Test_Class_3.var_31 = "; result += Test.Printer.print(var_31);
        result += "\n";
        result += "Test_Class_3.var_39 = "; result += Test.Printer.print(var_39);
        result += "\n";
        result += "Test_Class_3.var_40 = "; result += Test.Printer.print(var_40);
        result += "";
        result += "\n]";
        return result;
    }
}


class Test_Class_4 {
    final float var_47 = 1.9043434E38F;
    final byte var_48 = 32;
    final float var_49 = 2.8176504E38F;
    final char var_50 = 'r';
    final String var_51 = "uwgmnjpg";
    static int var_52;
    short[] var_53;
    Test_Class_1 var_54;


    public Test_Class_4()
    {
        final float var_55 = (3.1554042E38F == var_50 ^ (Test_Class_3.var_40 |= true) ? (Test_Class_3.var_40 ^= Test_Class_3.var_40) ^ true : Test_Class_3.var_40) ? new Test_Class_0().var_2 : 2.965321E38F;
        new Test_Class_0().var_2 = (new Test_Class_0().var_2 >>= +new Test_Class_1().var_25);
        ((Test_Class_1.var_29 = (Test_Class_1.var_29 = (Test_Class_1.var_29 = "l"))) + "").equalsIgnoreCase(Test_Class_1.var_29 = "garnio");
        double var_56 = 0;
        Test_Class_1.var_29 = var_51;
        while (var_56 < 1)
        {
            ((Test_Class_3)(Test_Class_2)(new Object[var_48])[var_48]).var_33 = ++Test_Class_1.var_26;
            var_56++;
            Test_Class_1.var_29 = (Test_Class_1.var_29 = "fvyjrih");
            float[] var_57;
        }
        {
            ((new Test_Class_2[var_48])[((new Test_Class_3[var_48][var_48])[var_48][var_48]).var_39]).var_34 *= 2.2119221943262553E307;
            Test_Class_2.var_32 = true ? 'q' : 't';
            ((new Test_Class_3[--((Test_Class_3)new Test_Class_2()).var_39])[var_48]).var_33 = new Test_Class_0().var_2;
            int var_58 = 'i' >> (var_48 << Test_Class_0.var_1);
        }
        Test_Class_3.var_40 &= true && var_51.equalsIgnoreCase(var_51) || new Test_Class_0().var_2 < --((new Test_Class_3[var_48])[var_48]).var_39;
        ((Test_Class_3)(Test_Class_2)(new Object[var_48][var_48])[var_48][var_48]).var_34 += Test_Class_1.var_26--;
        var_54 = new Test_Class_1();
        Test_Class_3.var_40 |= (long)(!true ^ var_47 > ((Test_Class_2)(new Object[var_48])[var_48]).var_34 ? (Test_Class_2.var_31 = (Test_Class_3.var_31 = (Test_Class_3.var_31 = var_48))) : (var_54 = new Test_Class_1()).var_25) <= var_48;
        (Test_Class_3.var_40 ? (true ? new Test_Class_0() : new Test_Class_0()) : new Test_Class_0()).var_2 &= var_48;
        (Test_Class_3.var_40 ? (Test_Class_3)new Test_Class_2() : (new Test_Class_3[var_48][var_48])[var_48][var_48]).var_34 += Test_Class_1.var_21;
        Test_Class_3 var_59;
        Test_Class_2.var_32 = 'H';
        --Test_Class_1.var_26;
    }





    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_4.var_50 = "; result += Test.Printer.print(var_50);
        result += "\n";
        result += "Test_Class_4.var_52 = "; result += Test.Printer.print(var_52);
        result += "\n";
        result += "Test_Class_4.var_53 = "; result += Test.Printer.print(var_53);
        result += "\n";
        result += "Test_Class_4.var_47 = "; result += Test.Printer.print(var_47);
        result += "\n";
        result += "Test_Class_4.var_49 = "; result += Test.Printer.print(var_49);
        result += "\n";
        result += "Test_Class_4.var_48 = "; result += Test.Printer.print(var_48);
        result += "\n";
        result += "Test_Class_4.var_51 = "; result += Test.Printer.print(var_51);
        result += "\n";
        result += "Test_Class_4.var_54 = "; result += Test.Printer.print(var_54);
        result += "";
        result += "\n]";
        return result;
    }
}


class Test_Class_5 extends Test_Class_4 {
    char var_60 = '_';
    final byte var_61 = 101;


    public Test_Class_5()
    {
        Test_Class_0.var_3.indexOf(Test_Class_1.var_21, (Test_Class_3.var_40 |= Test_Class_3.var_40) ? new Test_Class_1().var_24 : 'i');
    }




    final char func_0(Test_Class_1 arg_0, final Test_Class_1 arg_1)
    {
        long var_62 = 0L;
        "aoal".toLowerCase();
        for (byte var_63 = arg_0.var_25; var_62 < 1 && "ji".startsWith("dikrs".endsWith("va") ? (Test_Class_1.var_29 = "mvp") : Test_Class_0.var_3, Test_Class_1.var_23); ((Test_Class_2)(new Object[arg_0.var_25])[var_63]).var_34 -= new Test_Class_2().var_36)
        {
            ((Test_Class_3.var_40 ? false : Test_Class_3.var_40) ? (Test_Class_0)(new Object[arg_1.var_25][arg_1.var_25])[arg_1.var_25][var_63] : (Test_Class_0)(new Object[var_48][var_48])[var_63][var_63]).var_2 += true ^ Test_Class_3.var_40 ^ (((new Test_Class_3[var_63][var_63])[var_63][var_61]).var_35 != 2.1423512E38F | ! !false) ? var_49 + ~var_48 : 3.1549515E38F;
            var_62++;
            (!false & ((Test_Class_3.var_40 |= (Test_Class_3.var_40 ^= true)) & true) ? (Test_Class_2)(new Object[var_63])[var_63] : (new Test_Class_2[var_63][var_61])[var_63][arg_0.var_25]).var_33 = (var_60 *= (var_60 *= ((new Test_Class_3[var_48][var_61])[var_61][var_63]).var_35));
            float var_64;
        }
        Test_Class_1.var_29 = "xyenjknu";
        Test_Class_3.var_40 ^= (Test_Class_3.var_40 = !false & true) ? Test_Class_3.var_40 : Test_Class_3.var_40;
        ((new Test_Class_2[var_48][arg_1.var_25])[arg_0.var_25][var_48]).var_33 = var_61;
        Test_Class_1.var_21 |= --(((new Test_Class_3[Test_Class_3.var_31 = arg_0.var_25][var_61])[var_61])[(((new Test_Class_3[var_48][var_61])[var_48])[((Test_Class_3)(new Test_Class_2[var_48][arg_0.var_25])[var_61][var_48]).var_39]).var_39 >>>= var_60]).var_39;
        var_51.compareToIgnoreCase("hgcaybk");
        Test_Class_0 var_65 = (Test_Class_1.var_29 = "t").codePointBefore(1602805584) >= (float)((new Test_Class_3[var_48][var_61])[var_48][Test_Class_2.var_31 = arg_1.var_25]).var_39 - 7.256386549028811E307 ? new Test_Class_0() : ((new Test_Class_0[arg_0.var_25][var_48][var_48])[arg_0.var_25])[arg_0.var_25][Test_Class_2.var_31 = arg_1.var_25];
        return 'U';
    }

    protected static Test_Class_1 func_1(final short arg_0, long arg_1)
    {
        --new Test_Class_0().var_2;
        "xb".length();
        if ((Test_Class_3.var_40 ^= (Test_Class_2.var_32 = Test_Class_0.var_1) == 1.2609472E38F) ? (Test_Class_3.var_40 = (Test_Class_3.var_40 = Test_Class_3.var_40)) : true)
        {
            --Test_Class_1.var_26;
        }
        else
        {
            "ybbe".substring(209378562, var_52 = (Test_Class_1.var_21 |= (Test_Class_2.var_31 = (byte)'a')));
        }
        Test_Class_3.var_40 &= (Test_Class_3.var_40 &= true) && (Test_Class_1.var_29 = (Test_Class_1.var_29 = Test_Class_0.var_3)).endsWith(Test_Class_0.var_3);
        (false ? new Test_Class_0() : new Test_Class_0()).var_2 >>= new Test_Class_1().var_25;
        return 9.430116214455637E307 <= (true ? (Test_Class_3)new Test_Class_2() : (Test_Class_3)new Test_Class_2()).var_34 ? new Test_Class_1() : new Test_Class_1();
    }

    public String toString()
    {
        String result =  "[\n";
        result += "Test_Class_5.var_50 = "; result += Test.Printer.print(var_50);
        result += "\n";
        result += "Test_Class_5.var_60 = "; result += Test.Printer.print(var_60);
        result += "\n";
        result += "Test_Class_5.var_52 = "; result += Test.Printer.print(var_52);
        result += "\n";
        result += "Test_Class_5.var_53 = "; result += Test.Printer.print(var_53);
        result += "\n";
        result += "Test_Class_5.var_47 = "; result += Test.Printer.print(var_47);
        result += "\n";
        result += "Test_Class_5.var_49 = "; result += Test.Printer.print(var_49);
        result += "\n";
        result += "Test_Class_5.var_48 = "; result += Test.Printer.print(var_48);
        result += "\n";
        result += "Test_Class_5.var_61 = "; result += Test.Printer.print(var_61);
        result += "\n";
        result += "Test_Class_5.var_51 = "; result += Test.Printer.print(var_51);
        result += "\n";
        result += "Test_Class_5.var_54 = "; result += Test.Printer.print(var_54);
        result += "";
        result += "\n]";
        return result;
    }
}

public class Test {
    Test_Class_4 var_66;
    Test_Class_3 var_67;
    Test_Class_5 var_68;
    Test_Class_2[] var_69;
    long var_70 = ++Test_Class_1.var_26 & Test_Class_1.var_21++;
    final static double var_71 = 3.566207721984698E307;
    static boolean var_72;
    final static String var_73 = "nmxx";


    private final char func_0(Test_Class_3 arg_0, final boolean[] arg_1)
    {
        ((Test_Class_5)(arg_1[arg_0.var_39++] ? new Test_Class_2[(var_67 = arg_0).var_39] : (new Object[arg_0.var_39])[arg_0.var_39])).var_54 = new Test_Class_1();
        new Test_Class_0();
        (((new Test[arg_0.var_39][arg_0.var_39][arg_0.var_39])[++arg_0.var_39])[arg_0.var_39][arg_0.var_39]).var_66 = (var_68 = (new Test_Class_5[arg_0.var_39][arg_0.var_39])[arg_0.var_39][arg_0.var_39]);
        ((new Test[arg_0.var_39])[(arg_0 = (var_67 = (arg_0 = arg_0))).var_39]).var_70 = ((new long[arg_0.var_39][arg_0.var_39])[arg_0.var_39])[arg_0.var_39 = ((var_67 = (arg_0 = arg_0)).var_39 -= new Test_Class_0().var_2)] << ']';
        arg_0 = (new Test_Class_0().var_2 *= ((new Test_Class_2[arg_0.var_39])[arg_0.var_39]).var_34) >= arg_0.var_39 ? (var_67 = arg_0) : (arg_0 = arg_0);
        Test_Class_1.var_26--;
        Test_Class_4 var_74 = var_66 = (Test_Class_5)(new Test_Class_4[arg_0.var_39])[arg_0.var_39];
        Test_Class_3.var_40 ^= ! (Test_Class_3.var_40 &= (Test_Class_3.var_40 ^= Test_Class_3.var_40) | (Test_Class_3.var_40 &= Test_Class_3.var_40));
        var_72 = (arg_1[(var_67 = arg_0).var_39] | !Test_Class_3.var_40 & !Test_Class_3.var_40 ? (Test_Class_1.var_29 = var_73).endsWith((var_66 = var_74).var_51) && (Test_Class_3.var_40 ^= Test_Class_3.var_40) : (Test_Class_3.var_40 ^= Test_Class_3.var_40)) ^ !Test_Class_3.var_40;
        Test_Class_3.var_40 &= (Test_Class_3.var_40 &= (Test_Class_3.var_40 = Test_Class_3.var_40) & Test_Class_3.var_40 ^ Test_Class_3.var_40);
        arg_0.var_39 -= --var_70;
        int var_75;
        double var_76;
        {
            boolean var_77;
            var_70 ^= new Test_Class_0().var_2++;
        }
        Test_Class_1.var_26 /= Test_Class_0.var_3.lastIndexOf(~new Test_Class_1().var_25, Test_Class_1.var_21);
        Test_Class_1.var_26 |= Test_Class_1.var_21;
        (((new Test_Class_3[arg_0.var_39][arg_0.var_39][var_74.var_48])[arg_0.var_39])[arg_0.var_39][arg_0.var_39]).var_34 %= (var_67 = arg_0).var_39;
        Test_Class_1.var_21 &= arg_0.var_39;
        var_68 = (var_68 = (Test_Class_5)var_74);
        var_72 = false;
        return new Test_Class_5().var_60 ^= 'v';
    }

    public static Test_Class_2 func_1(byte[][] arg_0, final int arg_1, Test_Class_1 arg_2, final Test_Class_1 arg_3)
    {
        ((new Test[arg_3.var_25])[((Test_Class_3)new Test_Class_2()).var_39 *= --Test_Class_1.var_26]).var_67 = (((new Test[arg_2.var_25])[(((new Test[arg_2.var_25][arg_2.var_25])[arg_3.var_25][arg_3.var_25]).var_67 = (new Test_Class_3[arg_2.var_25][arg_2.var_25])[arg_2.var_25][arg_3.var_25]).var_39 %= Test_Class_1.var_26]).var_67 = (((new Test[arg_3.var_25][arg_2.var_25])[arg_3.var_25][arg_2.var_25]).var_67 = (((new Test[arg_3.var_25])[arg_2.var_25]).var_67 = (Test_Class_3)new Test_Class_2())));
        {
            --Test_Class_1.var_26;
        }
        if (!Test_Class_3.var_40)
        {
            "jfqj".replaceAll("ac", Test_Class_0.var_3);
        }
        else
        {
            arg_2 = (((new Test_Class_5[arg_3.var_25][arg_2.var_25])[((new Test_Class_3[arg_2.var_25])[arg_3.var_25]).var_39][((Test_Class_3)(new Test_Class_2[arg_2.var_25])[arg_3.var_25]).var_39]).var_54 = arg_3);
            new Test_Class_1();
        }
        if (true)
        {
            Test_Class_0.func_0();
        }
        else
        {
            Test_Class_1.var_23 /= Test_Class_1.var_26;
        }
        Test_Class_1.var_26--;
        Test_Class_1.var_23 ^= Test_Class_0.var_1;
        return new Test_Class_2();
    }

    public static String execute()
    {
        try {
            Test t = new Test();
            try { t.test(); }
            catch(Throwable e) { }
            try { return t.toString(); }
            catch (Throwable e) { return "Error during result conversion to String"; }
        } catch (Throwable e) { return "Error during test execution"; }
    }

    public static void main(String[] args)
    {
        try {
            Test t = new Test();
            try { t.test(); }
            catch(Throwable e) { }
            try { System.out.println(t); }
            catch(Throwable e) { }
        } catch (Throwable e) { }
    }

    private void test()
    {
        double var_78 = 0;
        --Test_Class_1.var_26;
        long var_79;
        for (var_70 /= 8.089457748637276E307; var_78 < 162 && !true & (true ? Test_Class_3.var_40 : (Test_Class_3.var_40 ^= Test_Class_3.var_40)); Test_Class_1.var_26 -= 1.2513521E38F)
        {
            short var_80 = 10682;
            Test_Class_1.var_21--;
            var_78++;
            var_72 = (Test_Class_3.var_40 |= (Test_Class_3.var_40 ^= false));
            ++Test_Class_1.var_26;
        }
        Test_Class_2 var_81;
        new Test_Class_4();
        int var_82 = 0;
        ++Test_Class_1.var_23;
        do
        {
            --Test_Class_1.var_26;
            var_82++;
            ++Test_Class_1.var_21;
        } while ((Test_Class_3.var_40 ^= false & false) && var_82 < 256);
        Test_Class_1.var_23 |= (var_68 = (var_68 = (Test_Class_5)(var_66 = new Test_Class_4()))).var_48 + (Test_Class_1.var_26 >>> new Test_Class_0().var_2);
        (true ? new Test_Class_5() : (var_68 = (var_68 = new Test_Class_5()))).var_60 *= Test_Class_0.var_1;
    }
    public String toString()
    {
        String result =  "[\n";
        result += "Test.var_69 = "; result += Printer.print(var_69);
        result += "\n";
        result += "Test.var_70 = "; result += Printer.print(var_70);
        result += "\n";
        result += "Test.var_71 = "; result += Printer.print(var_71);
        result += "\n";
        result += "Test.var_73 = "; result += Printer.print(var_73);
        result += "\n";
        result += "Test.var_68 = "; result += Printer.print(var_68);
        result += "\n";
        result += "Test.var_66 = "; result += Printer.print(var_66);
        result += "\n";
        result += "Test.var_72 = "; result += Printer.print(var_72);
        result += "\n";
        result += "Test.var_67 = "; result += Printer.print(var_67);
        result += "";
        result += "\n]";
        return result;
    }
    static class Printer
    {
        public static String print(boolean arg) { return String.valueOf(arg); }
        public static String print(byte arg)    { return String.valueOf(arg); }
        public static String print(short arg)   { return String.valueOf(arg); }
        public static String print(char arg)    { return String.valueOf((int)arg); }
        public static String print(int arg)     { return String.valueOf(arg); }
        public static String print(long arg)    { return String.valueOf(arg); }
        public static String print(float arg)   { return String.valueOf(arg); }
        public static String print(double arg)  { return String.valueOf(arg); }


        public static String print(Object arg)
        {
            return print_r(new java.util.Stack(), arg);
        }

        private static String print_r(java.util.Stack visitedObjects, Object arg)
        {
            String result = "";
            if (arg == null)
                result += "null";
            else
            if (arg.getClass().isArray())
            {
                for (int i = 0; i < visitedObjects.size(); i++)
                    if (visitedObjects.elementAt(i) == arg) return "<recursive>";

                visitedObjects.push(arg);

                final String delimiter = ", ";
                result += "[";

                if (arg instanceof Object[])
                {
                    Object[] array = (Object[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print_r(visitedObjects, array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof boolean[])
                {
                    boolean[] array = (boolean[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof byte[])
                {
                    byte[] array = (byte[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof short[])
                {
                    short[] array = (short[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof char[])
                {
                    char[] array = (char[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof int[])
                {
                     int[] array = (int[]) arg;
                     for (int i = 0; i < array.length; i++)
                     {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                     }
                }
                else
                if (arg instanceof long[])
                {
                    long[] array = (long[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof float[])
                {
                    float[] array = (float[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }
                else
                if (arg instanceof double[])
                {
                    double[] array = (double[]) arg;
                    for (int i = 0; i < array.length; i++)
                    {
                        result += print(array[i]);
                        if (i < array.length - 1) result += delimiter;
                    }
                }

                result += "]";
                visitedObjects.pop();

            } else
            {
                result += arg.toString();
            }

            return result;
        }
    }
}

