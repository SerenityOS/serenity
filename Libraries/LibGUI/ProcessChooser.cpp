/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ProcessChooser.h>
#include <LibGUI/RunningProcessesModel.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

namespace GUI {

ProcessChooser::ProcessChooser(const StringView& window_title, const StringView& button_label, const Gfx::Bitmap* window_icon, GUI::Window* parent_window)
    : Dialog(parent_window)
    , m_window_title(window_title)
    , m_button_label(button_label)
    , m_window_icon(window_icon)
{
    set_title(m_window_title);

    if (m_window_icon)
        set_icon(m_window_icon);
    else if (parent_window)
        set_icon(parent_window->icon());

    Gfx::IntRect window_rect { 0, 0, 300, 340 };
    window_rect.center_within(GUI::Desktop::the().rect());
    set_rect(window_rect);

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_margins({ 0, 0, 0, 2 });

    auto& table_view = widget.add<GUI::TableView>();
    auto sorting_model = GUI::SortingProxyModel::create(RunningProcessesModel::create());
    sorting_model->set_sort_role(GUI::Model::Role::Display);
    sorting_model->set_key_column_and_sort_order(RunningProcessesModel::Column::PID, GUI::SortOrder::Descending);
    table_view.set_model(sorting_model);

    auto& button_container = widget.add<GUI::Widget>();
    button_container.set_preferred_size(0, 30);
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    button_container.layout()->set_margins({ 0, 0, 4, 0 });
    button_container.layout()->add_spacer();

    auto& select_button = button_container.add<GUI::Button>(m_button_label);
    select_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    select_button.set_preferred_size(80, 24);
    select_button.on_click = [&](auto) {
        if (table_view.selection().is_empty()) {
            GUI::MessageBox::show(this, "No process selected!", m_window_title, GUI::MessageBox::Type::Error);
            return;
        }
        auto index = table_view.selection().first();
        auto pid_as_variant = table_view.model()->data(index, GUI::Model::Role::Custom);
        m_pid = pid_as_variant.as_i32();
        done(ExecOK);
    };
    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    cancel_button.set_preferred_size(80, 24);
    cancel_button.on_click = [this](auto) {
        done(ExecCancel);
    };

    table_view.model()->update();
}

ProcessChooser::~ProcessChooser()
{
}

}
