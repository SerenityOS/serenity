/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGfx/Bitmap.h>
#include <sys/stat.h>

namespace GUI {

#define ENUMERATE_FILETYPES(F) \
    F(cplusplus, ".cpp")       \
    F(form, ".frm")            \
    F(hackstudio, ".hsp")      \
    F(header, ".h")            \
    F(html, ".html")           \
    F(image, ".png")           \
    F(java, ".java")           \
    F(javascript, ".js")       \
    F(library, ".so", ".a")    \
    F(markdown, ".md")         \
    F(music, ".midi")          \
    F(object, ".o", ".obj")    \
    F(pdf, ".pdf")             \
    F(python, ".py")           \
    F(sound, ".wav")           \
    F(ini, ".ini")             \
    F(text, ".txt")            \
    F(spreadsheet, ".sheets")

#define __ENUMERATE_FILETYPE(filetype_name, ...) \
    static Icon s_filetype_##filetype_name##_icon;
ENUMERATE_FILETYPES(__ENUMERATE_FILETYPE)
#undef __ENUMERATE_FILETYPE

static Icon s_hard_disk_icon;
static Icon s_directory_icon;
static Icon s_directory_open_icon;
static Icon s_inaccessible_directory_icon;
static Icon s_home_directory_icon;
static Icon s_home_directory_open_icon;
static Icon s_file_icon;
static Icon s_symlink_icon;
static Icon s_socket_icon;
static Icon s_executable_icon;

static void initialize_if_needed()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;
    s_hard_disk_icon = Icon::default_icon("hard-disk");
    s_directory_icon = Icon::default_icon("filetype-folder");
    s_directory_open_icon = Icon::default_icon("filetype-folder-open");
    s_inaccessible_directory_icon = Icon::default_icon("filetype-folder-inaccessible");
    s_home_directory_icon = Icon::default_icon("home-directory");
    s_home_directory_open_icon = Icon::default_icon("home-directory-open");
    s_file_icon = Icon::default_icon("filetype-unknown");
    s_symlink_icon = Icon::default_icon("filetype-symlink");
    s_socket_icon = Icon::default_icon("filetype-socket");
    s_executable_icon = Icon::default_icon("filetype-executable");

#define __ENUMERATE_FILETYPE(filetype_name, ...) \
    s_filetype_##filetype_name##_icon = Icon::default_icon("filetype-" #filetype_name);
    ENUMERATE_FILETYPES(__ENUMERATE_FILETYPE)
#undef __ENUMERATE_FILETYPE

    s_initialized = true;
}

Icon FileIconProvider::directory_icon()
{
    initialize_if_needed();
    return s_directory_icon;
}

Icon FileIconProvider::directory_open_icon()
{
    initialize_if_needed();
    return s_directory_open_icon;
}

Icon FileIconProvider::home_directory_icon()
{
    initialize_if_needed();
    return s_home_directory_icon;
}

Icon FileIconProvider::home_directory_open_icon()
{
    initialize_if_needed();
    return s_home_directory_open_icon;
}

Icon FileIconProvider::filetype_image_icon()
{
    initialize_if_needed();
    return s_filetype_image_icon;
}

Icon FileIconProvider::icon_for_path(const String& path)
{
    struct stat stat;
    if (::stat(path.characters(), &stat) < 0)
        return {};
    return icon_for_path(path, stat.st_mode);
}

Icon FileIconProvider::icon_for_path(const String& path, mode_t mode)
{
    initialize_if_needed();
    if (path == "/")
        return s_hard_disk_icon;
    if (S_ISDIR(mode)) {
        if (path == Core::StandardPaths::home_directory())
            return s_home_directory_icon;
        if (access(path.characters(), R_OK | X_OK) < 0)
            return s_inaccessible_directory_icon;
        return s_directory_icon;
    }
    if (S_ISLNK(mode))
        return s_symlink_icon;
    if (S_ISSOCK(mode))
        return s_socket_icon;
    if (mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return s_executable_icon;

#define __ENUMERATE_FILETYPE(filetype_name, filetype_extensions...)      \
    for (auto& extension : { filetype_extensions }) {                    \
        if (path.ends_with(extension, CaseSensitivity::CaseInsensitive)) \
            return s_filetype_##filetype_name##_icon;                    \
    }
    ENUMERATE_FILETYPES(__ENUMERATE_FILETYPE)
#undef __ENUMERATE_FILETYPE

    return s_file_icon;
}

}
