/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Fabian Dellwing <fabian@dellwing.net>
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

static ErrorOr<off_t> find_seek_pos(Core::File& file, int wanted_lines, bool start_from_end)
{
    int lines = 0;

    if (start_from_end) {
        off_t pos = TRY(file.seek(0, SeekMode::FromEndPosition));
        off_t end = pos;
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

    off_t file_size = TRY(file.size());
    off_t pos = 0;

    for (; pos < file_size; pos++) {
        auto ch = TRY(file.read_value<u8>());
        if (ch == '\n') {
            lines++;
            if (lines == wanted_lines)
                break;
        }
    }

    return pos + 1;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool follow = false;
    size_t wanted_line_count = DEFAULT_LINE_COUNT;
    bool start_from_end = true;
    StringView file;
    size_t file_size;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Print the end ('tail') of a file.");
    args_parser.add_option(follow, "Output data as it is written to the file", "follow", 'f');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "output the last NUM lines, instead of the last 10;"
                       " or use -n +NUM to output starting with line NUM",
        .long_name = "lines",
        .short_name = 'n',
        .value_name = "[+]NUM",
        .accept_value = [&](StringView lines) -> ErrorOr<bool> {
            Optional<size_t> value;
            if (lines.starts_with('+')) {
                value = lines.substring_view(1, lines.length() - 1).to_number<size_t>();
                start_from_end = false;
            } else {
                value = lines.to_number<size_t>();
            }
            if (!value.has_value()) {
                warnln("Invalid number: {}", lines);
                return false;
            }
            wanted_line_count = value.value();
            return true;
        },
    });
    args_parser.add_positional_argument(file, "File path", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto f = TRY(Core::File::open_file_or_standard_stream(file, Core::File::OpenMode::Read));
    if (!follow)
        TRY(Core::System::pledge("stdio"));

    auto file_is_seekable = !f->seek(0, SeekMode::SetPosition).is_error();
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

            if (!start_from_end) {
                if (wanted_line_count > line_count) {
                    continue;
                }
                if (wanted_line_count == 0) {
                    out("{}", StringView { bytes });
                    continue;
                }
                for (size_t i = 0; i < bytes.size(); i++) {
                    auto ch = bytes.at(i);
                    if (ch == '\n') {
                        line_index++;
                    }
                    if (line_index >= wanted_line_count)
                        line.append(ch);
                }
                out("{}", line.to_byte_string().substring_view(1, line.length() - 1));
                continue;
            }

            for (size_t i = 0; i < bytes.size(); i++) {
                auto ch = bytes.at(i);
                line.append(ch);
                if (ch == '\n' || i == bytes.size() - 1) {
                    if (wanted_line_count > line_count || line_index >= line_count - wanted_line_count)
                        out("{}", line.to_byte_string());
                    line_index++;
                    line.clear();
                }
            }

            // Since we can't have FileWatchers on the standard input either,
            // we just loop forever if the -f option was passed.
        } while (follow);
        return 0;
    }

    auto pos = TRY(find_seek_pos(*f, wanted_line_count, start_from_end));
    TRY(tail_from_pos(*f, pos));

    if (follow) {
        file_size = TRY(f->size());
        TRY(f->seek(0, SeekMode::FromEndPosition));

        Core::EventLoop event_loop;
        auto watcher = TRY(Core::FileWatcher::create());
        watcher->on_change = [&](Core::FileWatcherEvent const& event) {
            if (event.type == Core::FileWatcherEvent::Type::ContentModified) {
                auto maybe_current_size = f->size();
                if (maybe_current_size.is_error()) {
                    auto error = maybe_current_size.release_error();
                    warnln(error.string_literal());
                    event_loop.quit(error.code());
                    return;
                }
                auto current_size = maybe_current_size.value();
                if (current_size < file_size) {
                    warnln("{}: file truncated", event.event_path);
                    auto maybe_seek_error = f->seek(0, SeekMode::SetPosition);
                    if (maybe_seek_error.is_error()) {
                        auto error = maybe_seek_error.release_error();
                        warnln(error.string_literal());
                        event_loop.quit(error.code());
                        return;
                    }
                }
                file_size = current_size;
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
