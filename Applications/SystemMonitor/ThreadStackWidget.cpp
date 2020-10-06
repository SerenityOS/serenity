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

#include "ThreadStackWidget.h"
#include <AK/ByteBuffer.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>

ThreadStackWidget::ThreadStackWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 4, 4, 4, 4 });
    m_stack_editor = add<GUI::TextEditor>();
    m_stack_editor->set_mode(GUI::TextEditor::ReadOnly);

    m_timer = add<Core::Timer>(1000, [this] { refresh(); });
}

ThreadStackWidget::~ThreadStackWidget()
{
}

void ThreadStackWidget::set_ids(pid_t pid, pid_t tid)
{
    if (m_pid == pid && m_tid == tid)
        return;
    m_pid = pid;
    m_tid = tid;
    refresh();
}

void ThreadStackWidget::refresh()
{
    auto file = Core::File::construct(String::formatted("/proc/{}/stacks/{}", m_pid, m_tid));
    if (!file->open(Core::IODevice::ReadOnly)) {
        m_stack_editor->set_text(String::formatted("Unable to open {}", file->filename()));
        return;
    }

    auto new_text = file->read_all();
    if (m_stack_editor->text() != new_text) {
        m_stack_editor->set_text(new_text);
    }
}
