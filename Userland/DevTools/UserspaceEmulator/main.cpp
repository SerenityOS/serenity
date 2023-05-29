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
#include <LibCore/Process.h>
#include <LibFileSystem/FileSystem.h>
#include <fcntl.h>
#include <pthread.h>
#include <serenity.h>
#include <string.h>

bool g_report_to_debug = false;

int main(int argc, char** argv, char** env)
{
    Vector<StringView> arguments;
    bool pause_on_startup { false };
    DeprecatedString profile_dump_path;
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

    auto executable_path_or_error = arguments[0].contains('/')
        ? FileSystem::real_path(arguments[0])
        : Core::System::resolve_executable_from_environment(arguments[0]);
    if (executable_path_or_error.is_error()) {
        reportln("Cannot find executable for '{}'."sv, arguments[0]);
        return 1;
    }
    auto executable_path = executable_path_or_error.release_value().to_deprecated_string();

    if (dump_profile && profile_dump_path.is_empty())
        profile_dump_path = DeprecatedString::formatted("{}.{}.profile", LexicalPath(executable_path).basename(), getpid());

    OwnPtr<Stream> profile_stream;
    OwnPtr<Vector<NonnullOwnPtr<DeprecatedString>>> profile_strings;
    OwnPtr<Vector<int>> profile_string_id_map;

    if (dump_profile) {
        auto profile_stream_or_error = Core::File::open(profile_dump_path, Core::File::OpenMode::Write);
        if (profile_stream_or_error.is_error()) {
            warnln("Failed to open '{}' for writing: {}", profile_dump_path, profile_stream_or_error.error());
            return 1;
        }
        profile_stream = profile_stream_or_error.release_value();
        profile_strings = make<Vector<NonnullOwnPtr<DeprecatedString>>>();
        profile_string_id_map = make<Vector<int>>();

        profile_stream->write_until_depleted(R"({"events":[)"sv.bytes()).release_value_but_fixme_should_propagate_errors();
        timeval tv {};
        gettimeofday(&tv, nullptr);
        profile_stream->write_until_depleted(
                          DeprecatedString::formatted(
                              R"~({{"type": "process_create", "parent_pid": 1, "executable": "{}", "pid": {}, "tid": {}, "timestamp": {}, "lost_samples": 0, "stack": []}})~",
                              executable_path, getpid(), gettid(), tv.tv_sec * 1000 + tv.tv_usec / 1000)
                              .bytes())
            .release_value_but_fixme_should_propagate_errors();
    }

    Vector<DeprecatedString> environment;
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
    builder.append("(UE) "sv);
    builder.append(LexicalPath::basename(arguments[0]));
    if (auto result = Core::Process::set_name(builder.string_view(), Core::Process::SetThreadName::Yes); result.is_error()) {
        reportln("Core::Process::set_name: {}"sv, result.error());
        return 1;
    }

    if (pause_on_startup)
        emulator.pause();

    int rc = emulator.exec();

    if (dump_profile) {
        emulator.profile_stream().write_until_depleted("], \"strings\": ["sv.bytes()).release_value_but_fixme_should_propagate_errors();
        if (emulator.profiler_strings().size()) {
            for (size_t i = 0; i < emulator.profiler_strings().size() - 1; ++i)
                emulator.profile_stream().write_until_depleted(DeprecatedString::formatted("\"{}\", ", emulator.profiler_strings().at(i)).bytes()).release_value_but_fixme_should_propagate_errors();
            emulator.profile_stream().write_until_depleted(DeprecatedString::formatted("\"{}\"", emulator.profiler_strings().last()).bytes()).release_value_but_fixme_should_propagate_errors();
        }
        emulator.profile_stream().write_until_depleted("]}"sv.bytes()).release_value_but_fixme_should_propagate_errors();
    }
    return rc;
}
