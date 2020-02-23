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

#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace GUI {

ComboBox::ComboBox()
{
    m_editor = add<TextBox>();
    m_editor->on_change = [this] {
        if (on_change)
            on_change(m_editor->text(), m_list_view->selection().first());
    };
    m_editor->on_return_pressed = [this] {
        if (on_return_pressed)
            on_return_pressed();
    };
    m_open_button = add<Button>();
    m_open_button->set_focusable(false);
    m_open_button->set_text("\xc3\xb7");
    m_open_button->on_click = [this](auto&) {
        if (m_list_window->is_visible())
            close();
        else
            open();
    };

    m_list_window = add<Window>();
    // FIXME: This is obviously not a tooltip window, but it's the closest thing to what we want atm.
    m_list_window->set_window_type(WindowType::Tooltip);

    m_list_view = ListView::construct();
    m_list_view->horizontal_scrollbar().set_visible(false);
    m_list_window->set_main_widget(m_list_view);

    m_list_view->on_selection = [this](auto& index) {
        ASSERT(model());
        auto new_value = model()->data(index).to_string();
        m_editor->set_text(new_value);
        m_editor->select_all();
        close();
        deferred_invoke([this, index](auto&) {
            if (on_change)
                on_change(m_editor->text(), index);
        });
    };
}

ComboBox::~ComboBox()
{
}

void ComboBox::resize_event(ResizeEvent& event)
{
    int frame_thickness = m_editor->frame_thickness();
    int button_height = event.size().height() - frame_thickness * 2;
    int button_width = 15;
    m_open_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness, button_width, button_height);
    m_editor->set_relative_rect(0, 0, width(), height());
}

void ComboBox::set_model(NonnullRefPtr<Model> model)
{
    m_list_view->set_model(move(model));
}

void ComboBox::select_all()
{
    m_editor->select_all();
}

void ComboBox::open()
{
    if (!model())
        return;

    auto my_screen_rect = screen_relative_rect();

    int longest_item_width = 0;
    for (int i = 0; i < model()->row_count(); ++i) {
        auto index = model()->index(i);
        auto item_text = model()->data(index).to_string();
        longest_item_width = max(longest_item_width, m_list_view->font().width(item_text));
    }
    Gfx::Size size {
        max(width(), longest_item_width + m_list_view->width_occupied_by_vertical_scrollbar() + m_list_view->frame_thickness() * 2 + m_list_view->horizontal_padding()),
        model()->row_count() * m_list_view->item_height() + m_list_view->frame_thickness() * 2
    };

    Gfx::Rect list_window_rect { my_screen_rect.bottom_left(), size };
    list_window_rect.intersect(Desktop::the().rect().shrunken(0, 128));

    m_list_window->set_rect(list_window_rect);
    m_list_window->show();
}

void ComboBox::close()
{
    m_list_window->hide();
    m_editor->set_focus(true);
}

String ComboBox::text() const
{
    return m_editor->text();
}

void ComboBox::set_text(const String& text)
{
    m_editor->set_text(text);
}

void ComboBox::set_only_allow_values_from_model(bool b)
{
    if (m_only_allow_values_from_model == b)
        return;
    m_only_allow_values_from_model = b;
    m_editor->set_readonly(m_only_allow_values_from_model);
}

Model* ComboBox::model()
{
    return m_list_view->model();
}

const Model* ComboBox::model() const
{
    return m_list_view->model();
}

int ComboBox::model_column() const
{
    return m_list_view->model_column();
}

void ComboBox::set_model_column(int column)
{
    m_list_view->set_model_column(column);
}

}
