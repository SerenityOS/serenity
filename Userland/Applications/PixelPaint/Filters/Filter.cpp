/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Filter.h"
#include "../ImageProcessor.h"
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

ErrorOr<RefPtr<GUI::Widget>> Filter::get_settings_widget()
{
    if (!m_settings_widget) {
        auto settings_widget = GUI::Widget::construct();
        settings_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& name_label = settings_widget->add<GUI::Label>(TRY(String::from_utf8(filter_name())));
        name_label.set_text_alignment(Gfx::TextAlignment::TopLeft);

        settings_widget->add<GUI::Widget>();
        m_settings_widget = settings_widget;
    }

    return m_settings_widget.ptr();
}

void Filter::apply()
{
    if (!m_editor)
        return;
    // FIXME: I am not thread-safe!
    // If you try to edit the bitmap while the image processor is still running... :yaksplode:
    if (auto* layer = m_editor->active_layer())
        MUST(ImageProcessor::the()->enqueue_command(make_ref_counted<FilterApplicationCommand>(*this, *layer)));
}

void Filter::update_preview()
{
    m_update_timer->restart();
}
}
