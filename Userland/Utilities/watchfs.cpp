/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/DirectoryEntry.h>
#include <LibCore/EventLoop.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static ErrorOr<NonnullRefPtr<Core::FileWatcher>> watch_directory_child_creation(StringView path, bool exit_after_first_change)
{
    auto watcher = TRY(Core::FileWatcher::create());

    watcher->on_change = [path, exit_after_first_change](auto&) {
        outln("{} has new file", path);
        if (exit_after_first_change)
            exit(1);
    };

    TRY(watcher->add_watch(path, Core::FileWatcherEvent::Type::ChildCreated));
    return watcher;
}

static ErrorOr<NonnullRefPtr<Core::FileWatcher>> watch_directory_child_deletion(StringView path, bool exit_after_first_change)
{
    auto watcher = TRY(Core::FileWatcher::create());

    watcher->on_change = [path, exit_after_first_change](auto&) {
        outln("{} has file being deleted", path);
        if (exit_after_first_change)
            exit(1);
    };

    TRY(watcher->add_watch(path, Core::FileWatcherEvent::Type::ChildDeleted));
    return watcher;
}

static ErrorOr<NonnullRefPtr<Core::FileWatcher>> watch_file_content_modified(StringView path, bool exit_after_first_change)
{
    auto watcher = TRY(Core::FileWatcher::create());

    watcher->on_change = [path, exit_after_first_change](auto&) {
        outln("{} content is modified", path);
        if (exit_after_first_change)
            exit(1);
    };

    TRY(watcher->add_watch(path, Core::FileWatcherEvent::Type::ContentModified));
    return watcher;
}

static ErrorOr<NonnullRefPtr<Core::FileWatcher>> watch_file_metadata_modified(StringView path, bool exit_after_first_change)
{
    auto watcher = TRY(Core::FileWatcher::create());

    watcher->on_change = [path, exit_after_first_change](auto&) {
        outln("{} metadata is modified", path);
        if (exit_after_first_change)
            exit(1);
    };

    TRY(watcher->add_watch(path, Core::FileWatcherEvent::Type::MetadataModified));
    return watcher;
}

static ErrorOr<NonnullRefPtr<Core::FileWatcher>> watch_file_being_deleted(StringView path, bool exit_after_first_change)
{
    auto watcher = TRY(Core::FileWatcher::create());

    watcher->on_change = [path, exit_after_first_change](auto&) {
        outln("{} is deleted", path);
        if (exit_after_first_change)
            exit(1);
    };

    TRY(watcher->add_watch(path, Core::FileWatcherEvent::Type::Deleted));
    return watcher;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;
    bool flag_exit_after_first_change = false;
    bool flag_watch_all_events = false;
    bool flag_watch_file_being_deleted = false;
    bool flag_watch_file_being_content_modified = false;
    bool flag_watch_file_being_metadata_modified = false;
    bool flag_watch_directory_child_creation = false;
    bool flag_watch_directory_child_deletion = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Watch for filesystem activity in a directory.");
    args_parser.add_option(flag_exit_after_first_change, "Wait for first change and exit", "exit-after-change", 'E');
    args_parser.add_option(flag_watch_all_events, "Watch all types of events", "watch-all-events", 'a');
    args_parser.add_option(flag_watch_file_being_deleted, "Watch file deletion events", "watch-delete-events", 'd');
    args_parser.add_option(flag_watch_file_being_content_modified, "Watch file content being modified", "watch-file-modify-events", 'm');
    args_parser.add_option(flag_watch_file_being_metadata_modified, "Watch file metadata being modified", "watch-file-metadata-events", 'M');
    args_parser.add_option(flag_watch_directory_child_creation, "Watch directory child creation events", "watch-directory-child-creation-events", 'c');
    args_parser.add_option(flag_watch_directory_child_deletion, "Watch directory child deletion events", "watch-directory-child-deletion-events", 'D');
    args_parser.add_positional_argument(paths, "Path to watch", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (flag_watch_all_events) {
        flag_watch_file_being_deleted = true;
        flag_watch_file_being_content_modified = true;
        flag_watch_file_being_metadata_modified = true;
        flag_watch_directory_child_creation = true;
        flag_watch_directory_child_deletion = true;
    }

    if (paths.is_empty())
        paths.append("."sv);

    Vector<NonnullRefPtr<Core::FileWatcher>> watchers;

    Core::EventLoop event_loop;

    for (auto& path : paths) {
        auto st = TRY(Core::System::stat(path));
        auto directory_entry_type = Core::DirectoryEntry::directory_entry_type_from_stat(st.st_mode);
        switch (directory_entry_type) {
        case Core::DirectoryEntry::Type::Directory: {
            if (flag_watch_directory_child_creation) {
                auto watcher = TRY(watch_directory_child_creation(path, flag_exit_after_first_change));
                TRY(watchers.try_append(watcher));
            }

            if (flag_watch_directory_child_deletion) {
                auto watcher = TRY(watch_directory_child_deletion(path, flag_exit_after_first_change));
                TRY(watchers.try_append(watcher));
            }
            break;
        }
        case Core::DirectoryEntry::Type::File: {
            if (flag_watch_file_being_content_modified) {
                auto watcher = TRY(watch_file_content_modified(path, flag_exit_after_first_change));
                TRY(watchers.try_append(watcher));
            }
            if (flag_watch_file_being_metadata_modified) {
                auto watcher = TRY(watch_file_metadata_modified(path, flag_exit_after_first_change));
                TRY(watchers.try_append(watcher));
            }
            if (flag_watch_file_being_deleted) {
                auto watcher = TRY(watch_file_being_deleted(path, flag_exit_after_first_change));
                TRY(watchers.try_append(watcher));
            }
            break;
        }

        default:
            warnln("Trying to watch unsupported file type");
            break;
        }
    }

    if (watchers.is_empty())
        return Error::from_string_literal("Watchers list is empty");

    event_loop.exec();
    return 0;
}
