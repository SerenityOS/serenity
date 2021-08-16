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

/*
 * @test
 * @bug 8032410
 * @summary Stack overflow at deoptimization doesn't release owned monitors
 *
 * @run main/othervm -XX:-BackgroundCompilation -Xss512K -XX:-UseOnStackReplacement
 *      -XX:CompileCommand=dontinline,compiler.uncommontrap.TestStackBangMonitorOwned::m1
 *      -XX:CompileCommand=exclude,compiler.uncommontrap.TestStackBangMonitorOwned::m2
 *      compiler.uncommontrap.TestStackBangMonitorOwned
 */

package compiler.uncommontrap;

public class TestStackBangMonitorOwned {

    static class UnloadedClass1 {
        volatile int field;
    }

    static Object m1(boolean deopt) {
        long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12,
        l13, l14, l15, l16, l17, l18, l19, l20, l21, l22, l23, l24,
        l25, l26, l27, l28, l29, l30, l31, l32, l33, l34, l35, l36,
        l37, l38, l39, l40, l41, l42, l43, l44, l45, l46, l47, l48,
        l49, l50, l51, l52, l53, l54, l55, l56, l57, l58, l59, l60,
        l61, l62, l63, l64, l65, l66, l67, l68, l69, l70, l71, l72,
        l73, l74, l75, l76, l77, l78, l79, l80, l81, l82, l83, l84,
        l85, l86, l87, l88, l89, l90, l91, l92, l93, l94, l95, l96,
        l97, l98, l99, l100, l101, l102, l103, l104, l105, l106, l107,
        l108, l109, l110, l111, l112, l113, l114, l115, l116, l117,
        l118, l119, l120, l121, l122, l123, l124, l125, l126, l127,
        l128, l129, l130, l131, l132, l133, l134, l135, l136, l137,
        l138, l139, l140, l141, l142, l143, l144, l145, l146, l147,
        l148, l149, l150, l151, l152, l153, l154, l155, l156, l157,
        l158, l159, l160, l161, l162, l163, l164, l165, l166, l167,
        l168, l169, l170, l171, l172, l173, l174, l175, l176, l177,
        l178, l179, l180, l181, l182, l183, l184, l185, l186, l187,
        l188, l189, l190, l191, l192, l193, l194, l195, l196, l197,
        l198, l199, l200, l201, l202, l203, l204, l205, l206, l207,
        l208, l209, l210, l211, l212, l213, l214, l215, l216, l217,
        l218, l219, l220, l221, l222, l223, l224, l225, l226, l227,
        l228, l229, l230, l231, l232, l233, l234, l235, l236, l237,
        l238, l239, l240, l241, l242, l243, l244, l245, l246, l247,
        l248, l249, l250, l251, l252, l253, l254, l255, l256, l257,
        l258, l259, l260, l261, l262, l263, l264, l265, l266, l267,
        l268, l269, l270, l271, l272, l273, l274, l275, l276, l277,
        l278, l279, l280, l281, l282, l283, l284, l285, l286, l287,
        l288, l289, l290, l291, l292, l293, l294, l295, l296, l297,
        l298, l299, l300, l301, l302, l303, l304, l305, l306, l307,
        l308, l309, l310, l311, l312, l313, l314, l315, l316, l317,
        l318, l319, l320, l321, l322, l323, l324, l325, l326, l327,
        l328, l329, l330, l331, l332, l333, l334, l335, l336, l337,
        l338, l339, l340, l341, l342, l343, l344, l345, l346, l347,
        l348, l349, l350, l351, l352, l353, l354, l355, l356, l357,
        l358, l359, l360, l361, l362, l363, l364, l365, l366, l367,
        l368, l369, l370, l371, l372, l373, l374, l375, l376, l377,
        l378, l379, l380, l381, l382, l383, l384, l385, l386, l387,
        l388, l389, l390, l391, l392, l393, l394, l395, l396, l397,
        l398, l399, l400, l401, l402, l403, l404, l405, l406, l407,
        l408, l409, l410, l411, l412, l413, l414, l415, l416, l417,
        l418, l419, l420, l421, l422, l423, l424, l425, l426, l427,
        l428, l429, l430, l431, l432, l433, l434, l435, l436, l437,
        l438, l439, l440, l441, l442, l443, l444, l445, l446, l447,
        l448, l449, l450, l451, l452, l453, l454, l455, l456, l457,
        l458, l459, l460, l461, l462, l463, l464, l465, l466, l467,
        l468, l469, l470, l471, l472, l473, l474, l475, l476, l477,
        l478, l479, l480, l481, l482, l483, l484, l485, l486, l487,
        l488, l489, l490, l491, l492, l493, l494, l495, l496, l497,
        l498, l499, l500, l501, l502, l503, l504, l505, l506, l507,
        l508, l509, l510, l511;

        long ll0, ll1, ll2, ll3, ll4, ll5, ll6, ll7, ll8, ll9, ll10, ll11, ll12,
        ll13, ll14, ll15, ll16, ll17, ll18, ll19, ll20, ll21, ll22, ll23, ll24,
        ll25, ll26, ll27, ll28, ll29, ll30, ll31, ll32, ll33, ll34, ll35, ll36,
        ll37, ll38, ll39, ll40, ll41, ll42, ll43, ll44, ll45, ll46, ll47, ll48,
        ll49, ll50, ll51, ll52, ll53, ll54, ll55, ll56, ll57, ll58, ll59, ll60,
        ll61, ll62, ll63, ll64, ll65, ll66, ll67, ll68, ll69, ll70, ll71, ll72,
        ll73, ll74, ll75, ll76, ll77, ll78, ll79, ll80, ll81, ll82, ll83, ll84,
        ll85, ll86, ll87, ll88, ll89, ll90, ll91, ll92, ll93, ll94, ll95, ll96,
        ll97, ll98, ll99, ll100, ll101, ll102, ll103, ll104, ll105, ll106, ll107,
        ll108, ll109, ll110, ll111, ll112, ll113, ll114, ll115, ll116, ll117,
        ll118, ll119, ll120, ll121, ll122, ll123, ll124, ll125, ll126, ll127,
        ll128, ll129, ll130, ll131, ll132, ll133, ll134, ll135, ll136, ll137,
        ll138, ll139, ll140, ll141, ll142, ll143, ll144, ll145, ll146, ll147,
        ll148, ll149, ll150, ll151, ll152, ll153, ll154, ll155, ll156, ll157,
        ll158, ll159, ll160, ll161, ll162, ll163, ll164, ll165, ll166, ll167,
        ll168, ll169, ll170, ll171, ll172, ll173, ll174, ll175, ll176, ll177,
        ll178, ll179, ll180, ll181, ll182, ll183, ll184, ll185, ll186, ll187,
        ll188, ll189, ll190, ll191, ll192, ll193, ll194, ll195, ll196, ll197,
        ll198, ll199, ll200, ll201, ll202, ll203, ll204, ll205, ll206, ll207,
        ll208, ll209, ll210, ll211, ll212, ll213, ll214, ll215, ll216, ll217,
        ll218, ll219, ll220, ll221, ll222, ll223, ll224, ll225, ll226, ll227,
        ll228, ll229, ll230, ll231, ll232, ll233, ll234, ll235, ll236, ll237,
        ll238, ll239, ll240, ll241, ll242, ll243, ll244, ll245, ll246, ll247,
        ll248, ll249, ll250, ll251, ll252, ll253, ll254, ll255, ll256, ll257,
        ll258, ll259, ll260, ll261, ll262, ll263, ll264, ll265, ll266, ll267,
        ll268, ll269, ll270, ll271, ll272, ll273, ll274, ll275, ll276, ll277,
        ll278, ll279, ll280, ll281, ll282, ll283, ll284, ll285, ll286, ll287,
        ll288, ll289, ll290, ll291, ll292, ll293, ll294, ll295, ll296, ll297,
        ll298, ll299, ll300, ll301, ll302, ll303, ll304, ll305, ll306, ll307,
        ll308, ll309, ll310, ll311, ll312, ll313, ll314, ll315, ll316, ll317,
        ll318, ll319, ll320, ll321, ll322, ll323, ll324, ll325, ll326, ll327,
        ll328, ll329, ll330, ll331, ll332, ll333, ll334, ll335, ll336, ll337,
        ll338, ll339, ll340, ll341, ll342, ll343, ll344, ll345, ll346, ll347,
        ll348, ll349, ll350, ll351, ll352, ll353, ll354, ll355, ll356, ll357,
        ll358, ll359, ll360, ll361, ll362, ll363, ll364, ll365, ll366, ll367,
        ll368, ll369, ll370, ll371, ll372, ll373, ll374, ll375, ll376, ll377,
        ll378, ll379, ll380, ll381, ll382, ll383, ll384, ll385, ll386, ll387,
        ll388, ll389, ll390, ll391, ll392, ll393, ll394, ll395, ll396, ll397,
        ll398, ll399, ll400, ll401, ll402, ll403, ll404, ll405, ll406, ll407,
        ll408, ll409, ll410, ll411, ll412, ll413, ll414, ll415, ll416, ll417,
        ll418, ll419, ll420, ll421, ll422, ll423, ll424, ll425, ll426, ll427,
        ll428, ll429, ll430, ll431, ll432, ll433, ll434, ll435, ll436, ll437,
        ll438, ll439, ll440, ll441, ll442, ll443, ll444, ll445, ll446, ll447,
        ll448, ll449, ll450, ll451, ll452, ll453, ll454, ll455, ll456, ll457,
        ll458, ll459, ll460, ll461, ll462, ll463, ll464, ll465, ll466, ll467,
        ll468, ll469, ll470, ll471, ll472, ll473, ll474, ll475, ll476, ll477,
        ll478, ll479, ll480, ll481, ll482, ll483, ll484, ll485, ll486, ll487,
        ll488, ll489, ll490, ll491, ll492, ll493, ll494, ll495, ll496, ll497,
        ll498, ll499, ll500, ll501, ll502, ll503, ll504, ll505, ll506, ll507,
        ll508, ll509, ll510, ll511;

        if (deopt) {
            method_entered = true;
            synchronized(monitor) {
                do_monitor_acquire = true;
                UnloadedClass1 res = new UnloadedClass1(); // forces deopt with c2
                res.field = 0; //forced deopt with c1
                return res;
            }
        }
        return null;
    }

