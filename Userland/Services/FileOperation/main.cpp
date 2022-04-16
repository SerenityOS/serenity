/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
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

static void report_warning(StringView message);
static void report_error(StringView message);
static ErrorOr<int> perform_copy(Vector<StringView> const& sources, String const& destination);
static ErrorOr<int> perform_move(Vector<StringView> const& sources, String const& destination);
static ErrorOr<int> perform_delete(Vector<StringView> const& sources);
static ErrorOr<int> execute_work_items(Vector<WorkItem> const& items);
static ErrorOr<NonnullOwnPtr<Core::Stream::File>> open_destination_file(String const& destination);
static String deduplicate_destination_file_name(String const& destination);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    String operation;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(operation, "Operation: either 'Copy', 'Move' or 'Delete'", "operation", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(paths, "Source paths, followed by a destination if applicable", "paths", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    if (operation == "Delete")
        return perform_delete(paths);

    String destination = paths.take_last();
    if (paths.is_empty())
        return Error::from_string_literal("At least one source and destination are required");

    if (operation == "Copy")
        return perform_copy(paths, destination);
    if (operation == "Move")
        return perform_move(paths, destination);

    // FIXME: Return the formatted string directly. There is no way to do this right now without the temporary going out of scope and being destroyed.
    report_error(String::formatted("Unknown operation '{}'", operation));
    return Error::from_string_literal("Unknown operation");
}

static void report_warning(StringView message)
{
    outln("WARN {}", message);
}

static void report_error(StringView message)
{
    outln("ERROR {}", message);
}

static ErrorOr<int> collect_copy_work_items(String const& source, String const& destination, Vector<WorkItem>& items)
{
    if (auto const st = TRY(Core::System::lstat(source)); !S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::CopyFile,
            .source = source,
            .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            .size = st.st_size,
        });
        return 0;
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
        TRY(collect_copy_work_items(
            LexicalPath::join(source, name).string(),
            LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            items));
    }

    return 0;
}

ErrorOr<int> perform_copy(Vector<StringView> const& sources, String const& destination)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        TRY(collect_copy_work_items(source, destination, items));
    }

    return execute_work_items(items);
}

static ErrorOr<int> collect_move_work_items(String const& source, String const& destination, Vector<WorkItem>& items)
{
    if (auto const st = TRY(Core::System::lstat(source)); !S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::MoveFile,
            .source = source,
            .destination = LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            .size = st.st_size,
        });
        return 0;
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
        TRY(collect_move_work_items(
            LexicalPath::join(source, name).string(),
            LexicalPath::join(destination, LexicalPath::basename(source)).string(),
            items));
    }

    items.append(WorkItem {
        .type = WorkItem::Type::DeleteDirectory,
        .source = source,
        .destination = {},
        .size = 0,
    });

    return 0;
}

ErrorOr<int> perform_move(Vector<StringView> const& sources, String const& destination)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        TRY(collect_move_work_items(source, destination, items));
    }

    return execute_work_items(items);
}

static ErrorOr<int> collect_delete_work_items(String const& source, Vector<WorkItem>& items)
{
    if (auto const st = TRY(Core::System::lstat(source)); !S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::DeleteFile,
            .source = source,
            .destination = {},
            .size = st.st_size,
        });
        return 0;
    }

    // It's a directory.
    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        TRY(collect_delete_work_items(LexicalPath::join(source, name).string(), items));
    }

    items.append(WorkItem {
        .type = WorkItem::Type::DeleteDirectory,
        .source = source,
        .destination = {},
        .size = 0,
    });

    return 0;
}

ErrorOr<int> perform_delete(Vector<StringView> const& sources)
{
    Vector<WorkItem> items;

    for (auto& source : sources) {
        TRY(collect_delete_work_items(source, items));
    }

    return execute_work_items(items);
}

