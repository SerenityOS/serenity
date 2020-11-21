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

#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$unveil(Userspace<const Syscall::SC_unveil_params*> user_params)
{
    Syscall::SC_unveil_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    if (!params.path.characters && !params.permissions.characters) {
        m_veil_state = VeilState::Locked;
        return 0;
    }

    if (m_veil_state == VeilState::Locked)
        return -EPERM;

    if (!params.path.characters || !params.permissions.characters)
        return -EINVAL;

    if (params.permissions.length > 5)
        return -EINVAL;

    auto path = get_syscall_path_argument(params.path);
    if (path.is_error())
        return path.error();

    if (path.value().is_empty() || path.value().characters()[0] != '/')
        return -EINVAL;

    auto permissions = copy_string_from_user(params.permissions);
    if (permissions.is_null())
        return -EFAULT;

    // Let's work out permissions first...
    unsigned new_permissions = 0;
    for (const char permission : permissions) {
        switch (permission) {
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
        case 'b':
            new_permissions |= UnveiledPath::Access::Browse;
            break;
        default:
            return -EINVAL;
        }
    }

    // Now, let's try and resolve the path and obtain custody of the inode on the disk, and if not, bail out with
    // the error from resolve_path_without_veil()
    // However, if the user specified unveil() with "c" permissions, we don't set errno if ENOENT is encountered,
    // because they most likely intend the program to create the file for them later on.
    // If this case is encountered, the parent node of the path is returned and the custody of that inode is used instead.
    RefPtr<Custody> parent_custody; // Parent inode in case of ENOENT
    String new_unveiled_path;
    auto custody_or_error = VFS::the().resolve_path_without_veil(path.value(), root_directory(), &parent_custody);
    if (!custody_or_error.is_error()) {
        new_unveiled_path = custody_or_error.value()->absolute_path();
    } else if (custody_or_error.error() == -ENOENT && parent_custody && (new_permissions & UnveiledPath::Access::CreateOrRemove)) {
        String basename = LexicalPath(path.value()).basename();
        new_unveiled_path = String::formatted("{}/{}", parent_custody->absolute_path(), basename);
    } else {
        // FIXME Should this be EINVAL?
        return custody_or_error.error();
    }

    for (auto& unveiled_path : m_unveiled_paths) {
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
