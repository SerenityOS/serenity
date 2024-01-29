/*
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Model.h>

namespace GUI {

class DatePicker : public Dialog {
    C_OBJECT(DatePicker)

public:
    virtual ~DatePicker() override = default;

    static Optional<DateTime::LocalDateTime> show(Window* parent_window, String title, DateTime::LocalDateTime focused_date = DateTime::LocalDateTime::now());

private:
    explicit DatePicker(Window* parent_window, String const& title, DateTime::LocalDateTime focused_date = DateTime::LocalDateTime::now());

    DateTime::LocalDateTime m_selected_date;
    RefPtr<ComboBox> m_month_box;
    RefPtr<SpinBox> m_year_box;
};

}
