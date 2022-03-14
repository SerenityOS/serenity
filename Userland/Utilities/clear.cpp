/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio>::pledge()));
    printf("\033[3J\033[H\033[2J");
    fflush(stdout);
    return 0;
}
