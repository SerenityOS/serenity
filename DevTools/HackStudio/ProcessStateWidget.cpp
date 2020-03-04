/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibCore/ProcessStatisticsReader.h>
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGfx/Font.h>
#include <unistd.h>

ProcessStateWidget::ProcessStateWidget()
{
    set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    set_preferred_size(0, 20);
    set_visible(false);

    set_layout<GUI::HorizontalBoxLayout>();

    auto pid_label_label = add<GUI::Label>("Process:");
    pid_label_label->set_font(Gfx::Font::default_bold_font());
    m_pid_label = add<GUI::Label>("");

    auto state_label_label = add<GUI::Label>("State:");
    state_label_label->set_font(Gfx::Font::default_bold_font());
    m_state_label = add<GUI::Label>("");

    // FIXME: This should show CPU% instead.
    auto cpu_label_label = add<GUI::Label>("Times scheduled:");
    cpu_label_label->set_font(Gfx::Font::default_bold_font());
    m_cpu_label = add<GUI::Label>("");

    auto memory_label_label = add<GUI::Label>("Memory (resident):");
    memory_label_label->set_font(Gfx::Font::default_bold_font());
    m_memory_label = add<GUI::Label>("");

    m_timer = add<Core::Timer>(500, [this] {
        refresh();
    });
}

ProcessStateWidget::~ProcessStateWidget()
{
}

void ProcessStateWidget::refresh()
{
    pid_t pid = tcgetpgrp(m_tty_fd);

    auto processes = Core::ProcessStatisticsReader::get_all();
    auto child_process_data = processes.get(pid);

    if (!child_process_data.has_value())
        return;

    auto active_process_data = processes.get(child_process_data.value().pgid);

    auto& data = active_process_data.value();

    m_pid_label->set_text(String::format("%s(%d)", data.name.characters(), pid));
    m_state_label->set_text(data.threads.first().state);
    m_cpu_label->set_text(String::format("%d", data.threads.first().times_scheduled));
    m_memory_label->set_text(String::format("%d", data.amount_resident));
}

void ProcessStateWidget::set_tty_fd(int tty_fd)
{
    m_tty_fd = tty_fd;
    if (m_tty_fd == -1) {
        set_visible(false);
        return;
    }
    set_visible(true);
    refresh();
}
