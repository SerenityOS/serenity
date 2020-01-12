#include "DirectoryView.h"
#include "FileUtils.h"
#include "PropertiesDialog.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CUserInfo.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
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
    if (pledge("stdio thread unix shared_buffer cpath rpath wpath fattr proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    RefPtr<CConfigFile> config = CConfigFile::get_for_app("FileManager");

    GApplication app(argc, argv);

    if (pledge("stdio thread shared_buffer cpath rpath wpath fattr proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_title("File Manager");

    auto left = config->read_num_entry("Window", "Left", 150);
    auto top = config->read_num_entry("Window", "Top", 75);
    auto width = config->read_num_entry("Window", "Width", 640);
    auto heigth = config->read_num_entry("Window", "Heigth", 480);
    window->set_rect({ left, top, width, heigth });

    auto widget = GWidget::construct();
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto main_toolbar = GToolBar::construct(widget);
    auto location_toolbar = GToolBar::construct(widget);
    location_toolbar->layout()->set_margins({ 6, 3, 6, 3 });
    location_toolbar->set_preferred_size(0, 25);

    auto location_label = GLabel::construct("Location: ", location_toolbar);
    location_label->size_to_fit();

    auto location_textbox = GTextEditor::construct(GTextEditor::SingleLine, location_toolbar);

    auto splitter = GSplitter::construct(Orientation::Horizontal, widget);
    auto tree_view = GTreeView::construct(splitter);
    auto directories_model = GFileSystemModel::create("/", GFileSystemModel::Mode::DirectoriesOnly);
    tree_view->set_model(directories_model);
    tree_view->set_column_hidden(GFileSystemModel::Column::Icon, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::Size, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::Owner, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::Group, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::Permissions, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::ModificationTime, true);
    tree_view->set_column_hidden(GFileSystemModel::Column::Inode, true);
    tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    tree_view->set_preferred_size(150, 0);
    auto directory_view = DirectoryView::construct(splitter);

    auto statusbar = GStatusBar::construct(widget);

    auto progressbar = GProgressBar::construct(statusbar);
    progressbar->set_caption("Generating thumbnails: ");
    progressbar->set_format(GProgressBar::Format::ValueSlashMax);
    progressbar->set_visible(false);
    progressbar->set_frame_shape(FrameShape::Panel);
    progressbar->set_frame_shadow(FrameShadow::Sunken);
    progressbar->set_frame_thickness(1);

    location_textbox->on_return_pressed = [&] {
        directory_view->open(location_textbox->text());
    };

    auto refresh_tree_view = [&] {
        directories_model->update();

        auto current_path = directory_view->path();

        struct stat st;
        // If the directory no longer exists, we find a parent that does.
        while (lstat(current_path.characters(), &st) != 0) {
            directory_view->open_parent_directory();
            current_path = directory_view->path();
            if (current_path == directories_model->root_path()) {
                break;
            }
        }

        // Reselect the existing folder in the tree.
        auto new_index = directories_model->index(current_path, GFileSystemModel::Column::Name);
        tree_view->selection().set(new_index);
        tree_view->scroll_into_view(new_index, Orientation::Vertical);
        tree_view->update();

        directory_view->refresh();
    };

    auto directory_context_menu = GMenu::construct("Directory View Directory");
    auto file_context_menu = GMenu::construct("Directory View File");
    auto directory_view_context_menu = GMenu::construct("Directory View");
    auto tree_view_directory_context_menu = GMenu::construct("Tree View Directory");
    auto tree_view_context_menu = GMenu::construct("Tree View");

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [&](const GAction&) {
        directory_view->open_parent_directory();
    });

    auto mkdir_action = GAction::create("New directory...", { Mod_Ctrl | Mod_Shift, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/mkdir.png"), [&](const GAction&) {
        auto input_box = GInputBox::construct("Enter name:", "New directory", window);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto new_dir_path = canonicalized_path(
                String::format("%s/%s",
                    directory_view->path().characters(),
                    input_box->text_value().characters()));
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                GMessageBox::show(String::format("mkdir(\"%s\") failed: %s", new_dir_path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            } else {
                refresh_tree_view();
            }
        }
    });

    RefPtr<GAction> view_as_table_action;
    RefPtr<GAction> view_as_icons_action;
    RefPtr<GAction> view_as_columns_action;

    view_as_table_action = GAction::create("Table view", { Mod_Ctrl, KeyCode::Key_L }, GraphicsBitmap::load_from_file("/res/icons/16x16/table-view.png"), [&](const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::List);
        view_as_table_action->set_checked(true);

        config->write_entry("DirectoryView", "ViewMode", "List");
        config->sync();
    });
    view_as_table_action->set_checkable(true);

    view_as_icons_action = GAction::create("Icon view", { Mod_Ctrl, KeyCode::Key_I }, GraphicsBitmap::load_from_file("/res/icons/16x16/icon-view.png"), [&](const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::Icon);
        view_as_icons_action->set_checked(true);

        config->write_entry("DirectoryView", "ViewMode", "Icon");
        config->sync();
    });
    view_as_icons_action->set_checkable(true);

    view_as_columns_action = GAction::create("Columns view", GraphicsBitmap::load_from_file("/res/icons/16x16/columns-view.png"), [&](const GAction&) {
        directory_view->set_view_mode(DirectoryView::ViewMode::Columns);
        view_as_columns_action->set_checked(true);

        config->write_entry("DirectoryView", "ViewMode", "Columns");
        config->sync();
    });
    view_as_columns_action->set_checkable(true);

    auto view_type_action_group = make<GActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(*view_as_table_action);
    view_type_action_group->add_action(*view_as_icons_action);
    view_type_action_group->add_action(*view_as_columns_action);

    auto selected_file_paths = [&] {
        Vector<String> paths;
        auto& view = directory_view->current_view();
        auto& model = *view.model();
        view.selection().for_each_index([&](const GModelIndex& index) {
            auto parent_index = model.parent_index(index);
            auto name_index = model.index(index.row(), GFileSystemModel::Column::Name, parent_index);
            auto path = model.data(name_index, GModel::Role::Custom).to_string();
            paths.append(path);
        });
        return paths;
    };

    auto tree_view_selected_file_paths = [&] {
        Vector<String> paths;
        auto& view = tree_view;
        view->selection().for_each_index([&](const GModelIndex& index) {
            paths.append(directories_model->full_path(index));
        });
        return paths;
    };

    auto select_all_action = GAction::create("Select all", { Mod_Ctrl, KeyCode::Key_A }, [&](const GAction&) {
        directory_view->current_view().select_all();
    });

    auto copy_action = GCommonActions::make_copy_action([&](const GAction& action) {
        Vector<String> paths;
        if (action.activator() == directory_context_menu || directory_view->active_widget()->is_focused()) {
            paths = selected_file_paths();
        } else {
            paths = tree_view_selected_file_paths();
        }
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
            } else {
                refresh_tree_view();
            }
        }
    });
    paste_action->set_enabled(GClipboard::the().type() == "file-list");

    GClipboard::the().on_content_change = [&](const String& data_type) {
        paste_action->set_enabled(data_type == "file-list");
    };

    auto properties_action
        = GAction::create("Properties...", { Mod_Alt, Key_Return }, GraphicsBitmap::load_from_file("/res/icons/16x16/properties.png"), [&](const GAction& action) {
              auto& model = directory_view->model();
              String path;
              Vector<String> selected;
              if (action.activator() == directory_context_menu || directory_view->active_widget()->is_focused()) {
                  path = directory_view->path();
                  selected = selected_file_paths();
              } else {
                  path = directories_model->full_path(tree_view->selection().first());
                  selected = tree_view_selected_file_paths();
              }

              RefPtr<PropertiesDialog> properties;
              if (selected.is_empty()) {
                  properties = PropertiesDialog::construct(model, path, true, window);
              } else {
                  properties = PropertiesDialog::construct(model, selected.first(), false, window);
              }

              properties->exec();
          });

    enum class ConfirmBeforeDelete {
        No,
        Yes
    };

    auto do_delete = [&](ConfirmBeforeDelete confirm, const GAction& action) {
        Vector<String> paths;
        if (action.activator() == directory_context_menu || directory_view->active_widget()->is_focused()) {
            paths = selected_file_paths();
        } else {
            paths = tree_view_selected_file_paths();
        }
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
                auto result = GMessageBox::show(
                    message,
                    "Confirm deletion",
                    GMessageBox::Type::Warning,
                    GMessageBox::InputType::OKCancel,
                    window);
                if (result == GMessageBox::ExecCancel)
                    return;
            }
        }

        for (auto& path : paths) {
            struct stat st;
            if (lstat(path.characters(), &st)) {
                GMessageBox::show(
                    String::format("lstat(%s) failed: %s", path.characters(), strerror(errno)),
                    "Delete failed",
                    GMessageBox::Type::Error,
                    GMessageBox::InputType::OK,
                    window);
                break;
            } else {
                refresh_tree_view();
            }

            if (S_ISDIR(st.st_mode)) {
                String error_path;
                int error = FileUtils::delete_directory(path, error_path);

                if (error) {
                    GMessageBox::show(
                        String::format("Failed to delete directory \"%s\": %s", error_path.characters(), strerror(error)),
                        "Delete failed",
                        GMessageBox::Type::Error,
                        GMessageBox::InputType::OK,
                        window);
                    break;
                } else {
                    refresh_tree_view();
                }
            } else if (unlink(path.characters()) < 0) {
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

    auto force_delete_action = GAction::create("Delete without confirmation", { Mod_Shift, Key_Delete }, [&](const GAction& action) {
        do_delete(ConfirmBeforeDelete::No, action);
    });

    auto delete_action = GCommonActions::make_delete_action([&](const GAction& action) {
        do_delete(ConfirmBeforeDelete::Yes, action);
    });
    delete_action->set_enabled(false);

    auto go_back_action = GCommonActions::make_go_back_action([&](auto&) {
        directory_view->open_previous_directory();
    });

    auto go_forward_action = GCommonActions::make_go_forward_action([&](auto&) {
        directory_view->open_next_directory();
    });

    auto go_home_action = GCommonActions::make_go_home_action([&](auto&) {
        directory_view->open(get_current_user_home_path());
    });

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("File Manager");
    app_menu->add_action(mkdir_action);
    app_menu->add_action(copy_action);
    app_menu->add_action(paste_action);
    app_menu->add_action(delete_action);
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto view_menu = GMenu::construct("View");
    view_menu->add_action(*view_as_icons_action);
    view_menu->add_action(*view_as_table_action);
    view_menu->add_action(*view_as_columns_action);
    menubar->add_menu(move(view_menu));

    auto go_menu = GMenu::construct("Go");
    go_menu->add_action(go_back_action);
    go_menu->add_action(go_forward_action);
    go_menu->add_action(open_parent_directory_action);
    go_menu->add_action(go_home_action);
    menubar->add_menu(move(go_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("File Manager", load_png("/res/icons/32x32/filetype-folder.png"), window);
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
    main_toolbar->add_action(*view_as_columns_action);

    directory_view->on_path_change = [&](const String& new_path) {
        window->set_title(String::format("File Manager: %s", new_path.characters()));
        location_textbox->set_text(new_path);
        auto new_index = directories_model->index(new_path, GFileSystemModel::Column::Name);
        if (new_index.is_valid()) {
            tree_view->selection().set(new_index);
            tree_view->scroll_into_view(new_index, Orientation::Vertical);
            tree_view->update();
        }

        go_forward_action->set_enabled(directory_view->path_history_position()
            < directory_view->path_history_size() - 1);
        go_back_action->set_enabled(directory_view->path_history_position() > 0);
    };

    directory_view->on_status_message = [&](const StringView& message) {
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

    auto open_in_text_editor_action = GAction::create("Open in TextEditor...", GraphicsBitmap::load_from_file("/res/icons/TextEditor16.png"), [&](auto&) {
        for (auto& path : selected_file_paths()) {
            if (!fork()) {
                int rc = execl("/bin/TextEditor", "TextEditor", path.characters(), nullptr);
                if (rc < 0)
                    perror("execl");
                exit(1);
            }
        }
    });

    directory_context_menu->add_action(copy_action);
    directory_context_menu->add_action(paste_action);
    directory_context_menu->add_action(delete_action);
    directory_context_menu->add_separator();
    directory_context_menu->add_action(properties_action);

    file_context_menu->add_action(copy_action);
    file_context_menu->add_action(paste_action);
    file_context_menu->add_action(delete_action);
    file_context_menu->add_separator();
    file_context_menu->add_action(open_in_text_editor_action);
    file_context_menu->add_separator();
    file_context_menu->add_action(properties_action);

    directory_view_context_menu->add_action(mkdir_action);

    tree_view_directory_context_menu->add_action(copy_action);
    tree_view_directory_context_menu->add_action(paste_action);
    tree_view_directory_context_menu->add_action(delete_action);
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(properties_action);
    tree_view_directory_context_menu->add_separator();
    tree_view_directory_context_menu->add_action(mkdir_action);

    directory_view->on_context_menu_request = [&](const GAbstractView&, const GModelIndex& index, const GContextMenuEvent& event) {
        if (index.is_valid()) {
            auto& node = directory_view->model().node(index);

            if (node.is_directory())
                directory_context_menu->popup(event.screen_position());
            else
                file_context_menu->popup(event.screen_position());
        } else {
            directory_view_context_menu->popup(event.screen_position());
        }
    };

    tree_view->on_selection_change = [&] {
        auto path = directories_model->full_path(tree_view->selection().first());
        if (directory_view->path() == path)
            return;
        directory_view->open(path);
        copy_action->set_enabled(!tree_view->selection().is_empty());
        delete_action->set_enabled(!tree_view->selection().is_empty());
    };

    tree_view->on_context_menu_request = [&](const GModelIndex& index, const GContextMenuEvent& event) {
        if (index.is_valid()) {
            tree_view_directory_context_menu->popup(event.screen_position());
        }
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

    // Read direcory read mode from config.
    auto dir_view_mode = config->read_entry("DirectoryView", "ViewMode", "Icon");

    if (dir_view_mode.contains("List")) {
        directory_view->set_view_mode(DirectoryView::ViewMode::List);
        view_as_table_action->set_checked(true);
    } else if (dir_view_mode.contains("Columns")) {
        directory_view->set_view_mode(DirectoryView::ViewMode::Columns);
        view_as_columns_action->set_checked(true);
    } else {
        directory_view->set_view_mode(DirectoryView::ViewMode::Icon);
        view_as_icons_action->set_checked(true);
    }

    // Write window position to config file on close request.
    window->on_close_request = [&] {
        config->write_num_entry("Window", "Left", window->x());
        config->write_num_entry("Window", "Top", window->y());
        config->write_num_entry("Window", "Width", window->width());
        config->write_num_entry("Window", "Heigth", window->height());
        config->sync();

        return GWindow::CloseRequestDecision::Close;
    };

    return app.exec();
}
