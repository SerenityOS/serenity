/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Format.h>
#include <YAK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <sched.h>
#include <sys/stat.h>
#include <unistd.h>

struct WorkItem {
    enum class Type {
        CreateDirectory,
        DeleteDirectory,
        CopyFile,
        MoveFile,
        DeleteFile,
    };
    Type type;
    String source;
    String destination;
    off_t size;
};

static int perform_copy(Vector<String> const& sources, String const& destination);
static int perform_move(Vector<String> const& sources, String const& destination);
static int perform_delete(Vector<String> const& sources);
static int execute_work_items(Vector<WorkItem> const& items);
static void report_error(String message);
static void report_warning(String message);

int main(int argc, char** argv)
{
    String operation;
    Vector<String> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(operation, "Operation: either 'Copy', 'Move' or 'Delete'", "operation", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(paths, "Source paths, followed by a destination if applicable", "paths", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    if (operation == "Delete")
        return perform_delete(paths);

    String destination = paths.take_last();
    if (paths.is_empty()) {
        report_warning("At least one source and destination are required");
        return 1;
    }

    if (operation == "Copy")
        return perform_copy(paths, destination);
    if (operation == "Move")
        return perform_move(paths, destination);

    report_warning(String::formatted("Unknown operation '{}'", operation));
    return 0;
}

static void report_error(String message)
{
    outln("ERROR {}", message);
}

static void report_warning(String message)
{
    outln("WARN {}", message);
}

static bool collect_copy_work_items(String const& source, String const& destination, Vector<WorkItem>& items)
{
    struct stat st = {};
    if (lstat(source.characters(), &st) < 0) {
        auto original_errno = errno;
        report_error(String::formatted("stat: {}", strerror(original_errno)));
        return false;
    }

    if (!S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::CopyFile,
            .source = source,
            .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            .size = st.st_size,
        });
        return true;
    }

    // It's a directory.
    items.append(WorkItem {
        .type = WorkItem::Type::CreateDirectory,
        .source = {},
        .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
        .size = 0,
    });

    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        if (!collect_copy_work_items(
                LexicalPath::join(source, name).string(),
                LexicalPath::join(destination, LexicalPath::basename(source)).string(),
                items)) {
            return false;
        }
    }

    return true;
}

int perform_copy(Vector<String> const& sources, String const& destination)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        if (!collect_copy_work_items(source, destination, items))
            return 1;
    }

    return execute_work_items(items);
}

static bool collect_move_work_items(String const& source, String const& destination, Vector<WorkItem>& items)
{
    struct stat st = {};
    if (lstat(source.characters(), &st) < 0) {
        auto original_errno = errno;
        report_error(String::formatted("stat: {}", strerror(original_errno)));
        return false;
    }

    if (!S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::MoveFile,
            .source = source,
            .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            .size = st.st_size,
        });
        return true;
    }

    // It's a directory.
    items.append(WorkItem {
        .type = WorkItem::Type::CreateDirectory,
        .source = {},
        .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
        .size = 0,
    });

    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        if (!collect_move_work_items(
                LexicalPath::join(source, name).string(),
                LexicalPath::join(destination, LexicalPath::basename(source)).string(),
                items)) {
            return false;
        }
    }

    items.append(WorkItem {
        .type = WorkItem::Type::DeleteDirectory,
        .source = source,
        .destination = {},
        .size = 0,
    });

    return true;
}

int perform_move(Vector<String> const& sources, String const& destination)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        if (!collect_move_work_items(source, destination, items))
            return 1;
    }

    return execute_work_items(items);
}

static bool collect_delete_work_items(String const& source, Vector<WorkItem>& items)
{
    struct stat st = {};
    if (lstat(source.characters(), &st) < 0) {
        auto original_errno = errno;
        report_error(String::formatted("stat: {}", strerror(original_errno)));
        return false;
    }

    if (!S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::DeleteFile,
            .source = source,
            .destination = {},
            .size = st.st_size,
        });
        return true;
    }

    // It's a directory.
    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        if (!collect_delete_work_items(LexicalPath::join(source, name).string(), items))
            return false;
    }

    items.append(WorkItem {
        .type = WorkItem::Type::DeleteDirectory,
        .source = source,
        .destination = {},
        .size = 0,
    });

    return true;
}

