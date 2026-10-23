/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "NameString.h"
#include <AK/ByteBuffer.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>

namespace ACPI::AML {

template<typename T>
concept IsNodeData = OneOf<T, u64, NameString>;

class NodeData {
public:
    constexpr NodeData()
        : m_data(AK::Empty {})
    {
    }

    template<IsNodeData T>
    constexpr NodeData(T const& value)
        : m_data(value)
    {
    }

    template<IsNodeData T>
    constexpr bool is()
    {
        return m_data.has<T>();
    }

    template<IsNodeData T>
    constexpr ErrorOr<T> as()
    {
        if (m_data.has<T>())
            return m_data.get<T>();

        return Error::from_string_view_or_print_error_and_return_errno("Can not cast given type!"sv, EINVAL);
    }

protected:
    Variant<AK::Empty, u64, NameString> m_data;
};

}
