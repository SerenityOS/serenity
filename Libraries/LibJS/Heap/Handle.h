/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Badge.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibJS/Forward.h>

namespace JS {

class HandleImpl : public RefCounted<HandleImpl> {
    AK_MAKE_NONCOPYABLE(HandleImpl);
    AK_MAKE_NONMOVABLE(HandleImpl);

public:
    ~HandleImpl();

    Cell* cell() { return m_cell; }
    const Cell* cell() const { return m_cell; }

private:
    template<class T>
    friend class Handle;

    explicit HandleImpl(Cell*);
    Cell* m_cell { nullptr };
};

template<class T>
class Handle {
public:
    Handle() { }

    static Handle create(T* cell)
    {
        return Handle(adopt(*new HandleImpl(cell)));
    }

    T* cell() { return static_cast<T*>(m_impl->cell()); }
    const T* cell() const { return static_cast<const T*>(m_impl->cell()); }

private:
    explicit Handle(NonnullRefPtr<HandleImpl> impl)
        : m_impl(move(impl))
    {
    }

    RefPtr<HandleImpl> m_impl;
};

template<class T>
inline Handle<T> make_handle(T* cell)
{
    return Handle<T>::create(cell);
}

}
