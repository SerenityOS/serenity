/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        layout()->set_margins({ 4, 4, 4, 4 });

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
    m_interrupt_table_view->model()->update();
}
