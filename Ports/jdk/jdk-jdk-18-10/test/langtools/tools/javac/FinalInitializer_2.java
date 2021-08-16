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
 * @bug 4974917
 * @summary bogus "already initialized" error
 * @author tball
 *
 * @compile FinalInitializer_2.java
 */

public class FinalInitializer_2 {
    // customer-supplied test case
    public void doKMDiscard()
    {

      //the problem will be gone by deleting any of the following
      //5 lines. (just comment them out works too)
      {
        final int t=0;
        final int degCnns[][]=null;
        int sklTmpGrps=0;
      }

      final int sklGrpCnt;
      //the problem will be gone by deleting the loop or
      //the final line or just delete the word 'final'
      for(int i=0;i<1;i++){
        final int j=0;
      }
      sklGrpCnt=0;
    }
}
