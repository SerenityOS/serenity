/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Calendar.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

class AddEventDialog final : public GUI::Dialog {
    C_OBJECT(AddEventDialog)
public:
    virtual ~AddEventDialog() override = default;

    static void show(Core::DateTime date_time, Window* parent_window = nullptr)
    {
        auto dialog = AddEventDialog::construct(date_time, parent_window);
        dialog->exec();
    }

private:
    AddEventDialog(Core::DateTime date_time, Window* parent_window = nullptr);

    class MonthListModel final : public GUI::Model {
    public:
        enum Column {
            Month,
            __Count,
        };

        static NonnullRefPtr<MonthListModel> create() { return adopt_ref(*new MonthListModel); }
        virtual ~MonthListModel() override = default;

        virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
        virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
        virtual String column_name(int) const override;
        virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    private:
        MonthListModel() = default;
    };

    Core::DateTime m_date_time;
};
