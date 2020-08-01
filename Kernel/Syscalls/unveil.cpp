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

#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$unveil(Userspace<const Syscall::SC_unveil_params*> user_params)
{
    Syscall::SC_unveil_params params;
    if (!validate_read_and_copy_typed(&params, user_params))
        return -EFAULT;

    if (!params.path.characters && !params.permissions.characters) {
        m_veil_state = VeilState::Locked;
        return 0;
    }

    if (m_veil_state == VeilState::Locked)
        return -EPERM;

    if (!params.path.characters || !params.permissions.characters)
        return -EINVAL;

    if (params.permissions.length > 4)
        return -EINVAL;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    if (path.value().is_empty() || path.value().characters()[0] != '/')
        return -EINVAL;

    auto custody_or_error = VFS::the().resolve_path_without_veil(path.value(), root_directory());
    if (custody_or_error.is_error())
        // FIXME Should this be EINVAL?
        return custody_or_error.error();

    auto& custody = custody_or_error.value();
    auto new_unveiled_path = custody->absolute_path();

    auto permissions = validate_and_copy_string_from_user(params.permissions);
    if (permissions.is_null())
        return -EFAULT;

    unsigned new_permissions = 0;
    for (size_t i = 0; i < permissions.length(); ++i) {
        switch (permissions[i]) {
        case 'r':
            new_permissions |= UnveiledPath::Access::Read;
            break;
        case 'w':
            new_permissions |= UnveiledPath::Access::Write;
            break;
        case 'x':
            new_permissions |= UnveiledPath::Access::Execute;
            break;
        case 'c':
            new_permissions |= UnveiledPath::Access::CreateOrRemove;
            break;
        default:
            return -EINVAL;
        }
    }

    for (size_t i = 0; i < m_unveiled_paths.size(); ++i) {
        auto& unveiled_path = m_unveiled_paths[i];
        if (unveiled_path.path == new_unveiled_path) {
            if (new_permissions & ~unveiled_path.permissions)
                return -EPERM;
            unveiled_path.permissions = new_permissions;
            return 0;
        }
    }

    m_unveiled_paths.append({ new_unveiled_path, new_permissions });
    ASSERT(m_veil_state != VeilState::Locked);
    m_veil_state = VeilState::Dropped;
    return 0;
}

}
