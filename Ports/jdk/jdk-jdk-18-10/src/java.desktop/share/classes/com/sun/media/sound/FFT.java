/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

/**
 * Fast Fourier Transformer.
 *
 * @author Karl Helgason
 */
public final class FFT {

    private final double[] w;
    private final int fftFrameSize;
    private final int sign;
    private final int[] bitm_array;
    private final int fftFrameSize2;

    // Sign = -1 is FFT, 1 is IFFT (inverse FFT)
    // Data = Interlaced double array to be transformed.
    // The order is: real (sin), complex (cos)
    // Framesize must be power of 2
    public FFT(int fftFrameSize, int sign) {
        w = computeTwiddleFactors(fftFrameSize, sign);

        this.fftFrameSize = fftFrameSize;
        this.sign = sign;
        fftFrameSize2 = fftFrameSize << 1;

        // Pre-process Bit-Reversal
        bitm_array = new int[fftFrameSize2];
        for (int i = 2; i < fftFrameSize2; i += 2) {
            int j;
            int bitm;
            for (bitm = 2, j = 0; bitm < fftFrameSize2; bitm <<= 1) {
                if ((i & bitm) != 0)
                    j++;
                j <<= 1;
            }
            bitm_array[i] = j;
        }

    }

    public void transform(double[] data) {
        bitreversal(data);
        calc(fftFrameSize, data, sign, w);
    }

    private static double[] computeTwiddleFactors(int fftFrameSize,
            int sign) {

        int imax = (int) (Math.log(fftFrameSize) / Math.log(2.));

        double[] warray = new double[(fftFrameSize - 1) * 4];
        int w_index = 0;

        for (int i = 0,  nstep = 2; i < imax; i++) {
            int jmax = nstep;
            nstep <<= 1;

            double wr = 1.0;
            double wi = 0.0;

            double arg = Math.PI / (jmax >> 1);
            double wfr = Math.cos(arg);
            double wfi = sign * Math.sin(arg);

            for (int j = 0; j < jmax; j += 2) {
                warray[w_index++] = wr;
                warray[w_index++] = wi;

                double tempr = wr;
                wr = tempr * wfr - wi * wfi;
                wi = tempr * wfi + wi * wfr;
            }
        }

        // PRECOMPUTATION of wwr1, wwi1 for factor 4 Decomposition (3 * complex
        // operators and 8 +/- complex operators)
        {
            w_index = 0;
            int w_index2 = warray.length >> 1;
            for (int i = 0,  nstep = 2; i < (imax - 1); i++) {
                int jmax = nstep;
                nstep *= 2;

                int ii = w_index + jmax;
                for (int j = 0; j < jmax; j += 2) {
                    double wr = warray[w_index++];
                    double wi = warray[w_index++];
                    double wr1 = warray[ii++];
                    double wi1 = warray[ii++];
                    warray[w_index2++] = wr * wr1 - wi * wi1;
                    warray[w_index2++] = wr * wi1 + wi * wr1;
                }
            }

        }

        return warray;
    }

    private static void calc(int fftFrameSize, double[] data, int sign,
            double[] w) {

        final int fftFrameSize2 = fftFrameSize << 1;

        int nstep = 2;

        if (nstep >= fftFrameSize2)
            return;
        int i = nstep - 2;
        if (sign == -1)
            calcF4F(fftFrameSize, data, i, nstep, w);
        else
            calcF4I(fftFrameSize, data, i, nstep, w);

    }

    private static void calcF2E(int fftFrameSize, double[] data, int i,
            int nstep, double[] w) {
        int jmax = nstep;
        for (int n = 0; n < jmax; n += 2) {
            double wr = w[i++];
            double wi = w[i++];
            int m = n + jmax;
            double datam_r = data[m];
            double datam_i = data[m + 1];
            double datan_r = data[n];
            double datan_i = data[n + 1];
            double tempr = datam_r * wr - datam_i * wi;
            double tempi = datam_r * wi + datam_i * wr;
            data[m] = datan_r - tempr;
            data[m + 1] = datan_i - tempi;
            data[n] = datan_r + tempr;
            data[n + 1] = datan_i + tempi;
        }
        return;

    }

    // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
    // complex operators
    private static void calcF4F(int fftFrameSize, double[] data, int i,
            int nstep, double[] w) {
        final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
        // Factor-4 Decomposition

        int w_len = w.length >> 1;
        while (nstep < fftFrameSize2) {

            if (nstep << 2 == fftFrameSize2) {
                // Goto Factor-4 Final Decomposition
                // calcF4E(data, i, nstep, -1, w);
                calcF4FE(fftFrameSize, data, i, nstep, w);
                return;
            }
            int jmax = nstep;
            int nnstep = nstep << 1;
            if (nnstep == fftFrameSize2) {
                // Factor-4 Decomposition not possible
                calcF2E(fftFrameSize, data, i, nstep, w);
                return;
            }
            nstep <<= 2;
            int ii = i + jmax;
            int iii = i + w_len;

            {
                i += 2;
                ii += 2;
                iii += 2;

                for (int n = 0; n < fftFrameSize2; n += nstep) {
                    int m = n + jmax;

                    double datam1_r = data[m];
                    double datam1_i = data[m + 1];
                    double datan1_r = data[n];
                    double datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    double datam2_r = data[m];
                    double datam2_i = data[m + 1];
                    double datan2_r = data[n];
                    double datan2_i = data[n + 1];

                    double tempr = datam1_r;
                    double tempi = datam1_i;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    double n2w1r = datan2_r;
                    double n2w1i = datan2_i;
                    double m2ww1r = datam2_r;
                    double m2ww1i = datam2_i;

                    tempr = m2ww1r - n2w1r;
                    tempi = m2ww1i - n2w1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;

                }
            }

            for (int j = 2; j < jmax; j += 2) {
                double wr = w[i++];
                double wi = w[i++];
                double wr1 = w[ii++];
                double wi1 = w[ii++];
                double wwr1 = w[iii++];
                double wwi1 = w[iii++];
                // double wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                // precomputed!!!
                // double wwi1 = wr * wi1 + wi * wr1;

                for (int n = j; n < fftFrameSize2; n += nstep) {
                    int m = n + jmax;

                    double datam1_r = data[m];
                    double datam1_i = data[m + 1];
                    double datan1_r = data[n];
                    double datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    double datam2_r = data[m];
                    double datam2_i = data[m + 1];
                    double datan2_r = data[n];
                    double datan2_i = data[n + 1];

                    double tempr = datam1_r * wr - datam1_i * wi;
                    double tempi = datam1_r * wi + datam1_i * wr;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    double n2w1r = datan2_r * wr1 - datan2_i * wi1;
                    double n2w1i = datan2_r * wi1 + datan2_i * wr1;
                    double m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                    double m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                    tempr = m2ww1r - n2w1r;
                    tempi = m2ww1i - n2w1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;
                }
            }

            i += jmax << 1;

        }

        calcF2E(fftFrameSize, data, i, nstep, w);

    }

    // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
    // complex operators
    private static void calcF4I(int fftFrameSize, double[] data, int i,
            int nstep, double[] w) {
        final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
        // Factor-4 Decomposition

        int w_len = w.length >> 1;
        while (nstep < fftFrameSize2) {

            if (nstep << 2 == fftFrameSize2) {
                // Goto Factor-4 Final Decomposition
                // calcF4E(data, i, nstep, 1, w);
                calcF4IE(fftFrameSize, data, i, nstep, w);
                return;
            }
            int jmax = nstep;
            int nnstep = nstep << 1;
            if (nnstep == fftFrameSize2) {
                // Factor-4 Decomposition not possible
                calcF2E(fftFrameSize, data, i, nstep, w);
                return;
            }
            nstep <<= 2;
            int ii = i + jmax;
            int iii = i + w_len;
            {
                i += 2;
                ii += 2;
                iii += 2;

                for (int n = 0; n < fftFrameSize2; n += nstep) {
                    int m = n + jmax;

                    double datam1_r = data[m];
                    double datam1_i = data[m + 1];
                    double datan1_r = data[n];
                    double datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    double datam2_r = data[m];
                    double datam2_i = data[m + 1];
                    double datan2_r = data[n];
                    double datan2_i = data[n + 1];

                    double tempr = datam1_r;
                    double tempi = datam1_i;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    double n2w1r = datan2_r;
                    double n2w1i = datan2_i;
                    double m2ww1r = datam2_r;
                    double m2ww1i = datam2_i;

                    tempr = n2w1r - m2ww1r;
                    tempi = n2w1i - m2ww1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;

                }

            }
            for (int j = 2; j < jmax; j += 2) {
                double wr = w[i++];
                double wi = w[i++];
                double wr1 = w[ii++];
                double wi1 = w[ii++];
                double wwr1 = w[iii++];
                double wwi1 = w[iii++];
                // double wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                // precomputed!!!
                // double wwi1 = wr * wi1 + wi * wr1;

                for (int n = j; n < fftFrameSize2; n += nstep) {
                    int m = n + jmax;

                    double datam1_r = data[m];
                    double datam1_i = data[m + 1];
                    double datan1_r = data[n];
                    double datan1_i = data[n + 1];

                    n += nnstep;
                    m += nnstep;
                    double datam2_r = data[m];
                    double datam2_i = data[m + 1];
                    double datan2_r = data[n];
                    double datan2_i = data[n + 1];

                    double tempr = datam1_r * wr - datam1_i * wi;
                    double tempi = datam1_r * wi + datam1_i * wr;

                    datam1_r = datan1_r - tempr;
                    datam1_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    double n2w1r = datan2_r * wr1 - datan2_i * wi1;
                    double n2w1i = datan2_r * wi1 + datan2_i * wr1;
                    double m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                    double m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                    tempr = n2w1r - m2ww1r;
                    tempi = n2w1i - m2ww1i;

                    datam2_r = datam1_r + tempi;
                    datam2_i = datam1_i - tempr;
                    datam1_r = datam1_r - tempi;
                    datam1_i = datam1_i + tempr;

                    tempr = n2w1r + m2ww1r;
                    tempi = n2w1i + m2ww1i;

                    datan2_r = datan1_r - tempr;
                    datan2_i = datan1_i - tempi;
                    datan1_r = datan1_r + tempr;
                    datan1_i = datan1_i + tempi;

                    data[m] = datam2_r;
                    data[m + 1] = datam2_i;
                    data[n] = datan2_r;
                    data[n + 1] = datan2_i;

                    n -= nnstep;
                    m -= nnstep;
                    data[m] = datam1_r;
                    data[m + 1] = datam1_i;
                    data[n] = datan1_r;
                    data[n + 1] = datan1_i;

                }
            }

            i += jmax << 1;

        }

        calcF2E(fftFrameSize, data, i, nstep, w);

    }

    // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
    // complex operators
    private static void calcF4FE(int fftFrameSize, double[] data, int i,
            int nstep, double[] w) {
        final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
        // Factor-4 Decomposition

        int w_len = w.length >> 1;
        while (nstep < fftFrameSize2) {

            int jmax = nstep;
            int nnstep = nstep << 1;
            if (nnstep == fftFrameSize2) {
                // Factor-4 Decomposition not possible
                calcF2E(fftFrameSize, data, i, nstep, w);
                return;
            }
            nstep <<= 2;
            int ii = i + jmax;
            int iii = i + w_len;
            for (int n = 0; n < jmax; n += 2) {
                double wr = w[i++];
                double wi = w[i++];
                double wr1 = w[ii++];
                double wi1 = w[ii++];
                double wwr1 = w[iii++];
                double wwi1 = w[iii++];
                // double wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                // precomputed!!!
                // double wwi1 = wr * wi1 + wi * wr1;

                int m = n + jmax;

                double datam1_r = data[m];
                double datam1_i = data[m + 1];
                double datan1_r = data[n];
                double datan1_i = data[n + 1];

                n += nnstep;
                m += nnstep;
                double datam2_r = data[m];
                double datam2_i = data[m + 1];
                double datan2_r = data[n];
                double datan2_i = data[n + 1];

                double tempr = datam1_r * wr - datam1_i * wi;
                double tempi = datam1_r * wi + datam1_i * wr;

                datam1_r = datan1_r - tempr;
                datam1_i = datan1_i - tempi;
                datan1_r = datan1_r + tempr;
                datan1_i = datan1_i + tempi;

                double n2w1r = datan2_r * wr1 - datan2_i * wi1;
                double n2w1i = datan2_r * wi1 + datan2_i * wr1;
                double m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                double m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                tempr = m2ww1r - n2w1r;
                tempi = m2ww1i - n2w1i;

                datam2_r = datam1_r + tempi;
                datam2_i = datam1_i - tempr;
                datam1_r = datam1_r - tempi;
                datam1_i = datam1_i + tempr;

                tempr = n2w1r + m2ww1r;
                tempi = n2w1i + m2ww1i;

                datan2_r = datan1_r - tempr;
                datan2_i = datan1_i - tempi;
                datan1_r = datan1_r + tempr;
                datan1_i = datan1_i + tempi;

                data[m] = datam2_r;
                data[m + 1] = datam2_i;
                data[n] = datan2_r;
                data[n + 1] = datan2_i;

                n -= nnstep;
                m -= nnstep;
                data[m] = datam1_r;
                data[m + 1] = datam1_i;
                data[n] = datan1_r;
                data[n + 1] = datan1_i;

            }

            i += jmax << 1;

        }
    }

