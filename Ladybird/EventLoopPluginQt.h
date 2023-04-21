/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Platform/EventLoopPlugin.h>

namespace Ladybird {

class EventLoopPluginQt final : public Web::Platform::EventLoopPlugin {
public:
    EventLoopPluginQt();
    virtual ~EventLoopPluginQt() override;

    virtual void spin_until(JS::SafeFunction<bool()> goal_condition) override;
    virtual void deferred_invoke(JS::SafeFunction<void()>) override;
    virtual NonnullRefPtr<Web::Platform::Timer> create_timer() override;
    virtual void quit() override;
};

}
