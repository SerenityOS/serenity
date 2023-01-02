/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CommonLocationsProvider.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FilePickerDialogGML.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/MultiView.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Tray.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <string.h>
#include <unistd.h>

namespace GUI {

ErrorOr<Optional<DeprecatedString>> FilePicker::get_open_filepath(Window* parent_window, DeprecatedString const& window_title, StringView path, bool folder, ScreenPosition screen_position)
{
    auto picker = TRY(FilePicker::try_create(parent_window, folder ? Mode::OpenFolder : Mode::Open, ""sv, path, screen_position));

    if (!window_title.is_null())
        picker->set_title(window_title);

    if (picker->exec() == ExecResult::OK) {
        DeprecatedString file_path = picker->selected_file();

        if (file_path.is_null())
            return { {} };

        return file_path;
    }
    return { {} };
}

ErrorOr<Optional<DeprecatedString>> FilePicker::get_save_filepath(Window* parent_window, DeprecatedString const& title, DeprecatedString const& extension, StringView path, ScreenPosition screen_position)
{
    auto picker = TRY(FilePicker::try_create(parent_window, Mode::Save, DeprecatedString::formatted("{}.{}", title, extension), path, screen_position));

    if (picker->exec() == ExecResult::OK) {
        DeprecatedString file_path = picker->selected_file();

        if (file_path.is_null())
            return { {} };

        return file_path;
    }
    return { {} };
}

ErrorOr<NonnullRefPtr<FilePicker>> FilePicker::try_create(GUI::Window* parent_window, GUI::FilePicker::Mode mode, StringView filename, StringView path, GUI::Dialog::ScreenPosition screen_position)
{
    auto file_picker = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FilePicker(parent_window, mode, path, screen_position)));

    switch (mode) {
    case Mode::Open:
    case Mode::OpenMultiple:
    case Mode::OpenFolder:
        file_picker->set_title("Open");
        file_picker->set_icon(TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png"sv)));
        break;
    case Mode::Save:
        file_picker->set_title("Save as");
        file_picker->set_icon(TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/save-as.png"sv)));
        break;
    }

    file_picker->resize(560, 320);

    auto& widget = file_picker->set_main_widget<GUI::Widget>();
    if (!widget.load_from_gml(file_picker_dialog_gml))
        VERIFY_NOT_REACHED();

    auto& toolbar = *widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar");

    file_picker->m_location_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("location_textbox");
    file_picker->m_location_textbox->set_text(path);

    file_picker->m_view = *widget.find_descendant_of_type_named<GUI::MultiView>("view");
    file_picker->m_view->set_selection_mode(mode == GUI::FilePicker::Mode::OpenMultiple ? GUI::AbstractView::SelectionMode::MultiSelection : GUI::AbstractView::SelectionMode::SingleSelection);
    file_picker->m_view->set_model(TRY(SortingProxyModel::create(*file_picker->m_model)));
    file_picker->m_view->set_model_column(FileSystemModel::Column::Name);
    file_picker->m_view->set_key_column_and_sort_order(GUI::FileSystemModel::Column::Name, GUI::SortOrder::Ascending);
    file_picker->m_view->set_column_visible(FileSystemModel::Column::User, true);
    file_picker->m_view->set_column_visible(FileSystemModel::Column::Group, true);
    file_picker->m_view->set_column_visible(FileSystemModel::Column::Permissions, true);
    file_picker->m_view->set_column_visible(FileSystemModel::Column::Inode, true);
    file_picker->m_view->set_column_visible(FileSystemModel::Column::SymlinkTarget, true);

    file_picker->m_model->register_client(file_picker);

    file_picker->m_error_label = file_picker->m_view->add<GUI::Label>();
    file_picker->m_error_label->set_font(file_picker->m_error_label->font().bold_variant());

    file_picker->m_location_textbox->on_return_pressed = [file_picker] {
        file_picker->set_path(file_picker->m_location_textbox->text());
    };

    auto open_parent_directory_action = Action::create(
        "Open parent directory", { Mod_Alt, Key_Up }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open-parent-directory.png"sv)), [file_picker](Action const&) {
            file_picker->set_path(DeprecatedString::formatted("{}/..", file_picker->m_model->root_path()));
        },
        file_picker);
    toolbar.add_action(*open_parent_directory_action);

    auto go_home_action = CommonActions::make_go_home_action([file_picker](auto&) {
        file_picker->set_path(Core::StandardPaths::home_directory());
    },
        file_picker);
    toolbar.add_action(go_home_action);
    toolbar.add_separator();

    auto mkdir_action = Action::create(
        "New directory...", { Mod_Ctrl | Mod_Shift, Key_N }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/mkdir.png"sv)), [file_picker](Action const&) {
            DeprecatedString value;
            if (InputBox::show(file_picker, value, "Enter name:"sv, "New directory"sv) == InputBox::ExecResult::OK && !value.is_empty()) {
                auto new_dir_path = LexicalPath::canonicalized_path(DeprecatedString::formatted("{}/{}", file_picker->m_model->root_path(), value));
                int rc = mkdir(new_dir_path.characters(), 0777);
                if (rc < 0) {
                    MessageBox::show(file_picker, DeprecatedString::formatted("mkdir(\"{}\") failed: {}", new_dir_path, strerror(errno)), "Error"sv, MessageBox::Type::Error);
                } else {
                    file_picker->m_model->invalidate();
                }
            }
        },
        file_picker);

    toolbar.add_action(*mkdir_action);

    toolbar.add_separator();

    toolbar.add_action(file_picker->m_view->view_as_icons_action());
    toolbar.add_action(file_picker->m_view->view_as_table_action());
    toolbar.add_action(file_picker->m_view->view_as_columns_action());

    file_picker->m_filename_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("filename_textbox");
    file_picker->m_filename_textbox->set_focus(true);
    if (mode == Mode::Save) {
        file_picker->m_filename_textbox->set_text(filename);
        file_picker->m_filename_textbox->select_all();
    }
    file_picker->m_filename_textbox->on_return_pressed = [file_picker] {
        file_picker->on_file_return();
    };

    file_picker->m_context_menu = GUI::Menu::construct();
    file_picker->m_context_menu->add_action(GUI::Action::create_checkable(
        "Show dotfiles", { Mod_Ctrl, Key_H }, [file_picker](auto& action) {
            file_picker->m_model->set_should_show_dotfiles(action.is_checked());
            file_picker->m_model->invalidate();
        },
        file_picker));

    file_picker->m_view->on_context_menu_request = [file_picker](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (!index.is_valid()) {
            file_picker->m_context_menu->popup(event.screen_position());
        }
    };

    auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.set_text(ok_button_name(mode));
    ok_button.on_click = [file_picker](auto) {
        file_picker->on_file_return();
    };
    ok_button.set_enabled(mode == Mode::OpenFolder || !file_picker->m_filename_textbox->text().is_empty());

    auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.set_text("Cancel");
    cancel_button.on_click = [file_picker](auto) {
        file_picker->done(ExecResult::Cancel);
    };

    file_picker->m_filename_textbox->on_change = [file_picker, &ok_button, mode] {
        ok_button.set_enabled(mode == Mode::OpenFolder || !file_picker->m_filename_textbox->text().is_empty());
    };

    file_picker->m_view->on_selection_change = [file_picker, mode] {
        auto index = file_picker->m_view->selection().first();
        auto& filter_model = (SortingProxyModel&)*file_picker->m_view->model();
        auto local_index = filter_model.map_to_source(index);
        const FileSystemModel::Node& node = file_picker->m_model->node(local_index);

        auto should_open_folder = mode == Mode::OpenFolder;
        if (should_open_folder == node.is_directory()) {
            file_picker->m_filename_textbox->set_text(node.name);
        } else if (mode != Mode::Save) {
            file_picker->m_filename_textbox->clear();
        }
    };

    file_picker->m_view->on_activation = [file_picker](auto& index) {
        auto& filter_model = (SortingProxyModel&)*file_picker->m_view->model();
        auto local_index = filter_model.map_to_source(index);
        const FileSystemModel::Node& node = file_picker->m_model->node(local_index);
        auto path = node.full_path();

        if (node.is_directory() || node.is_symlink_to_directory()) {
            file_picker->set_path(path);
            // NOTE: 'node' is invalid from here on
        } else {
            file_picker->on_file_return();
        }
    };

    file_picker->m_model->on_directory_change_error = [file_picker](int, char const* error_string) {
        file_picker->m_error_label->set_text(DeprecatedString::formatted("Could not open {}:\n{}", file_picker->m_model->root_path(), error_string));
        file_picker->m_view->set_active_widget(file_picker->m_error_label);

        file_picker->m_view->view_as_icons_action().set_enabled(false);
        file_picker->m_view->view_as_table_action().set_enabled(false);
        file_picker->m_view->view_as_columns_action().set_enabled(false);
    };

    auto& common_locations_tray = *widget.find_descendant_of_type_named<GUI::Tray>("common_locations_tray");
    file_picker->m_model->on_complete = [file_picker, &common_locations_tray] {
        file_picker->m_view->set_active_widget(&file_picker->m_view->current_view());
        for (auto& location_button : file_picker->m_common_location_buttons)
            common_locations_tray.set_item_checked(location_button.tray_item_index, file_picker->m_model->root_path() == location_button.path);

        file_picker->m_view->view_as_icons_action().set_enabled(true);
        file_picker->m_view->view_as_table_action().set_enabled(true);
        file_picker->m_view->view_as_columns_action().set_enabled(true);
    };

    common_locations_tray.on_item_activation = [file_picker](DeprecatedString const& path) {
        file_picker->set_path(path);
    };
    for (auto& location : CommonLocationsProvider::common_locations()) {
        auto index = common_locations_tray.add_item(location.name, FileIconProvider::icon_for_path(location.path).bitmap_for_size(16), location.path);
        file_picker->m_common_location_buttons.append({ location.path, index });
    }

    file_picker->m_location_textbox->set_icon(FileIconProvider::icon_for_path(path).bitmap_for_size(16));
    file_picker->m_model->on_complete();

    return file_picker;
}

