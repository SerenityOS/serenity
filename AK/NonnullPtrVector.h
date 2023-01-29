/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

namespace AK {

template<typename PtrType, size_t inline_capacity = 0>
class NonnullPtrVector : public Vector<PtrType, inline_capacity> {
    using T = typename PtrType::ElementType;
    using Base = Vector<PtrType, inline_capacity>;

public:
    NonnullPtrVector() = default;

    NonnullPtrVector(Vector<PtrType>&& other)
        : Base(static_cast<Base&&>(other))
    {
    }
    NonnullPtrVector(Vector<PtrType> const& other)
        : Base(static_cast<Base const&>(other))
    {
    }
    NonnullPtrVector(std::initializer_list<PtrType> list)
        : Base(list)
    {
    }

    using Base::size;

    using ConstIterator = SimpleIterator<NonnullPtrVector const, T const>;
    using Iterator = SimpleIterator<NonnullPtrVector, T>;
    using ReverseIterator = SimpleReverseIterator<NonnullPtrVector, T>;
    using ReverseConstIterator = SimpleReverseIterator<NonnullPtrVector const, T const>;

    ALWAYS_INLINE constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    ALWAYS_INLINE constexpr Iterator begin() { return Iterator::begin(*this); }
    ALWAYS_INLINE constexpr ReverseIterator rbegin() { return ReverseIterator::rbegin(*this); }
    ALWAYS_INLINE constexpr ReverseConstIterator rbegin() const { return ReverseConstIterator::rbegin(*this); }

    ALWAYS_INLINE constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    ALWAYS_INLINE constexpr Iterator end() { return Iterator::end(*this); }
    ALWAYS_INLINE constexpr ReverseIterator rend() { return ReverseIterator::rend(*this); }
    ALWAYS_INLINE constexpr ReverseConstIterator rend() const { return ReverseConstIterator::rend(*this); }

    ALWAYS_INLINE constexpr auto in_reverse() { return ReverseWrapper::in_reverse(*this); }
    ALWAYS_INLINE constexpr auto in_reverse() const { return ReverseWrapper::in_reverse(*this); }

    Optional<size_t> find_first_index(T const& value) const
    {
        if (auto const index = AK::find_index(begin(), end(), value);
            index < size()) {
            return index;
        }
        return {};
    }

    ALWAYS_INLINE PtrType& ptr_at(size_t index) { return Base::at(index); }
    ALWAYS_INLINE PtrType const& ptr_at(size_t index) const { return Base::at(index); }

    ALWAYS_INLINE T& at(size_t index) { return *Base::at(index); }
    ALWAYS_INLINE T const& at(size_t index) const { return *Base::at(index); }
    ALWAYS_INLINE T& operator[](size_t index) { return at(index); }
    ALWAYS_INLINE T const& operator[](size_t index) const { return at(index); }
    ALWAYS_INLINE T& first() { return at(0); }
    ALWAYS_INLINE T const& first() const { return at(0); }
    ALWAYS_INLINE T& last() { return at(size() - 1); }
    ALWAYS_INLINE T const& last() const { return at(size() - 1); }

private:
    // NOTE: You can't use resize() on a NonnullFooPtrVector since making the vector
    //       bigger would require being able to default-construct NonnullFooPtrs.
    //       Instead, use shrink(new_size).
    void resize(size_t) = delete;
};

}
