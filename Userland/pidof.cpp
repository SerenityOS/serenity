/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CProcessStatisticsReader.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static int pid_of(const String& process_name, bool single_shot, bool omit_pid, pid_t pid)
{
    bool displayed_at_least_one = false;

    auto processes = CProcessStatisticsReader().get_all();

    for (auto& it : processes) {
        if (it.value.name == process_name) {
            if (!omit_pid || it.value.pid != pid) {
                printf("%d ", it.value.pid);
                displayed_at_least_one = true;

                if (single_shot)
                    break;
            }
        }
    }

    if (displayed_at_least_one)
        printf("\n");

    return 0;
}

int main(int argc, char** argv)
{
    bool single_shot = false;
    const char* omit_pid_value = nullptr;
    const char* process_name = nullptr;

    CArgsParser args_parser;
    args_parser.add_option(single_shot, "Only return one pid", nullptr, 's');
    args_parser.add_option(omit_pid_value, "Omit the given PID, or the parent process if the special value %PPID is passed", nullptr, 'o', "pid");
    args_parser.add_positional_argument(process_name, "Process name to search for", "process-name");

    args_parser.parse(argc, argv);

    pid_t pid_to_omit = 0;
    if (omit_pid_value) {
        bool ok = true;
        if (!strcmp(omit_pid_value, "%PPID"))
            pid_to_omit = getppid();
        else
            pid_to_omit = StringView(omit_pid_value).to_uint(ok);
        if (!ok) {
            fprintf(stderr, "Invalid value for -o\n");
            args_parser.print_usage(stderr, argv[0]);
            return 1;
        }
    }
    return pid_of(process_name, single_shot, omit_pid_value != nullptr, pid_to_omit);
}
