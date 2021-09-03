/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/String.h>
#include <YAK/StringBuilder.h>
#include <YAK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <sys/utsname.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

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
    args_parser.parse(argc, argv);

    if (flag_all)
        flag_system = flag_node = flag_release = flag_machine = true;

    if (!flag_system && !flag_node && !flag_release && !flag_machine)
        flag_system = true;

    utsname uts;
    int rc = uname(&uts);
    if (rc < 0) {
        perror("uname() failed");
        return 0;
    }

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
