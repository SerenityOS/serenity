/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "AudioAlgorithms.h"
#include <AK/Complex.h>
#include <math.h>

// This function uses the input vector as output too. therefore, if you wish to
// leave it intact, pass a copy to this function
//
// The sampling frequency must be more than twice the frequency to resolve.
// The sample window must be at least large enough to reflect the periodicity
// of the smallest frequency to be resolved.
//
// For example, to resolve a 10 KHz and a 2 Hz sine waves we need at least
// a samplerate of 20 KHz and a window of 0.5 seconds
//
// If invert is true, this function computes the inverse discrete fourier transform.
//
// The data vector must be a power of 2
// Adapted from https://cp-algorithms.com/algebra/fft.html
void fft(Vector<Complex<double>>& sample_data, bool invert)
{
    int n = sample_data.size();
    auto data = sample_data.data();

    for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1)
            j ^= bit;
        j ^= bit;

        if (i < j)
            swap(data[i], data[j]);
    }

    for (int len = 2; len <= n; len <<= 1) {
        double ang = 2 * M_PI / len * (invert ? -1 : 1);
        Complex<double> wlen(cos(ang), sin(ang));
        for (int i = 0; i < n; i += len) {
            Complex<double> w = { 1., 0. };
            for (int j = 0; j < len / 2; j++) {
                Complex<double> u = data[i + j], v = data[i + j + len / 2] * w;
                data[i + j] = u + v;
                data[i + j + len / 2] = u - v;
                w *= wlen;
            }
        }
    }

    if (invert) {
        for (int i = 0; i < n; i++)
            data[i] /= n;
    }
}
