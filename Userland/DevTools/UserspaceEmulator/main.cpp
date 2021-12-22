/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Emulator.h"
#include <AK/FileStream.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <pthread.h>
#include <serenity.h>
#include <string.h>

bool g_report_to_debug = false;

int main(int argc, char** argv, char** env)
{
    Vector<StringView> arguments;
    bool pause_on_startup { false };
    String profile_dump_path;
    FILE* profile_output_file { nullptr };
    bool enable_roi_mode { false };
    bool dump_profile { false };
    unsigned profile_instruction_interval { 0 };

    Core::ArgsParser parser;
    parser.set_stop_on_first_non_option(true);
    parser.add_option(g_report_to_debug, "Write reports to the debug log", "report-to-debug", 0);
    parser.add_option(pause_on_startup, "Pause on startup", "pause", 'p');
    parser.add_option(dump_profile, "Generate a ProfileViewer-compatible profile", "profile", 0);
    parser.add_option(profile_instruction_interval, "Set the profile instruction capture interval, 128 by default", "profile-interval", 'i', "num_instructions");
    parser.add_option(profile_dump_path, "File path for profile dump", "profile-file", 0, "path");
    parser.add_option(enable_roi_mode, "Enable Region-of-Interest mode for profiling", "roi", 0);

    parser.add_positional_argument(arguments, "Command to emulate", "command");

    parser.parse(argc, argv);

    if (dump_profile && profile_instruction_interval == 0)
        profile_instruction_interval = 128;

    String executable_path;
    if (arguments[0].contains("/"sv))
        executable_path = Core::File::real_path_for(arguments[0]);
    else
        executable_path = Core::find_executable_in_path(arguments[0]);
    if (executable_path.is_empty()) {
        reportln("Cannot find executable for '{}'.", arguments[0]);
        return 1;
    }

    if (dump_profile && profile_dump_path.is_empty())
        profile_dump_path = String::formatted("{}.{}.profile", LexicalPath(executable_path).basename(), getpid());

    OwnPtr<OutputFileStream> profile_stream;
    OwnPtr<NonnullOwnPtrVector<String>> profile_strings;
    OwnPtr<Vector<int>> profile_string_id_map;

    if (dump_profile) {
        profile_output_file = fopen(profile_dump_path.characters(), "w+");
        if (profile_output_file == nullptr) {
            char const* error_string = strerror(errno);
            warnln("Failed to open '{}' for writing: {}", profile_dump_path, error_string);
            return 1;
        }
        profile_stream = make<OutputFileStream>(profile_output_file);
        profile_strings = make<NonnullOwnPtrVector<String>>();
        profile_string_id_map = make<Vector<int>>();

        profile_stream->write_or_error(R"({"events":[)"sv.bytes());
        timeval tv {};
        gettimeofday(&tv, nullptr);
        profile_stream->write_or_error(
            String::formatted(
                R"~({{"type": "process_create", "parent_pid": 1, "executable": "{}", "pid": {}, "tid": {}, "timestamp": {}, "lost_samples": 0, "stack": []}})~",
                executable_path, getpid(), gettid(), tv.tv_sec * 1000 + tv.tv_usec / 1000)
                .bytes());
    }

    Vector<String> environment;
    for (int i = 0; env[i]; ++i) {
        environment.append(env[i]);
    }

    // FIXME: It might be nice to tear down the emulator properly.
    auto& emulator = *new UserspaceEmulator::Emulator(executable_path, arguments, environment);

    emulator.set_profiling_details(dump_profile, profile_instruction_interval, profile_stream, profile_strings, profile_string_id_map);
    emulator.set_in_region_of_interest(!enable_roi_mode);

    if (!emulator.load_elf())
        return 1;

    StringBuilder builder;
    builder.append("(UE) ");
    builder.append(LexicalPath::basename(arguments[0]));
    if (set_process_name(builder.string_view().characters_without_null_termination(), builder.string_view().length()) < 0) {
        perror("set_process_name");
        return 1;
    }
    int rc = pthread_setname_np(pthread_self(), builder.to_string().characters());
    if (rc != 0) {
        reportln("pthread_setname_np: {}", strerror(rc));
        return 1;
    }

    if (pause_on_startup)
        emulator.pause();

    rc = emulator.exec();

    if (dump_profile) {
        emulator.profile_stream().write_or_error(", \"strings\": ["sv.bytes());
        if (emulator.profiler_strings().size()) {
            for (size_t i = 0; i < emulator.profiler_strings().size() - 1; ++i)
                emulator.profile_stream().write_or_error(String::formatted("\"{}\", ", emulator.profiler_strings().at(i)).bytes());
            emulator.profile_stream().write_or_error(String::formatted("\"{}\"", emulator.profiler_strings().last()).bytes());
        }
        emulator.profile_stream().write_or_error("]}"sv.bytes());
    }
    return rc;
}
