#include "Locator.h"
#include "Project.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWindow.h>

extern RefPtr<Project> g_project;
extern void open_file(const String&);
static RefPtr<GraphicsBitmap> s_file_icon;

class LocatorSuggestionModel final : public GModel {
public:
    explicit LocatorSuggestionModel(Vector<String>&& suggestions)
        : m_suggestions(move(suggestions))
    {
    }

    enum Column {
        Icon,
        Name,
        __Column_Count,
    };
    virtual int row_count(const GModelIndex& = GModelIndex()) const override { return m_suggestions.size(); }
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return Column::__Column_Count; }
    virtual GVariant data(const GModelIndex& index, Role role = Role::Display) const override
    {
        auto& suggestion = m_suggestions.at(index.row());
        if (role == Role::Display) {
            if (index.column() == Column::Name)
                return suggestion;
            if (index.column() == Column::Icon) {
                if (!s_file_icon) {
                    s_file_icon = GraphicsBitmap::load_from_file("/res/icons/16x16/filetype-unknown.png");
                }
                return *s_file_icon;
            }
        }
        return {};
    }
    virtual void update() override {};

private:
    Vector<String> m_suggestions;
};

class LocatorTextBox final : public GTextBox {
    C_OBJECT(LocatorTextBox)
public:
    virtual ~LocatorTextBox() override {}

    Function<void()> on_up;
    Function<void()> on_down;

    virtual void keydown_event(GKeyEvent& event) override
    {
        if (event.key() == Key_Up)
            on_up();
        else if (event.key() == Key_Down)
            on_down();

        GTextBox::keydown_event(event);
    }

private:
    LocatorTextBox(GWidget* parent)
        : GTextBox(parent)
    {
    }
};

Locator::Locator(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 20);
    m_textbox = LocatorTextBox::construct(this);
    m_textbox->on_change = [this] {
        update_suggestions();
    };
    m_textbox->on_escape_pressed = [this] {
        m_popup_window->hide();
    };
    m_textbox->on_up = [this] {
        GModelIndex new_index = m_suggestion_view->selection().first();
        if (new_index.is_valid())
            new_index = m_suggestion_view->model()->index(new_index.row() - 1);
        else
            new_index = m_suggestion_view->model()->index(0);

        if (new_index.is_valid())
            m_suggestion_view->selection().set(new_index);
    };
    m_textbox->on_down = [this] {
        GModelIndex new_index = m_suggestion_view->selection().first();
        if (new_index.is_valid())
            new_index = m_suggestion_view->model()->index(new_index.row() + 1);
        else
            new_index = m_suggestion_view->model()->index(0);

        if (new_index.is_valid())
            m_suggestion_view->selection().set(new_index);
    };
    m_textbox->on_return_pressed = [this] {
        auto selected_index = m_suggestion_view->selection().first();
        if (!selected_index.is_valid())
            return;
        auto filename_index = m_suggestion_view->model()->index(selected_index.row(), LocatorSuggestionModel::Column::Name);
        auto filename = m_suggestion_view->model()->data(filename_index, GModel::Role::Display).to_string();
        open_file(filename);
        close();
    };

    m_popup_window = GWindow::construct();
    // FIXME: This is obviously not a tooltip window, but it's the closest thing to what we want atm.
    m_popup_window->set_window_type(GWindowType::Tooltip);
    m_popup_window->set_rect(0, 0, 500, 200);

    m_suggestion_view = GTableView::construct(nullptr);
    m_suggestion_view->set_size_columns_to_fit_content(true);
    m_suggestion_view->set_headers_visible(false);
    m_popup_window->set_main_widget(m_suggestion_view);
}

Locator::~Locator()
{
}

void Locator::open()
{
    m_textbox->set_focus(true);
    if (!m_textbox->text().is_empty()) {
        m_textbox->select_all();
        m_popup_window->show();
    }
}

void Locator::close()
{
    m_popup_window->hide();
}

void Locator::keydown_event(GKeyEvent& event)
{
    GWidget::keydown_event(event);
}

void Locator::update_suggestions()
{
    auto typed_text = m_textbox->text();
    Vector<String> suggestions;
    g_project->for_each_text_file([&](auto& file) {
        if (file.name().contains(typed_text))
            suggestions.append(file.name());
    });
    dbg() << "I have " << suggestions.size() << " suggestion(s):";
    for (auto& s : suggestions) {
        dbg() << "    " << s;
    }

    m_suggestion_view->set_model(adopt(*new LocatorSuggestionModel(move(suggestions))));

    m_popup_window->move_to(screen_relative_rect().top_left().translated(0, -m_popup_window->height()));
    dbg() << "Popup rect: " << m_popup_window->rect();
    m_popup_window->show();
}
