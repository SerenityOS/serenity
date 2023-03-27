/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessEngine.h"
#include <LibCore/DeprecatedFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

#include <unistd.h>

namespace {

bool is_parent_dead()
{
    pid_t ppid = getppid();
    return kill(ppid, 0) < 0;
}

}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd unix proc"));
    Core::EventLoop loop;
    TRY(Core::System::unveil(nullptr, nullptr));

    auto engine = TRY(ChessEngine::try_create(Core::DeprecatedFile::standard_input(), Core::DeprecatedFile::standard_output()));

    loop.spin_until(is_parent_dead);
    return 0;
}
