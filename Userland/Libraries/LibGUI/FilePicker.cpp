/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FilePickerDialogGML.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/MultiView.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGfx/FontDatabase.h>
#include <string.h>

namespace GUI {

Optional<String> FilePicker::get_open_filepath(Window* parent_window, const String& window_title)
{
    auto picker = FilePicker::construct(parent_window, Mode::Open);

    if (!window_title.is_null())
        picker->set_title(window_title);

    if (picker->exec() == Dialog::ExecOK) {
        String file_path = picker->selected_file().string();

        if (file_path.is_null())
            return {};

        return file_path;
    }
    return {};
}

Optional<String> FilePicker::get_save_filepath(Window* parent_window, const String& title, const String& extension)
{
    auto picker = FilePicker::construct(parent_window, Mode::Save, String::formatted("{}.{}", title, extension));

    if (picker->exec() == Dialog::ExecOK) {
        String file_path = picker->selected_file().string();

        if (file_path.is_null())
            return {};

        return file_path;
    }
    return {};
}

FilePicker::FilePicker(Window* parent_window, Mode mode, const StringView& file_name, const StringView& path)
    : Dialog(parent_window)
    , m_model(FileSystemModel::create())
    , m_mode(mode)
{
    switch (m_mode) {
    case Mode::Open:
    case Mode::OpenMultiple:
        set_title("Open");
        set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"));
        break;
    case Mode::Save:
        set_title("Save as");
        set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"));
        break;
    }
    resize(560, 320);

    auto& widget = set_main_widget<GUI::Widget>();
    if (!widget.load_from_gml(file_picker_dialog_gml))
        VERIFY_NOT_REACHED();

    auto& toolbar = *widget.find_descendant_of_type_named<GUI::ToolBar>("toolbar");
    toolbar.set_has_frame(false);

    m_location_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("location_textbox");
    m_location_textbox->set_text(path);

    m_view = *widget.find_descendant_of_type_named<GUI::MultiView>("view");
    m_view->set_selection_mode(m_mode == Mode::OpenMultiple ? GUI::AbstractView::SelectionMode::MultiSelection : GUI::AbstractView::SelectionMode::SingleSelection);
    m_view->set_model(SortingProxyModel::create(*m_model));
    m_view->set_model_column(FileSystemModel::Column::Name);
    m_view->set_key_column_and_sort_order(GUI::FileSystemModel::Column::Name, GUI::SortOrder::Ascending);
    m_view->set_column_hidden(FileSystemModel::Column::Owner, true);
    m_view->set_column_hidden(FileSystemModel::Column::Group, true);
    m_view->set_column_hidden(FileSystemModel::Column::Permissions, true);
    m_view->set_column_hidden(FileSystemModel::Column::Inode, true);
    m_view->set_column_hidden(FileSystemModel::Column::SymlinkTarget, true);

    set_path(path);

    m_model->register_client(*this);

    m_location_textbox->on_return_pressed = [this] {
        set_path(m_location_textbox->text());
    };

    auto open_parent_directory_action = Action::create(
        "Open parent directory", { Mod_Alt, Key_Up }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [this](const Action&) {
            set_path(String::formatted("{}/..", m_model->root_path()));
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
        "New directory...", Gfx::Bitmap::load_from_file("/res/icons/16x16/mkdir.png"), [this](const Action&) {
            String value;
            if (InputBox::show(this, value, "Enter name:", "New directory") == InputBox::ExecOK && !value.is_empty()) {
                auto new_dir_path = LexicalPath::canonicalized_path(String::formatted("{}/{}", m_model->root_path(), value));
                int rc = mkdir(new_dir_path.characters(), 0777);
                if (rc < 0) {
                    MessageBox::show(this, String::formatted("mkdir(\"{}\") failed: {}", new_dir_path, strerror(errno)), "Error", MessageBox::Type::Error);
                } else {
                    m_model->update();
                }
            }
        },
        this);

    toolbar.add_action(*mkdir_action);

    toolbar.add_separator();

    toolbar.add_action(m_view->view_as_icons_action());
    toolbar.add_action(m_view->view_as_table_action());
    toolbar.add_action(m_view->view_as_columns_action());

    m_filename_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("filename_textbox");
    m_filename_textbox->set_focus(true);
    if (m_mode == Mode::Save) {
        m_filename_textbox->set_text(file_name);
        m_filename_textbox->select_all();
    }
    m_filename_textbox->on_return_pressed = [&] {
        on_file_return();
    };

    m_view->on_selection_change = [this] {
        auto index = m_view->selection().first();
        auto& filter_model = (SortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_source(index);
        const FileSystemModel::Node& node = m_model->node(local_index);
        LexicalPath path { node.full_path() };

        if (!node.is_directory())
            m_filename_textbox->set_text(node.name);
    };

    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(GUI::Action::create_checkable("Show dotfiles", [&](auto& action) {
        m_model->set_should_show_dotfiles(action.is_checked());
        m_model->update();
    }));

    m_view->on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (!index.is_valid()) {
            m_context_menu->popup(event.screen_position());
        }
    };

    auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.set_text(ok_button_name(m_mode));
    ok_button.on_click = [this](auto) {
        on_file_return();
    };

    auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.set_text("Cancel");
    cancel_button.on_click = [this](auto) {
        done(ExecCancel);
    };

    m_view->on_activation = [this](auto& index) {
        auto& filter_model = (SortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_source(index);
        const FileSystemModel::Node& node = m_model->node(local_index);
        auto path = node.full_path();

        if (node.is_directory()) {
            set_path(path);
            // NOTE: 'node' is invalid from here on
        } else {
            on_file_return();
        }
    };
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
    LexicalPath path(String::formatted("{}/{}", m_model->root_path(), m_filename_textbox->text()));

    if (Core::File::exists(path.string()) && m_mode == Mode::Save) {
        auto result = MessageBox::show(this, "File already exists. Overwrite?", "Existing File", MessageBox::Type::Warning, MessageBox::InputType::OKCancel);
        if (result == MessageBox::ExecCancel)
            return;
    }

    m_selected_file = path;
    done(ExecOK);
}

void FilePicker::set_path(const String& path)
{
    auto new_path = LexicalPath(path).string();
    m_location_textbox->set_icon(FileIconProvider::icon_for_path(new_path).bitmap_for_size(16));
    m_model->set_root_path(new_path);
}

}
