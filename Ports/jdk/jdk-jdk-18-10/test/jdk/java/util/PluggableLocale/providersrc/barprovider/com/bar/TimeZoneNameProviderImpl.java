/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package com.bar;

import java.util.*;
import java.util.spi.*;

import com.foobar.Utils;

public class TimeZoneNameProviderImpl extends TimeZoneNameProvider {
    static Locale[] avail = {new Locale("ja", "JP", "osaka"),
                        new Locale("ja", "JP", "kyoto"),
                        new Locale("xx"),
                        Locale.JAPAN};

    static String[][] zoneOsaka = {
        {"GMT",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "G_M_T_\u3084_\u3002",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "G_M_T_\u3084_\u3002"},
        {"JST",
         "\u3084_\u307e_\u3068_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "J_S_T_\u3084_\u3002",
         "\u3084_\u307e_\u3068_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "J_S_T_\u3084_\u3002"},
        {"America/Los_Angeles",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "P_S_T_\u3084_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3084_\u3002",
         "P_D_T_\u3084_\u3002"},
        {"SystemV/PST8",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "P_S_T_\u3084_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3084_\u3002",
         "P_D_T_\u3084_\u3002"},
        {"SystemV/PST8PDT",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "P_S_T_\u3084_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3084_\u3002",
         "P_D_T_\u3084_\u3002"},
        {"PST8PDT",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3084_\u3002",
         "P_S_T_\u3084_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3084_\u3002",
         "P_D_T_\u3084_\u3002"},
    };

    static String[][] zoneKyoto = {
        {"GMT",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "G_M_T_\u3069_\u3059_\u3002",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "G_M_T_\u3069_\u3059_\u3002"},
        {"America/Los_Angeles",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "P_S_T_\u3069_\u3059_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3069_\u3059_\u3002",
         "P_D_T_\u3069_\u3059_\u3002"},
        {"SystemV/PST8",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "P_S_T_\u3069_\u3059_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3069_\u3059_\u3002",
         "P_D_T_\u3069_\u3059_\u3002"},
        {"SystemV/PST8PDT",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "P_S_T_\u3069_\u3059_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3069_\u3059_\u3002",
         "P_D_T_\u3069_\u3059_\u3002"},
        {"PST8PDT",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3069_\u3059_\u3002",
         "P_S_T_\u3069_\u3059_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3069_\u3059_\u3002",
         "P_D_T_\u3069_\u3059_\u3002"},
    };

    static String[][] zoneXX = {
        {"GMT",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642\u3070\u3064\u3070\u3064\u3002",
         "G_M_T_\u3070\u3064\u3070\u3064\u3002",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642\u3070\u3064\u3070\u3064\u3002",
         "G_M_T_\u3070\u3064\u3070\u3064\u3002"},
        {"America/Los_Angeles",
         "\u592a_\u5e73_\u6d0b_\u6a19_\u6e96_\u6642_\u3070\u3064\u3070\u3064\u3002",
         "P_S_T_\u3070\u3064\u3070\u3064\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3070\u3064\u3070\u3064\u3002",
         "P_D_T_\u3070\u3064\u3070\u3064\u3002"}};

    static String[][] zoneJaJP = {
        {"GMT",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3067_\u3059_\u3002",
         "G_M_T_\u3067_\u3059_\u3002",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3067_\u3059_\u3002",
         "G_M_T_\u3067_\u3059_\u3002"},
        {"America/Los_Angeles",
         "\u30b0_\u30ea_\u30cb_\u30c3_\u30b8_\u6a19_\u6e96_\u6642_\u3067_\u3059_\u3002",
         "P_S_T_\u3067_\u3059_\u3002",
         "\u592a_\u5e73_\u6d0b_\u590f_\u6642_\u9593_\u3067_\u3059_\u3002",
         "P_D_T_\u3067_\u3059_\u3002"}};

    static String[][][] names = {zoneOsaka, zoneKyoto, zoneXX, zoneJaJP};

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public String getDisplayName(String id, boolean dst, int style, Locale language) {
        if (!Utils.supportsLocale(Arrays.asList(avail), language)) {
            throw new IllegalArgumentException("locale is not one of available locales: "+language);
        }

        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], language)) {
                String[][] namesForALocale = names[i];
                for (int j = 0; j < namesForALocale.length; j++) {
                    String[] array = namesForALocale[j];
                    if (id.equals(array[0])) {
                        String ret = array[(style==TimeZone.LONG?0:1)+(dst?2:0)+1];
                        return ret;
                    }
                }
            }
        }
        return null;
    }
}
