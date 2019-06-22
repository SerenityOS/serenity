#include <LibGUI/GButton.h>
#include <LibGUI/GComboBox.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWindow.h>

GComboBox::GComboBox(GWidget* parent)
    : GWidget(parent)
{
    m_editor = new GTextEditor(GTextEditor::Type::SingleLine, this);
    m_editor->on_change = [this] {
        if (on_change)
            on_change(m_editor->text());
    };
    m_open_button = new GButton(this);
    m_open_button->set_focusable(false);
    m_open_button->set_text("\xf7");
    m_open_button->on_click = [this](auto&) {
        if (m_list_window->is_visible())
            close();
        else
            open();
    };

    m_list_window = new GWindow(this);
    // FIXME: This is obviously not a tooltip window, but it's the closest thing to what we want atm.
    m_list_window->set_window_type(GWindowType::Tooltip);
    m_list_window->set_should_destroy_on_close(false);

    m_list_view = new GListView(nullptr);
    m_list_view->horizontal_scrollbar().set_visible(false);
    m_list_window->set_main_widget(m_list_view);

    m_list_view->on_selection = [this](auto& index) {
        ASSERT(model());
        auto new_value = model()->data(index).to_string();
        m_editor->set_text(new_value);
        m_editor->select_all();
        m_list_window->hide();
    };
}

GComboBox::~GComboBox()
{
}

void GComboBox::resize_event(GResizeEvent& event)
{
    int frame_thickness = m_editor->frame_thickness();
    int button_height = event.size().height() - frame_thickness * 2;
    int button_width = 15;
    m_open_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness, button_width, button_height);
    m_editor->set_relative_rect(0, 0, width(), height());
}

void GComboBox::set_model(NonnullRefPtr<GModel> model)
{
    m_list_view->set_model(move(model));
}

void GComboBox::open()
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
    Size size {
        max(width(), longest_item_width + m_list_view->width_occupied_by_vertical_scrollbar()) + m_list_view->frame_thickness() * 2 + m_list_view->horizontal_padding(),
        model()->row_count() * m_list_view->item_height() + m_list_view->frame_thickness() * 2
    };

    m_list_window->set_rect({ my_screen_rect.bottom_left(), size });
    m_list_window->show();
}

void GComboBox::close()
{
    m_list_window->hide();
}

String GComboBox::text() const
{
    return m_editor->text();
}
