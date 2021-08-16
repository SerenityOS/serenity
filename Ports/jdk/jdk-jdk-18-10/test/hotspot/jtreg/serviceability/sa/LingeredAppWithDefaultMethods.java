/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.apps.LingeredApp;

interface Language {
    static final long nbrOfWords = 99999;
    public abstract long getNbrOfWords();
    default boolean hasScript() {
        return true;
    }
}

class ParselTongue implements Language {
    public long getNbrOfWords() {
        return nbrOfWords * 4;
    }
}

class SlytherinSpeak extends ParselTongue {
    public boolean hasScript() {
        return false;
    }
}

public class LingeredAppWithDefaultMethods extends LingeredApp {

    public static void main(String args[]) {
        ParselTongue lang = new ParselTongue();
        SlytherinSpeak slang = new SlytherinSpeak();
        System.out.println(lang.hasScript() || slang.hasScript());

        LingeredApp.main(args);
    }
 }
