/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/MappedFile.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace GUI {

static Icon s_hard_disk_icon;
static Icon s_directory_icon;
static Icon s_directory_open_icon;
static Icon s_inaccessible_directory_icon;
static Icon s_desktop_directory_icon;
static Icon s_home_directory_icon;
static Icon s_home_directory_open_icon;
static Icon s_git_directory_icon;
static Icon s_git_directory_open_icon;
static Icon s_file_icon;
static Icon s_symlink_icon;
static Icon s_socket_icon;
static Icon s_executable_icon;
static Icon s_filetype_image_icon;
static RefPtr<Gfx::Bitmap> s_symlink_emblem;
static RefPtr<Gfx::Bitmap> s_symlink_emblem_small;

static HashMap<ByteString, Icon> s_filetype_icons;
static HashMap<ByteString, Vector<ByteString>> s_filetype_patterns;

static void initialize_executable_icon_if_needed()
{
    static bool initialized = false;
    if (initialized)
        return;
    initialized = true;
    s_executable_icon = Icon::default_icon("filetype-executable"sv);
}

static void initialize_filetype_image_icon_if_needed()
{
    static bool initialized = false;
    if (initialized)
        return;
    initialized = true;
    s_filetype_image_icon = Icon::default_icon("filetype-image"sv);
}

static void initialize_if_needed()
{
    static bool s_initialized = false;
    if (s_initialized)
        return;

    auto config = Core::ConfigFile::open("/etc/FileIconProvider.ini").release_value_but_fixme_should_propagate_errors();

    s_symlink_emblem = Gfx::Bitmap::load_from_file("/res/icons/symlink-emblem.png"sv).release_value_but_fixme_should_propagate_errors();
    s_symlink_emblem_small = Gfx::Bitmap::load_from_file("/res/icons/symlink-emblem-small.png"sv).release_value_but_fixme_should_propagate_errors();

    s_hard_disk_icon = Icon::default_icon("hard-disk"sv);
    s_directory_icon = Icon::default_icon("filetype-folder"sv);
    s_directory_open_icon = Icon::default_icon("filetype-folder-open"sv);
    s_inaccessible_directory_icon = Icon::default_icon("filetype-folder-inaccessible"sv);
    s_home_directory_icon = Icon::default_icon("home-directory"sv);
    s_home_directory_open_icon = Icon::default_icon("home-directory-open"sv);
    s_git_directory_icon = Icon::default_icon("git-directory"sv);
    s_git_directory_open_icon = Icon::default_icon("git-directory-open"sv);
    s_desktop_directory_icon = Icon::default_icon("desktop"sv);
    s_file_icon = Icon::default_icon("filetype-unknown"sv);
    s_symlink_icon = Icon::default_icon("filetype-symlink"sv);
    s_socket_icon = Icon::default_icon("filetype-socket"sv);

    initialize_filetype_image_icon_if_needed();
    initialize_executable_icon_if_needed();

    for (auto& filetype : config->keys("Icons")) {
        s_filetype_icons.set(filetype, Icon::default_icon(ByteString::formatted("filetype-{}", filetype)));
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

Icon FileIconProvider::desktop_directory_icon()
{
    initialize_if_needed();
    return s_desktop_directory_icon;
}

Icon FileIconProvider::home_directory_open_icon()
{
    initialize_if_needed();
    return s_home_directory_open_icon;
}

Icon FileIconProvider::git_directory_icon()
{
    initialize_if_needed();
    return s_git_directory_icon;
}

Icon FileIconProvider::git_directory_open_icon()
{
    initialize_if_needed();
    return s_git_directory_open_icon;
}

Icon FileIconProvider::filetype_image_icon()
{
    initialize_filetype_image_icon_if_needed();
    return s_filetype_image_icon;
}

Icon FileIconProvider::icon_for_path(StringView path)
{
    auto stat_or_error = Core::System::stat(path);
    if (stat_or_error.is_error())
        return s_file_icon;
    return icon_for_path(path, stat_or_error.release_value().st_mode);
}

Icon FileIconProvider::icon_for_executable(ByteString const& path)
{
    static HashMap<ByteString, Icon> app_icon_cache;

    if (auto it = app_icon_cache.find(path); it != app_icon_cache.end())
        return it->value;

    initialize_executable_icon_if_needed();

    // If the icon for an app isn't in the cache we attempt to load the file as an ELF image and extract
    // the serenity_app_icon_* sections which should contain the icons as raw PNG data. In the future it would
    // be better if the binary signalled the image format being used or we deduced it, e.g. using magic bytes.
    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error()) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    auto& mapped_file = file_or_error.value();

    if (mapped_file->size().release_value() < SELFMAG) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    if (memcmp(mapped_file->data(), ELFMAG, SELFMAG) != 0) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    auto image = ELF::Image((u8 const*)mapped_file->data(), mapped_file->size().release_value());
    if (!image.is_valid()) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }

    // If any of the required sections are missing then use the defaults
    Icon icon;
    struct IconSection {
        StringView section_name;
        int image_size;
    };

    static constexpr Array<IconSection, 2> icon_sections = {
        IconSection { .section_name = "serenity_icon_s"sv, .image_size = 16 },
        IconSection { .section_name = "serenity_icon_m"sv, .image_size = 32 }
    };

    bool had_error = false;
    for (auto const& icon_section : icon_sections) {
        auto section = image.lookup_section(icon_section.section_name);

        RefPtr<Gfx::Bitmap const> bitmap;
        if (!section.has_value()) {
            bitmap = s_executable_icon.bitmap_for_size(icon_section.image_size);
        } else {
            // FIXME: Use the ImageDecoder service.
            if (Gfx::PNGImageDecoderPlugin::sniff({ section->raw_data(), section->size() })) {
                auto png_decoder = Gfx::PNGImageDecoderPlugin::create({ section->raw_data(), section->size() });
                if (!png_decoder.is_error()) {
                    auto frame_or_error = png_decoder.value()->frame(0);
                    if (!frame_or_error.is_error()) {
                        bitmap = frame_or_error.value().image;
                    }
                }
            }
        }

        if (!bitmap) {
            dbgln("Failed to find embedded icon and failed to clone default icon for application {} at icon size {}", path, icon_section.image_size);
            had_error = true;
            continue;
        }

        icon.set_bitmap_for_size(icon_section.image_size, move(bitmap));
    }

    if (had_error) {
        app_icon_cache.set(path, s_executable_icon);
        return s_executable_icon;
    }
    app_icon_cache.set(path, icon);
    return icon;
}

Icon FileIconProvider::icon_for_path(StringView path, mode_t mode)
{
    initialize_if_needed();
    if (path == "/")
        return s_hard_disk_icon;
    if (S_ISDIR(mode)) {
        if (path == Core::StandardPaths::home_directory())
            return s_home_directory_icon;
        if (path == Core::StandardPaths::desktop_directory())
            return s_desktop_directory_icon;
        if (Core::System::access(path, R_OK | X_OK).is_error())
            return s_inaccessible_directory_icon;
        if (path.ends_with(".git"sv))
            return s_git_directory_icon;
        return s_directory_icon;
    }
    if (S_ISLNK(mode)) {
        auto raw_symlink_target_or_error = FileSystem::read_link(path);
        if (raw_symlink_target_or_error.is_error())
            return s_symlink_icon;
        auto raw_symlink_target = raw_symlink_target_or_error.release_value();

        ByteString target_path;
        if (raw_symlink_target.starts_with('/')) {
            target_path = raw_symlink_target;
        } else {
            auto error_or_path = FileSystem::real_path(ByteString::formatted("{}/{}", LexicalPath::dirname(path), raw_symlink_target));
            if (error_or_path.is_error())
                return s_symlink_icon;

            target_path = error_or_path.release_value();
        }
        auto target_icon = icon_for_path(target_path);

        Icon generated_icon;
        for (auto size : target_icon.sizes()) {
            auto& emblem = size < 32 ? *s_symlink_emblem_small : *s_symlink_emblem;
            auto original_bitmap = target_icon.bitmap_for_size(size);
            VERIFY(original_bitmap);
            auto generated_bitmap_or_error = original_bitmap->clone();
            if (generated_bitmap_or_error.is_error()) {
                dbgln("Failed to clone {}x{} icon for symlink variant", size, size);
                return s_symlink_icon;
            }
            auto generated_bitmap = generated_bitmap_or_error.release_value_but_fixme_should_propagate_errors();
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

    if (Gfx::Bitmap::is_path_a_supported_image_format(path))
        return s_filetype_image_icon;

    for (auto& filetype : s_filetype_icons.keys()) {
        auto pattern_it = s_filetype_patterns.find(filetype);
        if (pattern_it == s_filetype_patterns.end())
            continue;
        for (auto& pattern : pattern_it->value) {
            if (path.matches(pattern, CaseSensitivity::CaseInsensitive))
                return s_filetype_icons.get(filetype).value();
        }
    }

    return s_file_icon;
}

}
