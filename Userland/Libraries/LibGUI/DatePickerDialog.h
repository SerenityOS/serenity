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

class DatePickerDialog : public Dialog {
    C_OBJECT(DatePickerDialog)

public:
    virtual ~DatePickerDialog() override = default;

    static ExecResult show(Window* parent_window, String title, Core::DateTime& result, Core::DateTime focused_date = Core::DateTime::now());

private:
    explicit DatePickerDialog(Window* parent_window, String const& title, Core::DateTime focused_date = Core::DateTime::now());

    class MonthListModel final : public GUI::Model {
    public:
        enum Column {
            Month,
            __Count,
        };

        static NonnullRefPtr<MonthListModel> create() { return adopt_ref(*new MonthListModel); }
        virtual ~MonthListModel() override = default;

        virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 12; }
        virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
        virtual ErrorOr<String> column_name(int) const override;
        virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;

    private:
        MonthListModel() = default;
    };

    Core::DateTime m_selected_date;
    RefPtr<ComboBox> m_month_box;
    RefPtr<SpinBox> m_year_box;
};

}
