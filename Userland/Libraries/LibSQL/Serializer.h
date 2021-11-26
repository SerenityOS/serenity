/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>
#include <string.h>

namespace SQL {

class Serializer {
public:
    Serializer() = default;

    Serializer(RefPtr<Heap> heap)
        : m_heap(heap)
    {
    }

    void get_block(u32 pointer)
    {
        VERIFY(m_heap.ptr() != nullptr);
        auto buffer_or_error = m_heap->read_block(pointer);
        if (buffer_or_error.is_error())
            VERIFY_NOT_REACHED();
        m_buffer = buffer_or_error.value();
        m_current_offset = 0;
    }

    void reset()
    {
        m_buffer.clear();
        m_current_offset = 0;
    }

    void rewind()
    {
        m_current_offset = 0;
    }

    template<typename T, typename... Args>
    T deserialize_block(u32 pointer, Args&&... args)
    {
        get_block(pointer);
        return deserialize<T>(forward<Args>(args)...);
    }

    template<typename T>
    void deserialize_block_to(u32 pointer, T& t)
    {
        get_block(pointer);
        return deserialize_to<T>(t);
    }

    template<typename T>
    void deserialize_to(T& t)
    {
        if constexpr (IsArithmetic<T>)
            memcpy(&t, read(sizeof(T)), sizeof(T));
        else
            t.deserialize(*this);
    }

    void deserialize_to(String& text);

    template<typename T, typename... Args>
    NonnullOwnPtr<T> make_and_deserialize(Args&&... args)
    {
        auto ptr = make<T>(forward<Args>(args)...);
        ptr->deserialize(*this);
        return ptr;
    }

    template<typename T, typename... Args>
    NonnullRefPtr<T> adopt_and_deserialize(Args&&... args)
    {
        auto ptr = adopt_ref(*new T(forward<Args>(args)...));
        ptr->deserialize(*this);
        return ptr;
    }

    template<typename T, typename... Args>
    T deserialize(Args&&... args)
    {
        T t(forward<Args>(args)...);
        deserialize_to(t);
        return t;
    }

    template<typename T>
    void serialize(T const& t)
    {
        if constexpr (IsArithmetic<T>)
            write((u8 const*)(&t), sizeof(T));
        else
            t.serialize(*this);
    }

    void serialize(String const&);

    template<typename T>
    bool serialize_and_write(T const& t, u32 pointer)
    {
        VERIFY(m_heap.ptr() != nullptr);
        reset();
        serialize<T>(t);
        m_heap->add_to_wal(pointer, m_buffer);
        return true;
    }

    [[nodiscard]] size_t offset() const { return m_current_offset; }
    u32 new_record_pointer()
    {
        VERIFY(m_heap.ptr() != nullptr);
        return m_heap->new_record_pointer();
    }

    bool has_block(u32 pointer) const
    {
        VERIFY(m_heap.ptr() != nullptr);
        return pointer < m_heap->size();
    }

    Heap& heap()
    {
        VERIFY(m_heap.ptr() != nullptr);
        return *(m_heap.ptr());
    }

private:
    void write(u8 const* ptr, size_t sz)
    {
        if constexpr (SQL_DEBUG)
            dump(ptr, sz, "(out) =>");
        m_buffer.append(ptr, sz);
        m_current_offset += sz;
    }

    u8 const* read(size_t sz)
    {
        auto buffer_ptr = m_buffer.offset_pointer((int)m_current_offset);
        if constexpr (SQL_DEBUG)
            dump(buffer_ptr, sz, "<= (in)");
        m_current_offset += sz;
        return buffer_ptr;
    }

    static void dump(u8 const* ptr, size_t sz, String const& prefix)
    {
        StringBuilder builder;
        builder.appendff("{0} {1:04x} | ", prefix, sz);
        Vector<String> bytes;
        for (auto ix = 0u; ix < sz; ++ix) {
            bytes.append(String::formatted("{0:02x}", *(ptr + ix)));
        }
        StringBuilder bytes_builder;
        bytes_builder.join(" ", bytes);
        builder.append(bytes_builder.to_string());
        dbgln(builder.to_string());
    }

    ByteBuffer m_buffer {};
    size_t m_current_offset { 0 };
    RefPtr<Heap> m_heap { nullptr };
};

}
