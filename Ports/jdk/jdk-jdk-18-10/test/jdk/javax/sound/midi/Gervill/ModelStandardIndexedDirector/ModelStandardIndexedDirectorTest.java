/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @summary Test ModelStandardIndexedDirector class
   @modules java.desktop/com.sun.media.sound
*/

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.sun.media.sound.ModelConnectionBlock;
import com.sun.media.sound.ModelDirectedPlayer;
import com.sun.media.sound.ModelPerformer;
import com.sun.media.sound.ModelStandardDirector;
import com.sun.media.sound.ModelStandardIndexedDirector;

public class ModelStandardIndexedDirectorTest {

    private static String treeToString(TreeSet<Integer> set)
    {
        StringBuffer buff = new StringBuffer();
        boolean first = true;
        for(Integer s : set)
        {
            if(!first)
                buff.append(";");
            buff.append(s);
            first = false;
        }
        return buff.toString();
    }

    private static void testDirector(ModelPerformer[] performers) throws Exception
    {
        final TreeSet<Integer> played = new TreeSet<Integer>();
        ModelDirectedPlayer player = new ModelDirectedPlayer()
        {
            public void play(int performerIndex,
                    ModelConnectionBlock[] connectionBlocks) {
                played.add(performerIndex);
            }
        };
        ModelStandardIndexedDirector idirector =
            new ModelStandardIndexedDirector(performers, player);
        ModelStandardDirector director =
            new ModelStandardDirector(performers, player);

        for (int n = 0; n < 128; n++)
        {
            for (int v = 0; v < 128; v++)
            {
                director.noteOn(n, v);
                String p1 = treeToString(played);
                played.clear();
                idirector.noteOn(n, v);
                String p2 = treeToString(played);
                played.clear();
                if(!p1.equals(p2))
                    throw new Exception(
                            "Note = " + n + ", Vel = " + v + " failed");
            }
        }
    }

    private static void testDirectorCombinations(
            ModelPerformer[] performers) throws Exception
    {
        for (int i = 0; i < performers.length; i++) {
            ModelPerformer[] performers2 = new ModelPerformer[i];
            for (int j = 0; j < performers2.length; j++) {
                performers2[j] = performers[i];
            }
            testDirector(performers2);
        }
    }

    private static void addPerformer(
            List<ModelPerformer> performers,
            int keyfrom,
            int keyto,
            int velfrom,
            int velto)
    {
        ModelPerformer performer = new ModelPerformer();
        performer.setKeyFrom(keyfrom);
        performer.setKeyTo(keyto);
        performer.setVelFrom(velfrom);
        performer.setVelTo(velto);
        performers.add(performer);
    }

    public static void main(String[] args) throws Exception
    {
        // Test collection of normal values
        List<ModelPerformer> performers = new ArrayList<ModelPerformer>();
        addPerformer(performers, 0, 0, 0, 127);
        addPerformer(performers, 0, 50, 0, 127);
        addPerformer(performers, 0, 127, 0, 127);
        addPerformer(performers, 21, 21, 0, 127);
        addPerformer(performers, 21, 60, 0, 127);
        addPerformer(performers, 21, 127, 0, 127);
        addPerformer(performers, 50, 50, 0, 127);
        addPerformer(performers, 50, 60, 0, 127);
        addPerformer(performers, 50, 127, 0, 127);
        addPerformer(performers, 73, 73, 0, 127);
        addPerformer(performers, 73, 80, 0, 127);
        addPerformer(performers, 73, 127, 0, 127);
        addPerformer(performers, 127, 127, 0, 127);
        addPerformer(performers, 0, 0, 60, 127);
        addPerformer(performers, 0, 50, 60, 127);
        addPerformer(performers, 0, 127, 60, 127);
        addPerformer(performers, 21, 21, 60, 127);
        addPerformer(performers, 21, 60, 60, 127);
        addPerformer(performers, 21, 127, 60, 127);
        addPerformer(performers, 50, 50, 60, 127);
        addPerformer(performers, 50, 60, 60, 127);
        addPerformer(performers, 50, 127, 60, 127);
        addPerformer(performers, 73, 73, 60, 127);
        addPerformer(performers, 73, 80, 60, 127);
        addPerformer(performers, 73, 127, 60, 127);
        addPerformer(performers, 127, 127, 60, 127);
        addPerformer(performers, 0, 0, 80, 83);
        addPerformer(performers, 0, 50, 80, 83);
        addPerformer(performers, 0, 127, 80, 83);
        addPerformer(performers, 21, 21, 80, 83);
        addPerformer(performers, 21, 60, 80, 83);
        addPerformer(performers, 21, 127, 80, 83);
        addPerformer(performers, 50, 50, 80, 83);
        addPerformer(performers, 50, 60, 80, 83);
        addPerformer(performers, 50, 127, 80, 83);
        addPerformer(performers, 73, 73, 80, 83);
        addPerformer(performers, 73, 80, 80, 83);
        addPerformer(performers, 73, 127, 80, 83);
        addPerformer(performers, 127, 127, 80, 83);


        testDirectorCombinations(
                performers.toArray(
                        new ModelPerformer[performers.size()])
                );

        // Test reversed values
        performers.clear();
        addPerformer(performers, 50, 30, 80, 83);
        addPerformer(performers, 30, 30, 50, 30);
        addPerformer(performers, 37, 30, 50, 30);
        testDirector(
                performers.toArray(
                        new ModelPerformer[performers.size()])
                );

        // Test out-of-range values
        performers.clear();
        addPerformer(performers, -20, 6, 0, 127);
        addPerformer(performers, 0, 300, 0, 300);
        addPerformer(performers, -2, -8, -5, -9);

        testDirector(
                performers.toArray(
                        new ModelPerformer[performers.size()])
                );

    }
}
