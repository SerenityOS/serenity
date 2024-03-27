/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibWeb/Forward.h>
#include <LibWebView/Forward.h>

namespace Browser {

class TaskManagerWidget : public GUI::Widget {
    C_OBJECT(TaskManagerWidget)

public:
    TaskManagerWidget();
    ~TaskManagerWidget() override;

private:
    virtual void show_event(GUI::ShowEvent&) override;
    virtual void hide_event(GUI::HideEvent&) override;

    void update_statistics();

    RefPtr<WebView::OutOfProcessWebView> m_web_view;
    RefPtr<Core::Timer> m_update_timer;
};

}
