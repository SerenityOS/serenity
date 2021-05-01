/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <unistd.h>

int main(int, char**)
{
    Core::EventLoop event_loop;

    auto timer = Core::Timer::construct(10, [&] {
        dbgln("Now hanging!");
        while (true) {
            sleep(1);
        }
    });

    return event_loop.exec();
}
