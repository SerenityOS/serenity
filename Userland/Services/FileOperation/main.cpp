/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <sched.h>
#include <sys/stat.h>

static int perform_copy(const String& source, const String& destination);

int main(int argc, char** argv)
{
    String operation;
    String source;
    String destination;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(operation, "Operation", "operation", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(source, "Source", "source", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(destination, "Destination", "destination", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    if (operation == "Copy")
        return perform_copy(source, destination);

    warnln("Unknown operation '{}'", operation);
    return 0;
}

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

static bool collect_work_items(const String& source, const String& destination, Vector<WorkItem>& items)
{
    struct stat st = {};
    if (stat(source.characters(), &st) < 0) {
        perror("stat");
        return false;
    }

    if (!S_ISDIR(st.st_mode)) {
        // It's a file.
        items.append(WorkItem {
            .type = WorkItem::Type::CopyFile,
            .source = source,
            .destination = String::formatted("{}/{}", destination, LexicalPath(source).basename()),
            .size = st.st_size,
        });
        return true;
    }

    // It's a directory.
    items.append(WorkItem {
        .type = WorkItem::Type::CreateDirectory,
        .source = {},
        .destination = String::formatted("{}/{}", destination, LexicalPath(source).basename()),
        .size = 0,
    });

    Core::DirIterator dt(source, Core::DirIterator::SkipParentAndBaseDir);
    while (dt.has_next()) {
        auto name = dt.next_path();
        if (!collect_work_items(
                String::formatted("{}/{}", source, name),
                String::formatted("{}/{}", destination, LexicalPath(source).basename()),
                items)) {
            return false;
        }
    }

    return true;
}

int perform_copy(const String& source, const String& destination)
{
    Vector<WorkItem> items;

    if (!collect_work_items(source, destination, items))
        return 1;

    off_t total_bytes_to_copy = 0;
    for (auto& item : items)
        total_bytes_to_copy += item.size;

    off_t bytes_copied_so_far = 0;

    for (size_t i = 0; i < items.size(); ++i) {
        auto& item = items[i];
        auto print_progress = [&] {
            outln("PROGRESS {} {} {} {} {}", i, items.size(), bytes_copied_so_far, total_bytes_to_copy, item.source);
        };
        if (item.type == WorkItem::Type::CreateDirectory) {
            outln("MKDIR {}", item.destination);
            if (mkdir(item.destination.characters(), 0755) < 0 && errno != EEXIST) {
                perror("mkdir");
                return 1;
            }
            continue;
        }
        VERIFY(item.type == WorkItem::Type::CopyFile);
        auto source_file_or_error = Core::File::open(item.source, Core::File::ReadOnly);
        if (source_file_or_error.is_error()) {
            warnln("Failed to open {} for reading: {}", item.source, source_file_or_error.error());
            return 1;
        }
        auto destination_file_or_error = Core::File::open(item.destination, (Core::IODevice::OpenMode)(Core::File::WriteOnly | Core::File::Truncate));
        if (destination_file_or_error.is_error()) {
            warnln("Failed to open {} for write: {}", item.destination, destination_file_or_error.error());
            return 1;
        }
        auto& source_file = *source_file_or_error.value();
        auto& destination_file = *destination_file_or_error.value();

        while (true) {
            auto buffer = source_file.read(65536);
            if (buffer.is_null())
                break;
            if (!destination_file.write(buffer)) {
                warnln("Failed to write to destination file: {}", destination_file.error_string());
                return 1;
            }
            bytes_copied_so_far += buffer.size();
            print_progress();
            // FIXME: Remove this once the kernel is smart enough to schedule other threads
            //        while we're doing heavy I/O. Right now, copying a large file will totally
            //        starve the rest of the system.
            sched_yield();
        }
        print_progress();
    }

    outln("FINISH");
    return 0;
}
