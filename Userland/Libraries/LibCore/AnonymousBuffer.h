/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Noncopyable.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <YAK/Types.h>
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
