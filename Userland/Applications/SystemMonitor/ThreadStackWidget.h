/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/TableView.h>
#include <LibGUI/Widget.h>

class ThreadStackWidget final : public GUI::Widget {
    C_OBJECT(ThreadStackWidget)
public:
    virtual ~ThreadStackWidget() override;

    void set_ids(pid_t pid, pid_t tid);
    void refresh();

private:
    ThreadStackWidget();

    virtual void show_event(GUI::ShowEvent&) override;
    virtual void hide_event(GUI::HideEvent&) override;
    virtual void custom_event(Core::CustomEvent&) override;

    pid_t m_pid { -1 };
    pid_t m_tid { -1 };
    RefPtr<GUI::TableView> m_stack_table;
    RefPtr<Core::Timer> m_timer;
};
