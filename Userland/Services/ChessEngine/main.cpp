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

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd unix"));
    Core::EventLoop loop;
    TRY(Core::System::unveil(nullptr, nullptr));

    auto engine = TRY(ChessEngine::try_create(Core::File::standard_input(), Core::File::standard_output()));
    return loop.exec();
}
