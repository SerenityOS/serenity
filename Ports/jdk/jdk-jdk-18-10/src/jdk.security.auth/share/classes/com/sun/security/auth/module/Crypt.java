/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*      Copyright (c) 1988 AT&T */
/*        All Rights Reserved   */

/**
 * Implements the UNIX crypt(3) function, based on a direct port of the
 * libc crypt function.
 *
 * <p>
 * From the crypt man page:
 * <p>
 * crypt() is the password encryption routine, based on the NBS
 * Data  Encryption  Standard,  with variations intended (among
 * other things) to frustrate use of  hardware  implementations
 * of the DES for key search.
 * <p>
 * The first argument to crypt() is  normally  a  user's  typed
 * password.   The  second  is a 2-character string chosen from
 * the set [a-zA-Z0-9./].  the  salt string is used to perturb
 * the DES algorithm in one
 * of 4096 different ways, after which the password is used  as
 * the  key  to  encrypt  repeatedly  a  constant  string.  The
 * returned value points to the encrypted password, in the same
 * alphabet as the salt.  The first two characters are the salt
 * itself.
 *
 * @author Roland Schemers
 */

package com.sun.security.auth.module;

class Crypt {

/* EXPORT DELETE START */

    private static final byte[] IP = {
        58, 50, 42, 34, 26, 18, 10, 2,
        60, 52, 44, 36, 28, 20, 12, 4,
        62, 54, 46, 38, 30, 22, 14, 6,
        64, 56, 48, 40, 32, 24, 16, 8,
        57, 49, 41, 33, 25, 17, 9, 1,
        59, 51, 43, 35, 27, 19, 11, 3,
        61, 53, 45, 37, 29, 21, 13, 5,
        63, 55, 47, 39, 31, 23, 15, 7,
    };

    private static final byte[] FP = {
        40, 8, 48, 16, 56, 24, 64, 32,
        39, 7, 47, 15,  55, 23, 63, 31,
        38, 6, 46, 14, 54, 22, 62, 30,
        37, 5, 45, 13, 53, 21, 61, 29,
        36, 4, 44, 12, 52, 20, 60, 28,
        35, 3, 43, 11, 51, 19, 59, 27,
        34, 2, 42, 10, 50, 18, 58, 26,
        33, 1, 41, 9, 49, 17, 57, 25,
    };

    private static final byte[] PC1_C = {
        57, 49, 41, 33, 25, 17, 9,
        1, 58, 50, 42, 34, 26, 18,
        10, 2, 59, 51, 43, 35, 27,
        19, 11, 3, 60, 52, 44, 36,
    };

    private static final byte[] PC1_D = {
        63, 55, 47, 39, 31, 23, 15,
        7, 62, 54, 46, 38, 30, 22,
        14, 6, 61, 53, 45, 37, 29,
        21, 13, 5, 28, 20, 12, 4,
    };

    private static final byte[] shifts = { 1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1, };

    private static final byte[] PC2_C = {
        14, 17, 11, 24, 1, 5,
        3, 28, 15, 6, 21, 10,
        23, 19, 12, 4, 26, 8,
        16, 7, 27, 20, 13, 2,
    };

    private static final byte[] PC2_D = {
        41,52,31,37,47,55,
        30,40,51,45,33,48,
        44,49,39,56,34,53,
        46,42,50,36,29,32,
    };

    private byte[] C = new byte[28];
    private byte[] D = new byte[28];

    private byte[] KS;

    private byte[] E = new byte[48];

    private static final byte[] e2 = {
        32, 1, 2, 3, 4, 5,
        4, 5, 6, 7, 8, 9,
        8, 9,10,11,12,13,
        12,13,14,15,16,17,
        16,17,18,19,20,21,
        20,21,22,23,24,25,
        24,25,26,27,28,29,
        28,29,30,31,32, 1,
    };

    private void setkey(byte[] key) {
        int i, j, k;
        byte t;

        if (KS == null) {
            KS = new byte[16*48];
        }

        for (i = 0; i < 28; i++) {
                C[i] = key[PC1_C[i]-1];
                D[i] = key[PC1_D[i]-1];
        }
        for (i = 0; i < 16; i++) {
                for (k = 0; k < shifts[i]; k++) {
                        t = C[0];
                        for (j = 0; j < 28-1; j++)
                                C[j] = C[j+1];
                        C[27] = t;
                        t = D[0];
                        for (j = 0; j < 28-1; j++)
                                D[j] = D[j+1];
                        D[27] = t;
                }
                for (j = 0; j < 24; j++) {
                        int index = i * 48;

                        KS[index+j] = C[PC2_C[j]-1];
                        KS[index+j+24] = D[PC2_D[j]-28-1];
                }
        }
        for (i = 0; i < 48; i++)
                E[i] = e2[i];
    }


