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
#include <LibCore/System.h>
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

    ErrorOr<void> mount_new_filesystem(StringView fstype, StringView source, StringView target_path, int flags);

    ErrorOr<void> chown(StringView path, uid_t uid, gid_t gid);
    ErrorOr<void> chmod(StringView target_path, mode_t);
    ErrorOr<void> symlink(StringView path, StringView target_path);
    ErrorOr<void> copy_as_original(StringView source_path);
    ErrorOr<void> copy_to_custom_location(StringView source_path, StringView target_path);
    ErrorOr<void> mkdir(StringView target_path);

    static ErrorOr<NonnullOwnPtr<VFSRootContextLayout>> create_with_root_mount_point(StringView preparation_environment_path);
    ErrorOr<void> apply_mounts_on_vfs_root_context_id();

    static ErrorOr<void> mount_root_filesystem(StringView path, StringView fstype, StringView source, int flags);

    unsigned id() const { return m_target_vfs_root_context_id; }

private:
    VFSRootContextLayout(String preparation_environment_path, unsigned target_vfs_root_context_id);

    ErrorOr<String> generate_path_with_relation_to_preparation_environment_path(StringView target_path) const;

    String m_preparation_environment_path;
    unsigned const m_target_vfs_root_context_id { 0 };
    Vector<Mount> m_mounts;
};
