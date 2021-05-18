/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace AK {

template<auto TSize>
struct FixedString {
public:
    static constexpr auto Size = TSize;

    constexpr FixedString() = default;
    constexpr FixedString(const char* str)
    {
        for (auto i = 0u; i < Size + 1; ++i) {
            m_data[i] = str[i];
        }
    }

    constexpr operator StringView() const { return { m_data, Size }; }

    template<auto TOtherSize>
    constexpr auto operator+(const FixedString<TOtherSize>& other) -> FixedString<TSize + TOtherSize>
    {
        constexpr auto new_size = TSize + TOtherSize;
        FixedString<new_size> result {};
        for (auto i = 0u; i < TSize; ++i) {
            result.m_data[i] = m_data[i];
        }
        for (auto i = 0u; i < TOtherSize; ++i) {
            result.m_data[i + TSize] = other.m_data[i + TSize];
        }
        return result;
    }

    char m_data[Size + 1] {};
};

template<auto TSize>
FixedString(char const (&)[TSize]) -> FixedString<TSize - 1>;

template<FixedString TStr>
constexpr auto operator""_fs() { return TStr; }

}
