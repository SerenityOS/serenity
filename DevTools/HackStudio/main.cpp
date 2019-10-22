#include "Project.h"
#include "TerminalWrapper.h"
#include <LibCore/CFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <unistd.h>

String g_currently_open_file;

static void build(TerminalWrapper&);
static void run(TerminalWrapper&);

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_rect(100, 100, 800, 600);
    window->set_title("HackStudio");

    auto widget = GWidget::construct();
    window->set_main_widget(widget);

    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    if (chdir("/home/anon/little") < 0) {
        perror("chdir");
        return 1;
    }
    auto project = Project::load_from_file("little.files");
    ASSERT(project);

    auto toolbar = GToolBar::construct(widget);

    auto outer_splitter = GSplitter::construct(Orientation::Horizontal, widget);
    auto project_list_view = GListView::construct(outer_splitter);
    project_list_view->set_model(project->model());
    project_list_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    project_list_view->set_preferred_size(200, 0);

    auto inner_splitter = GSplitter::construct(Orientation::Vertical, outer_splitter);
    auto text_editor = GTextEditor::construct(GTextEditor::MultiLine, inner_splitter);
    text_editor->set_ruler_visible(true);

    project_list_view->on_activation = [&](auto& index) {
        auto filename = project_list_view->model()->data(index).to_string();
        auto file = CFile::construct(filename);
        if (!file->open(CFile::ReadOnly)) {
            GMessageBox::show("Could not open!", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            return;
        }
        text_editor->set_text(file->read_all());
        g_currently_open_file = filename;
        window->set_title(String::format("%s - HackStudio", g_currently_open_file.characters()));
        project_list_view->update();
    };

    auto terminal_wrapper = TerminalWrapper::construct(inner_splitter);

    auto statusbar = GStatusBar::construct(widget);

    text_editor->on_cursor_change = [&] {
        statusbar->set_text(String::format("Line: %d, Column: %d", text_editor->cursor().line(), text_editor->cursor().column()));
    };

    text_editor->add_custom_context_menu_action(GAction::create(
        "Go to line...", { Mod_Ctrl, Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [&](auto&) {
            auto input_box = GInputBox::construct("Line:", "Go to line", window);
            auto result = input_box->exec();
            if (result == GInputBox::ExecOK) {
                bool ok;
                auto line_number = input_box->text_value().to_uint(ok);
                if (ok) {
                    text_editor->set_cursor(line_number - 1, 0);
                }
            }
        },
        text_editor));

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("HackStudio");
    app_menu->add_action(GAction::create("Save", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&](auto&) {
        if (g_currently_open_file.is_empty())
            return;
        text_editor->write_to_file(g_currently_open_file);
    }));
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto build_menu = make<GMenu>("Build");
    build_menu->add_action(GAction::create("Build", { Mod_Ctrl, Key_B }, [&](auto&) {
        build(terminal_wrapper);
    }));
    build_menu->add_action(GAction::create("Run", { Mod_Ctrl, Key_R }, [&](auto&) {
        run(terminal_wrapper);
    }));
    menubar->add_menu(move(build_menu));

    auto small_icon = GraphicsBitmap::load_from_file("/res/icons/16x16/app-hack-studio.png");

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](auto&) {
        GAboutDialog::show("HackStudio", small_icon, window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->set_icon(small_icon);

    window->show();
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
