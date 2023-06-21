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

    auto infile = TRY(Core::File::standard_input());
    TRY(infile->set_blocking(false));
    auto outfile = TRY(Core::File::standard_output());
    TRY(outfile->set_blocking(false));
    auto engine = TRY(ChessEngine::try_create(move(infile), move(outfile)));
    engine->on_quit = [&](auto status_code) {
        loop.quit(status_code);
    };

    return loop.exec();
}
