/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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
#include <AK/StdLibExtras.h>
#include <unistd.h>

namespace IPC {

class File {
    AK_MAKE_NONCOPYABLE(File);

public:
    // Must have a default constructor, because LibIPC
    // default-constructs arguments prior to decoding them.
    File() { }

    // Intentionally not `explicit`.
    File(int fd)
        : m_fd(fd)
    {
    }

    // Tagged constructor for fd's that should be closed on destruction unless take_fd() is called.
    enum Tag {
        ConstructWithReceivedFileDescriptor = 1,
    };
    File(int fd, Tag)
        : m_fd(fd)
        , m_close_on_destruction(true)
    {
    }

    File(File&& other)
        : m_fd(exchange(other.m_fd, -1))
        , m_close_on_destruction(exchange(other.m_close_on_destruction, false))
    {
    }

    File& operator=(File&& other)
    {
        if (this != &other) {
            m_fd = exchange(other.m_fd, -1);
            m_close_on_destruction = exchange(other.m_close_on_destruction, false);
        }
        return *this;
    }

    ~File()
    {
        if (m_close_on_destruction && m_fd != -1)
            close(m_fd);
    }

    int fd() const { return m_fd; }

    // NOTE: This is 'const' since generated IPC messages expose all parameters by const reference.
    [[nodiscard]] int take_fd() const
    {
        return exchange(m_fd, -1);
    }

private:
    mutable int m_fd { -1 };
    bool m_close_on_destruction { false };
};

}
