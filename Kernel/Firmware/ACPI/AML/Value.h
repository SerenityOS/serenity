#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <Kernel/Library/KString.h>

namespace Kernel::ACPI::AML {

struct Value {
    struct Package {
        Vector<Value> elements;
    };

    constexpr explicit Value()
        : m_value(AK::Empty {})
    {
    }

    template<OneOf<u64, Package> T>
    constexpr explicit Value(T value)
        : m_value(move(value))
    {
    }

    template<OneOf<u64, Package> T>
    Optional<T const&> to() const
    {
        if (!m_value.has<T>())
            return {};

        return m_value.get<T>();
    }

private:
    Variant<AK::Empty, u64, Package> m_value;
};

}