ErrorOr<int> execute_work_items(Vector<WorkItem> const& items)
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

        auto copy_file = [&](String const& source, String const& destination) -> ErrorOr<int> {
            auto source_file = TRY(Core::Stream::File::open(source, Core::Stream::OpenMode::Read));
            // FIXME: When the file already exists, let the user choose the next action instead of renaming it by default.
            auto destination_file = TRY(open_destination_file(destination));
            auto buffer = TRY(ByteBuffer::create_zeroed(64 * KiB));

            while (true) {
                print_progress();
                auto bytes_read = TRY(source_file->read(buffer.bytes()));
                if (bytes_read.is_empty())
                    break;
                if (auto result = destination_file->write(bytes_read); result.is_error()) {
                    // FIXME: Return the formatted string directly. There is no way to do this right now without the temporary going out of scope and being destroyed.
                    report_warning(String::formatted("Failed to write to destination file: {}", result.error()));
                    return result.error();
                }
                item_done += bytes_read.size();
                executed_work_bytes += bytes_read.size();
                print_progress();
                // FIXME: Remove this once the kernel is smart enough to schedule other threads
                //        while we're doing heavy I/O. Right now, copying a large file will totally
                //        starve the rest of the system.
                sched_yield();
            }
            print_progress();
            return 0;
        };

        switch (item.type) {

        case WorkItem::Type::CreateDirectory: {
            outln("MKDIR {}", item.destination);
            // FIXME: Support deduplication like open_destination_file() when the directory already exists.
            if (mkdir(item.destination.characters(), 0755) < 0 && errno != EEXIST)
                return Error::from_syscall("mkdir", -errno);
            break;
        }

        case WorkItem::Type::DeleteDirectory: {
            TRY(Core::System::rmdir(item.source));
            break;
        }

        case WorkItem::Type::CopyFile: {
            TRY(copy_file(item.source, item.destination));
            break;
        }

        case WorkItem::Type::MoveFile: {
            String destination = item.destination;
            while (true) {
                if (rename(item.source.characters(), destination.characters()) == 0) {
                    item_done += item.size;
                    executed_work_bytes += item.size;
                    print_progress();
                    break;
                }
                auto original_errno = errno;

                if (original_errno == EEXIST) {
                    destination = deduplicate_destination_file_name(destination);
                    continue;
                }

                if (original_errno != EXDEV) {
                    // FIXME: Return the formatted string directly. There is no way to do this right now without the temporary going out of scope and being destroyed.
                    report_warning(String::formatted("Failed to move {}: {}", item.source, strerror(original_errno)));
                    return Error::from_errno(original_errno);
                }

                // EXDEV means we have to copy the file data and then remove the original
                TRY(copy_file(item.source, item.destination));
                TRY(Core::System::unlink(item.source));
                break;
            }

            break;
        }

        case WorkItem::Type::DeleteFile: {
            TRY(Core::System::unlink(item.source));

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

ErrorOr<NonnullOwnPtr<Core::Stream::File>> open_destination_file(String const& destination)
{
    auto destination_file_or_error = Core::Stream::File::open(destination, (Core::Stream::OpenMode)(Core::Stream::OpenMode::Write | Core::Stream::OpenMode::Truncate | Core::Stream::OpenMode::MustBeNew));
    if (destination_file_or_error.is_error() && destination_file_or_error.error().code() == EEXIST) {
        return open_destination_file(deduplicate_destination_file_name(destination));
    }
    return destination_file_or_error;
}

String deduplicate_destination_file_name(String const& destination)
{
    LexicalPath destination_path(destination);
    auto title_without_counter = destination_path.title();
    size_t next_counter = 1;

    auto last_hyphen_index = title_without_counter.find_last('-');
    if (last_hyphen_index.has_value()) {
        auto counter_string = title_without_counter.substring_view(*last_hyphen_index + 1);
        auto last_counter = counter_string.to_uint();
        if (last_counter.has_value()) {
            next_counter = *last_counter + 1;
            title_without_counter = title_without_counter.substring_view(0, *last_hyphen_index);
        }
    }

    StringBuilder basename;
    basename.appendff("{}-{}", title_without_counter, next_counter);
    if (!destination_path.extension().is_empty()) {
        basename.append(".");
        basename.append(destination_path.extension());
    }

    return LexicalPath::join(destination_path.dirname(), basename.to_string()).string();
}
