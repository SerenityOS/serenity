/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <LibGUI/Forward.h>
#include <sys/types.h>

namespace GUI {

class FileIconProvider {
public:
    static Icon icon_for_path(const String&, mode_t);
    static Icon icon_for_path(const String&);
    static Icon icon_for_executable(const String&);

    static Icon filetype_image_icon();
    static Icon directory_icon();
    static Icon directory_open_icon();
    static Icon home_directory_icon();
    static Icon home_directory_open_icon();
    static Icon desktop_directory_icon();
};

}
