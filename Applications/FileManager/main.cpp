#include "DirectoryView.h"
#include "FileUtils.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CUserInfo.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

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
    window->set_title("File Manager");
    window->set_rect(20, 200, 640, 480);

    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto* main_toolbar = new GToolBar(widget);
    auto* location_toolbar = new GToolBar(widget);
    location_toolbar->layout()->set_margins({ 6, 3, 6, 3 });
    location_toolbar->set_preferred_size(0, 25);

    auto location_label = GLabel::construct("Location: ", location_toolbar);
    location_label->size_to_fit();

    auto location_textbox = GTextEditor::construct(GTextEditor::SingleLine, location_toolbar);

    auto* splitter = new GSplitter(Orientation::Horizontal, widget);
    auto* tree_view = new GTreeView(splitter);
    auto file_system_model = GFileSystemModel::create("/", GFileSystemModel::Mode::DirectoriesOnly);
    tree_view->set_model(file_system_model);
    tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    tree_view->set_preferred_size(200, 0);
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

    tree_view->on_selection_change = [&] {
        auto path = file_system_model->path(tree_view->selection().first());
        if (directory_view->path() == path)
            return;
        directory_view->open(path);
    };

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [directory_view](const GAction&) {
        directory_view->open_parent_directory();
    });

    auto mkdir_action = GAction::create("New directory...", GraphicsBitmap::load_from_file("/res/icons/16x16/mkdir.png"), [&](const GAction&) {
        GInputBox input_box("Enter name:", "New directory", window);
        if (input_box.exec() == GInputBox::ExecOK && !input_box.text_value().is_empty()) {
            auto new_dir_path = canonicalized_path(
                String::format("%s/%s",
                    directory_view->path().characters(),
                    input_box.text_value().characters()));
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                GMessageBox::show(String::format("mkdir(\"%s\") failed: %s", new_dir_path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            } else {
                file_system_model->update();

                auto current_path = directory_view->path();

                // not exactly sure why i have to reselect the root node first, but the index() fails if I dont
                auto root_index = file_system_model->index(file_system_model->root_path());
                tree_view->selection().set(root_index);

                // reselect the existing folder in the tree
                auto new_index = file_system_model->index(current_path);
                tree_view->selection().set(new_index);
                tree_view->scroll_into_view(new_index, Orientation::Vertical);
                tree_view->update();

                directory_view->refresh();
            }
        }
    });

    RefPtr<GAction> view_as_table_action;
    RefPtr<GAction> view_as_icons_action;

    view_as_table_action = GAction::create("Table view", { Mod_Ctrl, KeyCode::Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/table-view.png"), [&](const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::List);
        view_as_table_action->set_checked(true);
    });
    view_as_table_action->set_checkable(true);

    view_as_icons_action = GAction::create("Icon view", { Mod_Ctrl, KeyCode::Key_I }, GraphicsBitmap::load_from_file("/res/icons/16x16/icon-view.png"), [&](const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::Icon);
        view_as_icons_action->set_checked(true);
    });
    view_as_icons_action->set_checkable(true);

    auto view_type_action_group = make<GActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(*view_as_table_action);
    view_type_action_group->add_action(*view_as_icons_action);

    view_as_icons_action->set_checked(true);

    auto selected_file_paths = [&] {
        Vector<String> paths;
        auto& view = directory_view->current_view();
        auto& model = *view.model();
        view.selection().for_each_index([&](const GModelIndex& index) {
            auto name_index = model.index(index.row(), GDirectoryModel::Column::Name);
            auto path = model.data(name_index, GModel::Role::Custom).to_string();
            paths.append(path);
        });
        return paths;
    };

    auto copy_action = GCommonActions::make_copy_action([&](const GAction&) {
        auto paths = selected_file_paths();
        if (paths.is_empty())
            return;
        StringBuilder copy_text;
        for (auto& path : paths) {
            copy_text.appendf("%s\n", path.characters());
        }
        GClipboard::the().set_data(copy_text.build(), "file-list");
    });
    copy_action->set_enabled(false);

    auto paste_action = GCommonActions::make_paste_action([&](const GAction&) {
        auto data_and_type = GClipboard::the().data_and_type();
        if (data_and_type.type != "file-list") {
            dbg() << "Cannot paste clipboard type " << data_and_type.type;
            return;
        }
        auto copied_lines = data_and_type.data.split('\n');
        if (copied_lines.is_empty()) {
            dbg() << "No files to paste";
            return;
        }
        for (auto& current_path : copied_lines) {
            if (current_path.is_empty())
                continue;
            auto current_directory = directory_view->path();
            auto new_path = String::format("%s/%s",
                current_directory.characters(),
                FileSystemPath(current_path).basename().characters());
            if (!FileUtils::copy_file_or_directory(current_path, new_path)) {
                auto error_message = String::format("Could not paste %s.",
                    current_path.characters());
                GMessageBox::show(error_message, "File Manager", GMessageBox::Type::Error);
            }
        }
    });
    paste_action->set_enabled(GClipboard::the().type() == "file-list");

    GClipboard::the().on_content_change = [&](const String& data_type) {
        paste_action->set_enabled(data_type == "file-list");
    };

    auto properties_action
        = GAction::create("Properties...", { Mod_Alt, Key_Return }, GraphicsBitmap::load_from_file("/res/icons/16x16/properties.png"), [](auto&) {});

    enum class ConfirmBeforeDelete { No, Yes };

    auto do_delete = [&](ConfirmBeforeDelete confirm) {
        auto paths = selected_file_paths();
        if (paths.is_empty())
            return;
        {
            String message;
            if (paths.size() == 1) {
                message = String::format("Really delete %s?", FileSystemPath(paths[0]).basename().characters());
            } else {
                message = String::format("Really delete %d files?", paths.size());
            }

            if (confirm == ConfirmBeforeDelete::Yes) {
                GMessageBox box(
                    message,
                    "Confirm deletion",
                    GMessageBox::Type::Warning,
                    GMessageBox::InputType::OKCancel,
                    window);
                auto result = box.exec();
                if (result == GMessageBox::ExecCancel)
                    return;
            }
        }
        for (auto& path : paths) {
            if (unlink(path.characters()) < 0) {
                int saved_errno = errno;
                GMessageBox::show(
                    String::format("unlink(%s) failed: %s", path.characters(), strerror(saved_errno)),
                    "Delete failed",
                    GMessageBox::Type::Error,
                    GMessageBox::InputType::OK,
                    window);
                break;
            }
        }
    };

    auto force_delete_action = GAction::create("Delete without confirmation", { Mod_Shift, Key_Delete }, [&](const GAction&) {
        do_delete(ConfirmBeforeDelete::No);
    });

    auto delete_action = GCommonActions::make_delete_action([&](const GAction&) {
        do_delete(ConfirmBeforeDelete::Yes);
    });
    delete_action->set_enabled(false);

    auto go_back_action = GAction::create("Go Back", { Mod_Alt, Key_Left }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-back.png"), [directory_view](const GAction&) {
        dbgprintf("'Go Back' action activated!\n");
        directory_view->open_previous_directory();
    });

    auto go_forward_action = GAction::create("Go Forward", { Mod_Alt, Key_Right }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [directory_view](const GAction&) {
        dbgprintf("'Go Forward' action activated!\n");
        directory_view->open_next_directory();
    });

    auto go_home_action = GAction::create("Go to Home Directory", GraphicsBitmap::load_from_file("/res/icons/16x16/go-home.png"), [directory_view](auto&) {
        directory_view->open(get_current_user_home_path());
    });

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("File Manager");
    app_menu->add_action(mkdir_action);
    app_menu->add_action(copy_action);
    app_menu->add_action(paste_action);
    app_menu->add_action(delete_action);
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto view_menu = make<GMenu>("View");
    view_menu->add_action(*view_as_icons_action);
    view_menu->add_action(*view_as_table_action);
    menubar->add_menu(move(view_menu));

    auto go_menu = make<GMenu>("Go");
    go_menu->add_action(go_back_action);
    go_menu->add_action(go_forward_action);
    go_menu->add_action(open_parent_directory_action);
    go_menu->add_action(go_home_action);
    menubar->add_menu(move(go_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    main_toolbar->add_action(go_back_action);
    main_toolbar->add_action(go_forward_action);
    main_toolbar->add_action(open_parent_directory_action);
    main_toolbar->add_action(go_home_action);

    main_toolbar->add_separator();
    main_toolbar->add_action(mkdir_action);
    main_toolbar->add_action(copy_action);
    main_toolbar->add_action(paste_action);
    main_toolbar->add_action(delete_action);

    main_toolbar->add_separator();
    main_toolbar->add_action(*view_as_icons_action);
    main_toolbar->add_action(*view_as_table_action);

    directory_view->on_path_change = [&](const String& new_path) {
        window->set_title(String::format("File Manager: %s", new_path.characters()));
        location_textbox->set_text(new_path);
        auto new_index = file_system_model->index(new_path);
        tree_view->selection().set(new_index);
        tree_view->scroll_into_view(new_index, Orientation::Vertical);
        tree_view->update();

        go_forward_action->set_enabled(directory_view->path_history_position()
            < directory_view->path_history_size() - 1);
        go_back_action->set_enabled(directory_view->path_history_position() > 0);
    };

    directory_view->on_status_message = [statusbar](const StringView& message) {
        statusbar->set_text(message);
    };

    directory_view->on_thumbnail_progress = [&](int done, int total) {
        if (done == total) {
            progressbar->set_visible(false);
            return;
        }
        progressbar->set_range(0, total);
        progressbar->set_value(done);
        progressbar->set_visible(true);
    };

    directory_view->on_selection_change = [&](GAbstractView& view) {
        // FIXME: Figure out how we can enable/disable the paste action, based on clipboard contents.
        copy_action->set_enabled(!view.selection().is_empty());
        delete_action->set_enabled(!view.selection().is_empty());
    };

    auto context_menu = make<GMenu>();
    context_menu->add_action(copy_action);
    context_menu->add_action(paste_action);
    context_menu->add_action(delete_action);
    context_menu->add_separator();
    context_menu->add_action(properties_action);

    directory_view->on_context_menu_request = [&](const GAbstractView&, const GModelIndex&, const GContextMenuEvent& event) {
        context_menu->popup(event.screen_position());
    };

    // our initial location is defined as, in order of precedence:
    // 1. the first command-line argument (e.g. FileManager /bin)
    // 2. the user's home directory
    // 3. the root directory

    String initial_location;

    if (argc >= 2)
        initial_location = argv[1];

    if (initial_location.is_empty())
        initial_location = get_current_user_home_path();

    if (initial_location.is_empty())
        initial_location = "/";

    directory_view->open(initial_location);
    directory_view->set_focus(true);

    window->set_main_widget(widget);
    window->show();

    window->set_icon(load_png("/res/icons/16x16/filetype-folder.png"));

    return app.exec();
}
