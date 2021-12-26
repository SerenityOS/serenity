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

#include "ProcessStacksWidget.h"
#include <AK/ByteBuffer.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>

ProcessStacksWidget::ProcessStacksWidget()
{
    set_layout(make<GUI::VerticalBoxLayout>());
    layout()->set_margins({ 4, 4, 4, 4 });
    m_stacks_editor = add<GUI::TextEditor>();
    m_stacks_editor->set_readonly(true);

    m_timer = add<Core::Timer>(1000, [this] { refresh(); });
}

ProcessStacksWidget::~ProcessStacksWidget()
{
}

void ProcessStacksWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    refresh();
}

void ProcessStacksWidget::refresh()
{
    auto file = Core::File::construct(String::format("/proc/%d/stack", m_pid));
    if (!file->open(Core::IODevice::ReadOnly)) {
        m_stacks_editor->set_text(String::format("Unable to open %s", file->filename().characters()));
        return;
    }

    m_stacks_editor->set_text(file->read_all());
}
