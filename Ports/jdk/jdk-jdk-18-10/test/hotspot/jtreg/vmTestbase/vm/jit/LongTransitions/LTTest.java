/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.jit.LongTransitions;
import java.io.*;
import java.util.Random;
import jdk.test.lib.Utils;

public class LTTest
{
    public static boolean flag = false;
    static Random rnd;
    static{
        rnd=Utils.getRandomInstance();
        System.loadLibrary("LTTest");
    }
    public static int getRndInt(){return rnd.nextInt(Integer.MAX_VALUE);}
    public static float getRndFloat(){return rnd.nextFloat()*Float.MAX_VALUE;}
    public static double getRndDouble(){return rnd.nextDouble()*Double.MAX_VALUE;}
    public static byte getRndByte(){return (byte)rnd.nextInt(Byte.MAX_VALUE);}
    private static void deleteFiles()
    {
        File f=new File("LTTest_java.txt");
        if ( f.exists())
        f.delete();
        f=new File("LTTest_c.txt");
        if ( f.exists())
         f.delete();
    }
    native public static void nativeFnc1(float p0,float p1,float p2,float p3,float p4,float p5,float p6,float p7
        ,float p8,float p9,float p10,float p11,float p12,float p13,float p14,float p15
        ,float p16,float p17,float p18,float p19,float p20,float p21,float p22,float p23
        ,float p24,float p25,float p26,float p27,float p28,float p29,float p30,float p31
        ,float p32,float p33,float p34,float p35,float p36,float p37,float p38,float p39
        ,float p40,float p41,float p42,float p43,float p44,float p45,float p46,float p47
        ,float p48,float p49,float p50,float p51,float p52,float p53,float p54,float p55
        ,float p56,float p57,float p58,float p59,float p60,float p61,float p62,float p63
        ,float p64,float p65,float p66,float p67,float p68,float p69,float p70,float p71
        ,float p72,float p73,float p74,float p75,float p76,float p77,float p78,float p79
        ,float p80,float p81,float p82,float p83,float p84,float p85,float p86,float p87
        ,float p88,float p89,float p90,float p91,float p92,float p93,float p94,float p95
        ,float p96,float p97,float p98,float p99,float p100,float p101,float p102
        ,float p103,float p104,float p105,float p106,float p107,float p108,float p109
        ,float p110,float p111,float p112,float p113,float p114,float p115,float p116
        ,float p117,float p118,float p119,float p120,float p121,float p122,float p123
        ,float p124,float p125,float p126    );
    private static void nativeFnc1_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        float  p6=getRndFloat();float  p7=getRndFloat();float  p8=getRndFloat();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        float  p12=getRndFloat();float  p13=getRndFloat();float  p14=getRndFloat();
        float  p15=getRndFloat();float  p16=getRndFloat();float  p17=getRndFloat();
        float  p18=getRndFloat();float  p19=getRndFloat();float  p20=getRndFloat();
        float  p21=getRndFloat();float  p22=getRndFloat();float  p23=getRndFloat();
        float  p24=getRndFloat();float  p25=getRndFloat();float  p26=getRndFloat();
        float  p27=getRndFloat();float  p28=getRndFloat();float  p29=getRndFloat();
        float  p30=getRndFloat();float  p31=getRndFloat();float  p32=getRndFloat();
        float  p33=getRndFloat();float  p34=getRndFloat();float  p35=getRndFloat();
        float  p36=getRndFloat();float  p37=getRndFloat();float  p38=getRndFloat();
        float  p39=getRndFloat();float  p40=getRndFloat();float  p41=getRndFloat();
        float  p42=getRndFloat();float  p43=getRndFloat();float  p44=getRndFloat();
        float  p45=getRndFloat();float  p46=getRndFloat();float  p47=getRndFloat();
        float  p48=getRndFloat();float  p49=getRndFloat();float  p50=getRndFloat();
        float  p51=getRndFloat();float  p52=getRndFloat();float  p53=getRndFloat();
        float  p54=getRndFloat();float  p55=getRndFloat();float  p56=getRndFloat();
        float  p57=getRndFloat();float  p58=getRndFloat();float  p59=getRndFloat();
        float  p60=getRndFloat();float  p61=getRndFloat();float  p62=getRndFloat();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        float  p66=getRndFloat();float  p67=getRndFloat();float  p68=getRndFloat();
        float  p69=getRndFloat();float  p70=getRndFloat();float  p71=getRndFloat();
        float  p72=getRndFloat();float  p73=getRndFloat();float  p74=getRndFloat();
        float  p75=getRndFloat();float  p76=getRndFloat();float  p77=getRndFloat();
        float  p78=getRndFloat();float  p79=getRndFloat();float  p80=getRndFloat();
        float  p81=getRndFloat();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();float  p85=getRndFloat();float  p86=getRndFloat();
        float  p87=getRndFloat();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        float  p93=getRndFloat();float  p94=getRndFloat();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();float  p100=getRndFloat();float  p101=getRndFloat();
        float  p102=getRndFloat();float  p103=getRndFloat();float  p104=getRndFloat();
        float  p105=getRndFloat();float  p106=getRndFloat();float  p107=getRndFloat();
        float  p108=getRndFloat();float  p109=getRndFloat();float  p110=getRndFloat();
        float  p111=getRndFloat();float  p112=getRndFloat();float  p113=getRndFloat();
        float  p114=getRndFloat();float  p115=getRndFloat();float  p116=getRndFloat();
        float  p117=getRndFloat();float  p118=getRndFloat();float  p119=getRndFloat();
        float  p120=getRndFloat();float  p121=getRndFloat();float  p122=getRndFloat();
        float  p123=getRndFloat();float  p124=getRndFloat();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc1(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc2(double p0,double p1,double p2,double p3,double p4,double p5,double p6,double p7
        ,double p8,double p9,double p10,double p11,double p12,double p13,double p14
        ,double p15,double p16,double p17,double p18,double p19,double p20,double p21
        ,double p22,double p23,double p24,double p25,double p26,double p27,double p28
        ,double p29,double p30,double p31,double p32,double p33,double p34,double p35
        ,double p36,double p37,double p38,double p39,double p40,double p41,double p42
        ,double p43,double p44,double p45,double p46,double p47,double p48,double p49
        ,double p50,double p51,double p52,double p53,double p54,double p55,double p56
        ,double p57,double p58,double p59,double p60,double p61,double p62,double p63
        ,double p64,double p65,double p66,double p67,double p68,double p69,double p70
        ,double p71,double p72,double p73,double p74,double p75,double p76,double p77
        ,double p78,double p79,double p80,double p81,double p82,double p83,double p84
        ,double p85,double p86,double p87,double p88,double p89,double p90,double p91
        ,double p92,double p93,double p94,double p95,double p96,double p97,double p98
        ,double p99,double p100,double p101,double p102,double p103,double p104
        ,double p105,double p106,double p107,double p108,double p109,double p110
        ,double p111,double p112,double p113,double p114,double p115,double p116
        ,double p117,double p118,double p119,double p120,double p121,double p122
        ,double p123,double p124,double p125,double p126    );
    private static void nativeFnc2_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();double  p38=getRndDouble();
        double  p39=getRndDouble();double  p40=getRndDouble();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();double  p46=getRndDouble();double  p47=getRndDouble();
        double  p48=getRndDouble();double  p49=getRndDouble();double  p50=getRndDouble();
        double  p51=getRndDouble();double  p52=getRndDouble();double  p53=getRndDouble();
        double  p54=getRndDouble();double  p55=getRndDouble();double  p56=getRndDouble();
        double  p57=getRndDouble();double  p58=getRndDouble();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();double  p64=getRndDouble();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();double  p70=getRndDouble();double  p71=getRndDouble();
        double  p72=getRndDouble();double  p73=getRndDouble();double  p74=getRndDouble();
        double  p75=getRndDouble();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();double  p82=getRndDouble();double  p83=getRndDouble();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        double  p87=getRndDouble();double  p88=getRndDouble();double  p89=getRndDouble();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();double  p94=getRndDouble();double  p95=getRndDouble();
        double  p96=getRndDouble();double  p97=getRndDouble();double  p98=getRndDouble();
        double  p99=getRndDouble();double  p100=getRndDouble();double  p101=getRndDouble();
        double  p102=getRndDouble();double  p103=getRndDouble();double  p104=getRndDouble();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();double  p109=getRndDouble();double  p110=getRndDouble();
        double  p111=getRndDouble();double  p112=getRndDouble();double  p113=getRndDouble();
        double  p114=getRndDouble();double  p115=getRndDouble();double  p116=getRndDouble();
        double  p117=getRndDouble();double  p118=getRndDouble();double  p119=getRndDouble();
        double  p120=getRndDouble();double  p121=getRndDouble();double  p122=getRndDouble();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc2(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc3(float p0,int p1,int p2,int p3,int p4,float p5,int p6,float p7,int p8,float p9
        ,int p10,int p11,float p12,float p13,float p14,float p15,int p16,int p17
        ,int p18,float p19,int p20,float p21,float p22,float p23,float p24,int p25
        ,float p26,int p27,float p28,float p29,float p30,int p31,int p32,float p33
        ,float p34,float p35,int p36,float p37,int p38,float p39,int p40,float p41
        ,float p42,float p43,int p44,int p45,float p46,float p47,float p48,float p49
        ,int p50,int p51,int p52,float p53,int p54,float p55,int p56,float p57,float p58
        ,float p59,float p60,int p61,int p62,int p63,float p64,float p65,float p66
        ,float p67,int p68,int p69,float p70,float p71,int p72,int p73,float p74
        ,int p75,int p76,int p77,float p78,float p79,float p80,int p81,float p82
        ,int p83,float p84,float p85,float p86,float p87,int p88,int p89,int p90
        ,float p91,int p92,int p93,float p94,float p95,int p96,float p97,float p98
        ,float p99,int p100,float p101,float p102,int p103,float p104,float p105
        ,int p106,float p107,float p108,int p109,float p110,float p111,int p112
        ,float p113,int p114,float p115,int p116,int p117,float p118,float p119
        ,int p120,float p121,int p122,float p123,int p124,int p125,float p126    );
    private static void nativeFnc3_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();int  p1=getRndInt();int  p2=getRndInt();
        int  p3=getRndInt();int  p4=getRndInt();float  p5=getRndFloat();int  p6=getRndInt();
        float  p7=getRndFloat();int  p8=getRndInt();float  p9=getRndFloat();int  p10=getRndInt();
        int  p11=getRndInt();float  p12=getRndFloat();float  p13=getRndFloat();
        float  p14=getRndFloat();float  p15=getRndFloat();int  p16=getRndInt();
        int  p17=getRndInt();int  p18=getRndInt();float  p19=getRndFloat();int  p20=getRndInt();
        float  p21=getRndFloat();float  p22=getRndFloat();float  p23=getRndFloat();
        float  p24=getRndFloat();int  p25=getRndInt();float  p26=getRndFloat();
        int  p27=getRndInt();float  p28=getRndFloat();float  p29=getRndFloat();
        float  p30=getRndFloat();int  p31=getRndInt();int  p32=getRndInt();float  p33=getRndFloat();
        float  p34=getRndFloat();float  p35=getRndFloat();int  p36=getRndInt();
        float  p37=getRndFloat();int  p38=getRndInt();float  p39=getRndFloat();
        int  p40=getRndInt();float  p41=getRndFloat();float  p42=getRndFloat();
        float  p43=getRndFloat();int  p44=getRndInt();int  p45=getRndInt();float  p46=getRndFloat();
        float  p47=getRndFloat();float  p48=getRndFloat();float  p49=getRndFloat();
        int  p50=getRndInt();int  p51=getRndInt();int  p52=getRndInt();float  p53=getRndFloat();
        int  p54=getRndInt();float  p55=getRndFloat();int  p56=getRndInt();float  p57=getRndFloat();
        float  p58=getRndFloat();float  p59=getRndFloat();float  p60=getRndFloat();
        int  p61=getRndInt();int  p62=getRndInt();int  p63=getRndInt();float  p64=getRndFloat();
        float  p65=getRndFloat();float  p66=getRndFloat();float  p67=getRndFloat();
        int  p68=getRndInt();int  p69=getRndInt();float  p70=getRndFloat();float  p71=getRndFloat();
        int  p72=getRndInt();int  p73=getRndInt();float  p74=getRndFloat();int  p75=getRndInt();
        int  p76=getRndInt();int  p77=getRndInt();float  p78=getRndFloat();float  p79=getRndFloat();
        float  p80=getRndFloat();int  p81=getRndInt();float  p82=getRndFloat();
        int  p83=getRndInt();float  p84=getRndFloat();float  p85=getRndFloat();
        float  p86=getRndFloat();float  p87=getRndFloat();int  p88=getRndInt();
        int  p89=getRndInt();int  p90=getRndInt();float  p91=getRndFloat();int  p92=getRndInt();
        int  p93=getRndInt();float  p94=getRndFloat();float  p95=getRndFloat();
        int  p96=getRndInt();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();int  p100=getRndInt();float  p101=getRndFloat();
        float  p102=getRndFloat();int  p103=getRndInt();float  p104=getRndFloat();
        float  p105=getRndFloat();int  p106=getRndInt();float  p107=getRndFloat();
        float  p108=getRndFloat();int  p109=getRndInt();float  p110=getRndFloat();
        float  p111=getRndFloat();int  p112=getRndInt();float  p113=getRndFloat();
        int  p114=getRndInt();float  p115=getRndFloat();int  p116=getRndInt();int  p117=getRndInt();
        float  p118=getRndFloat();float  p119=getRndFloat();int  p120=getRndInt();
        float  p121=getRndFloat();int  p122=getRndInt();float  p123=getRndFloat();
        int  p124=getRndInt();int  p125=getRndInt();float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%d\n",p1);ps.format("p2=%d\n",p2);ps.format("p3=%d\n",p3);
        ps.format("p4=%d\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%d\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%d\n",p10);ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%d\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%d\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%d\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%d\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%d\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);
        ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%d\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%d\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%d\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%d\n",p72);
        ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%d\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%d\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%d\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);ps.format("p93=%d\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%d\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%d\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%d\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);ps.format("p117=%d\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%d\n",p124);ps.format("p125=%d\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc3(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc4(float p0,float p1,float p2,byte p3,float p4,float p5,float p6,byte p7,float p8
        ,byte p9,float p10,byte p11,byte p12,byte p13,float p14,float p15,float p16
        ,float p17,byte p18,byte p19,byte p20,byte p21,float p22,float p23,byte p24
        ,byte p25,float p26,float p27,float p28,byte p29,float p30,float p31,float p32
        ,byte p33,byte p34,float p35,byte p36,float p37,byte p38,float p39,float p40
        ,byte p41,float p42,byte p43,byte p44,float p45,byte p46,float p47,float p48
        ,byte p49,byte p50,float p51,byte p52,float p53,float p54,byte p55,float p56
        ,float p57,byte p58,float p59,byte p60,byte p61,byte p62,float p63,byte p64
        ,byte p65,byte p66,float p67,float p68,float p69,float p70,byte p71,float p72
        ,float p73,float p74,byte p75,byte p76,float p77,float p78,float p79,float p80
        ,byte p81,float p82,byte p83,float p84,float p85,byte p86,float p87,byte p88
        ,float p89,float p90,byte p91,byte p92,byte p93,byte p94,float p95,float p96
        ,float p97,float p98,float p99,byte p100,byte p101,float p102,float p103
        ,float p104,byte p105,float p106,float p107,byte p108,float p109,float p110
        ,byte p111,byte p112,float p113,byte p114,byte p115,float p116,byte p117
        ,byte p118,byte p119,byte p120,float p121,byte p122,byte p123,byte p124
        ,byte p125,float p126    );
    private static void nativeFnc4_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        byte  p3=getRndByte();float  p4=getRndFloat();float  p5=getRndFloat();float  p6=getRndFloat();
        byte  p7=getRndByte();float  p8=getRndFloat();byte  p9=getRndByte();float  p10=getRndFloat();
        byte  p11=getRndByte();byte  p12=getRndByte();byte  p13=getRndByte();float  p14=getRndFloat();
        float  p15=getRndFloat();float  p16=getRndFloat();float  p17=getRndFloat();
        byte  p18=getRndByte();byte  p19=getRndByte();byte  p20=getRndByte();byte  p21=getRndByte();
        float  p22=getRndFloat();float  p23=getRndFloat();byte  p24=getRndByte();
        byte  p25=getRndByte();float  p26=getRndFloat();float  p27=getRndFloat();
        float  p28=getRndFloat();byte  p29=getRndByte();float  p30=getRndFloat();
        float  p31=getRndFloat();float  p32=getRndFloat();byte  p33=getRndByte();
        byte  p34=getRndByte();float  p35=getRndFloat();byte  p36=getRndByte();
        float  p37=getRndFloat();byte  p38=getRndByte();float  p39=getRndFloat();
        float  p40=getRndFloat();byte  p41=getRndByte();float  p42=getRndFloat();
        byte  p43=getRndByte();byte  p44=getRndByte();float  p45=getRndFloat();
        byte  p46=getRndByte();float  p47=getRndFloat();float  p48=getRndFloat();
        byte  p49=getRndByte();byte  p50=getRndByte();float  p51=getRndFloat();
        byte  p52=getRndByte();float  p53=getRndFloat();float  p54=getRndFloat();
        byte  p55=getRndByte();float  p56=getRndFloat();float  p57=getRndFloat();
        byte  p58=getRndByte();float  p59=getRndFloat();byte  p60=getRndByte();
        byte  p61=getRndByte();byte  p62=getRndByte();float  p63=getRndFloat();
        byte  p64=getRndByte();byte  p65=getRndByte();byte  p66=getRndByte();float  p67=getRndFloat();
        float  p68=getRndFloat();float  p69=getRndFloat();float  p70=getRndFloat();
        byte  p71=getRndByte();float  p72=getRndFloat();float  p73=getRndFloat();
        float  p74=getRndFloat();byte  p75=getRndByte();byte  p76=getRndByte();
        float  p77=getRndFloat();float  p78=getRndFloat();float  p79=getRndFloat();
        float  p80=getRndFloat();byte  p81=getRndByte();float  p82=getRndFloat();
        byte  p83=getRndByte();float  p84=getRndFloat();float  p85=getRndFloat();
        byte  p86=getRndByte();float  p87=getRndFloat();byte  p88=getRndByte();
        float  p89=getRndFloat();float  p90=getRndFloat();byte  p91=getRndByte();
        byte  p92=getRndByte();byte  p93=getRndByte();byte  p94=getRndByte();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();byte  p100=getRndByte();byte  p101=getRndByte();
        float  p102=getRndFloat();float  p103=getRndFloat();float  p104=getRndFloat();
        byte  p105=getRndByte();float  p106=getRndFloat();float  p107=getRndFloat();
        byte  p108=getRndByte();float  p109=getRndFloat();float  p110=getRndFloat();
        byte  p111=getRndByte();byte  p112=getRndByte();float  p113=getRndFloat();
        byte  p114=getRndByte();byte  p115=getRndByte();float  p116=getRndFloat();
        byte  p117=getRndByte();byte  p118=getRndByte();byte  p119=getRndByte();
        byte  p120=getRndByte();float  p121=getRndFloat();byte  p122=getRndByte();
        byte  p123=getRndByte();byte  p124=getRndByte();byte  p125=getRndByte();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%d\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%d\n",p11);ps.format("p12=%d\n",p12);ps.format("p13=%d\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%d\n",p18);ps.format("p19=%d\n",p19);
        ps.format("p20=%d\n",p20);ps.format("p21=%d\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%d\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%d\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%d\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%d\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%d\n",p43);
        ps.format("p44=%d\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%d\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%d\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%d\n",p64);
        ps.format("p65=%d\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%d\n",p75);ps.format("p76=%d\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%d\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%d\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);
        ps.format("p92=%d\n",p92);ps.format("p93=%d\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);ps.format("p112=%d\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);ps.format("p115=%d\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%d\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%d\n",p124);
        ps.format("p125=%d\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc4(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc5(int p0,float p1,int p2,float p3,byte p4,byte p5,byte p6,float p7,int p8
        ,byte p9,float p10,byte p11,float p12,byte p13,byte p14,int p15,float p16
        ,int p17,int p18,int p19,int p20,byte p21,byte p22,byte p23,byte p24,int p25
        ,int p26,float p27,byte p28,float p29,int p30,float p31,byte p32,float p33
        ,int p34,int p35,float p36,byte p37,int p38,byte p39,byte p40,float p41
        ,float p42,byte p43,byte p44,float p45,byte p46,byte p47,int p48,int p49
        ,int p50,float p51,int p52,float p53,byte p54,int p55,int p56,byte p57,int p58
        ,byte p59,int p60,float p61,int p62,float p63,int p64,float p65,byte p66
        ,byte p67,float p68,byte p69,int p70,float p71,byte p72,int p73,int p74
        ,byte p75,byte p76,byte p77,int p78,byte p79,byte p80,float p81,byte p82
        ,int p83,byte p84,int p85,int p86,byte p87,int p88,float p89,int p90,int p91
        ,float p92,byte p93,float p94,byte p95,float p96,int p97,float p98,float p99
        ,int p100,byte p101,int p102,byte p103,float p104,float p105,float p106
        ,float p107,float p108,float p109,int p110,int p111,int p112,byte p113,float p114
        ,float p115,float p116,int p117,float p118,int p119,byte p120,int p121,byte p122
        ,byte p123,float p124,float p125,float p126    );
    private static void nativeFnc5_invoke(PrintStream ps)
    {
        int  p0=getRndInt();float  p1=getRndFloat();int  p2=getRndInt();
        float  p3=getRndFloat();byte  p4=getRndByte();byte  p5=getRndByte();byte  p6=getRndByte();
        float  p7=getRndFloat();int  p8=getRndInt();byte  p9=getRndByte();float  p10=getRndFloat();
        byte  p11=getRndByte();float  p12=getRndFloat();byte  p13=getRndByte();
        byte  p14=getRndByte();int  p15=getRndInt();float  p16=getRndFloat();int  p17=getRndInt();
        int  p18=getRndInt();int  p19=getRndInt();int  p20=getRndInt();byte  p21=getRndByte();
        byte  p22=getRndByte();byte  p23=getRndByte();byte  p24=getRndByte();int  p25=getRndInt();
        int  p26=getRndInt();float  p27=getRndFloat();byte  p28=getRndByte();float  p29=getRndFloat();
        int  p30=getRndInt();float  p31=getRndFloat();byte  p32=getRndByte();float  p33=getRndFloat();
        int  p34=getRndInt();int  p35=getRndInt();float  p36=getRndFloat();byte  p37=getRndByte();
        int  p38=getRndInt();byte  p39=getRndByte();byte  p40=getRndByte();float  p41=getRndFloat();
        float  p42=getRndFloat();byte  p43=getRndByte();byte  p44=getRndByte();
        float  p45=getRndFloat();byte  p46=getRndByte();byte  p47=getRndByte();
        int  p48=getRndInt();int  p49=getRndInt();int  p50=getRndInt();float  p51=getRndFloat();
        int  p52=getRndInt();float  p53=getRndFloat();byte  p54=getRndByte();int  p55=getRndInt();
        int  p56=getRndInt();byte  p57=getRndByte();int  p58=getRndInt();byte  p59=getRndByte();
        int  p60=getRndInt();float  p61=getRndFloat();int  p62=getRndInt();float  p63=getRndFloat();
        int  p64=getRndInt();float  p65=getRndFloat();byte  p66=getRndByte();byte  p67=getRndByte();
        float  p68=getRndFloat();byte  p69=getRndByte();int  p70=getRndInt();float  p71=getRndFloat();
        byte  p72=getRndByte();int  p73=getRndInt();int  p74=getRndInt();byte  p75=getRndByte();
        byte  p76=getRndByte();byte  p77=getRndByte();int  p78=getRndInt();byte  p79=getRndByte();
        byte  p80=getRndByte();float  p81=getRndFloat();byte  p82=getRndByte();
        int  p83=getRndInt();byte  p84=getRndByte();int  p85=getRndInt();int  p86=getRndInt();
        byte  p87=getRndByte();int  p88=getRndInt();float  p89=getRndFloat();int  p90=getRndInt();
        int  p91=getRndInt();float  p92=getRndFloat();byte  p93=getRndByte();float  p94=getRndFloat();
        byte  p95=getRndByte();float  p96=getRndFloat();int  p97=getRndInt();float  p98=getRndFloat();
        float  p99=getRndFloat();int  p100=getRndInt();byte  p101=getRndByte();
        int  p102=getRndInt();byte  p103=getRndByte();float  p104=getRndFloat();
        float  p105=getRndFloat();float  p106=getRndFloat();float  p107=getRndFloat();
        float  p108=getRndFloat();float  p109=getRndFloat();int  p110=getRndInt();
        int  p111=getRndInt();int  p112=getRndInt();byte  p113=getRndByte();float  p114=getRndFloat();
        float  p115=getRndFloat();float  p116=getRndFloat();int  p117=getRndInt();
        float  p118=getRndFloat();int  p119=getRndInt();byte  p120=getRndByte();
        int  p121=getRndInt();byte  p122=getRndByte();byte  p123=getRndByte();float  p124=getRndFloat();
        float  p125=getRndFloat();float  p126=getRndFloat();
        ps.format("p0=%d\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%d\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%d\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%d\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%d\n",p13);ps.format("p14=%d\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%d\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%d\n",p20);ps.format("p21=%d\n",p21);
        ps.format("p22=%d\n",p22);ps.format("p23=%d\n",p23);ps.format("p24=%d\n",p24);
        ps.format("p25=%d\n",p25);ps.format("p26=%d\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%d\n",p34);ps.format("p35=%d\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%d\n",p37);ps.format("p38=%d\n",p38);ps.format("p39=%d\n",p39);
        ps.format("p40=%d\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%d\n",p43);ps.format("p44=%d\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%d\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%d\n",p48);
        ps.format("p49=%d\n",p49);ps.format("p50=%d\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);
        ps.format("p55=%d\n",p55);ps.format("p56=%d\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%d\n",p58);ps.format("p59=%d\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);
        ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);ps.format("p69=%d\n",p69);
        ps.format("p70=%d\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%d\n",p72);
        ps.format("p73=%d\n",p73);ps.format("p74=%d\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%d\n",p77);ps.format("p78=%d\n",p78);
        ps.format("p79=%d\n",p79);ps.format("p80=%d\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%d\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%d\n",p84);
        ps.format("p85=%d\n",p85);ps.format("p86=%d\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%d\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);ps.format("p102=%d\n",p102);
        ps.format("p103=%d\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%d\n",p110);ps.format("p111=%d\n",p111);
        ps.format("p112=%d\n",p112);ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%d\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%d\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc5(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc6(double p0,int p1,int p2,int p3,int p4,double p5,double p6,int p7,int p8
        ,double p9,double p10,int p11,int p12,double p13,double p14,double p15,double p16
        ,double p17,double p18,int p19,int p20,double p21,double p22,int p23,double p24
        ,double p25,double p26,int p27,int p28,int p29,double p30,double p31,double p32
        ,int p33,int p34,double p35,int p36,int p37,double p38,double p39,double p40
        ,int p41,double p42,double p43,int p44,int p45,int p46,int p47,double p48
        ,double p49,double p50,int p51,int p52,int p53,int p54,double p55,int p56
        ,int p57,double p58,int p59,double p60,double p61,int p62,int p63,double p64
        ,double p65,int p66,int p67,double p68,double p69,double p70,int p71,int p72
        ,double p73,int p74,int p75,double p76,double p77,double p78,int p79,int p80
        ,int p81,int p82,int p83,double p84,double p85,int p86,double p87,double p88
        ,int p89,double p90,int p91,int p92,double p93,int p94,int p95,double p96
        ,double p97,int p98,double p99,double p100,double p101,int p102,double p103
        ,double p104,int p105,int p106,int p107,double p108,int p109,double p110
        ,double p111,double p112,int p113,int p114,double p115,int p116,double p117
        ,double p118,int p119,double p120,double p121,int p122,int p123,int p124
        ,int p125,double p126    );
    private static void nativeFnc6_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();int  p1=getRndInt();int  p2=getRndInt();
        int  p3=getRndInt();int  p4=getRndInt();double  p5=getRndDouble();double  p6=getRndDouble();
        int  p7=getRndInt();int  p8=getRndInt();double  p9=getRndDouble();double  p10=getRndDouble();
        int  p11=getRndInt();int  p12=getRndInt();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        double  p18=getRndDouble();int  p19=getRndInt();int  p20=getRndInt();double  p21=getRndDouble();
        double  p22=getRndDouble();int  p23=getRndInt();double  p24=getRndDouble();
        double  p25=getRndDouble();double  p26=getRndDouble();int  p27=getRndInt();
        int  p28=getRndInt();int  p29=getRndInt();double  p30=getRndDouble();double  p31=getRndDouble();
        double  p32=getRndDouble();int  p33=getRndInt();int  p34=getRndInt();double  p35=getRndDouble();
        int  p36=getRndInt();int  p37=getRndInt();double  p38=getRndDouble();double  p39=getRndDouble();
        double  p40=getRndDouble();int  p41=getRndInt();double  p42=getRndDouble();
        double  p43=getRndDouble();int  p44=getRndInt();int  p45=getRndInt();int  p46=getRndInt();
        int  p47=getRndInt();double  p48=getRndDouble();double  p49=getRndDouble();
        double  p50=getRndDouble();int  p51=getRndInt();int  p52=getRndInt();int  p53=getRndInt();
        int  p54=getRndInt();double  p55=getRndDouble();int  p56=getRndInt();int  p57=getRndInt();
        double  p58=getRndDouble();int  p59=getRndInt();double  p60=getRndDouble();
        double  p61=getRndDouble();int  p62=getRndInt();int  p63=getRndInt();double  p64=getRndDouble();
        double  p65=getRndDouble();int  p66=getRndInt();int  p67=getRndInt();double  p68=getRndDouble();
        double  p69=getRndDouble();double  p70=getRndDouble();int  p71=getRndInt();
        int  p72=getRndInt();double  p73=getRndDouble();int  p74=getRndInt();int  p75=getRndInt();
        double  p76=getRndDouble();double  p77=getRndDouble();double  p78=getRndDouble();
        int  p79=getRndInt();int  p80=getRndInt();int  p81=getRndInt();int  p82=getRndInt();
        int  p83=getRndInt();double  p84=getRndDouble();double  p85=getRndDouble();
        int  p86=getRndInt();double  p87=getRndDouble();double  p88=getRndDouble();
        int  p89=getRndInt();double  p90=getRndDouble();int  p91=getRndInt();int  p92=getRndInt();
        double  p93=getRndDouble();int  p94=getRndInt();int  p95=getRndInt();double  p96=getRndDouble();
        double  p97=getRndDouble();int  p98=getRndInt();double  p99=getRndDouble();
        double  p100=getRndDouble();double  p101=getRndDouble();int  p102=getRndInt();
        double  p103=getRndDouble();double  p104=getRndDouble();int  p105=getRndInt();
        int  p106=getRndInt();int  p107=getRndInt();double  p108=getRndDouble();
        int  p109=getRndInt();double  p110=getRndDouble();double  p111=getRndDouble();
        double  p112=getRndDouble();int  p113=getRndInt();int  p114=getRndInt();
        double  p115=getRndDouble();int  p116=getRndInt();double  p117=getRndDouble();
        double  p118=getRndDouble();int  p119=getRndInt();double  p120=getRndDouble();
        double  p121=getRndDouble();int  p122=getRndInt();int  p123=getRndInt();
        int  p124=getRndInt();int  p125=getRndInt();double  p126=getRndDouble();

        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%d\n",p2);
        ps.format("p3=%d\n",p3);ps.format("p4=%d\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);ps.format("p8=%d\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%d\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%d\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%d\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%d\n",p45);ps.format("p46=%d\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%d\n",p51);ps.format("p52=%d\n",p52);ps.format("p53=%d\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%d\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%d\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);
        ps.format("p72=%d\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);
        ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);ps.format("p80=%d\n",p80);
        ps.format("p81=%d\n",p81);ps.format("p82=%d\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%d\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);ps.format("p92=%d\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%d\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%d\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%d\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%d\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%d\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%d\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc6(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc7(double p0,byte p1,double p2,double p3,byte p4,byte p5,byte p6,byte p7,double p8
        ,double p9,byte p10,byte p11,double p12,double p13,byte p14,byte p15,byte p16
        ,byte p17,byte p18,double p19,double p20,double p21,double p22,byte p23
        ,double p24,byte p25,double p26,byte p27,double p28,byte p29,byte p30,double p31
        ,byte p32,double p33,byte p34,double p35,double p36,double p37,double p38
        ,byte p39,double p40,double p41,byte p42,double p43,byte p44,double p45
        ,double p46,double p47,double p48,byte p49,byte p50,double p51,double p52
        ,double p53,byte p54,double p55,double p56,double p57,double p58,double p59
        ,byte p60,double p61,double p62,byte p63,byte p64,byte p65,byte p66,byte p67
        ,byte p68,double p69,double p70,byte p71,double p72,double p73,byte p74
        ,double p75,byte p76,byte p77,byte p78,byte p79,byte p80,double p81,double p82
        ,byte p83,byte p84,byte p85,byte p86,double p87,byte p88,double p89,double p90
        ,double p91,byte p92,byte p93,double p94,byte p95,byte p96,byte p97,byte p98
        ,double p99,byte p100,byte p101,byte p102,double p103,double p104,byte p105
        ,double p106,byte p107,byte p108,byte p109,double p110,byte p111,byte p112
        ,byte p113,byte p114,byte p115,byte p116,double p117,byte p118,double p119
        ,double p120,byte p121,byte p122,double p123,double p124,byte p125,double p126
            );
    private static void nativeFnc7_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();byte  p1=getRndByte();double  p2=getRndDouble();
        double  p3=getRndDouble();byte  p4=getRndByte();byte  p5=getRndByte();byte  p6=getRndByte();
        byte  p7=getRndByte();double  p8=getRndDouble();double  p9=getRndDouble();
        byte  p10=getRndByte();byte  p11=getRndByte();double  p12=getRndDouble();
        double  p13=getRndDouble();byte  p14=getRndByte();byte  p15=getRndByte();
        byte  p16=getRndByte();byte  p17=getRndByte();byte  p18=getRndByte();double  p19=getRndDouble();
        double  p20=getRndDouble();double  p21=getRndDouble();double  p22=getRndDouble();
        byte  p23=getRndByte();double  p24=getRndDouble();byte  p25=getRndByte();
        double  p26=getRndDouble();byte  p27=getRndByte();double  p28=getRndDouble();
        byte  p29=getRndByte();byte  p30=getRndByte();double  p31=getRndDouble();
        byte  p32=getRndByte();double  p33=getRndDouble();byte  p34=getRndByte();
        double  p35=getRndDouble();double  p36=getRndDouble();double  p37=getRndDouble();
        double  p38=getRndDouble();byte  p39=getRndByte();double  p40=getRndDouble();
        double  p41=getRndDouble();byte  p42=getRndByte();double  p43=getRndDouble();
        byte  p44=getRndByte();double  p45=getRndDouble();double  p46=getRndDouble();
        double  p47=getRndDouble();double  p48=getRndDouble();byte  p49=getRndByte();
        byte  p50=getRndByte();double  p51=getRndDouble();double  p52=getRndDouble();
        double  p53=getRndDouble();byte  p54=getRndByte();double  p55=getRndDouble();
        double  p56=getRndDouble();double  p57=getRndDouble();double  p58=getRndDouble();
        double  p59=getRndDouble();byte  p60=getRndByte();double  p61=getRndDouble();
        double  p62=getRndDouble();byte  p63=getRndByte();byte  p64=getRndByte();
        byte  p65=getRndByte();byte  p66=getRndByte();byte  p67=getRndByte();byte  p68=getRndByte();
        double  p69=getRndDouble();double  p70=getRndDouble();byte  p71=getRndByte();
        double  p72=getRndDouble();double  p73=getRndDouble();byte  p74=getRndByte();
        double  p75=getRndDouble();byte  p76=getRndByte();byte  p77=getRndByte();
        byte  p78=getRndByte();byte  p79=getRndByte();byte  p80=getRndByte();double  p81=getRndDouble();
        double  p82=getRndDouble();byte  p83=getRndByte();byte  p84=getRndByte();
        byte  p85=getRndByte();byte  p86=getRndByte();double  p87=getRndDouble();
        byte  p88=getRndByte();double  p89=getRndDouble();double  p90=getRndDouble();
        double  p91=getRndDouble();byte  p92=getRndByte();byte  p93=getRndByte();
        double  p94=getRndDouble();byte  p95=getRndByte();byte  p96=getRndByte();
        byte  p97=getRndByte();byte  p98=getRndByte();double  p99=getRndDouble();
        byte  p100=getRndByte();byte  p101=getRndByte();byte  p102=getRndByte();
        double  p103=getRndDouble();double  p104=getRndDouble();byte  p105=getRndByte();
        double  p106=getRndDouble();byte  p107=getRndByte();byte  p108=getRndByte();
        byte  p109=getRndByte();double  p110=getRndDouble();byte  p111=getRndByte();
        byte  p112=getRndByte();byte  p113=getRndByte();byte  p114=getRndByte();
        byte  p115=getRndByte();byte  p116=getRndByte();double  p117=getRndDouble();
        byte  p118=getRndByte();double  p119=getRndDouble();double  p120=getRndDouble();
        byte  p121=getRndByte();byte  p122=getRndByte();double  p123=getRndDouble();
        double  p124=getRndDouble();byte  p125=getRndByte();double  p126=getRndDouble();

        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);ps.format("p5=%d\n",p5);
        ps.format("p6=%d\n",p6);ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%d\n",p15);ps.format("p16=%d\n",p16);ps.format("p17=%d\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%d\n",p29);
        ps.format("p30=%d\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%d\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%d\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%d\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%d\n",p79);ps.format("p80=%d\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%d\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%d\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);ps.format("p98=%d\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%d\n",p111);ps.format("p112=%d\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%d\n",p114);ps.format("p115=%d\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%d\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc7(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc8(byte p0,double p1,byte p2,double p3,int p4,int p5,int p6,double p7,double p8
        ,int p9,double p10,byte p11,int p12,byte p13,double p14,int p15,byte p16
        ,int p17,double p18,double p19,byte p20,int p21,byte p22,int p23,int p24
        ,byte p25,double p26,int p27,int p28,byte p29,int p30,int p31,byte p32,double p33
        ,double p34,byte p35,double p36,int p37,double p38,int p39,byte p40,byte p41
        ,int p42,int p43,int p44,byte p45,double p46,double p47,int p48,int p49
        ,byte p50,byte p51,int p52,int p53,byte p54,byte p55,int p56,int p57,int p58
        ,double p59,byte p60,int p61,double p62,double p63,double p64,int p65,int p66
        ,double p67,int p68,double p69,int p70,int p71,byte p72,int p73,double p74
        ,double p75,byte p76,int p77,byte p78,byte p79,int p80,int p81,int p82,double p83
        ,double p84,int p85,byte p86,byte p87,byte p88,byte p89,int p90,int p91
        ,double p92,int p93,double p94,double p95,int p96,byte p97,double p98,double p99
        ,byte p100,byte p101,byte p102,byte p103,double p104,int p105,int p106,byte p107
        ,byte p108,double p109,double p110,double p111,byte p112,double p113,byte p114
        ,byte p115,int p116,double p117,int p118,int p119,byte p120,double p121
        ,byte p122,int p123,int p124,int p125,byte p126    );
    private static void nativeFnc8_invoke(PrintStream ps)
    {
        byte  p0=getRndByte();double  p1=getRndDouble();byte  p2=getRndByte();
        double  p3=getRndDouble();int  p4=getRndInt();int  p5=getRndInt();int  p6=getRndInt();
        double  p7=getRndDouble();double  p8=getRndDouble();int  p9=getRndInt();
        double  p10=getRndDouble();byte  p11=getRndByte();int  p12=getRndInt();
        byte  p13=getRndByte();double  p14=getRndDouble();int  p15=getRndInt();
        byte  p16=getRndByte();int  p17=getRndInt();double  p18=getRndDouble();
        double  p19=getRndDouble();byte  p20=getRndByte();int  p21=getRndInt();
        byte  p22=getRndByte();int  p23=getRndInt();int  p24=getRndInt();byte  p25=getRndByte();
        double  p26=getRndDouble();int  p27=getRndInt();int  p28=getRndInt();byte  p29=getRndByte();
        int  p30=getRndInt();int  p31=getRndInt();byte  p32=getRndByte();double  p33=getRndDouble();
        double  p34=getRndDouble();byte  p35=getRndByte();double  p36=getRndDouble();
        int  p37=getRndInt();double  p38=getRndDouble();int  p39=getRndInt();byte  p40=getRndByte();
        byte  p41=getRndByte();int  p42=getRndInt();int  p43=getRndInt();int  p44=getRndInt();
        byte  p45=getRndByte();double  p46=getRndDouble();double  p47=getRndDouble();
        int  p48=getRndInt();int  p49=getRndInt();byte  p50=getRndByte();byte  p51=getRndByte();
        int  p52=getRndInt();int  p53=getRndInt();byte  p54=getRndByte();byte  p55=getRndByte();
        int  p56=getRndInt();int  p57=getRndInt();int  p58=getRndInt();double  p59=getRndDouble();
        byte  p60=getRndByte();int  p61=getRndInt();double  p62=getRndDouble();
        double  p63=getRndDouble();double  p64=getRndDouble();int  p65=getRndInt();
        int  p66=getRndInt();double  p67=getRndDouble();int  p68=getRndInt();double  p69=getRndDouble();
        int  p70=getRndInt();int  p71=getRndInt();byte  p72=getRndByte();int  p73=getRndInt();
        double  p74=getRndDouble();double  p75=getRndDouble();byte  p76=getRndByte();
        int  p77=getRndInt();byte  p78=getRndByte();byte  p79=getRndByte();int  p80=getRndInt();
        int  p81=getRndInt();int  p82=getRndInt();double  p83=getRndDouble();double  p84=getRndDouble();
        int  p85=getRndInt();byte  p86=getRndByte();byte  p87=getRndByte();byte  p88=getRndByte();
        byte  p89=getRndByte();int  p90=getRndInt();int  p91=getRndInt();double  p92=getRndDouble();
        int  p93=getRndInt();double  p94=getRndDouble();double  p95=getRndDouble();
        int  p96=getRndInt();byte  p97=getRndByte();double  p98=getRndDouble();
        double  p99=getRndDouble();byte  p100=getRndByte();byte  p101=getRndByte();
        byte  p102=getRndByte();byte  p103=getRndByte();double  p104=getRndDouble();
        int  p105=getRndInt();int  p106=getRndInt();byte  p107=getRndByte();byte  p108=getRndByte();
        double  p109=getRndDouble();double  p110=getRndDouble();double  p111=getRndDouble();
        byte  p112=getRndByte();double  p113=getRndDouble();byte  p114=getRndByte();
        byte  p115=getRndByte();int  p116=getRndInt();double  p117=getRndDouble();
        int  p118=getRndInt();int  p119=getRndInt();byte  p120=getRndByte();double  p121=getRndDouble();
        byte  p122=getRndByte();int  p123=getRndInt();int  p124=getRndInt();int  p125=getRndInt();
        byte  p126=getRndByte();
        ps.format("p0=%d\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%d\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%d\n",p11);ps.format("p12=%d\n",p12);ps.format("p13=%d\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);ps.format("p16=%d\n",p16);
        ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%d\n",p20);ps.format("p21=%d\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%d\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%d\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);
        ps.format("p29=%d\n",p29);ps.format("p30=%d\n",p30);ps.format("p31=%d\n",p31);
        ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%d\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%d\n",p39);ps.format("p40=%d\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%d\n",p43);
        ps.format("p44=%d\n",p44);ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);ps.format("p49=%d\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);ps.format("p52=%d\n",p52);
        ps.format("p53=%d\n",p53);ps.format("p54=%d\n",p54);ps.format("p55=%d\n",p55);
        ps.format("p56=%d\n",p56);ps.format("p57=%d\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%d\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);
        ps.format("p71=%d\n",p71);ps.format("p72=%d\n",p72);ps.format("p73=%d\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);
        ps.format("p77=%d\n",p77);ps.format("p78=%d\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%d\n",p80);ps.format("p81=%d\n",p81);ps.format("p82=%d\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);
        ps.format("p86=%d\n",p86);ps.format("p87=%d\n",p87);ps.format("p88=%d\n",p88);
        ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);ps.format("p91=%d\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%d\n",p102);ps.format("p103=%d\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);ps.format("p106=%d\n",p106);
        ps.format("p107=%d\n",p107);ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%d\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);ps.format("p115=%d\n",p115);
        ps.format("p116=%d\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%d\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%d\n",p124);
        ps.format("p125=%d\n",p125);ps.format("p126=%d\n",p126);
        nativeFnc8(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc9(double p0,byte p1,int p2,byte p3,int p4,byte p5,int p6,float p7,float p8
        ,int p9,double p10,int p11,float p12,float p13,double p14,int p15,float p16
        ,byte p17,float p18,int p19,float p20,int p21,float p22,float p23,double p24
        ,byte p25,byte p26,byte p27,byte p28,float p29,byte p30,byte p31,byte p32
        ,double p33,int p34,double p35,float p36,int p37,double p38,int p39,double p40
        ,byte p41,double p42,float p43,float p44,double p45,float p46,int p47,float p48
        ,int p49,float p50,byte p51,byte p52,int p53,int p54,float p55,double p56
        ,int p57,int p58,float p59,int p60,byte p61,int p62,double p63,double p64
        ,int p65,byte p66,double p67,int p68,byte p69,byte p70,int p71,float p72
        ,float p73,byte p74,int p75,byte p76,double p77,float p78,double p79,byte p80
        ,int p81,int p82,byte p83,double p84,float p85,double p86,double p87,float p88
        ,byte p89,byte p90,double p91,double p92,double p93,float p94,double p95
        ,float p96,double p97,float p98,byte p99,float p100,byte p101,byte p102
        ,float p103,double p104,byte p105,float p106,double p107,double p108,int p109
        ,float p110,int p111,int p112,byte p113,double p114,byte p115,double p116
        ,int p117,double p118,float p119,byte p120,float p121,float p122,float p123
        ,double p124,int p125,byte p126    );
    private static void nativeFnc9_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();byte  p1=getRndByte();int  p2=getRndInt();
        byte  p3=getRndByte();int  p4=getRndInt();byte  p5=getRndByte();int  p6=getRndInt();
        float  p7=getRndFloat();float  p8=getRndFloat();int  p9=getRndInt();double  p10=getRndDouble();
        int  p11=getRndInt();float  p12=getRndFloat();float  p13=getRndFloat();
        double  p14=getRndDouble();int  p15=getRndInt();float  p16=getRndFloat();
        byte  p17=getRndByte();float  p18=getRndFloat();int  p19=getRndInt();float  p20=getRndFloat();
        int  p21=getRndInt();float  p22=getRndFloat();float  p23=getRndFloat();
        double  p24=getRndDouble();byte  p25=getRndByte();byte  p26=getRndByte();
        byte  p27=getRndByte();byte  p28=getRndByte();float  p29=getRndFloat();
        byte  p30=getRndByte();byte  p31=getRndByte();byte  p32=getRndByte();double  p33=getRndDouble();
        int  p34=getRndInt();double  p35=getRndDouble();float  p36=getRndFloat();
        int  p37=getRndInt();double  p38=getRndDouble();int  p39=getRndInt();double  p40=getRndDouble();
        byte  p41=getRndByte();double  p42=getRndDouble();float  p43=getRndFloat();
        float  p44=getRndFloat();double  p45=getRndDouble();float  p46=getRndFloat();
        int  p47=getRndInt();float  p48=getRndFloat();int  p49=getRndInt();float  p50=getRndFloat();
        byte  p51=getRndByte();byte  p52=getRndByte();int  p53=getRndInt();int  p54=getRndInt();
        float  p55=getRndFloat();double  p56=getRndDouble();int  p57=getRndInt();
        int  p58=getRndInt();float  p59=getRndFloat();int  p60=getRndInt();byte  p61=getRndByte();
        int  p62=getRndInt();double  p63=getRndDouble();double  p64=getRndDouble();
        int  p65=getRndInt();byte  p66=getRndByte();double  p67=getRndDouble();
        int  p68=getRndInt();byte  p69=getRndByte();byte  p70=getRndByte();int  p71=getRndInt();
        float  p72=getRndFloat();float  p73=getRndFloat();byte  p74=getRndByte();
        int  p75=getRndInt();byte  p76=getRndByte();double  p77=getRndDouble();
        float  p78=getRndFloat();double  p79=getRndDouble();byte  p80=getRndByte();
        int  p81=getRndInt();int  p82=getRndInt();byte  p83=getRndByte();double  p84=getRndDouble();
        float  p85=getRndFloat();double  p86=getRndDouble();double  p87=getRndDouble();
        float  p88=getRndFloat();byte  p89=getRndByte();byte  p90=getRndByte();
        double  p91=getRndDouble();double  p92=getRndDouble();double  p93=getRndDouble();
        float  p94=getRndFloat();double  p95=getRndDouble();float  p96=getRndFloat();
        double  p97=getRndDouble();float  p98=getRndFloat();byte  p99=getRndByte();
        float  p100=getRndFloat();byte  p101=getRndByte();byte  p102=getRndByte();
        float  p103=getRndFloat();double  p104=getRndDouble();byte  p105=getRndByte();
        float  p106=getRndFloat();double  p107=getRndDouble();double  p108=getRndDouble();
        int  p109=getRndInt();float  p110=getRndFloat();int  p111=getRndInt();int  p112=getRndInt();
        byte  p113=getRndByte();double  p114=getRndDouble();byte  p115=getRndByte();
        double  p116=getRndDouble();int  p117=getRndInt();double  p118=getRndDouble();
        float  p119=getRndFloat();byte  p120=getRndByte();float  p121=getRndFloat();
        float  p122=getRndFloat();float  p123=getRndFloat();double  p124=getRndDouble();
        int  p125=getRndInt();byte  p126=getRndByte();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%d\n",p1);ps.format("p2=%d\n",p2);ps.format("p3=%d\n",p3);
        ps.format("p4=%d\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%d\n",p25);ps.format("p26=%d\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);
        ps.format("p31=%d\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%d\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%d\n",p51);
        ps.format("p52=%d\n",p52);ps.format("p53=%d\n",p53);ps.format("p54=%d\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%d\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%d\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%d\n",p65);ps.format("p66=%d\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%d\n",p69);
        ps.format("p70=%d\n",p70);ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);ps.format("p81=%d\n",p81);
        ps.format("p82=%d\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%d\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%d\n",p101);ps.format("p102=%d\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);
        ps.format("p112=%d\n",p112);ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%d\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);ps.format("p126=%d\n",p126);

        nativeFnc9(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc10(double p0,double p1,float p2,double p3,float p4,float p5,double p6,float p7
        ,float p8,double p9,float p10,float p11,float p12,float p13,double p14,float p15
        ,float p16,double p17,double p18,double p19,double p20,float p21,double p22
        ,double p23,double p24,double p25,double p26,float p27,float p28,float p29
        ,double p30,float p31,float p32,float p33,double p34,double p35,double p36
        ,double p37,float p38,float p39,double p40,float p41,double p42,double p43
        ,float p44,double p45,double p46,double p47,float p48,double p49,double p50
        ,float p51,float p52,float p53,float p54,float p55,double p56,float p57
        ,float p58,double p59,float p60,double p61,double p62,float p63,float p64
        ,float p65,float p66,double p67,float p68,double p69,double p70,float p71
        ,double p72,double p73,double p74,float p75,double p76,double p77,float p78
        ,double p79,float p80,float p81,float p82,float p83,float p84,float p85
        ,double p86,double p87,float p88,float p89,double p90,double p91,double p92
        ,double p93,float p94,double p95,float p96,double p97,double p98,float p99
        ,float p100,float p101,double p102,float p103,float p104,float p105,double p106
        ,double p107,double p108,float p109,double p110,float p111,double p112,float p113
        ,float p114,double p115,double p116,double p117,double p118,float p119,double p120
        ,float p121,double p122,float p123,float p124,float p125,float p126    );
    private static void nativeFnc10_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();float  p2=getRndFloat();
        double  p3=getRndDouble();float  p4=getRndFloat();float  p5=getRndFloat();
        double  p6=getRndDouble();float  p7=getRndFloat();float  p8=getRndFloat();
        double  p9=getRndDouble();float  p10=getRndFloat();float  p11=getRndFloat();
        float  p12=getRndFloat();float  p13=getRndFloat();double  p14=getRndDouble();
        float  p15=getRndFloat();float  p16=getRndFloat();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        float  p21=getRndFloat();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        float  p27=getRndFloat();float  p28=getRndFloat();float  p29=getRndFloat();
        double  p30=getRndDouble();float  p31=getRndFloat();float  p32=getRndFloat();
        float  p33=getRndFloat();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();float  p38=getRndFloat();
        float  p39=getRndFloat();double  p40=getRndDouble();float  p41=getRndFloat();
        double  p42=getRndDouble();double  p43=getRndDouble();float  p44=getRndFloat();
        double  p45=getRndDouble();double  p46=getRndDouble();double  p47=getRndDouble();
        float  p48=getRndFloat();double  p49=getRndDouble();double  p50=getRndDouble();
        float  p51=getRndFloat();float  p52=getRndFloat();float  p53=getRndFloat();
        float  p54=getRndFloat();float  p55=getRndFloat();double  p56=getRndDouble();
        float  p57=getRndFloat();float  p58=getRndFloat();double  p59=getRndDouble();
        float  p60=getRndFloat();double  p61=getRndDouble();double  p62=getRndDouble();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        float  p66=getRndFloat();double  p67=getRndDouble();float  p68=getRndFloat();
        double  p69=getRndDouble();double  p70=getRndDouble();float  p71=getRndFloat();
        double  p72=getRndDouble();double  p73=getRndDouble();double  p74=getRndDouble();
        float  p75=getRndFloat();double  p76=getRndDouble();double  p77=getRndDouble();
        float  p78=getRndFloat();double  p79=getRndDouble();float  p80=getRndFloat();
        float  p81=getRndFloat();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();float  p85=getRndFloat();double  p86=getRndDouble();
        double  p87=getRndDouble();float  p88=getRndFloat();float  p89=getRndFloat();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();float  p94=getRndFloat();double  p95=getRndDouble();
        float  p96=getRndFloat();double  p97=getRndDouble();double  p98=getRndDouble();
        float  p99=getRndFloat();float  p100=getRndFloat();float  p101=getRndFloat();
        double  p102=getRndDouble();float  p103=getRndFloat();float  p104=getRndFloat();
        float  p105=getRndFloat();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();float  p109=getRndFloat();double  p110=getRndDouble();
        float  p111=getRndFloat();double  p112=getRndDouble();float  p113=getRndFloat();
        float  p114=getRndFloat();double  p115=getRndDouble();double  p116=getRndDouble();
        double  p117=getRndDouble();double  p118=getRndDouble();float  p119=getRndFloat();
        double  p120=getRndDouble();float  p121=getRndFloat();double  p122=getRndDouble();
        float  p123=getRndFloat();float  p124=getRndFloat();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc10(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc11(double p0,float p1,float p2,double p3,int p4,int p5,float p6,double p7,int p8
        ,int p9,int p10,int p11,int p12,double p13,float p14,float p15,double p16
        ,float p17,int p18,int p19,double p20,float p21,int p22,int p23,float p24
        ,int p25,int p26,int p27,float p28,float p29,double p30,double p31,int p32
        ,double p33,float p34,float p35,float p36,int p37,float p38,double p39,float p40
        ,double p41,double p42,double p43,double p44,int p45,int p46,float p47,float p48
        ,float p49,float p50,double p51,double p52,double p53,int p54,float p55
        ,int p56,double p57,double p58,double p59,double p60,int p61,float p62,int p63
        ,float p64,double p65,double p66,float p67,double p68,double p69,int p70
        ,double p71,float p72,double p73,int p74,float p75,float p76,float p77,float p78
        ,int p79,float p80,int p81,double p82,float p83,double p84,float p85,float p86
        ,float p87,double p88,float p89,int p90,float p91,double p92,float p93,double p94
        ,double p95,int p96,int p97,float p98,double p99,float p100,int p101,double p102
        ,double p103,float p104,double p105,int p106,float p107,float p108,int p109
        ,double p110,float p111,double p112,int p113,int p114,float p115,float p116
        ,int p117,double p118,float p119,float p120,float p121,int p122,double p123
        ,int p124,float p125,double p126    );
    private static void nativeFnc11_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();float  p1=getRndFloat();float  p2=getRndFloat();
        double  p3=getRndDouble();int  p4=getRndInt();int  p5=getRndInt();float  p6=getRndFloat();
        double  p7=getRndDouble();int  p8=getRndInt();int  p9=getRndInt();int  p10=getRndInt();
        int  p11=getRndInt();int  p12=getRndInt();double  p13=getRndDouble();float  p14=getRndFloat();
        float  p15=getRndFloat();double  p16=getRndDouble();float  p17=getRndFloat();
        int  p18=getRndInt();int  p19=getRndInt();double  p20=getRndDouble();float  p21=getRndFloat();
        int  p22=getRndInt();int  p23=getRndInt();float  p24=getRndFloat();int  p25=getRndInt();
        int  p26=getRndInt();int  p27=getRndInt();float  p28=getRndFloat();float  p29=getRndFloat();
        double  p30=getRndDouble();double  p31=getRndDouble();int  p32=getRndInt();
        double  p33=getRndDouble();float  p34=getRndFloat();float  p35=getRndFloat();
        float  p36=getRndFloat();int  p37=getRndInt();float  p38=getRndFloat();
        double  p39=getRndDouble();float  p40=getRndFloat();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        int  p45=getRndInt();int  p46=getRndInt();float  p47=getRndFloat();float  p48=getRndFloat();
        float  p49=getRndFloat();float  p50=getRndFloat();double  p51=getRndDouble();
        double  p52=getRndDouble();double  p53=getRndDouble();int  p54=getRndInt();
        float  p55=getRndFloat();int  p56=getRndInt();double  p57=getRndDouble();
        double  p58=getRndDouble();double  p59=getRndDouble();double  p60=getRndDouble();
        int  p61=getRndInt();float  p62=getRndFloat();int  p63=getRndInt();float  p64=getRndFloat();
        double  p65=getRndDouble();double  p66=getRndDouble();float  p67=getRndFloat();
        double  p68=getRndDouble();double  p69=getRndDouble();int  p70=getRndInt();
        double  p71=getRndDouble();float  p72=getRndFloat();double  p73=getRndDouble();
        int  p74=getRndInt();float  p75=getRndFloat();float  p76=getRndFloat();
        float  p77=getRndFloat();float  p78=getRndFloat();int  p79=getRndInt();
        float  p80=getRndFloat();int  p81=getRndInt();double  p82=getRndDouble();
        float  p83=getRndFloat();double  p84=getRndDouble();float  p85=getRndFloat();
        float  p86=getRndFloat();float  p87=getRndFloat();double  p88=getRndDouble();
        float  p89=getRndFloat();int  p90=getRndInt();float  p91=getRndFloat();
        double  p92=getRndDouble();float  p93=getRndFloat();double  p94=getRndDouble();
        double  p95=getRndDouble();int  p96=getRndInt();int  p97=getRndInt();float  p98=getRndFloat();
        double  p99=getRndDouble();float  p100=getRndFloat();int  p101=getRndInt();
        double  p102=getRndDouble();double  p103=getRndDouble();float  p104=getRndFloat();
        double  p105=getRndDouble();int  p106=getRndInt();float  p107=getRndFloat();
        float  p108=getRndFloat();int  p109=getRndInt();double  p110=getRndDouble();
        float  p111=getRndFloat();double  p112=getRndDouble();int  p113=getRndInt();
        int  p114=getRndInt();float  p115=getRndFloat();float  p116=getRndFloat();
        int  p117=getRndInt();double  p118=getRndDouble();float  p119=getRndFloat();
        float  p120=getRndFloat();float  p121=getRndFloat();int  p122=getRndInt();
        double  p123=getRndDouble();int  p124=getRndInt();float  p125=getRndFloat();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%d\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%d\n",p10);
        ps.format("p11=%d\n",p11);ps.format("p12=%d\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%d\n",p18);ps.format("p19=%d\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%d\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);
        ps.format("p26=%d\n",p26);ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%d\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%d\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%d\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%d\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%d\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%d\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%d\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%d\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc11(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc12(double p0,float p1,double p2,float p3,float p4,byte p5,byte p6,double p7
        ,double p8,byte p9,byte p10,float p11,byte p12,byte p13,byte p14,byte p15
        ,byte p16,byte p17,double p18,double p19,byte p20,float p21,byte p22,double p23
        ,byte p24,float p25,double p26,float p27,byte p28,byte p29,float p30,byte p31
        ,float p32,float p33,byte p34,byte p35,float p36,double p37,float p38,float p39
        ,double p40,double p41,float p42,float p43,byte p44,byte p45,double p46
        ,float p47,double p48,float p49,byte p50,double p51,double p52,float p53
        ,double p54,byte p55,float p56,byte p57,byte p58,byte p59,float p60,double p61
        ,float p62,byte p63,double p64,double p65,float p66,float p67,float p68
        ,float p69,float p70,double p71,float p72,float p73,double p74,byte p75
        ,float p76,byte p77,byte p78,byte p79,double p80,double p81,byte p82,float p83
        ,double p84,byte p85,float p86,double p87,float p88,float p89,float p90
        ,float p91,double p92,byte p93,float p94,byte p95,byte p96,byte p97,float p98
        ,byte p99,float p100,double p101,float p102,byte p103,byte p104,float p105
        ,double p106,byte p107,float p108,byte p109,double p110,float p111,float p112
        ,byte p113,float p114,byte p115,float p116,double p117,double p118,double p119
        ,float p120,double p121,byte p122,byte p123,float p124,double p125,double p126
            );
    private static void nativeFnc12_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();float  p1=getRndFloat();double  p2=getRndDouble();
        float  p3=getRndFloat();float  p4=getRndFloat();byte  p5=getRndByte();byte  p6=getRndByte();
        double  p7=getRndDouble();double  p8=getRndDouble();byte  p9=getRndByte();
        byte  p10=getRndByte();float  p11=getRndFloat();byte  p12=getRndByte();
        byte  p13=getRndByte();byte  p14=getRndByte();byte  p15=getRndByte();byte  p16=getRndByte();
        byte  p17=getRndByte();double  p18=getRndDouble();double  p19=getRndDouble();
        byte  p20=getRndByte();float  p21=getRndFloat();byte  p22=getRndByte();
        double  p23=getRndDouble();byte  p24=getRndByte();float  p25=getRndFloat();
        double  p26=getRndDouble();float  p27=getRndFloat();byte  p28=getRndByte();
        byte  p29=getRndByte();float  p30=getRndFloat();byte  p31=getRndByte();
        float  p32=getRndFloat();float  p33=getRndFloat();byte  p34=getRndByte();
        byte  p35=getRndByte();float  p36=getRndFloat();double  p37=getRndDouble();
        float  p38=getRndFloat();float  p39=getRndFloat();double  p40=getRndDouble();
        double  p41=getRndDouble();float  p42=getRndFloat();float  p43=getRndFloat();
        byte  p44=getRndByte();byte  p45=getRndByte();double  p46=getRndDouble();
        float  p47=getRndFloat();double  p48=getRndDouble();float  p49=getRndFloat();
        byte  p50=getRndByte();double  p51=getRndDouble();double  p52=getRndDouble();
        float  p53=getRndFloat();double  p54=getRndDouble();byte  p55=getRndByte();
        float  p56=getRndFloat();byte  p57=getRndByte();byte  p58=getRndByte();
        byte  p59=getRndByte();float  p60=getRndFloat();double  p61=getRndDouble();
        float  p62=getRndFloat();byte  p63=getRndByte();double  p64=getRndDouble();
        double  p65=getRndDouble();float  p66=getRndFloat();float  p67=getRndFloat();
        float  p68=getRndFloat();float  p69=getRndFloat();float  p70=getRndFloat();
        double  p71=getRndDouble();float  p72=getRndFloat();float  p73=getRndFloat();
        double  p74=getRndDouble();byte  p75=getRndByte();float  p76=getRndFloat();
        byte  p77=getRndByte();byte  p78=getRndByte();byte  p79=getRndByte();double  p80=getRndDouble();
        double  p81=getRndDouble();byte  p82=getRndByte();float  p83=getRndFloat();
        double  p84=getRndDouble();byte  p85=getRndByte();float  p86=getRndFloat();
        double  p87=getRndDouble();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();double  p92=getRndDouble();
        byte  p93=getRndByte();float  p94=getRndFloat();byte  p95=getRndByte();
        byte  p96=getRndByte();byte  p97=getRndByte();float  p98=getRndFloat();
        byte  p99=getRndByte();float  p100=getRndFloat();double  p101=getRndDouble();
        float  p102=getRndFloat();byte  p103=getRndByte();byte  p104=getRndByte();
        float  p105=getRndFloat();double  p106=getRndDouble();byte  p107=getRndByte();
        float  p108=getRndFloat();byte  p109=getRndByte();double  p110=getRndDouble();
        float  p111=getRndFloat();float  p112=getRndFloat();byte  p113=getRndByte();
        float  p114=getRndFloat();byte  p115=getRndByte();float  p116=getRndFloat();
        double  p117=getRndDouble();double  p118=getRndDouble();double  p119=getRndDouble();
        float  p120=getRndFloat();double  p121=getRndDouble();byte  p122=getRndByte();
        byte  p123=getRndByte();float  p124=getRndFloat();double  p125=getRndDouble();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%d\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%d\n",p12);ps.format("p13=%d\n",p13);
        ps.format("p14=%d\n",p14);ps.format("p15=%d\n",p15);ps.format("p16=%d\n",p16);
        ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%d\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);
        ps.format("p29=%d\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%d\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%d\n",p44);ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%d\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%d\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%d\n",p77);ps.format("p78=%d\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%d\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%d\n",p103);
        ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%d\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%d\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc12(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc13(float p0,float p1,float p2,float p3,int p4,float p5,int p6,float p7,int p8
        ,float p9,float p10,float p11,float p12,float p13,int p14,float p15,float p16
        ,int p17,int p18,float p19,float p20,int p21,int p22,int p23,int p24,int p25
        ,float p26,int p27,int p28,float p29,int p30,float p31,float p32,float p33
        ,float p34,int p35,int p36,float p37,float p38,float p39,int p40,float p41
        ,int p42,float p43,int p44,float p45,float p46,float p47,int p48,float p49
        ,int p50,int p51,float p52,int p53,float p54,float p55,float p56,float p57
        ,float p58,float p59,float p60,float p61,int p62,float p63,float p64,float p65
        ,int p66,int p67,float p68,float p69,float p70,int p71,float p72,float p73
        ,int p74,float p75,float p76,float p77,int p78,int p79,int p80,float p81
        ,int p82,int p83,float p84,int p85,int p86,float p87,float p88,float p89
        ,float p90,int p91,int p92,float p93,float p94,float p95,float p96,float p97
        ,float p98,int p99,float p100,int p101,float p102,float p103,int p104,float p105
        ,float p106,float p107,int p108,float p109,float p110,float p111,float p112
        ,float p113,float p114,float p115,float p116,int p117,int p118,float p119
        ,float p120,float p121,int p122,float p123,int p124,float p125,float p126
            );
    private static void nativeFnc13_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();int  p4=getRndInt();float  p5=getRndFloat();int  p6=getRndInt();
        float  p7=getRndFloat();int  p8=getRndInt();float  p9=getRndFloat();float  p10=getRndFloat();
        float  p11=getRndFloat();float  p12=getRndFloat();float  p13=getRndFloat();
        int  p14=getRndInt();float  p15=getRndFloat();float  p16=getRndFloat();
        int  p17=getRndInt();int  p18=getRndInt();float  p19=getRndFloat();float  p20=getRndFloat();
        int  p21=getRndInt();int  p22=getRndInt();int  p23=getRndInt();int  p24=getRndInt();
        int  p25=getRndInt();float  p26=getRndFloat();int  p27=getRndInt();int  p28=getRndInt();
        float  p29=getRndFloat();int  p30=getRndInt();float  p31=getRndFloat();
        float  p32=getRndFloat();float  p33=getRndFloat();float  p34=getRndFloat();
        int  p35=getRndInt();int  p36=getRndInt();float  p37=getRndFloat();float  p38=getRndFloat();
        float  p39=getRndFloat();int  p40=getRndInt();float  p41=getRndFloat();
        int  p42=getRndInt();float  p43=getRndFloat();int  p44=getRndInt();float  p45=getRndFloat();
        float  p46=getRndFloat();float  p47=getRndFloat();int  p48=getRndInt();
        float  p49=getRndFloat();int  p50=getRndInt();int  p51=getRndInt();float  p52=getRndFloat();
        int  p53=getRndInt();float  p54=getRndFloat();float  p55=getRndFloat();
        float  p56=getRndFloat();float  p57=getRndFloat();float  p58=getRndFloat();
        float  p59=getRndFloat();float  p60=getRndFloat();float  p61=getRndFloat();
        int  p62=getRndInt();float  p63=getRndFloat();float  p64=getRndFloat();
        float  p65=getRndFloat();int  p66=getRndInt();int  p67=getRndInt();float  p68=getRndFloat();
        float  p69=getRndFloat();float  p70=getRndFloat();int  p71=getRndInt();
        float  p72=getRndFloat();float  p73=getRndFloat();int  p74=getRndInt();
        float  p75=getRndFloat();float  p76=getRndFloat();float  p77=getRndFloat();
        int  p78=getRndInt();int  p79=getRndInt();int  p80=getRndInt();float  p81=getRndFloat();
        int  p82=getRndInt();int  p83=getRndInt();float  p84=getRndFloat();int  p85=getRndInt();
        int  p86=getRndInt();float  p87=getRndFloat();float  p88=getRndFloat();
        float  p89=getRndFloat();float  p90=getRndFloat();int  p91=getRndInt();
        int  p92=getRndInt();float  p93=getRndFloat();float  p94=getRndFloat();
        float  p95=getRndFloat();float  p96=getRndFloat();float  p97=getRndFloat();
        float  p98=getRndFloat();int  p99=getRndInt();float  p100=getRndFloat();
        int  p101=getRndInt();float  p102=getRndFloat();float  p103=getRndFloat();
        int  p104=getRndInt();float  p105=getRndFloat();float  p106=getRndFloat();
        float  p107=getRndFloat();int  p108=getRndInt();float  p109=getRndFloat();
        float  p110=getRndFloat();float  p111=getRndFloat();float  p112=getRndFloat();
        float  p113=getRndFloat();float  p114=getRndFloat();float  p115=getRndFloat();
        float  p116=getRndFloat();int  p117=getRndInt();int  p118=getRndInt();float  p119=getRndFloat();
        float  p120=getRndFloat();float  p121=getRndFloat();int  p122=getRndInt();
        float  p123=getRndFloat();int  p124=getRndInt();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%d\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%d\n",p17);ps.format("p18=%d\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%d\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%d\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%d\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%d\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%d\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%d\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%d\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%d\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%d\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);
        ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);
        ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);
        ps.format("p92=%d\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%d\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc13(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc14(float p0,float p1,float p2,float p3,byte p4,float p5,float p6,byte p7,float p8
        ,byte p9,byte p10,byte p11,float p12,float p13,float p14,byte p15,float p16
        ,byte p17,float p18,float p19,float p20,float p21,byte p22,byte p23,byte p24
        ,float p25,byte p26,float p27,float p28,float p29,float p30,float p31,float p32
        ,float p33,float p34,float p35,byte p36,float p37,float p38,float p39,float p40
        ,float p41,float p42,byte p43,byte p44,float p45,byte p46,float p47,byte p48
        ,float p49,float p50,float p51,float p52,float p53,float p54,float p55,byte p56
        ,float p57,byte p58,float p59,byte p60,float p61,byte p62,byte p63,float p64
        ,float p65,float p66,byte p67,float p68,float p69,float p70,byte p71,float p72
        ,byte p73,float p74,float p75,float p76,byte p77,float p78,float p79,byte p80
        ,float p81,float p82,float p83,float p84,byte p85,float p86,byte p87,byte p88
        ,byte p89,float p90,byte p91,float p92,float p93,byte p94,float p95,float p96
        ,float p97,float p98,float p99,float p100,float p101,float p102,byte p103
        ,byte p104,float p105,float p106,float p107,float p108,float p109,float p110
        ,byte p111,float p112,float p113,byte p114,byte p115,float p116,float p117
        ,float p118,float p119,byte p120,byte p121,byte p122,byte p123,byte p124
        ,float p125,float p126    );
    private static void nativeFnc14_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();byte  p4=getRndByte();float  p5=getRndFloat();float  p6=getRndFloat();
        byte  p7=getRndByte();float  p8=getRndFloat();byte  p9=getRndByte();byte  p10=getRndByte();
        byte  p11=getRndByte();float  p12=getRndFloat();float  p13=getRndFloat();
        float  p14=getRndFloat();byte  p15=getRndByte();float  p16=getRndFloat();
        byte  p17=getRndByte();float  p18=getRndFloat();float  p19=getRndFloat();
        float  p20=getRndFloat();float  p21=getRndFloat();byte  p22=getRndByte();
        byte  p23=getRndByte();byte  p24=getRndByte();float  p25=getRndFloat();
        byte  p26=getRndByte();float  p27=getRndFloat();float  p28=getRndFloat();
        float  p29=getRndFloat();float  p30=getRndFloat();float  p31=getRndFloat();
        float  p32=getRndFloat();float  p33=getRndFloat();float  p34=getRndFloat();
        float  p35=getRndFloat();byte  p36=getRndByte();float  p37=getRndFloat();
        float  p38=getRndFloat();float  p39=getRndFloat();float  p40=getRndFloat();
        float  p41=getRndFloat();float  p42=getRndFloat();byte  p43=getRndByte();
        byte  p44=getRndByte();float  p45=getRndFloat();byte  p46=getRndByte();
        float  p47=getRndFloat();byte  p48=getRndByte();float  p49=getRndFloat();
        float  p50=getRndFloat();float  p51=getRndFloat();float  p52=getRndFloat();
        float  p53=getRndFloat();float  p54=getRndFloat();float  p55=getRndFloat();
        byte  p56=getRndByte();float  p57=getRndFloat();byte  p58=getRndByte();
        float  p59=getRndFloat();byte  p60=getRndByte();float  p61=getRndFloat();
        byte  p62=getRndByte();byte  p63=getRndByte();float  p64=getRndFloat();
        float  p65=getRndFloat();float  p66=getRndFloat();byte  p67=getRndByte();
        float  p68=getRndFloat();float  p69=getRndFloat();float  p70=getRndFloat();
        byte  p71=getRndByte();float  p72=getRndFloat();byte  p73=getRndByte();
        float  p74=getRndFloat();float  p75=getRndFloat();float  p76=getRndFloat();
        byte  p77=getRndByte();float  p78=getRndFloat();float  p79=getRndFloat();
        byte  p80=getRndByte();float  p81=getRndFloat();float  p82=getRndFloat();
        float  p83=getRndFloat();float  p84=getRndFloat();byte  p85=getRndByte();
        float  p86=getRndFloat();byte  p87=getRndByte();byte  p88=getRndByte();
        byte  p89=getRndByte();float  p90=getRndFloat();byte  p91=getRndByte();
        float  p92=getRndFloat();float  p93=getRndFloat();byte  p94=getRndByte();
        float  p95=getRndFloat();float  p96=getRndFloat();float  p97=getRndFloat();
        float  p98=getRndFloat();float  p99=getRndFloat();float  p100=getRndFloat();
        float  p101=getRndFloat();float  p102=getRndFloat();byte  p103=getRndByte();
        byte  p104=getRndByte();float  p105=getRndFloat();float  p106=getRndFloat();
        float  p107=getRndFloat();float  p108=getRndFloat();float  p109=getRndFloat();
        float  p110=getRndFloat();byte  p111=getRndByte();float  p112=getRndFloat();
        float  p113=getRndFloat();byte  p114=getRndByte();byte  p115=getRndByte();
        float  p116=getRndFloat();float  p117=getRndFloat();float  p118=getRndFloat();
        float  p119=getRndFloat();byte  p120=getRndByte();byte  p121=getRndByte();
        byte  p122=getRndByte();byte  p123=getRndByte();byte  p124=getRndByte();
        float  p125=getRndFloat();float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%d\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%d\n",p10);ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%d\n",p22);ps.format("p23=%d\n",p23);ps.format("p24=%d\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%d\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%d\n",p43);ps.format("p44=%d\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%d\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%d\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%d\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%d\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%e\n",p90);
        ps.format("p91=%d\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%d\n",p103);ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);
        ps.format("p115=%d\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%d\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);
        ps.format("p124=%d\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc14(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc15(float p0,float p1,byte p2,float p3,float p4,float p5,byte p6,float p7,byte p8
        ,int p9,float p10,float p11,byte p12,byte p13,byte p14,byte p15,byte p16
        ,float p17,byte p18,float p19,int p20,byte p21,float p22,int p23,byte p24
        ,float p25,float p26,byte p27,float p28,int p29,float p30,byte p31,float p32
        ,float p33,byte p34,float p35,int p36,byte p37,int p38,float p39,float p40
        ,byte p41,float p42,byte p43,int p44,float p45,float p46,byte p47,int p48
        ,float p49,int p50,float p51,float p52,int p53,byte p54,int p55,byte p56
        ,float p57,float p58,float p59,float p60,byte p61,byte p62,byte p63,byte p64
        ,float p65,int p66,int p67,int p68,float p69,float p70,int p71,float p72
        ,float p73,float p74,int p75,float p76,float p77,int p78,float p79,int p80
        ,float p81,float p82,float p83,float p84,byte p85,int p86,byte p87,float p88
        ,float p89,float p90,int p91,float p92,byte p93,float p94,byte p95,int p96
        ,float p97,float p98,int p99,float p100,byte p101,byte p102,float p103,byte p104
        ,byte p105,byte p106,int p107,int p108,float p109,float p110,int p111,float p112
        ,byte p113,int p114,float p115,byte p116,byte p117,float p118,float p119
        ,int p120,float p121,float p122,int p123,float p124,byte p125,float p126
            );
    private static void nativeFnc15_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();byte  p2=getRndByte();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        byte  p6=getRndByte();float  p7=getRndFloat();byte  p8=getRndByte();int  p9=getRndInt();
        float  p10=getRndFloat();float  p11=getRndFloat();byte  p12=getRndByte();
        byte  p13=getRndByte();byte  p14=getRndByte();byte  p15=getRndByte();byte  p16=getRndByte();
        float  p17=getRndFloat();byte  p18=getRndByte();float  p19=getRndFloat();
        int  p20=getRndInt();byte  p21=getRndByte();float  p22=getRndFloat();int  p23=getRndInt();
        byte  p24=getRndByte();float  p25=getRndFloat();float  p26=getRndFloat();
        byte  p27=getRndByte();float  p28=getRndFloat();int  p29=getRndInt();float  p30=getRndFloat();
        byte  p31=getRndByte();float  p32=getRndFloat();float  p33=getRndFloat();
        byte  p34=getRndByte();float  p35=getRndFloat();int  p36=getRndInt();byte  p37=getRndByte();
        int  p38=getRndInt();float  p39=getRndFloat();float  p40=getRndFloat();
        byte  p41=getRndByte();float  p42=getRndFloat();byte  p43=getRndByte();
        int  p44=getRndInt();float  p45=getRndFloat();float  p46=getRndFloat();
        byte  p47=getRndByte();int  p48=getRndInt();float  p49=getRndFloat();int  p50=getRndInt();
        float  p51=getRndFloat();float  p52=getRndFloat();int  p53=getRndInt();
        byte  p54=getRndByte();int  p55=getRndInt();byte  p56=getRndByte();float  p57=getRndFloat();
        float  p58=getRndFloat();float  p59=getRndFloat();float  p60=getRndFloat();
        byte  p61=getRndByte();byte  p62=getRndByte();byte  p63=getRndByte();byte  p64=getRndByte();
        float  p65=getRndFloat();int  p66=getRndInt();int  p67=getRndInt();int  p68=getRndInt();
        float  p69=getRndFloat();float  p70=getRndFloat();int  p71=getRndInt();
        float  p72=getRndFloat();float  p73=getRndFloat();float  p74=getRndFloat();
        int  p75=getRndInt();float  p76=getRndFloat();float  p77=getRndFloat();
        int  p78=getRndInt();float  p79=getRndFloat();int  p80=getRndInt();float  p81=getRndFloat();
        float  p82=getRndFloat();float  p83=getRndFloat();float  p84=getRndFloat();
        byte  p85=getRndByte();int  p86=getRndInt();byte  p87=getRndByte();float  p88=getRndFloat();
        float  p89=getRndFloat();float  p90=getRndFloat();int  p91=getRndInt();
        float  p92=getRndFloat();byte  p93=getRndByte();float  p94=getRndFloat();
        byte  p95=getRndByte();int  p96=getRndInt();float  p97=getRndFloat();float  p98=getRndFloat();
        int  p99=getRndInt();float  p100=getRndFloat();byte  p101=getRndByte();
        byte  p102=getRndByte();float  p103=getRndFloat();byte  p104=getRndByte();
        byte  p105=getRndByte();byte  p106=getRndByte();int  p107=getRndInt();int  p108=getRndInt();
        float  p109=getRndFloat();float  p110=getRndFloat();int  p111=getRndInt();
        float  p112=getRndFloat();byte  p113=getRndByte();int  p114=getRndInt();
        float  p115=getRndFloat();byte  p116=getRndByte();byte  p117=getRndByte();
        float  p118=getRndFloat();float  p119=getRndFloat();int  p120=getRndInt();
        float  p121=getRndFloat();float  p122=getRndFloat();int  p123=getRndInt();
        float  p124=getRndFloat();byte  p125=getRndByte();float  p126=getRndFloat();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%d\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%d\n",p8);
        ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%d\n",p12);ps.format("p13=%d\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%d\n",p15);ps.format("p16=%d\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%d\n",p20);
        ps.format("p21=%d\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);
        ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%d\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%d\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%d\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%d\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%d\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%d\n",p86);
        ps.format("p87=%d\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%d\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%d\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%d\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%d\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%d\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc15(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc16(double p0,int p1,double p2,double p3,double p4,double p5,int p6,double p7
        ,double p8,double p9,double p10,double p11,double p12,double p13,double p14
        ,double p15,double p16,double p17,int p18,int p19,double p20,double p21
        ,double p22,double p23,double p24,double p25,int p26,double p27,double p28
        ,double p29,double p30,int p31,double p32,int p33,int p34,double p35,double p36
        ,double p37,int p38,double p39,double p40,double p41,double p42,int p43
        ,double p44,double p45,int p46,double p47,double p48,int p49,double p50
        ,double p51,int p52,double p53,int p54,double p55,int p56,int p57,double p58
        ,int p59,int p60,double p61,int p62,int p63,int p64,double p65,int p66,int p67
        ,int p68,int p69,int p70,double p71,int p72,int p73,double p74,double p75
        ,double p76,double p77,double p78,double p79,int p80,double p81,double p82
        ,double p83,double p84,double p85,double p86,double p87,double p88,double p89
        ,int p90,double p91,double p92,double p93,double p94,double p95,double p96
        ,double p97,int p98,double p99,int p100,double p101,double p102,double p103
        ,int p104,double p105,int p106,double p107,int p108,int p109,double p110
        ,double p111,double p112,int p113,double p114,double p115,int p116,double p117
        ,int p118,int p119,double p120,double p121,double p122,double p123,double p124
        ,double p125,int p126    );
    private static void nativeFnc16_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();int  p1=getRndInt();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        int  p6=getRndInt();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        int  p18=getRndInt();int  p19=getRndInt();double  p20=getRndDouble();double  p21=getRndDouble();
        double  p22=getRndDouble();double  p23=getRndDouble();double  p24=getRndDouble();
        double  p25=getRndDouble();int  p26=getRndInt();double  p27=getRndDouble();
        double  p28=getRndDouble();double  p29=getRndDouble();double  p30=getRndDouble();
        int  p31=getRndInt();double  p32=getRndDouble();int  p33=getRndInt();int  p34=getRndInt();
        double  p35=getRndDouble();double  p36=getRndDouble();double  p37=getRndDouble();
        int  p38=getRndInt();double  p39=getRndDouble();double  p40=getRndDouble();
        double  p41=getRndDouble();double  p42=getRndDouble();int  p43=getRndInt();
        double  p44=getRndDouble();double  p45=getRndDouble();int  p46=getRndInt();
        double  p47=getRndDouble();double  p48=getRndDouble();int  p49=getRndInt();
        double  p50=getRndDouble();double  p51=getRndDouble();int  p52=getRndInt();
        double  p53=getRndDouble();int  p54=getRndInt();double  p55=getRndDouble();
        int  p56=getRndInt();int  p57=getRndInt();double  p58=getRndDouble();int  p59=getRndInt();
        int  p60=getRndInt();double  p61=getRndDouble();int  p62=getRndInt();int  p63=getRndInt();
        int  p64=getRndInt();double  p65=getRndDouble();int  p66=getRndInt();int  p67=getRndInt();
        int  p68=getRndInt();int  p69=getRndInt();int  p70=getRndInt();double  p71=getRndDouble();
        int  p72=getRndInt();int  p73=getRndInt();double  p74=getRndDouble();double  p75=getRndDouble();
        double  p76=getRndDouble();double  p77=getRndDouble();double  p78=getRndDouble();
        double  p79=getRndDouble();int  p80=getRndInt();double  p81=getRndDouble();
        double  p82=getRndDouble();double  p83=getRndDouble();double  p84=getRndDouble();
        double  p85=getRndDouble();double  p86=getRndDouble();double  p87=getRndDouble();
        double  p88=getRndDouble();double  p89=getRndDouble();int  p90=getRndInt();
        double  p91=getRndDouble();double  p92=getRndDouble();double  p93=getRndDouble();
        double  p94=getRndDouble();double  p95=getRndDouble();double  p96=getRndDouble();
        double  p97=getRndDouble();int  p98=getRndInt();double  p99=getRndDouble();
        int  p100=getRndInt();double  p101=getRndDouble();double  p102=getRndDouble();
        double  p103=getRndDouble();int  p104=getRndInt();double  p105=getRndDouble();
        int  p106=getRndInt();double  p107=getRndDouble();int  p108=getRndInt();
        int  p109=getRndInt();double  p110=getRndDouble();double  p111=getRndDouble();
        double  p112=getRndDouble();int  p113=getRndInt();double  p114=getRndDouble();
        double  p115=getRndDouble();int  p116=getRndInt();double  p117=getRndDouble();
        int  p118=getRndInt();int  p119=getRndInt();double  p120=getRndDouble();
        double  p121=getRndDouble();double  p122=getRndDouble();double  p123=getRndDouble();
        double  p124=getRndDouble();double  p125=getRndDouble();int  p126=getRndInt();

        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%d\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%d\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%d\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%d\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%d\n",p59);
        ps.format("p60=%d\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%d\n",p69);ps.format("p70=%d\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%d\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%d\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%d\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%d\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);ps.format("p119=%d\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%d\n",p126);
        nativeFnc16(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc17(double p0,double p1,byte p2,double p3,double p4,double p5,double p6,byte p7
        ,double p8,byte p9,double p10,byte p11,byte p12,double p13,double p14,double p15
        ,double p16,byte p17,double p18,double p19,double p20,double p21,double p22
        ,double p23,double p24,double p25,double p26,double p27,double p28,double p29
        ,double p30,double p31,double p32,double p33,double p34,double p35,double p36
        ,double p37,byte p38,double p39,double p40,double p41,double p42,double p43
        ,double p44,double p45,double p46,byte p47,double p48,byte p49,double p50
        ,byte p51,double p52,byte p53,byte p54,byte p55,byte p56,double p57,byte p58
        ,double p59,double p60,byte p61,byte p62,double p63,double p64,byte p65
        ,byte p66,byte p67,double p68,byte p69,double p70,double p71,byte p72,double p73
        ,double p74,double p75,byte p76,double p77,double p78,double p79,double p80
        ,double p81,byte p82,double p83,double p84,byte p85,byte p86,double p87
        ,double p88,double p89,double p90,double p91,double p92,byte p93,byte p94
        ,double p95,double p96,double p97,double p98,byte p99,double p100,double p101
        ,double p102,double p103,double p104,byte p105,double p106,byte p107,double p108
        ,double p109,double p110,byte p111,double p112,double p113,byte p114,double p115
        ,double p116,byte p117,byte p118,byte p119,double p120,byte p121,double p122
        ,byte p123,double p124,byte p125,byte p126    );
    private static void nativeFnc17_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();byte  p2=getRndByte();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();byte  p7=getRndByte();double  p8=getRndDouble();
        byte  p9=getRndByte();double  p10=getRndDouble();byte  p11=getRndByte();
        byte  p12=getRndByte();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();byte  p17=getRndByte();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();byte  p38=getRndByte();
        double  p39=getRndDouble();double  p40=getRndDouble();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();double  p46=getRndDouble();byte  p47=getRndByte();
        double  p48=getRndDouble();byte  p49=getRndByte();double  p50=getRndDouble();
        byte  p51=getRndByte();double  p52=getRndDouble();byte  p53=getRndByte();
        byte  p54=getRndByte();byte  p55=getRndByte();byte  p56=getRndByte();double  p57=getRndDouble();
        byte  p58=getRndByte();double  p59=getRndDouble();double  p60=getRndDouble();
        byte  p61=getRndByte();byte  p62=getRndByte();double  p63=getRndDouble();
        double  p64=getRndDouble();byte  p65=getRndByte();byte  p66=getRndByte();
        byte  p67=getRndByte();double  p68=getRndDouble();byte  p69=getRndByte();
        double  p70=getRndDouble();double  p71=getRndDouble();byte  p72=getRndByte();
        double  p73=getRndDouble();double  p74=getRndDouble();double  p75=getRndDouble();
        byte  p76=getRndByte();double  p77=getRndDouble();double  p78=getRndDouble();
        double  p79=getRndDouble();double  p80=getRndDouble();double  p81=getRndDouble();
        byte  p82=getRndByte();double  p83=getRndDouble();double  p84=getRndDouble();
        byte  p85=getRndByte();byte  p86=getRndByte();double  p87=getRndDouble();
        double  p88=getRndDouble();double  p89=getRndDouble();double  p90=getRndDouble();
        double  p91=getRndDouble();double  p92=getRndDouble();byte  p93=getRndByte();
        byte  p94=getRndByte();double  p95=getRndDouble();double  p96=getRndDouble();
        double  p97=getRndDouble();double  p98=getRndDouble();byte  p99=getRndByte();
        double  p100=getRndDouble();double  p101=getRndDouble();double  p102=getRndDouble();
        double  p103=getRndDouble();double  p104=getRndDouble();byte  p105=getRndByte();
        double  p106=getRndDouble();byte  p107=getRndByte();double  p108=getRndDouble();
        double  p109=getRndDouble();double  p110=getRndDouble();byte  p111=getRndByte();
        double  p112=getRndDouble();double  p113=getRndDouble();byte  p114=getRndByte();
        double  p115=getRndDouble();double  p116=getRndDouble();byte  p117=getRndByte();
        byte  p118=getRndByte();byte  p119=getRndByte();double  p120=getRndDouble();
        byte  p121=getRndByte();double  p122=getRndDouble();byte  p123=getRndByte();
        double  p124=getRndDouble();byte  p125=getRndByte();byte  p126=getRndByte();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%d\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%d\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%d\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%d\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%d\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%d\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%d\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%d\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%d\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%d\n",p117);ps.format("p118=%d\n",p118);ps.format("p119=%d\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%d\n",p126);
        nativeFnc17(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc18(double p0,double p1,double p2,double p3,int p4,int p5,byte p6,int p7,double p8
        ,double p9,double p10,double p11,double p12,byte p13,double p14,double p15
        ,byte p16,double p17,int p18,byte p19,byte p20,double p21,double p22,double p23
        ,int p24,double p25,double p26,int p27,double p28,double p29,double p30
        ,double p31,double p32,double p33,byte p34,int p35,byte p36,double p37,double p38
        ,int p39,int p40,int p41,int p42,double p43,double p44,double p45,byte p46
        ,byte p47,double p48,int p49,byte p50,byte p51,int p52,int p53,int p54,double p55
        ,double p56,int p57,int p58,double p59,byte p60,byte p61,int p62,int p63
        ,double p64,int p65,int p66,double p67,double p68,double p69,int p70,double p71
        ,double p72,int p73,double p74,double p75,int p76,byte p77,double p78,int p79
        ,double p80,int p81,double p82,double p83,byte p84,double p85,double p86
        ,int p87,int p88,byte p89,double p90,double p91,double p92,int p93,byte p94
        ,byte p95,int p96,double p97,double p98,double p99,double p100,byte p101
        ,byte p102,double p103,double p104,double p105,int p106,int p107,double p108
        ,int p109,int p110,double p111,double p112,byte p113,int p114,byte p115
        ,double p116,double p117,int p118,int p119,int p120,double p121,double p122
        ,double p123,double p124,double p125,byte p126    );
    private static void nativeFnc18_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();int  p4=getRndInt();int  p5=getRndInt();byte  p6=getRndByte();
        int  p7=getRndInt();double  p8=getRndDouble();double  p9=getRndDouble();
        double  p10=getRndDouble();double  p11=getRndDouble();double  p12=getRndDouble();
        byte  p13=getRndByte();double  p14=getRndDouble();double  p15=getRndDouble();
        byte  p16=getRndByte();double  p17=getRndDouble();int  p18=getRndInt();
        byte  p19=getRndByte();byte  p20=getRndByte();double  p21=getRndDouble();
        double  p22=getRndDouble();double  p23=getRndDouble();int  p24=getRndInt();
        double  p25=getRndDouble();double  p26=getRndDouble();int  p27=getRndInt();
        double  p28=getRndDouble();double  p29=getRndDouble();double  p30=getRndDouble();
        double  p31=getRndDouble();double  p32=getRndDouble();double  p33=getRndDouble();
        byte  p34=getRndByte();int  p35=getRndInt();byte  p36=getRndByte();double  p37=getRndDouble();
        double  p38=getRndDouble();int  p39=getRndInt();int  p40=getRndInt();int  p41=getRndInt();
        int  p42=getRndInt();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();byte  p46=getRndByte();byte  p47=getRndByte();
        double  p48=getRndDouble();int  p49=getRndInt();byte  p50=getRndByte();
        byte  p51=getRndByte();int  p52=getRndInt();int  p53=getRndInt();int  p54=getRndInt();
        double  p55=getRndDouble();double  p56=getRndDouble();int  p57=getRndInt();
        int  p58=getRndInt();double  p59=getRndDouble();byte  p60=getRndByte();
        byte  p61=getRndByte();int  p62=getRndInt();int  p63=getRndInt();double  p64=getRndDouble();
        int  p65=getRndInt();int  p66=getRndInt();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();int  p70=getRndInt();double  p71=getRndDouble();
        double  p72=getRndDouble();int  p73=getRndInt();double  p74=getRndDouble();
        double  p75=getRndDouble();int  p76=getRndInt();byte  p77=getRndByte();
        double  p78=getRndDouble();int  p79=getRndInt();double  p80=getRndDouble();
        int  p81=getRndInt();double  p82=getRndDouble();double  p83=getRndDouble();
        byte  p84=getRndByte();double  p85=getRndDouble();double  p86=getRndDouble();
        int  p87=getRndInt();int  p88=getRndInt();byte  p89=getRndByte();double  p90=getRndDouble();
        double  p91=getRndDouble();double  p92=getRndDouble();int  p93=getRndInt();
        byte  p94=getRndByte();byte  p95=getRndByte();int  p96=getRndInt();double  p97=getRndDouble();
        double  p98=getRndDouble();double  p99=getRndDouble();double  p100=getRndDouble();
        byte  p101=getRndByte();byte  p102=getRndByte();double  p103=getRndDouble();
        double  p104=getRndDouble();double  p105=getRndDouble();int  p106=getRndInt();
        int  p107=getRndInt();double  p108=getRndDouble();int  p109=getRndInt();
        int  p110=getRndInt();double  p111=getRndDouble();double  p112=getRndDouble();
        byte  p113=getRndByte();int  p114=getRndInt();byte  p115=getRndByte();double  p116=getRndDouble();
        double  p117=getRndDouble();int  p118=getRndInt();int  p119=getRndInt();
        int  p120=getRndInt();double  p121=getRndDouble();double  p122=getRndDouble();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        byte  p126=getRndByte();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);ps.format("p7=%d\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%d\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%d\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%d\n",p18);ps.format("p19=%d\n",p19);
        ps.format("p20=%d\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%d\n",p39);ps.format("p40=%d\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);ps.format("p52=%d\n",p52);
        ps.format("p53=%d\n",p53);ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%d\n",p62);ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%d\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);
        ps.format("p77=%d\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%d\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%d\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);ps.format("p88=%d\n",p88);
        ps.format("p89=%d\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%d\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%d\n",p106);
        ps.format("p107=%d\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);
        ps.format("p110=%d\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%d\n",p114);ps.format("p115=%d\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%d\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%d\n",p126);
        nativeFnc18(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc19(float p0,float p1,float p2,float p3,float p4,int p5,float p6,float p7,double p8
        ,double p9,int p10,double p11,double p12,float p13,int p14,double p15,float p16
        ,int p17,float p18,double p19,float p20,byte p21,double p22,float p23,float p24
        ,double p25,float p26,byte p27,double p28,float p29,float p30,float p31
        ,byte p32,float p33,float p34,float p35,int p36,int p37,double p38,double p39
        ,int p40,byte p41,float p42,float p43,byte p44,byte p45,double p46,double p47
        ,int p48,double p49,double p50,int p51,double p52,float p53,double p54,double p55
        ,float p56,double p57,double p58,double p59,int p60,int p61,float p62,float p63
        ,double p64,double p65,int p66,byte p67,float p68,double p69,float p70,float p71
        ,double p72,float p73,byte p74,int p75,int p76,double p77,float p78,double p79
        ,double p80,double p81,double p82,int p83,float p84,double p85,byte p86
        ,byte p87,double p88,byte p89,byte p90,byte p91,int p92,byte p93,float p94
        ,byte p95,int p96,int p97,int p98,byte p99,double p100,int p101,double p102
        ,float p103,int p104,float p105,int p106,byte p107,float p108,int p109,float p110
        ,float p111,float p112,int p113,int p114,float p115,int p116,double p117
        ,byte p118,float p119,byte p120,byte p121,double p122,float p123,float p124
        ,byte p125,int p126    );
    private static void nativeFnc19_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();int  p5=getRndInt();float  p6=getRndFloat();
        float  p7=getRndFloat();double  p8=getRndDouble();double  p9=getRndDouble();
        int  p10=getRndInt();double  p11=getRndDouble();double  p12=getRndDouble();
        float  p13=getRndFloat();int  p14=getRndInt();double  p15=getRndDouble();
        float  p16=getRndFloat();int  p17=getRndInt();float  p18=getRndFloat();
        double  p19=getRndDouble();float  p20=getRndFloat();byte  p21=getRndByte();
        double  p22=getRndDouble();float  p23=getRndFloat();float  p24=getRndFloat();
        double  p25=getRndDouble();float  p26=getRndFloat();byte  p27=getRndByte();
        double  p28=getRndDouble();float  p29=getRndFloat();float  p30=getRndFloat();
        float  p31=getRndFloat();byte  p32=getRndByte();float  p33=getRndFloat();
        float  p34=getRndFloat();float  p35=getRndFloat();int  p36=getRndInt();
        int  p37=getRndInt();double  p38=getRndDouble();double  p39=getRndDouble();
        int  p40=getRndInt();byte  p41=getRndByte();float  p42=getRndFloat();float  p43=getRndFloat();
        byte  p44=getRndByte();byte  p45=getRndByte();double  p46=getRndDouble();
        double  p47=getRndDouble();int  p48=getRndInt();double  p49=getRndDouble();
        double  p50=getRndDouble();int  p51=getRndInt();double  p52=getRndDouble();
        float  p53=getRndFloat();double  p54=getRndDouble();double  p55=getRndDouble();
        float  p56=getRndFloat();double  p57=getRndDouble();double  p58=getRndDouble();
        double  p59=getRndDouble();int  p60=getRndInt();int  p61=getRndInt();float  p62=getRndFloat();
        float  p63=getRndFloat();double  p64=getRndDouble();double  p65=getRndDouble();
        int  p66=getRndInt();byte  p67=getRndByte();float  p68=getRndFloat();double  p69=getRndDouble();
        float  p70=getRndFloat();float  p71=getRndFloat();double  p72=getRndDouble();
        float  p73=getRndFloat();byte  p74=getRndByte();int  p75=getRndInt();int  p76=getRndInt();
        double  p77=getRndDouble();float  p78=getRndFloat();double  p79=getRndDouble();
        double  p80=getRndDouble();double  p81=getRndDouble();double  p82=getRndDouble();
        int  p83=getRndInt();float  p84=getRndFloat();double  p85=getRndDouble();
        byte  p86=getRndByte();byte  p87=getRndByte();double  p88=getRndDouble();
        byte  p89=getRndByte();byte  p90=getRndByte();byte  p91=getRndByte();int  p92=getRndInt();
        byte  p93=getRndByte();float  p94=getRndFloat();byte  p95=getRndByte();
        int  p96=getRndInt();int  p97=getRndInt();int  p98=getRndInt();byte  p99=getRndByte();
        double  p100=getRndDouble();int  p101=getRndInt();double  p102=getRndDouble();
        float  p103=getRndFloat();int  p104=getRndInt();float  p105=getRndFloat();
        int  p106=getRndInt();byte  p107=getRndByte();float  p108=getRndFloat();
        int  p109=getRndInt();float  p110=getRndFloat();float  p111=getRndFloat();
        float  p112=getRndFloat();int  p113=getRndInt();int  p114=getRndInt();float  p115=getRndFloat();
        int  p116=getRndInt();double  p117=getRndDouble();byte  p118=getRndByte();
        float  p119=getRndFloat();byte  p120=getRndByte();byte  p121=getRndByte();
        double  p122=getRndDouble();float  p123=getRndFloat();float  p124=getRndFloat();
        byte  p125=getRndByte();int  p126=getRndInt();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%d\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);
        ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%d\n",p40);ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%d\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%d\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);
        ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%d\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%d\n",p91);ps.format("p92=%d\n",p92);ps.format("p93=%d\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);ps.format("p96=%d\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%d\n",p98);ps.format("p99=%d\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%d\n",p106);ps.format("p107=%d\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);ps.format("p114=%d\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);ps.format("p126=%d\n",p126);

        nativeFnc19(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc20(double p0,float p1,float p2,float p3,float p4,float p5,float p6,double p7
        ,float p8,float p9,float p10,float p11,double p12,double p13,float p14,double p15
        ,double p16,double p17,float p18,float p19,float p20,float p21,float p22
        ,double p23,double p24,float p25,float p26,float p27,double p28,float p29
        ,double p30,float p31,float p32,double p33,float p34,double p35,double p36
        ,double p37,double p38,float p39,float p40,double p41,float p42,double p43
        ,double p44,float p45,float p46,double p47,double p48,float p49,double p50
        ,float p51,float p52,float p53,float p54,double p55,double p56,double p57
        ,float p58,double p59,float p60,float p61,float p62,float p63,double p64
        ,float p65,float p66,float p67,float p68,float p69,double p70,double p71
        ,float p72,float p73,double p74,float p75,float p76,float p77,double p78
        ,float p79,float p80,float p81,double p82,float p83,double p84,float p85
        ,double p86,float p87,double p88,float p89,float p90,float p91,float p92
        ,double p93,double p94,double p95,double p96,float p97,float p98,float p99
        ,float p100,float p101,float p102,float p103,float p104,float p105,float p106
        ,double p107,float p108,float p109,float p110,double p111,double p112,float p113
        ,float p114,float p115,float p116,double p117,float p118,float p119,float p120
        ,double p121,float p122,double p123,double p124,double p125,float p126    );
    private static void nativeFnc20_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        float  p6=getRndFloat();double  p7=getRndDouble();float  p8=getRndFloat();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        double  p12=getRndDouble();double  p13=getRndDouble();float  p14=getRndFloat();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        float  p18=getRndFloat();float  p19=getRndFloat();float  p20=getRndFloat();
        float  p21=getRndFloat();float  p22=getRndFloat();double  p23=getRndDouble();
        double  p24=getRndDouble();float  p25=getRndFloat();float  p26=getRndFloat();
        float  p27=getRndFloat();double  p28=getRndDouble();float  p29=getRndFloat();
        double  p30=getRndDouble();float  p31=getRndFloat();float  p32=getRndFloat();
        double  p33=getRndDouble();float  p34=getRndFloat();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();double  p38=getRndDouble();
        float  p39=getRndFloat();float  p40=getRndFloat();double  p41=getRndDouble();
        float  p42=getRndFloat();double  p43=getRndDouble();double  p44=getRndDouble();
        float  p45=getRndFloat();float  p46=getRndFloat();double  p47=getRndDouble();
        double  p48=getRndDouble();float  p49=getRndFloat();double  p50=getRndDouble();
        float  p51=getRndFloat();float  p52=getRndFloat();float  p53=getRndFloat();
        float  p54=getRndFloat();double  p55=getRndDouble();double  p56=getRndDouble();
        double  p57=getRndDouble();float  p58=getRndFloat();double  p59=getRndDouble();
        float  p60=getRndFloat();float  p61=getRndFloat();float  p62=getRndFloat();
        float  p63=getRndFloat();double  p64=getRndDouble();float  p65=getRndFloat();
        float  p66=getRndFloat();float  p67=getRndFloat();float  p68=getRndFloat();
        float  p69=getRndFloat();double  p70=getRndDouble();double  p71=getRndDouble();
        float  p72=getRndFloat();float  p73=getRndFloat();double  p74=getRndDouble();
        float  p75=getRndFloat();float  p76=getRndFloat();float  p77=getRndFloat();
        double  p78=getRndDouble();float  p79=getRndFloat();float  p80=getRndFloat();
        float  p81=getRndFloat();double  p82=getRndDouble();float  p83=getRndFloat();
        double  p84=getRndDouble();float  p85=getRndFloat();double  p86=getRndDouble();
        float  p87=getRndFloat();double  p88=getRndDouble();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        double  p93=getRndDouble();double  p94=getRndDouble();double  p95=getRndDouble();
        double  p96=getRndDouble();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();float  p100=getRndFloat();float  p101=getRndFloat();
        float  p102=getRndFloat();float  p103=getRndFloat();float  p104=getRndFloat();
        float  p105=getRndFloat();float  p106=getRndFloat();double  p107=getRndDouble();
        float  p108=getRndFloat();float  p109=getRndFloat();float  p110=getRndFloat();
        double  p111=getRndDouble();double  p112=getRndDouble();float  p113=getRndFloat();
        float  p114=getRndFloat();float  p115=getRndFloat();float  p116=getRndFloat();
        double  p117=getRndDouble();float  p118=getRndFloat();float  p119=getRndFloat();
        float  p120=getRndFloat();double  p121=getRndDouble();float  p122=getRndFloat();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc20(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc21(float p0,int p1,float p2,float p3,int p4,int p5,double p6,float p7,float p8
        ,double p9,int p10,double p11,double p12,float p13,double p14,double p15
        ,float p16,double p17,float p18,double p19,double p20,double p21,float p22
        ,double p23,double p24,int p25,double p26,int p27,int p28,double p29,float p30
        ,float p31,float p32,int p33,double p34,float p35,double p36,double p37
        ,float p38,float p39,float p40,float p41,float p42,float p43,int p44,int p45
        ,double p46,float p47,float p48,int p49,double p50,double p51,float p52
        ,double p53,int p54,double p55,int p56,double p57,double p58,float p59,float p60
        ,int p61,float p62,int p63,float p64,double p65,int p66,float p67,double p68
        ,double p69,double p70,double p71,double p72,float p73,int p74,int p75,double p76
        ,int p77,float p78,float p79,double p80,double p81,double p82,int p83,double p84
        ,double p85,float p86,double p87,int p88,double p89,float p90,float p91
        ,float p92,double p93,int p94,float p95,int p96,float p97,double p98,int p99
        ,int p100,int p101,int p102,float p103,float p104,float p105,float p106
        ,int p107,float p108,double p109,float p110,double p111,double p112,float p113
        ,float p114,double p115,double p116,float p117,int p118,double p119,float p120
        ,float p121,double p122,double p123,float p124,int p125,double p126    );
    private static void nativeFnc21_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();int  p1=getRndInt();float  p2=getRndFloat();
        float  p3=getRndFloat();int  p4=getRndInt();int  p5=getRndInt();double  p6=getRndDouble();
        float  p7=getRndFloat();float  p8=getRndFloat();double  p9=getRndDouble();
        int  p10=getRndInt();double  p11=getRndDouble();double  p12=getRndDouble();
        float  p13=getRndFloat();double  p14=getRndDouble();double  p15=getRndDouble();
        float  p16=getRndFloat();double  p17=getRndDouble();float  p18=getRndFloat();
        double  p19=getRndDouble();double  p20=getRndDouble();double  p21=getRndDouble();
        float  p22=getRndFloat();double  p23=getRndDouble();double  p24=getRndDouble();
        int  p25=getRndInt();double  p26=getRndDouble();int  p27=getRndInt();int  p28=getRndInt();
        double  p29=getRndDouble();float  p30=getRndFloat();float  p31=getRndFloat();
        float  p32=getRndFloat();int  p33=getRndInt();double  p34=getRndDouble();
        float  p35=getRndFloat();double  p36=getRndDouble();double  p37=getRndDouble();
        float  p38=getRndFloat();float  p39=getRndFloat();float  p40=getRndFloat();
        float  p41=getRndFloat();float  p42=getRndFloat();float  p43=getRndFloat();
        int  p44=getRndInt();int  p45=getRndInt();double  p46=getRndDouble();float  p47=getRndFloat();
        float  p48=getRndFloat();int  p49=getRndInt();double  p50=getRndDouble();
        double  p51=getRndDouble();float  p52=getRndFloat();double  p53=getRndDouble();
        int  p54=getRndInt();double  p55=getRndDouble();int  p56=getRndInt();double  p57=getRndDouble();
        double  p58=getRndDouble();float  p59=getRndFloat();float  p60=getRndFloat();
        int  p61=getRndInt();float  p62=getRndFloat();int  p63=getRndInt();float  p64=getRndFloat();
        double  p65=getRndDouble();int  p66=getRndInt();float  p67=getRndFloat();
        double  p68=getRndDouble();double  p69=getRndDouble();double  p70=getRndDouble();
        double  p71=getRndDouble();double  p72=getRndDouble();float  p73=getRndFloat();
        int  p74=getRndInt();int  p75=getRndInt();double  p76=getRndDouble();int  p77=getRndInt();
        float  p78=getRndFloat();float  p79=getRndFloat();double  p80=getRndDouble();
        double  p81=getRndDouble();double  p82=getRndDouble();int  p83=getRndInt();
        double  p84=getRndDouble();double  p85=getRndDouble();float  p86=getRndFloat();
        double  p87=getRndDouble();int  p88=getRndInt();double  p89=getRndDouble();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        double  p93=getRndDouble();int  p94=getRndInt();float  p95=getRndFloat();
        int  p96=getRndInt();float  p97=getRndFloat();double  p98=getRndDouble();
        int  p99=getRndInt();int  p100=getRndInt();int  p101=getRndInt();int  p102=getRndInt();
        float  p103=getRndFloat();float  p104=getRndFloat();float  p105=getRndFloat();
        float  p106=getRndFloat();int  p107=getRndInt();float  p108=getRndFloat();
        double  p109=getRndDouble();float  p110=getRndFloat();double  p111=getRndDouble();
        double  p112=getRndDouble();float  p113=getRndFloat();float  p114=getRndFloat();
        double  p115=getRndDouble();double  p116=getRndDouble();float  p117=getRndFloat();
        int  p118=getRndInt();double  p119=getRndDouble();float  p120=getRndFloat();
        float  p121=getRndFloat();double  p122=getRndDouble();double  p123=getRndDouble();
        float  p124=getRndFloat();int  p125=getRndInt();double  p126=getRndDouble();

        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);ps.format("p5=%d\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);
        ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%d\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc21(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc22(double p0,byte p1,double p2,float p3,float p4,float p5,float p6,float p7
        ,float p8,float p9,float p10,float p11,double p12,double p13,byte p14,float p15
        ,float p16,double p17,float p18,float p19,double p20,float p21,byte p22
        ,float p23,float p24,double p25,byte p26,double p27,double p28,double p29
        ,byte p30,byte p31,byte p32,byte p33,byte p34,double p35,double p36,float p37
        ,float p38,double p39,float p40,float p41,float p42,float p43,byte p44,float p45
        ,float p46,double p47,byte p48,byte p49,double p50,double p51,float p52
        ,float p53,double p54,float p55,double p56,float p57,float p58,float p59
        ,float p60,float p61,float p62,double p63,double p64,float p65,double p66
        ,double p67,byte p68,double p69,double p70,double p71,double p72,float p73
        ,byte p74,float p75,float p76,float p77,float p78,float p79,double p80,double p81
        ,float p82,double p83,byte p84,float p85,double p86,float p87,double p88
        ,byte p89,byte p90,double p91,byte p92,float p93,double p94,byte p95,float p96
        ,float p97,double p98,float p99,byte p100,float p101,double p102,double p103
        ,byte p104,double p105,float p106,byte p107,float p108,byte p109,float p110
        ,float p111,double p112,float p113,double p114,double p115,float p116,double p117
        ,float p118,float p119,float p120,byte p121,double p122,double p123,double p124
        ,float p125,float p126    );
    private static void nativeFnc22_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();byte  p1=getRndByte();double  p2=getRndDouble();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        float  p6=getRndFloat();float  p7=getRndFloat();float  p8=getRndFloat();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        double  p12=getRndDouble();double  p13=getRndDouble();byte  p14=getRndByte();
        float  p15=getRndFloat();float  p16=getRndFloat();double  p17=getRndDouble();
        float  p18=getRndFloat();float  p19=getRndFloat();double  p20=getRndDouble();
        float  p21=getRndFloat();byte  p22=getRndByte();float  p23=getRndFloat();
        float  p24=getRndFloat();double  p25=getRndDouble();byte  p26=getRndByte();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        byte  p30=getRndByte();byte  p31=getRndByte();byte  p32=getRndByte();byte  p33=getRndByte();
        byte  p34=getRndByte();double  p35=getRndDouble();double  p36=getRndDouble();
        float  p37=getRndFloat();float  p38=getRndFloat();double  p39=getRndDouble();
        float  p40=getRndFloat();float  p41=getRndFloat();float  p42=getRndFloat();
        float  p43=getRndFloat();byte  p44=getRndByte();float  p45=getRndFloat();
        float  p46=getRndFloat();double  p47=getRndDouble();byte  p48=getRndByte();
        byte  p49=getRndByte();double  p50=getRndDouble();double  p51=getRndDouble();
        float  p52=getRndFloat();float  p53=getRndFloat();double  p54=getRndDouble();
        float  p55=getRndFloat();double  p56=getRndDouble();float  p57=getRndFloat();
        float  p58=getRndFloat();float  p59=getRndFloat();float  p60=getRndFloat();
        float  p61=getRndFloat();float  p62=getRndFloat();double  p63=getRndDouble();
        double  p64=getRndDouble();float  p65=getRndFloat();double  p66=getRndDouble();
        double  p67=getRndDouble();byte  p68=getRndByte();double  p69=getRndDouble();
        double  p70=getRndDouble();double  p71=getRndDouble();double  p72=getRndDouble();
        float  p73=getRndFloat();byte  p74=getRndByte();float  p75=getRndFloat();
        float  p76=getRndFloat();float  p77=getRndFloat();float  p78=getRndFloat();
        float  p79=getRndFloat();double  p80=getRndDouble();double  p81=getRndDouble();
        float  p82=getRndFloat();double  p83=getRndDouble();byte  p84=getRndByte();
        float  p85=getRndFloat();double  p86=getRndDouble();float  p87=getRndFloat();
        double  p88=getRndDouble();byte  p89=getRndByte();byte  p90=getRndByte();
        double  p91=getRndDouble();byte  p92=getRndByte();float  p93=getRndFloat();
        double  p94=getRndDouble();byte  p95=getRndByte();float  p96=getRndFloat();
        float  p97=getRndFloat();double  p98=getRndDouble();float  p99=getRndFloat();
        byte  p100=getRndByte();float  p101=getRndFloat();double  p102=getRndDouble();
        double  p103=getRndDouble();byte  p104=getRndByte();double  p105=getRndDouble();
        float  p106=getRndFloat();byte  p107=getRndByte();float  p108=getRndFloat();
        byte  p109=getRndByte();float  p110=getRndFloat();float  p111=getRndFloat();
        double  p112=getRndDouble();float  p113=getRndFloat();double  p114=getRndDouble();
        double  p115=getRndDouble();float  p116=getRndFloat();double  p117=getRndDouble();
        float  p118=getRndFloat();float  p119=getRndFloat();float  p120=getRndFloat();
        byte  p121=getRndByte();double  p122=getRndDouble();double  p123=getRndDouble();
        double  p124=getRndDouble();float  p125=getRndFloat();float  p126=getRndFloat();

        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%d\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%d\n",p30);ps.format("p31=%d\n",p31);ps.format("p32=%d\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%d\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);
        ps.format("p90=%d\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%d\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc22(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc23(float p0,float p1,float p2,float p3,float p4,float p5,int p6,float p7,float p8
        ,float p9,float p10,int p11,float p12,float p13,float p14,int p15,float p16
        ,float p17,float p18,int p19,float p20,float p21,float p22,float p23,float p24
        ,float p25,float p26,float p27,float p28,float p29,int p30,float p31,float p32
        ,float p33,int p34,int p35,int p36,float p37,float p38,float p39,float p40
        ,float p41,float p42,int p43,float p44,float p45,float p46,float p47,int p48
        ,float p49,float p50,float p51,float p52,float p53,float p54,int p55,float p56
        ,float p57,float p58,float p59,float p60,int p61,float p62,float p63,int p64
        ,float p65,int p66,int p67,int p68,float p69,float p70,float p71,float p72
        ,int p73,float p74,float p75,float p76,float p77,int p78,float p79,float p80
        ,float p81,float p82,float p83,float p84,float p85,int p86,float p87,float p88
        ,float p89,float p90,int p91,int p92,float p93,int p94,float p95,int p96
        ,float p97,float p98,float p99,float p100,float p101,float p102,float p103
        ,float p104,float p105,float p106,int p107,float p108,float p109,float p110
        ,float p111,float p112,int p113,float p114,float p115,float p116,float p117
        ,float p118,int p119,float p120,float p121,int p122,int p123,float p124
        ,float p125,float p126    );
    private static void nativeFnc23_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        int  p6=getRndInt();float  p7=getRndFloat();float  p8=getRndFloat();float  p9=getRndFloat();
        float  p10=getRndFloat();int  p11=getRndInt();float  p12=getRndFloat();
        float  p13=getRndFloat();float  p14=getRndFloat();int  p15=getRndInt();
        float  p16=getRndFloat();float  p17=getRndFloat();float  p18=getRndFloat();
        int  p19=getRndInt();float  p20=getRndFloat();float  p21=getRndFloat();
        float  p22=getRndFloat();float  p23=getRndFloat();float  p24=getRndFloat();
        float  p25=getRndFloat();float  p26=getRndFloat();float  p27=getRndFloat();
        float  p28=getRndFloat();float  p29=getRndFloat();int  p30=getRndInt();
        float  p31=getRndFloat();float  p32=getRndFloat();float  p33=getRndFloat();
        int  p34=getRndInt();int  p35=getRndInt();int  p36=getRndInt();float  p37=getRndFloat();
        float  p38=getRndFloat();float  p39=getRndFloat();float  p40=getRndFloat();
        float  p41=getRndFloat();float  p42=getRndFloat();int  p43=getRndInt();
        float  p44=getRndFloat();float  p45=getRndFloat();float  p46=getRndFloat();
        float  p47=getRndFloat();int  p48=getRndInt();float  p49=getRndFloat();
        float  p50=getRndFloat();float  p51=getRndFloat();float  p52=getRndFloat();
        float  p53=getRndFloat();float  p54=getRndFloat();int  p55=getRndInt();
        float  p56=getRndFloat();float  p57=getRndFloat();float  p58=getRndFloat();
        float  p59=getRndFloat();float  p60=getRndFloat();int  p61=getRndInt();
        float  p62=getRndFloat();float  p63=getRndFloat();int  p64=getRndInt();
        float  p65=getRndFloat();int  p66=getRndInt();int  p67=getRndInt();int  p68=getRndInt();
        float  p69=getRndFloat();float  p70=getRndFloat();float  p71=getRndFloat();
        float  p72=getRndFloat();int  p73=getRndInt();float  p74=getRndFloat();
        float  p75=getRndFloat();float  p76=getRndFloat();float  p77=getRndFloat();
        int  p78=getRndInt();float  p79=getRndFloat();float  p80=getRndFloat();
        float  p81=getRndFloat();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();float  p85=getRndFloat();int  p86=getRndInt();
        float  p87=getRndFloat();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();int  p91=getRndInt();int  p92=getRndInt();float  p93=getRndFloat();
        int  p94=getRndInt();float  p95=getRndFloat();int  p96=getRndInt();float  p97=getRndFloat();
        float  p98=getRndFloat();float  p99=getRndFloat();float  p100=getRndFloat();
        float  p101=getRndFloat();float  p102=getRndFloat();float  p103=getRndFloat();
        float  p104=getRndFloat();float  p105=getRndFloat();float  p106=getRndFloat();
        int  p107=getRndInt();float  p108=getRndFloat();float  p109=getRndFloat();
        float  p110=getRndFloat();float  p111=getRndFloat();float  p112=getRndFloat();
        int  p113=getRndInt();float  p114=getRndFloat();float  p115=getRndFloat();
        float  p116=getRndFloat();float  p117=getRndFloat();float  p118=getRndFloat();
        int  p119=getRndInt();float  p120=getRndFloat();float  p121=getRndFloat();
        int  p122=getRndInt();int  p123=getRndInt();float  p124=getRndFloat();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%d\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%d\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%d\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%d\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);
        ps.format("p92=%d\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%d\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%d\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc23(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc24(float p0,float p1,byte p2,byte p3,float p4,float p5,byte p6,byte p7,byte p8
        ,byte p9,float p10,float p11,float p12,float p13,float p14,float p15,float p16
        ,float p17,float p18,byte p19,float p20,float p21,float p22,float p23,float p24
        ,float p25,float p26,byte p27,float p28,float p29,float p30,float p31,float p32
        ,float p33,float p34,float p35,float p36,byte p37,float p38,float p39,float p40
        ,float p41,byte p42,float p43,float p44,float p45,byte p46,byte p47,float p48
        ,float p49,float p50,float p51,float p52,float p53,byte p54,float p55,byte p56
        ,byte p57,float p58,float p59,float p60,float p61,float p62,byte p63,byte p64
        ,float p65,float p66,float p67,byte p68,float p69,float p70,float p71,float p72
        ,float p73,byte p74,float p75,float p76,float p77,float p78,float p79,byte p80
        ,float p81,float p82,byte p83,float p84,byte p85,float p86,byte p87,float p88
        ,byte p89,byte p90,float p91,float p92,float p93,float p94,byte p95,byte p96
        ,float p97,byte p98,float p99,float p100,float p101,float p102,float p103
        ,float p104,float p105,byte p106,float p107,float p108,float p109,float p110
        ,byte p111,float p112,float p113,float p114,float p115,float p116,float p117
        ,float p118,float p119,float p120,float p121,float p122,float p123,float p124
        ,float p125,float p126    );
    private static void nativeFnc24_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();byte  p2=getRndByte();
        byte  p3=getRndByte();float  p4=getRndFloat();float  p5=getRndFloat();byte  p6=getRndByte();
        byte  p7=getRndByte();byte  p8=getRndByte();byte  p9=getRndByte();float  p10=getRndFloat();
        float  p11=getRndFloat();float  p12=getRndFloat();float  p13=getRndFloat();
        float  p14=getRndFloat();float  p15=getRndFloat();float  p16=getRndFloat();
        float  p17=getRndFloat();float  p18=getRndFloat();byte  p19=getRndByte();
        float  p20=getRndFloat();float  p21=getRndFloat();float  p22=getRndFloat();
        float  p23=getRndFloat();float  p24=getRndFloat();float  p25=getRndFloat();
        float  p26=getRndFloat();byte  p27=getRndByte();float  p28=getRndFloat();
        float  p29=getRndFloat();float  p30=getRndFloat();float  p31=getRndFloat();
        float  p32=getRndFloat();float  p33=getRndFloat();float  p34=getRndFloat();
        float  p35=getRndFloat();float  p36=getRndFloat();byte  p37=getRndByte();
        float  p38=getRndFloat();float  p39=getRndFloat();float  p40=getRndFloat();
        float  p41=getRndFloat();byte  p42=getRndByte();float  p43=getRndFloat();
        float  p44=getRndFloat();float  p45=getRndFloat();byte  p46=getRndByte();
        byte  p47=getRndByte();float  p48=getRndFloat();float  p49=getRndFloat();
        float  p50=getRndFloat();float  p51=getRndFloat();float  p52=getRndFloat();
        float  p53=getRndFloat();byte  p54=getRndByte();float  p55=getRndFloat();
        byte  p56=getRndByte();byte  p57=getRndByte();float  p58=getRndFloat();
        float  p59=getRndFloat();float  p60=getRndFloat();float  p61=getRndFloat();
        float  p62=getRndFloat();byte  p63=getRndByte();byte  p64=getRndByte();
        float  p65=getRndFloat();float  p66=getRndFloat();float  p67=getRndFloat();
        byte  p68=getRndByte();float  p69=getRndFloat();float  p70=getRndFloat();
        float  p71=getRndFloat();float  p72=getRndFloat();float  p73=getRndFloat();
        byte  p74=getRndByte();float  p75=getRndFloat();float  p76=getRndFloat();
        float  p77=getRndFloat();float  p78=getRndFloat();float  p79=getRndFloat();
        byte  p80=getRndByte();float  p81=getRndFloat();float  p82=getRndFloat();
        byte  p83=getRndByte();float  p84=getRndFloat();byte  p85=getRndByte();
        float  p86=getRndFloat();byte  p87=getRndByte();float  p88=getRndFloat();
        byte  p89=getRndByte();byte  p90=getRndByte();float  p91=getRndFloat();
        float  p92=getRndFloat();float  p93=getRndFloat();float  p94=getRndFloat();
        byte  p95=getRndByte();byte  p96=getRndByte();float  p97=getRndFloat();
        byte  p98=getRndByte();float  p99=getRndFloat();float  p100=getRndFloat();
        float  p101=getRndFloat();float  p102=getRndFloat();float  p103=getRndFloat();
        float  p104=getRndFloat();float  p105=getRndFloat();byte  p106=getRndByte();
        float  p107=getRndFloat();float  p108=getRndFloat();float  p109=getRndFloat();
        float  p110=getRndFloat();byte  p111=getRndByte();float  p112=getRndFloat();
        float  p113=getRndFloat();float  p114=getRndFloat();float  p115=getRndFloat();
        float  p116=getRndFloat();float  p117=getRndFloat();float  p118=getRndFloat();
        float  p119=getRndFloat();float  p120=getRndFloat();float  p121=getRndFloat();
        float  p122=getRndFloat();float  p123=getRndFloat();float  p124=getRndFloat();
        float  p125=getRndFloat();float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%d\n",p2);ps.format("p3=%d\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%d\n",p7);ps.format("p8=%d\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%d\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%d\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%d\n",p63);
        ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);ps.format("p96=%d\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%d\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc24(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc25(float p0,int p1,float p2,float p3,float p4,int p5,int p6,byte p7,float p8
        ,int p9,float p10,float p11,float p12,float p13,int p14,byte p15,float p16
        ,float p17,float p18,byte p19,float p20,byte p21,float p22,byte p23,float p24
        ,float p25,float p26,float p27,float p28,int p29,byte p30,float p31,byte p32
        ,float p33,int p34,int p35,byte p36,byte p37,int p38,int p39,float p40,int p41
        ,float p42,float p43,float p44,float p45,float p46,int p47,float p48,int p49
        ,int p50,int p51,float p52,float p53,byte p54,int p55,byte p56,int p57,float p58
        ,int p59,int p60,byte p61,byte p62,int p63,float p64,float p65,byte p66
        ,byte p67,byte p68,int p69,byte p70,float p71,byte p72,float p73,float p74
        ,float p75,float p76,byte p77,float p78,int p79,byte p80,float p81,float p82
        ,int p83,float p84,byte p85,byte p86,float p87,float p88,byte p89,float p90
        ,float p91,int p92,float p93,float p94,int p95,float p96,int p97,int p98
        ,int p99,int p100,int p101,int p102,int p103,float p104,int p105,byte p106
        ,float p107,float p108,int p109,byte p110,float p111,float p112,float p113
        ,byte p114,float p115,byte p116,float p117,byte p118,float p119,float p120
        ,float p121,byte p122,float p123,byte p124,float p125,int p126    );
    private static void nativeFnc25_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();int  p1=getRndInt();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();int  p5=getRndInt();int  p6=getRndInt();
        byte  p7=getRndByte();float  p8=getRndFloat();int  p9=getRndInt();float  p10=getRndFloat();
        float  p11=getRndFloat();float  p12=getRndFloat();float  p13=getRndFloat();
        int  p14=getRndInt();byte  p15=getRndByte();float  p16=getRndFloat();float  p17=getRndFloat();
        float  p18=getRndFloat();byte  p19=getRndByte();float  p20=getRndFloat();
        byte  p21=getRndByte();float  p22=getRndFloat();byte  p23=getRndByte();
        float  p24=getRndFloat();float  p25=getRndFloat();float  p26=getRndFloat();
        float  p27=getRndFloat();float  p28=getRndFloat();int  p29=getRndInt();
        byte  p30=getRndByte();float  p31=getRndFloat();byte  p32=getRndByte();
        float  p33=getRndFloat();int  p34=getRndInt();int  p35=getRndInt();byte  p36=getRndByte();
        byte  p37=getRndByte();int  p38=getRndInt();int  p39=getRndInt();float  p40=getRndFloat();
        int  p41=getRndInt();float  p42=getRndFloat();float  p43=getRndFloat();
        float  p44=getRndFloat();float  p45=getRndFloat();float  p46=getRndFloat();
        int  p47=getRndInt();float  p48=getRndFloat();int  p49=getRndInt();int  p50=getRndInt();
        int  p51=getRndInt();float  p52=getRndFloat();float  p53=getRndFloat();
        byte  p54=getRndByte();int  p55=getRndInt();byte  p56=getRndByte();int  p57=getRndInt();
        float  p58=getRndFloat();int  p59=getRndInt();int  p60=getRndInt();byte  p61=getRndByte();
        byte  p62=getRndByte();int  p63=getRndInt();float  p64=getRndFloat();float  p65=getRndFloat();
        byte  p66=getRndByte();byte  p67=getRndByte();byte  p68=getRndByte();int  p69=getRndInt();
        byte  p70=getRndByte();float  p71=getRndFloat();byte  p72=getRndByte();
        float  p73=getRndFloat();float  p74=getRndFloat();float  p75=getRndFloat();
        float  p76=getRndFloat();byte  p77=getRndByte();float  p78=getRndFloat();
        int  p79=getRndInt();byte  p80=getRndByte();float  p81=getRndFloat();float  p82=getRndFloat();
        int  p83=getRndInt();float  p84=getRndFloat();byte  p85=getRndByte();byte  p86=getRndByte();
        float  p87=getRndFloat();float  p88=getRndFloat();byte  p89=getRndByte();
        float  p90=getRndFloat();float  p91=getRndFloat();int  p92=getRndInt();
        float  p93=getRndFloat();float  p94=getRndFloat();int  p95=getRndInt();
        float  p96=getRndFloat();int  p97=getRndInt();int  p98=getRndInt();int  p99=getRndInt();
        int  p100=getRndInt();int  p101=getRndInt();int  p102=getRndInt();int  p103=getRndInt();
        float  p104=getRndFloat();int  p105=getRndInt();byte  p106=getRndByte();
        float  p107=getRndFloat();float  p108=getRndFloat();int  p109=getRndInt();
        byte  p110=getRndByte();float  p111=getRndFloat();float  p112=getRndFloat();
        float  p113=getRndFloat();byte  p114=getRndByte();float  p115=getRndFloat();
        byte  p116=getRndByte();float  p117=getRndFloat();byte  p118=getRndByte();
        float  p119=getRndFloat();float  p120=getRndFloat();float  p121=getRndFloat();
        byte  p122=getRndByte();float  p123=getRndFloat();byte  p124=getRndByte();
        float  p125=getRndFloat();int  p126=getRndInt();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%d\n",p29);ps.format("p30=%d\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%d\n",p34);ps.format("p35=%d\n",p35);ps.format("p36=%d\n",p36);
        ps.format("p37=%d\n",p37);ps.format("p38=%d\n",p38);ps.format("p39=%d\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%d\n",p49);ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);
        ps.format("p55=%d\n",p55);ps.format("p56=%d\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%d\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%d\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%d\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);
        ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%d\n",p69);
        ps.format("p70=%d\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%d\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%d\n",p79);ps.format("p80=%d\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%d\n",p85);ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%e\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%d\n",p98);ps.format("p99=%d\n",p99);
        ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);ps.format("p102=%d\n",p102);
        ps.format("p103=%d\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);
        ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%d\n",p109);ps.format("p110=%d\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%d\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%d\n",p126);

        nativeFnc25(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc26(double p0,double p1,double p2,double p3,double p4,int p5,double p6,double p7
        ,double p8,double p9,double p10,double p11,double p12,double p13,double p14
        ,int p15,double p16,double p17,int p18,double p19,double p20,int p21,double p22
        ,double p23,double p24,double p25,double p26,double p27,double p28,double p29
        ,double p30,int p31,int p32,double p33,double p34,double p35,double p36
        ,double p37,double p38,double p39,double p40,double p41,double p42,int p43
        ,double p44,double p45,int p46,double p47,int p48,double p49,double p50
        ,double p51,double p52,int p53,double p54,double p55,double p56,int p57
        ,int p58,double p59,int p60,double p61,double p62,double p63,double p64
        ,double p65,double p66,double p67,double p68,double p69,int p70,int p71
        ,double p72,double p73,int p74,int p75,int p76,double p77,double p78,double p79
        ,double p80,double p81,double p82,double p83,double p84,double p85,int p86
        ,double p87,double p88,int p89,double p90,double p91,double p92,double p93
        ,int p94,double p95,double p96,int p97,double p98,double p99,double p100
        ,double p101,double p102,double p103,double p104,double p105,double p106
        ,double p107,double p108,double p109,int p110,int p111,double p112,int p113
        ,double p114,double p115,double p116,double p117,double p118,double p119
        ,int p120,int p121,int p122,double p123,double p124,double p125,int p126
            );
    private static void nativeFnc26_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();int  p5=getRndInt();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        int  p15=getRndInt();double  p16=getRndDouble();double  p17=getRndDouble();
        int  p18=getRndInt();double  p19=getRndDouble();double  p20=getRndDouble();
        int  p21=getRndInt();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();int  p31=getRndInt();int  p32=getRndInt();double  p33=getRndDouble();
        double  p34=getRndDouble();double  p35=getRndDouble();double  p36=getRndDouble();
        double  p37=getRndDouble();double  p38=getRndDouble();double  p39=getRndDouble();
        double  p40=getRndDouble();double  p41=getRndDouble();double  p42=getRndDouble();
        int  p43=getRndInt();double  p44=getRndDouble();double  p45=getRndDouble();
        int  p46=getRndInt();double  p47=getRndDouble();int  p48=getRndInt();double  p49=getRndDouble();
        double  p50=getRndDouble();double  p51=getRndDouble();double  p52=getRndDouble();
        int  p53=getRndInt();double  p54=getRndDouble();double  p55=getRndDouble();
        double  p56=getRndDouble();int  p57=getRndInt();int  p58=getRndInt();double  p59=getRndDouble();
        int  p60=getRndInt();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();double  p64=getRndDouble();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();int  p70=getRndInt();int  p71=getRndInt();double  p72=getRndDouble();
        double  p73=getRndDouble();int  p74=getRndInt();int  p75=getRndInt();int  p76=getRndInt();
        double  p77=getRndDouble();double  p78=getRndDouble();double  p79=getRndDouble();
        double  p80=getRndDouble();double  p81=getRndDouble();double  p82=getRndDouble();
        double  p83=getRndDouble();double  p84=getRndDouble();double  p85=getRndDouble();
        int  p86=getRndInt();double  p87=getRndDouble();double  p88=getRndDouble();
        int  p89=getRndInt();double  p90=getRndDouble();double  p91=getRndDouble();
        double  p92=getRndDouble();double  p93=getRndDouble();int  p94=getRndInt();
        double  p95=getRndDouble();double  p96=getRndDouble();int  p97=getRndInt();
        double  p98=getRndDouble();double  p99=getRndDouble();double  p100=getRndDouble();
        double  p101=getRndDouble();double  p102=getRndDouble();double  p103=getRndDouble();
        double  p104=getRndDouble();double  p105=getRndDouble();double  p106=getRndDouble();
        double  p107=getRndDouble();double  p108=getRndDouble();double  p109=getRndDouble();
        int  p110=getRndInt();int  p111=getRndInt();double  p112=getRndDouble();
        int  p113=getRndInt();double  p114=getRndDouble();double  p115=getRndDouble();
        double  p116=getRndDouble();double  p117=getRndDouble();double  p118=getRndDouble();
        double  p119=getRndDouble();int  p120=getRndInt();int  p121=getRndInt();
        int  p122=getRndInt();double  p123=getRndDouble();double  p124=getRndDouble();
        double  p125=getRndDouble();int  p126=getRndInt();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%d\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%d\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%d\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%d\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%d\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%d\n",p70);ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%d\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%e\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%d\n",p110);ps.format("p111=%d\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);
        ps.format("p121=%d\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%d\n",p126);

        nativeFnc26(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc27(double p0,double p1,double p2,double p3,double p4,double p5,double p6,double p7
        ,double p8,double p9,double p10,double p11,byte p12,byte p13,byte p14,double p15
        ,double p16,byte p17,double p18,double p19,double p20,double p21,double p22
        ,double p23,double p24,double p25,double p26,double p27,double p28,double p29
        ,byte p30,double p31,double p32,byte p33,double p34,double p35,double p36
        ,double p37,double p38,double p39,double p40,byte p41,byte p42,double p43
        ,byte p44,byte p45,double p46,double p47,double p48,byte p49,byte p50,byte p51
        ,double p52,byte p53,double p54,double p55,double p56,double p57,byte p58
        ,byte p59,double p60,double p61,double p62,double p63,double p64,double p65
        ,double p66,double p67,byte p68,double p69,double p70,byte p71,double p72
        ,double p73,double p74,double p75,double p76,double p77,double p78,double p79
        ,double p80,double p81,double p82,double p83,double p84,double p85,double p86
        ,double p87,byte p88,double p89,byte p90,byte p91,byte p92,double p93,byte p94
        ,byte p95,double p96,byte p97,double p98,double p99,double p100,double p101
        ,double p102,double p103,double p104,double p105,double p106,double p107
        ,double p108,double p109,double p110,double p111,double p112,double p113
        ,double p114,byte p115,double p116,double p117,byte p118,double p119,double p120
        ,byte p121,double p122,double p123,double p124,byte p125,double p126    );
    private static void nativeFnc27_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        byte  p12=getRndByte();byte  p13=getRndByte();byte  p14=getRndByte();double  p15=getRndDouble();
        double  p16=getRndDouble();byte  p17=getRndByte();double  p18=getRndDouble();
        double  p19=getRndDouble();double  p20=getRndDouble();double  p21=getRndDouble();
        double  p22=getRndDouble();double  p23=getRndDouble();double  p24=getRndDouble();
        double  p25=getRndDouble();double  p26=getRndDouble();double  p27=getRndDouble();
        double  p28=getRndDouble();double  p29=getRndDouble();byte  p30=getRndByte();
        double  p31=getRndDouble();double  p32=getRndDouble();byte  p33=getRndByte();
        double  p34=getRndDouble();double  p35=getRndDouble();double  p36=getRndDouble();
        double  p37=getRndDouble();double  p38=getRndDouble();double  p39=getRndDouble();
        double  p40=getRndDouble();byte  p41=getRndByte();byte  p42=getRndByte();
        double  p43=getRndDouble();byte  p44=getRndByte();byte  p45=getRndByte();
        double  p46=getRndDouble();double  p47=getRndDouble();double  p48=getRndDouble();
        byte  p49=getRndByte();byte  p50=getRndByte();byte  p51=getRndByte();double  p52=getRndDouble();
        byte  p53=getRndByte();double  p54=getRndDouble();double  p55=getRndDouble();
        double  p56=getRndDouble();double  p57=getRndDouble();byte  p58=getRndByte();
        byte  p59=getRndByte();double  p60=getRndDouble();double  p61=getRndDouble();
        double  p62=getRndDouble();double  p63=getRndDouble();double  p64=getRndDouble();
        double  p65=getRndDouble();double  p66=getRndDouble();double  p67=getRndDouble();
        byte  p68=getRndByte();double  p69=getRndDouble();double  p70=getRndDouble();
        byte  p71=getRndByte();double  p72=getRndDouble();double  p73=getRndDouble();
        double  p74=getRndDouble();double  p75=getRndDouble();double  p76=getRndDouble();
        double  p77=getRndDouble();double  p78=getRndDouble();double  p79=getRndDouble();
        double  p80=getRndDouble();double  p81=getRndDouble();double  p82=getRndDouble();
        double  p83=getRndDouble();double  p84=getRndDouble();double  p85=getRndDouble();
        double  p86=getRndDouble();double  p87=getRndDouble();byte  p88=getRndByte();
        double  p89=getRndDouble();byte  p90=getRndByte();byte  p91=getRndByte();
        byte  p92=getRndByte();double  p93=getRndDouble();byte  p94=getRndByte();
        byte  p95=getRndByte();double  p96=getRndDouble();byte  p97=getRndByte();
        double  p98=getRndDouble();double  p99=getRndDouble();double  p100=getRndDouble();
        double  p101=getRndDouble();double  p102=getRndDouble();double  p103=getRndDouble();
        double  p104=getRndDouble();double  p105=getRndDouble();double  p106=getRndDouble();
        double  p107=getRndDouble();double  p108=getRndDouble();double  p109=getRndDouble();
        double  p110=getRndDouble();double  p111=getRndDouble();double  p112=getRndDouble();
        double  p113=getRndDouble();double  p114=getRndDouble();byte  p115=getRndByte();
        double  p116=getRndDouble();double  p117=getRndDouble();byte  p118=getRndByte();
        double  p119=getRndDouble();double  p120=getRndDouble();byte  p121=getRndByte();
        double  p122=getRndDouble();double  p123=getRndDouble();double  p124=getRndDouble();
        byte  p125=getRndByte();double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%d\n",p12);
        ps.format("p13=%d\n",p13);ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%d\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);ps.format("p42=%d\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%d\n",p49);ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%d\n",p58);ps.format("p59=%d\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%d\n",p91);ps.format("p92=%d\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%d\n",p94);ps.format("p95=%d\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%d\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc27(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc28(double p0,double p1,byte p2,double p3,double p4,double p5,byte p6,int p7
        ,byte p8,int p9,int p10,byte p11,double p12,int p13,double p14,double p15
        ,byte p16,double p17,byte p18,double p19,double p20,byte p21,double p22
        ,byte p23,double p24,int p25,double p26,double p27,byte p28,double p29,int p30
        ,int p31,double p32,double p33,byte p34,byte p35,byte p36,byte p37,double p38
        ,double p39,double p40,double p41,int p42,double p43,double p44,double p45
        ,double p46,double p47,double p48,double p49,int p50,double p51,double p52
        ,byte p53,int p54,int p55,double p56,double p57,int p58,double p59,double p60
        ,double p61,double p62,byte p63,int p64,byte p65,double p66,double p67,int p68
        ,double p69,double p70,double p71,double p72,byte p73,double p74,double p75
        ,double p76,byte p77,byte p78,double p79,double p80,double p81,double p82
        ,double p83,byte p84,double p85,double p86,byte p87,double p88,byte p89
        ,double p90,double p91,int p92,byte p93,double p94,double p95,double p96
        ,double p97,double p98,double p99,byte p100,byte p101,double p102,double p103
        ,byte p104,double p105,double p106,byte p107,int p108,byte p109,byte p110
        ,byte p111,double p112,int p113,double p114,byte p115,int p116,double p117
        ,double p118,byte p119,byte p120,int p121,double p122,int p123,byte p124
        ,double p125,double p126    );
    private static void nativeFnc28_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();byte  p2=getRndByte();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        byte  p6=getRndByte();int  p7=getRndInt();byte  p8=getRndByte();int  p9=getRndInt();
        int  p10=getRndInt();byte  p11=getRndByte();double  p12=getRndDouble();
        int  p13=getRndInt();double  p14=getRndDouble();double  p15=getRndDouble();
        byte  p16=getRndByte();double  p17=getRndDouble();byte  p18=getRndByte();
        double  p19=getRndDouble();double  p20=getRndDouble();byte  p21=getRndByte();
        double  p22=getRndDouble();byte  p23=getRndByte();double  p24=getRndDouble();
        int  p25=getRndInt();double  p26=getRndDouble();double  p27=getRndDouble();
        byte  p28=getRndByte();double  p29=getRndDouble();int  p30=getRndInt();
        int  p31=getRndInt();double  p32=getRndDouble();double  p33=getRndDouble();
        byte  p34=getRndByte();byte  p35=getRndByte();byte  p36=getRndByte();byte  p37=getRndByte();
        double  p38=getRndDouble();double  p39=getRndDouble();double  p40=getRndDouble();
        double  p41=getRndDouble();int  p42=getRndInt();double  p43=getRndDouble();
        double  p44=getRndDouble();double  p45=getRndDouble();double  p46=getRndDouble();
        double  p47=getRndDouble();double  p48=getRndDouble();double  p49=getRndDouble();
        int  p50=getRndInt();double  p51=getRndDouble();double  p52=getRndDouble();
        byte  p53=getRndByte();int  p54=getRndInt();int  p55=getRndInt();double  p56=getRndDouble();
        double  p57=getRndDouble();int  p58=getRndInt();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        byte  p63=getRndByte();int  p64=getRndInt();byte  p65=getRndByte();double  p66=getRndDouble();
        double  p67=getRndDouble();int  p68=getRndInt();double  p69=getRndDouble();
        double  p70=getRndDouble();double  p71=getRndDouble();double  p72=getRndDouble();
        byte  p73=getRndByte();double  p74=getRndDouble();double  p75=getRndDouble();
        double  p76=getRndDouble();byte  p77=getRndByte();byte  p78=getRndByte();
        double  p79=getRndDouble();double  p80=getRndDouble();double  p81=getRndDouble();
        double  p82=getRndDouble();double  p83=getRndDouble();byte  p84=getRndByte();
        double  p85=getRndDouble();double  p86=getRndDouble();byte  p87=getRndByte();
        double  p88=getRndDouble();byte  p89=getRndByte();double  p90=getRndDouble();
        double  p91=getRndDouble();int  p92=getRndInt();byte  p93=getRndByte();
        double  p94=getRndDouble();double  p95=getRndDouble();double  p96=getRndDouble();
        double  p97=getRndDouble();double  p98=getRndDouble();double  p99=getRndDouble();
        byte  p100=getRndByte();byte  p101=getRndByte();double  p102=getRndDouble();
        double  p103=getRndDouble();byte  p104=getRndByte();double  p105=getRndDouble();
        double  p106=getRndDouble();byte  p107=getRndByte();int  p108=getRndInt();
        byte  p109=getRndByte();byte  p110=getRndByte();byte  p111=getRndByte();
        double  p112=getRndDouble();int  p113=getRndInt();double  p114=getRndDouble();
        byte  p115=getRndByte();int  p116=getRndInt();double  p117=getRndDouble();
        double  p118=getRndDouble();byte  p119=getRndByte();byte  p120=getRndByte();
        int  p121=getRndInt();double  p122=getRndDouble();int  p123=getRndInt();
        byte  p124=getRndByte();double  p125=getRndDouble();double  p126=getRndDouble();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%d\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%d\n",p6);ps.format("p7=%d\n",p7);ps.format("p8=%d\n",p8);
        ps.format("p9=%d\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%d\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%d\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%d\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%d\n",p30);ps.format("p31=%d\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);ps.format("p35=%d\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%d\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%d\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%d\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%d\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%d\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%d\n",p110);
        ps.format("p111=%d\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%d\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%d\n",p119);
        ps.format("p120=%d\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%d\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc28(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc29(float p0,float p1,double p2,double p3,float p4,float p5,int p6,double p7
        ,float p8,byte p9,float p10,float p11,float p12,double p13,double p14,int p15
        ,byte p16,float p17,float p18,byte p19,int p20,double p21,float p22,float p23
        ,int p24,float p25,float p26,byte p27,double p28,float p29,int p30,int p31
        ,float p32,float p33,float p34,double p35,double p36,float p37,double p38
        ,float p39,float p40,float p41,double p42,float p43,double p44,double p45
        ,double p46,int p47,double p48,float p49,byte p50,double p51,byte p52,double p53
        ,float p54,double p55,float p56,byte p57,float p58,double p59,float p60
        ,float p61,double p62,float p63,double p64,byte p65,float p66,float p67
        ,int p68,float p69,double p70,double p71,float p72,double p73,float p74
        ,float p75,byte p76,float p77,double p78,double p79,float p80,double p81
        ,double p82,byte p83,double p84,int p85,float p86,int p87,double p88,float p89
        ,byte p90,byte p91,float p92,byte p93,double p94,float p95,int p96,double p97
        ,double p98,float p99,double p100,float p101,float p102,double p103,float p104
        ,int p105,double p106,int p107,double p108,double p109,float p110,double p111
        ,float p112,double p113,double p114,int p115,float p116,float p117,int p118
        ,float p119,double p120,float p121,byte p122,float p123,float p124,float p125
        ,double p126    );
    private static void nativeFnc29_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();double  p2=getRndDouble();
        double  p3=getRndDouble();float  p4=getRndFloat();float  p5=getRndFloat();
        int  p6=getRndInt();double  p7=getRndDouble();float  p8=getRndFloat();byte  p9=getRndByte();
        float  p10=getRndFloat();float  p11=getRndFloat();float  p12=getRndFloat();
        double  p13=getRndDouble();double  p14=getRndDouble();int  p15=getRndInt();
        byte  p16=getRndByte();float  p17=getRndFloat();float  p18=getRndFloat();
        byte  p19=getRndByte();int  p20=getRndInt();double  p21=getRndDouble();
        float  p22=getRndFloat();float  p23=getRndFloat();int  p24=getRndInt();
        float  p25=getRndFloat();float  p26=getRndFloat();byte  p27=getRndByte();
        double  p28=getRndDouble();float  p29=getRndFloat();int  p30=getRndInt();
        int  p31=getRndInt();float  p32=getRndFloat();float  p33=getRndFloat();
        float  p34=getRndFloat();double  p35=getRndDouble();double  p36=getRndDouble();
        float  p37=getRndFloat();double  p38=getRndDouble();float  p39=getRndFloat();
        float  p40=getRndFloat();float  p41=getRndFloat();double  p42=getRndDouble();
        float  p43=getRndFloat();double  p44=getRndDouble();double  p45=getRndDouble();
        double  p46=getRndDouble();int  p47=getRndInt();double  p48=getRndDouble();
        float  p49=getRndFloat();byte  p50=getRndByte();double  p51=getRndDouble();
        byte  p52=getRndByte();double  p53=getRndDouble();float  p54=getRndFloat();
        double  p55=getRndDouble();float  p56=getRndFloat();byte  p57=getRndByte();
        float  p58=getRndFloat();double  p59=getRndDouble();float  p60=getRndFloat();
        float  p61=getRndFloat();double  p62=getRndDouble();float  p63=getRndFloat();
        double  p64=getRndDouble();byte  p65=getRndByte();float  p66=getRndFloat();
        float  p67=getRndFloat();int  p68=getRndInt();float  p69=getRndFloat();
        double  p70=getRndDouble();double  p71=getRndDouble();float  p72=getRndFloat();
        double  p73=getRndDouble();float  p74=getRndFloat();float  p75=getRndFloat();
        byte  p76=getRndByte();float  p77=getRndFloat();double  p78=getRndDouble();
        double  p79=getRndDouble();float  p80=getRndFloat();double  p81=getRndDouble();
        double  p82=getRndDouble();byte  p83=getRndByte();double  p84=getRndDouble();
        int  p85=getRndInt();float  p86=getRndFloat();int  p87=getRndInt();double  p88=getRndDouble();
        float  p89=getRndFloat();byte  p90=getRndByte();byte  p91=getRndByte();
        float  p92=getRndFloat();byte  p93=getRndByte();double  p94=getRndDouble();
        float  p95=getRndFloat();int  p96=getRndInt();double  p97=getRndDouble();
        double  p98=getRndDouble();float  p99=getRndFloat();double  p100=getRndDouble();
        float  p101=getRndFloat();float  p102=getRndFloat();double  p103=getRndDouble();
        float  p104=getRndFloat();int  p105=getRndInt();double  p106=getRndDouble();
        int  p107=getRndInt();double  p108=getRndDouble();double  p109=getRndDouble();
        float  p110=getRndFloat();double  p111=getRndDouble();float  p112=getRndFloat();
        double  p113=getRndDouble();double  p114=getRndDouble();int  p115=getRndInt();
        float  p116=getRndFloat();float  p117=getRndFloat();int  p118=getRndInt();
        float  p119=getRndFloat();double  p120=getRndDouble();float  p121=getRndFloat();
        byte  p122=getRndByte();float  p123=getRndFloat();float  p124=getRndFloat();
        float  p125=getRndFloat();double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%d\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%d\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%d\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%d\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%d\n",p30);
        ps.format("p31=%d\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%d\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%d\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%d\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc29(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc30(float p0,double p1,float p2,float p3,float p4,float p5,double p6,float p7
        ,double p8,float p9,float p10,float p11,float p12,float p13,float p14,float p15
        ,double p16,float p17,double p18,float p19,double p20,double p21,double p22
        ,float p23,float p24,float p25,float p26,float p27,float p28,float p29,float p30
        ,float p31,float p32,float p33,double p34,float p35,double p36,float p37
        ,double p38,float p39,float p40,float p41,double p42,float p43,float p44
        ,float p45,float p46,double p47,double p48,double p49,float p50,double p51
        ,double p52,float p53,float p54,float p55,double p56,double p57,float p58
        ,float p59,float p60,float p61,float p62,float p63,float p64,float p65,float p66
        ,float p67,float p68,double p69,float p70,float p71,float p72,float p73
        ,float p74,float p75,float p76,double p77,float p78,float p79,float p80
        ,double p81,float p82,float p83,float p84,double p85,double p86,double p87
        ,float p88,float p89,float p90,float p91,float p92,double p93,float p94
        ,float p95,float p96,float p97,float p98,double p99,double p100,float p101
        ,float p102,double p103,double p104,float p105,double p106,float p107,float p108
        ,float p109,float p110,double p111,float p112,float p113,float p114,float p115
        ,float p116,float p117,float p118,float p119,float p120,float p121,float p122
        ,float p123,double p124,float p125,float p126    );
    private static void nativeFnc30_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();double  p1=getRndDouble();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        double  p6=getRndDouble();float  p7=getRndFloat();double  p8=getRndDouble();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        float  p12=getRndFloat();float  p13=getRndFloat();float  p14=getRndFloat();
        float  p15=getRndFloat();double  p16=getRndDouble();float  p17=getRndFloat();
        double  p18=getRndDouble();float  p19=getRndFloat();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();float  p23=getRndFloat();
        float  p24=getRndFloat();float  p25=getRndFloat();float  p26=getRndFloat();
        float  p27=getRndFloat();float  p28=getRndFloat();float  p29=getRndFloat();
        float  p30=getRndFloat();float  p31=getRndFloat();float  p32=getRndFloat();
        float  p33=getRndFloat();double  p34=getRndDouble();float  p35=getRndFloat();
        double  p36=getRndDouble();float  p37=getRndFloat();double  p38=getRndDouble();
        float  p39=getRndFloat();float  p40=getRndFloat();float  p41=getRndFloat();
        double  p42=getRndDouble();float  p43=getRndFloat();float  p44=getRndFloat();
        float  p45=getRndFloat();float  p46=getRndFloat();double  p47=getRndDouble();
        double  p48=getRndDouble();double  p49=getRndDouble();float  p50=getRndFloat();
        double  p51=getRndDouble();double  p52=getRndDouble();float  p53=getRndFloat();
        float  p54=getRndFloat();float  p55=getRndFloat();double  p56=getRndDouble();
        double  p57=getRndDouble();float  p58=getRndFloat();float  p59=getRndFloat();
        float  p60=getRndFloat();float  p61=getRndFloat();float  p62=getRndFloat();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        float  p66=getRndFloat();float  p67=getRndFloat();float  p68=getRndFloat();
        double  p69=getRndDouble();float  p70=getRndFloat();float  p71=getRndFloat();
        float  p72=getRndFloat();float  p73=getRndFloat();float  p74=getRndFloat();
        float  p75=getRndFloat();float  p76=getRndFloat();double  p77=getRndDouble();
        float  p78=getRndFloat();float  p79=getRndFloat();float  p80=getRndFloat();
        double  p81=getRndDouble();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();double  p85=getRndDouble();double  p86=getRndDouble();
        double  p87=getRndDouble();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        double  p93=getRndDouble();float  p94=getRndFloat();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        double  p99=getRndDouble();double  p100=getRndDouble();float  p101=getRndFloat();
        float  p102=getRndFloat();double  p103=getRndDouble();double  p104=getRndDouble();
        float  p105=getRndFloat();double  p106=getRndDouble();float  p107=getRndFloat();
        float  p108=getRndFloat();float  p109=getRndFloat();float  p110=getRndFloat();
        double  p111=getRndDouble();float  p112=getRndFloat();float  p113=getRndFloat();
        float  p114=getRndFloat();float  p115=getRndFloat();float  p116=getRndFloat();
        float  p117=getRndFloat();float  p118=getRndFloat();float  p119=getRndFloat();
        float  p120=getRndFloat();float  p121=getRndFloat();float  p122=getRndFloat();
        float  p123=getRndFloat();double  p124=getRndDouble();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc30(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc31(double p0,double p1,float p2,float p3,float p4,double p5,float p6,float p7
        ,double p8,double p9,int p10,double p11,double p12,float p13,int p14,double p15
        ,float p16,float p17,float p18,float p19,float p20,int p21,float p22,double p23
        ,int p24,float p25,double p26,double p27,double p28,float p29,double p30
        ,float p31,double p32,float p33,float p34,float p35,double p36,float p37
        ,double p38,double p39,int p40,int p41,double p42,float p43,float p44,float p45
        ,double p46,double p47,float p48,float p49,double p50,double p51,double p52
        ,float p53,int p54,double p55,float p56,double p57,float p58,float p59,float p60
        ,double p61,float p62,int p63,double p64,int p65,int p66,double p67,double p68
        ,double p69,float p70,float p71,double p72,int p73,int p74,float p75,double p76
        ,float p77,float p78,float p79,float p80,double p81,double p82,double p83
        ,double p84,double p85,float p86,float p87,int p88,double p89,double p90
        ,float p91,double p92,float p93,int p94,float p95,float p96,double p97,int p98
        ,int p99,double p100,double p101,float p102,float p103,double p104,double p105
        ,double p106,double p107,double p108,float p109,float p110,double p111,float p112
        ,double p113,double p114,float p115,float p116,float p117,float p118,double p119
        ,int p120,double p121,float p122,double p123,double p124,double p125,float p126
            );
    private static void nativeFnc31_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();double  p5=getRndDouble();
        float  p6=getRndFloat();float  p7=getRndFloat();double  p8=getRndDouble();
        double  p9=getRndDouble();int  p10=getRndInt();double  p11=getRndDouble();
        double  p12=getRndDouble();float  p13=getRndFloat();int  p14=getRndInt();
        double  p15=getRndDouble();float  p16=getRndFloat();float  p17=getRndFloat();
        float  p18=getRndFloat();float  p19=getRndFloat();float  p20=getRndFloat();
        int  p21=getRndInt();float  p22=getRndFloat();double  p23=getRndDouble();
        int  p24=getRndInt();float  p25=getRndFloat();double  p26=getRndDouble();
        double  p27=getRndDouble();double  p28=getRndDouble();float  p29=getRndFloat();
        double  p30=getRndDouble();float  p31=getRndFloat();double  p32=getRndDouble();
        float  p33=getRndFloat();float  p34=getRndFloat();float  p35=getRndFloat();
        double  p36=getRndDouble();float  p37=getRndFloat();double  p38=getRndDouble();
        double  p39=getRndDouble();int  p40=getRndInt();int  p41=getRndInt();double  p42=getRndDouble();
        float  p43=getRndFloat();float  p44=getRndFloat();float  p45=getRndFloat();
        double  p46=getRndDouble();double  p47=getRndDouble();float  p48=getRndFloat();
        float  p49=getRndFloat();double  p50=getRndDouble();double  p51=getRndDouble();
        double  p52=getRndDouble();float  p53=getRndFloat();int  p54=getRndInt();
        double  p55=getRndDouble();float  p56=getRndFloat();double  p57=getRndDouble();
        float  p58=getRndFloat();float  p59=getRndFloat();float  p60=getRndFloat();
        double  p61=getRndDouble();float  p62=getRndFloat();int  p63=getRndInt();
        double  p64=getRndDouble();int  p65=getRndInt();int  p66=getRndInt();double  p67=getRndDouble();
        double  p68=getRndDouble();double  p69=getRndDouble();float  p70=getRndFloat();
        float  p71=getRndFloat();double  p72=getRndDouble();int  p73=getRndInt();
        int  p74=getRndInt();float  p75=getRndFloat();double  p76=getRndDouble();
        float  p77=getRndFloat();float  p78=getRndFloat();float  p79=getRndFloat();
        float  p80=getRndFloat();double  p81=getRndDouble();double  p82=getRndDouble();
        double  p83=getRndDouble();double  p84=getRndDouble();double  p85=getRndDouble();
        float  p86=getRndFloat();float  p87=getRndFloat();int  p88=getRndInt();
        double  p89=getRndDouble();double  p90=getRndDouble();float  p91=getRndFloat();
        double  p92=getRndDouble();float  p93=getRndFloat();int  p94=getRndInt();
        float  p95=getRndFloat();float  p96=getRndFloat();double  p97=getRndDouble();
        int  p98=getRndInt();int  p99=getRndInt();double  p100=getRndDouble();double  p101=getRndDouble();
        float  p102=getRndFloat();float  p103=getRndFloat();double  p104=getRndDouble();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();float  p109=getRndFloat();float  p110=getRndFloat();
        double  p111=getRndDouble();float  p112=getRndFloat();double  p113=getRndDouble();
        double  p114=getRndDouble();float  p115=getRndFloat();float  p116=getRndFloat();
        float  p117=getRndFloat();float  p118=getRndFloat();double  p119=getRndDouble();
        int  p120=getRndInt();double  p121=getRndDouble();float  p122=getRndFloat();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%d\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%d\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);
        ps.format("p74=%d\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%d\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%d\n",p98);ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc31(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc32(double p0,float p1,byte p2,float p3,double p4,byte p5,float p6,float p7
        ,double p8,float p9,float p10,float p11,double p12,float p13,float p14,double p15
        ,float p16,byte p17,float p18,float p19,float p20,float p21,byte p22,byte p23
        ,byte p24,float p25,float p26,double p27,byte p28,double p29,float p30,double p31
        ,double p32,float p33,byte p34,float p35,float p36,double p37,float p38
        ,double p39,float p40,double p41,byte p42,double p43,float p44,double p45
        ,byte p46,float p47,byte p48,float p49,float p50,float p51,float p52,double p53
        ,double p54,double p55,float p56,double p57,double p58,float p59,double p60
        ,double p61,double p62,double p63,float p64,double p65,double p66,double p67
        ,byte p68,double p69,float p70,double p71,byte p72,double p73,double p74
        ,byte p75,double p76,double p77,double p78,byte p79,double p80,double p81
        ,float p82,double p83,float p84,double p85,byte p86,double p87,double p88
        ,float p89,double p90,double p91,double p92,double p93,byte p94,float p95
        ,double p96,double p97,float p98,double p99,float p100,double p101,float p102
        ,float p103,byte p104,double p105,double p106,double p107,float p108,float p109
        ,float p110,byte p111,double p112,byte p113,double p114,double p115,float p116
        ,float p117,float p118,double p119,double p120,double p121,float p122,float p123
        ,float p124,byte p125,double p126    );
    private static void nativeFnc32_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();float  p1=getRndFloat();byte  p2=getRndByte();
        float  p3=getRndFloat();double  p4=getRndDouble();byte  p5=getRndByte();
        float  p6=getRndFloat();float  p7=getRndFloat();double  p8=getRndDouble();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        double  p12=getRndDouble();float  p13=getRndFloat();float  p14=getRndFloat();
        double  p15=getRndDouble();float  p16=getRndFloat();byte  p17=getRndByte();
        float  p18=getRndFloat();float  p19=getRndFloat();float  p20=getRndFloat();
        float  p21=getRndFloat();byte  p22=getRndByte();byte  p23=getRndByte();
        byte  p24=getRndByte();float  p25=getRndFloat();float  p26=getRndFloat();
        double  p27=getRndDouble();byte  p28=getRndByte();double  p29=getRndDouble();
        float  p30=getRndFloat();double  p31=getRndDouble();double  p32=getRndDouble();
        float  p33=getRndFloat();byte  p34=getRndByte();float  p35=getRndFloat();
        float  p36=getRndFloat();double  p37=getRndDouble();float  p38=getRndFloat();
        double  p39=getRndDouble();float  p40=getRndFloat();double  p41=getRndDouble();
        byte  p42=getRndByte();double  p43=getRndDouble();float  p44=getRndFloat();
        double  p45=getRndDouble();byte  p46=getRndByte();float  p47=getRndFloat();
        byte  p48=getRndByte();float  p49=getRndFloat();float  p50=getRndFloat();
        float  p51=getRndFloat();float  p52=getRndFloat();double  p53=getRndDouble();
        double  p54=getRndDouble();double  p55=getRndDouble();float  p56=getRndFloat();
        double  p57=getRndDouble();double  p58=getRndDouble();float  p59=getRndFloat();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();float  p64=getRndFloat();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();byte  p68=getRndByte();
        double  p69=getRndDouble();float  p70=getRndFloat();double  p71=getRndDouble();
        byte  p72=getRndByte();double  p73=getRndDouble();double  p74=getRndDouble();
        byte  p75=getRndByte();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();byte  p79=getRndByte();double  p80=getRndDouble();
        double  p81=getRndDouble();float  p82=getRndFloat();double  p83=getRndDouble();
        float  p84=getRndFloat();double  p85=getRndDouble();byte  p86=getRndByte();
        double  p87=getRndDouble();double  p88=getRndDouble();float  p89=getRndFloat();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();byte  p94=getRndByte();float  p95=getRndFloat();
        double  p96=getRndDouble();double  p97=getRndDouble();float  p98=getRndFloat();
        double  p99=getRndDouble();float  p100=getRndFloat();double  p101=getRndDouble();
        float  p102=getRndFloat();float  p103=getRndFloat();byte  p104=getRndByte();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        float  p108=getRndFloat();float  p109=getRndFloat();float  p110=getRndFloat();
        byte  p111=getRndByte();double  p112=getRndDouble();byte  p113=getRndByte();
        double  p114=getRndDouble();double  p115=getRndDouble();float  p116=getRndFloat();
        float  p117=getRndFloat();float  p118=getRndFloat();double  p119=getRndDouble();
        double  p120=getRndDouble();double  p121=getRndDouble();float  p122=getRndFloat();
        float  p123=getRndFloat();float  p124=getRndFloat();byte  p125=getRndByte();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%d\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%d\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%d\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%d\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%d\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc32(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc33(float p0,int p1,int p2,int p3,float p4,float p5,float p6,int p7,float p8
        ,float p9,float p10,float p11,float p12,float p13,float p14,float p15,float p16
        ,int p17,float p18,float p19,float p20,float p21,int p22,float p23,float p24
        ,float p25,float p26,float p27,float p28,float p29,float p30,float p31,float p32
        ,float p33,float p34,float p35,float p36,float p37,int p38,int p39,int p40
        ,float p41,float p42,float p43,float p44,int p45,float p46,float p47,int p48
        ,float p49,float p50,float p51,float p52,int p53,float p54,float p55,float p56
        ,float p57,float p58,float p59,float p60,int p61,float p62,float p63,float p64
        ,float p65,int p66,float p67,float p68,float p69,float p70,int p71,float p72
        ,float p73,float p74,float p75,float p76,float p77,float p78,float p79,float p80
        ,float p81,float p82,float p83,float p84,float p85,float p86,float p87,float p88
        ,int p89,int p90,float p91,float p92,float p93,int p94,float p95,float p96
        ,float p97,float p98,float p99,float p100,int p101,float p102,float p103
        ,float p104,int p105,int p106,float p107,int p108,float p109,float p110
        ,float p111,float p112,float p113,float p114,int p115,int p116,float p117
        ,float p118,int p119,float p120,float p121,float p122,float p123,float p124
        ,float p125,float p126    );
    private static void nativeFnc33_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();int  p1=getRndInt();int  p2=getRndInt();
        int  p3=getRndInt();float  p4=getRndFloat();float  p5=getRndFloat();float  p6=getRndFloat();
        int  p7=getRndInt();float  p8=getRndFloat();float  p9=getRndFloat();float  p10=getRndFloat();
        float  p11=getRndFloat();float  p12=getRndFloat();float  p13=getRndFloat();
        float  p14=getRndFloat();float  p15=getRndFloat();float  p16=getRndFloat();
        int  p17=getRndInt();float  p18=getRndFloat();float  p19=getRndFloat();
        float  p20=getRndFloat();float  p21=getRndFloat();int  p22=getRndInt();
        float  p23=getRndFloat();float  p24=getRndFloat();float  p25=getRndFloat();
        float  p26=getRndFloat();float  p27=getRndFloat();float  p28=getRndFloat();
        float  p29=getRndFloat();float  p30=getRndFloat();float  p31=getRndFloat();
        float  p32=getRndFloat();float  p33=getRndFloat();float  p34=getRndFloat();
        float  p35=getRndFloat();float  p36=getRndFloat();float  p37=getRndFloat();
        int  p38=getRndInt();int  p39=getRndInt();int  p40=getRndInt();float  p41=getRndFloat();
        float  p42=getRndFloat();float  p43=getRndFloat();float  p44=getRndFloat();
        int  p45=getRndInt();float  p46=getRndFloat();float  p47=getRndFloat();
        int  p48=getRndInt();float  p49=getRndFloat();float  p50=getRndFloat();
        float  p51=getRndFloat();float  p52=getRndFloat();int  p53=getRndInt();
        float  p54=getRndFloat();float  p55=getRndFloat();float  p56=getRndFloat();
        float  p57=getRndFloat();float  p58=getRndFloat();float  p59=getRndFloat();
        float  p60=getRndFloat();int  p61=getRndInt();float  p62=getRndFloat();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        int  p66=getRndInt();float  p67=getRndFloat();float  p68=getRndFloat();
        float  p69=getRndFloat();float  p70=getRndFloat();int  p71=getRndInt();
        float  p72=getRndFloat();float  p73=getRndFloat();float  p74=getRndFloat();
        float  p75=getRndFloat();float  p76=getRndFloat();float  p77=getRndFloat();
        float  p78=getRndFloat();float  p79=getRndFloat();float  p80=getRndFloat();
        float  p81=getRndFloat();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();float  p85=getRndFloat();float  p86=getRndFloat();
        float  p87=getRndFloat();float  p88=getRndFloat();int  p89=getRndInt();
        int  p90=getRndInt();float  p91=getRndFloat();float  p92=getRndFloat();
        float  p93=getRndFloat();int  p94=getRndInt();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();float  p100=getRndFloat();int  p101=getRndInt();
        float  p102=getRndFloat();float  p103=getRndFloat();float  p104=getRndFloat();
        int  p105=getRndInt();int  p106=getRndInt();float  p107=getRndFloat();int  p108=getRndInt();
        float  p109=getRndFloat();float  p110=getRndFloat();float  p111=getRndFloat();
        float  p112=getRndFloat();float  p113=getRndFloat();float  p114=getRndFloat();
        int  p115=getRndInt();int  p116=getRndInt();float  p117=getRndFloat();float  p118=getRndFloat();
        int  p119=getRndInt();float  p120=getRndFloat();float  p121=getRndFloat();
        float  p122=getRndFloat();float  p123=getRndFloat();float  p124=getRndFloat();
        float  p125=getRndFloat();float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%d\n",p1);ps.format("p2=%d\n",p2);ps.format("p3=%d\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%d\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);ps.format("p39=%d\n",p39);
        ps.format("p40=%d\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%d\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%d\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);ps.format("p90=%d\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);
        ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%d\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%d\n",p115);ps.format("p116=%d\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%d\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc33(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc34(float p0,float p1,float p2,float p3,float p4,byte p5,float p6,float p7,float p8
        ,float p9,float p10,float p11,byte p12,float p13,float p14,byte p15,float p16
        ,float p17,byte p18,byte p19,float p20,float p21,byte p22,float p23,float p24
        ,byte p25,byte p26,byte p27,float p28,float p29,float p30,float p31,float p32
        ,float p33,float p34,float p35,byte p36,float p37,float p38,float p39,float p40
        ,float p41,float p42,float p43,float p44,byte p45,float p46,byte p47,float p48
        ,byte p49,float p50,float p51,float p52,float p53,float p54,float p55,float p56
        ,byte p57,float p58,float p59,float p60,float p61,byte p62,float p63,float p64
        ,float p65,float p66,float p67,byte p68,byte p69,float p70,float p71,float p72
        ,float p73,float p74,byte p75,float p76,float p77,byte p78,float p79,float p80
        ,float p81,float p82,float p83,float p84,float p85,float p86,byte p87,byte p88
        ,float p89,float p90,float p91,float p92,float p93,float p94,float p95,float p96
        ,byte p97,float p98,float p99,float p100,float p101,byte p102,float p103
        ,float p104,byte p105,float p106,float p107,float p108,float p109,float p110
        ,float p111,float p112,byte p113,float p114,float p115,float p116,float p117
        ,float p118,float p119,float p120,float p121,float p122,float p123,float p124
        ,byte p125,byte p126    );
    private static void nativeFnc34_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();byte  p5=getRndByte();float  p6=getRndFloat();
        float  p7=getRndFloat();float  p8=getRndFloat();float  p9=getRndFloat();
        float  p10=getRndFloat();float  p11=getRndFloat();byte  p12=getRndByte();
        float  p13=getRndFloat();float  p14=getRndFloat();byte  p15=getRndByte();
        float  p16=getRndFloat();float  p17=getRndFloat();byte  p18=getRndByte();
        byte  p19=getRndByte();float  p20=getRndFloat();float  p21=getRndFloat();
        byte  p22=getRndByte();float  p23=getRndFloat();float  p24=getRndFloat();
        byte  p25=getRndByte();byte  p26=getRndByte();byte  p27=getRndByte();float  p28=getRndFloat();
        float  p29=getRndFloat();float  p30=getRndFloat();float  p31=getRndFloat();
        float  p32=getRndFloat();float  p33=getRndFloat();float  p34=getRndFloat();
        float  p35=getRndFloat();byte  p36=getRndByte();float  p37=getRndFloat();
        float  p38=getRndFloat();float  p39=getRndFloat();float  p40=getRndFloat();
        float  p41=getRndFloat();float  p42=getRndFloat();float  p43=getRndFloat();
        float  p44=getRndFloat();byte  p45=getRndByte();float  p46=getRndFloat();
        byte  p47=getRndByte();float  p48=getRndFloat();byte  p49=getRndByte();
        float  p50=getRndFloat();float  p51=getRndFloat();float  p52=getRndFloat();
        float  p53=getRndFloat();float  p54=getRndFloat();float  p55=getRndFloat();
        float  p56=getRndFloat();byte  p57=getRndByte();float  p58=getRndFloat();
        float  p59=getRndFloat();float  p60=getRndFloat();float  p61=getRndFloat();
        byte  p62=getRndByte();float  p63=getRndFloat();float  p64=getRndFloat();
        float  p65=getRndFloat();float  p66=getRndFloat();float  p67=getRndFloat();
        byte  p68=getRndByte();byte  p69=getRndByte();float  p70=getRndFloat();
        float  p71=getRndFloat();float  p72=getRndFloat();float  p73=getRndFloat();
        float  p74=getRndFloat();byte  p75=getRndByte();float  p76=getRndFloat();
        float  p77=getRndFloat();byte  p78=getRndByte();float  p79=getRndFloat();
        float  p80=getRndFloat();float  p81=getRndFloat();float  p82=getRndFloat();
        float  p83=getRndFloat();float  p84=getRndFloat();float  p85=getRndFloat();
        float  p86=getRndFloat();byte  p87=getRndByte();byte  p88=getRndByte();
        float  p89=getRndFloat();float  p90=getRndFloat();float  p91=getRndFloat();
        float  p92=getRndFloat();float  p93=getRndFloat();float  p94=getRndFloat();
        float  p95=getRndFloat();float  p96=getRndFloat();byte  p97=getRndByte();
        float  p98=getRndFloat();float  p99=getRndFloat();float  p100=getRndFloat();
        float  p101=getRndFloat();byte  p102=getRndByte();float  p103=getRndFloat();
        float  p104=getRndFloat();byte  p105=getRndByte();float  p106=getRndFloat();
        float  p107=getRndFloat();float  p108=getRndFloat();float  p109=getRndFloat();
        float  p110=getRndFloat();float  p111=getRndFloat();float  p112=getRndFloat();
        byte  p113=getRndByte();float  p114=getRndFloat();float  p115=getRndFloat();
        float  p116=getRndFloat();float  p117=getRndFloat();float  p118=getRndFloat();
        float  p119=getRndFloat();float  p120=getRndFloat();float  p121=getRndFloat();
        float  p122=getRndFloat();float  p123=getRndFloat();float  p124=getRndFloat();
        byte  p125=getRndByte();byte  p126=getRndByte();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%d\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%d\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%d\n",p18);
        ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%d\n",p22);ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%d\n",p25);ps.format("p26=%d\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%d\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%e\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%d\n",p69);
        ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%d\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%d\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%d\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%d\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);ps.format("p126=%d\n",p126);

        nativeFnc34(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc35(int p0,byte p1,byte p2,float p3,float p4,float p5,float p6,float p7,int p8
        ,float p9,int p10,float p11,float p12,byte p13,byte p14,float p15,float p16
        ,byte p17,byte p18,float p19,float p20,float p21,float p22,float p23,float p24
        ,float p25,float p26,int p27,float p28,float p29,float p30,byte p31,float p32
        ,float p33,float p34,float p35,byte p36,float p37,int p38,float p39,byte p40
        ,byte p41,float p42,float p43,float p44,int p45,float p46,byte p47,byte p48
        ,int p49,float p50,int p51,float p52,float p53,float p54,byte p55,byte p56
        ,float p57,float p58,float p59,int p60,int p61,float p62,int p63,byte p64
        ,float p65,int p66,float p67,float p68,byte p69,float p70,float p71,float p72
        ,int p73,float p74,float p75,float p76,byte p77,float p78,float p79,float p80
        ,byte p81,float p82,byte p83,float p84,float p85,byte p86,float p87,float p88
        ,float p89,float p90,byte p91,float p92,float p93,float p94,float p95,float p96
        ,float p97,float p98,int p99,float p100,float p101,int p102,float p103,float p104
        ,byte p105,float p106,int p107,float p108,float p109,float p110,float p111
        ,float p112,int p113,float p114,float p115,float p116,float p117,byte p118
        ,float p119,byte p120,float p121,float p122,int p123,float p124,float p125
        ,float p126    );
    private static void nativeFnc35_invoke(PrintStream ps)
    {
        int  p0=getRndInt();byte  p1=getRndByte();byte  p2=getRndByte();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        float  p6=getRndFloat();float  p7=getRndFloat();int  p8=getRndInt();float  p9=getRndFloat();
        int  p10=getRndInt();float  p11=getRndFloat();float  p12=getRndFloat();
        byte  p13=getRndByte();byte  p14=getRndByte();float  p15=getRndFloat();
        float  p16=getRndFloat();byte  p17=getRndByte();byte  p18=getRndByte();
        float  p19=getRndFloat();float  p20=getRndFloat();float  p21=getRndFloat();
        float  p22=getRndFloat();float  p23=getRndFloat();float  p24=getRndFloat();
        float  p25=getRndFloat();float  p26=getRndFloat();int  p27=getRndInt();
        float  p28=getRndFloat();float  p29=getRndFloat();float  p30=getRndFloat();
        byte  p31=getRndByte();float  p32=getRndFloat();float  p33=getRndFloat();
        float  p34=getRndFloat();float  p35=getRndFloat();byte  p36=getRndByte();
        float  p37=getRndFloat();int  p38=getRndInt();float  p39=getRndFloat();
        byte  p40=getRndByte();byte  p41=getRndByte();float  p42=getRndFloat();
        float  p43=getRndFloat();float  p44=getRndFloat();int  p45=getRndInt();
        float  p46=getRndFloat();byte  p47=getRndByte();byte  p48=getRndByte();
        int  p49=getRndInt();float  p50=getRndFloat();int  p51=getRndInt();float  p52=getRndFloat();
        float  p53=getRndFloat();float  p54=getRndFloat();byte  p55=getRndByte();
        byte  p56=getRndByte();float  p57=getRndFloat();float  p58=getRndFloat();
        float  p59=getRndFloat();int  p60=getRndInt();int  p61=getRndInt();float  p62=getRndFloat();
        int  p63=getRndInt();byte  p64=getRndByte();float  p65=getRndFloat();int  p66=getRndInt();
        float  p67=getRndFloat();float  p68=getRndFloat();byte  p69=getRndByte();
        float  p70=getRndFloat();float  p71=getRndFloat();float  p72=getRndFloat();
        int  p73=getRndInt();float  p74=getRndFloat();float  p75=getRndFloat();
        float  p76=getRndFloat();byte  p77=getRndByte();float  p78=getRndFloat();
        float  p79=getRndFloat();float  p80=getRndFloat();byte  p81=getRndByte();
        float  p82=getRndFloat();byte  p83=getRndByte();float  p84=getRndFloat();
        float  p85=getRndFloat();byte  p86=getRndByte();float  p87=getRndFloat();
        float  p88=getRndFloat();float  p89=getRndFloat();float  p90=getRndFloat();
        byte  p91=getRndByte();float  p92=getRndFloat();float  p93=getRndFloat();
        float  p94=getRndFloat();float  p95=getRndFloat();float  p96=getRndFloat();
        float  p97=getRndFloat();float  p98=getRndFloat();int  p99=getRndInt();
        float  p100=getRndFloat();float  p101=getRndFloat();int  p102=getRndInt();
        float  p103=getRndFloat();float  p104=getRndFloat();byte  p105=getRndByte();
        float  p106=getRndFloat();int  p107=getRndInt();float  p108=getRndFloat();
        float  p109=getRndFloat();float  p110=getRndFloat();float  p111=getRndFloat();
        float  p112=getRndFloat();int  p113=getRndInt();float  p114=getRndFloat();
        float  p115=getRndFloat();float  p116=getRndFloat();float  p117=getRndFloat();
        byte  p118=getRndByte();float  p119=getRndFloat();byte  p120=getRndByte();
        float  p121=getRndFloat();float  p122=getRndFloat();int  p123=getRndInt();
        float  p124=getRndFloat();float  p125=getRndFloat();float  p126=getRndFloat();

        ps.format("p0=%d\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%d\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%d\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%d\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%d\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%d\n",p40);ps.format("p41=%d\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%d\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%d\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%d\n",p60);ps.format("p61=%d\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%d\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%d\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%d\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc35(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc36(int p0,int p1,double p2,double p3,double p4,double p5,double p6,int p7,double p8
        ,double p9,double p10,double p11,double p12,double p13,double p14,double p15
        ,double p16,double p17,int p18,int p19,double p20,double p21,double p22
        ,double p23,double p24,double p25,double p26,double p27,double p28,double p29
        ,double p30,double p31,double p32,double p33,double p34,int p35,double p36
        ,double p37,double p38,double p39,double p40,double p41,double p42,double p43
        ,int p44,double p45,double p46,int p47,int p48,double p49,double p50,double p51
        ,double p52,double p53,double p54,double p55,int p56,double p57,double p58
        ,double p59,double p60,double p61,double p62,double p63,double p64,double p65
        ,double p66,double p67,double p68,double p69,int p70,double p71,int p72
        ,double p73,double p74,double p75,double p76,double p77,double p78,double p79
        ,int p80,double p81,double p82,double p83,double p84,double p85,double p86
        ,double p87,double p88,double p89,double p90,double p91,int p92,double p93
        ,double p94,double p95,double p96,double p97,double p98,int p99,int p100
        ,int p101,int p102,int p103,int p104,int p105,int p106,double p107,double p108
        ,double p109,int p110,double p111,double p112,double p113,double p114,double p115
        ,double p116,double p117,double p118,double p119,double p120,double p121
        ,double p122,double p123,double p124,double p125,double p126    );
    private static void nativeFnc36_invoke(PrintStream ps)
    {
        int  p0=getRndInt();int  p1=getRndInt();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();int  p7=getRndInt();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        int  p18=getRndInt();int  p19=getRndInt();double  p20=getRndDouble();double  p21=getRndDouble();
        double  p22=getRndDouble();double  p23=getRndDouble();double  p24=getRndDouble();
        double  p25=getRndDouble();double  p26=getRndDouble();double  p27=getRndDouble();
        double  p28=getRndDouble();double  p29=getRndDouble();double  p30=getRndDouble();
        double  p31=getRndDouble();double  p32=getRndDouble();double  p33=getRndDouble();
        double  p34=getRndDouble();int  p35=getRndInt();double  p36=getRndDouble();
        double  p37=getRndDouble();double  p38=getRndDouble();double  p39=getRndDouble();
        double  p40=getRndDouble();double  p41=getRndDouble();double  p42=getRndDouble();
        double  p43=getRndDouble();int  p44=getRndInt();double  p45=getRndDouble();
        double  p46=getRndDouble();int  p47=getRndInt();int  p48=getRndInt();double  p49=getRndDouble();
        double  p50=getRndDouble();double  p51=getRndDouble();double  p52=getRndDouble();
        double  p53=getRndDouble();double  p54=getRndDouble();double  p55=getRndDouble();
        int  p56=getRndInt();double  p57=getRndDouble();double  p58=getRndDouble();
        double  p59=getRndDouble();double  p60=getRndDouble();double  p61=getRndDouble();
        double  p62=getRndDouble();double  p63=getRndDouble();double  p64=getRndDouble();
        double  p65=getRndDouble();double  p66=getRndDouble();double  p67=getRndDouble();
        double  p68=getRndDouble();double  p69=getRndDouble();int  p70=getRndInt();
        double  p71=getRndDouble();int  p72=getRndInt();double  p73=getRndDouble();
        double  p74=getRndDouble();double  p75=getRndDouble();double  p76=getRndDouble();
        double  p77=getRndDouble();double  p78=getRndDouble();double  p79=getRndDouble();
        int  p80=getRndInt();double  p81=getRndDouble();double  p82=getRndDouble();
        double  p83=getRndDouble();double  p84=getRndDouble();double  p85=getRndDouble();
        double  p86=getRndDouble();double  p87=getRndDouble();double  p88=getRndDouble();
        double  p89=getRndDouble();double  p90=getRndDouble();double  p91=getRndDouble();
        int  p92=getRndInt();double  p93=getRndDouble();double  p94=getRndDouble();
        double  p95=getRndDouble();double  p96=getRndDouble();double  p97=getRndDouble();
        double  p98=getRndDouble();int  p99=getRndInt();int  p100=getRndInt();int  p101=getRndInt();
        int  p102=getRndInt();int  p103=getRndInt();int  p104=getRndInt();int  p105=getRndInt();
        int  p106=getRndInt();double  p107=getRndDouble();double  p108=getRndDouble();
        double  p109=getRndDouble();int  p110=getRndInt();double  p111=getRndDouble();
        double  p112=getRndDouble();double  p113=getRndDouble();double  p114=getRndDouble();
        double  p115=getRndDouble();double  p116=getRndDouble();double  p117=getRndDouble();
        double  p118=getRndDouble();double  p119=getRndDouble();double  p120=getRndDouble();
        double  p121=getRndDouble();double  p122=getRndDouble();double  p123=getRndDouble();
        double  p124=getRndDouble();double  p125=getRndDouble();double  p126=getRndDouble();

        ps.format("p0=%d\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%d\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%d\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%d\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%d\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%d\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%d\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%d\n",p103);ps.format("p104=%d\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%d\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%d\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc36(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc37(double p0,double p1,double p2,double p3,double p4,double p5,double p6,double p7
        ,double p8,double p9,double p10,byte p11,double p12,double p13,double p14
        ,double p15,double p16,double p17,double p18,double p19,double p20,double p21
        ,byte p22,byte p23,double p24,double p25,byte p26,double p27,double p28
        ,double p29,double p30,double p31,double p32,double p33,double p34,double p35
        ,double p36,double p37,byte p38,byte p39,double p40,double p41,double p42
        ,double p43,double p44,byte p45,double p46,double p47,double p48,byte p49
        ,double p50,double p51,double p52,double p53,double p54,double p55,byte p56
        ,byte p57,double p58,double p59,double p60,double p61,double p62,double p63
        ,double p64,double p65,byte p66,double p67,double p68,double p69,double p70
        ,byte p71,double p72,double p73,double p74,double p75,double p76,double p77
        ,double p78,double p79,double p80,double p81,double p82,double p83,double p84
        ,double p85,double p86,double p87,double p88,double p89,double p90,double p91
        ,double p92,double p93,double p94,double p95,byte p96,byte p97,double p98
        ,double p99,double p100,double p101,double p102,double p103,double p104
        ,double p105,double p106,double p107,double p108,byte p109,double p110,double p111
        ,double p112,double p113,double p114,double p115,double p116,double p117
        ,byte p118,double p119,double p120,double p121,double p122,double p123,byte p124
        ,double p125,byte p126    );
    private static void nativeFnc37_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();byte  p11=getRndByte();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();byte  p22=getRndByte();byte  p23=getRndByte();
        double  p24=getRndDouble();double  p25=getRndDouble();byte  p26=getRndByte();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();byte  p38=getRndByte();
        byte  p39=getRndByte();double  p40=getRndDouble();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        byte  p45=getRndByte();double  p46=getRndDouble();double  p47=getRndDouble();
        double  p48=getRndDouble();byte  p49=getRndByte();double  p50=getRndDouble();
        double  p51=getRndDouble();double  p52=getRndDouble();double  p53=getRndDouble();
        double  p54=getRndDouble();double  p55=getRndDouble();byte  p56=getRndByte();
        byte  p57=getRndByte();double  p58=getRndDouble();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();double  p64=getRndDouble();double  p65=getRndDouble();
        byte  p66=getRndByte();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();double  p70=getRndDouble();byte  p71=getRndByte();
        double  p72=getRndDouble();double  p73=getRndDouble();double  p74=getRndDouble();
        double  p75=getRndDouble();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();double  p82=getRndDouble();double  p83=getRndDouble();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        double  p87=getRndDouble();double  p88=getRndDouble();double  p89=getRndDouble();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();double  p94=getRndDouble();double  p95=getRndDouble();
        byte  p96=getRndByte();byte  p97=getRndByte();double  p98=getRndDouble();
        double  p99=getRndDouble();double  p100=getRndDouble();double  p101=getRndDouble();
        double  p102=getRndDouble();double  p103=getRndDouble();double  p104=getRndDouble();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();byte  p109=getRndByte();double  p110=getRndDouble();
        double  p111=getRndDouble();double  p112=getRndDouble();double  p113=getRndDouble();
        double  p114=getRndDouble();double  p115=getRndDouble();double  p116=getRndDouble();
        double  p117=getRndDouble();byte  p118=getRndByte();double  p119=getRndDouble();
        double  p120=getRndDouble();double  p121=getRndDouble();double  p122=getRndDouble();
        double  p123=getRndDouble();byte  p124=getRndByte();double  p125=getRndDouble();
        byte  p126=getRndByte();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%d\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%d\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%d\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%d\n",p38);ps.format("p39=%d\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%d\n",p56);ps.format("p57=%d\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%d\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%d\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%d\n",p126);
        nativeFnc37(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc38(double p0,double p1,double p2,int p3,int p4,double p5,double p6,double p7
        ,double p8,double p9,double p10,int p11,double p12,double p13,int p14,byte p15
        ,double p16,double p17,double p18,double p19,int p20,double p21,double p22
        ,double p23,double p24,int p25,double p26,int p27,byte p28,double p29,byte p30
        ,double p31,double p32,int p33,double p34,int p35,byte p36,byte p37,double p38
        ,double p39,double p40,double p41,int p42,double p43,double p44,double p45
        ,double p46,double p47,int p48,double p49,int p50,int p51,double p52,byte p53
        ,double p54,double p55,byte p56,double p57,double p58,double p59,double p60
        ,double p61,byte p62,double p63,double p64,double p65,byte p66,int p67,double p68
        ,double p69,double p70,double p71,double p72,byte p73,double p74,double p75
        ,double p76,byte p77,double p78,double p79,double p80,double p81,double p82
        ,int p83,double p84,double p85,double p86,int p87,double p88,int p89,double p90
        ,double p91,double p92,double p93,double p94,int p95,byte p96,byte p97,double p98
        ,int p99,double p100,double p101,double p102,double p103,double p104,double p105
        ,double p106,double p107,double p108,double p109,double p110,double p111
        ,int p112,double p113,double p114,double p115,double p116,byte p117,double p118
        ,double p119,byte p120,int p121,double p122,int p123,double p124,double p125
        ,byte p126    );
    private static void nativeFnc38_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        int  p3=getRndInt();int  p4=getRndInt();double  p5=getRndDouble();double  p6=getRndDouble();
        double  p7=getRndDouble();double  p8=getRndDouble();double  p9=getRndDouble();
        double  p10=getRndDouble();int  p11=getRndInt();double  p12=getRndDouble();
        double  p13=getRndDouble();int  p14=getRndInt();byte  p15=getRndByte();
        double  p16=getRndDouble();double  p17=getRndDouble();double  p18=getRndDouble();
        double  p19=getRndDouble();int  p20=getRndInt();double  p21=getRndDouble();
        double  p22=getRndDouble();double  p23=getRndDouble();double  p24=getRndDouble();
        int  p25=getRndInt();double  p26=getRndDouble();int  p27=getRndInt();byte  p28=getRndByte();
        double  p29=getRndDouble();byte  p30=getRndByte();double  p31=getRndDouble();
        double  p32=getRndDouble();int  p33=getRndInt();double  p34=getRndDouble();
        int  p35=getRndInt();byte  p36=getRndByte();byte  p37=getRndByte();double  p38=getRndDouble();
        double  p39=getRndDouble();double  p40=getRndDouble();double  p41=getRndDouble();
        int  p42=getRndInt();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();double  p46=getRndDouble();double  p47=getRndDouble();
        int  p48=getRndInt();double  p49=getRndDouble();int  p50=getRndInt();int  p51=getRndInt();
        double  p52=getRndDouble();byte  p53=getRndByte();double  p54=getRndDouble();
        double  p55=getRndDouble();byte  p56=getRndByte();double  p57=getRndDouble();
        double  p58=getRndDouble();double  p59=getRndDouble();double  p60=getRndDouble();
        double  p61=getRndDouble();byte  p62=getRndByte();double  p63=getRndDouble();
        double  p64=getRndDouble();double  p65=getRndDouble();byte  p66=getRndByte();
        int  p67=getRndInt();double  p68=getRndDouble();double  p69=getRndDouble();
        double  p70=getRndDouble();double  p71=getRndDouble();double  p72=getRndDouble();
        byte  p73=getRndByte();double  p74=getRndDouble();double  p75=getRndDouble();
        double  p76=getRndDouble();byte  p77=getRndByte();double  p78=getRndDouble();
        double  p79=getRndDouble();double  p80=getRndDouble();double  p81=getRndDouble();
        double  p82=getRndDouble();int  p83=getRndInt();double  p84=getRndDouble();
        double  p85=getRndDouble();double  p86=getRndDouble();int  p87=getRndInt();
        double  p88=getRndDouble();int  p89=getRndInt();double  p90=getRndDouble();
        double  p91=getRndDouble();double  p92=getRndDouble();double  p93=getRndDouble();
        double  p94=getRndDouble();int  p95=getRndInt();byte  p96=getRndByte();
        byte  p97=getRndByte();double  p98=getRndDouble();int  p99=getRndInt();
        double  p100=getRndDouble();double  p101=getRndDouble();double  p102=getRndDouble();
        double  p103=getRndDouble();double  p104=getRndDouble();double  p105=getRndDouble();
        double  p106=getRndDouble();double  p107=getRndDouble();double  p108=getRndDouble();
        double  p109=getRndDouble();double  p110=getRndDouble();double  p111=getRndDouble();
        int  p112=getRndInt();double  p113=getRndDouble();double  p114=getRndDouble();
        double  p115=getRndDouble();double  p116=getRndDouble();byte  p117=getRndByte();
        double  p118=getRndDouble();double  p119=getRndDouble();byte  p120=getRndByte();
        int  p121=getRndInt();double  p122=getRndDouble();int  p123=getRndInt();
        double  p124=getRndDouble();double  p125=getRndDouble();byte  p126=getRndByte();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%d\n",p3);ps.format("p4=%d\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%d\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%d\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%d\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%d\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%d\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%d\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%d\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%d\n",p96);ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%d\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%d\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%d\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%d\n",p126);
        nativeFnc38(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc39(double p0,float p1,double p2,byte p3,double p4,int p5,double p6,float p7
        ,float p8,double p9,int p10,byte p11,float p12,double p13,int p14,int p15
        ,float p16,byte p17,double p18,double p19,float p20,float p21,double p22
        ,float p23,float p24,double p25,float p26,float p27,float p28,float p29
        ,float p30,double p31,double p32,double p33,float p34,double p35,float p36
        ,double p37,float p38,float p39,float p40,int p41,byte p42,double p43,float p44
        ,float p45,int p46,byte p47,float p48,double p49,double p50,float p51,float p52
        ,float p53,double p54,int p55,float p56,float p57,float p58,float p59,float p60
        ,double p61,double p62,float p63,float p64,float p65,double p66,float p67
        ,double p68,double p69,double p70,double p71,double p72,double p73,double p74
        ,float p75,int p76,float p77,double p78,double p79,double p80,double p81
        ,byte p82,float p83,float p84,float p85,float p86,double p87,float p88,float p89
        ,float p90,float p91,float p92,double p93,float p94,double p95,float p96
        ,double p97,float p98,float p99,double p100,double p101,float p102,float p103
        ,double p104,double p105,float p106,byte p107,double p108,float p109,float p110
        ,double p111,double p112,double p113,double p114,double p115,double p116
        ,float p117,double p118,float p119,float p120,float p121,float p122,float p123
        ,double p124,float p125,float p126    );
    private static void nativeFnc39_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();float  p1=getRndFloat();double  p2=getRndDouble();
        byte  p3=getRndByte();double  p4=getRndDouble();int  p5=getRndInt();double  p6=getRndDouble();
        float  p7=getRndFloat();float  p8=getRndFloat();double  p9=getRndDouble();
        int  p10=getRndInt();byte  p11=getRndByte();float  p12=getRndFloat();double  p13=getRndDouble();
        int  p14=getRndInt();int  p15=getRndInt();float  p16=getRndFloat();byte  p17=getRndByte();
        double  p18=getRndDouble();double  p19=getRndDouble();float  p20=getRndFloat();
        float  p21=getRndFloat();double  p22=getRndDouble();float  p23=getRndFloat();
        float  p24=getRndFloat();double  p25=getRndDouble();float  p26=getRndFloat();
        float  p27=getRndFloat();float  p28=getRndFloat();float  p29=getRndFloat();
        float  p30=getRndFloat();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();float  p34=getRndFloat();double  p35=getRndDouble();
        float  p36=getRndFloat();double  p37=getRndDouble();float  p38=getRndFloat();
        float  p39=getRndFloat();float  p40=getRndFloat();int  p41=getRndInt();
        byte  p42=getRndByte();double  p43=getRndDouble();float  p44=getRndFloat();
        float  p45=getRndFloat();int  p46=getRndInt();byte  p47=getRndByte();float  p48=getRndFloat();
        double  p49=getRndDouble();double  p50=getRndDouble();float  p51=getRndFloat();
        float  p52=getRndFloat();float  p53=getRndFloat();double  p54=getRndDouble();
        int  p55=getRndInt();float  p56=getRndFloat();float  p57=getRndFloat();
        float  p58=getRndFloat();float  p59=getRndFloat();float  p60=getRndFloat();
        double  p61=getRndDouble();double  p62=getRndDouble();float  p63=getRndFloat();
        float  p64=getRndFloat();float  p65=getRndFloat();double  p66=getRndDouble();
        float  p67=getRndFloat();double  p68=getRndDouble();double  p69=getRndDouble();
        double  p70=getRndDouble();double  p71=getRndDouble();double  p72=getRndDouble();
        double  p73=getRndDouble();double  p74=getRndDouble();float  p75=getRndFloat();
        int  p76=getRndInt();float  p77=getRndFloat();double  p78=getRndDouble();
        double  p79=getRndDouble();double  p80=getRndDouble();double  p81=getRndDouble();
        byte  p82=getRndByte();float  p83=getRndFloat();float  p84=getRndFloat();
        float  p85=getRndFloat();float  p86=getRndFloat();double  p87=getRndDouble();
        float  p88=getRndFloat();float  p89=getRndFloat();float  p90=getRndFloat();
        float  p91=getRndFloat();float  p92=getRndFloat();double  p93=getRndDouble();
        float  p94=getRndFloat();double  p95=getRndDouble();float  p96=getRndFloat();
        double  p97=getRndDouble();float  p98=getRndFloat();float  p99=getRndFloat();
        double  p100=getRndDouble();double  p101=getRndDouble();float  p102=getRndFloat();
        float  p103=getRndFloat();double  p104=getRndDouble();double  p105=getRndDouble();
        float  p106=getRndFloat();byte  p107=getRndByte();double  p108=getRndDouble();
        float  p109=getRndFloat();float  p110=getRndFloat();double  p111=getRndDouble();
        double  p112=getRndDouble();double  p113=getRndDouble();double  p114=getRndDouble();
        double  p115=getRndDouble();double  p116=getRndDouble();float  p117=getRndFloat();
        double  p118=getRndDouble();float  p119=getRndFloat();float  p120=getRndFloat();
        float  p121=getRndFloat();float  p122=getRndFloat();float  p123=getRndFloat();
        double  p124=getRndDouble();float  p125=getRndFloat();float  p126=getRndFloat();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%d\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%d\n",p14);
        ps.format("p15=%d\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);
        ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%d\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%d\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc39(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc40(double p0,double p1,float p2,double p3,double p4,float p5,double p6,double p7
        ,double p8,double p9,double p10,double p11,double p12,float p13,float p14
        ,double p15,float p16,float p17,double p18,float p19,double p20,double p21
        ,double p22,double p23,double p24,double p25,double p26,float p27,float p28
        ,double p29,double p30,double p31,float p32,float p33,float p34,float p35
        ,double p36,float p37,float p38,float p39,double p40,float p41,double p42
        ,double p43,float p44,double p45,double p46,double p47,float p48,double p49
        ,double p50,double p51,double p52,float p53,double p54,double p55,double p56
        ,double p57,double p58,double p59,double p60,double p61,double p62,float p63
        ,float p64,float p65,float p66,float p67,double p68,double p69,double p70
        ,double p71,double p72,double p73,float p74,double p75,double p76,double p77
        ,double p78,double p79,float p80,double p81,double p82,float p83,double p84
        ,double p85,double p86,float p87,float p88,double p89,float p90,double p91
        ,float p92,float p93,float p94,float p95,double p96,float p97,float p98
        ,double p99,double p100,double p101,double p102,float p103,double p104,double p105
        ,double p106,double p107,double p108,double p109,float p110,float p111,double p112
        ,double p113,double p114,double p115,double p116,double p117,double p118
        ,double p119,float p120,double p121,double p122,double p123,double p124
        ,double p125,float p126    );
    private static void nativeFnc40_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();float  p2=getRndFloat();
        double  p3=getRndDouble();double  p4=getRndDouble();float  p5=getRndFloat();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();float  p13=getRndFloat();float  p14=getRndFloat();
        double  p15=getRndDouble();float  p16=getRndFloat();float  p17=getRndFloat();
        double  p18=getRndDouble();float  p19=getRndFloat();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        float  p27=getRndFloat();float  p28=getRndFloat();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();float  p32=getRndFloat();
        float  p33=getRndFloat();float  p34=getRndFloat();float  p35=getRndFloat();
        double  p36=getRndDouble();float  p37=getRndFloat();float  p38=getRndFloat();
        float  p39=getRndFloat();double  p40=getRndDouble();float  p41=getRndFloat();
        double  p42=getRndDouble();double  p43=getRndDouble();float  p44=getRndFloat();
        double  p45=getRndDouble();double  p46=getRndDouble();double  p47=getRndDouble();
        float  p48=getRndFloat();double  p49=getRndDouble();double  p50=getRndDouble();
        double  p51=getRndDouble();double  p52=getRndDouble();float  p53=getRndFloat();
        double  p54=getRndDouble();double  p55=getRndDouble();double  p56=getRndDouble();
        double  p57=getRndDouble();double  p58=getRndDouble();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        float  p66=getRndFloat();float  p67=getRndFloat();double  p68=getRndDouble();
        double  p69=getRndDouble();double  p70=getRndDouble();double  p71=getRndDouble();
        double  p72=getRndDouble();double  p73=getRndDouble();float  p74=getRndFloat();
        double  p75=getRndDouble();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();float  p80=getRndFloat();
        double  p81=getRndDouble();double  p82=getRndDouble();float  p83=getRndFloat();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        float  p87=getRndFloat();float  p88=getRndFloat();double  p89=getRndDouble();
        float  p90=getRndFloat();double  p91=getRndDouble();float  p92=getRndFloat();
        float  p93=getRndFloat();float  p94=getRndFloat();float  p95=getRndFloat();
        double  p96=getRndDouble();float  p97=getRndFloat();float  p98=getRndFloat();
        double  p99=getRndDouble();double  p100=getRndDouble();double  p101=getRndDouble();
        double  p102=getRndDouble();float  p103=getRndFloat();double  p104=getRndDouble();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();double  p109=getRndDouble();float  p110=getRndFloat();
        float  p111=getRndFloat();double  p112=getRndDouble();double  p113=getRndDouble();
        double  p114=getRndDouble();double  p115=getRndDouble();double  p116=getRndDouble();
        double  p117=getRndDouble();double  p118=getRndDouble();double  p119=getRndDouble();
        float  p120=getRndFloat();double  p121=getRndDouble();double  p122=getRndDouble();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc40(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc41(float p0,double p1,int p2,double p3,int p4,float p5,float p6,float p7,int p8
        ,int p9,float p10,double p11,double p12,double p13,int p14,double p15,double p16
        ,float p17,double p18,double p19,double p20,int p21,double p22,float p23
        ,double p24,float p25,double p26,double p27,int p28,double p29,double p30
        ,int p31,double p32,double p33,float p34,int p35,double p36,int p37,int p38
        ,double p39,float p40,int p41,double p42,double p43,double p44,double p45
        ,int p46,double p47,float p48,float p49,float p50,double p51,double p52
        ,float p53,float p54,double p55,float p56,float p57,double p58,float p59
        ,double p60,float p61,double p62,float p63,double p64,double p65,double p66
        ,double p67,double p68,double p69,float p70,float p71,double p72,float p73
        ,double p74,float p75,int p76,float p77,double p78,int p79,float p80,float p81
        ,double p82,double p83,double p84,double p85,double p86,float p87,float p88
        ,float p89,double p90,int p91,float p92,double p93,float p94,float p95,float p96
        ,float p97,float p98,double p99,float p100,int p101,double p102,float p103
        ,double p104,double p105,float p106,float p107,double p108,float p109,float p110
        ,double p111,float p112,double p113,float p114,float p115,float p116,double p117
        ,int p118,float p119,int p120,double p121,double p122,int p123,double p124
        ,float p125,float p126    );
    private static void nativeFnc41_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();double  p1=getRndDouble();int  p2=getRndInt();
        double  p3=getRndDouble();int  p4=getRndInt();float  p5=getRndFloat();float  p6=getRndFloat();
        float  p7=getRndFloat();int  p8=getRndInt();int  p9=getRndInt();float  p10=getRndFloat();
        double  p11=getRndDouble();double  p12=getRndDouble();double  p13=getRndDouble();
        int  p14=getRndInt();double  p15=getRndDouble();double  p16=getRndDouble();
        float  p17=getRndFloat();double  p18=getRndDouble();double  p19=getRndDouble();
        double  p20=getRndDouble();int  p21=getRndInt();double  p22=getRndDouble();
        float  p23=getRndFloat();double  p24=getRndDouble();float  p25=getRndFloat();
        double  p26=getRndDouble();double  p27=getRndDouble();int  p28=getRndInt();
        double  p29=getRndDouble();double  p30=getRndDouble();int  p31=getRndInt();
        double  p32=getRndDouble();double  p33=getRndDouble();float  p34=getRndFloat();
        int  p35=getRndInt();double  p36=getRndDouble();int  p37=getRndInt();int  p38=getRndInt();
        double  p39=getRndDouble();float  p40=getRndFloat();int  p41=getRndInt();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();int  p46=getRndInt();double  p47=getRndDouble();
        float  p48=getRndFloat();float  p49=getRndFloat();float  p50=getRndFloat();
        double  p51=getRndDouble();double  p52=getRndDouble();float  p53=getRndFloat();
        float  p54=getRndFloat();double  p55=getRndDouble();float  p56=getRndFloat();
        float  p57=getRndFloat();double  p58=getRndDouble();float  p59=getRndFloat();
        double  p60=getRndDouble();float  p61=getRndFloat();double  p62=getRndDouble();
        float  p63=getRndFloat();double  p64=getRndDouble();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();float  p70=getRndFloat();float  p71=getRndFloat();
        double  p72=getRndDouble();float  p73=getRndFloat();double  p74=getRndDouble();
        float  p75=getRndFloat();int  p76=getRndInt();float  p77=getRndFloat();
        double  p78=getRndDouble();int  p79=getRndInt();float  p80=getRndFloat();
        float  p81=getRndFloat();double  p82=getRndDouble();double  p83=getRndDouble();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        float  p87=getRndFloat();float  p88=getRndFloat();float  p89=getRndFloat();
        double  p90=getRndDouble();int  p91=getRndInt();float  p92=getRndFloat();
        double  p93=getRndDouble();float  p94=getRndFloat();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        double  p99=getRndDouble();float  p100=getRndFloat();int  p101=getRndInt();
        double  p102=getRndDouble();float  p103=getRndFloat();double  p104=getRndDouble();
        double  p105=getRndDouble();float  p106=getRndFloat();float  p107=getRndFloat();
        double  p108=getRndDouble();float  p109=getRndFloat();float  p110=getRndFloat();
        double  p111=getRndDouble();float  p112=getRndFloat();double  p113=getRndDouble();
        float  p114=getRndFloat();float  p115=getRndFloat();float  p116=getRndFloat();
        double  p117=getRndDouble();int  p118=getRndInt();float  p119=getRndFloat();
        int  p120=getRndInt();double  p121=getRndDouble();double  p122=getRndDouble();
        int  p123=getRndInt();double  p124=getRndDouble();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%d\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%d\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%d\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%d\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%d\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%d\n",p37);
        ps.format("p38=%d\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%d\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%d\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc41(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc42(float p0,float p1,double p2,double p3,float p4,double p5,double p6,float p7
        ,double p8,double p9,float p10,double p11,byte p12,float p13,float p14,float p15
        ,double p16,double p17,float p18,double p19,double p20,double p21,float p22
        ,float p23,double p24,float p25,double p26,float p27,float p28,float p29
        ,float p30,float p31,double p32,float p33,byte p34,float p35,double p36
        ,float p37,double p38,float p39,double p40,byte p41,byte p42,double p43
        ,float p44,double p45,float p46,float p47,double p48,double p49,float p50
        ,double p51,double p52,double p53,double p54,float p55,double p56,float p57
        ,byte p58,double p59,float p60,double p61,double p62,double p63,float p64
        ,float p65,double p66,double p67,byte p68,float p69,float p70,double p71
        ,float p72,float p73,double p74,double p75,float p76,float p77,float p78
        ,double p79,double p80,double p81,byte p82,float p83,double p84,double p85
        ,double p86,double p87,double p88,double p89,float p90,double p91,float p92
        ,byte p93,float p94,double p95,double p96,float p97,double p98,float p99
        ,float p100,float p101,double p102,double p103,double p104,float p105,float p106
        ,float p107,float p108,double p109,float p110,double p111,double p112,double p113
        ,float p114,double p115,float p116,byte p117,double p118,float p119,double p120
        ,float p121,byte p122,float p123,double p124,float p125,float p126    );
    private static void nativeFnc42_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();double  p2=getRndDouble();
        double  p3=getRndDouble();float  p4=getRndFloat();double  p5=getRndDouble();
        double  p6=getRndDouble();float  p7=getRndFloat();double  p8=getRndDouble();
        double  p9=getRndDouble();float  p10=getRndFloat();double  p11=getRndDouble();
        byte  p12=getRndByte();float  p13=getRndFloat();float  p14=getRndFloat();
        float  p15=getRndFloat();double  p16=getRndDouble();double  p17=getRndDouble();
        float  p18=getRndFloat();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();float  p22=getRndFloat();float  p23=getRndFloat();
        double  p24=getRndDouble();float  p25=getRndFloat();double  p26=getRndDouble();
        float  p27=getRndFloat();float  p28=getRndFloat();float  p29=getRndFloat();
        float  p30=getRndFloat();float  p31=getRndFloat();double  p32=getRndDouble();
        float  p33=getRndFloat();byte  p34=getRndByte();float  p35=getRndFloat();
        double  p36=getRndDouble();float  p37=getRndFloat();double  p38=getRndDouble();
        float  p39=getRndFloat();double  p40=getRndDouble();byte  p41=getRndByte();
        byte  p42=getRndByte();double  p43=getRndDouble();float  p44=getRndFloat();
        double  p45=getRndDouble();float  p46=getRndFloat();float  p47=getRndFloat();
        double  p48=getRndDouble();double  p49=getRndDouble();float  p50=getRndFloat();
        double  p51=getRndDouble();double  p52=getRndDouble();double  p53=getRndDouble();
        double  p54=getRndDouble();float  p55=getRndFloat();double  p56=getRndDouble();
        float  p57=getRndFloat();byte  p58=getRndByte();double  p59=getRndDouble();
        float  p60=getRndFloat();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();float  p64=getRndFloat();float  p65=getRndFloat();
        double  p66=getRndDouble();double  p67=getRndDouble();byte  p68=getRndByte();
        float  p69=getRndFloat();float  p70=getRndFloat();double  p71=getRndDouble();
        float  p72=getRndFloat();float  p73=getRndFloat();double  p74=getRndDouble();
        double  p75=getRndDouble();float  p76=getRndFloat();float  p77=getRndFloat();
        float  p78=getRndFloat();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();byte  p82=getRndByte();float  p83=getRndFloat();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        double  p87=getRndDouble();double  p88=getRndDouble();double  p89=getRndDouble();
        float  p90=getRndFloat();double  p91=getRndDouble();float  p92=getRndFloat();
        byte  p93=getRndByte();float  p94=getRndFloat();double  p95=getRndDouble();
        double  p96=getRndDouble();float  p97=getRndFloat();double  p98=getRndDouble();
        float  p99=getRndFloat();float  p100=getRndFloat();float  p101=getRndFloat();
        double  p102=getRndDouble();double  p103=getRndDouble();double  p104=getRndDouble();
        float  p105=getRndFloat();float  p106=getRndFloat();float  p107=getRndFloat();
        float  p108=getRndFloat();double  p109=getRndDouble();float  p110=getRndFloat();
        double  p111=getRndDouble();double  p112=getRndDouble();double  p113=getRndDouble();
        float  p114=getRndFloat();double  p115=getRndDouble();float  p116=getRndFloat();
        byte  p117=getRndByte();double  p118=getRndDouble();float  p119=getRndFloat();
        double  p120=getRndDouble();float  p121=getRndFloat();byte  p122=getRndByte();
        float  p123=getRndFloat();double  p124=getRndDouble();float  p125=getRndFloat();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%d\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%d\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc42(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc43(float p0,int p1,float p2,float p3,float p4,float p5,float p6,int p7,float p8
        ,float p9,float p10,float p11,float p12,float p13,int p14,float p15,float p16
        ,float p17,float p18,float p19,float p20,float p21,float p22,float p23,float p24
        ,float p25,float p26,float p27,float p28,float p29,float p30,float p31,int p32
        ,float p33,float p34,float p35,float p36,float p37,float p38,float p39,float p40
        ,float p41,int p42,float p43,float p44,float p45,float p46,float p47,float p48
        ,int p49,int p50,int p51,float p52,float p53,float p54,float p55,float p56
        ,float p57,float p58,float p59,float p60,int p61,float p62,int p63,float p64
        ,float p65,float p66,float p67,float p68,float p69,float p70,float p71,float p72
        ,float p73,float p74,float p75,float p76,float p77,float p78,int p79,int p80
        ,int p81,float p82,float p83,float p84,float p85,float p86,float p87,float p88
        ,float p89,float p90,float p91,float p92,float p93,float p94,float p95,float p96
        ,float p97,float p98,float p99,float p100,float p101,float p102,float p103
        ,float p104,float p105,float p106,float p107,float p108,float p109,float p110
        ,int p111,float p112,float p113,float p114,float p115,float p116,float p117
        ,float p118,float p119,float p120,float p121,float p122,float p123,float p124
        ,int p125,float p126    );
    private static void nativeFnc43_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();int  p1=getRndInt();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();float  p5=getRndFloat();
        float  p6=getRndFloat();int  p7=getRndInt();float  p8=getRndFloat();float  p9=getRndFloat();
        float  p10=getRndFloat();float  p11=getRndFloat();float  p12=getRndFloat();
        float  p13=getRndFloat();int  p14=getRndInt();float  p15=getRndFloat();
        float  p16=getRndFloat();float  p17=getRndFloat();float  p18=getRndFloat();
        float  p19=getRndFloat();float  p20=getRndFloat();float  p21=getRndFloat();
        float  p22=getRndFloat();float  p23=getRndFloat();float  p24=getRndFloat();
        float  p25=getRndFloat();float  p26=getRndFloat();float  p27=getRndFloat();
        float  p28=getRndFloat();float  p29=getRndFloat();float  p30=getRndFloat();
        float  p31=getRndFloat();int  p32=getRndInt();float  p33=getRndFloat();
        float  p34=getRndFloat();float  p35=getRndFloat();float  p36=getRndFloat();
        float  p37=getRndFloat();float  p38=getRndFloat();float  p39=getRndFloat();
        float  p40=getRndFloat();float  p41=getRndFloat();int  p42=getRndInt();
        float  p43=getRndFloat();float  p44=getRndFloat();float  p45=getRndFloat();
        float  p46=getRndFloat();float  p47=getRndFloat();float  p48=getRndFloat();
        int  p49=getRndInt();int  p50=getRndInt();int  p51=getRndInt();float  p52=getRndFloat();
        float  p53=getRndFloat();float  p54=getRndFloat();float  p55=getRndFloat();
        float  p56=getRndFloat();float  p57=getRndFloat();float  p58=getRndFloat();
        float  p59=getRndFloat();float  p60=getRndFloat();int  p61=getRndInt();
        float  p62=getRndFloat();int  p63=getRndInt();float  p64=getRndFloat();
        float  p65=getRndFloat();float  p66=getRndFloat();float  p67=getRndFloat();
        float  p68=getRndFloat();float  p69=getRndFloat();float  p70=getRndFloat();
        float  p71=getRndFloat();float  p72=getRndFloat();float  p73=getRndFloat();
        float  p74=getRndFloat();float  p75=getRndFloat();float  p76=getRndFloat();
        float  p77=getRndFloat();float  p78=getRndFloat();int  p79=getRndInt();
        int  p80=getRndInt();int  p81=getRndInt();float  p82=getRndFloat();float  p83=getRndFloat();
        float  p84=getRndFloat();float  p85=getRndFloat();float  p86=getRndFloat();
        float  p87=getRndFloat();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        float  p93=getRndFloat();float  p94=getRndFloat();float  p95=getRndFloat();
        float  p96=getRndFloat();float  p97=getRndFloat();float  p98=getRndFloat();
        float  p99=getRndFloat();float  p100=getRndFloat();float  p101=getRndFloat();
        float  p102=getRndFloat();float  p103=getRndFloat();float  p104=getRndFloat();
        float  p105=getRndFloat();float  p106=getRndFloat();float  p107=getRndFloat();
        float  p108=getRndFloat();float  p109=getRndFloat();float  p110=getRndFloat();
        int  p111=getRndInt();float  p112=getRndFloat();float  p113=getRndFloat();
        float  p114=getRndFloat();float  p115=getRndFloat();float  p116=getRndFloat();
        float  p117=getRndFloat();float  p118=getRndFloat();float  p119=getRndFloat();
        float  p120=getRndFloat();float  p121=getRndFloat();float  p122=getRndFloat();
        float  p123=getRndFloat();float  p124=getRndFloat();int  p125=getRndInt();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%d\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%d\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%d\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%d\n",p49);
        ps.format("p50=%d\n",p50);ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%d\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%d\n",p79);
        ps.format("p80=%d\n",p80);ps.format("p81=%d\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%d\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%d\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc43(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc44(float p0,float p1,float p2,float p3,float p4,byte p5,float p6,float p7,float p8
        ,float p9,float p10,float p11,float p12,float p13,float p14,byte p15,float p16
        ,float p17,float p18,byte p19,float p20,float p21,float p22,float p23,float p24
        ,byte p25,float p26,float p27,float p28,float p29,float p30,float p31,byte p32
        ,byte p33,float p34,float p35,float p36,float p37,byte p38,float p39,float p40
        ,byte p41,float p42,float p43,float p44,float p45,float p46,float p47,byte p48
        ,float p49,byte p50,float p51,float p52,float p53,float p54,float p55,float p56
        ,float p57,float p58,byte p59,float p60,float p61,float p62,float p63,float p64
        ,float p65,byte p66,float p67,float p68,float p69,float p70,byte p71,float p72
        ,byte p73,byte p74,float p75,float p76,byte p77,byte p78,float p79,float p80
        ,float p81,float p82,float p83,float p84,float p85,float p86,float p87,float p88
        ,float p89,float p90,float p91,float p92,byte p93,float p94,float p95,float p96
        ,byte p97,float p98,float p99,float p100,byte p101,float p102,float p103
        ,float p104,float p105,float p106,float p107,byte p108,float p109,float p110
        ,float p111,float p112,byte p113,float p114,float p115,float p116,byte p117
        ,float p118,float p119,float p120,byte p121,float p122,float p123,float p124
        ,float p125,float p126    );
    private static void nativeFnc44_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();byte  p5=getRndByte();float  p6=getRndFloat();
        float  p7=getRndFloat();float  p8=getRndFloat();float  p9=getRndFloat();
        float  p10=getRndFloat();float  p11=getRndFloat();float  p12=getRndFloat();
        float  p13=getRndFloat();float  p14=getRndFloat();byte  p15=getRndByte();
        float  p16=getRndFloat();float  p17=getRndFloat();float  p18=getRndFloat();
        byte  p19=getRndByte();float  p20=getRndFloat();float  p21=getRndFloat();
        float  p22=getRndFloat();float  p23=getRndFloat();float  p24=getRndFloat();
        byte  p25=getRndByte();float  p26=getRndFloat();float  p27=getRndFloat();
        float  p28=getRndFloat();float  p29=getRndFloat();float  p30=getRndFloat();
        float  p31=getRndFloat();byte  p32=getRndByte();byte  p33=getRndByte();
        float  p34=getRndFloat();float  p35=getRndFloat();float  p36=getRndFloat();
        float  p37=getRndFloat();byte  p38=getRndByte();float  p39=getRndFloat();
        float  p40=getRndFloat();byte  p41=getRndByte();float  p42=getRndFloat();
        float  p43=getRndFloat();float  p44=getRndFloat();float  p45=getRndFloat();
        float  p46=getRndFloat();float  p47=getRndFloat();byte  p48=getRndByte();
        float  p49=getRndFloat();byte  p50=getRndByte();float  p51=getRndFloat();
        float  p52=getRndFloat();float  p53=getRndFloat();float  p54=getRndFloat();
        float  p55=getRndFloat();float  p56=getRndFloat();float  p57=getRndFloat();
        float  p58=getRndFloat();byte  p59=getRndByte();float  p60=getRndFloat();
        float  p61=getRndFloat();float  p62=getRndFloat();float  p63=getRndFloat();
        float  p64=getRndFloat();float  p65=getRndFloat();byte  p66=getRndByte();
        float  p67=getRndFloat();float  p68=getRndFloat();float  p69=getRndFloat();
        float  p70=getRndFloat();byte  p71=getRndByte();float  p72=getRndFloat();
        byte  p73=getRndByte();byte  p74=getRndByte();float  p75=getRndFloat();
        float  p76=getRndFloat();byte  p77=getRndByte();byte  p78=getRndByte();
        float  p79=getRndFloat();float  p80=getRndFloat();float  p81=getRndFloat();
        float  p82=getRndFloat();float  p83=getRndFloat();float  p84=getRndFloat();
        float  p85=getRndFloat();float  p86=getRndFloat();float  p87=getRndFloat();
        float  p88=getRndFloat();float  p89=getRndFloat();float  p90=getRndFloat();
        float  p91=getRndFloat();float  p92=getRndFloat();byte  p93=getRndByte();
        float  p94=getRndFloat();float  p95=getRndFloat();float  p96=getRndFloat();
        byte  p97=getRndByte();float  p98=getRndFloat();float  p99=getRndFloat();
        float  p100=getRndFloat();byte  p101=getRndByte();float  p102=getRndFloat();
        float  p103=getRndFloat();float  p104=getRndFloat();float  p105=getRndFloat();
        float  p106=getRndFloat();float  p107=getRndFloat();byte  p108=getRndByte();
        float  p109=getRndFloat();float  p110=getRndFloat();float  p111=getRndFloat();
        float  p112=getRndFloat();byte  p113=getRndByte();float  p114=getRndFloat();
        float  p115=getRndFloat();float  p116=getRndFloat();byte  p117=getRndByte();
        float  p118=getRndFloat();float  p119=getRndFloat();float  p120=getRndFloat();
        byte  p121=getRndByte();float  p122=getRndFloat();float  p123=getRndFloat();
        float  p124=getRndFloat();float  p125=getRndFloat();float  p126=getRndFloat();

        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%d\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%d\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%d\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%d\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%d\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%d\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%d\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc44(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc45(byte p0,byte p1,float p2,float p3,float p4,byte p5,int p6,float p7,float p8
        ,byte p9,float p10,byte p11,byte p12,float p13,float p14,float p15,float p16
        ,byte p17,float p18,int p19,int p20,float p21,float p22,float p23,byte p24
        ,byte p25,float p26,float p27,byte p28,float p29,float p30,float p31,float p32
        ,float p33,float p34,int p35,float p36,int p37,float p38,float p39,float p40
        ,float p41,float p42,byte p43,float p44,int p45,byte p46,float p47,float p48
        ,float p49,byte p50,float p51,byte p52,float p53,float p54,float p55,float p56
        ,float p57,float p58,float p59,int p60,int p61,float p62,byte p63,float p64
        ,int p65,float p66,float p67,float p68,float p69,float p70,float p71,float p72
        ,float p73,float p74,int p75,float p76,float p77,byte p78,float p79,float p80
        ,float p81,float p82,byte p83,int p84,byte p85,float p86,int p87,float p88
        ,float p89,float p90,float p91,float p92,int p93,float p94,float p95,int p96
        ,float p97,byte p98,float p99,byte p100,float p101,int p102,float p103,float p104
        ,int p105,float p106,float p107,int p108,float p109,float p110,float p111
        ,float p112,int p113,float p114,float p115,float p116,float p117,float p118
        ,int p119,float p120,byte p121,float p122,float p123,float p124,byte p125
        ,float p126    );
    private static void nativeFnc45_invoke(PrintStream ps)
    {
        byte  p0=getRndByte();byte  p1=getRndByte();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();byte  p5=getRndByte();int  p6=getRndInt();
        float  p7=getRndFloat();float  p8=getRndFloat();byte  p9=getRndByte();float  p10=getRndFloat();
        byte  p11=getRndByte();byte  p12=getRndByte();float  p13=getRndFloat();
        float  p14=getRndFloat();float  p15=getRndFloat();float  p16=getRndFloat();
        byte  p17=getRndByte();float  p18=getRndFloat();int  p19=getRndInt();int  p20=getRndInt();
        float  p21=getRndFloat();float  p22=getRndFloat();float  p23=getRndFloat();
        byte  p24=getRndByte();byte  p25=getRndByte();float  p26=getRndFloat();
        float  p27=getRndFloat();byte  p28=getRndByte();float  p29=getRndFloat();
        float  p30=getRndFloat();float  p31=getRndFloat();float  p32=getRndFloat();
        float  p33=getRndFloat();float  p34=getRndFloat();int  p35=getRndInt();
        float  p36=getRndFloat();int  p37=getRndInt();float  p38=getRndFloat();
        float  p39=getRndFloat();float  p40=getRndFloat();float  p41=getRndFloat();
        float  p42=getRndFloat();byte  p43=getRndByte();float  p44=getRndFloat();
        int  p45=getRndInt();byte  p46=getRndByte();float  p47=getRndFloat();float  p48=getRndFloat();
        float  p49=getRndFloat();byte  p50=getRndByte();float  p51=getRndFloat();
        byte  p52=getRndByte();float  p53=getRndFloat();float  p54=getRndFloat();
        float  p55=getRndFloat();float  p56=getRndFloat();float  p57=getRndFloat();
        float  p58=getRndFloat();float  p59=getRndFloat();int  p60=getRndInt();
        int  p61=getRndInt();float  p62=getRndFloat();byte  p63=getRndByte();float  p64=getRndFloat();
        int  p65=getRndInt();float  p66=getRndFloat();float  p67=getRndFloat();
        float  p68=getRndFloat();float  p69=getRndFloat();float  p70=getRndFloat();
        float  p71=getRndFloat();float  p72=getRndFloat();float  p73=getRndFloat();
        float  p74=getRndFloat();int  p75=getRndInt();float  p76=getRndFloat();
        float  p77=getRndFloat();byte  p78=getRndByte();float  p79=getRndFloat();
        float  p80=getRndFloat();float  p81=getRndFloat();float  p82=getRndFloat();
        byte  p83=getRndByte();int  p84=getRndInt();byte  p85=getRndByte();float  p86=getRndFloat();
        int  p87=getRndInt();float  p88=getRndFloat();float  p89=getRndFloat();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        int  p93=getRndInt();float  p94=getRndFloat();float  p95=getRndFloat();
        int  p96=getRndInt();float  p97=getRndFloat();byte  p98=getRndByte();float  p99=getRndFloat();
        byte  p100=getRndByte();float  p101=getRndFloat();int  p102=getRndInt();
        float  p103=getRndFloat();float  p104=getRndFloat();int  p105=getRndInt();
        float  p106=getRndFloat();float  p107=getRndFloat();int  p108=getRndInt();
        float  p109=getRndFloat();float  p110=getRndFloat();float  p111=getRndFloat();
        float  p112=getRndFloat();int  p113=getRndInt();float  p114=getRndFloat();
        float  p115=getRndFloat();float  p116=getRndFloat();float  p117=getRndFloat();
        float  p118=getRndFloat();int  p119=getRndInt();float  p120=getRndFloat();
        byte  p121=getRndByte();float  p122=getRndFloat();float  p123=getRndFloat();
        float  p124=getRndFloat();byte  p125=getRndByte();float  p126=getRndFloat();

        ps.format("p0=%d\n",p0);ps.format("p1=%d\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%d\n",p5);
        ps.format("p6=%d\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%d\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%d\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%d\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%d\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%d\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%d\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%d\n",p45);ps.format("p46=%d\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%d\n",p60);ps.format("p61=%d\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%d\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%d\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%d\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);ps.format("p83=%d\n",p83);
        ps.format("p84=%d\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%d\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%d\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%d\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%d\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%d\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%d\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc45(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc46(int p0,double p1,double p2,double p3,double p4,double p5,double p6,double p7
        ,double p8,double p9,double p10,int p11,double p12,double p13,double p14
        ,double p15,int p16,double p17,double p18,double p19,double p20,double p21
        ,double p22,double p23,double p24,double p25,double p26,double p27,int p28
        ,double p29,double p30,double p31,double p32,double p33,double p34,double p35
        ,int p36,int p37,double p38,int p39,double p40,double p41,double p42,double p43
        ,double p44,double p45,double p46,double p47,double p48,double p49,double p50
        ,double p51,int p52,double p53,double p54,double p55,double p56,double p57
        ,double p58,double p59,double p60,double p61,double p62,double p63,int p64
        ,double p65,double p66,int p67,int p68,double p69,double p70,double p71
        ,double p72,double p73,double p74,double p75,double p76,int p77,int p78
        ,double p79,double p80,double p81,int p82,double p83,double p84,int p85
        ,double p86,double p87,double p88,int p89,double p90,double p91,double p92
        ,int p93,double p94,int p95,double p96,double p97,double p98,double p99
        ,double p100,double p101,int p102,double p103,double p104,double p105,double p106
        ,double p107,double p108,double p109,double p110,double p111,double p112
        ,double p113,double p114,double p115,int p116,int p117,double p118,double p119
        ,double p120,int p121,double p122,int p123,int p124,double p125,double p126
            );
    private static void nativeFnc46_invoke(PrintStream ps)
    {
        int  p0=getRndInt();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        double  p9=getRndDouble();double  p10=getRndDouble();int  p11=getRndInt();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();int  p16=getRndInt();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        double  p27=getRndDouble();int  p28=getRndInt();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();double  p34=getRndDouble();double  p35=getRndDouble();
        int  p36=getRndInt();int  p37=getRndInt();double  p38=getRndDouble();int  p39=getRndInt();
        double  p40=getRndDouble();double  p41=getRndDouble();double  p42=getRndDouble();
        double  p43=getRndDouble();double  p44=getRndDouble();double  p45=getRndDouble();
        double  p46=getRndDouble();double  p47=getRndDouble();double  p48=getRndDouble();
        double  p49=getRndDouble();double  p50=getRndDouble();double  p51=getRndDouble();
        int  p52=getRndInt();double  p53=getRndDouble();double  p54=getRndDouble();
        double  p55=getRndDouble();double  p56=getRndDouble();double  p57=getRndDouble();
        double  p58=getRndDouble();double  p59=getRndDouble();double  p60=getRndDouble();
        double  p61=getRndDouble();double  p62=getRndDouble();double  p63=getRndDouble();
        int  p64=getRndInt();double  p65=getRndDouble();double  p66=getRndDouble();
        int  p67=getRndInt();int  p68=getRndInt();double  p69=getRndDouble();double  p70=getRndDouble();
        double  p71=getRndDouble();double  p72=getRndDouble();double  p73=getRndDouble();
        double  p74=getRndDouble();double  p75=getRndDouble();double  p76=getRndDouble();
        int  p77=getRndInt();int  p78=getRndInt();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();int  p82=getRndInt();double  p83=getRndDouble();
        double  p84=getRndDouble();int  p85=getRndInt();double  p86=getRndDouble();
        double  p87=getRndDouble();double  p88=getRndDouble();int  p89=getRndInt();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        int  p93=getRndInt();double  p94=getRndDouble();int  p95=getRndInt();double  p96=getRndDouble();
        double  p97=getRndDouble();double  p98=getRndDouble();double  p99=getRndDouble();
        double  p100=getRndDouble();double  p101=getRndDouble();int  p102=getRndInt();
        double  p103=getRndDouble();double  p104=getRndDouble();double  p105=getRndDouble();
        double  p106=getRndDouble();double  p107=getRndDouble();double  p108=getRndDouble();
        double  p109=getRndDouble();double  p110=getRndDouble();double  p111=getRndDouble();
        double  p112=getRndDouble();double  p113=getRndDouble();double  p114=getRndDouble();
        double  p115=getRndDouble();int  p116=getRndInt();int  p117=getRndInt();
        double  p118=getRndDouble();double  p119=getRndDouble();double  p120=getRndDouble();
        int  p121=getRndInt();double  p122=getRndDouble();int  p123=getRndInt();
        int  p124=getRndInt();double  p125=getRndDouble();double  p126=getRndDouble();

        ps.format("p0=%d\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);ps.format("p11=%d\n",p11);
        ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%d\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%e\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%e\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%d\n",p36);ps.format("p37=%d\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%d\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);
        ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%d\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%e\n",p66);ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);ps.format("p89=%d\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%d\n",p93);ps.format("p94=%e\n",p94);ps.format("p95=%d\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);
        ps.format("p102=%d\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%d\n",p116);
        ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%e\n",p120);ps.format("p121=%d\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%d\n",p123);ps.format("p124=%d\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc46(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc47(double p0,double p1,double p2,double p3,byte p4,double p5,double p6,double p7
        ,byte p8,byte p9,double p10,double p11,double p12,double p13,double p14
        ,double p15,double p16,double p17,double p18,double p19,double p20,double p21
        ,byte p22,double p23,double p24,double p25,double p26,byte p27,double p28
        ,double p29,double p30,double p31,double p32,double p33,double p34,double p35
        ,double p36,byte p37,byte p38,double p39,double p40,double p41,double p42
        ,double p43,double p44,double p45,byte p46,double p47,double p48,double p49
        ,double p50,double p51,byte p52,double p53,double p54,double p55,double p56
        ,double p57,double p58,double p59,double p60,double p61,double p62,double p63
        ,double p64,double p65,double p66,double p67,double p68,double p69,byte p70
        ,double p71,double p72,double p73,double p74,double p75,double p76,double p77
        ,double p78,double p79,double p80,double p81,double p82,double p83,double p84
        ,double p85,double p86,double p87,double p88,double p89,double p90,double p91
        ,double p92,double p93,byte p94,double p95,double p96,double p97,double p98
        ,double p99,double p100,byte p101,double p102,double p103,byte p104,double p105
        ,double p106,double p107,double p108,double p109,double p110,double p111
        ,double p112,double p113,byte p114,double p115,double p116,double p117,double p118
        ,double p119,byte p120,double p121,byte p122,double p123,double p124,double p125
        ,double p126    );
    private static void nativeFnc47_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();byte  p4=getRndByte();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();byte  p8=getRndByte();
        byte  p9=getRndByte();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();byte  p22=getRndByte();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();double  p26=getRndDouble();
        byte  p27=getRndByte();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();double  p32=getRndDouble();
        double  p33=getRndDouble();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();byte  p37=getRndByte();byte  p38=getRndByte();
        double  p39=getRndDouble();double  p40=getRndDouble();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();byte  p46=getRndByte();double  p47=getRndDouble();
        double  p48=getRndDouble();double  p49=getRndDouble();double  p50=getRndDouble();
        double  p51=getRndDouble();byte  p52=getRndByte();double  p53=getRndDouble();
        double  p54=getRndDouble();double  p55=getRndDouble();double  p56=getRndDouble();
        double  p57=getRndDouble();double  p58=getRndDouble();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();double  p64=getRndDouble();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();byte  p70=getRndByte();double  p71=getRndDouble();
        double  p72=getRndDouble();double  p73=getRndDouble();double  p74=getRndDouble();
        double  p75=getRndDouble();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();double  p82=getRndDouble();double  p83=getRndDouble();
        double  p84=getRndDouble();double  p85=getRndDouble();double  p86=getRndDouble();
        double  p87=getRndDouble();double  p88=getRndDouble();double  p89=getRndDouble();
        double  p90=getRndDouble();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();byte  p94=getRndByte();double  p95=getRndDouble();
        double  p96=getRndDouble();double  p97=getRndDouble();double  p98=getRndDouble();
        double  p99=getRndDouble();double  p100=getRndDouble();byte  p101=getRndByte();
        double  p102=getRndDouble();double  p103=getRndDouble();byte  p104=getRndByte();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();double  p109=getRndDouble();double  p110=getRndDouble();
        double  p111=getRndDouble();double  p112=getRndDouble();double  p113=getRndDouble();
        byte  p114=getRndByte();double  p115=getRndDouble();double  p116=getRndDouble();
        double  p117=getRndDouble();double  p118=getRndDouble();double  p119=getRndDouble();
        byte  p120=getRndByte();double  p121=getRndDouble();byte  p122=getRndByte();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%d\n",p8);ps.format("p9=%d\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%d\n",p37);
        ps.format("p38=%d\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%d\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%d\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%d\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%d\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%d\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc47(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc48(int p0,double p1,double p2,double p3,byte p4,double p5,double p6,double p7
        ,double p8,byte p9,int p10,double p11,byte p12,int p13,double p14,double p15
        ,double p16,double p17,double p18,int p19,double p20,double p21,int p22
        ,double p23,double p24,int p25,double p26,int p27,byte p28,byte p29,double p30
        ,double p31,double p32,byte p33,double p34,double p35,double p36,double p37
        ,double p38,double p39,double p40,double p41,byte p42,int p43,double p44
        ,byte p45,double p46,byte p47,double p48,double p49,byte p50,byte p51,double p52
        ,double p53,int p54,double p55,double p56,int p57,double p58,double p59
        ,double p60,double p61,int p62,double p63,double p64,double p65,byte p66
        ,double p67,double p68,double p69,double p70,double p71,double p72,byte p73
        ,double p74,double p75,int p76,int p77,int p78,byte p79,double p80,double p81
        ,byte p82,double p83,double p84,byte p85,double p86,int p87,int p88,double p89
        ,double p90,double p91,double p92,double p93,int p94,double p95,double p96
        ,double p97,byte p98,double p99,byte p100,byte p101,double p102,double p103
        ,double p104,int p105,double p106,double p107,double p108,byte p109,double p110
        ,double p111,double p112,double p113,double p114,double p115,double p116
        ,double p117,double p118,double p119,int p120,double p121,double p122,double p123
        ,double p124,double p125,double p126    );
    private static void nativeFnc48_invoke(PrintStream ps)
    {
        int  p0=getRndInt();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();byte  p4=getRndByte();double  p5=getRndDouble();
        double  p6=getRndDouble();double  p7=getRndDouble();double  p8=getRndDouble();
        byte  p9=getRndByte();int  p10=getRndInt();double  p11=getRndDouble();byte  p12=getRndByte();
        int  p13=getRndInt();double  p14=getRndDouble();double  p15=getRndDouble();
        double  p16=getRndDouble();double  p17=getRndDouble();double  p18=getRndDouble();
        int  p19=getRndInt();double  p20=getRndDouble();double  p21=getRndDouble();
        int  p22=getRndInt();double  p23=getRndDouble();double  p24=getRndDouble();
        int  p25=getRndInt();double  p26=getRndDouble();int  p27=getRndInt();byte  p28=getRndByte();
        byte  p29=getRndByte();double  p30=getRndDouble();double  p31=getRndDouble();
        double  p32=getRndDouble();byte  p33=getRndByte();double  p34=getRndDouble();
        double  p35=getRndDouble();double  p36=getRndDouble();double  p37=getRndDouble();
        double  p38=getRndDouble();double  p39=getRndDouble();double  p40=getRndDouble();
        double  p41=getRndDouble();byte  p42=getRndByte();int  p43=getRndInt();
        double  p44=getRndDouble();byte  p45=getRndByte();double  p46=getRndDouble();
        byte  p47=getRndByte();double  p48=getRndDouble();double  p49=getRndDouble();
        byte  p50=getRndByte();byte  p51=getRndByte();double  p52=getRndDouble();
        double  p53=getRndDouble();int  p54=getRndInt();double  p55=getRndDouble();
        double  p56=getRndDouble();int  p57=getRndInt();double  p58=getRndDouble();
        double  p59=getRndDouble();double  p60=getRndDouble();double  p61=getRndDouble();
        int  p62=getRndInt();double  p63=getRndDouble();double  p64=getRndDouble();
        double  p65=getRndDouble();byte  p66=getRndByte();double  p67=getRndDouble();
        double  p68=getRndDouble();double  p69=getRndDouble();double  p70=getRndDouble();
        double  p71=getRndDouble();double  p72=getRndDouble();byte  p73=getRndByte();
        double  p74=getRndDouble();double  p75=getRndDouble();int  p76=getRndInt();
        int  p77=getRndInt();int  p78=getRndInt();byte  p79=getRndByte();double  p80=getRndDouble();
        double  p81=getRndDouble();byte  p82=getRndByte();double  p83=getRndDouble();
        double  p84=getRndDouble();byte  p85=getRndByte();double  p86=getRndDouble();
        int  p87=getRndInt();int  p88=getRndInt();double  p89=getRndDouble();double  p90=getRndDouble();
        double  p91=getRndDouble();double  p92=getRndDouble();double  p93=getRndDouble();
        int  p94=getRndInt();double  p95=getRndDouble();double  p96=getRndDouble();
        double  p97=getRndDouble();byte  p98=getRndByte();double  p99=getRndDouble();
        byte  p100=getRndByte();byte  p101=getRndByte();double  p102=getRndDouble();
        double  p103=getRndDouble();double  p104=getRndDouble();int  p105=getRndInt();
        double  p106=getRndDouble();double  p107=getRndDouble();double  p108=getRndDouble();
        byte  p109=getRndByte();double  p110=getRndDouble();double  p111=getRndDouble();
        double  p112=getRndDouble();double  p113=getRndDouble();double  p114=getRndDouble();
        double  p115=getRndDouble();double  p116=getRndDouble();double  p117=getRndDouble();
        double  p118=getRndDouble();double  p119=getRndDouble();int  p120=getRndInt();
        double  p121=getRndDouble();double  p122=getRndDouble();double  p123=getRndDouble();
        double  p124=getRndDouble();double  p125=getRndDouble();double  p126=getRndDouble();

        ps.format("p0=%d\n",p0);ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);
        ps.format("p3=%e\n",p3);ps.format("p4=%d\n",p4);ps.format("p5=%e\n",p5);
        ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);
        ps.format("p9=%d\n",p9);ps.format("p10=%d\n",p10);ps.format("p11=%e\n",p11);
        ps.format("p12=%d\n",p12);ps.format("p13=%d\n",p13);ps.format("p14=%e\n",p14);
        ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);
        ps.format("p18=%e\n",p18);ps.format("p19=%d\n",p19);ps.format("p20=%e\n",p20);
        ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);ps.format("p23=%e\n",p23);
        ps.format("p24=%e\n",p24);ps.format("p25=%d\n",p25);ps.format("p26=%e\n",p26);
        ps.format("p27=%d\n",p27);ps.format("p28=%d\n",p28);ps.format("p29=%d\n",p29);
        ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);ps.format("p32=%e\n",p32);
        ps.format("p33=%d\n",p33);ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);
        ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);
        ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);
        ps.format("p42=%d\n",p42);ps.format("p43=%d\n",p43);ps.format("p44=%e\n",p44);
        ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);ps.format("p47=%d\n",p47);
        ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);ps.format("p50=%d\n",p50);
        ps.format("p51=%d\n",p51);ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);
        ps.format("p54=%d\n",p54);ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);
        ps.format("p57=%d\n",p57);ps.format("p58=%e\n",p58);ps.format("p59=%e\n",p59);
        ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);ps.format("p62=%d\n",p62);
        ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);
        ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);ps.format("p68=%e\n",p68);
        ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);ps.format("p71=%e\n",p71);
        ps.format("p72=%e\n",p72);ps.format("p73=%d\n",p73);ps.format("p74=%e\n",p74);
        ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);ps.format("p77=%d\n",p77);
        ps.format("p78=%d\n",p78);ps.format("p79=%d\n",p79);ps.format("p80=%e\n",p80);
        ps.format("p81=%e\n",p81);ps.format("p82=%d\n",p82);ps.format("p83=%e\n",p83);
        ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);ps.format("p86=%e\n",p86);
        ps.format("p87=%d\n",p87);ps.format("p88=%d\n",p88);ps.format("p89=%e\n",p89);
        ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);
        ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);
        ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);ps.format("p98=%d\n",p98);
        ps.format("p99=%e\n",p99);ps.format("p100=%d\n",p100);ps.format("p101=%d\n",p101);
        ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);
        ps.format("p105=%d\n",p105);ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);
        ps.format("p108=%e\n",p108);ps.format("p109=%d\n",p109);ps.format("p110=%e\n",p110);
        ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);
        ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);
        ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);
        ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);
        ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);
        ps.format("p126=%e\n",p126);
        nativeFnc48(p0,p1,p2,p3,p4,p5,p6,p7
        ,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26
        ,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44
        ,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62
        ,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80
        ,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98
        ,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108,p109,p110,p111,p112,p113
        ,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc49(float p0,float p1,double p2,float p3,float p4,byte p5,double p6,int p7,double p8
        ,float p9,byte p10,float p11,double p12,float p13,double p14,double p15
        ,double p16,float p17,float p18,double p19,float p20,float p21,int p22,float p23
        ,byte p24,float p25,byte p26,double p27,double p28,double p29,double p30
        ,double p31,double p32,int p33,float p34,float p35,byte p36,float p37,float p38
        ,float p39,double p40,byte p41,int p42,float p43,double p44,float p45,double p46
        ,int p47,byte p48,double p49,float p50,double p51,float p52,float p53,float p54
        ,float p55,float p56,double p57,byte p58,float p59,float p60,float p61,float p62
        ,float p63,float p64,float p65,byte p66,double p67,byte p68,float p69,float p70
        ,double p71,double p72,double p73,int p74,double p75,float p76,float p77
        ,float p78,float p79,double p80,float p81,double p82,float p83,float p84
        ,int p85,float p86,double p87,double p88,float p89,int p90,double p91,double p92
        ,double p93,float p94,float p95,double p96,double p97,float p98,double p99
        ,float p100,float p101,float p102,float p103,double p104,float p105,double p106
        ,float p107,double p108,float p109,float p110,double p111,float p112,byte p113
        ,float p114,double p115,double p116,float p117,double p118,double p119,byte p120
        ,float p121,float p122,byte p123,double p124,double p125,float p126    );
    private static void nativeFnc49_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();double  p2=getRndDouble();
        float  p3=getRndFloat();float  p4=getRndFloat();byte  p5=getRndByte();double  p6=getRndDouble();
        int  p7=getRndInt();double  p8=getRndDouble();float  p9=getRndFloat();byte  p10=getRndByte();
        float  p11=getRndFloat();double  p12=getRndDouble();float  p13=getRndFloat();
        double  p14=getRndDouble();double  p15=getRndDouble();double  p16=getRndDouble();
        float  p17=getRndFloat();float  p18=getRndFloat();double  p19=getRndDouble();
        float  p20=getRndFloat();float  p21=getRndFloat();int  p22=getRndInt();
        float  p23=getRndFloat();byte  p24=getRndByte();float  p25=getRndFloat();
        byte  p26=getRndByte();double  p27=getRndDouble();double  p28=getRndDouble();
        double  p29=getRndDouble();double  p30=getRndDouble();double  p31=getRndDouble();
        double  p32=getRndDouble();int  p33=getRndInt();float  p34=getRndFloat();
        float  p35=getRndFloat();byte  p36=getRndByte();float  p37=getRndFloat();
        float  p38=getRndFloat();float  p39=getRndFloat();double  p40=getRndDouble();
        byte  p41=getRndByte();int  p42=getRndInt();float  p43=getRndFloat();double  p44=getRndDouble();
        float  p45=getRndFloat();double  p46=getRndDouble();int  p47=getRndInt();
        byte  p48=getRndByte();double  p49=getRndDouble();float  p50=getRndFloat();
        double  p51=getRndDouble();float  p52=getRndFloat();float  p53=getRndFloat();
        float  p54=getRndFloat();float  p55=getRndFloat();float  p56=getRndFloat();
        double  p57=getRndDouble();byte  p58=getRndByte();float  p59=getRndFloat();
        float  p60=getRndFloat();float  p61=getRndFloat();float  p62=getRndFloat();
        float  p63=getRndFloat();float  p64=getRndFloat();float  p65=getRndFloat();
        byte  p66=getRndByte();double  p67=getRndDouble();byte  p68=getRndByte();
        float  p69=getRndFloat();float  p70=getRndFloat();double  p71=getRndDouble();
        double  p72=getRndDouble();double  p73=getRndDouble();int  p74=getRndInt();
        double  p75=getRndDouble();float  p76=getRndFloat();float  p77=getRndFloat();
        float  p78=getRndFloat();float  p79=getRndFloat();double  p80=getRndDouble();
        float  p81=getRndFloat();double  p82=getRndDouble();float  p83=getRndFloat();
        float  p84=getRndFloat();int  p85=getRndInt();float  p86=getRndFloat();
        double  p87=getRndDouble();double  p88=getRndDouble();float  p89=getRndFloat();
        int  p90=getRndInt();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();float  p94=getRndFloat();float  p95=getRndFloat();
        double  p96=getRndDouble();double  p97=getRndDouble();float  p98=getRndFloat();
        double  p99=getRndDouble();float  p100=getRndFloat();float  p101=getRndFloat();
        float  p102=getRndFloat();float  p103=getRndFloat();double  p104=getRndDouble();
        float  p105=getRndFloat();double  p106=getRndDouble();float  p107=getRndFloat();
        double  p108=getRndDouble();float  p109=getRndFloat();float  p110=getRndFloat();
        double  p111=getRndDouble();float  p112=getRndFloat();byte  p113=getRndByte();
        float  p114=getRndFloat();double  p115=getRndDouble();double  p116=getRndDouble();
        float  p117=getRndFloat();double  p118=getRndDouble();double  p119=getRndDouble();
        byte  p120=getRndByte();float  p121=getRndFloat();float  p122=getRndFloat();
        byte  p123=getRndByte();double  p124=getRndDouble();double  p125=getRndDouble();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%d\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%d\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%d\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%d\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%d\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%d\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%d\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%d\n",p41);ps.format("p42=%d\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%d\n",p47);ps.format("p48=%d\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%d\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%d\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%d\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%d\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%d\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%d\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc49(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc50(double p0,double p1,double p2,double p3,float p4,float p5,double p6,float p7
        ,double p8,float p9,double p10,double p11,double p12,double p13,double p14
        ,double p15,float p16,double p17,float p18,double p19,float p20,double p21
        ,float p22,double p23,double p24,double p25,float p26,double p27,double p28
        ,double p29,double p30,double p31,float p32,float p33,double p34,double p35
        ,double p36,double p37,float p38,double p39,double p40,double p41,double p42
        ,double p43,double p44,double p45,double p46,float p47,double p48,float p49
        ,double p50,double p51,double p52,double p53,double p54,double p55,double p56
        ,double p57,double p58,double p59,double p60,double p61,float p62,float p63
        ,double p64,double p65,double p66,double p67,double p68,double p69,double p70
        ,float p71,double p72,double p73,float p74,double p75,double p76,double p77
        ,double p78,double p79,double p80,double p81,double p82,float p83,float p84
        ,double p85,double p86,float p87,double p88,double p89,float p90,double p91
        ,double p92,double p93,float p94,double p95,float p96,double p97,double p98
        ,float p99,double p100,float p101,double p102,double p103,double p104,double p105
        ,double p106,double p107,double p108,double p109,float p110,double p111
        ,double p112,double p113,double p114,float p115,double p116,double p117
        ,double p118,float p119,double p120,double p121,double p122,double p123
        ,double p124,double p125,double p126    );
    private static void nativeFnc50_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();float  p4=getRndFloat();float  p5=getRndFloat();
        double  p6=getRndDouble();float  p7=getRndFloat();double  p8=getRndDouble();
        float  p9=getRndFloat();double  p10=getRndDouble();double  p11=getRndDouble();
        double  p12=getRndDouble();double  p13=getRndDouble();double  p14=getRndDouble();
        double  p15=getRndDouble();float  p16=getRndFloat();double  p17=getRndDouble();
        float  p18=getRndFloat();double  p19=getRndDouble();float  p20=getRndFloat();
        double  p21=getRndDouble();float  p22=getRndFloat();double  p23=getRndDouble();
        double  p24=getRndDouble();double  p25=getRndDouble();float  p26=getRndFloat();
        double  p27=getRndDouble();double  p28=getRndDouble();double  p29=getRndDouble();
        double  p30=getRndDouble();double  p31=getRndDouble();float  p32=getRndFloat();
        float  p33=getRndFloat();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();double  p37=getRndDouble();float  p38=getRndFloat();
        double  p39=getRndDouble();double  p40=getRndDouble();double  p41=getRndDouble();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        double  p45=getRndDouble();double  p46=getRndDouble();float  p47=getRndFloat();
        double  p48=getRndDouble();float  p49=getRndFloat();double  p50=getRndDouble();
        double  p51=getRndDouble();double  p52=getRndDouble();double  p53=getRndDouble();
        double  p54=getRndDouble();double  p55=getRndDouble();double  p56=getRndDouble();
        double  p57=getRndDouble();double  p58=getRndDouble();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();float  p62=getRndFloat();
        float  p63=getRndFloat();double  p64=getRndDouble();double  p65=getRndDouble();
        double  p66=getRndDouble();double  p67=getRndDouble();double  p68=getRndDouble();
        double  p69=getRndDouble();double  p70=getRndDouble();float  p71=getRndFloat();
        double  p72=getRndDouble();double  p73=getRndDouble();float  p74=getRndFloat();
        double  p75=getRndDouble();double  p76=getRndDouble();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();double  p80=getRndDouble();
        double  p81=getRndDouble();double  p82=getRndDouble();float  p83=getRndFloat();
        float  p84=getRndFloat();double  p85=getRndDouble();double  p86=getRndDouble();
        float  p87=getRndFloat();double  p88=getRndDouble();double  p89=getRndDouble();
        float  p90=getRndFloat();double  p91=getRndDouble();double  p92=getRndDouble();
        double  p93=getRndDouble();float  p94=getRndFloat();double  p95=getRndDouble();
        float  p96=getRndFloat();double  p97=getRndDouble();double  p98=getRndDouble();
        float  p99=getRndFloat();double  p100=getRndDouble();float  p101=getRndFloat();
        double  p102=getRndDouble();double  p103=getRndDouble();double  p104=getRndDouble();
        double  p105=getRndDouble();double  p106=getRndDouble();double  p107=getRndDouble();
        double  p108=getRndDouble();double  p109=getRndDouble();float  p110=getRndFloat();
        double  p111=getRndDouble();double  p112=getRndDouble();double  p113=getRndDouble();
        double  p114=getRndDouble();float  p115=getRndFloat();double  p116=getRndDouble();
        double  p117=getRndDouble();double  p118=getRndDouble();float  p119=getRndFloat();
        double  p120=getRndDouble();double  p121=getRndDouble();double  p122=getRndDouble();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        double  p126=getRndDouble();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%e\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%e\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%e\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%e\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%e\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%e\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%e\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);ps.format("p103=%e\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%e\n",p112);
        ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc50(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    native public static void nativeFnc51(float p0,float p1,float p2,float p3,float p4,double p5,float p6,float p7
        ,double p8,double p9,int p10,float p11,double p12,double p13,float p14,double p15
        ,double p16,double p17,double p18,double p19,float p20,double p21,float p22
        ,int p23,double p24,double p25,float p26,int p27,double p28,float p29,float p30
        ,double p31,int p32,int p33,float p34,float p35,double p36,float p37,float p38
        ,float p39,float p40,float p41,double p42,double p43,double p44,int p45
        ,double p46,float p47,float p48,double p49,double p50,float p51,float p52
        ,float p53,double p54,float p55,double p56,float p57,float p58,int p59,int p60
        ,double p61,double p62,double p63,float p64,float p65,double p66,int p67
        ,int p68,float p69,int p70,double p71,int p72,float p73,float p74,double p75
        ,double p76,double p77,float p78,float p79,float p80,double p81,double p82
        ,float p83,float p84,double p85,float p86,int p87,float p88,float p89,double p90
        ,float p91,double p92,double p93,int p94,float p95,float p96,float p97,double p98
        ,double p99,double p100,double p101,float p102,float p103,double p104,float p105
        ,double p106,double p107,float p108,float p109,double p110,float p111,float p112
        ,float p113,float p114,float p115,double p116,float p117,double p118,double p119
        ,double p120,float p121,float p122,float p123,double p124,double p125,float p126
            );
    private static void nativeFnc51_invoke(PrintStream ps)
    {
        float  p0=getRndFloat();float  p1=getRndFloat();float  p2=getRndFloat();
        float  p3=getRndFloat();float  p4=getRndFloat();double  p5=getRndDouble();
        float  p6=getRndFloat();float  p7=getRndFloat();double  p8=getRndDouble();
        double  p9=getRndDouble();int  p10=getRndInt();float  p11=getRndFloat();
        double  p12=getRndDouble();double  p13=getRndDouble();float  p14=getRndFloat();
        double  p15=getRndDouble();double  p16=getRndDouble();double  p17=getRndDouble();
        double  p18=getRndDouble();double  p19=getRndDouble();float  p20=getRndFloat();
        double  p21=getRndDouble();float  p22=getRndFloat();int  p23=getRndInt();
        double  p24=getRndDouble();double  p25=getRndDouble();float  p26=getRndFloat();
        int  p27=getRndInt();double  p28=getRndDouble();float  p29=getRndFloat();
        float  p30=getRndFloat();double  p31=getRndDouble();int  p32=getRndInt();
        int  p33=getRndInt();float  p34=getRndFloat();float  p35=getRndFloat();
        double  p36=getRndDouble();float  p37=getRndFloat();float  p38=getRndFloat();
        float  p39=getRndFloat();float  p40=getRndFloat();float  p41=getRndFloat();
        double  p42=getRndDouble();double  p43=getRndDouble();double  p44=getRndDouble();
        int  p45=getRndInt();double  p46=getRndDouble();float  p47=getRndFloat();
        float  p48=getRndFloat();double  p49=getRndDouble();double  p50=getRndDouble();
        float  p51=getRndFloat();float  p52=getRndFloat();float  p53=getRndFloat();
        double  p54=getRndDouble();float  p55=getRndFloat();double  p56=getRndDouble();
        float  p57=getRndFloat();float  p58=getRndFloat();int  p59=getRndInt();
        int  p60=getRndInt();double  p61=getRndDouble();double  p62=getRndDouble();
        double  p63=getRndDouble();float  p64=getRndFloat();float  p65=getRndFloat();
        double  p66=getRndDouble();int  p67=getRndInt();int  p68=getRndInt();float  p69=getRndFloat();
        int  p70=getRndInt();double  p71=getRndDouble();int  p72=getRndInt();float  p73=getRndFloat();
        float  p74=getRndFloat();double  p75=getRndDouble();double  p76=getRndDouble();
        double  p77=getRndDouble();float  p78=getRndFloat();float  p79=getRndFloat();
        float  p80=getRndFloat();double  p81=getRndDouble();double  p82=getRndDouble();
        float  p83=getRndFloat();float  p84=getRndFloat();double  p85=getRndDouble();
        float  p86=getRndFloat();int  p87=getRndInt();float  p88=getRndFloat();
        float  p89=getRndFloat();double  p90=getRndDouble();float  p91=getRndFloat();
        double  p92=getRndDouble();double  p93=getRndDouble();int  p94=getRndInt();
        float  p95=getRndFloat();float  p96=getRndFloat();float  p97=getRndFloat();
        double  p98=getRndDouble();double  p99=getRndDouble();double  p100=getRndDouble();
        double  p101=getRndDouble();float  p102=getRndFloat();float  p103=getRndFloat();
        double  p104=getRndDouble();float  p105=getRndFloat();double  p106=getRndDouble();
        double  p107=getRndDouble();float  p108=getRndFloat();float  p109=getRndFloat();
        double  p110=getRndDouble();float  p111=getRndFloat();float  p112=getRndFloat();
        float  p113=getRndFloat();float  p114=getRndFloat();float  p115=getRndFloat();
        double  p116=getRndDouble();float  p117=getRndFloat();double  p118=getRndDouble();
        double  p119=getRndDouble();double  p120=getRndDouble();float  p121=getRndFloat();
        float  p122=getRndFloat();float  p123=getRndFloat();double  p124=getRndDouble();
        double  p125=getRndDouble();float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);
        ps.format("p1=%e\n",p1);ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);
        ps.format("p4=%e\n",p4);ps.format("p5=%e\n",p5);ps.format("p6=%e\n",p6);
        ps.format("p7=%e\n",p7);ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);
        ps.format("p10=%d\n",p10);ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);
        ps.format("p13=%e\n",p13);ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);
        ps.format("p16=%e\n",p16);ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);
        ps.format("p19=%e\n",p19);ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);
        ps.format("p22=%e\n",p22);ps.format("p23=%d\n",p23);ps.format("p24=%e\n",p24);
        ps.format("p25=%e\n",p25);ps.format("p26=%e\n",p26);ps.format("p27=%d\n",p27);
        ps.format("p28=%e\n",p28);ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);
        ps.format("p31=%e\n",p31);ps.format("p32=%d\n",p32);ps.format("p33=%d\n",p33);
        ps.format("p34=%e\n",p34);ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);
        ps.format("p37=%e\n",p37);ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);
        ps.format("p40=%e\n",p40);ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);
        ps.format("p43=%e\n",p43);ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);
        ps.format("p46=%e\n",p46);ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);
        ps.format("p49=%e\n",p49);ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);
        ps.format("p52=%e\n",p52);ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);
        ps.format("p55=%e\n",p55);ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);
        ps.format("p58=%e\n",p58);ps.format("p59=%d\n",p59);ps.format("p60=%d\n",p60);
        ps.format("p61=%e\n",p61);ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);
        ps.format("p64=%e\n",p64);ps.format("p65=%e\n",p65);ps.format("p66=%e\n",p66);
        ps.format("p67=%d\n",p67);ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);
        ps.format("p70=%d\n",p70);ps.format("p71=%e\n",p71);ps.format("p72=%d\n",p72);
        ps.format("p73=%e\n",p73);ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);
        ps.format("p76=%e\n",p76);ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);
        ps.format("p79=%e\n",p79);ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);
        ps.format("p82=%e\n",p82);ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);
        ps.format("p85=%e\n",p85);ps.format("p86=%e\n",p86);ps.format("p87=%d\n",p87);
        ps.format("p88=%e\n",p88);ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);
        ps.format("p91=%e\n",p91);ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);
        ps.format("p94=%d\n",p94);ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);
        ps.format("p97=%e\n",p97);ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);
        ps.format("p100=%e\n",p100);ps.format("p101=%e\n",p101);ps.format("p102=%e\n",p102);
        ps.format("p103=%e\n",p103);ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);
        ps.format("p106=%e\n",p106);ps.format("p107=%e\n",p107);ps.format("p108=%e\n",p108);
        ps.format("p109=%e\n",p109);ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);
        ps.format("p112=%e\n",p112);ps.format("p113=%e\n",p113);ps.format("p114=%e\n",p114);
        ps.format("p115=%e\n",p115);ps.format("p116=%e\n",p116);ps.format("p117=%e\n",p117);
        ps.format("p118=%e\n",p118);ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);
        ps.format("p121=%e\n",p121);ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);
        ps.format("p124=%e\n",p124);ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);

        nativeFnc51(p0,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15
        ,p16,p17,p18,p19,p20,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33
        ,p34,p35,p36,p37,p38,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51
        ,p52,p53,p54,p55,p56,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69
        ,p70,p71,p72,p73,p74,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87
        ,p88,p89,p90,p91,p92,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104
        ,p105,p106,p107,p108,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119
        ,p120,p121,p122,p123,p124,p125,p126);
}

    native public static void nativeFnc52(double p0,double p1,double p2,double p3,double p4,byte p5,float p6,double p7
        ,float p8,float p9,float p10,float p11,double p12,float p13,double p14,double p15
        ,double p16,float p17,float p18,double p19,double p20,double p21,double p22
        ,float p23,double p24,float p25,float p26,double p27,double p28,float p29
        ,double p30,float p31,float p32,float p33,double p34,double p35,double p36
        ,float p37,float p38,double p39,double p40,float p41,float p42,float p43
        ,float p44,byte p45,float p46,double p47,float p48,float p49,double p50
        ,float p51,byte p52,float p53,float p54,double p55,float p56,double p57
        ,byte p58,double p59,double p60,double p61,double p62,float p63,double p64
        ,byte p65,float p66,float p67,byte p68,double p69,byte p70,double p71,double p72
        ,double p73,float p74,double p75,byte p76,double p77,double p78,double p79
        ,double p80,float p81,double p82,double p83,double p84,float p85,float p86
        ,float p87,float p88,double p89,float p90,float p91,float p92,double p93
        ,byte p94,double p95,float p96,float p97,double p98,double p99,float p100
        ,double p101,byte p102,byte p103,float p104,float p105,double p106,float p107
        ,byte p108,float p109,double p110,float p111,byte p112,byte p113,double p114
        ,double p115,double p116,byte p117,float p118,double p119,double p120,float p121
        ,double p122,double p123,double p124,double p125,float p126    );
    private static void nativeFnc52_invoke(PrintStream ps)
    {
        double  p0=getRndDouble();double  p1=getRndDouble();double  p2=getRndDouble();
        double  p3=getRndDouble();double  p4=getRndDouble();byte  p5=getRndByte();
        float  p6=getRndFloat();double  p7=getRndDouble();float  p8=getRndFloat();
        float  p9=getRndFloat();float  p10=getRndFloat();float  p11=getRndFloat();
        double  p12=getRndDouble();float  p13=getRndFloat();double  p14=getRndDouble();
        double  p15=getRndDouble();double  p16=getRndDouble();float  p17=getRndFloat();
        float  p18=getRndFloat();double  p19=getRndDouble();double  p20=getRndDouble();
        double  p21=getRndDouble();double  p22=getRndDouble();float  p23=getRndFloat();
        double  p24=getRndDouble();float  p25=getRndFloat();float  p26=getRndFloat();
        double  p27=getRndDouble();double  p28=getRndDouble();float  p29=getRndFloat();
        double  p30=getRndDouble();float  p31=getRndFloat();float  p32=getRndFloat();
        float  p33=getRndFloat();double  p34=getRndDouble();double  p35=getRndDouble();
        double  p36=getRndDouble();float  p37=getRndFloat();float  p38=getRndFloat();
        double  p39=getRndDouble();double  p40=getRndDouble();float  p41=getRndFloat();
        float  p42=getRndFloat();float  p43=getRndFloat();float  p44=getRndFloat();
        byte  p45=getRndByte();float  p46=getRndFloat();double  p47=getRndDouble();
        float  p48=getRndFloat();float  p49=getRndFloat();double  p50=getRndDouble();
        float  p51=getRndFloat();byte  p52=getRndByte();float  p53=getRndFloat();
        float  p54=getRndFloat();double  p55=getRndDouble();float  p56=getRndFloat();
        double  p57=getRndDouble();byte  p58=getRndByte();double  p59=getRndDouble();
        double  p60=getRndDouble();double  p61=getRndDouble();double  p62=getRndDouble();
        float  p63=getRndFloat();double  p64=getRndDouble();byte  p65=getRndByte();
        float  p66=getRndFloat();float  p67=getRndFloat();byte  p68=getRndByte();
        double  p69=getRndDouble();byte  p70=getRndByte();double  p71=getRndDouble();
        double  p72=getRndDouble();double  p73=getRndDouble();float  p74=getRndFloat();
        double  p75=getRndDouble();byte  p76=getRndByte();double  p77=getRndDouble();
        double  p78=getRndDouble();double  p79=getRndDouble();double  p80=getRndDouble();
        float  p81=getRndFloat();double  p82=getRndDouble();double  p83=getRndDouble();
        double  p84=getRndDouble();float  p85=getRndFloat();float  p86=getRndFloat();
        float  p87=getRndFloat();float  p88=getRndFloat();double  p89=getRndDouble();
        float  p90=getRndFloat();float  p91=getRndFloat();float  p92=getRndFloat();
        double  p93=getRndDouble();byte  p94=getRndByte();double  p95=getRndDouble();
        float  p96=getRndFloat();float  p97=getRndFloat();double  p98=getRndDouble();
        double  p99=getRndDouble();float  p100=getRndFloat();double  p101=getRndDouble();
        byte  p102=getRndByte();byte  p103=getRndByte();float  p104=getRndFloat();
        float  p105=getRndFloat();double  p106=getRndDouble();float  p107=getRndFloat();
        byte  p108=getRndByte();float  p109=getRndFloat();double  p110=getRndDouble();
        float  p111=getRndFloat();byte  p112=getRndByte();byte  p113=getRndByte();
        double  p114=getRndDouble();double  p115=getRndDouble();double  p116=getRndDouble();
        byte  p117=getRndByte();float  p118=getRndFloat();double  p119=getRndDouble();
        double  p120=getRndDouble();float  p121=getRndFloat();double  p122=getRndDouble();
        double  p123=getRndDouble();double  p124=getRndDouble();double  p125=getRndDouble();
        float  p126=getRndFloat();
        ps.format("p0=%e\n",p0);ps.format("p1=%e\n",p1);
        ps.format("p2=%e\n",p2);ps.format("p3=%e\n",p3);ps.format("p4=%e\n",p4);
        ps.format("p5=%d\n",p5);ps.format("p6=%e\n",p6);ps.format("p7=%e\n",p7);
        ps.format("p8=%e\n",p8);ps.format("p9=%e\n",p9);ps.format("p10=%e\n",p10);
        ps.format("p11=%e\n",p11);ps.format("p12=%e\n",p12);ps.format("p13=%e\n",p13);
        ps.format("p14=%e\n",p14);ps.format("p15=%e\n",p15);ps.format("p16=%e\n",p16);
        ps.format("p17=%e\n",p17);ps.format("p18=%e\n",p18);ps.format("p19=%e\n",p19);
        ps.format("p20=%e\n",p20);ps.format("p21=%e\n",p21);ps.format("p22=%e\n",p22);
        ps.format("p23=%e\n",p23);ps.format("p24=%e\n",p24);ps.format("p25=%e\n",p25);
        ps.format("p26=%e\n",p26);ps.format("p27=%e\n",p27);ps.format("p28=%e\n",p28);
        ps.format("p29=%e\n",p29);ps.format("p30=%e\n",p30);ps.format("p31=%e\n",p31);
        ps.format("p32=%e\n",p32);ps.format("p33=%e\n",p33);ps.format("p34=%e\n",p34);
        ps.format("p35=%e\n",p35);ps.format("p36=%e\n",p36);ps.format("p37=%e\n",p37);
        ps.format("p38=%e\n",p38);ps.format("p39=%e\n",p39);ps.format("p40=%e\n",p40);
        ps.format("p41=%e\n",p41);ps.format("p42=%e\n",p42);ps.format("p43=%e\n",p43);
        ps.format("p44=%e\n",p44);ps.format("p45=%d\n",p45);ps.format("p46=%e\n",p46);
        ps.format("p47=%e\n",p47);ps.format("p48=%e\n",p48);ps.format("p49=%e\n",p49);
        ps.format("p50=%e\n",p50);ps.format("p51=%e\n",p51);ps.format("p52=%d\n",p52);
        ps.format("p53=%e\n",p53);ps.format("p54=%e\n",p54);ps.format("p55=%e\n",p55);
        ps.format("p56=%e\n",p56);ps.format("p57=%e\n",p57);ps.format("p58=%d\n",p58);
        ps.format("p59=%e\n",p59);ps.format("p60=%e\n",p60);ps.format("p61=%e\n",p61);
        ps.format("p62=%e\n",p62);ps.format("p63=%e\n",p63);ps.format("p64=%e\n",p64);
        ps.format("p65=%d\n",p65);ps.format("p66=%e\n",p66);ps.format("p67=%e\n",p67);
        ps.format("p68=%d\n",p68);ps.format("p69=%e\n",p69);ps.format("p70=%d\n",p70);
        ps.format("p71=%e\n",p71);ps.format("p72=%e\n",p72);ps.format("p73=%e\n",p73);
        ps.format("p74=%e\n",p74);ps.format("p75=%e\n",p75);ps.format("p76=%d\n",p76);
        ps.format("p77=%e\n",p77);ps.format("p78=%e\n",p78);ps.format("p79=%e\n",p79);
        ps.format("p80=%e\n",p80);ps.format("p81=%e\n",p81);ps.format("p82=%e\n",p82);
        ps.format("p83=%e\n",p83);ps.format("p84=%e\n",p84);ps.format("p85=%e\n",p85);
        ps.format("p86=%e\n",p86);ps.format("p87=%e\n",p87);ps.format("p88=%e\n",p88);
        ps.format("p89=%e\n",p89);ps.format("p90=%e\n",p90);ps.format("p91=%e\n",p91);
        ps.format("p92=%e\n",p92);ps.format("p93=%e\n",p93);ps.format("p94=%d\n",p94);
        ps.format("p95=%e\n",p95);ps.format("p96=%e\n",p96);ps.format("p97=%e\n",p97);
        ps.format("p98=%e\n",p98);ps.format("p99=%e\n",p99);ps.format("p100=%e\n",p100);
        ps.format("p101=%e\n",p101);ps.format("p102=%d\n",p102);ps.format("p103=%d\n",p103);
        ps.format("p104=%e\n",p104);ps.format("p105=%e\n",p105);ps.format("p106=%e\n",p106);
        ps.format("p107=%e\n",p107);ps.format("p108=%d\n",p108);ps.format("p109=%e\n",p109);
        ps.format("p110=%e\n",p110);ps.format("p111=%e\n",p111);ps.format("p112=%d\n",p112);
        ps.format("p113=%d\n",p113);ps.format("p114=%e\n",p114);ps.format("p115=%e\n",p115);
        ps.format("p116=%e\n",p116);ps.format("p117=%d\n",p117);ps.format("p118=%e\n",p118);
        ps.format("p119=%e\n",p119);ps.format("p120=%e\n",p120);ps.format("p121=%e\n",p121);
        ps.format("p122=%e\n",p122);ps.format("p123=%e\n",p123);ps.format("p124=%e\n",p124);
        ps.format("p125=%e\n",p125);ps.format("p126=%e\n",p126);
        nativeFnc52(p0
        ,p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20
        ,p21,p22,p23,p24,p25,p26,p27,p28,p29,p30,p31,p32,p33,p34,p35,p36,p37,p38
        ,p39,p40,p41,p42,p43,p44,p45,p46,p47,p48,p49,p50,p51,p52,p53,p54,p55,p56
        ,p57,p58,p59,p60,p61,p62,p63,p64,p65,p66,p67,p68,p69,p70,p71,p72,p73,p74
        ,p75,p76,p77,p78,p79,p80,p81,p82,p83,p84,p85,p86,p87,p88,p89,p90,p91,p92
        ,p93,p94,p95,p96,p97,p98,p99,p100,p101,p102,p103,p104,p105,p106,p107,p108
        ,p109,p110,p111,p112,p113,p114,p115,p116,p117,p118,p119,p120,p121,p122,p123
        ,p124,p125,p126);
}

    public static void main(String[] args) throws Exception
    {
        if ( System.getProperty("os.name").matches(".*[Ww][Ii][Nn].*") )
        {
            System.out.println("TEST PASSED! Dummy execution on Windows* OS!");
            return;
        }
        deleteFiles();
        PrintStream ps=new PrintStream(new File("LTTest_java.txt"));
    if (args.length>0)
        switch(args[0])
        {
        case "nativeFnc1":
            nativeFnc1_invoke(ps);
            break;
        case "nativeFnc2":
            nativeFnc2_invoke(ps);
            break;
        case "nativeFnc3":
            nativeFnc3_invoke(ps);
            break;
        case "nativeFnc4":
            nativeFnc4_invoke(ps);
            break;
        case "nativeFnc5":
            nativeFnc5_invoke(ps);
            break;
        case "nativeFnc6":
            nativeFnc6_invoke(ps);
            break;
        case "nativeFnc7":
            nativeFnc7_invoke(ps);
            break;
        case "nativeFnc8":
            nativeFnc8_invoke(ps);
            break;
        case "nativeFnc9":
            nativeFnc9_invoke(ps);
            break;
        case "nativeFnc10":
            nativeFnc10_invoke(ps);
            break;
        case "nativeFnc11":
            nativeFnc11_invoke(ps);
            break;
        case "nativeFnc12":
            nativeFnc12_invoke(ps);
            break;
        case "nativeFnc13":
            nativeFnc13_invoke(ps);
            break;
        case "nativeFnc14":
            nativeFnc14_invoke(ps);
            break;
        case "nativeFnc15":
            nativeFnc15_invoke(ps);
            break;
        case "nativeFnc16":
            nativeFnc16_invoke(ps);
            break;
        case "nativeFnc17":
            nativeFnc17_invoke(ps);
            break;
        case "nativeFnc18":
            nativeFnc18_invoke(ps);
            break;
        case "nativeFnc19":
            nativeFnc19_invoke(ps);
            break;
        case "nativeFnc20":
            nativeFnc20_invoke(ps);
            break;
        case "nativeFnc21":
            nativeFnc21_invoke(ps);
            break;
        case "nativeFnc22":
            nativeFnc22_invoke(ps);
            break;
        case "nativeFnc23":
            nativeFnc23_invoke(ps);
            break;
        case "nativeFnc24":
            nativeFnc24_invoke(ps);
            break;
        case "nativeFnc25":
            nativeFnc25_invoke(ps);
            break;
        case "nativeFnc26":
            nativeFnc26_invoke(ps);
            break;
        case "nativeFnc27":
            nativeFnc27_invoke(ps);
            break;
        case "nativeFnc28":
            nativeFnc28_invoke(ps);
            break;
        case "nativeFnc29":
            nativeFnc29_invoke(ps);
            break;
        case "nativeFnc30":
            nativeFnc30_invoke(ps);
            break;
        case "nativeFnc31":
            nativeFnc31_invoke(ps);
            break;
        case "nativeFnc32":
            nativeFnc32_invoke(ps);
            break;
        case "nativeFnc33":
            nativeFnc33_invoke(ps);
            break;
        case "nativeFnc34":
            nativeFnc34_invoke(ps);
            break;
        case "nativeFnc35":
            nativeFnc35_invoke(ps);
            break;
        case "nativeFnc36":
            nativeFnc36_invoke(ps);
            break;
        case "nativeFnc37":
            nativeFnc37_invoke(ps);
            break;
        case "nativeFnc38":
            nativeFnc38_invoke(ps);
            break;
        case "nativeFnc39":
            nativeFnc39_invoke(ps);
            break;
        case "nativeFnc40":
            nativeFnc40_invoke(ps);
            break;
        case "nativeFnc41":
            nativeFnc41_invoke(ps);
            break;
        case "nativeFnc42":
            nativeFnc42_invoke(ps);
            break;
        case "nativeFnc43":
            nativeFnc43_invoke(ps);
            break;
        case "nativeFnc44":
            nativeFnc44_invoke(ps);
            break;
        case "nativeFnc45":
            nativeFnc45_invoke(ps);
            break;
        case "nativeFnc46":
            nativeFnc46_invoke(ps);
            break;
        case "nativeFnc47":
            nativeFnc47_invoke(ps);
            break;
        case "nativeFnc48":
            nativeFnc48_invoke(ps);
            break;
        case "nativeFnc49":
            nativeFnc49_invoke(ps);
            break;
        case "nativeFnc50":
            nativeFnc50_invoke(ps);
            break;
        case "nativeFnc51":
            nativeFnc51_invoke(ps);
            break;
        case "nativeFnc52":
            nativeFnc52_invoke(ps);
            break;
        default:
            throw new Exception("FAIL: invalid args!");
        }
        else
        {
            throw new Exception("FAIL: invalid args!");
        }
        flag=chkFile();
        if(!flag)
            throw new Exception("FAIL:Tests failed!");
    }
    private static boolean chkFile()
    {
        File javaFile=new File("LTTest_java.txt");
        if (! javaFile.exists())
        {
            System.out.println("FAIL:Failed to open file LTTest_java.txt - file not exists!");
            return false;
        }
        File cFile=new File("LTTest_c.txt");
        if (! cFile.exists())
        {
                System.out.println("FAIL:Failed to open file LTTest_c.txt - file not exists!");
                return false;
        }
        if ( cFile.length()!=javaFile.length() )
        {
            System.out.println("FAIL:File length not equal!");
            return false;
        }
        long byteCount=cFile.length();
        try{
            FileInputStream fisC=new FileInputStream(cFile);
            FileInputStream fisJava=new FileInputStream(javaFile);
            byte[] cData=new byte[fisC.available()];
            fisC.read(cData);
            byte[] javaData=new byte[fisJava.available()];
            fisJava.read(javaData);
            for ( int cnt=0;cnt<byteCount;++cnt)
            {
                if ( cData[cnt]!=javaData[cnt] )
                {
                    System.out.println("FAIL:Test failed! "+cnt+" byte are wrong! C file - " + cData[cnt] + " Java file - "+javaData[cnt] );
                    return false;
                }
            }
        }
        catch (FileNotFoundException ex)
        {
            System.out.println("FAIL:Some of files not found!");
            return false;
        }
        catch (IOException ex)
        {
            System.out.println("FAIL:Failed to read files!");
            return false;
        }
        System.out.println("PASS: all data passed correctly!");
        return true;
    }
}
