/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <sched.h>
#include <sys/stat.h>

struct WorkItem {
    enum class Type {
        CreateDirectory,
        CopyFile,
    };
    Type type;
    String source;
    String destination;
    off_t size;
};

static int perform_copy(Vector<String> const& sources, String const& destination);
static int execute_work_items(Vector<WorkItem> const& items);
static void report_error(String message);
static void report_warning(String message);

int main(int argc, char** argv)
{
    String operation;
    Vector<String> sources;
    String destination;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(operation, "Operation: either 'Copy' or 'Move'", "operation", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(sources, "Sources", "sources", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(destination, "Destination", "destination", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    if (operation == "Copy")
        return perform_copy(sources, destination);

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

static bool collect_work_items(String const& source, String const& destination, Vector<WorkItem>& items)
{
    struct stat st = {};
    if (stat(source.characters(), &st) < 0) {
        auto original_errno = errno;
        report_error(String::formatted("stat: {}", strerror(original_errno)));
        return false;
    }

    if (!S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::CopyFile,
            .source = source,
            .destination = String::formatted("{}/{}", destination, LexicalPath::basename(source)),
            .size = st.st_size,
        });
        return true;
    }

    // It's a directory.
    items.append(WorkItem {
        .type = WorkItem::Type::CreateDirectory,
        .source = {},
        .destination = String::formatted("{}/{}", destination, LexicalPath::basename(source)),
        .size = 0,
    });

    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        if (!collect_work_items(
                String::formatted("{}/{}", source, name),
                String::formatted("{}/{}", destination, LexicalPath::basename(source)),
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
        if (!collect_work_items(source, destination, items))
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

        case WorkItem::Type::CopyFile: {
            auto source_file_or_error = Core::File::open(item.source, Core::OpenMode::ReadOnly);
            if (source_file_or_error.is_error()) {
                report_warning(String::formatted("Failed to open {} for reading: {}", item.source, source_file_or_error.error()));
                return 1;
            }
            auto destination_file_or_error = Core::File::open(item.destination, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
            if (destination_file_or_error.is_error()) {
                report_warning(String::formatted("Failed to open {} for write: {}", item.destination, destination_file_or_error.error()));
                return 1;
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
                    return 1;
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
            break;
        }

        default:
            VERIFY_NOT_REACHED();
        }
    }

    outln("FINISH");
    return 0;
}
