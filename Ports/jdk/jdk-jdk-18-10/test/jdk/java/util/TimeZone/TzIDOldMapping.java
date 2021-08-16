/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.HashMap;

class TzIDOldMapping {
    static final Map<String, String> MAP = new HashMap<String, String>();
    static {
        String[][] oldmap = {
            { "ACT", "Australia/Darwin" },
            { "AET", "Australia/Sydney" },
            { "AGT", "America/Argentina/Buenos_Aires" },
            { "ART", "Africa/Cairo" },
            { "AST", "America/Anchorage" },
            { "BET", "America/Sao_Paulo" },
            { "BST", "Asia/Dhaka" },
            { "CAT", "Africa/Harare" },
            { "CNT", "America/St_Johns" },
            { "CST", "America/Chicago" },
            { "CTT", "Asia/Shanghai" },
            { "EAT", "Africa/Addis_Ababa" },
            { "ECT", "Europe/Paris" },
            { "EST", "America/New_York" },
            { "HST", "Pacific/Honolulu" },
            { "IET", "America/Indianapolis" },
            { "IST", "Asia/Calcutta" },
            { "JST", "Asia/Tokyo" },
            { "MIT", "Pacific/Apia" },
            { "MST", "America/Denver" },
            { "NET", "Asia/Yerevan" },
            { "NST", "Pacific/Auckland" },
            { "PLT", "Asia/Karachi" },
            { "PNT", "America/Phoenix" },
            { "PRT", "America/Puerto_Rico" },
            { "PST", "America/Los_Angeles" },
            { "SST", "Pacific/Guadalcanal" },
            { "VST", "Asia/Saigon" },
        };
        for (String[] pair : oldmap) {
            MAP.put(pair[0], pair[1]);
        }
    }
}
