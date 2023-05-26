/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AddEventDialog.h"
#include "EventManager.h"
#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

namespace Calendar {

class AddEventWidget final : public GUI::Widget {
    C_OBJECT(AddEventWidget);

public:
    static ErrorOr<NonnullRefPtr<AddEventWidget>> create(AddEventDialog*, Core::DateTime start_time, Core::DateTime end_time);
    virtual ~AddEventWidget() override = default;

private:
    AddEventWidget() = default;
    static ErrorOr<NonnullRefPtr<AddEventWidget>> try_create();

    void update_start_date();
    void update_end_date();
    void update_duration();

    Core::DateTime m_start_date_time;
    Core::DateTime m_end_date_time;

    RefPtr<GUI::TextBox> m_start_date_box;
    RefPtr<GUI::TextBox> m_end_date_box;
    RefPtr<GUI::SpinBox> m_start_hour_box;
    RefPtr<GUI::SpinBox> m_start_minute_box;
    RefPtr<GUI::SpinBox> m_end_hour_box;
    RefPtr<GUI::SpinBox> m_end_minute_box;
    RefPtr<GUI::SpinBox> m_duration_hour_box;
    RefPtr<GUI::SpinBox> m_duration_minute_box;
};

}
