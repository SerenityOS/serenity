/*
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace NotificationServer {

class NotificationWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(NotificationWidget)
public:
    static ErrorOr<NonnullRefPtr<NotificationWidget>> try_create();

    virtual ~NotificationWidget() override = default;

    Function<void()> on_click;
    virtual void mousedown_event(GUI::MouseEvent&) override
    {
        if (on_click)
            on_click();
    }

private:
    NotificationWidget() = default;
};

}
