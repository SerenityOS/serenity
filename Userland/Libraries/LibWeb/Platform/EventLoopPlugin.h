/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Platform {

class EventLoopPlugin {
public:
    static EventLoopPlugin& the();
    static void install(EventLoopPlugin&);

    virtual ~EventLoopPlugin();

    virtual void spin_until(Function<bool()> goal_condition) = 0;
    virtual void deferred_invoke(Function<void()>) = 0;
    virtual NonnullRefPtr<Timer> create_timer() = 0;
};

}
