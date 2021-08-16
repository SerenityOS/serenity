/*
 *
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */



import java.util.Random;

/**
 * An object that implements a cheesy pseudorandom permutation of the integers
 * from zero to some user-specified value. (The permutation is a linear
 * function.)
 *
 * @author Josh Bloch
 */
class Permuter {
    /**
     * The size of the permutation.
     */
    private int modulus;

    /**
     * Nonnegative integer less than n that is relatively prime to m.
     */
    private int multiplier;

    /**
     * Pseudorandom nonnegative integer less than n.
     */
    private int addend = 22;

    public Permuter(int n) {
        if (n<0) {
            throw new IllegalArgumentException();
        }
        modulus = n;
        if (n==1) {
            return;
        }

        // Initialize the multiplier and offset
        multiplier = (int) Math.sqrt(n);
        while (gcd(multiplier, n) != 1) {
            if (++multiplier == n) {
                multiplier = 1;
            }
        }
    }

    /**
     * Returns the integer to which this permuter maps the specified integer.
     * The specified integer must be between 0 and n-1, and the returned
     * integer will be as well.
     */
    public int map(int i) {
        return (multiplier * i + addend) % modulus;
    }

    /**
     * Calculate GCD of a and b, which are assumed to be non-negative.
     */
    private static int gcd(int a, int b) {
        while(b != 0) {
            int tmp = a % b;
            a = b;
            b = tmp;
        }
        return a;
    }

    /**
     * Simple test.  Takes modulus on command line and prints out permutation.
     */
    public static void main(String[] args) {
        int modulus = Integer.parseInt(args[0]);
        Permuter p = new Permuter(modulus);
        for (int i=0; i<modulus; i++) {
            System.out.print(p.map(i)+" ");
        }
        System.out.println();
    }
}
