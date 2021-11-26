/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    File() = default;

    // Intentionally not `explicit`.
    File(int fd)
        : m_fd(fd)
    {
    }

    // Tagged constructor for fd's that should be closed on destruction unless take_fd() is called.
    // Note that the tags are the same, this is intentional to allow expressive invocation.
    enum Tag {
        ConstructWithReceivedFileDescriptor = 1,
        CloseAfterSending = 1,
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
