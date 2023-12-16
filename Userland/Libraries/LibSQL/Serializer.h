/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/ByteString.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Heap.h>

namespace SQL {

class Serializer {
public:
    Serializer() = default;

    Serializer(RefPtr<Heap> heap)
        : m_heap(heap)
    {
    }

    void read_storage(Block::Index block_index)
    {
        m_buffer = m_heap->read_storage(block_index).release_value_but_fixme_should_propagate_errors();
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
    T deserialize_block(Block::Index block_index, Args&&... args)
    {
        read_storage(block_index);
        return deserialize<T>(forward<Args>(args)...);
    }

    template<typename T>
    void deserialize_block_to(Block::Index block_index, T& t)
    {
        read_storage(block_index);
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

    void deserialize_to(ByteString& text);

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

    void serialize(ByteString const&);

    template<typename T>
    bool serialize_and_write(T const& t)
    {
        VERIFY(!m_heap.is_null());
        reset();
        serialize<T>(t);
        m_heap->write_storage(t.block_index(), m_buffer).release_value_but_fixme_should_propagate_errors();
        return true;
    }

    [[nodiscard]] size_t offset() const { return m_current_offset; }
    u32 request_new_block_index()
    {
        return m_heap->request_new_block_index();
    }

    bool has_block(u32 pointer) const
    {
        return m_heap->has_block(pointer);
    }

    Heap& heap()
    {
        return *m_heap;
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
        auto buffer_ptr = m_buffer.offset_pointer(m_current_offset);
        if constexpr (SQL_DEBUG)
            dump(buffer_ptr, sz, "<= (in)");
        m_current_offset += sz;
        return buffer_ptr;
    }

    static void dump(u8 const* ptr, size_t sz, ByteString const& prefix)
    {
        StringBuilder builder;
        builder.appendff("{0} {1:04x} | ", prefix, sz);
        Vector<ByteString> bytes;
        for (auto ix = 0u; ix < sz; ++ix)
            bytes.append(ByteString::formatted("{0:02x}", *(ptr + ix)));
        StringBuilder bytes_builder;
        bytes_builder.join(' ', bytes);
        builder.append(bytes_builder.to_byte_string());
        dbgln(builder.to_byte_string());
    }

    ByteBuffer m_buffer {};
    size_t m_current_offset { 0 };
    // FIXME: make this a NonnullRefPtr<Heap> so we can get rid of the null checks
    RefPtr<Heap> m_heap { nullptr };
};

}