    // Perform Factor-4 Decomposition with 3 * complex operators and 8 +/-
    // complex operators
    private static void calcF4IE(int fftFrameSize, double[] data, int i,
            int nstep, double[] w) {
        final int fftFrameSize2 = fftFrameSize << 1; // 2*fftFrameSize;
        // Factor-4 Decomposition

        int w_len = w.length >> 1;
        while (nstep < fftFrameSize2) {

            int jmax = nstep;
            int nnstep = nstep << 1;
            if (nnstep == fftFrameSize2) {
                // Factor-4 Decomposition not possible
                calcF2E(fftFrameSize, data, i, nstep, w);
                return;
            }
            nstep <<= 2;
            int ii = i + jmax;
            int iii = i + w_len;
            for (int n = 0; n < jmax; n += 2) {
                double wr = w[i++];
                double wi = w[i++];
                double wr1 = w[ii++];
                double wi1 = w[ii++];
                double wwr1 = w[iii++];
                double wwi1 = w[iii++];
                // double wwr1 = wr * wr1 - wi * wi1; // these numbers can be
                // precomputed!!!
                // double wwi1 = wr * wi1 + wi * wr1;

                int m = n + jmax;

                double datam1_r = data[m];
                double datam1_i = data[m + 1];
                double datan1_r = data[n];
                double datan1_i = data[n + 1];

                n += nnstep;
                m += nnstep;
                double datam2_r = data[m];
                double datam2_i = data[m + 1];
                double datan2_r = data[n];
                double datan2_i = data[n + 1];

                double tempr = datam1_r * wr - datam1_i * wi;
                double tempi = datam1_r * wi + datam1_i * wr;

                datam1_r = datan1_r - tempr;
                datam1_i = datan1_i - tempi;
                datan1_r = datan1_r + tempr;
                datan1_i = datan1_i + tempi;

                double n2w1r = datan2_r * wr1 - datan2_i * wi1;
                double n2w1i = datan2_r * wi1 + datan2_i * wr1;
                double m2ww1r = datam2_r * wwr1 - datam2_i * wwi1;
                double m2ww1i = datam2_r * wwi1 + datam2_i * wwr1;

                tempr = n2w1r - m2ww1r;
                tempi = n2w1i - m2ww1i;

                datam2_r = datam1_r + tempi;
                datam2_i = datam1_i - tempr;
                datam1_r = datam1_r - tempi;
                datam1_i = datam1_i + tempr;

                tempr = n2w1r + m2ww1r;
                tempi = n2w1i + m2ww1i;

                datan2_r = datan1_r - tempr;
                datan2_i = datan1_i - tempi;
                datan1_r = datan1_r + tempr;
                datan1_i = datan1_i + tempi;

                data[m] = datam2_r;
                data[m + 1] = datam2_i;
                data[n] = datan2_r;
                data[n + 1] = datan2_i;

                n -= nnstep;
                m -= nnstep;
                data[m] = datam1_r;
                data[m + 1] = datam1_i;
                data[n] = datan1_r;
                data[n + 1] = datan1_i;

            }

            i += jmax << 1;

        }
    }

    private void bitreversal(double[] data) {
        if (fftFrameSize < 4)
            return;

        int inverse = fftFrameSize2 - 2;
        for (int i = 0; i < fftFrameSize; i += 4) {
            int j = bitm_array[i];

            // Performing Bit-Reversal, even v.s. even, O(2N)
            if (i < j) {

                int n = i;
                int m = j;

                // COMPLEX: SWAP(data[n], data[m])
                // Real Part
                double tempr = data[n];
                data[n] = data[m];
                data[m] = tempr;
                // Imagery Part
                n++;
                m++;
                double tempi = data[n];
                data[n] = data[m];
                data[m] = tempi;

                n = inverse - i;
                m = inverse - j;

                // COMPLEX: SWAP(data[n], data[m])
                // Real Part
                tempr = data[n];
                data[n] = data[m];
                data[m] = tempr;
                // Imagery Part
                n++;
                m++;
                tempi = data[n];
                data[n] = data[m];
                data[m] = tempi;
            }

            // Performing Bit-Reversal, odd v.s. even, O(N)

            int m = j + fftFrameSize; // bitm_array[i+2];
            // COMPLEX: SWAP(data[n], data[m])
            // Real Part
            int n = i + 2;
            double tempr = data[n];
            data[n] = data[m];
            data[m] = tempr;
            // Imagery Part
            n++;
            m++;
            double tempi = data[n];
            data[n] = data[m];
            data[m] = tempi;
        }
    }
}
