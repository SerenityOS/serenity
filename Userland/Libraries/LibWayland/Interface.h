/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>

namespace Wayland {

class Connection;
class ResolvedArgument;
class Object;

// https://gitlab.freedesktop.org/wayland/wayland/-/blob/main/src/wayland-private.h#L54
struct WireArgumentType {
    enum {
        Integer,
        UnsignedInteger,
        FixedFloat,
        String,
        Object,
        NewId,
        Array,
        FileDescriptor,
    } kind;
    bool nullable;
};

struct Argument {
    char const* name;
    WireArgumentType const type;
};

class FixedFloat {
    friend Connection;
    friend class ResolvedArgument;

public:
    FixedFloat(FixedFloat const& other)
        : m_raw(other.m_raw)
    {
    }

protected:
    FixedFloat(int32_t raw)
        : m_raw(raw)
    {
    }
    int32_t get_raw() const
    {
        return m_raw;
    }

private:
    int32_t m_raw;
};

class ResolvedArgument {
    friend Connection;

public:
    struct Argument* m_argument;

    bool is_string() const
    {
        return m_argument->type.kind == WireArgumentType::String && !m_argument->type.nullable;
    }

    bool is_opt_string() const
    {
        return m_argument->type.kind == WireArgumentType::String && m_argument->type.nullable;
    }

    Optional<ByteString> as_opt_string()
    {
        VERIFY(is_opt_string());
        if (!m_string.is_empty()) {
            return m_string;
        }

        return {};
    }

    ByteString& as_string()
    {
        VERIFY(is_string());
        return m_string;
    }

    bool is_object() const
    {
        return m_argument->type.kind == WireArgumentType::Object;
    }

    template<class A>
    RefPtr<A> as_opt_object()
    {
        VERIFY(is_object());
        VERIFY(m_argument->type.nullable);

        auto* obj = static_cast<A*>(m_object.ptr());
        auto ref = adopt_ref_if_nonnull(obj);
        return ref;
    }

    template<class A>
    NonnullRefPtr<A> as_object()
    {
        VERIFY(is_object());
        VERIFY(!m_argument->type.nullable);
        VERIFY(!m_object.is_null());

        auto* obj = static_cast<A*>(m_object.ptr());
        auto ref = adopt_ref(*obj);
        return ref;
    }

    ByteBuffer& as_buffer()
    {
        VERIFY(m_argument->type.kind == WireArgumentType::Array);
        return m_buffer;
    }

    int32_t as_signed() const
    {
        VERIFY(m_argument->type.kind == WireArgumentType::Integer);
        return m_data;
    }

    uint32_t as_unsigned() const
    {
        VERIFY(m_argument->type.kind == WireArgumentType::UnsignedInteger);
        return m_data;
    }

    bool is_fd() const
    {
        return m_argument->type.kind == WireArgumentType::FileDescriptor;
    }

    bool is_fd_resolved() const
    {
        VERIFY(is_fd());
        return m_data > 0;
    }

    int as_fd() const
    {
        VERIFY(is_fd());
        return m_data;
    }

    void push_fd(int fd)
    {
        VERIFY(is_fd());
        m_data = fd;
    }

    bool is_array() const
    {
        return m_argument->type.kind == WireArgumentType::Array;
    }

    FixedFloat as_fixed() const
    {
        VERIFY(m_argument->type.kind == WireArgumentType::FixedFloat);
        return FixedFloat(m_data);
    }

    uint32_t as_new_id() const
    {
        VERIFY(m_argument->type.kind == WireArgumentType::NewId);

        return m_data;
    }

    ResolvedArgument(struct Argument* arg, int32_t data)
        : m_argument(arg)
        , m_data(data)
    {
    }

    ResolvedArgument(struct Argument* arg, ByteString string)
        : m_argument(arg)
        , m_string(move(string))
    {
        VERIFY(is_string());
    }

    ResolvedArgument(struct Argument* arg, Optional<ByteString> string)
        : m_argument(arg)
        , m_opt_string(move(string))
    {
        VERIFY(is_opt_string());
    }

    ResolvedArgument(struct Argument* arg, ByteBuffer const& buffer)
        : m_argument(arg)
        , m_buffer(buffer)
    {
        VERIFY(is_array());
    }

    ResolvedArgument(struct Argument* arg, RefPtr<Object> obj)
        : m_argument(arg)
        , m_object(move(obj))
    {
        VERIFY(is_object());
    }

    ByteBuffer* message_bytes() const
    {
        ByteBuffer* out = new ByteBuffer();

        if (is_fd()) {
        } else if (is_array()) {
            uint32_t length = m_buffer.bytes().size();
            out->append(&length, sizeof(uint32_t));
            out->append(m_buffer.bytes());
        } else if (is_string()) {
            uint32_t length = m_string.bytes().size() * sizeof(char) + sizeof(char);
            out->append(&length, sizeof(uint32_t));
            out->append(m_string.bytes());
        } else {
            out->append(&m_data, sizeof(uint32_t));
        }

        auto padding = out->size() % sizeof(uint32_t);
        if (padding != 0) {
            u8 zero = 0;
            out->append(&zero, sizeof(uint32_t) - padding);
        }

        return out;
    }

private:
    int32_t m_data { 0 };
    Optional<ByteString> m_opt_string;
    ByteString m_string;
    ByteBuffer m_buffer;
    RefPtr<Object> m_object;
};

struct Method {
    char const* name;
    uint8_t amount_args;
    struct Argument** arg;
    void (*handler)(Object& object, Vector<NonnullOwnPtr<ResolvedArgument>>&);
};

struct Interface {
    char const* name;
    struct Method** requests;
    struct Method** events;
};

}
