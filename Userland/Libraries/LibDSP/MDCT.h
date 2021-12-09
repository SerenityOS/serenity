/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Math.h>
#include <AK/Span.h>

namespace LibDSP {

template<size_t N>
requires(N % 2 == 0) class MDCT {
public:
    constexpr MDCT()
    {
        for (size_t n = 0; n < N; n++) {
            for (size_t k = 0; k < N / 2; k++) {
                m_phi[n][k] = AK::cos(AK::Pi<double> / (2 * N) * (2 * n + 1 + N / 2.0) * (2 * k + 1));
            }
        }
    }

    void transform(Span<double const> data, Span<double> output)
    {
        assert(N == 2 * data.size());
        assert(N == output.size());
        for (size_t n = 0; n < N; n++) {
            output[n] = 0;
            for (size_t k = 0; k < N / 2; k++) {
                output[n] += data[k] * m_phi[n][k];
            }
        }
    }

private:
    Array<Array<double, N / 2>, N> m_phi;
};

}
