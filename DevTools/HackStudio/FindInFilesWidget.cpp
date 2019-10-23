#include "FindInFilesWidget.h"
#include "Project.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GTextBox.h>

extern void open_file(const String&);
extern OwnPtr<Project> g_project;

struct FilenameAndLineNumber {
    String filename;
    int line_number { -1 };
};

class SearchResultsModel final : public GModel {
public:
    explicit SearchResultsModel(const Vector<FilenameAndLineNumber>&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(const GModelIndex& = GModelIndex()) const override { return m_matches.size(); }
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return 1; }
    virtual GVariant data(const GModelIndex& index, Role role = Role::Display) const override
    {
        if (role == Role::Display) {
            auto& match = m_matches.at(index.row());
            return String::format("%s:%d", match.filename.characters(), match.line_number);
        }
        return {};
    }
    virtual void update() override {}

private:
    Vector<FilenameAndLineNumber> m_matches;
};

static RefPtr<SearchResultsModel> find_in_files(const StringView& text)
{
    Vector<FilenameAndLineNumber> matches;
    g_project->for_each_text_file([&](auto& file) {
        auto matches_in_file = file.find(text);
        for (int match : matches_in_file) {
            matches.append({ file.name(), match });
        }
    });

    return adopt(*new SearchResultsModel(move(matches)));
}

FindInFilesWidget::FindInFilesWidget(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    m_textbox = GTextBox::construct(this);
    m_textbox->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_textbox->set_preferred_size(0, 20);
    m_button = GButton::construct("Find in files", this);
    m_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_button->set_preferred_size(0, 20);

    m_result_view = GListView::construct(this);

    m_result_view->on_activation = [this](auto& index) {
        auto match_string = m_result_view->model()->data(index).to_string();
        auto parts = match_string.split(':');
        ASSERT(parts.size() == 2);
        bool ok;
        int line_number = parts[1].to_int(ok);
        ASSERT(ok);
        open_file(parts[0]);
        m_textbox->set_cursor(line_number - 1, 0);
        m_textbox->set_focus(true);
    };

    m_button->on_click = [this](auto&) {
        auto results_model = find_in_files(m_textbox->text());
        m_result_view->set_model(results_model);
    };
    m_textbox->on_return_pressed = [this] {
        m_button->click();
    };
}

void FindInFilesWidget::focus_textbox_and_select_all()
{
    m_textbox->select_all();
    m_textbox->set_focus(true);
}
