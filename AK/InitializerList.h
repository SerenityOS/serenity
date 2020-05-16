/*
 * Copyright (c) 2020, Andrew Kaster <andrewdkaster@gmail.com>
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

// std::initializer_list is a special type as far as the compiler is concerned.
// As far as why it needs implemented in the library... blame the committee

namespace std {

template<class E>
class initializer_list {
public:
    typedef E value_type;
    typedef const E& reference;
    typedef const E& const_reference;
    typedef size_t size_type;
    typedef const E* iterator;
    typedef const E* const_iterator;

    constexpr initializer_list() noexcept = default;
    constexpr size_t size() const noexcept { return m_size; };
    constexpr const E* begin() const noexcept { return m_begin; };
    constexpr const E* end() const noexcept { return m_begin + m_size; }

private:
    // Note: begin has to be first or the compiler gets very upset
    const E* m_begin = { nullptr };
    size_t m_size = { 0 };

    // The compiler is allowed to call this constructor
    constexpr initializer_list(const E* e, size_t s) noexcept
        : m_begin(e)
        , m_size(s)
    {
    }
};

template<class E>
constexpr const E* begin(initializer_list<E> il) noexcept
{
    return il.begin();
}

template<class E>
constexpr const E* end(initializer_list<E> il) noexcept
{
    return il.end();
}

} // end namespace std
