/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<rpath, stdio>::pledge()));
    outln(TRY(Core::System::getcwd()));
    return 0;
}
