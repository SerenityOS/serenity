/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessEngine.h"
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

const char* serenity_get_initial_promises()
{
    return "stdio recvfd sendfd unix rpath";
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop loop;
    TRY(Core::System::retract("rpath"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto engine = TRY(ChessEngine::try_create(Core::File::standard_input(), Core::File::standard_output()));
    return loop.exec();
}
