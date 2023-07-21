/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace JS::Bytecode {

class Register {
public:
    constexpr static u32 accumulator_index = 0;

    static constexpr Register accumulator()
    {
        return Register(accumulator_index);
    }

    constexpr static u32 saved_return_value_index = 1;

    static constexpr Register saved_return_value()
    {
        return Register(saved_return_value_index);
    }

    constexpr explicit Register(u32 index)
        : m_index(index)
    {
    }

    constexpr bool operator==(Register reg) const { return m_index == reg.index(); }

    constexpr u32 index() const { return m_index; }

private:
    u32 m_index;
};

}

template<>
struct AK::Formatter<JS::Bytecode::Register> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, JS::Bytecode::Register const& value)
    {
        if (value.index() == JS::Bytecode::Register::accumulator_index)
            return builder.put_string("acc"sv);
        return AK::Formatter<FormatString>::format(builder, "${}"sv, value.index());
    }
};
