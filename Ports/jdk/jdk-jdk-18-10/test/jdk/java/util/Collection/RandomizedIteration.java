/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8201518
 * @key     randomness
 * @summary Ensure that randomized iteration order of unmodifiable sets
 *          and maps is actually randomized. Must be run othervm so that
 *          the per-VM-instance salt value differs.
 * @run main/othervm RandomizedIteration 0
 * @run main/othervm RandomizedIteration 1
 * @run main/othervm RandomizedIteration 2
 * @run main/othervm RandomizedIteration 3
 * @run main/othervm RandomizedIteration 4
 * @run main/othervm RandomizedIteration verify 5
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Test of randomized iteration of unmodifiable sets and maps.
 *
 * Usage: RandomizedIteration n
 *            - writes files suffixed with 'n' containing set elements and map keys
 *              in iteration order
 *        RandomizedIteration "verify" count
 *            - reads files 0..count-1 and checks to ensure that their orders differ
 *
 * The idea is to generate several test files by invoking this test with an arg
 * of 0 through count-1. Then invoke the test once more with two args, the first being
 * the word "verify" and the second arg being the count. This will read all the generated
 * files and perform verification.
 *
 * The test is considered to pass if any of the runs result in different iteration
 * orders. The randomization is not actually very random, so over many runs there is
 * the possibility of a couple of the test files having the same order. That's ok, as
 * long as the iteration order is usually different. The test fails if *all* of the
 * iteration orders are the same.
 */
public class RandomizedIteration {
    /**
     * Generates a set and a map from the word array, and then writes
     * text files "set.#" and "map.#" containing the set elements and
     * map keys in iteration order.
     *
     * @param suffix number used for the file suffix
     */
    static void writeFiles(int suffix) throws IOException {
        try (PrintStream setOut = new PrintStream("set." + suffix)) {
            Set.of(WORDS)
               .forEach(setOut::println);
        }

        try (PrintStream mapOut = new PrintStream("map." + suffix)) {
            var map = Map.ofEntries(Arrays.stream(WORDS)
                                          .map(word -> Map.entry(word, ""))
                                          .toArray(Map.Entry<?, ?>[]::new));
            map.keySet()
               .forEach(mapOut::println);
        }
    }

    /**
     * Reads lines from each file derived from the prefix and index from 0..count-1
     * into a list, computes its hashcode, and returns a set of those hashcodes.
     * The hashcode of the list is order sensitive, so the same lines in a different
     * order should have different hashcodes.
     *
     * @param prefix the file prefix
     * @param count the number of files to read
     * @return a set of hashcodes of each file
     */
    static Set<Integer> readFiles(String prefix, int count) throws IOException {
        Set<Integer> hashes = new HashSet<>();
        for (int suffix = 0; suffix < count; suffix++) {
            String name = prefix + suffix;
            int hash = Files.readAllLines(Paths.get(name)).hashCode();
            System.out.println(name + ": " + hash);
            hashes.add(hash);
        }
        return hashes;
    }

    /**
     * Test main routine.
     *
     * @param args n | "verify" count
     * @throws IOException if an error occurred
     */
    public static void main(String[] args) throws IOException {
        if ("verify".equals(args[0])) {
            int count = Integer.parseInt(args[1]);
            System.out.println("Verifying " + count + " files.");
            Set<Integer> setHashes = readFiles("set.", count);
            Set<Integer> mapHashes = readFiles("map.", count);
            if (setHashes.size() > 1 && mapHashes.size() > 1) {
                System.out.println("Passed: differing iteration orders were detected.");
            } else {
                throw new AssertionError("FAILED: iteration order not randomized!");
            }
        } else {
            int suffix = Integer.parseInt(args[0]);
            System.out.println("Generating files: " + suffix);
            writeFiles(suffix);
        }
    }

    /**
     * List of 63 words of 22 or more letters from BSD /usr/share/dict/words.
     */
    static final String[] WORDS = {
        "anatomicophysiological",
        "anthropomorphologically",
        "aquopentamminecobaltic",
        "blepharoconjunctivitis",
        "blepharosphincterectomy",
        "cholecystenterorrhaphy",
        "cholecystoduodenostomy",
        "choledochoduodenostomy",
        "counterexcommunication",
        "dacryocystoblennorrhea",
        "dacryocystosyringotomy",
        "deanthropomorphization",
        "duodenocholecystostomy",
        "electroencephalography",
        "electrotelethermometer",
        "epididymodeferentectomy",
        "formaldehydesulphoxylate",
        "formaldehydesulphoxylic",
        "gastroenteroanastomosis",
        "hematospectrophotometer",
        "hexamethylenetetramine",
        "hexanitrodiphenylamine",
        "historicocabbalistical",
        "hydropneumopericardium",
        "hyperconscientiousness",
        "laparocolpohysterotomy",
        "lymphangioendothelioma",
        "macracanthrorhynchiasis",
        "microcryptocrystalline",
        "naphthylaminesulphonic",
        "nonrepresentationalism",
        "omnirepresentativeness",
        "pancreaticoduodenostomy",
        "pancreaticogastrostomy",
        "pathologicohistological",
        "pathologicopsychological",
        "pericardiomediastinitis",
        "phenolsulphonephthalein",
        "philosophicohistorical",
        "philosophicotheological",
        "photochronographically",
        "photospectroheliograph",
        "pneumohydropericardium",
        "pneumoventriculography",
        "polioencephalomyelitis",
        "Prorhipidoglossomorpha",
        "Pseudolamellibranchiata",
        "pseudolamellibranchiate",
        "pseudomonocotyledonous",
        "pyopneumocholecystitis",
        "scientificogeographical",
        "scientificophilosophical",
        "scleroticochorioiditis",
        "stereophotomicrography",
        "tetraiodophenolphthalein",
        "theologicoastronomical",
        "theologicometaphysical",
        "thymolsulphonephthalein",
        "thyroparathyroidectomize",
        "thyroparathyroidectomy",
        "transubstantiationalist",
        "ureterocystanastomosis",
        "zoologicoarchaeologist"
    };
}
