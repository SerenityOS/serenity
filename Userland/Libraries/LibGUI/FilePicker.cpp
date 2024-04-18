/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CommonLocationsProvider.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FilePickerDialogWidget.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/FileTypeFilter.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/MultiView.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Tray.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <unistd.h>

namespace GUI {

ErrorOr<Optional<String>> FilePicker::get_filepath(Badge<FileSystemAccessServer::ConnectionFromClient>, i32 window_server_client_id, i32 parent_window_id, Mode mode, StringView window_title, StringView file_basename, StringView path, Optional<Vector<FileTypeFilter>> allowed_file_types)
{
    auto picker = FilePicker::construct(nullptr, mode, file_basename, path, ScreenPosition::DoNotPosition, move(allowed_file_types));
    auto parent_rect = ConnectionToWindowServer::the().get_window_rect_from_client(window_server_client_id, parent_window_id);
    picker->center_within(parent_rect);
    picker->constrain_to_desktop();
    if (!window_title.is_empty())
        picker->set_title(window_title);
    picker->show();
    ConnectionToWindowServer::the().set_window_parent_from_client(window_server_client_id, parent_window_id, picker->window_id());

    if (picker->exec() == ExecResult::OK) {
        auto file_path = TRY(picker->selected_file().map([](auto& v) { return String::from_byte_string(v); }));
        if (file_path.has_value() && file_path->is_empty())
            return Optional<String> {};

        return file_path;
    }
    return Optional<String> {};
}

Optional<ByteString> FilePicker::get_open_filepath(Window* parent_window, ByteString const& window_title, StringView path, bool folder, ScreenPosition screen_position, Optional<Vector<FileTypeFilter>> allowed_file_types)
{
    auto picker = FilePicker::construct(parent_window, folder ? Mode::OpenFolder : Mode::Open, ""sv, path, screen_position, move(allowed_file_types));

    if (!window_title.is_empty())
        picker->set_title(window_title);

    if (picker->exec() == ExecResult::OK)
        return picker->selected_file();

    return {};
}

Optional<ByteString> FilePicker::get_save_filepath(Window* parent_window, ByteString const& title, ByteString const& extension, StringView path, ScreenPosition screen_position)
{
    auto picker = FilePicker::construct(parent_window, Mode::Save, ByteString::formatted("{}.{}", title, extension), path, screen_position);

    if (picker->exec() == ExecResult::OK)
        return picker->selected_file();
    return {};
}

FilePicker::FilePicker(Window* parent_window, Mode mode, StringView filename, StringView path, ScreenPosition screen_position, Optional<Vector<FileTypeFilter>> allowed_file_types)
    : Dialog(parent_window, screen_position)
    , m_model(FileSystemModel::create(path))
    , m_allowed_file_types(move(allowed_file_types))
    , m_mode(mode)
{
    switch (m_mode) {
    case Mode::Open:
    case Mode::OpenMultiple:
    case Mode::OpenFolder:
        set_title("Open");
        set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv).release_value_but_fixme_should_propagate_errors());
        break;
    case Mode::Save:
        set_title("Save As");
        set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/save-as.png"sv).release_value_but_fixme_should_propagate_errors());
        break;
    }
    resize(560, 320);

    auto widget = GUI::FilePickerDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(widget);

    auto& toolbar = *widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");

    m_location_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("location_textbox");
    m_location_textbox->set_text(path);

    m_view = *widget->find_descendant_of_type_named<GUI::MultiView>("view");
    m_view->set_selection_mode(m_mode == Mode::OpenMultiple ? GUI::AbstractView::SelectionMode::MultiSelection : GUI::AbstractView::SelectionMode::SingleSelection);
    m_view->set_model(MUST(SortingProxyModel::create(*m_model)));
    m_view->set_model_column(FileSystemModel::Column::Name);
    m_view->set_key_column_and_sort_order(GUI::FileSystemModel::Column::Name, GUI::SortOrder::Ascending);
    m_view->set_column_visible(FileSystemModel::Column::User, true);
    m_view->set_column_visible(FileSystemModel::Column::Group, true);
    m_view->set_column_visible(FileSystemModel::Column::Permissions, true);
    m_view->set_column_visible(FileSystemModel::Column::Inode, true);
    m_view->set_column_visible(FileSystemModel::Column::SymlinkTarget, true);

    m_model->register_client(*this);

    m_error_label = m_view->add<GUI::Label>();
    m_error_label->set_font(m_error_label->font().bold_variant());

    m_location_textbox->on_return_pressed = [this] {
        set_path(m_location_textbox->text());
    };

    auto* file_types_filters_combo = widget->find_descendant_of_type_named<GUI::ComboBox>("allowed_file_type_filters_combo");

    if (m_allowed_file_types.has_value()) {
        for (auto& filter : *m_allowed_file_types) {
            if (!filter.extensions.has_value()) {
                m_allowed_file_types_names.append(filter.name);
                continue;
            }

            StringBuilder extension_list;
            extension_list.join("; "sv, *filter.extensions);
            m_allowed_file_types_names.append(ByteString::formatted("{} ({})", filter.name, extension_list.to_byte_string()));
        }

        file_types_filters_combo->set_model(*GUI::ItemListModel<ByteString, Vector<ByteString>>::create(m_allowed_file_types_names));
        file_types_filters_combo->on_change = [this](ByteString const&, GUI::ModelIndex const& index) {
            m_model->set_allowed_file_extensions((*m_allowed_file_types)[index.row()].extensions);
        };
        file_types_filters_combo->set_selected_index(0);
        m_model->set_allowed_file_extensions((*m_allowed_file_types)[0].extensions);
    } else {
        auto* file_types_filter_label = widget->find_descendant_of_type_named<GUI::Label>("allowed_file_types_label");
        auto& spacer = file_types_filter_label->parent_widget()->add<GUI::Widget>();
        spacer.set_fixed_height(22);
        file_types_filter_label->remove_from_parent();

        file_types_filters_combo->parent_widget()->insert_child_before(GUI::Widget::construct(), *file_types_filters_combo);

        file_types_filters_combo->remove_from_parent();
    }

    auto open_parent_directory_action = Action::create(
        "Open Parent Directory", { Mod_Alt, Key_Up }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"sv).release_value_but_fixme_should_propagate_errors(), [this](Action const&) {
            set_path(ByteString::formatted("{}/..", m_model->root_path()));
        },
        this);
    toolbar.add_action(*open_parent_directory_action);

    auto go_home_action = CommonActions::make_go_home_action([this](auto&) {
        set_path(Core::StandardPaths::home_directory());
    },
        this);
    toolbar.add_action(go_home_action);
    toolbar.add_separator();

    auto mkdir_action = Action::create(
        "New Directory...", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"sv).release_value_but_fixme_should_propagate_errors(), [this](Action const&) {
            String value;
            if (InputBox::show(this, value, "Enter a name:"sv, "New Directory"sv, GUI::InputType::NonemptyText) == InputBox::ExecResult::OK) {
                auto new_dir_path = LexicalPath::canonicalized_path(ByteString::formatted("{}/{}", m_model->root_path(), value));
                int rc = mkdir(new_dir_path.characters(), 0777);
                if (rc < 0) {
                    (void)MessageBox::try_show_error(this, ByteString::formatted("Making new directory \"{}\" failed: {}", new_dir_path, Error::from_errno(errno)));
                } else {
                    m_model->invalidate();
                }
            }
        },
        this);

    toolbar.add_action(*mkdir_action);

    toolbar.add_separator();

    toolbar.add_action(m_view->view_as_icons_action());
    toolbar.add_action(m_view->view_as_table_action());
    toolbar.add_action(m_view->view_as_columns_action());

    m_filename_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("filename_textbox");
    m_filename_textbox->set_focus(true);
    if (m_mode == Mode::Save) {
        LexicalPath lexical_filename { filename };
        m_filename_textbox->set_text(filename);

        if (auto extension = lexical_filename.extension(); !extension.is_empty()) {
            TextPosition start_of_filename { 0, 0 };
            TextPosition end_of_filename { 0, filename.length() - extension.length() - 1 };

            m_filename_textbox->set_selection({ end_of_filename, start_of_filename });
        } else {
            m_filename_textbox->select_all();
        }
    }

    m_context_menu = GUI::Menu::construct();

    m_context_menu->add_action(mkdir_action);
    m_context_menu->add_separator();

    auto show_dotfiles = GUI::Action::create_checkable(
        "Show Dotfiles", { Mod_Ctrl, Key_H }, [&](auto& action) {
            m_model->set_should_show_dotfiles(action.is_checked());
            m_model->invalidate();
        },
        this);
    auto show_dotfiles_preset = Config::read_bool("FileManager"sv, "DirectoryView"sv, "ShowDotFiles"sv, false);
    if (show_dotfiles_preset)
        show_dotfiles->activate();

    m_context_menu->add_action(show_dotfiles);

    m_view->on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (!index.is_valid()) {
            m_context_menu->popup(event.screen_position());
        }
    };

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.set_text(ok_button_name(m_mode));
    ok_button.on_click = [this](auto) {
        on_file_return();
    };
    ok_button.set_enabled(m_mode == Mode::OpenFolder || !m_filename_textbox->text().is_empty());
    ok_button.set_default(true);

    m_location_textbox->on_focus_change = [&ok_button](auto focused, auto) {
        ok_button.set_default(!focused);
    };

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.set_text("Cancel"_string);
    cancel_button.on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    m_filename_textbox->on_change = [&] {
        ok_button.set_enabled(m_mode == Mode::OpenFolder || !m_filename_textbox->text().is_empty());
    };

    m_view->on_selection_change = [this] {
        auto index = m_view->selection().first();
        auto& filter_model = (SortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_source(index);
        FileSystemModel::Node const& node = m_model->node(local_index);

        auto should_open_folder = m_mode == Mode::OpenFolder;
        if (should_open_folder == node.is_directory()) {
            m_filename_textbox->set_text(node.name);
        } else if (m_mode != Mode::Save) {
            m_filename_textbox->clear();
        }
    };

    m_view->on_activation = [this](auto& index) {
        auto& filter_model = (SortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_source(index);
        FileSystemModel::Node const& node = m_model->node(local_index);
        auto path = node.full_path();

        if (node.is_directory() || node.is_symlink_to_directory()) {
            set_path(path);
            // NOTE: 'node' is invalid from here on
        } else {
            on_file_return();
        }
    };

    m_model->on_directory_change_error = [&](int, char const* error_string) {
        m_error_label->set_text(String::formatted("Opening \"{}\" failed: {}", m_model->root_path(), error_string).release_value_but_fixme_should_propagate_errors());
        m_view->set_active_widget(m_error_label);

        m_view->view_as_icons_action().set_enabled(false);
        m_view->view_as_table_action().set_enabled(false);
        m_view->view_as_columns_action().set_enabled(false);
    };

    auto& common_locations_tray = *widget->find_descendant_of_type_named<GUI::Tray>("common_locations_tray");
    m_model->on_complete = [&] {
        m_view->set_active_widget(&m_view->current_view());
        for (auto& location_button : m_common_location_buttons)
            common_locations_tray.set_item_checked(location_button.tray_item_index, m_model->root_path() == location_button.path);

        m_view->view_as_icons_action().set_enabled(true);
        m_view->view_as_table_action().set_enabled(true);
        m_view->view_as_columns_action().set_enabled(true);
    };

    common_locations_tray.on_item_activation = [this](ByteString const& path) {
        set_path(path);
    };
    for (auto& location : CommonLocationsProvider::common_locations()) {
        auto index = common_locations_tray.add_item(location.name, FileIconProvider::icon_for_path(location.path).bitmap_for_size(16), location.path);
        m_common_location_buttons.append({ location.path, index });
    }

    m_location_textbox->set_icon(FileIconProvider::icon_for_path(path).bitmap_for_size(16));
    m_model->on_complete();
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
    if (!path.starts_with('/'))
        path = LexicalPath::join(m_model->root_path(), path).string();

    auto stat_or_error = Core::System::stat(path);
    bool file_exists = !stat_or_error.is_error();

    if (!file_exists && (m_mode == Mode::Open || m_mode == Mode::OpenFolder)) {
        (void)MessageBox::try_show_error(this, ByteString::formatted("Opening \"{}\" failed: {}", m_filename_textbox->text(), Error::from_errno(ENOENT)));
        return;
    }

    if (file_exists && m_mode == Mode::Save) {
        auto text = String::formatted("Are you sure you want to overwrite \"{}\"?", m_filename_textbox->text());
        if (text.is_error())
            return;
        auto result = MessageBox::show(this, text.release_value(), "Confirm Overwrite"sv, MessageBox::Type::Warning, MessageBox::InputType::OKCancel);
        if (result == MessageBox::ExecResult::Cancel)
            return;
    }

    // If the entered filename matches an existing directory, traverse into it
    if (file_exists && m_mode != Mode::OpenFolder && S_ISDIR(stat_or_error.value().st_mode)) {
        m_filename_textbox->clear();
        set_path(path);
        return;
    }

    m_selected_file = path;
    done(ExecResult::OK);
}

void FilePicker::set_path(ByteString const& path)
{
    if (access(path.characters(), R_OK | X_OK) == -1) {
        (void)GUI::MessageBox::try_show_error(this, ByteString::formatted("Opening \"{}\" failed: {}", path, Error::from_errno(errno)));
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
