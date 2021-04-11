/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include "ProcessStateWidget.h"
#include "ProcessModel.h"
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SortingProxyModel.h>
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
        refresh();
    }
    virtual ~ProcessStateModel() override { }

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
                return Gfx::FontDatabase::default_bold_font();
            }
        }

        return {};
    }

    virtual void update() override
    {
        did_update(GUI::Model::DontInvalidateIndexes);
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
        update();
    }

private:
    ProcessModel& m_target;
    GUI::ModelIndex m_target_index;
    pid_t m_pid { -1 };
};

ProcessStateWidget::ProcessStateWidget(pid_t pid)
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    m_table_view = add<GUI::TableView>();
    m_table_view->column_header().set_visible(false);
    m_table_view->column_header().set_section_size(0, 90);
    m_table_view->set_model(adopt(*new ProcessStateModel(ProcessModel::the(), pid)));
}

ProcessStateWidget::~ProcessStateWidget()
{
}
