/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/StandardPaths.h>
#include <LibELF/Image.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGLoader.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace GUI {

FileIconProvider& FileIconProvider::the()
{
    static FileIconProvider s_the;
    return s_the;
}

FileIconProvider::FileIconProvider()
{
    auto config = Core::ConfigFile::open("/etc/FileIconProvider.ini");

    m_symlink_emblem = Gfx::Bitmap::try_load_from_file("/res/icons/symlink-emblem.png").release_value_but_fixme_should_propagate_errors();
    m_symlink_emblem_small = Gfx::Bitmap::try_load_from_file("/res/icons/symlink-emblem-small.png").release_value_but_fixme_should_propagate_errors();

    m_hard_disk_icon = Icon::default_icon("hard-disk");
    m_directory_icon = Icon::default_icon("filetype-folder");
    m_directory_open_icon = Icon::default_icon("filetype-folder-open");
    m_inaccessible_directory_icon = Icon::default_icon("filetype-folder-inaccessible");
    m_home_directory_icon = Icon::default_icon("home-directory");
    m_home_directory_open_icon = Icon::default_icon("home-directory-open");
    m_desktop_directory_icon = Icon::default_icon("desktop");
    m_file_icon = Icon::default_icon("filetype-unknown");
    m_symlink_icon = Icon::default_icon("filetype-symlink");
    m_socket_icon = Icon::default_icon("filetype-socket");

    m_executable_icon = Icon::default_icon("filetype-executable");
    m_filetype_image_icon = Icon::default_icon("filetype-image");

    for (auto& filetype : config->keys("Icons")) {
        m_filetype_icons.set(filetype, Icon::default_icon(String::formatted("filetype-{}", filetype)));
        m_filetype_patterns.set(filetype, config->read_entry("Icons", filetype).split(','));
    }
}

Icon FileIconProvider::icon_for_path(const String& path)
{
    struct stat stat;
    if (::stat(path.characters(), &stat) < 0)
        return m_file_icon;
    return icon_for_path(path, stat.st_mode);
}

Icon FileIconProvider::icon_for_executable(const String& path)
{
    static HashMap<String, Icon> app_icon_cache;

    if (auto it = app_icon_cache.find(path); it != app_icon_cache.end())
        return it->value;

    // If the icon for an app isn't in the cache we attempt to load the file as an ELF image and extract
    // the serenity_app_icon_* sections which should contain the icons as raw PNG data. In the future it would
    // be better if the binary signalled the image format being used or we deduced it, e.g. using magic bytes.
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error()) {
        app_icon_cache.set(path, m_executable_icon);
        return m_executable_icon;
    }

    auto& mapped_file = file_or_error.value();

    if (mapped_file->size() < SELFMAG) {
        app_icon_cache.set(path, m_executable_icon);
        return m_executable_icon;
    }

    if (memcmp(mapped_file->data(), ELFMAG, SELFMAG) != 0) {
        app_icon_cache.set(path, m_executable_icon);
        return m_executable_icon;
    }

    auto image = ELF::Image((const u8*)mapped_file->data(), mapped_file->size());
    if (!image.is_valid()) {
        app_icon_cache.set(path, m_executable_icon);
        return m_executable_icon;
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
        if (!section.has_value()) {
            bitmap = m_executable_icon.bitmap_for_size(icon_section.image_size);
        } else {
            // FIXME: Use the ImageDecoder service.
            auto frame_or_error = Gfx::PNGImageDecoderPlugin(reinterpret_cast<u8 const*>(section->raw_data()), section->size()).frame(0);
            if (!frame_or_error.is_error()) {
                bitmap = frame_or_error.value().image;
            }
        }

        if (!bitmap) {
            dbgln("Failed to find embedded icon and failed to clone default icon for application {} at icon size {}", path, icon_section.image_size);
            had_error = true;
            continue;
        }

        icon.set_bitmap_for_size(icon_section.image_size, std::move(bitmap));
    }

    if (had_error) {
        app_icon_cache.set(path, m_executable_icon);
        return m_executable_icon;
    }
    app_icon_cache.set(path, icon);
    return icon;
}

Icon FileIconProvider::icon_for_path(const String& path, mode_t mode)
{
    if (path == "/")
        return m_hard_disk_icon;
    if (S_ISDIR(mode)) {
        if (path == Core::StandardPaths::home_directory())
            return m_home_directory_icon;
        if (path == Core::StandardPaths::desktop_directory())
            return m_desktop_directory_icon;
        if (access(path.characters(), R_OK | X_OK) < 0)
            return m_inaccessible_directory_icon;
        return m_directory_icon;
    }
    if (S_ISLNK(mode)) {
        auto raw_symlink_target = Core::File::read_link(path);
        if (raw_symlink_target.is_null())
            return m_symlink_icon;

        String target_path;
        if (raw_symlink_target.starts_with('/')) {
            target_path = raw_symlink_target;
        } else {
            target_path = Core::File::real_path_for(String::formatted("{}/{}", LexicalPath::dirname(path), raw_symlink_target));
        }
        auto target_icon = icon_for_path(target_path);

        Icon generated_icon;
        for (auto size : target_icon.sizes()) {
            auto& emblem = size < 32 ? *m_symlink_emblem_small : *m_symlink_emblem;
            auto original_bitmap = target_icon.bitmap_for_size(size);
            VERIFY(original_bitmap);
            auto generated_bitmap_or_error = original_bitmap->clone();
            if (generated_bitmap_or_error.is_error()) {
                dbgln("Failed to clone {}x{} icon for symlink variant", size, size);
                return m_symlink_icon;
            }
            auto generated_bitmap = generated_bitmap_or_error.release_value_but_fixme_should_propagate_errors();
            GUI::Painter painter(*generated_bitmap);
            painter.blit({ size - emblem.width(), size - emblem.height() }, emblem, emblem.rect());

            generated_icon.set_bitmap_for_size(size, move(generated_bitmap));
        }
        return generated_icon;
    }
    if (S_ISSOCK(mode))
        return m_socket_icon;

    if (mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return icon_for_executable(path);

    if (Gfx::Bitmap::is_path_a_supported_image_format(path.view()))
        return m_filetype_image_icon;

    for (auto& filetype : m_filetype_icons.keys()) {
        auto pattern_it = m_filetype_patterns.find(filetype);
        if (pattern_it == m_filetype_patterns.end())
            continue;
        for (auto& pattern : pattern_it->value) {
            if (path.matches(pattern, CaseSensitivity::CaseInsensitive))
                return m_filetype_icons.get(filetype).value();
        }
    }

    return m_file_icon;
}

}
