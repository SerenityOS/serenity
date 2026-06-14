/*
 * Copyright (c) 2024, Sanil Gupta <sanilg566@gmail.com>.
 * Copyright (c) 2026, RiffPointer <riffpointer@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewEventWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>

namespace Calendar {
ErrorOr<NonnullRefPtr<ViewEventWidget>> ViewEventWidget::create(ViewEventDialog* parent_window, Vector<Event>& events)
{
    auto widget = TRY(try_create());

    auto* events_list = widget->find_descendant_of_type_named<GUI::Widget>("events_list");
    for (auto const& event : events) {
        auto container = GUI::Widget::construct();
        container->set_layout<GUI::HorizontalBoxLayout>();
        container->layout()->set_spacing(4);
        container->set_fixed_height(22);

        String text = MUST(String::formatted("{} {}", event.start.to_byte_string("%H:%M"sv), event.summary));
        auto label = GUI::Label::construct(text);
        label->set_fill_with_background_color(true);
        label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        label->set_text_wrapping(Gfx::TextWrapping::DontWrap);
        container->add_child(label);

        auto edit_button = GUI::Button::construct("Edit"_string);
        edit_button->set_fixed_width(50);
        edit_button->on_click = [parent_window, event](auto) {
            parent_window->close_and_open_edit_event_dialog(event);
        };
        container->add_child(edit_button);

        auto delete_button = GUI::Button::construct("Delete"_string);
        delete_button->set_fixed_width(60);
        delete_button->on_click = [parent_window, event](auto) {
            parent_window->delete_event(event);
        };
        container->add_child(delete_button);

        events_list->add_child(container);
    }

    auto* add_new_event_button = widget->find_descendant_of_type_named<GUI::Button>("add_event_button");
    add_new_event_button->on_click = [window = parent_window](auto) {
        window->close_and_open_add_event_dialog();
    };

    return widget;
}

}
