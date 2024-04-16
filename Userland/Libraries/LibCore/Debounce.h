/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>

namespace Core {

template<typename TFunction>
auto debounce(int timeout, TFunction function)
{
    RefPtr<Core::Timer> timer;
    return [=]<typename... T>(T... args) mutable {
        auto apply_function = [=] { function(args...); };
        if (timer) {
            timer->stop();
            timer->on_timeout = move(apply_function);
        } else {
            timer = Core::Timer::create_single_shot(timeout, move(apply_function));
        }
        timer->start();
    };
}

}
