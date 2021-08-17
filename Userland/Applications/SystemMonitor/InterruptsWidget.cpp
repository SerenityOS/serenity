/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InterruptsWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

InterruptsWidget::InterruptsWidget()
{
    on_first_show = [this](auto&) {
        set_layout<GUI::VerticalBoxLayout>();
        layout()->set_margins(4);

        Vector<GUI::JsonArrayModel::FieldSpec> interrupts_field;
        interrupts_field.empend("interrupt_line", "Line", Gfx::TextAlignment::CenterRight);
        interrupts_field.empend("purpose", "Purpose", Gfx::TextAlignment::CenterLeft);
        interrupts_field.empend("controller", "Controller", Gfx::TextAlignment::CenterLeft);
        interrupts_field.empend("cpu_handler", "CPU Handler", Gfx::TextAlignment::CenterRight);
        interrupts_field.empend("device_sharing", "# Devices Sharing", Gfx::TextAlignment::CenterRight);
        interrupts_field.empend("call_count", "Call Count", Gfx::TextAlignment::CenterRight);

        m_interrupt_table_view = add<GUI::TableView>();
        m_interrupt_model = GUI::JsonArrayModel::create("/proc/interrupts", move(interrupts_field));
        m_interrupt_table_view->set_model(GUI::SortingProxyModel::create(*m_interrupt_model));

        m_update_timer = add<Core::Timer>(
            1000, [this] {
                update_model();
            });

        update_model();
    };
}

InterruptsWidget::~InterruptsWidget()
{
}

void InterruptsWidget::update_model()
{
    m_interrupt_model->invalidate();
}
