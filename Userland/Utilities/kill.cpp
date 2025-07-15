/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static void print_usage_and_exit()
{
    warnln("usage: kill [-signal] <PID>");
    exit(1);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{

    TRY(Core::System::pledge("stdio proc"));

    int argc = arguments.argc;
    auto strings = arguments.strings;

    if (argc == 2 && strings[1] == "-l") {
        size_t valid_signal_count = 0;
        char signal_name[SIG2STR_MAX] = { 0 };
        for (size_t i = 1; i < NSIG; ++i) {
            if (valid_signal_count && valid_signal_count % 5 == 0)
                outln("");
            if (sig2str(i, signal_name) != 0)
                continue;
            valid_signal_count++;
            out("{:2}) {:10}", i, signal_name);
        }
        outln("");
        return 0;
    }

    if (argc != 2 && argc != 3)
        print_usage_and_exit();
    int signum = SIGTERM;
    int pid_argi = 1;
    if (argc == 3) {
        pid_argi = 2;
        if (strings[1][0] != '-' || strings[1].length() <= 1)
            print_usage_and_exit();

        ByteString signal_name = strings[1].substring_view(1).starts_with("SIG"sv) ? strings[1].substring_view(4) : strings[1].substring_view(1);
        if (str2sig(signal_name.characters(), &signum) != 0) {
            warnln("'{}' is not a valid signal name or number", &strings[1][1]);
            return 2;
        }
    }
    auto pid_opt = strings[pid_argi].to_number<pid_t>();
    if (!pid_opt.has_value()) {
        warnln("'{}' is not a valid PID", strings[pid_argi]);
        return 3;
    }
    pid_t pid = pid_opt.value();

    TRY(Core::System::kill(pid, signum));
    return 0;
}
