/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/CustodyBase.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<Custody>> CustodyBase::resolve() const
{
    if (m_base)
        return *m_base;
    if (KLexicalPath::is_absolute(m_path)) {
        return Process::current().vfs_root_context()->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
            return custody;
        });
    }
    return Process::current().custody_for_dirfd({}, m_dirfd);
}

}
