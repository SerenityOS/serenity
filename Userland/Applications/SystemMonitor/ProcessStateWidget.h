/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace SystemMonitor {

class ProcessStateWidget final : public GUI::Widget {
    C_OBJECT(ProcessStateWidget);

public:
    virtual ~ProcessStateWidget() override = default;

    void set_pid(pid_t);

private:
    ProcessStateWidget();
    RefPtr<GUI::TableView> m_table_view;
};

}
