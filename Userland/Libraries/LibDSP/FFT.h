/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Complex.h>
#include <AK/Math.h>
#include <AK/Span.h>

namespace LibDSP {

constexpr void fft(Span<Complex<double>> sample_data, bool invert = false)
{
    int n = sample_data.size();

    for (int i = 1, j = 0; i < n; i++) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1)
            j ^= bit;
        j ^= bit;

        if (i < j)
            swap(sample_data[i], sample_data[j]);
    }

    for (int len = 2; len <= n; len <<= 1) {
        double ang = 2 * AK::Pi<double> / len * (invert ? -1 : 1);
        Complex<double> wlen(AK::cos(ang), AK::sin(ang));
        for (int i = 0; i < n; i += len) {
            Complex<double> w = { 1., 0. };
            for (int j = 0; j < len / 2; j++) {
                Complex<double> u = sample_data[i + j], v = sample_data[i + j + len / 2] * w;
                sample_data[i + j] = u + v;
                sample_data[i + j + len / 2] = u - v;
                w *= wlen;
            }
        }
    }

    if (invert) {
        for (int i = 0; i < n; i++)
            sample_data[i] /= n;
    }
}

}
