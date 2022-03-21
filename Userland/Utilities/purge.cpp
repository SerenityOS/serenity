/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int mode = 0;

    bool purge_all_volatile = false;
    bool purge_all_clean_inode = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(purge_all_volatile, "Mode PURGE_ALL_VOLATILE", nullptr, 'v');
    args_parser.add_option(purge_all_clean_inode, "Mode PURGE_ALL_CLEAN_INODE", nullptr, 'c');
    args_parser.parse(arguments);

    if (!purge_all_volatile && !purge_all_clean_inode)
        purge_all_volatile = purge_all_clean_inode = true;

    if (purge_all_volatile)
        mode |= PURGE_ALL_VOLATILE;
    if (purge_all_clean_inode)
        mode |= PURGE_ALL_CLEAN_INODE;

    int purged_page_count = purge(mode);
    if (purged_page_count < 0) {
        perror("purge");
        return 1;
    }
    outln("Purged page count: {}", purged_page_count);
    return 0;
}
