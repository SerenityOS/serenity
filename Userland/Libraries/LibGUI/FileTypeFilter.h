/*
 * Copyright (c) 2023, Marcus Nilsson <marcus.nilsson@genarp.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <AK/Vector.h>

namespace GUI {

struct FileTypeFilter {
    DeprecatedString name;
    Optional<Vector<DeprecatedString>> extensions;

    static FileTypeFilter all_files()
    {
        return FileTypeFilter { "All Files", {} };
    }

    static FileTypeFilter image_files()
    {
        return FileTypeFilter { "Image Files", Vector<DeprecatedString> { "png", "gif", "bmp", "dip", "pbm", "pgm", "ppm", "ico", "jpeg", "jpg", "dds", "qoi" } };
    }
};

}
