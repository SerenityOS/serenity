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

#include "Assertions.h"
#include "StdLibExtras.h"

namespace AK {

template<class T>
constexpr auto call_will_be_destroyed_if_present(T* object) -> decltype(object->will_be_destroyed(), TrueType {})
{
    object->will_be_destroyed();
    return {};
}

constexpr auto call_will_be_destroyed_if_present(...) -> FalseType
{
    return {};
}

template<class T>
constexpr auto call_one_ref_left_if_present(T* object) -> decltype(object->one_ref_left(), TrueType {})
{
    object->one_ref_left();
    return {};
}

constexpr auto call_one_ref_left_if_present(...) -> FalseType
{
    return {};
}

class RefCountedBase {
public:
    void ref()
    {
        ASSERT(m_ref_count);
        ++m_ref_count;
    }

    int ref_count() const
    {
        return m_ref_count;
    }

protected:
    RefCountedBase() {}
    ~RefCountedBase()
    {
        ASSERT(!m_ref_count);
    }

    void deref_base()
    {
        ASSERT(m_ref_count);
        --m_ref_count;
    }

    int m_ref_count { 1 };
};

template<typename T>
class RefCounted : public RefCountedBase {
public:
    void unref()
    {
        deref_base();
        if (m_ref_count == 0) {
            call_will_be_destroyed_if_present(static_cast<T*>(this));
            delete static_cast<T*>(this);
        } else if (m_ref_count == 1) {
            call_one_ref_left_if_present(static_cast<T*>(this));
        }
    }
};

}

using AK::RefCounted;
