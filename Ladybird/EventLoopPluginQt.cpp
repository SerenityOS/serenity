/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "EventLoopPluginQt.h"
#include "TimerQt.h"
#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <QCoreApplication>
#include <QTimer>

namespace Ladybird {

EventLoopPluginQt::EventLoopPluginQt() = default;
EventLoopPluginQt::~EventLoopPluginQt() = default;

void EventLoopPluginQt::spin_until(Function<bool()> goal_condition)
{
    while (!goal_condition())
        QCoreApplication::processEvents();
}

void EventLoopPluginQt::deferred_invoke(Function<void()> function)
{
    VERIFY(function);
    QTimer::singleShot(0, [function = move(function)] {
        function();
    });
}

NonnullRefPtr<Web::Platform::Timer> EventLoopPluginQt::create_timer()
{
    return TimerQt::create();
}

}
