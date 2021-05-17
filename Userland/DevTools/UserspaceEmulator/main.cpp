/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Emulator.h"
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <pthread.h>
#include <serenity.h>
#include <string.h>

bool g_report_to_debug = false;

int main(int argc, char** argv, char** env)
{
    Vector<String> arguments;

    Core::ArgsParser parser;
    parser.add_option(g_report_to_debug, "Write reports to the debug log", "report-to-debug", 0);
    parser.add_positional_argument(arguments, "Command to emulate", "command");
    parser.parse(argc, argv);

    String executable_path;
    if (arguments[0].contains("/"sv))
        executable_path = Core::File::real_path_for(arguments[0]);
    else
        executable_path = Core::find_executable_in_path(arguments[0]);
    if (executable_path.is_empty()) {
        reportln("Cannot find executable for '{}'.", arguments[0]);
        return 1;
    }

    Vector<String> environment;
    for (int i = 0; env[i]; ++i) {
        environment.append(env[i]);
    }

    // FIXME: It might be nice to tear down the emulator properly.
    auto& emulator = *new UserspaceEmulator::Emulator(executable_path, arguments, environment);
    if (!emulator.load_elf())
        return 1;

    StringBuilder builder;
    builder.append("(UE) ");
    builder.append(LexicalPath(arguments[0]).basename());
    if (set_process_name(builder.string_view().characters_without_null_termination(), builder.string_view().length()) < 0) {
        perror("set_process_name");
        return 1;
    }
    int rc = pthread_setname_np(pthread_self(), builder.to_string().characters());
    if (rc != 0) {
        reportln("pthread_setname_np: {}", strerror(rc));
        return 1;
    }
    return emulator.exec();
}
