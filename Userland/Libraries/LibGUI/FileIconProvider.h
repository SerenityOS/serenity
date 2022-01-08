/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Icon.h>
#include <sys/types.h>

namespace GUI {

class FileIconProvider {
public:
    static FileIconProvider& the();

    Icon icon_for_path(const String&, mode_t);
    Icon icon_for_path(const String&);
    Icon icon_for_executable(const String&);

    Icon filetype_image_icon() { return m_filetype_image_icon; }
    Icon directory_icon() { return m_directory_icon; }
    Icon directory_open_icon() { return m_directory_open_icon; }
    Icon home_directory_icon() { return m_home_directory_icon; }
    Icon home_directory_open_icon() { return m_home_directory_open_icon; }
    Icon desktop_directory_icon() { return m_desktop_directory_icon; }

private:
    FileIconProvider();

    Icon m_hard_disk_icon;
    Icon m_directory_icon;
    Icon m_directory_open_icon;
    Icon m_inaccessible_directory_icon;
    Icon m_desktop_directory_icon;
    Icon m_home_directory_icon;
    Icon m_home_directory_open_icon;
    Icon m_file_icon;
    Icon m_symlink_icon;
    Icon m_socket_icon;
    Icon m_executable_icon;
    Icon m_filetype_image_icon;
    RefPtr<Gfx::Bitmap> m_symlink_emblem;
    RefPtr<Gfx::Bitmap> m_symlink_emblem_small;

    HashMap<String, Icon> m_filetype_icons;
    HashMap<String, Vector<String>> m_filetype_patterns;
};

}
