/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <stdio.h>

extern "C" {

JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc1(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jfloat p5,jfloat p6,jfloat p7
    ,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jfloat p14
    ,jfloat p15,jfloat p16,jfloat p17,jfloat p18,jfloat p19,jfloat p20,jfloat p21
    ,jfloat p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35
    ,jfloat p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jfloat p43,jfloat p44,jfloat p45,jfloat p46,jfloat p47,jfloat p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55,jfloat p56
    ,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jfloat p62,jfloat p63
    ,jfloat p64,jfloat p65,jfloat p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jfloat p71,jfloat p72,jfloat p73,jfloat p74,jfloat p75,jfloat p76,jfloat p77
    ,jfloat p78,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84
    ,jfloat p85,jfloat p86,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jfloat p91
    ,jfloat p92,jfloat p93,jfloat p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98
    ,jfloat p99,jfloat p100,jfloat p101,jfloat p102,jfloat p103,jfloat p104
    ,jfloat p105,jfloat p106,jfloat p107,jfloat p108,jfloat p109,jfloat p110
    ,jfloat p111,jfloat p112,jfloat p113,jfloat p114,jfloat p115,jfloat p116
    ,jfloat p117,jfloat p118,jfloat p119,jfloat p120,jfloat p121,jfloat p122
    ,jfloat p123,jfloat p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc2(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jdouble p19
    ,jdouble p20,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25
    ,jdouble p26,jdouble p27,jdouble p28,jdouble p29,jdouble p30,jdouble p31
    ,jdouble p32,jdouble p33,jdouble p34,jdouble p35,jdouble p36,jdouble p37
    ,jdouble p38,jdouble p39,jdouble p40,jdouble p41,jdouble p42,jdouble p43
    ,jdouble p44,jdouble p45,jdouble p46,jdouble p47,jdouble p48,jdouble p49
    ,jdouble p50,jdouble p51,jdouble p52,jdouble p53,jdouble p54,jdouble p55
    ,jdouble p56,jdouble p57,jdouble p58,jdouble p59,jdouble p60,jdouble p61
    ,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jdouble p66,jdouble p67
    ,jdouble p68,jdouble p69,jdouble p70,jdouble p71,jdouble p72,jdouble p73
    ,jdouble p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79
    ,jdouble p80,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85
    ,jdouble p86,jdouble p87,jdouble p88,jdouble p89,jdouble p90,jdouble p91
    ,jdouble p92,jdouble p93,jdouble p94,jdouble p95,jdouble p96,jdouble p97
    ,jdouble p98,jdouble p99,jdouble p100,jdouble p101,jdouble p102,jdouble p103
    ,jdouble p104,jdouble p105,jdouble p106,jdouble p107,jdouble p108,jdouble p109
    ,jdouble p110,jdouble p111,jdouble p112,jdouble p113,jdouble p114,jdouble p115
    ,jdouble p116,jdouble p117,jdouble p118,jdouble p119,jdouble p120,jdouble p121
    ,jdouble p122,jdouble p123,jdouble p124,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc3(JNIEnv *e, jclass c
,jfloat p0,jint p1,jint p2,jint p3,jint p4,jfloat p5,jint p6,jfloat p7,jint p8
    ,jfloat p9,jint p10,jint p11,jfloat p12,jfloat p13,jfloat p14,jfloat p15
    ,jint p16,jint p17,jint p18,jfloat p19,jint p20,jfloat p21,jfloat p22,jfloat p23
    ,jfloat p24,jint p25,jfloat p26,jint p27,jfloat p28,jfloat p29,jfloat p30
    ,jint p31,jint p32,jfloat p33,jfloat p34,jfloat p35,jint p36,jfloat p37
    ,jint p38,jfloat p39,jint p40,jfloat p41,jfloat p42,jfloat p43,jint p44
    ,jint p45,jfloat p46,jfloat p47,jfloat p48,jfloat p49,jint p50,jint p51
    ,jint p52,jfloat p53,jint p54,jfloat p55,jint p56,jfloat p57,jfloat p58
    ,jfloat p59,jfloat p60,jint p61,jint p62,jint p63,jfloat p64,jfloat p65
    ,jfloat p66,jfloat p67,jint p68,jint p69,jfloat p70,jfloat p71,jint p72
    ,jint p73,jfloat p74,jint p75,jint p76,jint p77,jfloat p78,jfloat p79,jfloat p80
    ,jint p81,jfloat p82,jint p83,jfloat p84,jfloat p85,jfloat p86,jfloat p87
    ,jint p88,jint p89,jint p90,jfloat p91,jint p92,jint p93,jfloat p94,jfloat p95
    ,jint p96,jfloat p97,jfloat p98,jfloat p99,jint p100,jfloat p101,jfloat p102
    ,jint p103,jfloat p104,jfloat p105,jint p106,jfloat p107,jfloat p108,jint p109
    ,jfloat p110,jfloat p111,jint p112,jfloat p113,jint p114,jfloat p115,jint p116
    ,jint p117,jfloat p118,jfloat p119,jint p120,jfloat p121,jint p122,jfloat p123
    ,jint p124,jint p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc4(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jbyte p3,jfloat p4,jfloat p5,jfloat p6,jbyte p7
    ,jfloat p8,jbyte p9,jfloat p10,jbyte p11,jbyte p12,jbyte p13,jfloat p14
    ,jfloat p15,jfloat p16,jfloat p17,jbyte p18,jbyte p19,jbyte p20,jbyte p21
    ,jfloat p22,jfloat p23,jbyte p24,jbyte p25,jfloat p26,jfloat p27,jfloat p28
    ,jbyte p29,jfloat p30,jfloat p31,jfloat p32,jbyte p33,jbyte p34,jfloat p35
    ,jbyte p36,jfloat p37,jbyte p38,jfloat p39,jfloat p40,jbyte p41,jfloat p42
    ,jbyte p43,jbyte p44,jfloat p45,jbyte p46,jfloat p47,jfloat p48,jbyte p49
    ,jbyte p50,jfloat p51,jbyte p52,jfloat p53,jfloat p54,jbyte p55,jfloat p56
    ,jfloat p57,jbyte p58,jfloat p59,jbyte p60,jbyte p61,jbyte p62,jfloat p63
    ,jbyte p64,jbyte p65,jbyte p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jbyte p71,jfloat p72,jfloat p73,jfloat p74,jbyte p75,jbyte p76,jfloat p77
    ,jfloat p78,jfloat p79,jfloat p80,jbyte p81,jfloat p82,jbyte p83,jfloat p84
    ,jfloat p85,jbyte p86,jfloat p87,jbyte p88,jfloat p89,jfloat p90,jbyte p91
    ,jbyte p92,jbyte p93,jbyte p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98
    ,jfloat p99,jbyte p100,jbyte p101,jfloat p102,jfloat p103,jfloat p104,jbyte p105
    ,jfloat p106,jfloat p107,jbyte p108,jfloat p109,jfloat p110,jbyte p111,jbyte p112
    ,jfloat p113,jbyte p114,jbyte p115,jfloat p116,jbyte p117,jbyte p118,jbyte p119
    ,jbyte p120,jfloat p121,jbyte p122,jbyte p123,jbyte p124,jbyte p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc5(JNIEnv *e, jclass c
,jint p0,jfloat p1,jint p2,jfloat p3,jbyte p4,jbyte p5,jbyte p6,jfloat p7
    ,jint p8,jbyte p9,jfloat p10,jbyte p11,jfloat p12,jbyte p13,jbyte p14,jint p15
    ,jfloat p16,jint p17,jint p18,jint p19,jint p20,jbyte p21,jbyte p22,jbyte p23
    ,jbyte p24,jint p25,jint p26,jfloat p27,jbyte p28,jfloat p29,jint p30,jfloat p31
    ,jbyte p32,jfloat p33,jint p34,jint p35,jfloat p36,jbyte p37,jint p38,jbyte p39
    ,jbyte p40,jfloat p41,jfloat p42,jbyte p43,jbyte p44,jfloat p45,jbyte p46
    ,jbyte p47,jint p48,jint p49,jint p50,jfloat p51,jint p52,jfloat p53,jbyte p54
    ,jint p55,jint p56,jbyte p57,jint p58,jbyte p59,jint p60,jfloat p61,jint p62
    ,jfloat p63,jint p64,jfloat p65,jbyte p66,jbyte p67,jfloat p68,jbyte p69
    ,jint p70,jfloat p71,jbyte p72,jint p73,jint p74,jbyte p75,jbyte p76,jbyte p77
    ,jint p78,jbyte p79,jbyte p80,jfloat p81,jbyte p82,jint p83,jbyte p84,jint p85
    ,jint p86,jbyte p87,jint p88,jfloat p89,jint p90,jint p91,jfloat p92,jbyte p93
    ,jfloat p94,jbyte p95,jfloat p96,jint p97,jfloat p98,jfloat p99,jint p100
    ,jbyte p101,jint p102,jbyte p103,jfloat p104,jfloat p105,jfloat p106,jfloat p107
    ,jfloat p108,jfloat p109,jint p110,jint p111,jint p112,jbyte p113,jfloat p114
    ,jfloat p115,jfloat p116,jint p117,jfloat p118,jint p119,jbyte p120,jint p121
    ,jbyte p122,jbyte p123,jfloat p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc6(JNIEnv *e, jclass c
,jdouble p0,jint p1,jint p2,jint p3,jint p4,jdouble p5,jdouble p6,jint p7
    ,jint p8,jdouble p9,jdouble p10,jint p11,jint p12,jdouble p13,jdouble p14
    ,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jint p19,jint p20,jdouble p21
    ,jdouble p22,jint p23,jdouble p24,jdouble p25,jdouble p26,jint p27,jint p28
    ,jint p29,jdouble p30,jdouble p31,jdouble p32,jint p33,jint p34,jdouble p35
    ,jint p36,jint p37,jdouble p38,jdouble p39,jdouble p40,jint p41,jdouble p42
    ,jdouble p43,jint p44,jint p45,jint p46,jint p47,jdouble p48,jdouble p49
    ,jdouble p50,jint p51,jint p52,jint p53,jint p54,jdouble p55,jint p56,jint p57
    ,jdouble p58,jint p59,jdouble p60,jdouble p61,jint p62,jint p63,jdouble p64
    ,jdouble p65,jint p66,jint p67,jdouble p68,jdouble p69,jdouble p70,jint p71
    ,jint p72,jdouble p73,jint p74,jint p75,jdouble p76,jdouble p77,jdouble p78
    ,jint p79,jint p80,jint p81,jint p82,jint p83,jdouble p84,jdouble p85,jint p86
    ,jdouble p87,jdouble p88,jint p89,jdouble p90,jint p91,jint p92,jdouble p93
    ,jint p94,jint p95,jdouble p96,jdouble p97,jint p98,jdouble p99,jdouble p100
    ,jdouble p101,jint p102,jdouble p103,jdouble p104,jint p105,jint p106,jint p107
    ,jdouble p108,jint p109,jdouble p110,jdouble p111,jdouble p112,jint p113
    ,jint p114,jdouble p115,jint p116,jdouble p117,jdouble p118,jint p119,jdouble p120
    ,jdouble p121,jint p122,jint p123,jint p124,jint p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc7(JNIEnv *e, jclass c
,jdouble p0,jbyte p1,jdouble p2,jdouble p3,jbyte p4,jbyte p5,jbyte p6,jbyte p7
    ,jdouble p8,jdouble p9,jbyte p10,jbyte p11,jdouble p12,jdouble p13,jbyte p14
    ,jbyte p15,jbyte p16,jbyte p17,jbyte p18,jdouble p19,jdouble p20,jdouble p21
    ,jdouble p22,jbyte p23,jdouble p24,jbyte p25,jdouble p26,jbyte p27,jdouble p28
    ,jbyte p29,jbyte p30,jdouble p31,jbyte p32,jdouble p33,jbyte p34,jdouble p35
    ,jdouble p36,jdouble p37,jdouble p38,jbyte p39,jdouble p40,jdouble p41,jbyte p42
    ,jdouble p43,jbyte p44,jdouble p45,jdouble p46,jdouble p47,jdouble p48,jbyte p49
    ,jbyte p50,jdouble p51,jdouble p52,jdouble p53,jbyte p54,jdouble p55,jdouble p56
    ,jdouble p57,jdouble p58,jdouble p59,jbyte p60,jdouble p61,jdouble p62,jbyte p63
    ,jbyte p64,jbyte p65,jbyte p66,jbyte p67,jbyte p68,jdouble p69,jdouble p70
    ,jbyte p71,jdouble p72,jdouble p73,jbyte p74,jdouble p75,jbyte p76,jbyte p77
    ,jbyte p78,jbyte p79,jbyte p80,jdouble p81,jdouble p82,jbyte p83,jbyte p84
    ,jbyte p85,jbyte p86,jdouble p87,jbyte p88,jdouble p89,jdouble p90,jdouble p91
    ,jbyte p92,jbyte p93,jdouble p94,jbyte p95,jbyte p96,jbyte p97,jbyte p98
    ,jdouble p99,jbyte p100,jbyte p101,jbyte p102,jdouble p103,jdouble p104
    ,jbyte p105,jdouble p106,jbyte p107,jbyte p108,jbyte p109,jdouble p110,jbyte p111
    ,jbyte p112,jbyte p113,jbyte p114,jbyte p115,jbyte p116,jdouble p117,jbyte p118
    ,jdouble p119,jdouble p120,jbyte p121,jbyte p122,jdouble p123,jdouble p124
    ,jbyte p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc8(JNIEnv *e, jclass c
,jbyte p0,jdouble p1,jbyte p2,jdouble p3,jint p4,jint p5,jint p6,jdouble p7
    ,jdouble p8,jint p9,jdouble p10,jbyte p11,jint p12,jbyte p13,jdouble p14
    ,jint p15,jbyte p16,jint p17,jdouble p18,jdouble p19,jbyte p20,jint p21
    ,jbyte p22,jint p23,jint p24,jbyte p25,jdouble p26,jint p27,jint p28,jbyte p29
    ,jint p30,jint p31,jbyte p32,jdouble p33,jdouble p34,jbyte p35,jdouble p36
    ,jint p37,jdouble p38,jint p39,jbyte p40,jbyte p41,jint p42,jint p43,jint p44
    ,jbyte p45,jdouble p46,jdouble p47,jint p48,jint p49,jbyte p50,jbyte p51
    ,jint p52,jint p53,jbyte p54,jbyte p55,jint p56,jint p57,jint p58,jdouble p59
    ,jbyte p60,jint p61,jdouble p62,jdouble p63,jdouble p64,jint p65,jint p66
    ,jdouble p67,jint p68,jdouble p69,jint p70,jint p71,jbyte p72,jint p73,jdouble p74
    ,jdouble p75,jbyte p76,jint p77,jbyte p78,jbyte p79,jint p80,jint p81,jint p82
    ,jdouble p83,jdouble p84,jint p85,jbyte p86,jbyte p87,jbyte p88,jbyte p89
    ,jint p90,jint p91,jdouble p92,jint p93,jdouble p94,jdouble p95,jint p96
    ,jbyte p97,jdouble p98,jdouble p99,jbyte p100,jbyte p101,jbyte p102,jbyte p103
    ,jdouble p104,jint p105,jint p106,jbyte p107,jbyte p108,jdouble p109,jdouble p110
    ,jdouble p111,jbyte p112,jdouble p113,jbyte p114,jbyte p115,jint p116,jdouble p117
    ,jint p118,jint p119,jbyte p120,jdouble p121,jbyte p122,jint p123,jint p124
    ,jint p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc9(JNIEnv *e, jclass c
,jdouble p0,jbyte p1,jint p2,jbyte p3,jint p4,jbyte p5,jint p6,jfloat p7
    ,jfloat p8,jint p9,jdouble p10,jint p11,jfloat p12,jfloat p13,jdouble p14
    ,jint p15,jfloat p16,jbyte p17,jfloat p18,jint p19,jfloat p20,jint p21,jfloat p22
    ,jfloat p23,jdouble p24,jbyte p25,jbyte p26,jbyte p27,jbyte p28,jfloat p29
    ,jbyte p30,jbyte p31,jbyte p32,jdouble p33,jint p34,jdouble p35,jfloat p36
    ,jint p37,jdouble p38,jint p39,jdouble p40,jbyte p41,jdouble p42,jfloat p43
    ,jfloat p44,jdouble p45,jfloat p46,jint p47,jfloat p48,jint p49,jfloat p50
    ,jbyte p51,jbyte p52,jint p53,jint p54,jfloat p55,jdouble p56,jint p57,jint p58
    ,jfloat p59,jint p60,jbyte p61,jint p62,jdouble p63,jdouble p64,jint p65
    ,jbyte p66,jdouble p67,jint p68,jbyte p69,jbyte p70,jint p71,jfloat p72
    ,jfloat p73,jbyte p74,jint p75,jbyte p76,jdouble p77,jfloat p78,jdouble p79
    ,jbyte p80,jint p81,jint p82,jbyte p83,jdouble p84,jfloat p85,jdouble p86
    ,jdouble p87,jfloat p88,jbyte p89,jbyte p90,jdouble p91,jdouble p92,jdouble p93
    ,jfloat p94,jdouble p95,jfloat p96,jdouble p97,jfloat p98,jbyte p99,jfloat p100
    ,jbyte p101,jbyte p102,jfloat p103,jdouble p104,jbyte p105,jfloat p106,jdouble p107
    ,jdouble p108,jint p109,jfloat p110,jint p111,jint p112,jbyte p113,jdouble p114
    ,jbyte p115,jdouble p116,jint p117,jdouble p118,jfloat p119,jbyte p120,jfloat p121
    ,jfloat p122,jfloat p123,jdouble p124,jint p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc10(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jfloat p2,jdouble p3,jfloat p4,jfloat p5,jdouble p6
    ,jfloat p7,jfloat p8,jdouble p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13
    ,jdouble p14,jfloat p15,jfloat p16,jdouble p17,jdouble p18,jdouble p19,jdouble p20
    ,jfloat p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jfloat p27,jfloat p28,jfloat p29,jdouble p30,jfloat p31,jfloat p32,jfloat p33
    ,jdouble p34,jdouble p35,jdouble p36,jdouble p37,jfloat p38,jfloat p39,jdouble p40
    ,jfloat p41,jdouble p42,jdouble p43,jfloat p44,jdouble p45,jdouble p46,jdouble p47
    ,jfloat p48,jdouble p49,jdouble p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54
    ,jfloat p55,jdouble p56,jfloat p57,jfloat p58,jdouble p59,jfloat p60,jdouble p61
    ,jdouble p62,jfloat p63,jfloat p64,jfloat p65,jfloat p66,jdouble p67,jfloat p68
    ,jdouble p69,jdouble p70,jfloat p71,jdouble p72,jdouble p73,jdouble p74
    ,jfloat p75,jdouble p76,jdouble p77,jfloat p78,jdouble p79,jfloat p80,jfloat p81
    ,jfloat p82,jfloat p83,jfloat p84,jfloat p85,jdouble p86,jdouble p87,jfloat p88
    ,jfloat p89,jdouble p90,jdouble p91,jdouble p92,jdouble p93,jfloat p94,jdouble p95
    ,jfloat p96,jdouble p97,jdouble p98,jfloat p99,jfloat p100,jfloat p101,jdouble p102
    ,jfloat p103,jfloat p104,jfloat p105,jdouble p106,jdouble p107,jdouble p108
    ,jfloat p109,jdouble p110,jfloat p111,jdouble p112,jfloat p113,jfloat p114
    ,jdouble p115,jdouble p116,jdouble p117,jdouble p118,jfloat p119,jdouble p120
    ,jfloat p121,jdouble p122,jfloat p123,jfloat p124,jfloat p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc11(JNIEnv *e, jclass c
,jdouble p0,jfloat p1,jfloat p2,jdouble p3,jint p4,jint p5,jfloat p6,jdouble p7
    ,jint p8,jint p9,jint p10,jint p11,jint p12,jdouble p13,jfloat p14,jfloat p15
    ,jdouble p16,jfloat p17,jint p18,jint p19,jdouble p20,jfloat p21,jint p22
    ,jint p23,jfloat p24,jint p25,jint p26,jint p27,jfloat p28,jfloat p29,jdouble p30
    ,jdouble p31,jint p32,jdouble p33,jfloat p34,jfloat p35,jfloat p36,jint p37
    ,jfloat p38,jdouble p39,jfloat p40,jdouble p41,jdouble p42,jdouble p43,jdouble p44
    ,jint p45,jint p46,jfloat p47,jfloat p48,jfloat p49,jfloat p50,jdouble p51
    ,jdouble p52,jdouble p53,jint p54,jfloat p55,jint p56,jdouble p57,jdouble p58
    ,jdouble p59,jdouble p60,jint p61,jfloat p62,jint p63,jfloat p64,jdouble p65
    ,jdouble p66,jfloat p67,jdouble p68,jdouble p69,jint p70,jdouble p71,jfloat p72
    ,jdouble p73,jint p74,jfloat p75,jfloat p76,jfloat p77,jfloat p78,jint p79
    ,jfloat p80,jint p81,jdouble p82,jfloat p83,jdouble p84,jfloat p85,jfloat p86
    ,jfloat p87,jdouble p88,jfloat p89,jint p90,jfloat p91,jdouble p92,jfloat p93
    ,jdouble p94,jdouble p95,jint p96,jint p97,jfloat p98,jdouble p99,jfloat p100
    ,jint p101,jdouble p102,jdouble p103,jfloat p104,jdouble p105,jint p106
    ,jfloat p107,jfloat p108,jint p109,jdouble p110,jfloat p111,jdouble p112
    ,jint p113,jint p114,jfloat p115,jfloat p116,jint p117,jdouble p118,jfloat p119
    ,jfloat p120,jfloat p121,jint p122,jdouble p123,jint p124,jfloat p125,jdouble p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc12(JNIEnv *e, jclass c
,jdouble p0,jfloat p1,jdouble p2,jfloat p3,jfloat p4,jbyte p5,jbyte p6,jdouble p7
    ,jdouble p8,jbyte p9,jbyte p10,jfloat p11,jbyte p12,jbyte p13,jbyte p14
    ,jbyte p15,jbyte p16,jbyte p17,jdouble p18,jdouble p19,jbyte p20,jfloat p21
    ,jbyte p22,jdouble p23,jbyte p24,jfloat p25,jdouble p26,jfloat p27,jbyte p28
    ,jbyte p29,jfloat p30,jbyte p31,jfloat p32,jfloat p33,jbyte p34,jbyte p35
    ,jfloat p36,jdouble p37,jfloat p38,jfloat p39,jdouble p40,jdouble p41,jfloat p42
    ,jfloat p43,jbyte p44,jbyte p45,jdouble p46,jfloat p47,jdouble p48,jfloat p49
    ,jbyte p50,jdouble p51,jdouble p52,jfloat p53,jdouble p54,jbyte p55,jfloat p56
    ,jbyte p57,jbyte p58,jbyte p59,jfloat p60,jdouble p61,jfloat p62,jbyte p63
    ,jdouble p64,jdouble p65,jfloat p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jdouble p71,jfloat p72,jfloat p73,jdouble p74,jbyte p75,jfloat p76,jbyte p77
    ,jbyte p78,jbyte p79,jdouble p80,jdouble p81,jbyte p82,jfloat p83,jdouble p84
    ,jbyte p85,jfloat p86,jdouble p87,jfloat p88,jfloat p89,jfloat p90,jfloat p91
    ,jdouble p92,jbyte p93,jfloat p94,jbyte p95,jbyte p96,jbyte p97,jfloat p98
    ,jbyte p99,jfloat p100,jdouble p101,jfloat p102,jbyte p103,jbyte p104,jfloat p105
    ,jdouble p106,jbyte p107,jfloat p108,jbyte p109,jdouble p110,jfloat p111
    ,jfloat p112,jbyte p113,jfloat p114,jbyte p115,jfloat p116,jdouble p117
    ,jdouble p118,jdouble p119,jfloat p120,jdouble p121,jbyte p122,jbyte p123
    ,jfloat p124,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc13(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jint p4,jfloat p5,jint p6,jfloat p7
    ,jint p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jint p14
    ,jfloat p15,jfloat p16,jint p17,jint p18,jfloat p19,jfloat p20,jint p21
    ,jint p22,jint p23,jint p24,jint p25,jfloat p26,jint p27,jint p28,jfloat p29
    ,jint p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jint p35,jint p36
    ,jfloat p37,jfloat p38,jfloat p39,jint p40,jfloat p41,jint p42,jfloat p43
    ,jint p44,jfloat p45,jfloat p46,jfloat p47,jint p48,jfloat p49,jint p50
    ,jint p51,jfloat p52,jint p53,jfloat p54,jfloat p55,jfloat p56,jfloat p57
    ,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jint p62,jfloat p63,jfloat p64
    ,jfloat p65,jint p66,jint p67,jfloat p68,jfloat p69,jfloat p70,jint p71
    ,jfloat p72,jfloat p73,jint p74,jfloat p75,jfloat p76,jfloat p77,jint p78
    ,jint p79,jint p80,jfloat p81,jint p82,jint p83,jfloat p84,jint p85,jint p86
    ,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jint p91,jint p92,jfloat p93
    ,jfloat p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98,jint p99,jfloat p100
    ,jint p101,jfloat p102,jfloat p103,jint p104,jfloat p105,jfloat p106,jfloat p107
    ,jint p108,jfloat p109,jfloat p110,jfloat p111,jfloat p112,jfloat p113,jfloat p114
    ,jfloat p115,jfloat p116,jint p117,jint p118,jfloat p119,jfloat p120,jfloat p121
    ,jint p122,jfloat p123,jint p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc14(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jbyte p4,jfloat p5,jfloat p6,jbyte p7
    ,jfloat p8,jbyte p9,jbyte p10,jbyte p11,jfloat p12,jfloat p13,jfloat p14
    ,jbyte p15,jfloat p16,jbyte p17,jfloat p18,jfloat p19,jfloat p20,jfloat p21
    ,jbyte p22,jbyte p23,jbyte p24,jfloat p25,jbyte p26,jfloat p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35
    ,jbyte p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jbyte p43,jbyte p44,jfloat p45,jbyte p46,jfloat p47,jbyte p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55,jbyte p56
    ,jfloat p57,jbyte p58,jfloat p59,jbyte p60,jfloat p61,jbyte p62,jbyte p63
    ,jfloat p64,jfloat p65,jfloat p66,jbyte p67,jfloat p68,jfloat p69,jfloat p70
    ,jbyte p71,jfloat p72,jbyte p73,jfloat p74,jfloat p75,jfloat p76,jbyte p77
    ,jfloat p78,jfloat p79,jbyte p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84
    ,jbyte p85,jfloat p86,jbyte p87,jbyte p88,jbyte p89,jfloat p90,jbyte p91
    ,jfloat p92,jfloat p93,jbyte p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98
    ,jfloat p99,jfloat p100,jfloat p101,jfloat p102,jbyte p103,jbyte p104,jfloat p105
    ,jfloat p106,jfloat p107,jfloat p108,jfloat p109,jfloat p110,jbyte p111
    ,jfloat p112,jfloat p113,jbyte p114,jbyte p115,jfloat p116,jfloat p117,jfloat p118
    ,jfloat p119,jbyte p120,jbyte p121,jbyte p122,jbyte p123,jbyte p124,jfloat p125
    ,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc15(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jbyte p2,jfloat p3,jfloat p4,jfloat p5,jbyte p6,jfloat p7
    ,jbyte p8,jint p9,jfloat p10,jfloat p11,jbyte p12,jbyte p13,jbyte p14,jbyte p15
    ,jbyte p16,jfloat p17,jbyte p18,jfloat p19,jint p20,jbyte p21,jfloat p22
    ,jint p23,jbyte p24,jfloat p25,jfloat p26,jbyte p27,jfloat p28,jint p29
    ,jfloat p30,jbyte p31,jfloat p32,jfloat p33,jbyte p34,jfloat p35,jint p36
    ,jbyte p37,jint p38,jfloat p39,jfloat p40,jbyte p41,jfloat p42,jbyte p43
    ,jint p44,jfloat p45,jfloat p46,jbyte p47,jint p48,jfloat p49,jint p50,jfloat p51
    ,jfloat p52,jint p53,jbyte p54,jint p55,jbyte p56,jfloat p57,jfloat p58
    ,jfloat p59,jfloat p60,jbyte p61,jbyte p62,jbyte p63,jbyte p64,jfloat p65
    ,jint p66,jint p67,jint p68,jfloat p69,jfloat p70,jint p71,jfloat p72,jfloat p73
    ,jfloat p74,jint p75,jfloat p76,jfloat p77,jint p78,jfloat p79,jint p80
    ,jfloat p81,jfloat p82,jfloat p83,jfloat p84,jbyte p85,jint p86,jbyte p87
    ,jfloat p88,jfloat p89,jfloat p90,jint p91,jfloat p92,jbyte p93,jfloat p94
    ,jbyte p95,jint p96,jfloat p97,jfloat p98,jint p99,jfloat p100,jbyte p101
    ,jbyte p102,jfloat p103,jbyte p104,jbyte p105,jbyte p106,jint p107,jint p108
    ,jfloat p109,jfloat p110,jint p111,jfloat p112,jbyte p113,jint p114,jfloat p115
    ,jbyte p116,jbyte p117,jfloat p118,jfloat p119,jint p120,jfloat p121,jfloat p122
    ,jint p123,jfloat p124,jbyte p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc16(JNIEnv *e, jclass c
,jdouble p0,jint p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jint p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jint p18,jint p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jint p26,jdouble p27
    ,jdouble p28,jdouble p29,jdouble p30,jint p31,jdouble p32,jint p33,jint p34
    ,jdouble p35,jdouble p36,jdouble p37,jint p38,jdouble p39,jdouble p40,jdouble p41
    ,jdouble p42,jint p43,jdouble p44,jdouble p45,jint p46,jdouble p47,jdouble p48
    ,jint p49,jdouble p50,jdouble p51,jint p52,jdouble p53,jint p54,jdouble p55
    ,jint p56,jint p57,jdouble p58,jint p59,jint p60,jdouble p61,jint p62,jint p63
    ,jint p64,jdouble p65,jint p66,jint p67,jint p68,jint p69,jint p70,jdouble p71
    ,jint p72,jint p73,jdouble p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78
    ,jdouble p79,jint p80,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85
    ,jdouble p86,jdouble p87,jdouble p88,jdouble p89,jint p90,jdouble p91,jdouble p92
    ,jdouble p93,jdouble p94,jdouble p95,jdouble p96,jdouble p97,jint p98,jdouble p99
    ,jint p100,jdouble p101,jdouble p102,jdouble p103,jint p104,jdouble p105
    ,jint p106,jdouble p107,jint p108,jint p109,jdouble p110,jdouble p111,jdouble p112
    ,jint p113,jdouble p114,jdouble p115,jint p116,jdouble p117,jint p118,jint p119
    ,jdouble p120,jdouble p121,jdouble p122,jdouble p123,jdouble p124,jdouble p125
    ,jint p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc17(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jbyte p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jbyte p7,jdouble p8,jbyte p9,jdouble p10,jbyte p11,jbyte p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jbyte p17,jdouble p18,jdouble p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jdouble p27,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32
    ,jdouble p33,jdouble p34,jdouble p35,jdouble p36,jdouble p37,jbyte p38,jdouble p39
    ,jdouble p40,jdouble p41,jdouble p42,jdouble p43,jdouble p44,jdouble p45
    ,jdouble p46,jbyte p47,jdouble p48,jbyte p49,jdouble p50,jbyte p51,jdouble p52
    ,jbyte p53,jbyte p54,jbyte p55,jbyte p56,jdouble p57,jbyte p58,jdouble p59
    ,jdouble p60,jbyte p61,jbyte p62,jdouble p63,jdouble p64,jbyte p65,jbyte p66
    ,jbyte p67,jdouble p68,jbyte p69,jdouble p70,jdouble p71,jbyte p72,jdouble p73
    ,jdouble p74,jdouble p75,jbyte p76,jdouble p77,jdouble p78,jdouble p79,jdouble p80
    ,jdouble p81,jbyte p82,jdouble p83,jdouble p84,jbyte p85,jbyte p86,jdouble p87
    ,jdouble p88,jdouble p89,jdouble p90,jdouble p91,jdouble p92,jbyte p93,jbyte p94
    ,jdouble p95,jdouble p96,jdouble p97,jdouble p98,jbyte p99,jdouble p100
    ,jdouble p101,jdouble p102,jdouble p103,jdouble p104,jbyte p105,jdouble p106
    ,jbyte p107,jdouble p108,jdouble p109,jdouble p110,jbyte p111,jdouble p112
    ,jdouble p113,jbyte p114,jdouble p115,jdouble p116,jbyte p117,jbyte p118
    ,jbyte p119,jdouble p120,jbyte p121,jdouble p122,jbyte p123,jdouble p124
    ,jbyte p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc18(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jint p4,jint p5,jbyte p6,jint p7
    ,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jbyte p13,jdouble p14
    ,jdouble p15,jbyte p16,jdouble p17,jint p18,jbyte p19,jbyte p20,jdouble p21
    ,jdouble p22,jdouble p23,jint p24,jdouble p25,jdouble p26,jint p27,jdouble p28
    ,jdouble p29,jdouble p30,jdouble p31,jdouble p32,jdouble p33,jbyte p34,jint p35
    ,jbyte p36,jdouble p37,jdouble p38,jint p39,jint p40,jint p41,jint p42,jdouble p43
    ,jdouble p44,jdouble p45,jbyte p46,jbyte p47,jdouble p48,jint p49,jbyte p50
    ,jbyte p51,jint p52,jint p53,jint p54,jdouble p55,jdouble p56,jint p57,jint p58
    ,jdouble p59,jbyte p60,jbyte p61,jint p62,jint p63,jdouble p64,jint p65
    ,jint p66,jdouble p67,jdouble p68,jdouble p69,jint p70,jdouble p71,jdouble p72
    ,jint p73,jdouble p74,jdouble p75,jint p76,jbyte p77,jdouble p78,jint p79
    ,jdouble p80,jint p81,jdouble p82,jdouble p83,jbyte p84,jdouble p85,jdouble p86
    ,jint p87,jint p88,jbyte p89,jdouble p90,jdouble p91,jdouble p92,jint p93
    ,jbyte p94,jbyte p95,jint p96,jdouble p97,jdouble p98,jdouble p99,jdouble p100
    ,jbyte p101,jbyte p102,jdouble p103,jdouble p104,jdouble p105,jint p106
    ,jint p107,jdouble p108,jint p109,jint p110,jdouble p111,jdouble p112,jbyte p113
    ,jint p114,jbyte p115,jdouble p116,jdouble p117,jint p118,jint p119,jint p120
    ,jdouble p121,jdouble p122,jdouble p123,jdouble p124,jdouble p125,jbyte p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc19(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jint p5,jfloat p6,jfloat p7
    ,jdouble p8,jdouble p9,jint p10,jdouble p11,jdouble p12,jfloat p13,jint p14
    ,jdouble p15,jfloat p16,jint p17,jfloat p18,jdouble p19,jfloat p20,jbyte p21
    ,jdouble p22,jfloat p23,jfloat p24,jdouble p25,jfloat p26,jbyte p27,jdouble p28
    ,jfloat p29,jfloat p30,jfloat p31,jbyte p32,jfloat p33,jfloat p34,jfloat p35
    ,jint p36,jint p37,jdouble p38,jdouble p39,jint p40,jbyte p41,jfloat p42
    ,jfloat p43,jbyte p44,jbyte p45,jdouble p46,jdouble p47,jint p48,jdouble p49
    ,jdouble p50,jint p51,jdouble p52,jfloat p53,jdouble p54,jdouble p55,jfloat p56
    ,jdouble p57,jdouble p58,jdouble p59,jint p60,jint p61,jfloat p62,jfloat p63
    ,jdouble p64,jdouble p65,jint p66,jbyte p67,jfloat p68,jdouble p69,jfloat p70
    ,jfloat p71,jdouble p72,jfloat p73,jbyte p74,jint p75,jint p76,jdouble p77
    ,jfloat p78,jdouble p79,jdouble p80,jdouble p81,jdouble p82,jint p83,jfloat p84
    ,jdouble p85,jbyte p86,jbyte p87,jdouble p88,jbyte p89,jbyte p90,jbyte p91
    ,jint p92,jbyte p93,jfloat p94,jbyte p95,jint p96,jint p97,jint p98,jbyte p99
    ,jdouble p100,jint p101,jdouble p102,jfloat p103,jint p104,jfloat p105,jint p106
    ,jbyte p107,jfloat p108,jint p109,jfloat p110,jfloat p111,jfloat p112,jint p113
    ,jint p114,jfloat p115,jint p116,jdouble p117,jbyte p118,jfloat p119,jbyte p120
    ,jbyte p121,jdouble p122,jfloat p123,jfloat p124,jbyte p125,jint p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc20(JNIEnv *e, jclass c
,jdouble p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jfloat p5,jfloat p6
    ,jdouble p7,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jdouble p12,jdouble p13
    ,jfloat p14,jdouble p15,jdouble p16,jdouble p17,jfloat p18,jfloat p19,jfloat p20
    ,jfloat p21,jfloat p22,jdouble p23,jdouble p24,jfloat p25,jfloat p26,jfloat p27
    ,jdouble p28,jfloat p29,jdouble p30,jfloat p31,jfloat p32,jdouble p33,jfloat p34
    ,jdouble p35,jdouble p36,jdouble p37,jdouble p38,jfloat p39,jfloat p40,jdouble p41
    ,jfloat p42,jdouble p43,jdouble p44,jfloat p45,jfloat p46,jdouble p47,jdouble p48
    ,jfloat p49,jdouble p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jdouble p55
    ,jdouble p56,jdouble p57,jfloat p58,jdouble p59,jfloat p60,jfloat p61,jfloat p62
    ,jfloat p63,jdouble p64,jfloat p65,jfloat p66,jfloat p67,jfloat p68,jfloat p69
    ,jdouble p70,jdouble p71,jfloat p72,jfloat p73,jdouble p74,jfloat p75,jfloat p76
    ,jfloat p77,jdouble p78,jfloat p79,jfloat p80,jfloat p81,jdouble p82,jfloat p83
    ,jdouble p84,jfloat p85,jdouble p86,jfloat p87,jdouble p88,jfloat p89,jfloat p90
    ,jfloat p91,jfloat p92,jdouble p93,jdouble p94,jdouble p95,jdouble p96,jfloat p97
    ,jfloat p98,jfloat p99,jfloat p100,jfloat p101,jfloat p102,jfloat p103,jfloat p104
    ,jfloat p105,jfloat p106,jdouble p107,jfloat p108,jfloat p109,jfloat p110
    ,jdouble p111,jdouble p112,jfloat p113,jfloat p114,jfloat p115,jfloat p116
    ,jdouble p117,jfloat p118,jfloat p119,jfloat p120,jdouble p121,jfloat p122
    ,jdouble p123,jdouble p124,jdouble p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc21(JNIEnv *e, jclass c
,jfloat p0,jint p1,jfloat p2,jfloat p3,jint p4,jint p5,jdouble p6,jfloat p7
    ,jfloat p8,jdouble p9,jint p10,jdouble p11,jdouble p12,jfloat p13,jdouble p14
    ,jdouble p15,jfloat p16,jdouble p17,jfloat p18,jdouble p19,jdouble p20,jdouble p21
    ,jfloat p22,jdouble p23,jdouble p24,jint p25,jdouble p26,jint p27,jint p28
    ,jdouble p29,jfloat p30,jfloat p31,jfloat p32,jint p33,jdouble p34,jfloat p35
    ,jdouble p36,jdouble p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jfloat p43,jint p44,jint p45,jdouble p46,jfloat p47,jfloat p48,jint p49
    ,jdouble p50,jdouble p51,jfloat p52,jdouble p53,jint p54,jdouble p55,jint p56
    ,jdouble p57,jdouble p58,jfloat p59,jfloat p60,jint p61,jfloat p62,jint p63
    ,jfloat p64,jdouble p65,jint p66,jfloat p67,jdouble p68,jdouble p69,jdouble p70
    ,jdouble p71,jdouble p72,jfloat p73,jint p74,jint p75,jdouble p76,jint p77
    ,jfloat p78,jfloat p79,jdouble p80,jdouble p81,jdouble p82,jint p83,jdouble p84
    ,jdouble p85,jfloat p86,jdouble p87,jint p88,jdouble p89,jfloat p90,jfloat p91
    ,jfloat p92,jdouble p93,jint p94,jfloat p95,jint p96,jfloat p97,jdouble p98
    ,jint p99,jint p100,jint p101,jint p102,jfloat p103,jfloat p104,jfloat p105
    ,jfloat p106,jint p107,jfloat p108,jdouble p109,jfloat p110,jdouble p111
    ,jdouble p112,jfloat p113,jfloat p114,jdouble p115,jdouble p116,jfloat p117
    ,jint p118,jdouble p119,jfloat p120,jfloat p121,jdouble p122,jdouble p123
    ,jfloat p124,jint p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc22(JNIEnv *e, jclass c
,jdouble p0,jbyte p1,jdouble p2,jfloat p3,jfloat p4,jfloat p5,jfloat p6
    ,jfloat p7,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jdouble p12,jdouble p13
    ,jbyte p14,jfloat p15,jfloat p16,jdouble p17,jfloat p18,jfloat p19,jdouble p20
    ,jfloat p21,jbyte p22,jfloat p23,jfloat p24,jdouble p25,jbyte p26,jdouble p27
    ,jdouble p28,jdouble p29,jbyte p30,jbyte p31,jbyte p32,jbyte p33,jbyte p34
    ,jdouble p35,jdouble p36,jfloat p37,jfloat p38,jdouble p39,jfloat p40,jfloat p41
    ,jfloat p42,jfloat p43,jbyte p44,jfloat p45,jfloat p46,jdouble p47,jbyte p48
    ,jbyte p49,jdouble p50,jdouble p51,jfloat p52,jfloat p53,jdouble p54,jfloat p55
    ,jdouble p56,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jfloat p62
    ,jdouble p63,jdouble p64,jfloat p65,jdouble p66,jdouble p67,jbyte p68,jdouble p69
    ,jdouble p70,jdouble p71,jdouble p72,jfloat p73,jbyte p74,jfloat p75,jfloat p76
    ,jfloat p77,jfloat p78,jfloat p79,jdouble p80,jdouble p81,jfloat p82,jdouble p83
    ,jbyte p84,jfloat p85,jdouble p86,jfloat p87,jdouble p88,jbyte p89,jbyte p90
    ,jdouble p91,jbyte p92,jfloat p93,jdouble p94,jbyte p95,jfloat p96,jfloat p97
    ,jdouble p98,jfloat p99,jbyte p100,jfloat p101,jdouble p102,jdouble p103
    ,jbyte p104,jdouble p105,jfloat p106,jbyte p107,jfloat p108,jbyte p109,jfloat p110
    ,jfloat p111,jdouble p112,jfloat p113,jdouble p114,jdouble p115,jfloat p116
    ,jdouble p117,jfloat p118,jfloat p119,jfloat p120,jbyte p121,jdouble p122
    ,jdouble p123,jdouble p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc23(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jfloat p5,jint p6,jfloat p7
    ,jfloat p8,jfloat p9,jfloat p10,jint p11,jfloat p12,jfloat p13,jfloat p14
    ,jint p15,jfloat p16,jfloat p17,jfloat p18,jint p19,jfloat p20,jfloat p21
    ,jfloat p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27,jfloat p28
    ,jfloat p29,jint p30,jfloat p31,jfloat p32,jfloat p33,jint p34,jint p35
    ,jint p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jint p43,jfloat p44,jfloat p45,jfloat p46,jfloat p47,jint p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jint p55,jfloat p56
    ,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jint p61,jfloat p62,jfloat p63
    ,jint p64,jfloat p65,jint p66,jint p67,jint p68,jfloat p69,jfloat p70,jfloat p71
    ,jfloat p72,jint p73,jfloat p74,jfloat p75,jfloat p76,jfloat p77,jint p78
    ,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84,jfloat p85
    ,jint p86,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jint p91,jint p92
    ,jfloat p93,jint p94,jfloat p95,jint p96,jfloat p97,jfloat p98,jfloat p99
    ,jfloat p100,jfloat p101,jfloat p102,jfloat p103,jfloat p104,jfloat p105
    ,jfloat p106,jint p107,jfloat p108,jfloat p109,jfloat p110,jfloat p111,jfloat p112
    ,jint p113,jfloat p114,jfloat p115,jfloat p116,jfloat p117,jfloat p118,jint p119
    ,jfloat p120,jfloat p121,jint p122,jint p123,jfloat p124,jfloat p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc24(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jbyte p2,jbyte p3,jfloat p4,jfloat p5,jbyte p6,jbyte p7
    ,jbyte p8,jbyte p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jfloat p14
    ,jfloat p15,jfloat p16,jfloat p17,jfloat p18,jbyte p19,jfloat p20,jfloat p21
    ,jfloat p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jbyte p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35
    ,jfloat p36,jbyte p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jbyte p42
    ,jfloat p43,jfloat p44,jfloat p45,jbyte p46,jbyte p47,jfloat p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jfloat p53,jbyte p54,jfloat p55,jbyte p56
    ,jbyte p57,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jfloat p62,jbyte p63
    ,jbyte p64,jfloat p65,jfloat p66,jfloat p67,jbyte p68,jfloat p69,jfloat p70
    ,jfloat p71,jfloat p72,jfloat p73,jbyte p74,jfloat p75,jfloat p76,jfloat p77
    ,jfloat p78,jfloat p79,jbyte p80,jfloat p81,jfloat p82,jbyte p83,jfloat p84
    ,jbyte p85,jfloat p86,jbyte p87,jfloat p88,jbyte p89,jbyte p90,jfloat p91
    ,jfloat p92,jfloat p93,jfloat p94,jbyte p95,jbyte p96,jfloat p97,jbyte p98
    ,jfloat p99,jfloat p100,jfloat p101,jfloat p102,jfloat p103,jfloat p104
    ,jfloat p105,jbyte p106,jfloat p107,jfloat p108,jfloat p109,jfloat p110
    ,jbyte p111,jfloat p112,jfloat p113,jfloat p114,jfloat p115,jfloat p116
    ,jfloat p117,jfloat p118,jfloat p119,jfloat p120,jfloat p121,jfloat p122
    ,jfloat p123,jfloat p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc25(JNIEnv *e, jclass c
,jfloat p0,jint p1,jfloat p2,jfloat p3,jfloat p4,jint p5,jint p6,jbyte p7
    ,jfloat p8,jint p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jint p14
    ,jbyte p15,jfloat p16,jfloat p17,jfloat p18,jbyte p19,jfloat p20,jbyte p21
    ,jfloat p22,jbyte p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27,jfloat p28
    ,jint p29,jbyte p30,jfloat p31,jbyte p32,jfloat p33,jint p34,jint p35,jbyte p36
    ,jbyte p37,jint p38,jint p39,jfloat p40,jint p41,jfloat p42,jfloat p43,jfloat p44
    ,jfloat p45,jfloat p46,jint p47,jfloat p48,jint p49,jint p50,jint p51,jfloat p52
    ,jfloat p53,jbyte p54,jint p55,jbyte p56,jint p57,jfloat p58,jint p59,jint p60
    ,jbyte p61,jbyte p62,jint p63,jfloat p64,jfloat p65,jbyte p66,jbyte p67
    ,jbyte p68,jint p69,jbyte p70,jfloat p71,jbyte p72,jfloat p73,jfloat p74
    ,jfloat p75,jfloat p76,jbyte p77,jfloat p78,jint p79,jbyte p80,jfloat p81
    ,jfloat p82,jint p83,jfloat p84,jbyte p85,jbyte p86,jfloat p87,jfloat p88
    ,jbyte p89,jfloat p90,jfloat p91,jint p92,jfloat p93,jfloat p94,jint p95
    ,jfloat p96,jint p97,jint p98,jint p99,jint p100,jint p101,jint p102,jint p103
    ,jfloat p104,jint p105,jbyte p106,jfloat p107,jfloat p108,jint p109,jbyte p110
    ,jfloat p111,jfloat p112,jfloat p113,jbyte p114,jfloat p115,jbyte p116,jfloat p117
    ,jbyte p118,jfloat p119,jfloat p120,jfloat p121,jbyte p122,jfloat p123,jbyte p124
    ,jfloat p125,jint p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc26(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jint p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jint p15,jdouble p16,jdouble p17,jint p18,jdouble p19,jdouble p20
    ,jint p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26,jdouble p27
    ,jdouble p28,jdouble p29,jdouble p30,jint p31,jint p32,jdouble p33,jdouble p34
    ,jdouble p35,jdouble p36,jdouble p37,jdouble p38,jdouble p39,jdouble p40
    ,jdouble p41,jdouble p42,jint p43,jdouble p44,jdouble p45,jint p46,jdouble p47
    ,jint p48,jdouble p49,jdouble p50,jdouble p51,jdouble p52,jint p53,jdouble p54
    ,jdouble p55,jdouble p56,jint p57,jint p58,jdouble p59,jint p60,jdouble p61
    ,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jdouble p66,jdouble p67
    ,jdouble p68,jdouble p69,jint p70,jint p71,jdouble p72,jdouble p73,jint p74
    ,jint p75,jint p76,jdouble p77,jdouble p78,jdouble p79,jdouble p80,jdouble p81
    ,jdouble p82,jdouble p83,jdouble p84,jdouble p85,jint p86,jdouble p87,jdouble p88
    ,jint p89,jdouble p90,jdouble p91,jdouble p92,jdouble p93,jint p94,jdouble p95
    ,jdouble p96,jint p97,jdouble p98,jdouble p99,jdouble p100,jdouble p101
    ,jdouble p102,jdouble p103,jdouble p104,jdouble p105,jdouble p106,jdouble p107
    ,jdouble p108,jdouble p109,jint p110,jint p111,jdouble p112,jint p113,jdouble p114
    ,jdouble p115,jdouble p116,jdouble p117,jdouble p118,jdouble p119,jint p120
    ,jint p121,jint p122,jdouble p123,jdouble p124,jdouble p125,jint p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc27(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jbyte p12,jbyte p13
    ,jbyte p14,jdouble p15,jdouble p16,jbyte p17,jdouble p18,jdouble p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jdouble p27,jdouble p28,jdouble p29,jbyte p30,jdouble p31,jdouble p32,jbyte p33
    ,jdouble p34,jdouble p35,jdouble p36,jdouble p37,jdouble p38,jdouble p39
    ,jdouble p40,jbyte p41,jbyte p42,jdouble p43,jbyte p44,jbyte p45,jdouble p46
    ,jdouble p47,jdouble p48,jbyte p49,jbyte p50,jbyte p51,jdouble p52,jbyte p53
    ,jdouble p54,jdouble p55,jdouble p56,jdouble p57,jbyte p58,jbyte p59,jdouble p60
    ,jdouble p61,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jdouble p66
    ,jdouble p67,jbyte p68,jdouble p69,jdouble p70,jbyte p71,jdouble p72,jdouble p73
    ,jdouble p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79
    ,jdouble p80,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85
    ,jdouble p86,jdouble p87,jbyte p88,jdouble p89,jbyte p90,jbyte p91,jbyte p92
    ,jdouble p93,jbyte p94,jbyte p95,jdouble p96,jbyte p97,jdouble p98,jdouble p99
    ,jdouble p100,jdouble p101,jdouble p102,jdouble p103,jdouble p104,jdouble p105
    ,jdouble p106,jdouble p107,jdouble p108,jdouble p109,jdouble p110,jdouble p111
    ,jdouble p112,jdouble p113,jdouble p114,jbyte p115,jdouble p116,jdouble p117
    ,jbyte p118,jdouble p119,jdouble p120,jbyte p121,jdouble p122,jdouble p123
    ,jdouble p124,jbyte p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc28(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jbyte p2,jdouble p3,jdouble p4,jdouble p5,jbyte p6
    ,jint p7,jbyte p8,jint p9,jint p10,jbyte p11,jdouble p12,jint p13,jdouble p14
    ,jdouble p15,jbyte p16,jdouble p17,jbyte p18,jdouble p19,jdouble p20,jbyte p21
    ,jdouble p22,jbyte p23,jdouble p24,jint p25,jdouble p26,jdouble p27,jbyte p28
    ,jdouble p29,jint p30,jint p31,jdouble p32,jdouble p33,jbyte p34,jbyte p35
    ,jbyte p36,jbyte p37,jdouble p38,jdouble p39,jdouble p40,jdouble p41,jint p42
    ,jdouble p43,jdouble p44,jdouble p45,jdouble p46,jdouble p47,jdouble p48
    ,jdouble p49,jint p50,jdouble p51,jdouble p52,jbyte p53,jint p54,jint p55
    ,jdouble p56,jdouble p57,jint p58,jdouble p59,jdouble p60,jdouble p61,jdouble p62
    ,jbyte p63,jint p64,jbyte p65,jdouble p66,jdouble p67,jint p68,jdouble p69
    ,jdouble p70,jdouble p71,jdouble p72,jbyte p73,jdouble p74,jdouble p75,jdouble p76
    ,jbyte p77,jbyte p78,jdouble p79,jdouble p80,jdouble p81,jdouble p82,jdouble p83
    ,jbyte p84,jdouble p85,jdouble p86,jbyte p87,jdouble p88,jbyte p89,jdouble p90
    ,jdouble p91,jint p92,jbyte p93,jdouble p94,jdouble p95,jdouble p96,jdouble p97
    ,jdouble p98,jdouble p99,jbyte p100,jbyte p101,jdouble p102,jdouble p103
    ,jbyte p104,jdouble p105,jdouble p106,jbyte p107,jint p108,jbyte p109,jbyte p110
    ,jbyte p111,jdouble p112,jint p113,jdouble p114,jbyte p115,jint p116,jdouble p117
    ,jdouble p118,jbyte p119,jbyte p120,jint p121,jdouble p122,jint p123,jbyte p124
    ,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc29(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jdouble p2,jdouble p3,jfloat p4,jfloat p5,jint p6,jdouble p7
    ,jfloat p8,jbyte p9,jfloat p10,jfloat p11,jfloat p12,jdouble p13,jdouble p14
    ,jint p15,jbyte p16,jfloat p17,jfloat p18,jbyte p19,jint p20,jdouble p21
    ,jfloat p22,jfloat p23,jint p24,jfloat p25,jfloat p26,jbyte p27,jdouble p28
    ,jfloat p29,jint p30,jint p31,jfloat p32,jfloat p33,jfloat p34,jdouble p35
    ,jdouble p36,jfloat p37,jdouble p38,jfloat p39,jfloat p40,jfloat p41,jdouble p42
    ,jfloat p43,jdouble p44,jdouble p45,jdouble p46,jint p47,jdouble p48,jfloat p49
    ,jbyte p50,jdouble p51,jbyte p52,jdouble p53,jfloat p54,jdouble p55,jfloat p56
    ,jbyte p57,jfloat p58,jdouble p59,jfloat p60,jfloat p61,jdouble p62,jfloat p63
    ,jdouble p64,jbyte p65,jfloat p66,jfloat p67,jint p68,jfloat p69,jdouble p70
    ,jdouble p71,jfloat p72,jdouble p73,jfloat p74,jfloat p75,jbyte p76,jfloat p77
    ,jdouble p78,jdouble p79,jfloat p80,jdouble p81,jdouble p82,jbyte p83,jdouble p84
    ,jint p85,jfloat p86,jint p87,jdouble p88,jfloat p89,jbyte p90,jbyte p91
    ,jfloat p92,jbyte p93,jdouble p94,jfloat p95,jint p96,jdouble p97,jdouble p98
    ,jfloat p99,jdouble p100,jfloat p101,jfloat p102,jdouble p103,jfloat p104
    ,jint p105,jdouble p106,jint p107,jdouble p108,jdouble p109,jfloat p110
    ,jdouble p111,jfloat p112,jdouble p113,jdouble p114,jint p115,jfloat p116
    ,jfloat p117,jint p118,jfloat p119,jdouble p120,jfloat p121,jbyte p122,jfloat p123
    ,jfloat p124,jfloat p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc30(JNIEnv *e, jclass c
,jfloat p0,jdouble p1,jfloat p2,jfloat p3,jfloat p4,jfloat p5,jdouble p6
    ,jfloat p7,jdouble p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13
    ,jfloat p14,jfloat p15,jdouble p16,jfloat p17,jdouble p18,jfloat p19,jdouble p20
    ,jdouble p21,jdouble p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27
    ,jfloat p28,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jdouble p34
    ,jfloat p35,jdouble p36,jfloat p37,jdouble p38,jfloat p39,jfloat p40,jfloat p41
    ,jdouble p42,jfloat p43,jfloat p44,jfloat p45,jfloat p46,jdouble p47,jdouble p48
    ,jdouble p49,jfloat p50,jdouble p51,jdouble p52,jfloat p53,jfloat p54,jfloat p55
    ,jdouble p56,jdouble p57,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jfloat p62
    ,jfloat p63,jfloat p64,jfloat p65,jfloat p66,jfloat p67,jfloat p68,jdouble p69
    ,jfloat p70,jfloat p71,jfloat p72,jfloat p73,jfloat p74,jfloat p75,jfloat p76
    ,jdouble p77,jfloat p78,jfloat p79,jfloat p80,jdouble p81,jfloat p82,jfloat p83
    ,jfloat p84,jdouble p85,jdouble p86,jdouble p87,jfloat p88,jfloat p89,jfloat p90
    ,jfloat p91,jfloat p92,jdouble p93,jfloat p94,jfloat p95,jfloat p96,jfloat p97
    ,jfloat p98,jdouble p99,jdouble p100,jfloat p101,jfloat p102,jdouble p103
    ,jdouble p104,jfloat p105,jdouble p106,jfloat p107,jfloat p108,jfloat p109
    ,jfloat p110,jdouble p111,jfloat p112,jfloat p113,jfloat p114,jfloat p115
    ,jfloat p116,jfloat p117,jfloat p118,jfloat p119,jfloat p120,jfloat p121
    ,jfloat p122,jfloat p123,jdouble p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc31(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jfloat p2,jfloat p3,jfloat p4,jdouble p5,jfloat p6
    ,jfloat p7,jdouble p8,jdouble p9,jint p10,jdouble p11,jdouble p12,jfloat p13
    ,jint p14,jdouble p15,jfloat p16,jfloat p17,jfloat p18,jfloat p19,jfloat p20
    ,jint p21,jfloat p22,jdouble p23,jint p24,jfloat p25,jdouble p26,jdouble p27
    ,jdouble p28,jfloat p29,jdouble p30,jfloat p31,jdouble p32,jfloat p33,jfloat p34
    ,jfloat p35,jdouble p36,jfloat p37,jdouble p38,jdouble p39,jint p40,jint p41
    ,jdouble p42,jfloat p43,jfloat p44,jfloat p45,jdouble p46,jdouble p47,jfloat p48
    ,jfloat p49,jdouble p50,jdouble p51,jdouble p52,jfloat p53,jint p54,jdouble p55
    ,jfloat p56,jdouble p57,jfloat p58,jfloat p59,jfloat p60,jdouble p61,jfloat p62
    ,jint p63,jdouble p64,jint p65,jint p66,jdouble p67,jdouble p68,jdouble p69
    ,jfloat p70,jfloat p71,jdouble p72,jint p73,jint p74,jfloat p75,jdouble p76
    ,jfloat p77,jfloat p78,jfloat p79,jfloat p80,jdouble p81,jdouble p82,jdouble p83
    ,jdouble p84,jdouble p85,jfloat p86,jfloat p87,jint p88,jdouble p89,jdouble p90
    ,jfloat p91,jdouble p92,jfloat p93,jint p94,jfloat p95,jfloat p96,jdouble p97
    ,jint p98,jint p99,jdouble p100,jdouble p101,jfloat p102,jfloat p103,jdouble p104
    ,jdouble p105,jdouble p106,jdouble p107,jdouble p108,jfloat p109,jfloat p110
    ,jdouble p111,jfloat p112,jdouble p113,jdouble p114,jfloat p115,jfloat p116
    ,jfloat p117,jfloat p118,jdouble p119,jint p120,jdouble p121,jfloat p122
    ,jdouble p123,jdouble p124,jdouble p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc32(JNIEnv *e, jclass c
,jdouble p0,jfloat p1,jbyte p2,jfloat p3,jdouble p4,jbyte p5,jfloat p6,jfloat p7
    ,jdouble p8,jfloat p9,jfloat p10,jfloat p11,jdouble p12,jfloat p13,jfloat p14
    ,jdouble p15,jfloat p16,jbyte p17,jfloat p18,jfloat p19,jfloat p20,jfloat p21
    ,jbyte p22,jbyte p23,jbyte p24,jfloat p25,jfloat p26,jdouble p27,jbyte p28
    ,jdouble p29,jfloat p30,jdouble p31,jdouble p32,jfloat p33,jbyte p34,jfloat p35
    ,jfloat p36,jdouble p37,jfloat p38,jdouble p39,jfloat p40,jdouble p41,jbyte p42
    ,jdouble p43,jfloat p44,jdouble p45,jbyte p46,jfloat p47,jbyte p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jdouble p53,jdouble p54,jdouble p55,jfloat p56
    ,jdouble p57,jdouble p58,jfloat p59,jdouble p60,jdouble p61,jdouble p62
    ,jdouble p63,jfloat p64,jdouble p65,jdouble p66,jdouble p67,jbyte p68,jdouble p69
    ,jfloat p70,jdouble p71,jbyte p72,jdouble p73,jdouble p74,jbyte p75,jdouble p76
    ,jdouble p77,jdouble p78,jbyte p79,jdouble p80,jdouble p81,jfloat p82,jdouble p83
    ,jfloat p84,jdouble p85,jbyte p86,jdouble p87,jdouble p88,jfloat p89,jdouble p90
    ,jdouble p91,jdouble p92,jdouble p93,jbyte p94,jfloat p95,jdouble p96,jdouble p97
    ,jfloat p98,jdouble p99,jfloat p100,jdouble p101,jfloat p102,jfloat p103
    ,jbyte p104,jdouble p105,jdouble p106,jdouble p107,jfloat p108,jfloat p109
    ,jfloat p110,jbyte p111,jdouble p112,jbyte p113,jdouble p114,jdouble p115
    ,jfloat p116,jfloat p117,jfloat p118,jdouble p119,jdouble p120,jdouble p121
    ,jfloat p122,jfloat p123,jfloat p124,jbyte p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc33(JNIEnv *e, jclass c
,jfloat p0,jint p1,jint p2,jint p3,jfloat p4,jfloat p5,jfloat p6,jint p7
    ,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jfloat p14
    ,jfloat p15,jfloat p16,jint p17,jfloat p18,jfloat p19,jfloat p20,jfloat p21
    ,jint p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35
    ,jfloat p36,jfloat p37,jint p38,jint p39,jint p40,jfloat p41,jfloat p42
    ,jfloat p43,jfloat p44,jint p45,jfloat p46,jfloat p47,jint p48,jfloat p49
    ,jfloat p50,jfloat p51,jfloat p52,jint p53,jfloat p54,jfloat p55,jfloat p56
    ,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jint p61,jfloat p62,jfloat p63
    ,jfloat p64,jfloat p65,jint p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jint p71,jfloat p72,jfloat p73,jfloat p74,jfloat p75,jfloat p76,jfloat p77
    ,jfloat p78,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84
    ,jfloat p85,jfloat p86,jfloat p87,jfloat p88,jint p89,jint p90,jfloat p91
    ,jfloat p92,jfloat p93,jint p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98
    ,jfloat p99,jfloat p100,jint p101,jfloat p102,jfloat p103,jfloat p104,jint p105
    ,jint p106,jfloat p107,jint p108,jfloat p109,jfloat p110,jfloat p111,jfloat p112
    ,jfloat p113,jfloat p114,jint p115,jint p116,jfloat p117,jfloat p118,jint p119
    ,jfloat p120,jfloat p121,jfloat p122,jfloat p123,jfloat p124,jfloat p125
    ,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%d\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc34(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jbyte p5,jfloat p6,jfloat p7
    ,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jbyte p12,jfloat p13,jfloat p14
    ,jbyte p15,jfloat p16,jfloat p17,jbyte p18,jbyte p19,jfloat p20,jfloat p21
    ,jbyte p22,jfloat p23,jfloat p24,jbyte p25,jbyte p26,jbyte p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35
    ,jbyte p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jfloat p43,jfloat p44,jbyte p45,jfloat p46,jbyte p47,jfloat p48,jbyte p49
    ,jfloat p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55,jfloat p56
    ,jbyte p57,jfloat p58,jfloat p59,jfloat p60,jfloat p61,jbyte p62,jfloat p63
    ,jfloat p64,jfloat p65,jfloat p66,jfloat p67,jbyte p68,jbyte p69,jfloat p70
    ,jfloat p71,jfloat p72,jfloat p73,jfloat p74,jbyte p75,jfloat p76,jfloat p77
    ,jbyte p78,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84
    ,jfloat p85,jfloat p86,jbyte p87,jbyte p88,jfloat p89,jfloat p90,jfloat p91
    ,jfloat p92,jfloat p93,jfloat p94,jfloat p95,jfloat p96,jbyte p97,jfloat p98
    ,jfloat p99,jfloat p100,jfloat p101,jbyte p102,jfloat p103,jfloat p104,jbyte p105
    ,jfloat p106,jfloat p107,jfloat p108,jfloat p109,jfloat p110,jfloat p111
    ,jfloat p112,jbyte p113,jfloat p114,jfloat p115,jfloat p116,jfloat p117
    ,jfloat p118,jfloat p119,jfloat p120,jfloat p121,jfloat p122,jfloat p123
    ,jfloat p124,jbyte p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc35(JNIEnv *e, jclass c
,jint p0,jbyte p1,jbyte p2,jfloat p3,jfloat p4,jfloat p5,jfloat p6,jfloat p7
    ,jint p8,jfloat p9,jint p10,jfloat p11,jfloat p12,jbyte p13,jbyte p14,jfloat p15
    ,jfloat p16,jbyte p17,jbyte p18,jfloat p19,jfloat p20,jfloat p21,jfloat p22
    ,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jint p27,jfloat p28,jfloat p29
    ,jfloat p30,jbyte p31,jfloat p32,jfloat p33,jfloat p34,jfloat p35,jbyte p36
    ,jfloat p37,jint p38,jfloat p39,jbyte p40,jbyte p41,jfloat p42,jfloat p43
    ,jfloat p44,jint p45,jfloat p46,jbyte p47,jbyte p48,jint p49,jfloat p50
    ,jint p51,jfloat p52,jfloat p53,jfloat p54,jbyte p55,jbyte p56,jfloat p57
    ,jfloat p58,jfloat p59,jint p60,jint p61,jfloat p62,jint p63,jbyte p64,jfloat p65
    ,jint p66,jfloat p67,jfloat p68,jbyte p69,jfloat p70,jfloat p71,jfloat p72
    ,jint p73,jfloat p74,jfloat p75,jfloat p76,jbyte p77,jfloat p78,jfloat p79
    ,jfloat p80,jbyte p81,jfloat p82,jbyte p83,jfloat p84,jfloat p85,jbyte p86
    ,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jbyte p91,jfloat p92,jfloat p93
    ,jfloat p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98,jint p99,jfloat p100
    ,jfloat p101,jint p102,jfloat p103,jfloat p104,jbyte p105,jfloat p106,jint p107
    ,jfloat p108,jfloat p109,jfloat p110,jfloat p111,jfloat p112,jint p113,jfloat p114
    ,jfloat p115,jfloat p116,jfloat p117,jbyte p118,jfloat p119,jbyte p120,jfloat p121
    ,jfloat p122,jint p123,jfloat p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%d\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%d\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%d\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc36(JNIEnv *e, jclass c
,jint p0,jint p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jint p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jint p18,jint p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jdouble p27,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32
    ,jdouble p33,jdouble p34,jint p35,jdouble p36,jdouble p37,jdouble p38,jdouble p39
    ,jdouble p40,jdouble p41,jdouble p42,jdouble p43,jint p44,jdouble p45,jdouble p46
    ,jint p47,jint p48,jdouble p49,jdouble p50,jdouble p51,jdouble p52,jdouble p53
    ,jdouble p54,jdouble p55,jint p56,jdouble p57,jdouble p58,jdouble p59,jdouble p60
    ,jdouble p61,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jdouble p66
    ,jdouble p67,jdouble p68,jdouble p69,jint p70,jdouble p71,jint p72,jdouble p73
    ,jdouble p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79
    ,jint p80,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85,jdouble p86
    ,jdouble p87,jdouble p88,jdouble p89,jdouble p90,jdouble p91,jint p92,jdouble p93
    ,jdouble p94,jdouble p95,jdouble p96,jdouble p97,jdouble p98,jint p99,jint p100
    ,jint p101,jint p102,jint p103,jint p104,jint p105,jint p106,jdouble p107
    ,jdouble p108,jdouble p109,jint p110,jdouble p111,jdouble p112,jdouble p113
    ,jdouble p114,jdouble p115,jdouble p116,jdouble p117,jdouble p118,jdouble p119
    ,jdouble p120,jdouble p121,jdouble p122,jdouble p123,jdouble p124,jdouble p125
    ,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%d\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%d\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%d\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%d\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%d\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc37(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jbyte p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jdouble p19
    ,jdouble p20,jdouble p21,jbyte p22,jbyte p23,jdouble p24,jdouble p25,jbyte p26
    ,jdouble p27,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32
    ,jdouble p33,jdouble p34,jdouble p35,jdouble p36,jdouble p37,jbyte p38,jbyte p39
    ,jdouble p40,jdouble p41,jdouble p42,jdouble p43,jdouble p44,jbyte p45,jdouble p46
    ,jdouble p47,jdouble p48,jbyte p49,jdouble p50,jdouble p51,jdouble p52,jdouble p53
    ,jdouble p54,jdouble p55,jbyte p56,jbyte p57,jdouble p58,jdouble p59,jdouble p60
    ,jdouble p61,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jbyte p66,jdouble p67
    ,jdouble p68,jdouble p69,jdouble p70,jbyte p71,jdouble p72,jdouble p73,jdouble p74
    ,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79,jdouble p80
    ,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85,jdouble p86
    ,jdouble p87,jdouble p88,jdouble p89,jdouble p90,jdouble p91,jdouble p92
    ,jdouble p93,jdouble p94,jdouble p95,jbyte p96,jbyte p97,jdouble p98,jdouble p99
    ,jdouble p100,jdouble p101,jdouble p102,jdouble p103,jdouble p104,jdouble p105
    ,jdouble p106,jdouble p107,jdouble p108,jbyte p109,jdouble p110,jdouble p111
    ,jdouble p112,jdouble p113,jdouble p114,jdouble p115,jdouble p116,jdouble p117
    ,jbyte p118,jdouble p119,jdouble p120,jdouble p121,jdouble p122,jdouble p123
    ,jbyte p124,jdouble p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc38(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jint p3,jint p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jint p11,jdouble p12,jdouble p13
    ,jint p14,jbyte p15,jdouble p16,jdouble p17,jdouble p18,jdouble p19,jint p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jint p25,jdouble p26,jint p27
    ,jbyte p28,jdouble p29,jbyte p30,jdouble p31,jdouble p32,jint p33,jdouble p34
    ,jint p35,jbyte p36,jbyte p37,jdouble p38,jdouble p39,jdouble p40,jdouble p41
    ,jint p42,jdouble p43,jdouble p44,jdouble p45,jdouble p46,jdouble p47,jint p48
    ,jdouble p49,jint p50,jint p51,jdouble p52,jbyte p53,jdouble p54,jdouble p55
    ,jbyte p56,jdouble p57,jdouble p58,jdouble p59,jdouble p60,jdouble p61,jbyte p62
    ,jdouble p63,jdouble p64,jdouble p65,jbyte p66,jint p67,jdouble p68,jdouble p69
    ,jdouble p70,jdouble p71,jdouble p72,jbyte p73,jdouble p74,jdouble p75,jdouble p76
    ,jbyte p77,jdouble p78,jdouble p79,jdouble p80,jdouble p81,jdouble p82,jint p83
    ,jdouble p84,jdouble p85,jdouble p86,jint p87,jdouble p88,jint p89,jdouble p90
    ,jdouble p91,jdouble p92,jdouble p93,jdouble p94,jint p95,jbyte p96,jbyte p97
    ,jdouble p98,jint p99,jdouble p100,jdouble p101,jdouble p102,jdouble p103
    ,jdouble p104,jdouble p105,jdouble p106,jdouble p107,jdouble p108,jdouble p109
    ,jdouble p110,jdouble p111,jint p112,jdouble p113,jdouble p114,jdouble p115
    ,jdouble p116,jbyte p117,jdouble p118,jdouble p119,jbyte p120,jint p121
    ,jdouble p122,jint p123,jdouble p124,jdouble p125,jbyte p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%d\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%d\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%d\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%d\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%d\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc39(JNIEnv *e, jclass c
,jdouble p0,jfloat p1,jdouble p2,jbyte p3,jdouble p4,jint p5,jdouble p6
    ,jfloat p7,jfloat p8,jdouble p9,jint p10,jbyte p11,jfloat p12,jdouble p13
    ,jint p14,jint p15,jfloat p16,jbyte p17,jdouble p18,jdouble p19,jfloat p20
    ,jfloat p21,jdouble p22,jfloat p23,jfloat p24,jdouble p25,jfloat p26,jfloat p27
    ,jfloat p28,jfloat p29,jfloat p30,jdouble p31,jdouble p32,jdouble p33,jfloat p34
    ,jdouble p35,jfloat p36,jdouble p37,jfloat p38,jfloat p39,jfloat p40,jint p41
    ,jbyte p42,jdouble p43,jfloat p44,jfloat p45,jint p46,jbyte p47,jfloat p48
    ,jdouble p49,jdouble p50,jfloat p51,jfloat p52,jfloat p53,jdouble p54,jint p55
    ,jfloat p56,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jdouble p61,jdouble p62
    ,jfloat p63,jfloat p64,jfloat p65,jdouble p66,jfloat p67,jdouble p68,jdouble p69
    ,jdouble p70,jdouble p71,jdouble p72,jdouble p73,jdouble p74,jfloat p75
    ,jint p76,jfloat p77,jdouble p78,jdouble p79,jdouble p80,jdouble p81,jbyte p82
    ,jfloat p83,jfloat p84,jfloat p85,jfloat p86,jdouble p87,jfloat p88,jfloat p89
    ,jfloat p90,jfloat p91,jfloat p92,jdouble p93,jfloat p94,jdouble p95,jfloat p96
    ,jdouble p97,jfloat p98,jfloat p99,jdouble p100,jdouble p101,jfloat p102
    ,jfloat p103,jdouble p104,jdouble p105,jfloat p106,jbyte p107,jdouble p108
    ,jfloat p109,jfloat p110,jdouble p111,jdouble p112,jdouble p113,jdouble p114
    ,jdouble p115,jdouble p116,jfloat p117,jdouble p118,jfloat p119,jfloat p120
    ,jfloat p121,jfloat p122,jfloat p123,jdouble p124,jfloat p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%d\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%d\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%d\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc40(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jfloat p2,jdouble p3,jdouble p4,jfloat p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jdouble p11,jdouble p12,jfloat p13
    ,jfloat p14,jdouble p15,jfloat p16,jfloat p17,jdouble p18,jfloat p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jfloat p27,jfloat p28,jdouble p29,jdouble p30,jdouble p31,jfloat p32,jfloat p33
    ,jfloat p34,jfloat p35,jdouble p36,jfloat p37,jfloat p38,jfloat p39,jdouble p40
    ,jfloat p41,jdouble p42,jdouble p43,jfloat p44,jdouble p45,jdouble p46,jdouble p47
    ,jfloat p48,jdouble p49,jdouble p50,jdouble p51,jdouble p52,jfloat p53,jdouble p54
    ,jdouble p55,jdouble p56,jdouble p57,jdouble p58,jdouble p59,jdouble p60
    ,jdouble p61,jdouble p62,jfloat p63,jfloat p64,jfloat p65,jfloat p66,jfloat p67
    ,jdouble p68,jdouble p69,jdouble p70,jdouble p71,jdouble p72,jdouble p73
    ,jfloat p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79
    ,jfloat p80,jdouble p81,jdouble p82,jfloat p83,jdouble p84,jdouble p85,jdouble p86
    ,jfloat p87,jfloat p88,jdouble p89,jfloat p90,jdouble p91,jfloat p92,jfloat p93
    ,jfloat p94,jfloat p95,jdouble p96,jfloat p97,jfloat p98,jdouble p99,jdouble p100
    ,jdouble p101,jdouble p102,jfloat p103,jdouble p104,jdouble p105,jdouble p106
    ,jdouble p107,jdouble p108,jdouble p109,jfloat p110,jfloat p111,jdouble p112
    ,jdouble p113,jdouble p114,jdouble p115,jdouble p116,jdouble p117,jdouble p118
    ,jdouble p119,jfloat p120,jdouble p121,jdouble p122,jdouble p123,jdouble p124
    ,jdouble p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc41(JNIEnv *e, jclass c
,jfloat p0,jdouble p1,jint p2,jdouble p3,jint p4,jfloat p5,jfloat p6,jfloat p7
    ,jint p8,jint p9,jfloat p10,jdouble p11,jdouble p12,jdouble p13,jint p14
    ,jdouble p15,jdouble p16,jfloat p17,jdouble p18,jdouble p19,jdouble p20
    ,jint p21,jdouble p22,jfloat p23,jdouble p24,jfloat p25,jdouble p26,jdouble p27
    ,jint p28,jdouble p29,jdouble p30,jint p31,jdouble p32,jdouble p33,jfloat p34
    ,jint p35,jdouble p36,jint p37,jint p38,jdouble p39,jfloat p40,jint p41
    ,jdouble p42,jdouble p43,jdouble p44,jdouble p45,jint p46,jdouble p47,jfloat p48
    ,jfloat p49,jfloat p50,jdouble p51,jdouble p52,jfloat p53,jfloat p54,jdouble p55
    ,jfloat p56,jfloat p57,jdouble p58,jfloat p59,jdouble p60,jfloat p61,jdouble p62
    ,jfloat p63,jdouble p64,jdouble p65,jdouble p66,jdouble p67,jdouble p68
    ,jdouble p69,jfloat p70,jfloat p71,jdouble p72,jfloat p73,jdouble p74,jfloat p75
    ,jint p76,jfloat p77,jdouble p78,jint p79,jfloat p80,jfloat p81,jdouble p82
    ,jdouble p83,jdouble p84,jdouble p85,jdouble p86,jfloat p87,jfloat p88,jfloat p89
    ,jdouble p90,jint p91,jfloat p92,jdouble p93,jfloat p94,jfloat p95,jfloat p96
    ,jfloat p97,jfloat p98,jdouble p99,jfloat p100,jint p101,jdouble p102,jfloat p103
    ,jdouble p104,jdouble p105,jfloat p106,jfloat p107,jdouble p108,jfloat p109
    ,jfloat p110,jdouble p111,jfloat p112,jdouble p113,jfloat p114,jfloat p115
    ,jfloat p116,jdouble p117,jint p118,jfloat p119,jint p120,jdouble p121,jdouble p122
    ,jint p123,jdouble p124,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%d\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%d\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%d\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%d\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%d\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc42(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jdouble p2,jdouble p3,jfloat p4,jdouble p5,jdouble p6
    ,jfloat p7,jdouble p8,jdouble p9,jfloat p10,jdouble p11,jbyte p12,jfloat p13
    ,jfloat p14,jfloat p15,jdouble p16,jdouble p17,jfloat p18,jdouble p19,jdouble p20
    ,jdouble p21,jfloat p22,jfloat p23,jdouble p24,jfloat p25,jdouble p26,jfloat p27
    ,jfloat p28,jfloat p29,jfloat p30,jfloat p31,jdouble p32,jfloat p33,jbyte p34
    ,jfloat p35,jdouble p36,jfloat p37,jdouble p38,jfloat p39,jdouble p40,jbyte p41
    ,jbyte p42,jdouble p43,jfloat p44,jdouble p45,jfloat p46,jfloat p47,jdouble p48
    ,jdouble p49,jfloat p50,jdouble p51,jdouble p52,jdouble p53,jdouble p54
    ,jfloat p55,jdouble p56,jfloat p57,jbyte p58,jdouble p59,jfloat p60,jdouble p61
    ,jdouble p62,jdouble p63,jfloat p64,jfloat p65,jdouble p66,jdouble p67,jbyte p68
    ,jfloat p69,jfloat p70,jdouble p71,jfloat p72,jfloat p73,jdouble p74,jdouble p75
    ,jfloat p76,jfloat p77,jfloat p78,jdouble p79,jdouble p80,jdouble p81,jbyte p82
    ,jfloat p83,jdouble p84,jdouble p85,jdouble p86,jdouble p87,jdouble p88
    ,jdouble p89,jfloat p90,jdouble p91,jfloat p92,jbyte p93,jfloat p94,jdouble p95
    ,jdouble p96,jfloat p97,jdouble p98,jfloat p99,jfloat p100,jfloat p101,jdouble p102
    ,jdouble p103,jdouble p104,jfloat p105,jfloat p106,jfloat p107,jfloat p108
    ,jdouble p109,jfloat p110,jdouble p111,jdouble p112,jdouble p113,jfloat p114
    ,jdouble p115,jfloat p116,jbyte p117,jdouble p118,jfloat p119,jdouble p120
    ,jfloat p121,jbyte p122,jfloat p123,jdouble p124,jfloat p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%d\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc43(JNIEnv *e, jclass c
,jfloat p0,jint p1,jfloat p2,jfloat p3,jfloat p4,jfloat p5,jfloat p6,jint p7
    ,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jint p14
    ,jfloat p15,jfloat p16,jfloat p17,jfloat p18,jfloat p19,jfloat p20,jfloat p21
    ,jfloat p22,jfloat p23,jfloat p24,jfloat p25,jfloat p26,jfloat p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jint p32,jfloat p33,jfloat p34,jfloat p35
    ,jfloat p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jint p42
    ,jfloat p43,jfloat p44,jfloat p45,jfloat p46,jfloat p47,jfloat p48,jint p49
    ,jint p50,jint p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55,jfloat p56
    ,jfloat p57,jfloat p58,jfloat p59,jfloat p60,jint p61,jfloat p62,jint p63
    ,jfloat p64,jfloat p65,jfloat p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jfloat p71,jfloat p72,jfloat p73,jfloat p74,jfloat p75,jfloat p76,jfloat p77
    ,jfloat p78,jint p79,jint p80,jint p81,jfloat p82,jfloat p83,jfloat p84
    ,jfloat p85,jfloat p86,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jfloat p91
    ,jfloat p92,jfloat p93,jfloat p94,jfloat p95,jfloat p96,jfloat p97,jfloat p98
    ,jfloat p99,jfloat p100,jfloat p101,jfloat p102,jfloat p103,jfloat p104
    ,jfloat p105,jfloat p106,jfloat p107,jfloat p108,jfloat p109,jfloat p110
    ,jint p111,jfloat p112,jfloat p113,jfloat p114,jfloat p115,jfloat p116,jfloat p117
    ,jfloat p118,jfloat p119,jfloat p120,jfloat p121,jfloat p122,jfloat p123
    ,jfloat p124,jint p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%d\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%d\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%d\n",p80);
    fprintf(file,"p81=%d\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%d\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc44(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jbyte p5,jfloat p6,jfloat p7
    ,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jfloat p12,jfloat p13,jfloat p14
    ,jbyte p15,jfloat p16,jfloat p17,jfloat p18,jbyte p19,jfloat p20,jfloat p21
    ,jfloat p22,jfloat p23,jfloat p24,jbyte p25,jfloat p26,jfloat p27,jfloat p28
    ,jfloat p29,jfloat p30,jfloat p31,jbyte p32,jbyte p33,jfloat p34,jfloat p35
    ,jfloat p36,jfloat p37,jbyte p38,jfloat p39,jfloat p40,jbyte p41,jfloat p42
    ,jfloat p43,jfloat p44,jfloat p45,jfloat p46,jfloat p47,jbyte p48,jfloat p49
    ,jbyte p50,jfloat p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55,jfloat p56
    ,jfloat p57,jfloat p58,jbyte p59,jfloat p60,jfloat p61,jfloat p62,jfloat p63
    ,jfloat p64,jfloat p65,jbyte p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jbyte p71,jfloat p72,jbyte p73,jbyte p74,jfloat p75,jfloat p76,jbyte p77
    ,jbyte p78,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jfloat p83,jfloat p84
    ,jfloat p85,jfloat p86,jfloat p87,jfloat p88,jfloat p89,jfloat p90,jfloat p91
    ,jfloat p92,jbyte p93,jfloat p94,jfloat p95,jfloat p96,jbyte p97,jfloat p98
    ,jfloat p99,jfloat p100,jbyte p101,jfloat p102,jfloat p103,jfloat p104,jfloat p105
    ,jfloat p106,jfloat p107,jbyte p108,jfloat p109,jfloat p110,jfloat p111
    ,jfloat p112,jbyte p113,jfloat p114,jfloat p115,jfloat p116,jbyte p117,jfloat p118
    ,jfloat p119,jfloat p120,jbyte p121,jfloat p122,jfloat p123,jfloat p124
    ,jfloat p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%d\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%d\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%d\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc45(JNIEnv *e, jclass c
,jbyte p0,jbyte p1,jfloat p2,jfloat p3,jfloat p4,jbyte p5,jint p6,jfloat p7
    ,jfloat p8,jbyte p9,jfloat p10,jbyte p11,jbyte p12,jfloat p13,jfloat p14
    ,jfloat p15,jfloat p16,jbyte p17,jfloat p18,jint p19,jint p20,jfloat p21
    ,jfloat p22,jfloat p23,jbyte p24,jbyte p25,jfloat p26,jfloat p27,jbyte p28
    ,jfloat p29,jfloat p30,jfloat p31,jfloat p32,jfloat p33,jfloat p34,jint p35
    ,jfloat p36,jint p37,jfloat p38,jfloat p39,jfloat p40,jfloat p41,jfloat p42
    ,jbyte p43,jfloat p44,jint p45,jbyte p46,jfloat p47,jfloat p48,jfloat p49
    ,jbyte p50,jfloat p51,jbyte p52,jfloat p53,jfloat p54,jfloat p55,jfloat p56
    ,jfloat p57,jfloat p58,jfloat p59,jint p60,jint p61,jfloat p62,jbyte p63
    ,jfloat p64,jint p65,jfloat p66,jfloat p67,jfloat p68,jfloat p69,jfloat p70
    ,jfloat p71,jfloat p72,jfloat p73,jfloat p74,jint p75,jfloat p76,jfloat p77
    ,jbyte p78,jfloat p79,jfloat p80,jfloat p81,jfloat p82,jbyte p83,jint p84
    ,jbyte p85,jfloat p86,jint p87,jfloat p88,jfloat p89,jfloat p90,jfloat p91
    ,jfloat p92,jint p93,jfloat p94,jfloat p95,jint p96,jfloat p97,jbyte p98
    ,jfloat p99,jbyte p100,jfloat p101,jint p102,jfloat p103,jfloat p104,jint p105
    ,jfloat p106,jfloat p107,jint p108,jfloat p109,jfloat p110,jfloat p111,jfloat p112
    ,jint p113,jfloat p114,jfloat p115,jfloat p116,jfloat p117,jfloat p118,jint p119
    ,jfloat p120,jbyte p121,jfloat p122,jfloat p123,jfloat p124,jbyte p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%d\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%d\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%d\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%d\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%d\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%d\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%d\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%d\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%d\n",p83);
    fprintf(file,"p84=%d\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%d\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%d\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%d\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc46(JNIEnv *e, jclass c
,jint p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jdouble p9,jdouble p10,jint p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jint p16,jdouble p17,jdouble p18,jdouble p19,jdouble p20
    ,jdouble p21,jdouble p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jdouble p27,jint p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32,jdouble p33
    ,jdouble p34,jdouble p35,jint p36,jint p37,jdouble p38,jint p39,jdouble p40
    ,jdouble p41,jdouble p42,jdouble p43,jdouble p44,jdouble p45,jdouble p46
    ,jdouble p47,jdouble p48,jdouble p49,jdouble p50,jdouble p51,jint p52,jdouble p53
    ,jdouble p54,jdouble p55,jdouble p56,jdouble p57,jdouble p58,jdouble p59
    ,jdouble p60,jdouble p61,jdouble p62,jdouble p63,jint p64,jdouble p65,jdouble p66
    ,jint p67,jint p68,jdouble p69,jdouble p70,jdouble p71,jdouble p72,jdouble p73
    ,jdouble p74,jdouble p75,jdouble p76,jint p77,jint p78,jdouble p79,jdouble p80
    ,jdouble p81,jint p82,jdouble p83,jdouble p84,jint p85,jdouble p86,jdouble p87
    ,jdouble p88,jint p89,jdouble p90,jdouble p91,jdouble p92,jint p93,jdouble p94
    ,jint p95,jdouble p96,jdouble p97,jdouble p98,jdouble p99,jdouble p100,jdouble p101
    ,jint p102,jdouble p103,jdouble p104,jdouble p105,jdouble p106,jdouble p107
    ,jdouble p108,jdouble p109,jdouble p110,jdouble p111,jdouble p112,jdouble p113
    ,jdouble p114,jdouble p115,jint p116,jint p117,jdouble p118,jdouble p119
    ,jdouble p120,jint p121,jdouble p122,jint p123,jint p124,jdouble p125,jdouble p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%d\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%d\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%d\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%d\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%d\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%d\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%d\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%d\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%d\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%d\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc47(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jbyte p4,jdouble p5,jdouble p6
    ,jdouble p7,jbyte p8,jbyte p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jdouble p19
    ,jdouble p20,jdouble p21,jbyte p22,jdouble p23,jdouble p24,jdouble p25,jdouble p26
    ,jbyte p27,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32,jdouble p33
    ,jdouble p34,jdouble p35,jdouble p36,jbyte p37,jbyte p38,jdouble p39,jdouble p40
    ,jdouble p41,jdouble p42,jdouble p43,jdouble p44,jdouble p45,jbyte p46,jdouble p47
    ,jdouble p48,jdouble p49,jdouble p50,jdouble p51,jbyte p52,jdouble p53,jdouble p54
    ,jdouble p55,jdouble p56,jdouble p57,jdouble p58,jdouble p59,jdouble p60
    ,jdouble p61,jdouble p62,jdouble p63,jdouble p64,jdouble p65,jdouble p66
    ,jdouble p67,jdouble p68,jdouble p69,jbyte p70,jdouble p71,jdouble p72,jdouble p73
    ,jdouble p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78,jdouble p79
    ,jdouble p80,jdouble p81,jdouble p82,jdouble p83,jdouble p84,jdouble p85
    ,jdouble p86,jdouble p87,jdouble p88,jdouble p89,jdouble p90,jdouble p91
    ,jdouble p92,jdouble p93,jbyte p94,jdouble p95,jdouble p96,jdouble p97,jdouble p98
    ,jdouble p99,jdouble p100,jbyte p101,jdouble p102,jdouble p103,jbyte p104
    ,jdouble p105,jdouble p106,jdouble p107,jdouble p108,jdouble p109,jdouble p110
    ,jdouble p111,jdouble p112,jdouble p113,jbyte p114,jdouble p115,jdouble p116
    ,jdouble p117,jdouble p118,jdouble p119,jbyte p120,jdouble p121,jbyte p122
    ,jdouble p123,jdouble p124,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%d\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%d\n",p37);fprintf(file,"p38=%d\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%d\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%d\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%d\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%d\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc48(JNIEnv *e, jclass c
,jint p0,jdouble p1,jdouble p2,jdouble p3,jbyte p4,jdouble p5,jdouble p6
    ,jdouble p7,jdouble p8,jbyte p9,jint p10,jdouble p11,jbyte p12,jint p13
    ,jdouble p14,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jint p19,jdouble p20
    ,jdouble p21,jint p22,jdouble p23,jdouble p24,jint p25,jdouble p26,jint p27
    ,jbyte p28,jbyte p29,jdouble p30,jdouble p31,jdouble p32,jbyte p33,jdouble p34
    ,jdouble p35,jdouble p36,jdouble p37,jdouble p38,jdouble p39,jdouble p40
    ,jdouble p41,jbyte p42,jint p43,jdouble p44,jbyte p45,jdouble p46,jbyte p47
    ,jdouble p48,jdouble p49,jbyte p50,jbyte p51,jdouble p52,jdouble p53,jint p54
    ,jdouble p55,jdouble p56,jint p57,jdouble p58,jdouble p59,jdouble p60,jdouble p61
    ,jint p62,jdouble p63,jdouble p64,jdouble p65,jbyte p66,jdouble p67,jdouble p68
    ,jdouble p69,jdouble p70,jdouble p71,jdouble p72,jbyte p73,jdouble p74,jdouble p75
    ,jint p76,jint p77,jint p78,jbyte p79,jdouble p80,jdouble p81,jbyte p82
    ,jdouble p83,jdouble p84,jbyte p85,jdouble p86,jint p87,jint p88,jdouble p89
    ,jdouble p90,jdouble p91,jdouble p92,jdouble p93,jint p94,jdouble p95,jdouble p96
    ,jdouble p97,jbyte p98,jdouble p99,jbyte p100,jbyte p101,jdouble p102,jdouble p103
    ,jdouble p104,jint p105,jdouble p106,jdouble p107,jdouble p108,jbyte p109
    ,jdouble p110,jdouble p111,jdouble p112,jdouble p113,jdouble p114,jdouble p115
    ,jdouble p116,jdouble p117,jdouble p118,jdouble p119,jint p120,jdouble p121
    ,jdouble p122,jdouble p123,jdouble p124,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%d\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%d\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%d\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%d\n",p12);fprintf(file,"p13=%d\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%d\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%d\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%d\n",p28);fprintf(file,"p29=%d\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%d\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%d\n",p50);
    fprintf(file,"p51=%d\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%d\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%d\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%d\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%d\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%d\n",p77);
    fprintf(file,"p78=%d\n",p78);fprintf(file,"p79=%d\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%d\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%d\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%d\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%d\n",p100);fprintf(file,"p101=%d\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%d\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%d\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc49(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jdouble p2,jfloat p3,jfloat p4,jbyte p5,jdouble p6
    ,jint p7,jdouble p8,jfloat p9,jbyte p10,jfloat p11,jdouble p12,jfloat p13
    ,jdouble p14,jdouble p15,jdouble p16,jfloat p17,jfloat p18,jdouble p19,jfloat p20
    ,jfloat p21,jint p22,jfloat p23,jbyte p24,jfloat p25,jbyte p26,jdouble p27
    ,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jdouble p32,jint p33,jfloat p34
    ,jfloat p35,jbyte p36,jfloat p37,jfloat p38,jfloat p39,jdouble p40,jbyte p41
    ,jint p42,jfloat p43,jdouble p44,jfloat p45,jdouble p46,jint p47,jbyte p48
    ,jdouble p49,jfloat p50,jdouble p51,jfloat p52,jfloat p53,jfloat p54,jfloat p55
    ,jfloat p56,jdouble p57,jbyte p58,jfloat p59,jfloat p60,jfloat p61,jfloat p62
    ,jfloat p63,jfloat p64,jfloat p65,jbyte p66,jdouble p67,jbyte p68,jfloat p69
    ,jfloat p70,jdouble p71,jdouble p72,jdouble p73,jint p74,jdouble p75,jfloat p76
    ,jfloat p77,jfloat p78,jfloat p79,jdouble p80,jfloat p81,jdouble p82,jfloat p83
    ,jfloat p84,jint p85,jfloat p86,jdouble p87,jdouble p88,jfloat p89,jint p90
    ,jdouble p91,jdouble p92,jdouble p93,jfloat p94,jfloat p95,jdouble p96,jdouble p97
    ,jfloat p98,jdouble p99,jfloat p100,jfloat p101,jfloat p102,jfloat p103
    ,jdouble p104,jfloat p105,jdouble p106,jfloat p107,jdouble p108,jfloat p109
    ,jfloat p110,jdouble p111,jfloat p112,jbyte p113,jfloat p114,jdouble p115
    ,jdouble p116,jfloat p117,jdouble p118,jdouble p119,jbyte p120,jfloat p121
    ,jfloat p122,jbyte p123,jdouble p124,jdouble p125,jfloat p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%d\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%d\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%d\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%d\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%d\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%d\n",p41);
    fprintf(file,"p42=%d\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%d\n",p47);
    fprintf(file,"p48=%d\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%d\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%d\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%d\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%d\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%d\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%d\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc50(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jfloat p4,jfloat p5,jdouble p6
    ,jfloat p7,jdouble p8,jfloat p9,jdouble p10,jdouble p11,jdouble p12,jdouble p13
    ,jdouble p14,jdouble p15,jfloat p16,jdouble p17,jfloat p18,jdouble p19,jfloat p20
    ,jdouble p21,jfloat p22,jdouble p23,jdouble p24,jdouble p25,jfloat p26,jdouble p27
    ,jdouble p28,jdouble p29,jdouble p30,jdouble p31,jfloat p32,jfloat p33,jdouble p34
    ,jdouble p35,jdouble p36,jdouble p37,jfloat p38,jdouble p39,jdouble p40
    ,jdouble p41,jdouble p42,jdouble p43,jdouble p44,jdouble p45,jdouble p46
    ,jfloat p47,jdouble p48,jfloat p49,jdouble p50,jdouble p51,jdouble p52,jdouble p53
    ,jdouble p54,jdouble p55,jdouble p56,jdouble p57,jdouble p58,jdouble p59
    ,jdouble p60,jdouble p61,jfloat p62,jfloat p63,jdouble p64,jdouble p65,jdouble p66
    ,jdouble p67,jdouble p68,jdouble p69,jdouble p70,jfloat p71,jdouble p72
    ,jdouble p73,jfloat p74,jdouble p75,jdouble p76,jdouble p77,jdouble p78
    ,jdouble p79,jdouble p80,jdouble p81,jdouble p82,jfloat p83,jfloat p84,jdouble p85
    ,jdouble p86,jfloat p87,jdouble p88,jdouble p89,jfloat p90,jdouble p91,jdouble p92
    ,jdouble p93,jfloat p94,jdouble p95,jfloat p96,jdouble p97,jdouble p98,jfloat p99
    ,jdouble p100,jfloat p101,jdouble p102,jdouble p103,jdouble p104,jdouble p105
    ,jdouble p106,jdouble p107,jdouble p108,jdouble p109,jfloat p110,jdouble p111
    ,jdouble p112,jdouble p113,jdouble p114,jfloat p115,jdouble p116,jdouble p117
    ,jdouble p118,jfloat p119,jdouble p120,jdouble p121,jdouble p122,jdouble p123
    ,jdouble p124,jdouble p125,jdouble p126)
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%e\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%e\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%e\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%e\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc51(JNIEnv *e, jclass c
,jfloat p0,jfloat p1,jfloat p2,jfloat p3,jfloat p4,jdouble p5,jfloat p6
    ,jfloat p7,jdouble p8,jdouble p9,jint p10,jfloat p11,jdouble p12,jdouble p13
    ,jfloat p14,jdouble p15,jdouble p16,jdouble p17,jdouble p18,jdouble p19
    ,jfloat p20,jdouble p21,jfloat p22,jint p23,jdouble p24,jdouble p25,jfloat p26
    ,jint p27,jdouble p28,jfloat p29,jfloat p30,jdouble p31,jint p32,jint p33
    ,jfloat p34,jfloat p35,jdouble p36,jfloat p37,jfloat p38,jfloat p39,jfloat p40
    ,jfloat p41,jdouble p42,jdouble p43,jdouble p44,jint p45,jdouble p46,jfloat p47
    ,jfloat p48,jdouble p49,jdouble p50,jfloat p51,jfloat p52,jfloat p53,jdouble p54
    ,jfloat p55,jdouble p56,jfloat p57,jfloat p58,jint p59,jint p60,jdouble p61
    ,jdouble p62,jdouble p63,jfloat p64,jfloat p65,jdouble p66,jint p67,jint p68
    ,jfloat p69,jint p70,jdouble p71,jint p72,jfloat p73,jfloat p74,jdouble p75
    ,jdouble p76,jdouble p77,jfloat p78,jfloat p79,jfloat p80,jdouble p81,jdouble p82
    ,jfloat p83,jfloat p84,jdouble p85,jfloat p86,jint p87,jfloat p88,jfloat p89
    ,jdouble p90,jfloat p91,jdouble p92,jdouble p93,jint p94,jfloat p95,jfloat p96
    ,jfloat p97,jdouble p98,jdouble p99,jdouble p100,jdouble p101,jfloat p102
    ,jfloat p103,jdouble p104,jfloat p105,jdouble p106,jdouble p107,jfloat p108
    ,jfloat p109,jdouble p110,jfloat p111,jfloat p112,jfloat p113,jfloat p114
    ,jfloat p115,jdouble p116,jfloat p117,jdouble p118,jdouble p119,jdouble p120
    ,jfloat p121,jfloat p122,jfloat p123,jdouble p124,jdouble p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%e\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%d\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%d\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%d\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%d\n",p32);
    fprintf(file,"p33=%d\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%e\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%e\n",p58);fprintf(file,"p59=%d\n",p59);
    fprintf(file,"p60=%d\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%e\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%d\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%d\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%e\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%d\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%e\n",p102);fprintf(file,"p103=%e\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%e\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%e\n",p112);fprintf(file,"p113=%e\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%e\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}
JNIEXPORT void JNICALL Java_vm_jit_LongTransitions_LTTest_nativeFnc52(JNIEnv *e, jclass c
,jdouble p0,jdouble p1,jdouble p2,jdouble p3,jdouble p4,jbyte p5,jfloat p6
    ,jdouble p7,jfloat p8,jfloat p9,jfloat p10,jfloat p11,jdouble p12,jfloat p13
    ,jdouble p14,jdouble p15,jdouble p16,jfloat p17,jfloat p18,jdouble p19,jdouble p20
    ,jdouble p21,jdouble p22,jfloat p23,jdouble p24,jfloat p25,jfloat p26,jdouble p27
    ,jdouble p28,jfloat p29,jdouble p30,jfloat p31,jfloat p32,jfloat p33,jdouble p34
    ,jdouble p35,jdouble p36,jfloat p37,jfloat p38,jdouble p39,jdouble p40,jfloat p41
    ,jfloat p42,jfloat p43,jfloat p44,jbyte p45,jfloat p46,jdouble p47,jfloat p48
    ,jfloat p49,jdouble p50,jfloat p51,jbyte p52,jfloat p53,jfloat p54,jdouble p55
    ,jfloat p56,jdouble p57,jbyte p58,jdouble p59,jdouble p60,jdouble p61,jdouble p62
    ,jfloat p63,jdouble p64,jbyte p65,jfloat p66,jfloat p67,jbyte p68,jdouble p69
    ,jbyte p70,jdouble p71,jdouble p72,jdouble p73,jfloat p74,jdouble p75,jbyte p76
    ,jdouble p77,jdouble p78,jdouble p79,jdouble p80,jfloat p81,jdouble p82
    ,jdouble p83,jdouble p84,jfloat p85,jfloat p86,jfloat p87,jfloat p88,jdouble p89
    ,jfloat p90,jfloat p91,jfloat p92,jdouble p93,jbyte p94,jdouble p95,jfloat p96
    ,jfloat p97,jdouble p98,jdouble p99,jfloat p100,jdouble p101,jbyte p102
    ,jbyte p103,jfloat p104,jfloat p105,jdouble p106,jfloat p107,jbyte p108
    ,jfloat p109,jdouble p110,jfloat p111,jbyte p112,jbyte p113,jdouble p114
    ,jdouble p115,jdouble p116,jbyte p117,jfloat p118,jdouble p119,jdouble p120
    ,jfloat p121,jdouble p122,jdouble p123,jdouble p124,jdouble p125,jfloat p126
    )
{
    FILE *file=fopen("LTTest_c.txt","a");fprintf(file,"p0=%e\n",p0);fprintf(file,"p1=%e\n",p1);fprintf(file,"p2=%e\n",p2);
    fprintf(file,"p3=%e\n",p3);fprintf(file,"p4=%e\n",p4);fprintf(file,"p5=%d\n",p5);
    fprintf(file,"p6=%e\n",p6);fprintf(file,"p7=%e\n",p7);fprintf(file,"p8=%e\n",p8);
    fprintf(file,"p9=%e\n",p9);fprintf(file,"p10=%e\n",p10);fprintf(file,"p11=%e\n",p11);
    fprintf(file,"p12=%e\n",p12);fprintf(file,"p13=%e\n",p13);fprintf(file,"p14=%e\n",p14);
    fprintf(file,"p15=%e\n",p15);fprintf(file,"p16=%e\n",p16);fprintf(file,"p17=%e\n",p17);
    fprintf(file,"p18=%e\n",p18);fprintf(file,"p19=%e\n",p19);fprintf(file,"p20=%e\n",p20);
    fprintf(file,"p21=%e\n",p21);fprintf(file,"p22=%e\n",p22);fprintf(file,"p23=%e\n",p23);
    fprintf(file,"p24=%e\n",p24);fprintf(file,"p25=%e\n",p25);fprintf(file,"p26=%e\n",p26);
    fprintf(file,"p27=%e\n",p27);fprintf(file,"p28=%e\n",p28);fprintf(file,"p29=%e\n",p29);
    fprintf(file,"p30=%e\n",p30);fprintf(file,"p31=%e\n",p31);fprintf(file,"p32=%e\n",p32);
    fprintf(file,"p33=%e\n",p33);fprintf(file,"p34=%e\n",p34);fprintf(file,"p35=%e\n",p35);
    fprintf(file,"p36=%e\n",p36);fprintf(file,"p37=%e\n",p37);fprintf(file,"p38=%e\n",p38);
    fprintf(file,"p39=%e\n",p39);fprintf(file,"p40=%e\n",p40);fprintf(file,"p41=%e\n",p41);
    fprintf(file,"p42=%e\n",p42);fprintf(file,"p43=%e\n",p43);fprintf(file,"p44=%e\n",p44);
    fprintf(file,"p45=%d\n",p45);fprintf(file,"p46=%e\n",p46);fprintf(file,"p47=%e\n",p47);
    fprintf(file,"p48=%e\n",p48);fprintf(file,"p49=%e\n",p49);fprintf(file,"p50=%e\n",p50);
    fprintf(file,"p51=%e\n",p51);fprintf(file,"p52=%d\n",p52);fprintf(file,"p53=%e\n",p53);
    fprintf(file,"p54=%e\n",p54);fprintf(file,"p55=%e\n",p55);fprintf(file,"p56=%e\n",p56);
    fprintf(file,"p57=%e\n",p57);fprintf(file,"p58=%d\n",p58);fprintf(file,"p59=%e\n",p59);
    fprintf(file,"p60=%e\n",p60);fprintf(file,"p61=%e\n",p61);fprintf(file,"p62=%e\n",p62);
    fprintf(file,"p63=%e\n",p63);fprintf(file,"p64=%e\n",p64);fprintf(file,"p65=%d\n",p65);
    fprintf(file,"p66=%e\n",p66);fprintf(file,"p67=%e\n",p67);fprintf(file,"p68=%d\n",p68);
    fprintf(file,"p69=%e\n",p69);fprintf(file,"p70=%d\n",p70);fprintf(file,"p71=%e\n",p71);
    fprintf(file,"p72=%e\n",p72);fprintf(file,"p73=%e\n",p73);fprintf(file,"p74=%e\n",p74);
    fprintf(file,"p75=%e\n",p75);fprintf(file,"p76=%d\n",p76);fprintf(file,"p77=%e\n",p77);
    fprintf(file,"p78=%e\n",p78);fprintf(file,"p79=%e\n",p79);fprintf(file,"p80=%e\n",p80);
    fprintf(file,"p81=%e\n",p81);fprintf(file,"p82=%e\n",p82);fprintf(file,"p83=%e\n",p83);
    fprintf(file,"p84=%e\n",p84);fprintf(file,"p85=%e\n",p85);fprintf(file,"p86=%e\n",p86);
    fprintf(file,"p87=%e\n",p87);fprintf(file,"p88=%e\n",p88);fprintf(file,"p89=%e\n",p89);
    fprintf(file,"p90=%e\n",p90);fprintf(file,"p91=%e\n",p91);fprintf(file,"p92=%e\n",p92);
    fprintf(file,"p93=%e\n",p93);fprintf(file,"p94=%d\n",p94);fprintf(file,"p95=%e\n",p95);
    fprintf(file,"p96=%e\n",p96);fprintf(file,"p97=%e\n",p97);fprintf(file,"p98=%e\n",p98);
    fprintf(file,"p99=%e\n",p99);fprintf(file,"p100=%e\n",p100);fprintf(file,"p101=%e\n",p101);
    fprintf(file,"p102=%d\n",p102);fprintf(file,"p103=%d\n",p103);fprintf(file,"p104=%e\n",p104);
    fprintf(file,"p105=%e\n",p105);fprintf(file,"p106=%e\n",p106);fprintf(file,"p107=%e\n",p107);
    fprintf(file,"p108=%d\n",p108);fprintf(file,"p109=%e\n",p109);fprintf(file,"p110=%e\n",p110);
    fprintf(file,"p111=%e\n",p111);fprintf(file,"p112=%d\n",p112);fprintf(file,"p113=%d\n",p113);
    fprintf(file,"p114=%e\n",p114);fprintf(file,"p115=%e\n",p115);fprintf(file,"p116=%e\n",p116);
    fprintf(file,"p117=%d\n",p117);fprintf(file,"p118=%e\n",p118);fprintf(file,"p119=%e\n",p119);
    fprintf(file,"p120=%e\n",p120);fprintf(file,"p121=%e\n",p121);fprintf(file,"p122=%e\n",p122);
    fprintf(file,"p123=%e\n",p123);fprintf(file,"p124=%e\n",p124);fprintf(file,"p125=%e\n",p125);
    fprintf(file,"p126=%e\n",p126);
    fclose(file);
}

}
