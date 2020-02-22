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

#include <LibCore/ArgsParser.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;

    const char* pid_argument = nullptr;
    const char* cmd_argument = nullptr;
    bool enable = false;
    bool disable = false;

    args_parser.add_option(pid_argument, "Target PID", nullptr, 'p', "PID");
    args_parser.add_option(enable, "Enable", nullptr, 'e');
    args_parser.add_option(disable, "Disable", nullptr, 'd');
    args_parser.add_option(cmd_argument, "Command", nullptr, 'c', "command");

    args_parser.parse(argc, argv);

    if (!pid_argument && !cmd_argument) {
        args_parser.print_usage(stdout, argv[0]);
        return 0;
    }

    if (pid_argument) {
        if (!(enable ^ disable)) {
            fprintf(stderr, "-p <PID> requires -e xor -d.\n");
            return 1;
        }

        pid_t pid = atoi(pid_argument);

        if (enable) {
            if (profiling_enable(pid) < 0) {
                perror("profiling_enable");
                return 1;
            }
            return 0;
        }

        if (profiling_disable(pid) < 0) {
            perror("profiling_disable");
            return 1;
        }

        return 0;
    }

    auto cmd_parts = String(cmd_argument).split(' ');
    Vector<const char*> cmd_argv;

    for (auto& part : cmd_parts)
        cmd_argv.append(part.characters());

    cmd_argv.append(nullptr);

    dbg() << "Enabling profiling for PID " << getpid();
    profiling_enable(getpid());
    if (execvp(cmd_argv[0], const_cast<char**>(cmd_argv.data())) < 0) {
        perror("execv");
        return 1;
    }

    return 0;
}
