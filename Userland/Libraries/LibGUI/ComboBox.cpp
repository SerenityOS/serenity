/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Event.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

REGISTER_WIDGET(GUI, ComboBox)

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
            on_mousewheel(event.wheel_delta_y());
    }

    virtual void keydown_event(KeyEvent& event) override
    {
        if (event.key() == Key_Escape) {
            if (is_focused())
                set_focus(false);
            event.accept();
        } else
            TextEditor::keydown_event(event);
    }
};

ComboBox::ComboBox()
{
    REGISTER_STRING_PROPERTY("placeholder", editor_placeholder, set_editor_placeholder);
    REGISTER_BOOL_PROPERTY("model_only", only_allow_values_from_model, set_only_allow_values_from_model);

    set_min_width(32);
    set_fixed_height(22);

    m_editor = add<ComboBoxEditor>();
    m_editor->set_frame_thickness(0);
    m_editor->on_return_pressed = [this] {
        if (on_return_pressed)
            on_return_pressed();
    };
    m_editor->on_up_pressed = [this] {
        navigate(AbstractView::CursorMovement::Up);
    };
    m_editor->on_down_pressed = [this] {
        navigate(AbstractView::CursorMovement::Down);
    };
    m_editor->on_pageup_pressed = [this] {
        navigate(AbstractView::CursorMovement::PageUp);
    };
    m_editor->on_pagedown_pressed = [this] {
        navigate(AbstractView::CursorMovement::PageDown);
    };
    m_editor->on_mousewheel = [this](int delta) {
        // Since we can only show one item at a time we don't want to
        // skip any. So just move one item at a time.
        navigate_relative(delta > 0 ? 1 : -1);
    };
    m_editor->on_mousedown = [this] {
        if (only_allow_values_from_model())
            m_open_button->click();
    };

    m_open_button = add<Button>();
    m_open_button->set_button_style(Gfx::ButtonStyle::ThickCap);
    m_open_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/downward-triangle.png").release_value_but_fixme_should_propagate_errors());
    m_open_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
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
    m_list_view->on_selection_change = [this] {
        VERIFY(model());
        const auto& index = m_list_view->selection().first();
        m_list_view->set_activates_on_selection(true);
        if (m_updating_model)
            selection_updated(index);
    };

    m_list_view->on_activation = [this](auto& index) {
        deferred_invoke([this, index] {
            selection_updated(index);
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

void ComboBox::set_editor_placeholder(StringView placeholder)
{
    m_editor->set_placeholder(placeholder);
}

const String& ComboBox::editor_placeholder() const
{
    return m_editor->placeholder();
}

void ComboBox::navigate(AbstractView::CursorMovement cursor_movement)
{
    auto previous_selected = m_list_view->cursor_index();
    m_list_view->move_cursor(cursor_movement, AbstractView::SelectionUpdate::Set);
    auto current_selected = m_list_view->cursor_index();
    selection_updated(current_selected);
    if (previous_selected.row() != current_selected.row() && on_change)
        on_change(m_editor->text(), current_selected);
}

void ComboBox::navigate_relative(int delta)
{
    auto previous_selected = m_list_view->cursor_index();
    m_list_view->move_cursor_relative(delta, AbstractView::SelectionUpdate::Set);
    auto current_selected = m_list_view->cursor_index();
    selection_updated(current_selected);
    if (previous_selected.row() != current_selected.row() && on_change)
        on_change(m_editor->text(), current_selected);
}

void ComboBox::selection_updated(const ModelIndex& index)
{
    if (index.is_valid())
        m_selected_index = index;
    else
        m_selected_index.clear();
    auto new_value = index.data().to_string();
    m_editor->set_text(new_value);
    if (!m_only_allow_values_from_model)
        m_editor->select_all();
}

void ComboBox::resize_event(ResizeEvent& event)
{
    Widget::resize_event(event);
    int button_height = event.size().height() - frame_thickness() * 2;
    int button_width = 15;
    m_open_button->set_relative_rect(width() - button_width - frame_thickness(), frame_thickness(), button_width, button_height);
    auto editor_rect = frame_inner_rect();
    editor_rect.set_width(editor_rect.width() - button_width);
    m_editor->set_relative_rect(editor_rect);
}

void ComboBox::set_model(NonnullRefPtr<Model> model)
{
    TemporaryChange change(m_updating_model, true);
    m_selected_index.clear();
    m_list_view->set_model(move(model));
}

void ComboBox::set_selected_index(size_t index, AllowCallback allow_callback)
{
    if (!m_list_view->model())
        return;
    size_t previous_index = selected_index();
    TemporaryChange change(m_updating_model, true);
    m_list_view->set_cursor(m_list_view->model()->index(index, 0), AbstractView::SelectionUpdate::Set);
    if (previous_index != selected_index() && on_change && allow_callback == AllowCallback::Yes)
        on_change(m_editor->text(), m_list_view->cursor_index());
}

size_t ComboBox::selected_index() const
{
    return m_selected_index.has_value() ? m_selected_index.value().row() : 0;
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
    Gfx::IntRect list_window_rect { my_screen_rect.bottom_left(), size };
    list_window_rect.intersect(Desktop::the().rect().shrunken(0, taskbar_height * 2));

    m_editor->set_focus(true);
    if (m_selected_index.has_value()) {
        // Don't set m_updating_model to true here because we only want to
        // change the list view's selected item without triggering a change to it.
        m_list_view->set_cursor(m_selected_index.value(), AbstractView::SelectionUpdate::Set);
    }

    // Change direction and go upwards to prevent the list from becoming
    // infinitesimally small when pushed up against the screen edge.
    auto minimum_height = min(3, model()->row_count()) * m_list_view->item_height() + m_list_view->frame_thickness() * 2;
    bool go_upwards_instead = list_window_rect.height() < minimum_height;
    if (go_upwards_instead) {
        auto origin_point = my_screen_rect.top_left();
        list_window_rect = { Gfx::IntPoint { origin_point.x(), origin_point.y() - size.height() }, size };
        list_window_rect.intersect(Desktop::the().rect());
    }

    m_list_window->set_rect(list_window_rect);
    m_list_window->show();
}

void ComboBox::close()
{
    m_list_window->hide();
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
