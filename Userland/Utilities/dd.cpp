/*
 * Copyright (c) 2021, János Tóth <toth-janos@outlook.com>
 * Copyright (c) 2023, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/NumberFormat.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

char const* usage = "usage:\n"
                    "\tdd <options>\n"
                    "options:\n"
                    "\tif=<file>\tinput file (default: stdin)\n"
                    "\tof=<file>\toutput file (default: stdout)\n"
                    "\tbs=<size>\tblocks size may be followed by multiplicate suffixes: k=1024, M=1024*1024, G=1024*1024*1024 (default: 512)\n"
                    "\tcount=<size>\t<size> blocks to copy (default: 0 (until end-of-file))\n"
                    "\tseek=<size>\tskip <size> blocks at start of output (default: 0)\n"
                    "\tskip=<size>\tskip <size> blocks at start of input (default: 0)\n"
                    "\tstatus=<level>\tlevel of output (default: default)\n"
                    "\t\t\tdefault - error messages + final statistics\n"
                    "\t\t\tnone - just error messages\n"
                    "\t\t\tnoxfer - no final statistics\n"
                    "\t--help\t\tshows this text\n";

enum Status {
    Default,
    None,
    Noxfer
};

struct {
    Status status = Default;
    size_t total_bytes_copied = 0;
    size_t total_blocks_in = 0, partial_blocks_in = 0;
    size_t total_blocks_out = 0, partial_blocks_out = 0;
    Core::ElapsedTimer timer { Core::TimerType::Precise };
} statistics;

static void closing_statistics()
{
    if (statistics.status == None)
        return;
    warnln("{}+{} blocks in", statistics.total_blocks_in, statistics.partial_blocks_in);
    warnln("{}+{} blocks out", statistics.total_blocks_out, statistics.partial_blocks_out);
    if (statistics.status != Noxfer) {
        auto elapsed_time = statistics.timer.elapsed_time();
        String copy_speed = "INF B/s"_string;
        if (!elapsed_time.is_zero()) {
            auto speed = statistics.total_bytes_copied * 1000 / elapsed_time.to_milliseconds();
            copy_speed = human_readable_quantity(speed, AK::HumanReadableBasedOn::Base2, "B/s"sv);
        }
        warnln("{} bytes copied ({}), {} ms, {}", statistics.total_bytes_copied, human_readable_size(statistics.total_bytes_copied), elapsed_time.to_milliseconds(), copy_speed);
    }
}

static StringView split_at_equals(StringView argument)
{
    auto values = argument.split_view('=', SplitBehavior::KeepEmpty);
    if (values.size() < 2) {
        warnln("Unable to parse: {}", argument);
        return {};
    } else {
        return values[1];
    }
}

static int handle_io_file_arguments(int& fd, int flags, StringView argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    fd = open(value.to_byte_string().characters(), flags, 0666);
    if (fd == -1) {
        warnln("Unable to open: {}", value);
        return -1;
    } else {
        return 0;
    }
}

static int handle_size_arguments(size_t& numeric_value, StringView argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    unsigned suffix_multiplier = 1;
    auto suffix = value[value.length() - 1];
    switch (to_ascii_lowercase(suffix)) {
    case 'k':
        suffix_multiplier = KiB;
        value = value.substring_view(0, value.length() - 1);
        break;
    case 'm':
        suffix_multiplier = MiB;
        value = value.substring_view(0, value.length() - 1);
        break;
    case 'g':
        suffix_multiplier = GiB;
        value = value.substring_view(0, value.length() - 1);
        break;
    }

    Optional<unsigned> numeric_optional = value.to_number<unsigned>();
    if (!numeric_optional.has_value()) {
        warnln("Invalid size-value: {}", value);
        return -1;
    }

    numeric_value = numeric_optional.value() * suffix_multiplier;
    if (numeric_value < 1) {
        warnln("Invalid size-value: {}", numeric_value);
        return -1;
    } else {
        return 0;
    }
}

static int handle_status_arguments(Status& status, StringView argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    if (value == "default") {
        status = Default;
        return 0;
    } else if (value == "noxfer") {
        status = Noxfer;
        return 0;
    } else if (value == "none") {
        status = None;
        return 0;
    } else {
        warnln("Unknown status: {}", value);
        return -1;
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int input_fd = 0;
    int input_flags = O_RDONLY;
    int output_fd = 1;
    int output_flags = O_CREAT | O_WRONLY | O_TRUNC;
    size_t block_size = 512;
    size_t count = 0;
    size_t skip = 0;
    size_t seek = 0;

    uint8_t* buffer = nullptr;
    ssize_t nread = 0, nwritten = 0;

    for (size_t a = 1; a < arguments.strings.size(); a++) {
        auto argument = arguments.strings[a];

        if (argument == "--help") {
            out("{}", usage);
            return 0;
        } else if (argument.starts_with("if="sv)) {
            if (handle_io_file_arguments(input_fd, input_flags, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("of="sv)) {
            if (handle_io_file_arguments(output_fd, output_flags, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("bs="sv)) {
            if (handle_size_arguments(block_size, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("count="sv)) {
            if (handle_size_arguments(count, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("seek="sv)) {
            if (handle_size_arguments(seek, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("skip="sv)) {
            if (handle_size_arguments(skip, argument) < 0) {
                return 1;
            }
        } else if (argument.starts_with("status="sv)) {
            if (handle_status_arguments(statistics.status, argument) < 0) {
                return 1;
            }
        } else {
            warn("{}", usage);
            return 1;
        }
    }

    if ((buffer = (uint8_t*)malloc(block_size)) == nullptr) {
        warnln("Unable to allocate {} bytes for the buffer.", block_size);
        return -1;
    }

    if (seek > 0) {
        if (lseek(output_fd, seek * block_size, SEEK_SET) < 0) {
            warnln("Unable to seek {} bytes.", seek * block_size);
            return -1;
        }
    }

    TRY(Core::System::signal(SIGINT, [](int status) {
        closing_statistics();
        exit(status);
    }));

    statistics.timer.start();

    while (1) {
        nread = read(input_fd, buffer, block_size);
        if (nread < 0) {
            warnln("Cannot read from the input.");
            break;
        } else if (nread == 0) {
            break;
        } else {
            if ((size_t)nread != block_size) {
                statistics.partial_blocks_in++;
            } else {
                statistics.total_blocks_in++;
            }

            if (statistics.partial_blocks_in + statistics.total_blocks_in <= skip) {
                continue;
            }

            nwritten = write(output_fd, buffer, nread);
            if (nwritten < 0) {
                warnln("Cannot write to the output.");
                break;
            } else if (nwritten == 0) {
                break;
            } else {
                if ((size_t)nwritten < block_size) {
                    statistics.partial_blocks_out++;
                } else {
                    statistics.total_blocks_out++;
                }

                statistics.total_bytes_copied += nwritten;

                if (count > 0 && (statistics.partial_blocks_out + statistics.total_blocks_out) >= count) {
                    break;
                }
            }
        }
    }

    closing_statistics();

    free(buffer);

    if (input_fd != 0) {
        close(input_fd);
    }

    if (output_fd != 1) {
        close(output_fd);
    }

    return 0;
}
