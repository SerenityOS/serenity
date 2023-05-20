/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/System.h>

#define DEFAULT_LINE_COUNT 10

static ErrorOr<void> tail_from_pos(Core::File& file, off_t startline)
{
    TRY(file.seek(startline, SeekMode::SetPosition));
    auto buffer = TRY(file.read_until_eof());
    out("{}", StringView { buffer });
    return {};
}

static ErrorOr<off_t> find_seek_pos(Core::File& file, int wanted_lines)
{
    // Rather than reading the whole file, start at the end and work backwards,
    // stopping when we've found the number of lines we want.
    off_t pos = TRY(file.seek(0, SeekMode::FromEndPosition));

    off_t end = pos;
    int lines = 0;

    for (; pos >= 1; pos--) {
        TRY(file.seek(pos - 1, SeekMode::SetPosition));

        auto ch = TRY(file.read_value<u8>());
        if (ch == '\n' && (end - pos) > 0) {
            lines++;
            if (lines == wanted_lines)
                break;
        }
    }

    return pos;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool follow = false;
    size_t wanted_line_count = DEFAULT_LINE_COUNT;
    StringView file;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print the end ('tail') of a file.");
    args_parser.add_option(follow, "Output data as it is written to the file", "follow", 'f');
    args_parser.add_option(wanted_line_count, "Fetch the specified number of lines", "lines", 'n', "number");
    args_parser.add_positional_argument(file, "File path", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto f = TRY(Core::File::open_file_or_standard_stream(file, Core::File::OpenMode::Read));
    if (!follow)
        TRY(Core::System::pledge("stdio"));

    auto file_is_seekable = !f->tell().is_error();
    if (!file_is_seekable) {
        do {
            // FIXME: If f is the standard input, f->read_all() does not block
            // anymore after sending EOF (^D), despite f->is_open() returning true.
            auto buffer = TRY(f->read_until_eof(PAGE_SIZE));
            auto line_count = StringView(buffer).count("\n"sv);
            auto bytes = buffer.bytes();
            if (bytes.size() > 0 && bytes.last() != '\n')
                line_count++;

            size_t line_index = 0;
            StringBuilder line;

            if (!line_count && wanted_line_count) {
                out("{}", StringView { bytes });
                continue;
            }

            for (size_t i = 0; i < bytes.size(); i++) {
                auto ch = bytes.at(i);
                line.append(ch);
                if (ch == '\n' || i == bytes.size() - 1) {
                    if (wanted_line_count > line_count || line_index >= line_count - wanted_line_count)
                        out("{}", line.to_deprecated_string());
                    line_index++;
                    line.clear();
                }
            }

            // Since we can't have FileWatchers on the standard input either,
            // we just loop forever if the -f option was passed.
        } while (follow);
        return 0;
    }

    auto pos = TRY(find_seek_pos(*f, wanted_line_count));
    TRY(tail_from_pos(*f, pos));

    if (follow) {
        TRY(f->seek(0, SeekMode::FromEndPosition));

        Core::EventLoop event_loop;
        auto watcher = TRY(Core::FileWatcher::create());
        watcher->on_change = [&](Core::FileWatcherEvent const& event) {
            if (event.type == Core::FileWatcherEvent::Type::ContentModified) {
                auto buffer_or_error = f->read_until_eof();
                if (buffer_or_error.is_error()) {
                    auto error = buffer_or_error.release_error();
                    warnln(error.string_literal());
                    event_loop.quit(error.code());
                    return;
                }
                auto bytes = buffer_or_error.value().bytes();
                out("{}", StringView { bytes });

                auto potential_error = f->seek(0, SeekMode::FromEndPosition);
                if (potential_error.is_error()) {
                    auto error = potential_error.release_error();
                    warnln(error.string_literal());
                    event_loop.quit(error.code());
                    return;
                }
            }
        };
        TRY(watcher->add_watch(file, Core::FileWatcherEvent::Type::ContentModified));
        TRY(Core::System::pledge("stdio"));
        return event_loop.exec();
    }
    return 0;
}
