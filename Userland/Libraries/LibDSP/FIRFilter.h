/*
 * Copyright (c) 2023, Sarsaparilla
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>

namespace DSP {

template<typename SampleType, typename TapType>
class FIRFilter {
public:
    static ErrorOr<FIRFilter> create(Span<TapType> coefficients) {
        Vector<TapType> taps;
        TRY(taps.try_resize(coefficients.size()));

        for (size_t i = 0; i < coefficients.size(); i++) {
            taps[i] = coefficients[i];
        }

        Vector<SampleType> buffer;
        TRY(buffer.try_resize(coefficients.size()));

        return FIRFilter(move(taps), move(buffer));
    }

    FIRFilter(FIRFilter&& other)
        : m_coefficients(move(other.m_coefficients)), m_buffer(move(other.m_buffer))
    {
    }

    SampleType process(SampleType input)
    {
        memmove(&m_buffer[1], &m_buffer[0], (m_buffer.size() - 1) * sizeof(SampleType));
        m_buffer[0] = input;

        SampleType result {};
        for (size_t i = 0; i < m_coefficients.size(); i++) {
            result += m_buffer[i] * m_coefficients[i];
        }

        return result;
    }

private:
    FIRFilter(Vector<TapType>&& coefficients, Vector<SampleType>&& buffer)
        : m_coefficients(move(coefficients)), m_buffer(move(buffer))
    {
    }

    Vector<TapType> m_coefficients;
    Vector<SampleType> m_buffer;
};

}
