/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class ProcessUnveiledPathsWidget final : public GUI::Widget {
    C_OBJECT(ProcessUnveiledPathsWidget);

public:
    virtual ~ProcessUnveiledPathsWidget() override;

    void set_pid(pid_t);

private:
    ProcessUnveiledPathsWidget();

    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::JsonArrayModel> m_model;
    pid_t m_pid { -1 };
};
