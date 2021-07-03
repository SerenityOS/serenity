/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$beep()
{
    PCSpeaker::tone_on(440);
    auto result = Thread::current()->sleep(Time::from_nanoseconds(200'000'000));
    PCSpeaker::tone_off();
    if (result.was_interrupted())
        return EINTR;
    return 0;
}

}
