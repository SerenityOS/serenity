/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <LibIPC/Forward.h>

namespace Core {

class AnonymousBufferImpl final : public RefCounted<AnonymousBufferImpl> {
public:
    static RefPtr<AnonymousBufferImpl> create(int fd, size_t);
    ~AnonymousBufferImpl();

    int fd() const { return m_fd; }
    size_t size() const { return m_size; }
    void* data() { return m_data; }
    const void* data() const { return m_data; }

private:
    AnonymousBufferImpl(int fd, size_t, void*);

    int m_fd { -1 };
    size_t m_size { 0 };
    void* m_data { nullptr };
};

class AnonymousBuffer {
public:
    static AnonymousBuffer create_with_size(size_t);
    static AnonymousBuffer create_from_anon_fd(int fd, size_t);

    AnonymousBuffer() { }
    ~AnonymousBuffer();

    bool is_valid() const { return m_impl; }

    int fd() const { return m_impl ? m_impl->fd() : -1; }
    size_t size() const { return m_impl ? m_impl->size() : 0; }

    template<typename T>
    T* data()
    {
        static_assert(IsVoid<T> || IsTrivial<T>);
        if (!m_impl)
            return nullptr;
        return (T*)m_impl->data();
    }

    template<typename T>
    const T* data() const
    {
        static_assert(IsVoid<T> || IsTrivial<T>);
        if (!m_impl)
            return nullptr;
        return (const T*)m_impl->data();
    }

private:
    explicit AnonymousBuffer(NonnullRefPtr<AnonymousBufferImpl>);

    RefPtr<AnonymousBufferImpl> m_impl;
};

}

namespace IPC {

bool encode(Encoder&, const Core::AnonymousBuffer&);
bool decode(Decoder&, Core::AnonymousBuffer&);

}
