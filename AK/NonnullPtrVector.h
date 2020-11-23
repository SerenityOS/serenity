/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Vector.h>

namespace AK {

template<typename PtrType, int inline_capacity = 0>
class NonnullPtrVector : public Vector<PtrType, inline_capacity> {
    using T = typename PtrType::ElementType;
    using Base = Vector<PtrType, inline_capacity>;

public:
    NonnullPtrVector()
    {
    }

    NonnullPtrVector(Vector<PtrType>&& other)
        : Base(static_cast<Base&&>(other))
    {
    }
    NonnullPtrVector(const Vector<PtrType>& other)
        : Base(static_cast<const Base&>(other))
    {
    }

    using Base::size;

    using ConstIterator = SimpleIterator<const NonnullPtrVector, const T>;
    using Iterator = SimpleIterator<NonnullPtrVector, T>;

    ALWAYS_INLINE constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    ALWAYS_INLINE constexpr Iterator begin() { return Iterator::begin(*this); }

    ALWAYS_INLINE constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    ALWAYS_INLINE constexpr Iterator end() { return Iterator::end(*this); }

    ALWAYS_INLINE PtrType& ptr_at(int index) { return Base::at(index); }
    ALWAYS_INLINE const PtrType& ptr_at(int index) const { return Base::at(index); }

    ALWAYS_INLINE T& at(int index) { return *Base::at(index); }
    ALWAYS_INLINE const T& at(int index) const { return *Base::at(index); }
    ALWAYS_INLINE T& operator[](int index) { return at(index); }
    ALWAYS_INLINE const T& operator[](int index) const { return at(index); }
    ALWAYS_INLINE T& first() { return at(0); }
    ALWAYS_INLINE const T& first() const { return at(0); }
    ALWAYS_INLINE T& last() { return at(size() - 1); }
    ALWAYS_INLINE const T& last() const { return at(size() - 1); }

private:
    // NOTE: You can't use resize() on a NonnullFooPtrVector since making the vector
    //       bigger would require being able to default-construct NonnullFooPtrs.
    //       Instead, use shrink(new_size).
    void resize(int) = delete;
};

}
