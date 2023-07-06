/*
 * Copyright (c) 2023, Sarsaparilla
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Noncopyable.h>
#include <AK/Span.h>
#include <AK/StdLibExtraDetails.h>

namespace DSP {

template<typename SampleType, typename TapType>
requires(IsTriviallyMoveConstructible<SampleType>)
class FIRFilter {
    AK_MAKE_NONCOPYABLE(FIRFilter);

public:
    static ErrorOr<FIRFilter> create(Span<TapType> coefficients_span)
    {
        auto coefficients = TRY(FixedArray<TapType>::create(coefficients_span.size()));
        coefficients_span.copy_to(coefficients.span());
        return create(move(coefficients));
    }

    static ErrorOr<FIRFilter> create(FixedArray<TapType>&& coefficients)
    {
        return FIRFilter(move(coefficients), TRY(FixedArray<SampleType>::create(coefficients.size())));
    }

    FIRFilter(FIRFilter&& other)
        : m_coefficients(move(other.m_coefficients))
        , m_buffer(move(other.m_buffer))
    {
    }

    SampleType process(SampleType input)
    {
        // This memmove is only defined behavior if the type is trivially moveable, therefore the requires() above.
        m_buffer.span().move_trimmed_to(m_buffer.span().slice(1));
        m_buffer.unchecked_at(0) = input;

        SampleType result {};
        // Indices are known to never be out of range, so unchecked indices allows optimizations to kick in better.
        for (size_t i = 0; i < m_coefficients.size(); i++)
            result += m_buffer.unchecked_at(i) * m_coefficients.unchecked_at(i);

        return result;
    }

private:
    FIRFilter(FixedArray<TapType>&& coefficients, FixedArray<SampleType>&& buffer)
        : m_coefficients(move(coefficients))
        , m_buffer(move(buffer))
    {
    }

    FixedArray<TapType> m_coefficients;
    FixedArray<SampleType> m_buffer;
};

}
