#include "Project.h"
#include <LibCore/CFile.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GListView.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <unistd.h>

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

    if (chdir("/home/anon/serenity") < 0) {
        perror("chdir");
        return 1;
    }
    auto project = Project::load_from_file("serenity.files");
    ASSERT(project);

    auto toolbar = GToolBar::construct(widget);

    auto splitter = GSplitter::construct(Orientation::Horizontal, widget);
    auto project_list_view = GListView::construct(splitter);
    project_list_view->set_model(project->model());
    project_list_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    project_list_view->set_preferred_size(200, 0);

    auto text_editor = GTextEditor::construct(GTextEditor::MultiLine, splitter);
    text_editor->set_ruler_visible(true);

    project_list_view->on_activation = [&](auto& index) {
        auto filename = project_list_view->model()->data(index).to_string();
        auto file = CFile::construct(filename);
        if (!file->open(CFile::ReadOnly)) {
            GMessageBox::show("Could not open!", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            return;
        }
        text_editor->set_text(file->read_all());
    };

    auto statusbar = GStatusBar::construct(widget);

    text_editor->on_cursor_change = [&] {
        statusbar->set_text(String::format("Line: %d, Column: %d", text_editor->cursor().line(), text_editor->cursor().column()));
    };

    text_editor->add_custom_context_menu_action(GAction::create("Go to line...", { Mod_Ctrl, Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [&](auto&) {
        auto input_box = GInputBox::construct("Line:", "Go to line", window);
        auto result = input_box->exec();
        if (result == GInputBox::ExecOK) {
            bool ok;
            auto line_number = input_box->text_value().to_uint(ok);
            if (ok) {
                text_editor->set_cursor(line_number, 0);
            }
        }
    }));

    window->show();
    return app.exec();
}
