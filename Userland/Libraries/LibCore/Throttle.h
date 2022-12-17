/*
 * Copyright (c) 2022, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>

namespace Core {

template<typename TFunction>
auto throttle(TFunction function, int timeout)
{
    RefPtr<Core::Timer> timer;
    return [=]<typename... T>(T... args) mutable {
        if (!timer)
            timer = Core::Timer::create_single_shot(timeout, nullptr);
        else if (timer->is_active())
            return;
        timer->start();
        function(args...);
    };
};

}
