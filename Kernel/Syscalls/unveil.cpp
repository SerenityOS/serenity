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
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static void update_intermediate_node_permissions(UnveilNode& root_node, UnveilAccess new_permissions)
{
    for (auto& entry : root_node.children()) {
        auto& node = static_cast<UnveilNode&>(*entry.value);
        if (node.was_explicitly_unveiled())
            continue;
        node.metadata_value().permissions = new_permissions;
        update_intermediate_node_permissions(node, new_permissions);
    }
}

static ErrorOr<void> update_unveil_data(Process::UnveilData& locked_unveil_data, StringView unveiled_path, UnveilAccess new_permissions)
{
    auto path_parts = KLexicalPath::parts(unveiled_path);
    auto it = path_parts.begin();
    // Note: For the sake of completence, we check if the locked state was inherited
    // by an execve'd sequence. If that is the case, just silently ignore this.
    if (locked_unveil_data.state == VeilState::LockedInherited)
        return {};
    // NOTE: We have to check again, since the veil may have been locked by another thread
    //       while we were parsing the arguments.
    if (locked_unveil_data.state == VeilState::Locked)
        return EPERM;

    auto& matching_node = locked_unveil_data.paths.traverse_until_last_accessible_node(it, path_parts.end());
    if (it.is_end()) {
        // If the path has already been explicitly unveiled, do not allow elevating its permissions.
        if (matching_node.was_explicitly_unveiled()) {
            if (new_permissions & ~matching_node.permissions())
                return EPERM;
        }

        // It is possible that nodes that are "grandchildren" of the matching node have already been unveiled.
        // This means that there may be intermediate nodes between this one and the unveiled "grandchildren"
        // that inherited the current node's previous permissions. Those nodes now need their permissions
        // updated to match the current node.
        if (matching_node.permissions() != new_permissions)
            update_intermediate_node_permissions(matching_node, new_permissions);

        matching_node.metadata_value().explicitly_unveiled = true;
        matching_node.metadata_value().permissions = new_permissions;
        locked_unveil_data.state = VeilState::Dropped;
        return {};
    }

    auto new_unveiled_path = TRY(KString::try_create(unveiled_path));
    TRY(matching_node.insert(
        it,
        path_parts.end(),
        { move(new_unveiled_path), new_permissions, true },
        [](auto& parent, auto& it) -> ErrorOr<Optional<UnveilMetadata>> {
            auto path = TRY(KString::formatted("{}/{}", parent.path(), *it));
            return UnveilMetadata(move(path), parent.permissions(), false);
        }));

    VERIFY(locked_unveil_data.state != VeilState::Locked);
    locked_unveil_data.state = VeilState::Dropped;
    return {};
}

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

    auto path = TRY(get_syscall_path_argument(params.path));

    if (path->is_empty() || !path->view().starts_with('/'))
        return EINVAL;

    auto permissions = TRY(get_syscall_string_fixed_buffer<5>(params.permissions));

    // Let's work out permissions first...
    unsigned new_permissions = 0;
    for (char const permission : permissions.representable_view()) {
        switch (permission) {
        case 'r':
            new_permissions |= UnveilAccess::Read;
            break;
        case 'w':
            new_permissions |= UnveilAccess::Write;
            break;
        case 'x':
            new_permissions |= UnveilAccess::Execute;
            break;
        case 'c':
            new_permissions |= UnveilAccess::CreateOrRemove;
            break;
        case 'b':
            new_permissions |= UnveilAccess::Browse;
            break;
        default:
            return EINVAL;
        }
    }

    // Now, let's try and resolve the path and obtain custody of the inode on the disk, and if not, bail out with
    // the error from resolve_path_without_veil()
    // However, if the user specified unveil() with "c" permissions, we don't set errno if ENOENT is encountered,
    // because they most likely intend the program to create the file for them later on.
    // If this case is encountered, the parent node of the path is returned and the custody of that inode is used instead.
    RefPtr<Custody> parent_custody; // Parent inode in case of ENOENT
    OwnPtr<KString> new_unveiled_path;
    auto vfs_root_context_root_custody = Process::current().vfs_root_context()->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
        return custody;
    });
    auto custody_or_error = VirtualFileSystem::resolve_path_without_veil(vfs_root_context(), credentials(), path->view(), vfs_root_context_root_custody, &parent_custody);
    if (!custody_or_error.is_error()) {
        new_unveiled_path = TRY(custody_or_error.value()->try_serialize_absolute_path());
    } else if (custody_or_error.error().code() == ENOENT && parent_custody && (new_permissions & UnveilAccess::CreateOrRemove)) {
        auto parent_custody_path = TRY(parent_custody->try_serialize_absolute_path());
        new_unveiled_path = TRY(KLexicalPath::try_join(parent_custody_path->view(), KLexicalPath::basename(path->view())));
    } else {
        // FIXME Should this be EINVAL?
        return custody_or_error.release_error();
    }

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
    return 0;
}

}
