/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <unistd.h>

class VFSRootContextLayout {
public:
    struct Mount {
        enum class Type {
            BindMount,
            RegularMount,
        };
        Type type;
        Optional<unsigned> source_vfs_root_context_id;
        Optional<unsigned> source_fd;
        String path;
        String fstype;
    };

    ErrorOr<void> mount_new_filesystem(StringView fstype, StringView source, StringView target_path, int flags);
    // ErrorOr<void> bind_mount(int source_vfs_root_context_id, StringView source_path, StringView target_path, int flags);

    ErrorOr<void> chown(StringView path, uid_t uid, gid_t gid);
    ErrorOr<void> chmod(StringView target_path, mode_t);
    ErrorOr<void> symlink(StringView path, StringView target_path);
    ErrorOr<void> copy_as_original(StringView source_path);
    ErrorOr<void> copy_to_custom_location(StringView source_path, StringView target_path);
    ErrorOr<void> mkdir(StringView target_path);

    static ErrorOr<NonnullOwnPtr<VFSRootContextLayout>> create(StringView preparation_environment_path, unsigned target_vfs_root_context_id);
    ErrorOr<void> apply_mounts_on_vfs_root_context_id();

private:
    VFSRootContextLayout(String preparation_environment_path, unsigned target_vfs_root_context_id);

    ErrorOr<String> generate_path_with_relation_to_preparation_environment_path(StringView target_path) const;

    String m_preparation_environment_path;
    unsigned const m_target_vfs_root_context_id { 0 };
    Vector<Mount> m_mounts;
};
