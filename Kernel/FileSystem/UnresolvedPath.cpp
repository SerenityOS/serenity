/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/UnresolvedPath.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Custody>> UnresolvedPath::resolve() const
{
    if (KLexicalPath::is_absolute(m_path)) {
        return Process::current().vfs_root_context()->root_path().with([](auto& root_path) -> NonnullRefPtr<Custody> {
            return root_path.custody();
        });
    }
    if (m_parent)
        return *m_parent;
    return Process::current().custody_for_dirfd({}, m_dirfd);
}

}
