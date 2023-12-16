/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    m_top_container->set_fixed_height(20);

    m_function_name_label = m_top_container->add<GUI::Label>();

    m_disassembly_view = add<GUI::TableView>();

    m_unavailable_disassembly_widget = add<UnavailableDisassemblyWidget>("");

    hide_disassembly("Program isn't running");
}

void DisassemblyWidget::update_state(Debug::DebugSession const& debug_session, PtraceRegisters const& regs)
{
    m_disassembly_view->set_model(DisassemblyModel::create(debug_session, regs));

    if (m_disassembly_view->model()->row_count() > 0) {
        auto lib = debug_session.library_at(regs.ip());
        if (!lib)
            return;
        auto containing_function = lib->debug_info->get_containing_function(regs.ip() - lib->base_address);
        if (containing_function.has_value())
            m_function_name_label->set_text(String::from_byte_string(containing_function.value().name).release_value_but_fixme_should_propagate_errors());
        else
            m_function_name_label->set_text("<missing>"_string);
        show_disassembly();
    } else {
        hide_disassembly("No disassembly to show for this function");
    }
}

void DisassemblyWidget::program_stopped()
{
    m_disassembly_view->set_model({});
    m_function_name_label->set_text({});
    hide_disassembly("Program isn't running");
}

void DisassemblyWidget::show_disassembly()
{
    m_top_container->set_visible(true);
    m_disassembly_view->set_visible(true);
    m_function_name_label->set_visible(true);
    m_unavailable_disassembly_widget->set_visible(false);
}

void DisassemblyWidget::hide_disassembly(ByteString const& reason)
{
    m_top_container->set_visible(false);
    m_disassembly_view->set_visible(false);
    m_function_name_label->set_visible(false);
    m_unavailable_disassembly_widget->set_visible(true);
    m_unavailable_disassembly_widget->set_reason(reason);
}

}
