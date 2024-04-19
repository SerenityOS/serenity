/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/StdLibExtras.h>
#include <LibCore/File.h>
#include <LibCore/System.h>

namespace IPC {

class File {
    AK_MAKE_NONCOPYABLE(File);

public:
    File() = default;

    static File adopt_file(NonnullOwnPtr<Core::File> file)
    {
        return File(file->leak_fd(Badge<File> {}));
    }

    static File adopt_fd(int fd)
    {
        return File(fd);
    }

    static ErrorOr<File> clone_fd(int fd)
    {
        int new_fd = TRY(Core::System::dup(fd));
        return File(new_fd);
    }

    File(File&& other)
        : m_fd(exchange(other.m_fd, -1))
    {
    }

    File& operator=(File&& other)
    {
        if (this != &other) {
            m_fd = exchange(other.m_fd, -1);
        }
        return *this;
    }

    ~File()
    {
        if (m_fd != -1)
            (void)Core::System::close(m_fd);
    }

    int fd() const { return m_fd; }

    // NOTE: This is 'const' since generated IPC messages expose all parameters by const reference.
    [[nodiscard]] int take_fd() const
    {
        return exchange(m_fd, -1);
    }

private:
    explicit File(int fd)
        : m_fd(fd)
    {
    }

    mutable int m_fd { -1 };
};

}
