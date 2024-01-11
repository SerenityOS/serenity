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
#include <LibGUI/Widget.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(SystemMonitor, ProcessStateWidget)

namespace SystemMonitor {

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
                return m_target.column_name(index.row()).release_value_but_fixme_should_propagate_errors();
            }
            return m_target_index.sibling_at_column(index.row()).data(ProcessModel::DISPLAY_VERBOSE);
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
        did_update(GUI::Model::UpdateFlag::DontInvalidateIndices);
    }

    void set_pid(pid_t pid)
    {
        m_pid = pid;
        refresh();
    }
    pid_t pid() const { return m_pid; }

private:
    ProcessModel& m_target;
    GUI::ModelIndex m_target_index;
    pid_t m_pid { -1 };
};

ErrorOr<NonnullRefPtr<ProcessStateWidget>> ProcessStateWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessStateWidget()));
    widget->set_layout<GUI::VerticalBoxLayout>(4);
    widget->m_table_view = widget->add<GUI::TableView>();
    widget->m_table_view->set_model(TRY(try_make_ref_counted<ProcessStateModel>(ProcessModel::the(), 0)));
    widget->m_table_view->column_header().set_visible(false);
    widget->m_table_view->column_header().set_section_size(0, 90);
    return widget;
}

void ProcessStateWidget::set_pid(pid_t pid)
{
    static_cast<ProcessStateModel*>(m_table_view->model())->set_pid(pid);
    update();
}

}
