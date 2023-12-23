/*
 * Copyright (c) 2020, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <stdio.h>

struct OpenFile {
    int fd;
    int pid;
    ByteString type;
    ByteString name;
    ByteString state;
    ByteString full_name;
};

static bool parse_name(StringView name, OpenFile& file)
{
    GenericLexer lexer(name);
    auto component1 = lexer.consume_until(':');
    lexer.ignore();

    if (lexer.tell_remaining() == 0) {
        file.name = component1;
        return true;
    } else {
        file.type = component1;
        auto component2 = lexer.consume_while([](char c) { return is_ascii_printable(c) && c != '('; });
        lexer.ignore_while(is_ascii_space);
        file.name = component2;

        if (lexer.tell_remaining() == 0) {
            return true;
        } else {
            if (!lexer.consume_specific('(')) {
                dbgln("parse_name: expected (");
                return false;
            }

            auto component3 = lexer.consume_until(')');
            lexer.ignore();
            if (lexer.tell_remaining() != 0) {
                dbgln("parse_name: expected EOF");
                return false;
            }

            file.state = component3;
            return true;
        }
    }
}

static Vector<OpenFile> get_open_files_by_pid(pid_t pid)
{
    auto file = Core::File::open(ByteString::formatted("/proc/{}/fds", pid), Core::File::OpenMode::Read);
    if (file.is_error()) {
        outln("lsof: PID {}: {}", pid, file.error());
        return Vector<OpenFile>();
    }
    auto data = file.value()->read_until_eof();
    if (data.is_error()) {
        outln("lsof: PID {}: {}", pid, data.error());
        return {};
    }

    auto json_or_error = JsonValue::from_string(data.value());
    if (json_or_error.is_error()) {
        outln("lsof: {}", json_or_error.error());
        return Vector<OpenFile>();
    }
    auto json = json_or_error.release_value();

    Vector<OpenFile> files;
    json.as_array().for_each([pid, &files](JsonValue const& object) {
        OpenFile open_file;
        open_file.pid = pid;
        open_file.fd = object.as_object().get_integer<int>("fd"sv).value();

        ByteString name = object.as_object().get_byte_string("absolute_path"sv).value_or({});
        VERIFY(parse_name(name, open_file));
        open_file.full_name = name;

        files.append(open_file);
    });
    return files;
}

static void display_entry(OpenFile const& file, Core::ProcessStatistics const& statistics)
{
    outln("{:28} {:>4} {:>4} {:10} {:>4} {}", statistics.name, file.pid, statistics.pgid, statistics.username, file.fd, file.full_name);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath proc"));

    TRY(Core::System::unveil("/proc", "r"));
    // needed by ProcessStatisticsReader::get_all()
    TRY(Core::System::unveil("/sys/kernel/processes", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool arg_all_processes { false };
    int arg_fd { -1 };
    StringView arg_uid;
    int arg_uid_int = -1;
    int arg_pgid { -1 };
    pid_t arg_pid { -1 };
    StringView arg_filename;

    if (arguments.strings.size() == 1)
        arg_all_processes = true;
    else {
        Core::ArgsParser parser;
        parser.set_general_help("List open files of a processes. This can mean actual files in the file system, sockets, pipes, etc.");
        parser.add_option(arg_pid, "Select by PID", nullptr, 'p', "pid");
        parser.add_option(arg_fd, "Select by file descriptor", nullptr, 'd', "fd");
        parser.add_option(arg_uid, "Select by login/UID", nullptr, 'u', "login/UID");
        parser.add_option(arg_pgid, "Select by process group ID", nullptr, 'g', "PGID");
        parser.add_positional_argument(arg_filename, "Filename", "filename", Core::ArgsParser::Required::No);
        parser.parse(arguments);
    }
    {
        // try convert UID to int
        auto arg = ByteString(arg_uid).to_number<int>();
        if (arg.has_value())
            arg_uid_int = arg.value();
    }

    outln("{:28} {:>4} {:>4} {:10} {:>4} {}", "COMMAND", "PID", "PGID", "USER", "FD", "NAME");
    auto all_processes = TRY(Core::ProcessStatisticsReader::get_all());
    if (arg_pid == -1) {
        for (auto& process : all_processes.processes) {
            if (process.pid == 0)
                continue;
            auto open_files = get_open_files_by_pid(process.pid);

            if (open_files.is_empty())
                continue;

            for (auto& file : open_files) {
                if ((arg_all_processes)
                    || (arg_fd != -1 && file.fd == arg_fd)
                    || (arg_uid_int != -1 && (int)process.uid == arg_uid_int)
                    || (!arg_uid.is_empty() && process.username == arg_uid)
                    || (arg_pgid != -1 && (int)process.pgid == arg_pgid)
                    || (!arg_filename.is_empty() && file.name == arg_filename))
                    display_entry(file, process);
            }
        }
    } else {
        auto open_files = get_open_files_by_pid(arg_pid);

        if (open_files.is_empty())
            return 0;

        for (auto& file : open_files) {
            display_entry(file, *all_processes.processes.find_if([&](auto& entry) { return entry.pid == arg_pid; }));
        }
    }

    return 0;
}
