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

#include "ProcessChooser.h"
#include "RunningProcessesModel.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>

namespace Profiler {

ProcessChooser::ProcessChooser(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    build();
}

void ProcessChooser::build()
{
    set_title("Profiler");
    Gfx::IntRect window_rect { 0, 0, 480, 360 };
    window_rect.center_within(GUI::Desktop::the().rect());
    set_rect(window_rect);

    auto& widget = set_main_widget<GUI::Widget>();
    widget.set_fill_with_background_color(true);
    widget.set_layout<GUI::VerticalBoxLayout>();
    auto& table_view = widget.add<GUI::TableView>();
    table_view.set_model(GUI::SortingProxyModel::create(Profiler::RunningProcessesModel::create()));
    table_view.model()->set_key_column_and_sort_order(Profiler::RunningProcessesModel::Column::PID, GUI::SortOrder::Descending);
    auto& button_container = widget.add<GUI::Widget>();
    button_container.set_preferred_size(0, 30);
    button_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_container.set_layout<GUI::HorizontalBoxLayout>();
    auto& profile_button = button_container.add<GUI::Button>("Profile");
    profile_button.on_click = [&](auto) {
        if (table_view.selection().is_empty()) {
            GUI::MessageBox::show("No process selected!", "Profiler", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, this);
            return;
        }
        auto index = table_view.selection().first();
        auto pid_as_variant = table_view.model()->data(index, GUI::Model::Role::Custom);
        m_pid = pid_as_variant.as_i32();
        done(ExecOK);
    };
    auto& cancel_button = button_container.add<GUI::Button>("Cancel");
    cancel_button.on_click = [this](auto) {
        done(ExecCancel);
    };

    table_view.model()->update();
}

}
