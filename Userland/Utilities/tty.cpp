/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    char* tty = ttyname(0);
    if (!tty) {
        perror("Error");
        return 1;
    }
    outln("{}", tty);
    return 0;
}
