/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>

namespace AK {

template<typename ChunkType, bool IsConst>
struct DisjointIterator {
    struct EndTag {
    };
    using ReferenceType = Conditional<IsConst, AddConst<Vector<ChunkType>>, Vector<ChunkType>>&;

    DisjointIterator(ReferenceType chunks)
        : m_chunks(chunks)
    {
    }

    DisjointIterator(ReferenceType chunks, EndTag)
        : m_chunk_index(chunks.size())
        , m_index_in_chunk(0)
        , m_chunks(chunks)
    {
    }

    DisjointIterator& operator++()
    {
        if (m_chunk_index >= m_chunks.size())
            return *this;

        auto& chunk = m_chunks[m_chunk_index];
        if (m_index_in_chunk + 1 >= chunk.size()) {
            ++m_chunk_index;
            m_index_in_chunk = 0;
        } else {
            ++m_index_in_chunk;
        }
        if (m_chunk_index < m_chunks.size()) {
            while (m_chunks[m_chunk_index].is_empty())
                ++m_chunk_index;
        }
        return *this;
    }

    bool operator==(DisjointIterator const& other) const
    {
        return &other.m_chunks == &m_chunks && other.m_index_in_chunk == m_index_in_chunk && other.m_chunk_index == m_chunk_index;
    }

    auto& operator*() requires(!IsConst) { return m_chunks[m_chunk_index][m_index_in_chunk]; }
    auto* operator->() requires(!IsConst) { return &m_chunks[m_chunk_index][m_index_in_chunk]; }
    auto const& operator*() const { return m_chunks[m_chunk_index][m_index_in_chunk]; }
    auto const* operator->() const { return &m_chunks[m_chunk_index][m_index_in_chunk]; }

private:
    size_t m_chunk_index { 0 };
    size_t m_index_in_chunk { 0 };
    ReferenceType m_chunks;
};

template<typename T>
class DisjointSpans {
public:
    DisjointSpans() = default;
    ~DisjointSpans() = default;
    DisjointSpans(DisjointSpans const&) = default;
    DisjointSpans(DisjointSpans&&) = default;

    explicit DisjointSpans(Vector<Span<T>> spans)
        : m_spans(move(spans))
    {
    }

    DisjointSpans& operator=(DisjointSpans&&) = default;
    DisjointSpans& operator=(DisjointSpans const&) = default;

    bool operator==(DisjointSpans const& other) const
    {
        if (other.size() != size())
            return false;

        auto it = begin();
        auto other_it = other.begin();
        for (; it != end(); ++it, ++other_it) {
            if (*it != *other_it)
                return false;
        }
        return true;
    }

    T& operator[](size_t index) { return at(index); }
    T const& operator[](size_t index) const { return at(index); }
    T const& at(size_t index) const { return const_cast<DisjointSpans&>(*this).at(index); }
    T& at(size_t index)
    {
        auto span_and_offset = span_around(index);
        VERIFY(span_and_offset.offset < span_and_offset.span.size());
        return span_and_offset.span.at(span_and_offset.offset);
    }

    size_t size() const
    {
        size_t size = 0;
        for (auto& span : m_spans)
            size += span.size();
        return size;
    }

    bool is_empty() const { return size() == 0; }

    DisjointSpans slice(size_t start, size_t length) const
    {
        DisjointSpans spans;
        for (auto& span : m_spans) {
            if (length == 0)
                break;
            if (start >= span.size()) {
                start -= span.size();
                continue;
            }

            auto sliced_length = min(length, span.size() - start);
            spans.m_spans.append(span.slice(start, sliced_length));
            start = 0;
            length -= sliced_length;
        }
        // Make sure that we weren't asked to make a slice larger than possible.
        VERIFY(length == 0);
        return spans;
    }
    DisjointSpans slice(size_t start) const { return slice(start, size() - start); }
    DisjointSpans slice_from_end(size_t length) const { return slice(size() - length, length); }

    DisjointIterator<Span<T>, false> begin() { return { m_spans }; }
    DisjointIterator<Span<T>, false> end() { return { m_spans, {} }; }
    DisjointIterator<Span<T>, true> begin() const { return { m_spans }; }
    DisjointIterator<Span<T>, true> end() const { return { m_spans, {} }; }

private:
    struct SpanAndOffset {
        Span<T>& span;
        size_t offset;
    };
    SpanAndOffset span_around(size_t index)
    {
        size_t offset = 0;
        for (auto& span : m_spans) {
            if (span.is_empty())
                continue;
            auto next_offset = span.size() + offset;
            if (next_offset <= index) {
                offset = next_offset;
                continue;
            }

            return { span, index - offset };
        }

        return { m_spans.last(), index - (offset - m_spans.last().size()) };
    }

    Vector<Span<T>> m_spans;
};

template<typename T, typename ChunkType = Vector<T>>
class DisjointChunks {
public:
    DisjointChunks() = default;
    ~DisjointChunks() = default;
    DisjointChunks(DisjointChunks const&) = default;
    DisjointChunks(DisjointChunks&&) = default;

    DisjointChunks& operator=(DisjointChunks&&) = default;
    DisjointChunks& operator=(DisjointChunks const&) = default;

