/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/RAMBackedFileType.h>

namespace Kernel {

struct segmented_global_inode_index {
    StringView name;
    RAMBackedFileType file_type;
    u32 primary;
    u16 subdirectory;
    u32 property;
};

constexpr segmented_global_inode_index global_inode_ids[] = {
    { "."sv, RAMBackedFileType::Directory, 0, 0, 1 }, // NOTE: This is here for the root directory
    { "self"sv, RAMBackedFileType::Directory, 0, 0, 2 }
};

struct segmented_process_directory_entry {
    StringView name;
    RAMBackedFileType file_type;
    u16 subdirectory;
    u32 property;
};

constexpr segmented_process_directory_entry main_process_directory_root_entry = { "."sv, RAMBackedFileType::Directory, 0, 0 };
constexpr segmented_process_directory_entry process_fd_subdirectory_root_entry = { "."sv, RAMBackedFileType::Directory, 1, 0 };
constexpr segmented_process_directory_entry process_stacks_subdirectory_root_entry = { "."sv, RAMBackedFileType::Directory, 2, 0 };
constexpr segmented_process_directory_entry process_children_subdirectory_root_entry = { "."sv, RAMBackedFileType::Directory, 3, 0 };

constexpr segmented_process_directory_entry process_fd_directory_entry = { "fd"sv, RAMBackedFileType::Directory, 1, 0 };
constexpr segmented_process_directory_entry process_stacks_directory_entry = { "stacks"sv, RAMBackedFileType::Directory, 2, 0 };
constexpr segmented_process_directory_entry process_children_directory_entry = { "children"sv, RAMBackedFileType::Directory, 3, 0 };
constexpr segmented_process_directory_entry process_unveil_list_entry = { "unveil"sv, RAMBackedFileType::Regular, 0, 1 };
constexpr segmented_process_directory_entry process_pledge_list_entry = { "pledge"sv, RAMBackedFileType::Regular, 0, 2 };
constexpr segmented_process_directory_entry process_fds_list_entry = { "fds"sv, RAMBackedFileType::Regular, 0, 3 };
constexpr segmented_process_directory_entry process_exe_symlink_entry = { "exe"sv, RAMBackedFileType::Link, 0, 4 };
constexpr segmented_process_directory_entry process_cwd_symlink_entry = { "cwd"sv, RAMBackedFileType::Link, 0, 5 };
constexpr segmented_process_directory_entry process_perf_events_entry = { "perf_events"sv, RAMBackedFileType::Regular, 0, 6 };
constexpr segmented_process_directory_entry process_vm_entry = { "vm"sv, RAMBackedFileType::Regular, 0, 7 };
constexpr segmented_process_directory_entry process_cmdline_entry = { "cmdline"sv, RAMBackedFileType::Regular, 0, 8 };
constexpr segmented_process_directory_entry main_process_directory_entries[] = {
    process_fd_directory_entry,
    process_stacks_directory_entry,
    process_children_directory_entry,
    process_unveil_list_entry,
    process_pledge_list_entry,
    process_fds_list_entry,
    process_exe_symlink_entry,
    process_cwd_symlink_entry,
    process_perf_events_entry,
    process_vm_entry,
    process_cmdline_entry,
};

}
