/*
 * Copyright (c) 2020, Maciej Zygmanowski <sppmacd@pm.me>
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

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/ProcessStatisticsReader.h>
#include <stdio.h>

struct OpenFile {
    int fd;
    int pid;
    String name;
};

static Vector<OpenFile> get_open_files_by_pid(pid_t pid)
{
    auto file = Core::File::open(String::format("/proc/%d/fds", pid), Core::IODevice::OpenMode::ReadOnly);
    if (file.is_error()) {
        printf("lsof: PID %d: %s\n", pid, file.error().characters());
        return Vector<OpenFile>();
    }
    auto data = file.value()->read_all();

    JsonParser parser(data);
    auto result = parser.parse();

    if (!result.has_value()) {
        ASSERT_NOT_REACHED();
    }

    Vector<OpenFile> files;
    result.value().as_array().for_each([pid, &files](const JsonValue& object) {
        OpenFile open_file;
        open_file.pid = pid;
        open_file.fd = object.as_object().get("fd").to_int();
        open_file.name = object.as_object().get("absolute_path").to_string();
        files.append(open_file);
    });
    return files;
}

static void display_entry(const OpenFile& file, const Core::ProcessStatistics& statistics)
{
    printf("%-28s %4d %4d %-10s %4d %s\n", statistics.name.characters(), file.pid, statistics.pgid, statistics.username.characters(), file.fd, file.name.characters());
}

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil /proc");
        return 1;
    }

    // needed by ProcessStatisticsReader::get_all()
    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil /etc/passwd");
        return 1;
    }

    unveil(nullptr, nullptr);

    bool arg_all_processes { false };
    int arg_fd { -1 };
    const char* arg_uid { nullptr };
    int arg_uid_int = -1;
    int arg_pgid { -1 };
    pid_t arg_pid { -1 };

    Core::ArgsParser parser;
    if (argc == 1)
        arg_all_processes = true;
    else {
        parser.add_option(arg_pid, "Select PID", nullptr, 'p', "pid");
        parser.add_option(arg_fd, "Select file descriptor", nullptr, 'd', "fd");
        parser.add_option(arg_uid, "Select login/UID", nullptr, 'u', "login/UID");
        parser.add_option(arg_pgid, "Select process group ID", nullptr, 'g', "PGID");
        parser.parse(argc, argv);
    }
    {
        // try convert UID to int
        auto arg = String(arg_uid).to_int();
        if (arg.has_value())
            arg_uid_int = arg.value();
    }

    printf("%-28s %4s %4s %-10s %4s %s\n", "COMMAND", "PID", "PGID", "USER", "FD", "NAME");
    if (arg_pid == -1) {
        auto processes = Core::ProcessStatisticsReader::get_all();
        for (auto process : processes) {
            if (process.key == 0)
                continue;
            auto open_files = get_open_files_by_pid(process.key);

            if (open_files.is_empty())
                continue;

            for (auto file : open_files) {
                if ((arg_all_processes)
                    || (arg_fd != -1 && file.fd == arg_fd)
                    || (arg_uid_int != -1 && (int)process.value.uid == arg_uid_int)
                    || (arg_uid != nullptr && process.value.username == arg_uid)
                    || (arg_pgid != -1 && (int)process.value.pgid == arg_pgid))
                    display_entry(file, process.value);
            }
        }
    } else {
        auto processes = Core::ProcessStatisticsReader::get_all();
        auto open_files = get_open_files_by_pid(arg_pid);

        if (open_files.is_empty())
            return 0;

        for (auto file : open_files) {
            display_entry(file, processes.get(arg_pid).value());
        }
    }

    return 0;
}