    static boolean m2(boolean deopt) {
        long l0, l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12,
        l13, l14, l15, l16, l17, l18, l19, l20, l21, l22, l23, l24,
        l25, l26, l27, l28, l29, l30, l31, l32, l33, l34, l35, l36,
        l37, l38, l39, l40, l41, l42, l43, l44, l45, l46, l47, l48,
        l49, l50, l51, l52, l53, l54, l55, l56, l57, l58, l59, l60,
        l61, l62, l63, l64, l65, l66, l67, l68, l69, l70, l71, l72,
        l73, l74, l75, l76, l77, l78, l79, l80, l81, l82, l83, l84,
        l85, l86, l87, l88, l89, l90, l91, l92, l93, l94, l95, l96,
        l97, l98, l99, l100, l101, l102, l103, l104, l105, l106, l107,
        l108, l109, l110, l111, l112, l113, l114, l115, l116, l117,
        l118, l119, l120, l121, l122, l123, l124, l125, l126, l127,
        l128, l129, l130, l131, l132, l133, l134, l135, l136, l137,
        l138, l139, l140, l141, l142, l143, l144, l145, l146, l147,
        l148, l149, l150, l151, l152, l153, l154, l155, l156, l157,
        l158, l159, l160, l161, l162, l163, l164, l165, l166, l167,
        l168, l169, l170, l171, l172, l173, l174, l175, l176, l177,
        l178, l179, l180, l181, l182, l183, l184, l185, l186, l187,
        l188, l189, l190, l191, l192, l193, l194, l195, l196, l197,
        l198, l199, l200, l201, l202, l203, l204, l205, l206, l207,
        l208, l209, l210, l211, l212, l213, l214, l215, l216, l217,
        l218, l219, l220, l221, l222, l223, l224, l225, l226, l227,
        l228, l229, l230, l231, l232, l233, l234, l235, l236, l237,
        l238, l239, l240, l241, l242, l243, l244, l245, l246, l247,
        l248, l249, l250, l251, l252, l253, l254, l255, l256, l257,
        l258, l259, l260, l261, l262, l263, l264, l265, l266, l267,
        l268, l269, l270, l271, l272, l273, l274, l275, l276, l277,
        l278, l279, l280, l281, l282, l283, l284, l285, l286, l287,
        l288, l289, l290, l291, l292, l293, l294, l295, l296, l297,
        l298, l299, l300, l301, l302, l303, l304, l305, l306, l307,
        l308, l309, l310, l311, l312, l313, l314, l315, l316, l317,
        l318, l319, l320, l321, l322, l323, l324, l325, l326, l327,
        l328, l329, l330, l331, l332, l333, l334, l335, l336, l337,
        l338, l339, l340, l341, l342, l343, l344, l345, l346, l347,
        l348, l349, l350, l351, l352, l353, l354, l355, l356, l357,
        l358, l359, l360, l361, l362, l363, l364, l365, l366, l367,
        l368, l369, l370, l371, l372, l373, l374, l375, l376, l377,
        l378, l379, l380, l381, l382, l383, l384, l385, l386, l387,
        l388, l389, l390, l391, l392, l393, l394, l395, l396, l397,
        l398, l399, l400, l401, l402, l403, l404, l405, l406, l407,
        l408, l409, l410, l411, l412, l413, l414, l415, l416, l417,
        l418, l419, l420, l421, l422, l423, l424, l425, l426, l427,
        l428, l429, l430, l431, l432, l433, l434, l435, l436, l437,
        l438, l439, l440, l441, l442, l443, l444, l445, l446, l447,
        l448, l449, l450, l451, l452, l453, l454, l455, l456, l457,
        l458, l459, l460, l461, l462, l463, l464, l465, l466, l467,
        l468, l469, l470, l471, l472, l473, l474, l475, l476, l477,
        l478, l479, l480, l481, l482, l483, l484, l485, l486, l487,
        l488, l489, l490, l491, l492, l493, l494, l495, l496, l497,
        l498, l499, l500, l501, l502, l503, l504, l505, l506, l507,
        l508, l509, l510, l511;

        boolean do_m3 = false;
        try {
            do_m3 = m2(deopt);
        } catch (StackOverflowError e) {
            return true;
        }
        if (do_m3) {
            try {
                m1(deopt);
            } catch (StackOverflowError e) {}
        }
        return false;
    }

    // Used for synchronization betwen threads
    static volatile boolean thread_started = false;
    static volatile boolean do_monitor_acquire = false;
    static volatile boolean monitor_acquired = false;
    static volatile boolean method_entered = false;

    static Object monitor = new Object();

    static public void main(String[] args) {
        // get m1 compiled
        for (int i = 0; i < 20000; i++) {
            m1(false);
        }

        Thread thread = new Thread() {
            public void run() {
                thread_started = true;
                while(!do_monitor_acquire);
                System.out.println("Ok to try to acquire the lock");
                synchronized(monitor) {
                    monitor_acquired = true;
                }
            }
        };

        thread.setDaemon(true);
        thread.start();

        while(!thread_started);

        m2(true);

        if (!method_entered) {
            System.out.println("TEST PASSED");
            return;
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("Is lock acquired?");
            if (monitor_acquired) {
                System.out.println("TEST PASSED");
                return;
            }
            try {
                Thread.sleep(10000);
            } catch(InterruptedException ie) {
            }
        }
        System.out.println("TEST FAILED");
    }
}
