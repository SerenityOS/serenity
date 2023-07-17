/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>

namespace BitTorrent {

ErrorOr<void> create_file_with_subdirs(AK::DeprecatedString const& absolute_path);

struct FileInTorrent {
    DeprecatedString const path;
    i64 const size;
};

struct LocalFile {
    DeprecatedString const path_in_torrent;
    DeprecatedString const path;
    i64 const size;
};

}
