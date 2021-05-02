/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileUtils.h"
#include <AK/LexicalPath.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/MessageBox.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileUtils {

void delete_path(const String& path, GUI::Window* parent_window)
{
    struct stat st;
    if (lstat(path.characters(), &st)) {
        GUI::MessageBox::show(parent_window,
            String::formatted("lstat({}) failed: {}", path, strerror(errno)),
            "Delete failed",
            GUI::MessageBox::Type::Error);
    }

    bool is_directory = S_ISDIR(st.st_mode);
    auto result = Core::File::remove(path, Core::File::RecursionMode::Allowed, false);

    if (result.is_error()) {
        auto& error = result.error();
        if (is_directory) {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete directory \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        } else {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete file \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        }
    }
}

void delete_paths(const Vector<String>& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Really delete {}?", LexicalPath(paths[0]).basename());
    } else {
        message = String::formatted("Really delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;
    }

    for (auto& path : paths) {
        delete_path(path, parent_window);
    }
}

void create_trash_bin()
{
    auto trash_bin_path = Core::StandardPaths::trash_directory();
    auto trash_bin_files = String::formatted("{}/files", trash_bin_path);
    auto trash_bin_info = String::formatted("{}/info", trash_bin_path);

    if (!Core::File::is_directory(trash_bin_path)) {
        if (mkdir(trash_bin_path.characters(), 0755) < 0) {
            auto saved_errno = errno;
            warnln("Failed to create directory {}: {}", trash_bin_path, strerror(saved_errno));
        }
    }

    if (!Core::File::is_directory(trash_bin_files)) {
        if (mkdir(trash_bin_files.characters(), 0755) < 0) {
            auto saved_errno = errno;
            warnln("Failed to create directory {}: {}", trash_bin_files, strerror(saved_errno));
        }
    }

    if (!Core::File::is_directory(trash_bin_info)) {
        if (mkdir(trash_bin_info.characters(), 0755) < 0) {
            auto saved_errno = errno;
            warnln("Failed to create directory {}: {}", trash_bin_info, strerror(saved_errno));
        }
    }
}

[[maybe_unused]] bool move_to_trash(String const& path)
{
    auto file_name = LexicalPath(path).basename();
    auto file_info_name = String::formatted("{}.trashinfo", file_name);
    auto deletion_date = Core::DateTime::now().to_string("%Y-%m-%dT%H:%M:%S");
    auto trash_bin_files = String::formatted("{}/files", Core::StandardPaths::trash_directory());
    auto trash_bin_info = String::formatted("{}/info", Core::StandardPaths::trash_directory());

    create_trash_bin();

    auto trash_info_path = String::formatted("{}/{}", trash_bin_info, file_info_name);

    auto file_or_error = Core::File::open(trash_info_path, Core::IODevice::WriteOnly);

    if (file_or_error.is_error()) {
        warnln("Cannot write trash info for file '{}'", path);
        return false;
    }

    auto file = file_or_error.release_value();
    file->close();

    auto trash_info = Core::ConfigFile::open(trash_info_path);

    trash_info->write_entry("Trash Info", "Path", path);
    trash_info->write_entry("Trash Info", "DeletionDate", deletion_date);
    trash_info->sync();

    auto trash_file_path = String::formatted("{}/{}", trash_bin_files, file_name);

    auto rc = rename(path.characters(), trash_file_path.characters());

    if (rc < 0) {
        if (errno == EXDEV) {
            auto result = Core::File::copy_file_or_directory(
                trash_file_path,
                path,
                Core::File::RecursionMode::Allowed,
                Core::File::LinkMode::Disallowed,
                Core::File::AddDuplicateFileMarker::No);

            if (result.is_error()) {
                dbgln("Could not move '{}' to trash: {}", path, result.error().error_code);
                return false;
            }

            rc = unlink(path.characters());

            if (rc < 0)
                dbgln("Could not unlink '{}': {}", path, strerror(errno));
        }
    } else {
        dbgln("Could not rename '{}': {}", path, strerror(errno));
        return false;
    }

    return true;
}

// FIXME: I'm not sure this is a good practice
[[maybe_unused]] bool move_to_trash(Vector<String> const& paths)
{
    auto rv = false;

    for (auto& path : paths) {
        rv = move_to_trash(path);
    }

    return rv;
}

}
