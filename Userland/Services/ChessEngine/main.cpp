/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessEngine.h"
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <unistd.h>

int main()
{
#ifdef __serenity__
    if (pledge("stdio recvfd sendfd unix rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    Core::EventLoop loop;

#ifdef __serenity__
    if (pledge("stdio recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }
#endif

    auto engine = ChessEngine::construct(Core::File::standard_input(), Core::File::standard_output());
    return loop.exec();
}
