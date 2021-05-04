/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessPickerWidget.h"
#include "Profile.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>

namespace Profiler {

ProcessPickerWidget::ProcessPickerWidget(Profile& profile)
    : m_profile(profile)
{
    set_layout<GUI::HorizontalBoxLayout>();
    set_fixed_height(30);

    set_frame_shape(Gfx::FrameShape::NoFrame);

    auto& label = add<GUI::Label>("Process:");
    label.set_fixed_width(50);
    label.set_text_alignment(Gfx::TextAlignment::CenterRight);

    m_process_combo = add<GUI::ComboBox>();
    m_process_combo->set_only_allow_values_from_model(true);

    m_processes.append("All processes");

    for (auto& process : m_profile.processes())
        m_processes.append(String::formatted("{}: {}", process.pid, process.executable));

    m_process_combo->set_model(*GUI::ItemListModel<String>::create(m_processes));
    m_process_combo->set_selected_index(0);
    m_process_combo->on_change = [this](auto&, const GUI::ModelIndex& index) {
        if (index.row() == 0) {
            m_profile.clear_process_filter();
        } else {
            auto& process = m_profile.processes()[index.row() - 1];
            auto end_valid = process.end_valid == 0 ? m_profile.last_timestamp() : process.end_valid;
            m_profile.set_process_filter(process.pid, process.start_valid, end_valid);
        }
    };
}

ProcessPickerWidget::~ProcessPickerWidget()
{
}

}
