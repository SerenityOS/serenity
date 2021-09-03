/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Format.h>
#include <Kernel/Modules/module_syms.h>

extern "C" const char module_name[] = "TestModule";

extern "C" void module_init()
{
    dmesgln("TestModule has booted!");

    for (int i = 0; i < 3; ++i) {
        dmesgln("i is now {}", i);
    }
}

extern "C" void module_fini()
{
    dmesgln("TestModule is being removed!");
}
