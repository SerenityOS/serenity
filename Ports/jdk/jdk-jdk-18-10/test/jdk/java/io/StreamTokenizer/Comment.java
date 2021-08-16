/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4106810 4027740 4078997 4097738
   @summary Make sure StreamTokenizer will correctly
            parse different types of comments in input.
   */


import java.io.*;

public class Comment {

    public static void main(String[] args) throws Exception {

        File f = new File(System.getProperty("test.src", "."), "input.txt");

        int slashIsCommentStart = 1;
        int slashSlashComment = 2;
        int slashStarComment = 4;

        for (int i = 0; i < 8 ; i++) {
            FileReader reader = new FileReader(f);
            try {
                StreamTokenizer st = new StreamTokenizer(reader);

                /* decide the state of this run */
                boolean slashCommentFlag = ((i & slashIsCommentStart) != 0);
                boolean slashSlashCommentFlag = ((i & slashSlashComment) != 0);
                boolean slashStarCommentFlag = ((i & slashStarComment) != 0);

                /* set the initial state of the tokenizer */
                if (!slashCommentFlag) {
                    st.ordinaryChar('/');
                }
                st.slashSlashComments(slashSlashCommentFlag);
                st.slashStarComments(slashStarCommentFlag);

                /* now go throgh the input file */
                while(st.nextToken() != StreamTokenizer.TT_EOF)
                {
                    String token = st.sval;
                    if (token == null) {
                        continue;
                    } else {
                        if ((token.compareTo("Error1") == 0) && slashStarCommentFlag) {
                            throw new Exception("Failed to pass one line C comments!");
                        }
                        if ((token.compareTo("Error2") == 0) && slashStarCommentFlag) {
                            throw new Exception("Failed to pass multi line C comments!");
                        }
                        if ((token.compareTo("Error3") == 0) && slashSlashCommentFlag) {
                            throw new Exception("Failed to pass C++ comments!");
                        }
                        if ((token.compareTo("Error4") == 0) && slashCommentFlag) {
                            throw new Exception("Failed to pass / comments!");
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
    }
}