int perform_delete(Vector<String> const& sources)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        if (!collect_delete_work_items(source, items))
            return 1;
    }

    return execute_work_items(items);
}

int execute_work_items(Vector<WorkItem> const& items)
{
    off_t total_work_bytes = 0;
    for (auto& item : items)
        total_work_bytes += item.size;

    off_t executed_work_bytes = 0;

    for (size_t i = 0; i < items.size(); ++i) {
        auto& item = items[i];
        off_t item_done = 0;
        auto print_progress = [&] {
            outln("PROGRESS {} {} {} {} {} {} {}", i, items.size(), executed_work_bytes, total_work_bytes, item_done, item.size, item.source);
        };

        auto copy_file = [&](String const& source, String const& destination) {
            auto source_file_or_error = Core::File::open(source, Core::OpenMode::ReadOnly);
            if (source_file_or_error.is_error()) {
                report_warning(String::formatted("Failed to open {} for reading: {}", source, source_file_or_error.error()));
                return false;
            }
            auto destination_file_or_error = Core::File::open(destination, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
            if (destination_file_or_error.is_error()) {
                report_warning(String::formatted("Failed to open {} for write: {}", destination, destination_file_or_error.error()));
                return false;
            }
            auto& source_file = *source_file_or_error.value();
            auto& destination_file = *destination_file_or_error.value();

            while (true) {
                print_progress();
                auto buffer = source_file.read(65536);
                if (buffer.is_empty())
                    break;
                if (!destination_file.write(buffer)) {
                    report_warning(String::formatted("Failed to write to destination file: {}", destination_file.error_string()));
                    return false;
                }
                item_done += buffer.size();
                executed_work_bytes += buffer.size();
                print_progress();
                // FIXME: Remove this once the kernel is smart enough to schedule other threads
                //        while we're doing heavy I/O. Right now, copying a large file will totally
                //        starve the rest of the system.
                sched_yield();
            }
            print_progress();
            return true;
        };

        switch (item.type) {

        case WorkItem::Type::CreateDirectory: {
            outln("MKDIR {}", item.destination);
            if (mkdir(item.destination.characters(), 0755) < 0 && errno != EEXIST) {
                auto original_errno = errno;
                report_error(String::formatted("mkdir: {}", strerror(original_errno)));
                return 1;
            }
            break;
        }

        case WorkItem::Type::DeleteDirectory: {
            if (rmdir(item.source.characters()) < 0) {
                auto original_errno = errno;
                report_error(String::formatted("rmdir: {}", strerror(original_errno)));
                return 1;
            }
            break;
        }

        case WorkItem::Type::CopyFile: {
            if (!copy_file(item.source, item.destination))
                return 1;

            break;
        }

        case WorkItem::Type::MoveFile: {
            if (rename(item.source.characters(), item.destination.characters()) == 0) {
                item_done += item.size;
                executed_work_bytes += item.size;
                print_progress();
                continue;
            }

            auto original_errno = errno;
            if (original_errno != EXDEV) {
                report_warning(String::formatted("Failed to move {}: {}", item.source, strerror(original_errno)));
                return 1;
            }

            // EXDEV means we have to copy the file data and then remove the original
            if (!copy_file(item.source, item.destination))
                return 1;

            if (unlink(item.source.characters()) < 0) {
                auto original_errno = errno;
                report_error(String::formatted("unlink: {}", strerror(original_errno)));
                return 1;
            }

            break;
        }

        case WorkItem::Type::DeleteFile: {
            if (unlink(item.source.characters()) < 0) {
                auto original_errno = errno;
                report_error(String::formatted("unlink: {}", strerror(original_errno)));
                return 1;
            }

            item_done += item.size;
            executed_work_bytes += item.size;
            print_progress();

            break;
        }

        default:
            VERIFY_NOT_REACHED();
        }
    }

    outln("FINISH");
    return 0;
}
