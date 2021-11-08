/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/Custody.h>
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
        node.set_metadata({ node.path(), new_permissions, node.was_explicitly_unveiled() });
        update_intermediate_node_permissions(node, new_permissions);
    }
}

ErrorOr<FlatPtr> Process::sys$unveil(Userspace<const Syscall::SC_unveil_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto params = TRY(copy_typed_from_user(user_params));

    if (!params.path.characters && !params.permissions.characters) {
        m_veil_state = VeilState::Locked;
        return 0;
    }

    if (m_veil_state == VeilState::Locked)
        return EPERM;

    if (!params.path.characters || !params.permissions.characters)
        return EINVAL;

    if (params.permissions.length > 5)
        return EINVAL;

    auto path = TRY(get_syscall_path_argument(params.path));

    if (path->is_empty() || !path->view().starts_with('/'))
        return EINVAL;

    auto permissions = TRY(try_copy_kstring_from_user(params.permissions));

    // Let's work out permissions first...
    unsigned new_permissions = 0;
    for (const char permission : permissions->view()) {
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
    auto custody_or_error = VirtualFileSystem::the().resolve_path_without_veil(path->view(), VirtualFileSystem::the().root_custody(), &parent_custody);
    if (!custody_or_error.is_error()) {
        new_unveiled_path = TRY(custody_or_error.value()->try_serialize_absolute_path());
    } else if (custody_or_error.error().code() == ENOENT && parent_custody && (new_permissions & UnveilAccess::CreateOrRemove)) {
        auto parent_custody_path = TRY(parent_custody->try_serialize_absolute_path());
        new_unveiled_path = TRY(KLexicalPath::try_join(parent_custody_path->view(), KLexicalPath::basename(path->view())));
    } else {
        // FIXME Should this be EINVAL?
        return custody_or_error.release_error();
    }

    auto path_parts = KLexicalPath::parts(new_unveiled_path->view());
    auto it = path_parts.begin();
    auto& matching_node = m_unveiled_paths.traverse_until_last_accessible_node(it, path_parts.end());
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
            update_intermediate_node_permissions(matching_node, (UnveilAccess)new_permissions);

        matching_node.set_metadata({ matching_node.path(), (UnveilAccess)new_permissions, true });
        m_veil_state = VeilState::Dropped;
        return 0;
    }

    matching_node.insert(
        it,
        path_parts.end(),
        { new_unveiled_path->view(), (UnveilAccess)new_permissions, true },
        [](auto& parent, auto& it) -> Optional<UnveilMetadata> {
            auto path = String::formatted("{}/{}", parent.path(), *it);
            return UnveilMetadata { path, parent.permissions(), false };
        });

    VERIFY(m_veil_state != VeilState::Locked);
    m_veil_state = VeilState::Dropped;
    return 0;
}

}
