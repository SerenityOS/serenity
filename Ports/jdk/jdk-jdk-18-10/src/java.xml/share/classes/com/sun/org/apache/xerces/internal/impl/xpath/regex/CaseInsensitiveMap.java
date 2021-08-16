/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xpath.regex;

/**
 */

final class CaseInsensitiveMap {

    private static final int CHUNK_SHIFT = 10;           /* 2^10 = 1k */
    private static final int CHUNK_SIZE = (1<<CHUNK_SHIFT);
    private static final int CHUNK_MASK = (CHUNK_SIZE-1);
    private static final int INITIAL_CHUNK_COUNT = 64;   /* up to 0xFFFF */

    private static int[][][] caseInsensitiveMap;

    private static final int LOWER_CASE_MATCH = 1;
    private static final int UPPER_CASE_MATCH = 2;

    static {
        buildCaseInsensitiveMap();
    }

    /**
     *  Return a list of code point characters (not including the input value)
     *  that can be substituted in a case insensitive match
     */
    static public int[] get(int codePoint) {
        return (codePoint < 0x10000) ? getMapping(codePoint) : null;
    }

    private static int[] getMapping(int codePoint) {
        int chunk = codePoint >>> CHUNK_SHIFT;
        int offset = codePoint & CHUNK_MASK;

        return caseInsensitiveMap[chunk][offset];
    }

    private static void buildCaseInsensitiveMap() {
        caseInsensitiveMap = new int[INITIAL_CHUNK_COUNT][CHUNK_SIZE][];
        int lc, uc;
        for (int i=0; i<0x10000; i++) {
            lc = Character.toLowerCase(i);
            uc = Character.toUpperCase(i);

            // lower/upper case value is not the same as code point
            if (lc != uc || lc != i) {
                int[] map = new int[2];
                int index = 0;

                if (lc != i) {
                    map[index++] = lc;
                    map[index++] = LOWER_CASE_MATCH;
                    int[] lcMap = getMapping(lc);
                    if (lcMap != null) {
                        map = updateMap(i, map, lc, lcMap, LOWER_CASE_MATCH);
                    }
                }

                if (uc != i) {
                    if (index == map.length) {
                        map = expandMap(map, 2);
                    }
                    map[index++] = uc;
                    map[index++] = UPPER_CASE_MATCH;
                    int[] ucMap = getMapping(uc);
                    if (ucMap != null) {
                        map = updateMap(i, map, uc, ucMap, UPPER_CASE_MATCH);
                    }
                }

                set(i, map);
            }
        }
    }

    private static int[] expandMap(int[] srcMap, int expandBy) {
        final int oldLen = srcMap.length;
        int[] newMap = new int[oldLen + expandBy];

        System.arraycopy(srcMap, 0, newMap, 0, oldLen);
        return newMap;
    }

    private static void set(int codePoint, int[] map) {
        int chunk = codePoint >>> CHUNK_SHIFT;
        int offset = codePoint & CHUNK_MASK;

        caseInsensitiveMap[chunk][offset] = map;
    }

    private static int[] updateMap(int codePoint, int[] codePointMap,
            int ciCodePoint, int[] ciCodePointMap, int matchType) {
        for (int i=0; i<ciCodePointMap.length; i+=2) {
            int c = ciCodePointMap[i];
            int[] cMap = getMapping(c);
            if (cMap != null) {
                if (contains(cMap, ciCodePoint, matchType)) {
                    if (!contains(cMap, codePoint)) {
                        cMap = expandAndAdd(cMap, codePoint, matchType);
                        set(c, cMap);
                    }
                    if (!contains(codePointMap, c)) {
                        codePointMap = expandAndAdd(codePointMap, c,matchType);
                    }
                }
            }
        }

        if (!contains(ciCodePointMap, codePoint)) {
            ciCodePointMap = expandAndAdd(ciCodePointMap, codePoint, matchType);
            set(ciCodePoint, ciCodePointMap);
        }

        return codePointMap;
    }

    private static boolean contains(int[] map, int codePoint) {
        for (int i=0; i<map.length; i += 2) {
            if (map[i] == codePoint) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(int[] map, int codePoint, int matchType) {
        for (int i=0; i<map.length; i += 2) {
            if (map[i] == codePoint && map[i+1] == matchType) {
                return true;
            }
        }
        return false;
    }

    private static int[] expandAndAdd(int[] srcMap, int codePoint, int matchType) {
        final int oldLen = srcMap.length;
        int[] newMap = new int[oldLen + 2];

        System.arraycopy(srcMap, 0, newMap, 0, oldLen);
        newMap[oldLen] = codePoint;
        newMap[oldLen+1] = matchType;
        return newMap;
    }
}
