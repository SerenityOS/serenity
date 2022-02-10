/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class ProcessFileDescriptorMapWidget final : public GUI::Widget {
    C_OBJECT(ProcessFileDescriptorMapWidget);

public:
    virtual ~ProcessFileDescriptorMapWidget() override = default;

    void set_pid(pid_t);

private:
    ProcessFileDescriptorMapWidget();

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::JsonArrayModel> m_model;
    pid_t m_pid { -1 };
};
