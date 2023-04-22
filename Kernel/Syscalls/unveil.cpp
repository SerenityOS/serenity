/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <Kernel/API/Unveil.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Unveil.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$unveil(Userspace<Syscall::SC_unveil_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    auto params = TRY(copy_typed_from_user(user_params));

    if (!params.path.characters && !params.permissions.characters) {
        m_unveil_data.with([&](auto& unveil_data) { unveil_data.state = VeilState::Locked; });
        return 0;
    }

    if (!((params.flags & to_underlying(UnveilFlags::CurrentProgram)) || (params.flags & to_underlying(UnveilFlags::AfterExec))))
        return EINVAL;

    // Note: If we inherited a locked state, then silently ignore the unveil request,
    // and let the user program potentially deal with an ENOENT error later on.
    if ((params.flags & static_cast<unsigned>(UnveilFlags::CurrentProgram)) && veil_state() == VeilState::LockedInherited)
        return 0;

    // Note: We only lock the unveil state for current program, while allowing adding
    // indefinitely unveil data before doing the actual exec().
    if ((params.flags & static_cast<unsigned>(UnveilFlags::CurrentProgram)) && veil_state() == VeilState::Locked)
        return EPERM;

    if (!params.path.characters || !params.permissions.characters)
        return EINVAL;

    if (params.permissions.length > 5)
        return EINVAL;

    auto path = TRY(get_syscall_path_argument(params.path));

    if (path->is_empty() || !path->view().starts_with('/'))
        return EINVAL;

    auto permissions = TRY(try_copy_kstring_from_user(params.permissions));

    OwnPtr<KString> new_unveiled_path;
    UnveilAccess new_permissions;
    TRY(prepare_parameters_for_new_unveiled_path(path->view(), permissions->view(), new_unveiled_path, new_permissions));

    if (params.flags & static_cast<unsigned>(UnveilFlags::CurrentProgram)) {
        TRY(unveil_data().with([&](auto& data) -> ErrorOr<void> {
            TRY(update_unveil_data(data, new_unveiled_path->view(), static_cast<UnveilAccess>(new_permissions)));
            return {};
        }));
    }

    if (params.flags & static_cast<unsigned>(UnveilFlags::AfterExec)) {
        TRY(exec_unveil_data().with([&](auto& data) -> ErrorOr<void> {
            // Note: The only valid way to get into this state is by using unveil before doing
            // an actual exec with the UnveilFlags::AfterExec flag. Then this state is applied on
            // the actual new program unveil data, and never on the m_exec_unveil_data.
            VERIFY(data.state != VeilState::LockedInherited);
            TRY(update_unveil_data(data, new_unveiled_path->view(), static_cast<UnveilAccess>(new_permissions)));
            return {};
        }));
    }

    /*TRY(unveil_data().with([&](auto& data) -> ErrorOr<void> {
        TRY(exec_unveil_data().with([&](auto& exec_data) -> ErrorOr<void> {
            Array<UnveilData*, 2> locked_unveil_datas { nullptr, nullptr };
            if (params.flags & static_cast<unsigned>(UnveilFlags::CurrentProgram)) {
                locked_unveil_datas[0] = &data;
            }
            if (params.flags & static_cast<unsigned>(UnveilFlags::AfterExec)) {
                // Note: The only valid way to get into this state is by using unveil before doing
                // an actual exec with the UnveilFlags::AfterExec flag. Then this state is applied on
                // the actual new program unveil data, and never on the m_exec_unveil_data.
                VERIFY(data.state != VeilState::LockedInherited);
                locked_unveil_datas[1] = &exec_data;
            }
            TRY(update_unveil_data(locked_unveil_datas, path->view(), permissions->view()));
            return {};
        }));
    }));*/
    return 0;
}

}
