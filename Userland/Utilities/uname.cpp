/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <sys/utsname.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    bool flag_system = false;
    bool flag_node = false;
    bool flag_release = false;
    bool flag_machine = false;
    bool flag_all = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_system, "Print the system name (default)", nullptr, 's');
    args_parser.add_option(flag_node, "Print the node name", nullptr, 'n');
    args_parser.add_option(flag_release, "Print the system release", nullptr, 'r');
    args_parser.add_option(flag_machine, "Print the machine hardware name", nullptr, 'm');
    args_parser.add_option(flag_all, "Print all information (same as -snrm)", nullptr, 'a');
    args_parser.parse(arguments);

    if (flag_all)
        flag_system = flag_node = flag_release = flag_machine = true;

    if (!flag_system && !flag_node && !flag_release && !flag_machine)
        flag_system = true;

    utsname uts = TRY(Core::System::uname());

    Vector<String> parts;
    if (flag_system)
        parts.append(uts.sysname);
    if (flag_node)
        parts.append(uts.nodename);
    if (flag_release)
        parts.append(uts.release);
    if (flag_machine)
        parts.append(uts.machine);
    StringBuilder builder;
    builder.join(' ', parts);
    puts(builder.to_string().characters());
    return 0;
}
