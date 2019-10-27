#include "CppLexer.h"
#include "Editor.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "Project.h"
#include "TerminalWrapper.h"
#include <LibCore/CFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <unistd.h>

NonnullRefPtrVector<EditorWrapper> g_all_editor_wrappers;
RefPtr<EditorWrapper> g_current_editor_wrapper;

String g_currently_open_file;
OwnPtr<Project> g_project;
RefPtr<GWindow> g_window;
RefPtr<GListView> g_project_list_view;

void add_new_editor(GWidget& parent)
{
    auto wrapper = EditorWrapper::construct(&parent);
    g_current_editor_wrapper = wrapper;
    g_all_editor_wrappers.append(wrapper);
}

EditorWrapper& current_editor_wrapper()
{
    ASSERT(g_current_editor_wrapper);
    return *g_current_editor_wrapper;
}

Editor& current_editor()
{
    return current_editor_wrapper().editor();
}

static void build(TerminalWrapper&);
static void run(TerminalWrapper&);
void open_file(const String&);

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    g_window = GWindow::construct();
    g_window->set_rect(100, 100, 800, 600);
    g_window->set_title("HackStudio");

    auto widget = GWidget::construct();
    g_window->set_main_widget(widget);

    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    if (chdir("/home/anon/little") < 0) {
        perror("chdir");
        return 1;
    }
    g_project = Project::load_from_file("little.files");
    ASSERT(g_project);

    auto toolbar = GToolBar::construct(widget);

    auto outer_splitter = GSplitter::construct(Orientation::Horizontal, widget);
    g_project_list_view = GListView::construct(outer_splitter);
    g_project_list_view->set_model(g_project->model());
    g_project_list_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    g_project_list_view->set_preferred_size(200, 0);

    auto inner_splitter = GSplitter::construct(Orientation::Vertical, outer_splitter);
    inner_splitter->layout()->set_margins({ 0, 3, 0, 0 });
    add_new_editor(inner_splitter);
    add_new_editor(inner_splitter);

    auto new_action = GAction::create("Add new file to project...", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [&](const GAction&) {
        auto input_box = GInputBox::construct("Enter name of new file:", "Add new file to project", g_window);
        if (input_box->exec() == GInputBox::ExecCancel)
            return;
        auto filename = input_box->text_value();
        auto file = CFile::construct(filename);
        if (!file->open((CIODevice::OpenMode)(CIODevice::WriteOnly | CIODevice::MustBeNew))) {
            GMessageBox::show(String::format("Failed to create '%s'", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            return;
        }
        if (!g_project->add_file(filename)) {
            GMessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            // FIXME: Should we unlink the file here maybe?
            return;
        }
        open_file(filename);
    });

    auto add_existing_file_action = GAction::create("Add existing file to project...", GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), [&](auto&) {
        auto result = GFilePicker::get_open_filepath("Add existing file to project");
        if (!result.has_value())
            return;
        auto& filename = result.value();
        if (!g_project->add_file(filename)) {
            GMessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            return;
        }
        open_file(filename);
    });

    auto switch_to_next_editor = GAction::create("Switch to next editor", { Mod_Ctrl, Key_E }, [&](auto&) {
        if (g_all_editor_wrappers.size() <= 1)
            return;
        // FIXME: This will only work correctly when there are 2 editors. Make it work for any editor count.
        for (auto& wrapper : g_all_editor_wrappers) {
            if (&wrapper == &current_editor_wrapper())
                continue;
            wrapper.editor().set_focus(true);
            return;
        }
    });

    auto save_action = GAction::create("Save", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&](auto&) {
        if (g_currently_open_file.is_empty())
            return;
        current_editor().write_to_file(g_currently_open_file);
    });

    toolbar->add_action(new_action);
    toolbar->add_action(add_existing_file_action);
    toolbar->add_action(save_action);
    toolbar->add_separator();

    toolbar->add_action(GCommonActions::make_cut_action([&](auto&) { current_editor().cut_action().activate(); }));
    toolbar->add_action(GCommonActions::make_copy_action([&](auto&) { current_editor().copy_action().activate(); }));
    toolbar->add_action(GCommonActions::make_paste_action([&](auto&) { current_editor().paste_action().activate(); }));
    toolbar->add_separator();
    toolbar->add_action(GCommonActions::make_undo_action([&](auto&) { current_editor().undo_action().activate(); }));
    toolbar->add_action(GCommonActions::make_redo_action([&](auto&) { current_editor().redo_action().activate(); }));
    toolbar->add_separator();

    g_project_list_view->on_activation = [&](auto& index) {
        auto filename = g_project_list_view->model()->data(index).to_string();
        open_file(filename);
    };

    auto tab_widget = GTabWidget::construct(inner_splitter);

    tab_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    tab_widget->set_preferred_size(0, 24);

    auto reveal_action_tab = [&](auto& widget) {
        dbg() << "tab_widget preferred height: " << tab_widget->preferred_size().height();
        if (tab_widget->preferred_size().height() < 200)
            tab_widget->set_preferred_size(0, 200);
        tab_widget->set_active_widget(widget);
    };

    auto hide_action_tabs = [&] {
        tab_widget->set_preferred_size(0, 24);
    };

    auto hide_action_tabs_action = GAction::create("Hide action tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [&](auto&) {
        hide_action_tabs();
    });

    auto find_in_files_widget = FindInFilesWidget::construct(nullptr);
    tab_widget->add_widget("Find in files", find_in_files_widget);

    auto terminal_wrapper = TerminalWrapper::construct(nullptr);
    tab_widget->add_widget("Console", terminal_wrapper);

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("HackStudio");
    app_menu->add_action(save_action);
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto project_menu = make<GMenu>("Project");
    project_menu->add_action(new_action);
    project_menu->add_action(add_existing_file_action);
    menubar->add_menu(move(project_menu));

    auto edit_menu = make<GMenu>("Edit");
    edit_menu->add_action(GAction::create("Find in files...", { Mod_Ctrl | Mod_Shift, Key_F }, [&](auto&) {
        reveal_action_tab(find_in_files_widget);
        find_in_files_widget->focus_textbox_and_select_all();
    }));
    menubar->add_menu(move(edit_menu));

    auto build_action = GAction::create("Build", { Mod_Ctrl, Key_B }, GraphicsBitmap::load_from_file("/res/icons/16x16/build.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        build(terminal_wrapper);
    });
    toolbar->add_action(build_action);

    auto run_action = GAction::create("Run", { Mod_Ctrl, Key_R }, GraphicsBitmap::load_from_file("/res/icons/16x16/run.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        run(terminal_wrapper);
    });
    toolbar->add_action(run_action);

    auto build_menu = make<GMenu>("Build");
    build_menu->add_action(build_action);
    build_menu->add_action(run_action);
    menubar->add_menu(move(build_menu));

    auto view_menu = make<GMenu>("View");
    view_menu->add_action(hide_action_tabs_action);
    menubar->add_menu(move(view_menu));

    auto small_icon = GraphicsBitmap::load_from_file("/res/icons/16x16/app-hack-studio.png");

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](auto&) {
        GAboutDialog::show("HackStudio", small_icon, g_window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    g_window->set_icon(small_icon);

    g_window->show();

    open_file("main.cpp");
    return app.exec();
}

void build(TerminalWrapper& wrapper)
{
    wrapper.run_command("make");
}

void run(TerminalWrapper& wrapper)
{
    wrapper.run_command("make run");
}

struct TextStyle {
    Color color;
    const Font* font { nullptr };
};

static TextStyle style_for_token_type(CppToken::Type type)
{
    switch (type) {
    case CppToken::Type::Keyword:
        return { Color::Black, &Font::default_bold_fixed_width_font() };
    case CppToken::Type::KnownType:
        return { Color::from_rgb(0x929200), &Font::default_bold_fixed_width_font() };
    case CppToken::Type::Identifier:
        return { Color::from_rgb(0x000092) };
    case CppToken::Type::DoubleQuotedString:
    case CppToken::Type::SingleQuotedString:
    case CppToken::Type::Number:
        return { Color::from_rgb(0x920000) };
    case CppToken::Type::PreprocessorStatement:
        return { Color::from_rgb(0x009292) };
    case CppToken::Type::Comment:
        return { Color::from_rgb(0x009200) };
    default:
        return { Color::Black };
    }
}

static void rehighlight()
{
    auto text = current_editor().text();
    CppLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GTextEditor::Span> spans;
    for (auto& token : tokens) {
#ifdef DEBUG_SYNTAX_HIGHLIGHTING
        dbg() << token.to_string() << " @ " << token.m_start.line << ":" << token.m_start.column << " - " << token.m_end.line << ":" << token.m_end.column;
#endif
        GTextEditor::Span span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(token.m_type);
        span.color = style.color;
        span.font = style.font;
        spans.append(span);
    }
    current_editor().set_spans(spans);
    current_editor().update();
}

void open_file(const String& filename)
{
    auto file = CFile::construct(filename);
    if (!file->open(CFile::ReadOnly)) {
        GMessageBox::show("Could not open!", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
        return;
    }
    auto contents = file->read_all();
    current_editor().set_text(contents);

    if (filename.ends_with(".cpp")) {
        current_editor().on_change = [] { rehighlight(); };
        rehighlight();
    } else {
        current_editor().on_change = nullptr;
    }

    g_currently_open_file = filename;
    g_window->set_title(String::format("%s - HackStudio", g_currently_open_file.characters()));
    g_project_list_view->update();

    current_editor_wrapper().filename_label().set_text(filename);

    current_editor().set_focus(true);
}