    private static final byte[][] S = {
        {14, 4,13, 1, 2,15,11, 8, 3,10, 6,12, 5, 9, 0, 7,
        0,15, 7, 4,14, 2,13, 1,10, 6,12,11, 9, 5, 3, 8,
        4, 1,14, 8,13, 6, 2,11,15,12, 9, 7, 3,10, 5, 0,
        15,12, 8, 2, 4, 9, 1, 7, 5,11, 3,14,10, 0, 6,13},

        {15, 1, 8,14, 6,11, 3, 4, 9, 7, 2,13,12, 0, 5,10,
        3,13, 4, 7,15, 2, 8,14,12, 0, 1,10, 6, 9,11, 5,
        0,14, 7,11,10, 4,13, 1, 5, 8,12, 6, 9, 3, 2,15,
        13, 8,10, 1, 3,15, 4, 2,11, 6, 7,12, 0, 5,14, 9},

        {10, 0, 9,14, 6, 3,15, 5, 1,13,12, 7,11, 4, 2, 8,
        13, 7, 0, 9, 3, 4, 6,10, 2, 8, 5,14,12,11,15, 1,
        13, 6, 4, 9, 8,15, 3, 0,11, 1, 2,12, 5,10,14, 7,
         1,10,13, 0, 6, 9, 8, 7, 4,15,14, 3,11, 5, 2,12},

        {7,13,14, 3, 0, 6, 9,10, 1, 2, 8, 5,11,12, 4,15,
        13, 8,11, 5, 6,15, 0, 3, 4, 7, 2,12, 1,10,14, 9,
        10, 6, 9, 0,12,11, 7,13,15, 1, 3,14, 5, 2, 8, 4,
         3,15, 0, 6,10, 1,13, 8, 9, 4, 5,11,12, 7, 2,14},

        {2,12, 4, 1, 7,10,11, 6, 8, 5, 3,15,13, 0,14, 9,
        14,11, 2,12, 4, 7,13, 1, 5, 0,15,10, 3, 9, 8, 6,
         4, 2, 1,11,10,13, 7, 8,15, 9,12, 5, 6, 3, 0,14,
        11, 8,12, 7, 1,14, 2,13, 6,15, 0, 9,10, 4, 5, 3},

        {12, 1,10,15, 9, 2, 6, 8, 0,13, 3, 4,14, 7, 5,11,
        10,15, 4, 2, 7,12, 9, 5, 6, 1,13,14, 0,11, 3, 8,
         9,14,15, 5, 2, 8,12, 3, 7, 0, 4,10, 1,13,11, 6,
         4, 3, 2,12, 9, 5,15,10,11,14, 1, 7, 6, 0, 8,13},

        {4,11, 2,14,15, 0, 8,13, 3,12, 9, 7, 5,10, 6, 1,
        13, 0,11, 7, 4, 9, 1,10,14, 3, 5,12, 2,15, 8, 6,
         1, 4,11,13,12, 3, 7,14,10,15, 6, 8, 0, 5, 9, 2,
         6,11,13, 8, 1, 4,10, 7, 9, 5, 0,15,14, 2, 3,12},

        {13, 2, 8, 4, 6,15,11, 1,10, 9, 3,14, 5, 0,12, 7,
         1,15,13, 8,10, 3, 7, 4,12, 5, 6,11, 0,14, 9, 2,
         7,11, 4, 1, 9,12,14, 2, 0, 6,10,13,15, 3, 5, 8,
         2, 1,14, 7, 4,10, 8,13,15,12, 9, 0, 3, 5, 6,11},
    };


    private static final byte[] P = {
        16, 7,20,21,
        29,12,28,17,
         1,15,23,26,
         5,18,31,10,
         2, 8,24,14,
        32,27, 3, 9,
        19,13,30, 6,
        22,11, 4,25,
    };

    private byte[]  L = new byte[64];
    private byte[] tempL = new byte[32];
    private byte[] f = new byte[32];
    private byte[] preS = new byte[48];


