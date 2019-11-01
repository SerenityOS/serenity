#include "FindInFilesWidget.h"
#include "Project.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GTextBox.h>

extern GTextEditor& current_editor();
extern void open_file(const String&);
extern OwnPtr<Project> g_project;

struct FilenameAndRange {
    String filename;
    GTextRange range;
};

class SearchResultsModel final : public GModel {
public:
    explicit SearchResultsModel(const Vector<FilenameAndRange>&& matches)
        : m_matches(move(matches))
    {
    }

    virtual int row_count(const GModelIndex& = GModelIndex()) const override { return m_matches.size(); }
    virtual int column_count(const GModelIndex& = GModelIndex()) const override { return 1; }
    virtual GVariant data(const GModelIndex& index, Role role = Role::Display) const override
    {
        if (role == Role::Display) {
            auto& match = m_matches.at(index.row());
            return String::format("%s: (%d,%d - %d,%d)",
                match.filename.characters(),
                match.range.start().line() + 1,
                match.range.start().column(),
                match.range.end().line() + 1,
                match.range.end().column());
        }
        return {};
    }
    virtual void update() override {}
    virtual GModelIndex index(int row, int column = 0, const GModelIndex& = GModelIndex()) const override { return create_index(row, column, &m_matches.at(row)); }

private:
    Vector<FilenameAndRange> m_matches;
};

static RefPtr<SearchResultsModel> find_in_files(const StringView& text)
{
    Vector<FilenameAndRange> matches;
    g_project->for_each_text_file([&](auto& file) {
        auto matches_in_file = file.document().find_all(text);
        for (auto& range : matches_in_file) {
            matches.append({ file.name(), range });
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

    m_result_view->on_activation = [](auto& index) {
        auto& match = *(const FilenameAndRange*)index.internal_data();
        open_file(match.filename);
        current_editor().set_selection(match.range);
        current_editor().set_focus(true);
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
