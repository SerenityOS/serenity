/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.Sequence;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

/**
 * @test
 * @bug 4666845
 * @summary RFE: Add properties to AudioFileFormat and MidiFileFormat
 */
public class Properties {

    static boolean g_failed = false;

    // all of p1 need to be in p2
    static boolean compare(Map p1, Map p2) {
        boolean failed = false;
        for(String key: (Set<String>) p1.keySet()) {
            out("  testing key: "+key);
            if (!p2.containsKey(key)) {
                out("  missing property: '"+key+"'. Failed");
                failed = true;
            }
            Object v1 = p1.get(key);
            Object v2 = p2.get(key);
            if (((v1 == null) && (v2 != null))
                || ((v1 != null) && (v2 == null))
                || !(v1.equals(v2))) {
                out("  property '"+key+"' is different: "
                    +"expected='"+v1+"'  "
                    +"actual='"+v2+"'. Failed");
                failed = true;
            }
        }
        // test if we can modify p2
        try {
             int oldSize = p2.size();
             p2.clear();
             if (oldSize > 0 && p2.size() == 0) {
                 out("  could clear the properties! Failed.");
                 failed = true;
             }
        } catch (Exception e) {
            // correct
        }
        return failed;
    }

    public static void main(String argv[]) throws Exception {
        // don't need to catch exceptions: any exception is a
        // failure of this test

        Map<String, Object> p = new HashMap<String,Object>();
        p.put("author", "Florian");
        p.put("duration", new Long(1000));
        p.put("MyProp", "test");

        out("Testing AudioFileFormat properties:");
        // create an AudioFileFormat with properties
        AudioFormat format = new AudioFormat( 44100.0f, 16, 2, true, false);
        AudioFileFormat aff =
            new AudioFileFormat(AudioFileFormat.Type.WAVE,
                                format, 1000, p);
        // test that it has the properties
        boolean failed = compare(p, aff.properties());
        // test getProperty()
        Object o = aff.getProperty("author");
        if (o == null || !o.equals("Florian")) {
            out("  getProperty did not report an existing property!");
            failed = true;
        }
        o = aff.getProperty("does not exist");
        if (o != null) {
            out("  getProperty returned something for a non-existing property!");
            failed = true;
        }
        if (!failed) {
            out("  OK");
        } else {
            g_failed = true;
        }



        out("Testing MidiFileFormat properties:");
        // create a MidiFileFormat with properties
        MidiFileFormat mff =
            new MidiFileFormat(0, Sequence.PPQ, 240,
                               1000, 100, p);
        // test that it has the properties
        failed = compare(p, mff.properties());
        // test getProperty()
        o = mff.getProperty("author");
        if (o == null || !o.equals("Florian")) {
            out("  getProperty did not report an existing property!");
            failed = true;
        }
        o = mff.getProperty("does not exist");
        if (o != null) {
            out("  getProperty returned something for a non-existing property!");
            failed = true;
        }
        if (!failed) {
            out("  OK");
        } else {
            g_failed = true;
        }


        if (g_failed) throw new Exception("Test FAILED!");
        System.out.println("Test passed.");
    }

    static void out(String s) {
        System.out.println(s);
    }
}
