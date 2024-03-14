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

#define TRY_OR_REPORT_ERROR(expression)                                                              \
    ({                                                                                               \
        /* Ignore -Wshadow to allow nesting the macro. */                                            \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                             \
            auto&& _temporary_result = (expression));                                                \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        if (_temporary_result.is_error()) [[unlikely]] {                                             \
            warnln("{}", _temporary_result.error().string_literal());                                \
            event_loop.quit(_temporary_result.error().code());                                       \
        }                                                                                            \
        _temporary_result.release_value();                                                           \
    })

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

        if (wanted_lines == 0)
            return pos;

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

    // If we want the first or zeroeth line, we don't need to seek at all.
    if (wanted_lines == 0 || wanted_lines == 1)
        return 0;

    off_t file_size = TRY(file.size());
    off_t pos = 0;

    for (; pos < file_size; pos++) {
        auto ch = TRY(file.read_value<u8>());
        if (ch == '\n') {
            lines++;
            if (lines + 1 == wanted_lines)
                break;
        }
    }

    return pos + 1;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool follow = false;
    size_t wanted_byte_count = 0;
    bool byte_mode = false;
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
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "output the last NUM bytes; or use -c +NUM to"
                       " output starting with byte NUM",
        .long_name = "bytes",
        .short_name = 'c',
        .value_name = "[+]NUM",
        .accept_value = [&](StringView bytes) -> ErrorOr<bool> {
            Optional<size_t> value;
            if (bytes.starts_with('+')) {
                value = bytes.substring_view(1, bytes.length() - 1).to_number<size_t>();
                start_from_end = false;
            } else {
                value = bytes.to_number<size_t>();
            }
            if (!value.has_value()) {
                warnln("Invalid number: {}", bytes);
                return false;
            }
            wanted_byte_count = value.value();
            byte_mode = true;
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

            if (byte_mode) {
                if (start_from_end) {
                    if (wanted_byte_count > bytes.size()) {
                        out("{}", StringView { bytes });
                        continue;
                    }
                    out("{}", StringView { bytes }.substring_view(bytes.size() - wanted_byte_count, wanted_byte_count));
                    continue;
                }

                if (wanted_byte_count > bytes.size())
                    continue;

                if (wanted_byte_count > 0)
                    out("{}", StringView { bytes }.substring_view(wanted_byte_count - 1, bytes.size() - wanted_byte_count + 1));
                else
                    out("{}", StringView { bytes });
                continue;
            }

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

                auto line_string = line.to_byte_string();
                out("{}", line_string.substring_view(1, line.length() - 1));

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

    off_t pos = 0;
    if (byte_mode) {
        auto file_size = TRY(f->size());
        if (wanted_byte_count > file_size)
            wanted_byte_count = file_size;
        if (start_from_end) {
            TRY(f->seek(wanted_byte_count * -1, SeekMode::FromEndPosition));
        } else {
            if (wanted_byte_count > 0 && wanted_byte_count < file_size)
                TRY(f->seek(wanted_byte_count - 1, SeekMode::SetPosition));
            else
                TRY(f->seek(0, SeekMode::FromEndPosition));
        }
        pos = TRY(f->tell());
    } else {
        pos = TRY(find_seek_pos(*f, wanted_line_count, start_from_end));
    }
    TRY(tail_from_pos(*f, pos));

    if (follow) {
        file_size = TRY(f->size());
        TRY(f->seek(0, SeekMode::FromEndPosition));

        Core::EventLoop event_loop;
        auto watcher = TRY(Core::FileWatcher::create());
        watcher->on_change = [&](Core::FileWatcherEvent const& event) {
            if (event.type == Core::FileWatcherEvent::Type::ContentModified) {
                auto current_size = TRY_OR_REPORT_ERROR(f->size());
                if (current_size < file_size) {
                    warnln("{}: file truncated", event.event_path);
                    TRY_OR_REPORT_ERROR(f->seek(0, SeekMode::SetPosition));
                }
                file_size = current_size;
                auto bytes = TRY_OR_REPORT_ERROR(f->read_until_eof());
                out("{}", StringView { bytes });
                TRY_OR_REPORT_ERROR(f->seek(0, SeekMode::FromEndPosition));
            }
        };
        TRY(watcher->add_watch(file, Core::FileWatcherEvent::Type::ContentModified));
        TRY(Core::System::pledge("stdio"));
        return event_loop.exec();
    }
    return 0;
}
