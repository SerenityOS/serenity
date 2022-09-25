/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventLoopPluginSerenity.h"
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/EventLoop.h>
#include <LibWeb/Platform/TimerSerenity.h>

namespace Web::Platform {

EventLoopPluginSerenity::EventLoopPluginSerenity() = default;
EventLoopPluginSerenity::~EventLoopPluginSerenity() = default;

void EventLoopPluginSerenity::spin_until(Function<bool()> goal_condition)
{
    Core::EventLoop::current().spin_until(move(goal_condition));
}

void EventLoopPluginSerenity::deferred_invoke(Function<void()> function)
{
    VERIFY(function);
    Core::deferred_invoke(move(function));
}

NonnullRefPtr<Timer> EventLoopPluginSerenity::create_timer()
{
    return TimerSerenity::create();
}

}