    void append(ChunkType&& chunk) { m_chunks.append(chunk); }
    void extend(DisjointChunks&& chunks) { m_chunks.extend(move(chunks.m_chunks)); }
    void extend(DisjointChunks const& chunks) { m_chunks.extend(chunks.m_chunks); }

    ChunkType& first_chunk() { return m_chunks.first(); }
    ChunkType& last_chunk() { return m_chunks.last(); }
    ChunkType const& first_chunk() const { return m_chunks.first(); }
    ChunkType const& last_chunk() const { return m_chunks.last(); }

    void insert(size_t index, T value)
    {
        if (m_chunks.size() == 1)
            return m_chunks.first().insert(index, value);
        auto chunk_and_offset = chunk_around(index);
        chunk_and_offset.chunk.insert(chunk_and_offset.offset, move(value));
    }

    void clear() { m_chunks.clear(); }

    T& operator[](size_t index) { return at(index); }
    T const& operator[](size_t index) const { return at(index); }
    T const& at(size_t index) const { return const_cast<DisjointChunks&>(*this).at(index); }
    T& at(size_t index)
    {
        if (m_chunks.size() == 1)
            return m_chunks.first().at(index);
        auto chunk_and_offset = chunk_around(index);
        VERIFY(chunk_and_offset.offset < chunk_and_offset.chunk.size());
        return chunk_and_offset.chunk.at(chunk_and_offset.offset);
    }

    size_t size() const
    {
        size_t sum = 0;
        for (auto& chunk : m_chunks)
            sum += chunk.size();
        return sum;
    }

    bool is_empty() const { return size() == 0; }

    DisjointSpans<T> spans() const&
    {
        Vector<Span<T>> spans;
        spans.ensure_capacity(m_chunks.size());
        for (auto& chunk : m_chunks)
            spans.unchecked_append(const_cast<ChunkType&>(chunk).span());
        return DisjointSpans<T> { move(spans) };
    }

    bool operator==(DisjointChunks const& other) const
    {
        if (other.size() != size())
            return false;

        auto it = begin();
        auto other_it = other.begin();
        for (; it != end(); ++it, ++other_it) {
            if (*it != *other_it)
                return false;
        }
        return true;
    }

    DisjointChunks release_slice(size_t start, size_t length) & { return move(*this).slice(start, length); }
    DisjointChunks release_slice(size_t start) & { return move(*this).slice(start); }

    DisjointChunks slice(size_t start, size_t length) &&
    {
        DisjointChunks result;
        for (auto& chunk : m_chunks) {
            if (length == 0)
                break;
            if (start >= chunk.size()) {
                start -= chunk.size();
                continue;
            }

            auto sliced_length = min(length, chunk.size() - start);
            if (start == 0 && sliced_length == chunk.size()) {
                // Happy path! move the chunk itself.
                result.m_chunks.append(move(chunk));
            } else {
                // Shatter the chunk, we were asked for only a part of it :(
                auto wanted_slice = chunk.span().slice(start, sliced_length);

                ChunkType new_chunk;
                if constexpr (IsTriviallyConstructible<T>) {
                    new_chunk.resize(wanted_slice.size());
                    TypedTransfer<T>::move(new_chunk.data(), wanted_slice.data(), wanted_slice.size());
                } else {
                    new_chunk.ensure_capacity(wanted_slice.size());
                    for (auto& entry : wanted_slice)
                        new_chunk.unchecked_append(move(entry));
                }
                result.m_chunks.append(move(new_chunk));

                chunk.remove(start, sliced_length);
            }
            start = 0;
            length -= sliced_length;
        }

        m_chunks.remove_all_matching([](auto& chunk) { return chunk.is_empty(); });

        // Make sure that we weren't asked to make a slice larger than possible.
        VERIFY(length == 0);
        return result;
    }
    DisjointChunks slice(size_t start) && { return move(*this).slice(start, size() - start); }
    DisjointChunks slice_from_end(size_t length) && { return move(*this).slice(size() - length, length); }

    void flatten()
    {
        if (m_chunks.is_empty())
            return;

        auto size = this->size();
        auto& first_chunk = m_chunks.first();
        first_chunk.ensure_capacity(size);
        bool first = true;
        for (auto& chunk : m_chunks) {
            if (first) {
                first = false;
                continue;
            }

            first_chunk.extend(move(chunk));
        }
        m_chunks.remove(1, m_chunks.size() - 1);
    }

    DisjointIterator<ChunkType, false> begin() { return { m_chunks }; }
    DisjointIterator<ChunkType, false> end() { return { m_chunks, {} }; }
    DisjointIterator<ChunkType, true> begin() const { return { m_chunks }; }
    DisjointIterator<ChunkType, true> end() const { return { m_chunks, {} }; }

private:
    struct ChunkAndOffset {
        ChunkType& chunk;
        size_t offset;
    };
    ChunkAndOffset chunk_around(size_t index)
    {
        size_t offset = 0;
        for (auto& chunk : m_chunks) {
            if (chunk.is_empty())
                continue;
            auto next_offset = chunk.size() + offset;
            if (next_offset <= index) {
                offset = next_offset;
                continue;
            }

            return { chunk, index - offset };
        }

        return { m_chunks.last(), index - (offset - m_chunks.last().size()) };
    }

    Vector<ChunkType> m_chunks;
};

}

using AK::DisjointChunks;
