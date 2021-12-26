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

#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibELF/Image.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGLoader.h>
#include <sys/stat.h>

namespace GUI {

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
static Icon s_filetype_image_icon;
static RefPtr<Gfx::Bitmap> s_symlink_emblem;
static RefPtr<Gfx::Bitmap> s_symlink_emblem_small;

static HashMap<String, Icon> s_filetype_icons;
static HashMap<String, Vector<String>> s_filetype_patterns;

static void initialize_executable_icon_if_needed()
{
    static bool initialized = false;
    if (initialized)
        return;
    initialized = true;
    s_executable_icon = Icon::default_icon("filetype-executable");
}

static void initialize_if_needed()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

    auto config = Core::ConfigFile::open("/etc/FileIconProvider.ini");

    s_symlink_emblem = Gfx::Bitmap::load_from_file("/res/icons/symlink-emblem.png");
    s_symlink_emblem_small = Gfx::Bitmap::load_from_file("/res/icons/symlink-emblem-small.png");

    s_hard_disk_icon = Icon::default_icon("hard-disk");
    s_directory_icon = Icon::default_icon("filetype-folder");
    s_directory_open_icon = Icon::default_icon("filetype-folder-open");
    s_inaccessible_directory_icon = Icon::default_icon("filetype-folder-inaccessible");
    s_home_directory_icon = Icon::default_icon("home-directory");
    s_home_directory_open_icon = Icon::default_icon("home-directory-open");
    s_file_icon = Icon::default_icon("filetype-unknown");
    s_symlink_icon = Icon::default_icon("filetype-symlink");
    s_socket_icon = Icon::default_icon("filetype-socket");

    s_filetype_image_icon = Icon::default_icon("filetype-image");

    initialize_executable_icon_if_needed();

    for (auto& filetype : config->keys("Icons")) {
        s_filetype_icons.set(filetype, Icon::default_icon(String::formatted("filetype-{}", filetype)));
        s_filetype_patterns.set(filetype, config->read_entry("Icons", filetype).split(','));
    }

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

Icon FileIconProvider::icon_for_executable(const String& path)
{
    static HashMap<String, Icon> app_icon_cache;

    if (auto it = app_icon_cache.find(path); it != app_icon_cache.end())
        return it->value;

    initialize_executable_icon_if_needed();

    // If the icon for an app isn't in the cache we attempt to load the file as an ELF image and extract
    // the serenity_app_icon_* sections which should contain the icons as raw PNG data. In the future it would
    // be better if the binary signalled the image format being used or we deduced it, e.g. using magic bytes.
    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error()) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    auto& mapped_file = file_or_error.value();

    if (mapped_file->size() < SELFMAG) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    if (memcmp(mapped_file->data(), ELFMAG, SELFMAG) != 0) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    auto image = ELF::Image((const u8*)mapped_file->data(), mapped_file->size());
    if (!image.is_valid()) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    // If any of the required sections are missing then use the defaults
    Icon icon;
    struct IconSection {
        const char* section_name;
        int image_size;
    };

    static const IconSection icon_sections[] = { { .section_name = "serenity_icon_s", .image_size = 16 }, { .section_name = "serenity_icon_m", .image_size = 32 } };

    bool had_error = false;
    for (const auto& icon_section : icon_sections) {
        auto section = image.lookup_section(icon_section.section_name);

        RefPtr<Gfx::Bitmap> bitmap;
        if (section.is_undefined()) {
            bitmap = s_executable_icon.bitmap_for_size(icon_section.image_size)->clone();
        } else {
            bitmap = Gfx::load_png_from_memory(reinterpret_cast<const u8*>(section.raw_data()), section.size());
        }

        if (!bitmap) {
            dbgln("Failed to find embedded icon and failed to clone default icon for application {} at icon size {}", path, icon_section.image_size);
            had_error = true;
            continue;
        }

        icon.set_bitmap_for_size(icon_section.image_size, std::move(bitmap));
    }

    if (had_error) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }
    app_icon_cache.set(path, icon);
    return icon;
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
    if (S_ISLNK(mode)) {
        auto raw_symlink_target = Core::File::read_link(path);
        if (raw_symlink_target.is_null())
            return s_symlink_icon;

        String target_path;
        if (raw_symlink_target.starts_with('/')) {
            target_path = raw_symlink_target;
        } else {
            target_path = Core::File::real_path_for(String::formatted("{}/{}", LexicalPath(path).dirname(), raw_symlink_target));
        }
        auto target_icon = icon_for_path(target_path);

        Icon generated_icon;
        for (auto size : target_icon.sizes()) {
            auto& emblem = size < 32 ? *s_symlink_emblem_small : *s_symlink_emblem;
            auto original_bitmap = target_icon.bitmap_for_size(size);
            ASSERT(original_bitmap);
            auto generated_bitmap = original_bitmap->clone();
            if (!generated_bitmap) {
                dbgln("Failed to clone {}x{} icon for symlink variant", size, size);
                return s_symlink_icon;
            }
            GUI::Painter painter(*generated_bitmap);
            painter.blit({ size - emblem.width(), size - emblem.height() }, emblem, emblem.rect());

            generated_icon.set_bitmap_for_size(size, move(generated_bitmap));
        }
        return generated_icon;
    }
    if (S_ISSOCK(mode))
        return s_socket_icon;

    if (mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return icon_for_executable(path);

    if (Gfx::Bitmap::is_path_a_supported_image_format(path.view()))
        return s_filetype_image_icon;

    for (auto& filetype : s_filetype_icons.keys()) {
        auto patterns = s_filetype_patterns.get(filetype).value();
        for (auto& pattern : patterns) {
            if (path.matches(pattern, CaseSensitivity::CaseInsensitive))
                return s_filetype_icons.get(filetype).value();
        }
    }

    return s_file_icon;
}

}
