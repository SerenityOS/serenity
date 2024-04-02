/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Utils.h"
#include <AK/ByteString.h>
#include <AK/NonnullOwnPtr.h>

namespace Wayland {

enum class PrimitiveType {
    UnsignedInteger,
    Integer,
    Fixed,
};

class ArgumentType {
    enum class Enum {
        Primitive,
        Array,
        Enum,
        String,
        Object,
        NewId,
        FileDescriptor,
    };

public:
    ArgumentType(PrimitiveType type)
        : m_type(Enum::Primitive)
        , m_primitive(type)
        , m_nullable(false)
    {
        m_type = Enum::Primitive;
    }

    static NonnullOwnPtr<ArgumentType> create_enum(StringView enum_name, bool signed_integer)
    {
        auto* self = new ArgumentType(Enum::Enum);
        self->m_type_name = enum_name;
        self->m_primitive = signed_integer ? PrimitiveType::Integer : PrimitiveType::UnsignedInteger;
        return MUST(adopt_nonnull_own_or_enomem(self));
    }

    static NonnullOwnPtr<ArgumentType> create_new_id(Optional<ByteString> const& interface)
    {
        auto* self = new ArgumentType(Enum::NewId);
        if (interface.has_value()) {
            self->m_type_name = interface.value();
        }
        return MUST(adopt_nonnull_own_or_enomem(self));
    }

    static NonnullOwnPtr<ArgumentType> create_object(Optional<ByteString> const& interface)
    {
        auto* self = new ArgumentType(Enum::Object);
        if (interface.has_value()) {
            self->m_type_name = interface.value();
        }
        return MUST(adopt_nonnull_own_or_enomem(self));
    }

    static NonnullOwnPtr<ArgumentType> create_file_descriptor()
    {
        auto* self = new ArgumentType(Enum::FileDescriptor);
        return MUST(adopt_nonnull_own_or_enomem(self));
    }

    static NonnullOwnPtr<ArgumentType> create_string()
    {
        return MUST(adopt_nonnull_own_or_enomem(new ArgumentType(Enum::String)));
    }

    static NonnullOwnPtr<ArgumentType> create_array()
    {
        return MUST(adopt_nonnull_own_or_enomem(new ArgumentType(Enum::Array)));
    }

    bool nullable_type() const
    {
        return m_type == Enum::String || m_type == Enum::Object;
    }

    bool interface_type() const
    {
        return m_type == Enum::Object || m_type == Enum::NewId;
    }

    void set_nullable(bool value)
    {
        VERIFY(nullable_type());
        m_nullable = value;
    }

    bool nullable() const
    {
        VERIFY(nullable_type());
        return m_nullable;
    }

    bool is_primitive() const
    {
        return m_type == Enum::Primitive;
    }

    bool is_signed_integer() const
    {
        return m_primitive.has_value() && m_primitive.value() == PrimitiveType::Integer;
    }

    bool is_unsigned_integer() const
    {
        return m_primitive.has_value() && m_primitive.value() == PrimitiveType::UnsignedInteger;
    }

    bool is_new_id() const
    {
        return m_type == Enum::NewId;
    }

    bool is_enum() const
    {
        return m_type == Enum::Enum;
    }

    bool is_object() const
    {
        return m_type == Enum::Object;
    }

    bool has_type_name() const
    {
        return m_type_name.has_value();
    }

    ByteString& type_name()
    {
        return m_type_name.value();
    }

    bool can_reference()
    {
        return m_type == Enum::Array || m_type == Enum::Object || m_type == Enum::NewId || (m_type == Enum::String && !m_nullable);
    }

    ByteString get_binding_symbol()
    {
        switch (m_type) {

        case Enum::Primitive:
            switch (m_primitive.value()) {
            case PrimitiveType::UnsignedInteger:
                return "uint32_t"sv;
            case PrimitiveType::Integer:
                return "int32_t"sv;
            case PrimitiveType::Fixed:
                return "FixedFloat"sv;
            }
            VERIFY_NOT_REACHED();

        case Enum::Array:
            return "ByteBuffer"sv;

        case Enum::Enum:
            // The interface that contains this enum can be inferred implcitly
            // (the parent interface) or referenced directly (seperation by '.' in m_type_name);
            // We don't have that information here yet, so handle it in the CodeGenerator.

            VERIFY_NOT_REACHED();

        case Enum::String:
            if (m_nullable) {
                return "Optional<ByteString>"sv;
            }
            return "ByteString"sv;

        case Enum::Object:
        case Enum::NewId: {
            if (m_type_name.has_value()) {
                auto code_name = to_code_name(m_type_name.value());

                return code_name.to_byte_string();
            }

            // should only really happen on registry.bind, hopefully
            return "Object"sv;
        }

        case Enum::FileDescriptor:
            return "int"sv;
        }

        VERIFY_NOT_REACHED();
    }

    ByteString get_resolved_argument_caster()
    {
        ByteString value;

        switch (m_type) {

        case Enum::Primitive:
            switch (m_primitive.value()) {
            case PrimitiveType::UnsignedInteger:
                value = "as_unsigned";
                break;
            case PrimitiveType::Integer:
                value = "as_signed";
                break;
            case PrimitiveType::Fixed:
                value = "as_fixed";
                break;
            }
            break;
        case Enum::Array:
            value = "as_buffer";
            break;
        case Enum::Enum:
            switch (m_primitive.value()) {
            case PrimitiveType::UnsignedInteger:
                value = "as_unsigned";
                break;
            case PrimitiveType::Integer:
                value = "as_signed";
                break;
            default:
                VERIFY_NOT_REACHED();
            };
            break;
        case Enum::String:
            if (m_nullable) {
                value = "as_opt_string";
            } else {
                value = "as_string";
            }
            break;
        case Enum::Object:
        case Enum::NewId:
            if (m_nullable) {
                value = ByteString::formatted("as_opt_object<{}>", get_binding_symbol());
            } else {
                value = ByteString::formatted("as_object<{}>", get_binding_symbol());
            }
            break;
        case Enum::FileDescriptor:
            value = "as_fd";
            break;
        }
        return value;
    }

    ByteString get_wire_argument_type()
    {
        ByteString value;

        switch (m_type) {
        case Enum::Primitive:
            switch (m_primitive.value()) {
            case PrimitiveType::UnsignedInteger:
                value = "UnsignedInteger";
                break;
            case PrimitiveType::Integer:
                value = "Integer";
                break;
            case PrimitiveType::Fixed:
                value = "FixedFloat";
                break;
            }
            break;
        case Enum::Array:
            value = "Array";
            break;
        case Enum::Enum:
            switch (m_primitive.value()) {
            case PrimitiveType::UnsignedInteger:
                value = "UnsignedInteger";
                break;
            case PrimitiveType::Integer:
                value = "Integer";
                break;
            default:
                VERIFY_NOT_REACHED();
            };
            break;
        case Enum::String:
            value = "String";
            break;
        case Enum::Object:
            value = "Object";
            break;
        case Enum::NewId:
            value = "NewId";
            break;
        case Enum::FileDescriptor:
            value = "FileDescriptor";
            break;
        }
        return value;
    }

    Enum type() const
    {
        return m_type;
    }

private:
    ArgumentType(Enum type)
        : m_type(type)
    {
    }

    Enum m_type;
    Optional<PrimitiveType> m_primitive;
    Optional<ByteString> m_type_name;

    bool m_nullable;
};

};
