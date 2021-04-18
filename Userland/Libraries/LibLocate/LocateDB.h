/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Base64.h>
#include <AK/BitCast.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Queue.h>
#include <AK/SinglyLinkedListWithCount.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibC/string.h>
#include <LibC/unistd.h>
#include <LibCore/File.h>
#include <LibLocate/Types.h>
#include <stdio.h>
#include <sys/stat.h>

namespace Locate {
constexpr static auto locate_db_path = "/var/lib/locate/locate.db";
constexpr static auto magic_header = "locatedb|1";

class LocateDB {
public:
    LocateDB(String const& path, LocateDbMode mode);
    ~LocateDB();

    void write_header();
    void write_directory(DirectoryInfo& directory_info);
    bool verify_header();
    OwnPtr<DirectoryInfo> get_next_directory();
    const PermissionInfo* get_permission_info(uint32_t db_id);

private:
    long m_file_size;
    FILE* m_file_handle;
    LocateDbMode m_mode;
    HashMap<uint32_t, NonnullOwnPtr<PermissionInfo>> m_path_relations;

    enum class ChunkType {
        DirectoryStart,
        DirectoryEnd,
        File,
        Directory,
        __Count,
    };

    struct DirStartEndHeader {
        uint8_t type;
        uint32_t db_id;
        uint32_t parent_db_id;
        size_t path_size;
    };

    struct ChildFileData {
        uint8_t type;
        uint32_t db_id;
        uint32_t parent_db_id;
        size_t name_size;
    };

    ChunkType identify_chunk();
    void parse_directory_info(DirectoryInfo& directory_info);
    NonnullOwnPtr<ChildInfo> parse_child_info();
};

}
