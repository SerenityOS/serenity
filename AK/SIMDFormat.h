/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace AK {

template<SIMD::SIMDVector V>
struct Formatter<V> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, V const& vector)
    {
        TRY(builder.put_literal("{"sv));
        for (u32 i = 0; i < SIMD::vector_length<V>; ++i) {
            if constexpr (IsFloatingPoint<SIMD::ElementOf<V>>)
                TRY(builder.put_f32_or_f64(vector[i]));
            else
                TRY(builder.put_u64(vector[i]));
            if (i != SIMD::vector_length<V> - 1)
                TRY(builder.put_literal(", "sv));
        }
        TRY(builder.put_literal("}"sv));

        return {};
    }
};

}
