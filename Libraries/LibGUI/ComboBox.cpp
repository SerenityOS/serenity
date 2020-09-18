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

#include <LibGUI/ComboBox.h>
#include <LibGUI/ControlBoxButton.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace GUI {

class ComboBoxEditor final : public TextEditor {
    C_OBJECT(ComboBoxEditor);

public:
    Function<void(int delta)> on_mousewheel;

private:
    ComboBoxEditor()
        : TextEditor(TextEditor::SingleLine)
    {
    }

    virtual void mousewheel_event(MouseEvent& event) override
    {
        if (!is_focused())
            set_focus(true);
        if (on_mousewheel)
            on_mousewheel(event.wheel_delta());
    }
};

ComboBox::ComboBox()
{
    m_editor = add<ComboBoxEditor>();
    m_editor->set_has_open_button(true);
    m_editor->on_return_pressed = [this] {
        if (on_return_pressed)
            on_return_pressed();
    };
    m_editor->on_up_pressed = [this] {
        m_list_view->move_cursor(AbstractView::CursorMovement::Up, AbstractView::SelectionUpdate::Set);
    };
    m_editor->on_down_pressed = [this] {
        m_list_view->move_cursor(AbstractView::CursorMovement::Down, AbstractView::SelectionUpdate::Set);
    };
    m_editor->on_pageup_pressed = [this] {
        m_list_view->move_cursor(AbstractView::CursorMovement::PageUp, AbstractView::SelectionUpdate::Set);
    };
    m_editor->on_pagedown_pressed = [this] {
        m_list_view->move_cursor(AbstractView::CursorMovement::PageDown, AbstractView::SelectionUpdate::Set);
    };
    m_editor->on_mousewheel = [this](int delta) {
        m_list_view->move_cursor_relative(delta, AbstractView::SelectionUpdate::Set);
    };
    m_editor->on_mousedown = [this] {
        if (only_allow_values_from_model())
            m_open_button->click();
    };

    m_open_button = add<ControlBoxButton>(ControlBoxButton::DownArrow);
    m_open_button->set_focusable(false);
    m_open_button->on_click = [this](auto) {
        if (m_list_window->is_visible())
            close();
        else
            open();
    };

    m_list_window = add<Window>(window());
    m_list_window->set_frameless(true);
    m_list_window->set_accessory(true);
    m_list_window->on_active_input_change = [this](bool is_active_input) {
        if (!is_active_input) {
            m_open_button->set_enabled(false);
            close();
        }
        m_open_button->set_enabled(true);
    };

    m_list_view = m_list_window->set_main_widget<ListView>();
    m_list_view->horizontal_scrollbar().set_visible(false);
    m_list_view->set_alternating_row_colors(false);
    m_list_view->set_hover_highlighting(true);
    m_list_view->set_frame_thickness(1);
    m_list_view->set_frame_shadow(Gfx::FrameShadow::Plain);
    m_list_view->on_selection = [this](auto& index) {
        ASSERT(model());
        m_list_view->set_activates_on_selection(true);
        auto new_value = index.data().to_string();
        m_editor->set_text(new_value);
        if (!m_only_allow_values_from_model)
            m_editor->select_all();
    };

    m_list_view->on_activation = [this](auto& index) {
        deferred_invoke([this, index](auto&) {
            if (on_change)
                on_change(m_editor->text(), index);
        });
        m_list_view->set_activates_on_selection(false);
        close();
    };

    m_list_view->on_escape_pressed = [this] {
        close();
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

void ComboBox::set_selected_index(size_t index)
{
    if (!m_list_view->model())
        return;
    m_list_view->set_cursor(m_list_view->model()->index(index, 0), AbstractView::SelectionUpdate::Set);
}

size_t ComboBox::selected_index() const
{
    return m_list_view->cursor_index().row();
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
        auto item_text = index.data().to_string();
        longest_item_width = max(longest_item_width, m_list_view->font().width(item_text));
    }
    Gfx::IntSize size {
        max(width(), longest_item_width + m_list_view->width_occupied_by_vertical_scrollbar() + m_list_view->frame_thickness() * 2 + m_list_view->horizontal_padding()),
        model()->row_count() * m_list_view->item_height() + m_list_view->frame_thickness() * 2
    };

    auto taskbar_height = GUI::Desktop::the().taskbar_height();
    auto menubar_height = GUI::Desktop::the().menubar_height();
    // NOTE: This is so the combobox bottom edge exactly fits the taskbar's
    //       top edge - the value was found through trial and error though.
    auto offset = 8;
    Gfx::IntRect list_window_rect { my_screen_rect.bottom_left(), size };
    list_window_rect.intersect(Desktop::the().rect().shrunken(0, taskbar_height + menubar_height + offset));

    m_editor->set_has_visible_list(true);
    m_editor->set_focus(true);
    m_list_window->set_rect(list_window_rect);
    m_list_window->show();
}

void ComboBox::close()
{
    m_list_window->hide();
    m_editor->set_has_visible_list(false);
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
    m_editor->set_mode(m_only_allow_values_from_model ? TextEditor::DisplayOnly : TextEditor::Editable);
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
