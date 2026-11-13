/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibACPI/NameString.h>
#include <LibACPI/Node.h>

namespace ACPI::AML {

class Object;

class String {
    AK_MAKE_NONCOPYABLE(String);
    AK_MAKE_DEFAULT_MOVABLE(String);

public:
    String(ByteBuffer buffer)
        : m_buffer(move(buffer))
    {
    }

    ByteBuffer const& buffer() const { return m_buffer; }

    ErrorOr<String> clone() const;

private:
    ByteBuffer m_buffer;
};

class Buffer {
    AK_MAKE_NONCOPYABLE(Buffer);
    AK_MAKE_DEFAULT_MOVABLE(Buffer);

public:
    Buffer(ByteBuffer buffer)
        : m_buffer(move(buffer))
    {
    }

    ByteBuffer const& buffer() const { return m_buffer; }

    ErrorOr<Buffer> clone() const;

private:
    ByteBuffer m_buffer;
};

class Package {
    AK_MAKE_NONCOPYABLE(Package);
    AK_MAKE_DEFAULT_MOVABLE(Package);

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

class ArgumentReference {
public:
    ArgumentReference(size_t index)
        : m_index(index)
    {
    }

    size_t index() const { return m_index; }

private:
    size_t m_index { 0 };
};

class LocalReference {
public:
    LocalReference(size_t index)
        : m_index(index)
    {
    }

    size_t index() const { return m_index; }

private:
    size_t m_index { 0 };
};

class NodeReference {
public:
    NodeReference(RefPtr<Node> node)
        : m_node(move(node))
    {
    }

    RefPtr<Node> const& node() const { return m_node; }

private:
    RefPtr<Node> m_node;
};

template<typename T>
concept IsNodeData = OneOf<T, u64, String, Buffer, Package, NameString, ArgumentReference, LocalReference, NodeReference>;

class Object {
    AK_MAKE_NONCOPYABLE(Object);
    AK_MAKE_DEFAULT_MOVABLE(Object);

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

    constexpr bool is_empty() const
    {
        return m_data.has<AK::Empty>();
    }

    template<IsNodeData T>
    constexpr bool is() const
    {
        return m_data.has<T>();
    }

    constexpr bool is_reference() const
    {
        return is<ArgumentReference>() || is<LocalReference>() || is<NodeReference>();
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
    Variant<AK::Empty, u64, String, Buffer, Package, NameString, ArgumentReference, LocalReference, NodeReference> m_data;
};

}

template<>
struct AK::Formatter<::ACPI::AML::Object> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, ::ACPI::AML::Object const& object)
    {
        if (object.is_empty())
            return AK::Formatter<FormatString>::format(builder, "Empty"sv);

        if (object.is<u64>()) {
            auto value = MUST(object.as<u64>());
            return AK::Formatter<FormatString>::format(builder, "(Decimal: {}, Hexadecimal: {:X})"sv, value, value);
        }

        if (object.is<::ACPI::AML::String>())
            return AK::Formatter<FormatString>::format(builder, "'{}'"sv, StringView(MUST(object.as_pointer<::ACPI::AML::String>())->buffer()));

        if (object.is<::ACPI::AML::Buffer>())
            return AK::Formatter<FormatString>::format(builder, "Buffer ({:hex-dump})"sv, MUST(object.as_pointer<::ACPI::AML::Buffer>())->buffer().bytes());

        if (object.is<::ACPI::AML::Package>())
            return AK::Formatter<FormatString>::format(builder, "Package ({})"sv, MUST(object.as_pointer<::ACPI::AML::Package>())->package());

        if (object.is<::ACPI::AML::NameString>())
            return AK::Formatter<FormatString>::format(builder, "{}"sv, TRY(MUST(object.as_pointer<::ACPI::AML::NameString>())->to_string()));

        if (object.is<::ACPI::AML::ArgumentReference>())
            return AK::Formatter<FormatString>::format(builder, "Argument {}"sv, MUST(object.as_pointer<::ACPI::AML::ArgumentReference>())->index());

        if (object.is<::ACPI::AML::LocalReference>())
            return AK::Formatter<FormatString>::format(builder, "Local {}"sv, MUST(object.as_pointer<::ACPI::AML::LocalReference>())->index());

        if (object.is<::ACPI::AML::NodeReference>())
            return AK::Formatter<FormatString>::format(builder, "{} Reference"sv, MUST(object.as_pointer<::ACPI::AML::NodeReference>())->node()->kind());

        VERIFY_NOT_REACHED();
    }
};
