/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Syscall.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView new_jail_name;
    Core::ArgsParser args_parser;
    bool pid_isolation = false;
    bool unveil_isolation = false;
    Vector<DeprecatedString> unveil_paths;
    args_parser.add_positional_argument(new_jail_name, "New jail name", "jail name");
    args_parser.add_option(pid_isolation, "Use PID-isolation (as a custom isolation option)", "pid-isolation", 'p');
    args_parser.add_option(unveil_isolation, "Use unveil-isolation (as a custom isolation option)", "unveil-isolation", 'l');
    args_parser.add_option(unveil_paths, "Path to unveil [permissions,path]", "path", 'u', "");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio jail"));

    if (new_jail_name.is_null() || new_jail_name.is_empty())
        return Error::from_string_view("Can't create a jail with empty name."sv);

    JailIsolationFlags flags = JailIsolationFlags::None;
    if (pid_isolation)
        flags |= JailIsolationFlags::PIDIsolation;
    if (unveil_isolation)
        flags |= JailIsolationFlags::FileSystemUnveilIsolation;

    u64 jail_index = TRY(Core::System::create_jail(new_jail_name, flags));
    if (unveil_isolation) {
        for (auto& path : unveil_paths) {
            auto parts = path.split_view(',');
            if (parts.size() != 2)
                return Error::from_string_literal("Unveil path being specified is invalid.");
            Syscall::StringArgument path_argument { parts[1].characters_without_null_termination(), parts[1].length() };
            Syscall::StringArgument permissions { parts[0].characters_without_null_termination(), parts[0].length() };
            TRY(Core::System::configure_jail(jail_index, static_cast<u64>(JailConfigureRequest::UnveilPath), (u64)&path_argument, (u64)&permissions));
        }
        TRY(Core::System::configure_jail(jail_index, static_cast<u64>(JailConfigureRequest::LockUnveil), 0, 0));
    }

    return 0;
}
