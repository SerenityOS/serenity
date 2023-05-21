/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace SystemMonitor {

class ProcessFileDescriptorMapWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(ProcessFileDescriptorMapWidget)

public:
    virtual ~ProcessFileDescriptorMapWidget() override = default;

    static ErrorOr<NonnullRefPtr<ProcessFileDescriptorMapWidget>> try_create();

    void set_pid(pid_t);

private:
    ProcessFileDescriptorMapWidget() = default;

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::JsonArrayModel> m_model;
    pid_t m_pid { -1 };
};

}
