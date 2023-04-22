/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
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

ErrorOr<void> update_unveil_data(UnveilData& locked_unveil_data, StringView unveiled_path, UnveilAccess new_permissions)
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

static ErrorOr<void> prepare_unveil_permissions(StringView permissions, UnveilAccess& new_permissions)
{
    unsigned new_unveil_permissions = 0;
    for (char const permission : permissions) {
        switch (permission) {
        case 'r':
            new_unveil_permissions |= UnveilAccess::Read;
            break;
        case 'w':
            new_unveil_permissions |= UnveilAccess::Write;
            break;
        case 'x':
            new_unveil_permissions |= UnveilAccess::Execute;
            break;
        case 'c':
            new_unveil_permissions |= UnveilAccess::CreateOrRemove;
            break;
        case 'b':
            new_unveil_permissions |= UnveilAccess::Browse;
            break;
        default:
            return EINVAL;
        }
    }
    new_permissions = static_cast<UnveilAccess>(new_unveil_permissions);
    return {};
}

ErrorOr<void> prepare_parameters_for_new_jail_unveiled_path(StringView unveiled_path, StringView permissions, UnveilAccess& new_permissions)
{
    // Let's work out permissions first...
    TRY(prepare_unveil_permissions(permissions, new_permissions));

    // NOTE: We basically don't know anything about the about-to-be-jailed-program (or programs)
    // so we should not be smart and just ensure the path is canonical.
    // NOTE: We don't do anything smarter than this also because we are probably under a spinlock,
    // so trying to resolve any path with VirtualFileSystem code is just wrong in this case.
    if (!KLexicalPath::is_canonical(unveiled_path))
        return EINVAL;
    return {};
}

ErrorOr<void> prepare_parameters_for_new_unveiled_path(StringView unveiled_path, StringView permissions, OwnPtr<KString>& new_unveiled_path, UnveilAccess& new_permissions)
{
    // Let's work out permissions first...
    TRY(prepare_unveil_permissions(permissions, new_permissions));

    // Now, let's try and resolve the path and obtain custody of the inode on the disk, and if not, bail out with
    // the error from resolve_path_without_veil()
    // However, if the user specified unveil() with "c" permissions, we don't set errno if ENOENT is encountered,
    // because they most likely intend the program to create the file for them later on.
    // If this case is encountered, the parent node of the path is returned and the custody of that inode is used instead.
    RefPtr<Custody> parent_custody; // Parent inode in case of ENOENT
    auto current_process_credentials = Process::current().credentials();
    auto custody_or_error = VirtualFileSystem::the().resolve_path_without_veil(current_process_credentials, unveiled_path, VirtualFileSystem::the().root_custody(), &parent_custody);
    if (!custody_or_error.is_error()) {
        new_unveiled_path = TRY(custody_or_error.value()->try_serialize_absolute_path());
    } else if (custody_or_error.error().code() == ENOENT && parent_custody && (new_permissions & UnveilAccess::CreateOrRemove)) {
        auto parent_custody_path = TRY(parent_custody->try_serialize_absolute_path());
        new_unveiled_path = TRY(KLexicalPath::try_join(parent_custody_path->view(), KLexicalPath::basename(unveiled_path)));
    } else {
        // FIXME Should this be EINVAL?
        return custody_or_error.release_error();
    }
    return {};
}

}
