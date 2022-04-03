/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Iterator.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>

namespace AK {

namespace Detail {

template<typename T>
class Span {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(T* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }

    template<size_t size>
    ALWAYS_INLINE constexpr Span(T (&values)[size])
        : m_values(values)
        , m_size(size)
    {
    }

    template<size_t size>
    ALWAYS_INLINE constexpr Span(Array<T, size>& array)
        : m_values(array.data())
        , m_size(size)
    {
    }

    template<size_t size>
    requires(IsConst<T>)
        ALWAYS_INLINE constexpr Span(Array<T, size> const& array)
        : m_values(array.data())
        , m_size(size)
    {
    }

protected:
    T* m_values { nullptr };
    size_t m_size { 0 };
};

template<>
class Span<u8> {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(u8* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(void* values, size_t size)
        : m_values(reinterpret_cast<u8*>(values))
        , m_size(size)
    {
    }

protected:
    u8* m_values { nullptr };
    size_t m_size { 0 };
};

template<>
class Span<u8 const> {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(u8 const* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(void const* values, size_t size)
        : m_values(reinterpret_cast<u8 const*>(values))
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(char const* values, size_t size)
        : m_values(reinterpret_cast<u8 const*>(values))
        , m_size(size)
    {
    }

protected:
    u8 const* m_values { nullptr };
    size_t m_size { 0 };
};

}

template<typename T>
class Span : public Detail::Span<T> {
public:
    using Detail::Span<T>::Span;

    constexpr Span() = default;

    [[nodiscard]] ALWAYS_INLINE constexpr T const* data() const { return this->m_values; }
    [[nodiscard]] ALWAYS_INLINE constexpr T* data() { return this->m_values; }

    [[nodiscard]] ALWAYS_INLINE constexpr T const* offset_pointer(size_t offset) const { return this->m_values + offset; }
    [[nodiscard]] ALWAYS_INLINE constexpr T* offset_pointer(size_t offset) { return this->m_values + offset; }

    using ConstIterator = SimpleIterator<Span const, T const>;
    using Iterator = SimpleIterator<Span, T>;

    constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    constexpr Iterator begin() { return Iterator::begin(*this); }

    constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    constexpr Iterator end() { return Iterator::end(*this); }

    [[nodiscard]] ALWAYS_INLINE constexpr size_t size() const { return this->m_size; }
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_null() const { return this->m_values == nullptr; }
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_empty() const { return this->m_size == 0; }

    [[nodiscard]] ALWAYS_INLINE constexpr Span slice(size_t start, size_t length) const
    {
        VERIFY(start + length <= size());
        return { this->m_values + start, length };
    }
    [[nodiscard]] ALWAYS_INLINE constexpr Span slice(size_t start) const
    {
        VERIFY(start <= size());
        return { this->m_values + start, size() - start };
    }
    [[nodiscard]] ALWAYS_INLINE constexpr Span slice_from_end(size_t count) const
    {
        VERIFY(count <= size());
        return { this->m_values + size() - count, count };
    }

    [[nodiscard]] ALWAYS_INLINE constexpr Span trim(size_t length) const
    {
        return { this->m_values, min(size(), length) };
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T* offset(size_t start) const
    {
        VERIFY(start < this->m_size);
        return this->m_values + start;
    }

    ALWAYS_INLINE constexpr void overwrite(size_t offset, void const* data, size_t data_size)
    {
        // make sure we're not told to write past the end
        VERIFY(offset + data_size <= size());
        __builtin_memmove(this->data() + offset, data, data_size);
    }

    ALWAYS_INLINE constexpr size_t copy_to(Span<RemoveConst<T>> other) const
    {
        VERIFY(other.size() >= size());
        return TypedTransfer<RemoveConst<T>>::copy(other.data(), data(), size());
    }

    ALWAYS_INLINE constexpr size_t copy_trimmed_to(Span<RemoveConst<T>> other) const
    {
        auto const count = min(size(), other.size());
        return TypedTransfer<RemoveConst<T>>::copy(other.data(), data(), count);
    }

    ALWAYS_INLINE constexpr size_t fill(T const& value)
    {
        for (size_t idx = 0; idx < size(); ++idx)
            data()[idx] = value;

        return size();
    }

    [[nodiscard]] bool constexpr contains_slow(T const& value) const
    {
        for (size_t i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    [[nodiscard]] bool constexpr starts_with(Span<T const> other) const
    {
        if (size() < other.size())
            return false;

        return TypedTransfer<T>::compare(data(), other.data(), other.size());
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T const& at(size_t index) const
    {
        VERIFY(index < this->m_size);
        return this->m_values[index];
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T& at(size_t index)
    {
        VERIFY(index < this->m_size);
        return this->m_values[index];
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T const& last() const
    {
        return this->at(this->size() - 1);
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T& last()
    {
        return this->at(this->size() - 1);
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T const& operator[](size_t index) const
    {
        return at(index);
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T& operator[](size_t index)
    {
        return at(index);
    }

    constexpr bool operator==(Span const& other) const
    {
        if (size() != other.size())
            return false;

        return TypedTransfer<T>::compare(data(), other.data(), size());
    }

    ALWAYS_INLINE constexpr operator Span<T const>() const
    {
        return { data(), size() };
    }
};

template<typename T>
struct Traits<Span<T>> : public GenericTraits<Span<T>> {
    static unsigned hash(Span<T> const& span)
    {
        unsigned hash = 0;
        for (auto const& value : span) {
            auto value_hash = Traits<T>::hash(value);
            hash = pair_int_hash(hash, value_hash);
        }
        return hash;
    }
};

using ReadonlyBytes = Span<u8 const>;
using Bytes = Span<u8>;

}

using AK::Bytes;
using AK::ReadonlyBytes;
using AK::Span;
