/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

class ProcessStateWidget final : public GUI::Widget {
    C_OBJECT(ProcessStateWidget);

public:
    virtual ~ProcessStateWidget() override;

private:
    explicit ProcessStateWidget(pid_t);
    RefPtr<GUI::TableView> m_table_view;
};
