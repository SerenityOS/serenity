/*
 * Copyright (c) 2022, nyabla <hewwo@nyabla.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <AK/Vector.h>

// home directory trash
namespace Trash {

enum class RecursionMode {
    Allowed,
    Disallowed
};

struct TrashItem {
    String origin_path;
    String trash_path;
    i64 timestamp;
};

class TrashCan {
public:
    TrashCan();

    ErrorOr<TrashItem> trash(String path, RecursionMode);
    ErrorOr<void> empty();
    ErrorOr<void> remove(TrashItem);
    ErrorOr<void> restore(TrashItem);
    Vector<TrashItem> list();
    ErrorOr<Vector<TrashItem>> list_versions(String path);

    String trashed_filename(String origin_path, i64 timestamp);
    String trash_directory();

    ErrorOr<void> create_trash_directory_if_needed();

private:
    String m_trash_directory;
    String m_info_file_path;
};

}
