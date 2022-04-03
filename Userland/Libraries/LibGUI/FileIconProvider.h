/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGUI/Forward.h>
#include <sys/types.h>

namespace GUI {

class FileIconProvider {
public:
    static Icon icon_for_path(String const&, mode_t);
    static Icon icon_for_path(String const&);
    static Icon icon_for_executable(String const&);

    static Icon filetype_image_icon();
    static Icon directory_icon();
    static Icon directory_open_icon();
    static Icon home_directory_icon();
    static Icon home_directory_open_icon();
    static Icon desktop_directory_icon();
};

}