FilePicker::FilePicker(Window* parent_window, Mode mode, StringView path, ScreenPosition screen_position)
    : Dialog(parent_window, screen_position)
    , m_model(FileSystemModel::create(path))
    , m_mode(mode)
{
}

FilePicker::~FilePicker()
{
    m_model->unregister_client(*this);
}

void FilePicker::model_did_update(unsigned)
{
    m_location_textbox->set_text(m_model->root_path());
}

void FilePicker::on_file_return()
{
    auto path = m_filename_textbox->text();
    if (!path.starts_with('/')) {
        path = LexicalPath::join(m_model->root_path(), path).string();
    }

    bool file_exists = Core::File::exists(path);

    if (!file_exists && (m_mode == Mode::Open || m_mode == Mode::OpenFolder)) {
        MessageBox::show(this, DeprecatedString::formatted("No such file or directory: {}", m_filename_textbox->text()), "File not found"sv, MessageBox::Type::Error, MessageBox::InputType::OK);
        return;
    }

    if (file_exists && m_mode == Mode::Save) {
        auto result = MessageBox::show(this, "File already exists. Overwrite?"sv, "Existing File"sv, MessageBox::Type::Warning, MessageBox::InputType::OKCancel);
        if (result == MessageBox::ExecResult::Cancel)
            return;
    }

    m_selected_file = path;
    done(ExecResult::OK);
}

void FilePicker::set_path(DeprecatedString const& path)
{
    if (access(path.characters(), R_OK | X_OK) == -1) {
        GUI::MessageBox::show(this, DeprecatedString::formatted("Could not open '{}':\n{}", path, strerror(errno)), "Error"sv, GUI::MessageBox::Type::Error);
        auto& common_locations_tray = *find_descendant_of_type_named<GUI::Tray>("common_locations_tray");
        for (auto& location_button : m_common_location_buttons)
            common_locations_tray.set_item_checked(location_button.tray_item_index, m_model->root_path() == location_button.path);
        return;
    }

    auto new_path = LexicalPath(path).string();
    m_location_textbox->set_icon(FileIconProvider::icon_for_path(new_path).bitmap_for_size(16));
    m_model->set_root_path(new_path);
}

}
