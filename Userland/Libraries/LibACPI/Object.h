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

class Object;

class String {
public:
    String(ByteBuffer buffer)
        : m_buffer(move(buffer))
    {
    }

    ByteBuffer const& buffer() const { return m_buffer; }

private:
    ByteBuffer m_buffer;
};

class Buffer {
public:
    Buffer(ByteBuffer buffer)
        : m_buffer(move(buffer))
    {
    }

    ByteBuffer const& buffer() const { return m_buffer; }

private:
    ByteBuffer m_buffer;
};

class Package {
public:
    Package(Vector<Object> package)
        : m_package(move(package))
    {
    }

    Vector<Object> const& package() const { return m_package; }

    ErrorOr<Package> clone() const;

private:
    Vector<Object> m_package;
};

template<typename T>
concept IsNodeData = OneOf<T, u64, String, Buffer, Package, NameString>;

class Object {
public:
    constexpr Object()
        : m_data(AK::Empty {})
    {
    }

    template<IsNodeData T>
    constexpr Object(T&& value)
        : m_data(forward<T>(value))
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
        return *TRY(as_pointer<T>());
    }

    template<IsNodeData T>
    constexpr ErrorOr<T const*> as_pointer() const
    {
        if (m_data.has<T>())
            return m_data.get_pointer<T>();

        // TODO: Implement casting according both to ACPI spec and whatever Windows does.
        // https://uefi.org/specs/ACPI/6.6/19_ASL_Reference.html#implicit-source-operand-conversion
        return Error::from_string_view_or_print_error_and_return_errno("Can not cast given type!"sv, EINVAL);
    }

    template<IsNodeData T>
    constexpr ErrorOr<T> release_as()
    {
        if (m_data.has<T>())
            return move(m_data.get<T>());

        // TODO: Implement casting according both to ACPI spec and whatever Windows does.
        // https://uefi.org/specs/ACPI/6.6/19_ASL_Reference.html#implicit-source-operand-conversion
        return Error::from_string_view_or_print_error_and_return_errno("Can not cast given type!"sv, EINVAL);
    }

    ErrorOr<Object> clone() const;

protected:
    Variant<AK::Empty, u64, String, Buffer, Package, NameString> m_data;
};

}

template<>
struct AK::Formatter<::ACPI::AML::Object> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, ::ACPI::AML::Object const& object)
    {
        if (object.is<u64>()) {
            auto value = MUST(object.as<u64>());
            return AK::Formatter<FormatString>::format(builder, "(Decimal: {}, Hexadecimal: {:X})"sv, value, value);
        }

        if (object.is<::ACPI::AML::String>())
            return AK::Formatter<FormatString>::format(builder, "'{}'"sv, StringView(MUST(object.as<::ACPI::AML::String>()).buffer()));

        if (object.is<::ACPI::AML::Buffer>())
            return AK::Formatter<FormatString>::format(builder, "Buffer ({:hex-dump})"sv, MUST(object.as<::ACPI::AML::Buffer>()).buffer().bytes());

        if (object.is<::ACPI::AML::Package>())
            return AK::Formatter<FormatString>::format(builder, "Package ({})"sv, MUST(object.as<::ACPI::AML::Package>()).package());

        if (object.is<::ACPI::AML::NameString>())
            return AK::Formatter<FormatString>::format(builder, "{}"sv, TRY(MUST(object.as<::ACPI::AML::NameString>()).to_string()));

        VERIFY_NOT_REACHED();
    }
};
