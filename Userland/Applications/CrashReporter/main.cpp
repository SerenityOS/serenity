/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/URL.h>
#include <Applications/CrashReporter/CrashReporterWindowGML.h>
#include <LibC/serenity.h>
#include <LibC/spawn.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCoredump/Backtrace.h>
#include <LibCoredump/Reader.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibELF/Core.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <string.h>
#include <unistd.h>

struct TitleAndText {
    String title;
    String text;
};

static TitleAndText build_backtrace(Coredump::Reader const& coredump, ELF::Core::ThreadInfo const& thread_info, size_t thread_index)
{
    Coredump::Backtrace backtrace(coredump, thread_info, [&](size_t, size_t) {});

    auto metadata = coredump.metadata();

    StringBuilder builder;

    auto prepend_metadata = [&](auto& key, StringView fmt) {
        auto maybe_value = metadata.get(key);
        if (!maybe_value.has_value() || maybe_value.value().is_empty())
            return;
        builder.appendff(fmt, maybe_value.value());
        builder.append('\n');
        builder.append('\n');
    };

    if (metadata.contains("assertion"))
        prepend_metadata("assertion", "ASSERTION FAILED: {}");
    else if (metadata.contains("pledge_violation"))
        prepend_metadata("pledge_violation", "Has not pledged {}");

    auto fault_address = metadata.get("fault_address");
    auto fault_type = metadata.get("fault_type");
    auto fault_access = metadata.get("fault_access");
    if (fault_address.has_value() && fault_type.has_value() && fault_access.has_value()) {
        builder.appendff("{} fault on {} at address {}\n\n", fault_type.value(), fault_access.value(), fault_address.value());
    }

    auto first_entry = true;
    for (auto& entry : backtrace.entries()) {
        if (first_entry)
            first_entry = false;
        else
            builder.append('\n');
        builder.append(entry.to_string());
    }

    dbgln("--- Backtrace for thread #{} (TID {}) ---", thread_index, thread_info.tid);
    for (auto& entry : backtrace.entries()) {
        dbgln("{}", entry.to_string(true));
    }

    return {
        String::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.build()
    };
}

static void unlink_coredump(StringView const& coredump_path)
{
    if (Core::File::remove(coredump_path, Core::File::RecursionMode::Disallowed, false).is_error())
        dbgln("Failed deleting coredump file");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd cpath rpath unix proc exec"));

    const char* coredump_path = nullptr;
    bool unlink_on_exit = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show information from an application crash coredump.");
    args_parser.add_positional_argument(coredump_path, "Coredump path", "coredump-path");
    args_parser.add_option(unlink_on_exit, "Delete the coredump after its parsed", "unlink", 0);
    args_parser.parse(arguments);

    String executable_path;

    {
        auto coredump = Coredump::Reader::create(coredump_path);
        if (!coredump) {
            warnln("Could not open coredump '{}'", coredump_path);
            return 1;
        }

        size_t thread_index = 0;
        coredump->for_each_thread_info([&](auto& thread_info) {
            [[maybe_unused]] auto bt = build_backtrace(*coredump, thread_info, thread_index);
            ++thread_index;
            return IterationDecision::Continue;
        });

        if (unlink_on_exit)
            unlink_coredump(coredump_path);
    }

    return 0;
}