    private void encrypt(byte[] block,int fake) {
        int     i;
        int t, j, k;
        int R = 32; // &L[32]

        if (KS == null) {
            KS = new byte[16*48];
        }

        for(j=0; j < 64; j++) {
            L[j] = block[IP[j]-1];
        }
        for(i=0; i < 16; i++) {
            int index = i * 48;

            for(j=0; j < 32; j++) {
                tempL[j] = L[R+j];
            }
            for(j=0; j < 48; j++) {
                preS[j] = (byte) (L[R+E[j]-1] ^ KS[index+j]);
            }
            for(j=0; j < 8; j++) {
                t = 6*j;
                k = S[j][(preS[t+0]<<5)+
                         (preS[t+1]<<3)+
                         (preS[t+2]<<2)+
                         (preS[t+3]<<1)+
                         (preS[t+4]<<0)+
                         (preS[t+5]<<4)];
                t = 4*j;
                f[t+0] = (byte) ((k>>3)&01);
                f[t+1] = (byte) ((k>>2)&01);
                f[t+2] = (byte) ((k>>1)&01);
                f[t+3] = (byte) ((k>>0)&01);
            }
            for(j=0; j < 32; j++) {
                        L[R+j] = (byte) (L[j] ^ f[P[j]-1]);
            }
            for(j=0; j < 32; j++) {
                        L[j] = tempL[j];
            }
        }
        for(j=0; j < 32; j++) {
            t = L[j];
            L[j] = L[R+j];
            L[R+j] = (byte)t;
        }
        for(j=0; j < 64; j++) {
                block[j] = L[FP[j]-1];
        }
    }
/* EXPORT DELETE END */

    /**
     * Creates a new Crypt object for use with the crypt method.
     *
     */

    public Crypt()
    {
        // does nothing at this time
        super();
    }

    /**
     * Implements the libc crypt(3) function.
     *
     * @param pw the password to "encrypt".
     *
     * @param salt the salt to use.
     *
     * @return A new byte[13] array that contains the encrypted
     * password. The first two characters are the salt.
     *
     */

    public synchronized byte[] crypt(byte[] pw, byte[] salt) {
        int c, i, j, pwi;
        byte temp;
        byte[] block = new byte[66];
        byte[] iobuf = new byte[13];

/* EXPORT DELETE START */

        pwi = 0;

        for(i=0; pwi < pw.length && i < 64; pwi++) {
            c = pw[pwi];
            for(j=0; j < 7; j++, i++) {
                block[i] = (byte) ((c>>(6-j)) & 01);
            }
            i++;
        }

        setkey(block);

        for(i=0; i < 66; i++) {
            block[i] = 0;
        }

        for(i=0; i < 2; i++) {
            c = salt[i];
            iobuf[i] = (byte)c;
            if(c > 'Z')
                c -= 6;
            if(c > '9')
                c -= 7;
            c -= '.';
            for(j=0; j < 6; j++) {
                if( ((c>>j) & 01) != 0) {
                    temp = E[6*i+j];
                    E[6*i+j] = E[6*i+j+24];
                    E[6*i+j+24] = temp;
                }
            }
        }

        for(i=0; i < 25; i++) {
                encrypt(block,0);
        }

        for(i=0; i < 11; i++) {
            c = 0;
            for(j=0; j < 6; j++) {
                c <<= 1;
                c |= block[6*i+j];
            }
            c += '.';
            if(c > '9') {
                c += 7;
            }
            if(c > 'Z') {
                c += 6;
            }
            iobuf[i+2] = (byte)c;
        }
        //iobuf[i+2] = 0;
        if(iobuf[1] == 0) {
            iobuf[1] = iobuf[0];
        }
/* EXPORT DELETE END */
        return(iobuf);
    }

    /**
     * program to test the crypt routine.
     *
     * The first parameter is the cleartext password, the second is
     * the salt to use. The salt should be two characters from the
     * set [a-zA-Z0-9./]. Outputs the crypt result.
     *
     * @param arg command line arguments.
     *
     */

    public static void main(String[] arg) {

        if (arg.length!=2) {
            System.err.println("usage: Crypt password salt");
            System.exit(1);
        }

        Crypt c = new Crypt();
        try {
            byte[] result = c.crypt
                (arg[0].getBytes("ISO-8859-1"), arg[1].getBytes("ISO-8859-1"));
            for (int i=0; i<result.length; i++) {
                System.out.println(" "+i+" "+(char)result[i]);
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            // cannot happen
        }
    }
}
