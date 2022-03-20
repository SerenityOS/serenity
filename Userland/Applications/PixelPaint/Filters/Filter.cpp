/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Filter.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>

namespace PixelPaint {

Filter::Filter(ImageEditor* editor)
    : m_editor(editor)
    , m_update_timer(Core::Timer::create_single_shot(100, [&] {
        if (on_settings_change)
            on_settings_change();
    }))
{
    m_update_timer->set_active(false);
}

RefPtr<GUI::Widget> Filter::get_settings_widget()
{
    if (!m_settings_widget) {
        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = m_settings_widget->add<GUI::Label>(filter_name());
        name_label.set_text_alignment(Gfx::TextAlignment::TopLeft);

        m_settings_widget->add<GUI::Widget>();
    }

    return m_settings_widget.ptr();
}

void Filter::apply() const
{
    if (!m_editor)
        return;
    if (auto* layer = m_editor->active_layer()) {
        apply(layer->content_bitmap(), layer->content_bitmap());
        layer->did_modify_bitmap(layer->rect());
        m_editor->did_complete_action();
    }
}

void Filter::update_preview()
{
    m_update_timer->restart();
}
}
