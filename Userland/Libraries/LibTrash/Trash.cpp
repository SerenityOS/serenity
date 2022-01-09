/*
 * Copyright (c) 2022, nyabla <hewwo@nyabla.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Time.h>
#include <AK/URL.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibTrash/Trash.h>
#include <errno_codes.h>

namespace Trash {

TrashCan::TrashCan()
{
    m_trash_directory = String::formatted("{}/.trash", Core::StandardPaths::home_directory());
    m_info_file_path = String::formatted("{}/trash-info.ini", trash_directory());
}

ErrorOr<TrashItem> TrashCan::trash(String path, RecursionMode recursive)
{
    if (path.is_empty() || !path.starts_with("/"))
        return Error::from_string_literal("Must be an absolute path");
    if (!Core::File::exists(path))
        return Error::from_errno(ENOENT);
    if (Core::File::is_directory(path) && recursive == RecursionMode::Disallowed)
        return Error::from_errno(EISDIR);
    if (m_trash_directory.starts_with(path) || path.starts_with(m_trash_directory))
        return Error::from_errno(EPERM); //? not sure about this

    i64 timestamp = Time::now_realtime().to_milliseconds();
    String trash_path = trashed_filename(path, timestamp);

    auto info_file = Core::ConfigFile::open(m_info_file_path, Core::ConfigFile::AllowWriting::Yes);
    info_file->write_entry(URL::percent_encode(path), String::number(timestamp), "");

    TRY(Core::System::rename(path, trash_path));

    return TrashItem { path, trash_path, timestamp };
}

ErrorOr<void> TrashCan::empty()
{
    TRY(Core::File::remove(m_trash_directory, Core::File::RecursionMode::Allowed, true));
    TRY(create_trash_directory_if_needed());
    return {};
}

ErrorOr<void> TrashCan::remove(TrashItem item)
{
    TRY(Core::File::remove(item.trash_path, Core::File::RecursionMode::Allowed, true));

    auto info_file = Core::ConfigFile::open(m_info_file_path, Core::ConfigFile::AllowWriting::Yes);

    String encoded_origin_path = URL::percent_encode(item.origin_path);

    info_file->remove_entry(encoded_origin_path, String::number(item.timestamp));
    if (info_file->keys(encoded_origin_path).is_empty())
        info_file->remove_group(encoded_origin_path);

    return {};
}

ErrorOr<void> TrashCan::restore(TrashItem item)
{
    TRY(Core::System::rename(item.trash_path, item.origin_path));

    auto info_file = Core::ConfigFile::open(m_info_file_path, Core::ConfigFile::AllowWriting::Yes);

    String encoded_origin_path = URL::percent_encode(item.origin_path);

    info_file->remove_entry(encoded_origin_path, String::number(item.timestamp));
    if (info_file->keys(encoded_origin_path).is_empty())
        info_file->remove_group(encoded_origin_path);

    return {};
}

Vector<TrashItem> TrashCan::list()
{
    auto info_file = Core::ConfigFile::open(m_info_file_path, Core::ConfigFile::AllowWriting::No);

    Vector<TrashItem> trashed_items;

    Vector<String> trashed_paths = info_file->groups();

    if (trashed_paths.is_empty())
        return {};

    for (auto& trashed_path : trashed_paths) {
        String origin_path = URL::percent_decode(trashed_path);

        Vector<String> timestamps = info_file->keys(trashed_path);

        for (auto& string_timestamp : timestamps) {
            i64 timestamp = string_timestamp.to_int<i64>().value();

            trashed_items.append(TrashItem {
                origin_path,
                trashed_filename(origin_path, timestamp),
                timestamp });
        }
    }

    return trashed_items;
}

ErrorOr<Vector<TrashItem>> TrashCan::list_versions(String path)
{
    if (path.is_empty() || !path.starts_with("/"))
        return Error::from_string_literal("Must be an absolute path");

    String encoded_path = URL::percent_encode(path);

    auto info_file = Core::ConfigFile::open(m_info_file_path, Core::ConfigFile::AllowWriting::No);

    Vector<String> timestamps = info_file->keys(encoded_path);
    if (timestamps.is_empty())
        return Vector<TrashItem> {};

    Vector<TrashItem> versions;

    for (auto& string_timestamp : timestamps) {
        i64 timestamp = string_timestamp.to_int<i64>().value();

        versions.append(TrashItem {
            path,
            trashed_filename(path, timestamp),
            timestamp });
    }

    return versions;
}

String TrashCan::trash_directory()
{
    return m_trash_directory;
}

// Creates directory structure for trash
// $HOME/.trash/
//     files/
//     trash-info.ini
ErrorOr<void> TrashCan::create_trash_directory_if_needed()
{
    String trash_dir = trash_directory();

    if (!Core::File::is_directory(m_trash_directory))
        TRY(Core::System::mkdir(trash_dir, 0755));

    String trash_files_directory = String::formatted("{}/{}", m_trash_directory, "files");
    if (!Core::File::is_directory(trash_files_directory))
        TRY(Core::System::mkdir(trash_files_directory, 0755));

    if (!Core::File::exists(m_info_file_path)) {
        int fd = TRY(Core::System::open(m_info_file_path, O_CREAT, 0644));
        TRY(Core::System::close(fd));
    }

    return {};
}

String TrashCan::trashed_filename(String origin_path, time_t timestamp)
{
    return String::formatted("{}/files/{}.{}", m_trash_directory, timestamp, LexicalPath::basename(origin_path));
}

}
