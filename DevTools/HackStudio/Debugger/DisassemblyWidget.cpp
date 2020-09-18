/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include "DisassemblyWidget.h"
#include "DisassemblyModel.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

void UnavailableDisassemblyWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);
    if (reason().is_empty())
        return;
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_text(frame_inner_rect(), reason(), Gfx::TextAlignment::Center, palette().window_text(), Gfx::TextElision::Right);
}

DisassemblyWidget::DisassemblyWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    m_top_container = add<GUI::Widget>();
    m_top_container->set_layout<GUI::HorizontalBoxLayout>();
    m_top_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_top_container->set_preferred_size(0, 20);

    m_function_name_label = m_top_container->add<GUI::Label>("");
    m_function_name_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);

    m_disassembly_view = add<GUI::TableView>();

    m_unavailable_disassembly_widget = add<UnavailableDisassemblyWidget>("");

    hide_disassembly("Program isn't running");
}

void DisassemblyWidget::update_state(const Debug::DebugSession& debug_session, const PtraceRegisters& regs)
{
    m_disassembly_view->set_model(DisassemblyModel::create(debug_session, regs));

    if (m_disassembly_view->model()->row_count() > 0) {
        auto containing_function = debug_session.debug_info().get_containing_function(regs.eip);
        if (containing_function.has_value())
            m_function_name_label->set_text(containing_function.value().name);
        else
            m_function_name_label->set_text("<missing>");
        show_disassembly();
    } else {
        hide_disassembly("No disassembly to show for this function");
    }
}

void DisassemblyWidget::program_stopped()
{
    m_disassembly_view->set_model({});
    m_function_name_label->set_text("");
    hide_disassembly("Program isn't running");
}

void DisassemblyWidget::show_disassembly()
{
    m_top_container->set_visible(true);
    m_disassembly_view->set_visible(true);
    m_function_name_label->set_visible(true);
    m_unavailable_disassembly_widget->set_visible(false);
}

void DisassemblyWidget::hide_disassembly(const String& reason)
{
    m_top_container->set_visible(false);
    m_disassembly_view->set_visible(false);
    m_function_name_label->set_visible(false);
    m_unavailable_disassembly_widget->set_visible(true);
    m_unavailable_disassembly_widget->set_reason(reason);
}

}
