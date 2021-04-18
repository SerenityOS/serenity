/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedListWithCount.h>

namespace Locate {

enum class FileType {
    Directory,
    BlockDevice,
    File,
    Symlink
};

typedef struct ChildInfo {
    String name;
    FileType type;
    uint32_t db_id;
    uint32_t parent_db_id;
} ChildInfo;

typedef struct DirectoryInfo {
    String path;
    uint32_t db_id;
    uint32_t parent_db_id;
    SinglyLinkedListWithCount<NonnullOwnPtr<ChildInfo>> children;
} DirectoryInfo;

typedef struct PermissionInfo {
    uint32_t parent_id;
    String path;
} PermissionInfo;

enum class LocateDbMode {
    Read,
    Write
};

}
