/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4902078
 * @summary concurrent modification not detected on 2nd to last iteration
 * @author  Josh Bloch
 *
 * @ignore Bug fix temporarily removed as it uncovered other bugs (4992226)
 */

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class CheckForComodification {
    private static final int LENGTH = 10;
    public static void main(String[] args) throws Exception {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < LENGTH; i++)
            list.add(i);
        try {
            for (int i : list)
                if (i == LENGTH - 2)
                    list.remove(i);
        } catch (ConcurrentModificationException e) {
            return;
        }
        throw new RuntimeException("No ConcurrentModificationException");
    }
}
