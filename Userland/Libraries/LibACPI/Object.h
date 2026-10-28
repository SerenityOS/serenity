/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibACPI/NameString.h>

namespace ACPI::AML {

class String {
public:
    String(ByteBuffer buffer)
        : m_buffer(move(buffer))
    {
    }

    ByteBuffer& buffer() { return m_buffer; }

private:
    ByteBuffer m_buffer;
};

template<typename T>
concept IsNodeData = OneOf<T, u64, String, NameString>;

class Object {
public:
    constexpr Object()
        : m_data(AK::Empty {})
    {
    }

    template<IsNodeData T>
    constexpr Object(T const& value)
        : m_data(value)
    {
    }

    template<IsNodeData T>
    constexpr bool is() const
    {
        return m_data.has<T>();
    }

    template<IsNodeData T>
    constexpr ErrorOr<T> as() const
    {
        if (m_data.has<T>())
            return m_data.get<T>();

        // TODO: Implement casting according both to ACPI spec and whatever Windows does.
        // https://uefi.org/specs/ACPI/6.6/19_ASL_Reference.html#implicit-source-operand-conversion
        return Error::from_string_view_or_print_error_and_return_errno("Can not cast given type!"sv, EINVAL);
    }

protected:
    Variant<AK::Empty, u64, String, NameString> m_data;
};

}
