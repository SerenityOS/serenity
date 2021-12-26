#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GSplitter.h>
#include <AK/FileSystemPath.h>
#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include "DirectoryView.h"

int main(int argc, char** argv)
{
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("FileManager");
    window->set_rect(20, 200, 640, 480);
    window->set_should_exit_event_loop_on_close(true);

    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto* main_toolbar = new GToolBar(widget);
    auto* location_toolbar = new GToolBar(widget);
    location_toolbar->layout()->set_margins({ 6, 3, 6, 3 });
    location_toolbar->set_preferred_size({ 0, 25 });

    auto* location_label = new GLabel("Location: ", location_toolbar);
    location_label->size_to_fit();

    auto* location_textbox = new GTextEditor(GTextEditor::SingleLine, location_toolbar);

    auto* splitter = new GSplitter(Orientation::Horizontal, widget);
    auto* tree_view = new GTreeView(splitter);
    auto file_system_model = GFileSystemModel::create("/", GFileSystemModel::Mode::DirectoriesOnly);
    tree_view->set_model(file_system_model.copy_ref());
    tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    tree_view->set_preferred_size({ 200, 0 });
    auto* directory_view = new DirectoryView(splitter);

    auto* statusbar = new GStatusBar(widget);

    auto* progressbar = new GProgressBar(statusbar);
    progressbar->set_caption("Generating thumbnails: ");
    progressbar->set_format(GProgressBar::Format::ValueSlashMax);
    progressbar->set_visible(false);
    progressbar->set_frame_shape(FrameShape::Panel);
    progressbar->set_frame_shadow(FrameShadow::Sunken);
    progressbar->set_frame_thickness(1);

    location_textbox->on_return_pressed = [directory_view, location_textbox] {
        directory_view->open(location_textbox->text());
    };

    file_system_model->on_selection_changed = [&] (auto& index) {
        directory_view->open(file_system_model->path(index));
    };

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [directory_view] (const GAction&) {
        directory_view->open_parent_directory();
    });

    auto mkdir_action = GAction::create("New directory...", GraphicsBitmap::load_from_file("/res/icons/16x16/mkdir.png"), [&] (const GAction&) {
        GInputBox input_box("Enter name:", "New directory", window);
        if (input_box.exec() == GInputBox::ExecOK && !input_box.text_value().is_empty()) {
            auto new_dir_path = FileSystemPath(String::format("%s/%s",
                directory_view->path().characters(),
                input_box.text_value().characters()
            )).string();
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                GMessageBox::show(String::format("mkdir(\"%s\") failed: %s", new_dir_path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, window);
            } else {
                directory_view->refresh();
            }
        }
    });

    RetainPtr<GAction> view_as_table_action;
    RetainPtr<GAction> view_as_icons_action;

    view_as_table_action = GAction::create("Table view", { Mod_Ctrl, KeyCode::Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/table-view.png"), [&] (const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::List);
        view_as_icons_action->set_checked(false);
        view_as_table_action->set_checked(true);
    });
    view_as_table_action->set_checkable(true);
    view_as_table_action->set_checked(false);

    view_as_icons_action = GAction::create("Icon view", { Mod_Ctrl, KeyCode::Key_I }, GraphicsBitmap::load_from_file("/res/icons/16x16/icon-view.png"), [&] (const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::Icon);
        view_as_table_action->set_checked(false);
        view_as_icons_action->set_checked(true);
    });
    view_as_icons_action->set_checkable(true);
    view_as_icons_action->set_checked(true);

    auto copy_action = GAction::create("Copy", GraphicsBitmap::load_from_file("/res/icons/16x16/edit-copy.png"), [] (const GAction&) {
        dbgprintf("'Copy' action activated!\n");
    });

    auto delete_action = GAction::create("Delete", GraphicsBitmap::load_from_file("/res/icons/16x16/delete.png"), [] (const GAction&) {
        dbgprintf("'Delete' action activated!\n");
    });

    auto go_back_action = GAction::create("Go Back", GraphicsBitmap::load_from_file("/res/icons/16x16/go-back.png"), [] (const GAction&) {
        dbgprintf("'Go Back' action activated!\n");
    });

    auto go_forward_action = GAction::create("Go Forward", GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [] (const GAction&) {
        dbgprintf("'Go Forward' action activated!\n");
    });

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("FileManager");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(mkdir_action.copy_ref());
    file_menu->add_action(copy_action.copy_ref());
    file_menu->add_action(delete_action.copy_ref());
    menubar->add_menu(move(file_menu));

    auto view_menu = make<GMenu>("View");
    view_menu->add_action(*view_as_icons_action);
    view_menu->add_action(*view_as_table_action);
    menubar->add_menu(move(view_menu));

    auto go_menu = make<GMenu>("Go");
    go_menu->add_action(go_back_action.copy_ref());
    go_menu->add_action(go_forward_action.copy_ref());
    go_menu->add_action(open_parent_directory_action.copy_ref());

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    main_toolbar->add_action(go_back_action.copy_ref());
    main_toolbar->add_action(go_forward_action.copy_ref());
    main_toolbar->add_action(open_parent_directory_action.copy_ref());

    main_toolbar->add_separator();
    main_toolbar->add_action(mkdir_action.copy_ref());
    main_toolbar->add_action(copy_action.copy_ref());
    main_toolbar->add_action(delete_action.copy_ref());

    main_toolbar->add_separator();
    main_toolbar->add_action(*view_as_icons_action);
    main_toolbar->add_action(*view_as_table_action);

    directory_view->on_path_change = [window, location_textbox, &file_system_model, tree_view] (const String& new_path) {
        window->set_title(String::format("FileManager: %s", new_path.characters()));
        location_textbox->set_text(new_path);
        file_system_model->set_selected_index(file_system_model->index(new_path));
        tree_view->scroll_into_view(file_system_model->selected_index(), Orientation::Vertical);
        tree_view->update();
    };

    directory_view->on_status_message = [statusbar] (String message) {
        statusbar->set_text(move(message));
    };

    directory_view->on_thumbnail_progress = [&] (int done, int total) {
        if (done == total) {
            progressbar->set_visible(false);
            return;
        }
        progressbar->set_range(0, total);
        progressbar->set_value(done);
        progressbar->set_visible(true);
    };

    directory_view->open("/");
    directory_view->set_focus(true);

    window->set_main_widget(widget);
    window->show();

    window->set_icon_path("/res/icons/16x16/filetype-folder.png");

    return app.exec();
}
