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
    constexpr static u32 global_object_index = 1;

    static Register accumulator()
    {
        static Register accumulator(accumulator_index);
        return accumulator;
    }

    static Register global_object()
    {
        static Register global_object(global_object_index);
        return global_object;
    }

    explicit Register(u32 index)
        : m_index(index)
    {
    }

    u32 index() const { return m_index; }

private:
    u32 m_index;
};

}

template<>
struct AK::Formatter<JS::Bytecode::Register> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, JS::Bytecode::Register const& value)
    {
        if (value.index() == JS::Bytecode::Register::accumulator_index)
            return AK::Formatter<FormatString>::format(builder, "acc");
        return AK::Formatter<FormatString>::format(builder, "${}", value.index());
    }
};
