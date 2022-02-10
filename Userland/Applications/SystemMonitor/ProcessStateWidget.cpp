/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessStateWidget.h"
#include "ProcessModel.h"
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TableView.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>

class ProcessStateModel final
    : public GUI::Model
    , public GUI::ModelClient {
public:
    explicit ProcessStateModel(ProcessModel& target, pid_t pid)
        : m_target(target)
        , m_pid(pid)
    {
        m_target.register_client(*this);
        refresh();
    }

    virtual ~ProcessStateModel() override
    {
        m_target.unregister_client(*this);
    }

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return m_target.column_count({}); }
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return 2; }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const override
    {
        if (role == GUI::ModelRole::Display) {
            if (index.column() == 0) {
                if (index.row() == ProcessModel::Column::Icon) {
                    // NOTE: The icon column is nameless in ProcessModel, but we want it to have a name here.
                    return "Icon";
                }
                return m_target.column_name(index.row());
            }
            return m_target_index.sibling_at_column(index.row()).data();
        }

        if (role == GUI::ModelRole::Font) {
            if (index.column() == 0) {
                return Gfx::FontDatabase::default_font().bold_variant();
            }
        }

        return {};
    }

    virtual void model_did_update([[maybe_unused]] unsigned flags) override
    {
        refresh();
    }

    void refresh()
    {
        m_target_index = {};
        for (int row = 0; row < m_target.row_count({}); ++row) {
            auto index = m_target.index(row, ProcessModel::Column::PID);
            if (index.data().to_i32() == m_pid) {
                m_target_index = index;
                break;
            }
        }
        invalidate();
    }

private:
    ProcessModel& m_target;
    GUI::ModelIndex m_target_index;
    pid_t m_pid { -1 };
};

ProcessStateWidget::ProcessStateWidget(pid_t pid)
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(4);
    m_table_view = add<GUI::TableView>();
    m_table_view->set_model(adopt_ref(*new ProcessStateModel(ProcessModel::the(), pid)));
    m_table_view->column_header().set_visible(false);
    m_table_view->column_header().set_section_size(0, 90);
}
