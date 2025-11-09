/*
 * Copyright (c) 2025, Kusekushi <0kusekushi0@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

class FileDescriptionTable : public RefCounted<FileDescriptionTable> {
public:
    FileDescriptionTable() = default;

    template<typename F>
    auto with_exclusive(F&& f) { return m_fds.with_exclusive(forward<F>(f)); }

    template<typename F>
    auto with_shared(F&& f) const { return m_fds.with_shared(forward<F>(f)); }

    ErrorOr<void> try_clone_from(MutexProtected<Process::OpenFileDescriptions> const& other)
    {
        return other.with_shared([&](auto& parent_fds) -> ErrorOr<void> {
            return m_fds.with_exclusive([&](auto& my_fds) -> ErrorOr<void> {
                return my_fds.try_clone(parent_fds);
            });
        });
    }

private:
    MutexProtected<Process::OpenFileDescriptions> m_fds;
};

}
