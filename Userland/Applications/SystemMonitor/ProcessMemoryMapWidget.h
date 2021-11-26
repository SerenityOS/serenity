/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class ProcessMemoryMapWidget final : public GUI::Widget {
    C_OBJECT(ProcessMemoryMapWidget);

public:
    virtual ~ProcessMemoryMapWidget() override;

    void set_pid(pid_t);
    void refresh();

private:
    ProcessMemoryMapWidget();
    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::JsonArrayModel> m_json_model;
    pid_t m_pid { -1 };
    RefPtr<Core::Timer> m_timer;
};
