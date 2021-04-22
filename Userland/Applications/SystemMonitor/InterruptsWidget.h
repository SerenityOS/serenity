/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/LazyWidget.h>

class InterruptsWidget final : public GUI::LazyWidget {
    C_OBJECT(InterruptsWidget)
public:
    virtual ~InterruptsWidget() override;

private:
    InterruptsWidget();
    void update_model();

    RefPtr<GUI::TableView> m_interrupt_table_view;
    RefPtr<GUI::JsonArrayModel> m_interrupt_model;
    RefPtr<Core::Timer> m_update_timer;
};
