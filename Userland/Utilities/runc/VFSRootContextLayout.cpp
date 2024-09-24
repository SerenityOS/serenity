/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>

#include "VFSRootContextLayout.h"

static bool is_source_none(StringView source)
{
    return source == "none"sv;
}

static ErrorOr<int> get_source_fd(StringView source)
{
    if (is_source_none(source))
        return -1;
    auto fd_or_error = Core::System::open(source, O_RDWR);
    if (fd_or_error.is_error())
        fd_or_error = Core::System::open(source, O_RDONLY);
    return fd_or_error;
}

ErrorOr<void> VFSRootContextLayout::mount_new_filesystem(StringView fstype, StringView source, StringView target_path, int flags)
{
    auto source_fd = TRY(get_source_fd(source));
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(target_path));

    auto target_path_string = TRY(String::from_utf8(target_path));
    auto fstype_string = TRY(String::from_utf8(fstype));

    TRY(Core::System::mount({}, source_fd, actual_path, fstype, flags));

    auto mount = Mount { Mount::Type::RegularMount, {}, source_fd, target_path_string, fstype_string };
    TRY(m_mounts.try_append(mount));
    return {};
}

ErrorOr<void> VFSRootContextLayout::chown(StringView path, uid_t uid, gid_t gid)
{
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(path));
    return Core::System::chown(actual_path, uid, gid);
}

ErrorOr<void> VFSRootContextLayout::chmod(StringView path, mode_t mode)
{
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(path));
    return Core::System::chmod(actual_path, mode);
}

ErrorOr<void> VFSRootContextLayout::symlink(StringView path, StringView target_path)
{
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(path));
    return Core::System::symlink(target_path, actual_path);
}

ErrorOr<void> VFSRootContextLayout::copy_as_original(StringView source_path)
{
    return copy_to_custom_location(source_path, source_path);
}

ErrorOr<void> VFSRootContextLayout::copy_to_custom_location(StringView source_path, StringView target_path)
{
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(target_path));
    TRY(FileSystem::copy_file_or_directory(
        actual_path, source_path,
        FileSystem::RecursionMode::Disallowed,
        FileSystem::LinkMode::Disallowed,
        FileSystem::AddDuplicateFileMarker::No,
        FileSystem::PreserveMode::Ownership | FileSystem::PreserveMode::Permissions));
    return {};
}

ErrorOr<void> VFSRootContextLayout::mkdir(StringView target_path)
{
    auto actual_path = TRY(generate_path_with_relation_to_preparation_environment_path(target_path));
    TRY(Core::System::mkdir(actual_path.bytes_as_string_view(), 0700));
    return {};
}

ErrorOr<String> VFSRootContextLayout::generate_path_with_relation_to_preparation_environment_path(StringView target_path) const
{
    VERIFY(LexicalPath(target_path).is_canonical());
    auto path = LexicalPath::join(m_preparation_environment_path.bytes_as_string_view(), target_path);
    return String::from_byte_string(path.string());
}

VFSRootContextLayout::VFSRootContextLayout(String preparation_environment_path, unsigned target_vfs_root_context_id)
    : m_preparation_environment_path(move(preparation_environment_path))
    , m_target_vfs_root_context_id(target_vfs_root_context_id)
{
}

ErrorOr<NonnullOwnPtr<VFSRootContextLayout>> VFSRootContextLayout::create(StringView preparation_environment_path, unsigned target_vfs_root_context_id)
{
    auto path = TRY(String::from_utf8(preparation_environment_path));
    return adopt_nonnull_own_or_enomem(new (nothrow) VFSRootContextLayout(move(path), target_vfs_root_context_id));
}

ErrorOr<void> VFSRootContextLayout::apply_mounts_on_vfs_root_context_id()
{
    for (auto& mount : m_mounts) {
        auto path_on_preparation_environment = TRY(generate_path_with_relation_to_preparation_environment_path(mount.path));
        TRY(Core::System::copy_mount({},
            m_target_vfs_root_context_id,
            path_on_preparation_environment.bytes_as_string_view(),
            mount.path, 0));
    }

    for (auto& mount : m_mounts.in_reverse()) {
        auto path_on_preparation_environment = TRY(generate_path_with_relation_to_preparation_environment_path(mount.path));
        TRY(Core::System::umount({}, path_on_preparation_environment.bytes_as_string_view()));
    }
    m_mounts.clear();
    return {};
}
