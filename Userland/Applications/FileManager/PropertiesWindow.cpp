/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PropertiesWindow.h"
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <Applications/FileManager/DirectoryView.h>
#include <Applications/FileManager/PropertiesWindowGeneralTabGML.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/IconView.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/TabWidget.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

ErrorOr<NonnullRefPtr<PropertiesWindow>> PropertiesWindow::try_create(DeprecatedString const& path, bool disable_rename, Window* parent)
{
    auto window = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PropertiesWindow(path, parent)));
    window->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/properties.png"sv)));
    TRY(window->create_widgets(disable_rename));
    return window;
}

PropertiesWindow::PropertiesWindow(DeprecatedString const& path, Window* parent_window)
    : Window(parent_window)
{
    auto lexical_path = LexicalPath(path);

    m_name = lexical_path.basename();
    m_path = lexical_path.string();
    m_parent_path = lexical_path.dirname();

    set_rect({ 0, 0, 360, 420 });
    set_resizable(false);
}

ErrorOr<void> PropertiesWindow::create_widgets(bool disable_rename)
{
    auto main_widget = TRY(set_main_widget<GUI::Widget>());
    (void)TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>());
    main_widget->layout()->set_spacing(6);
    main_widget->layout()->set_margins(4);
    main_widget->set_fill_with_background_color(true);

    auto tab_widget = TRY(main_widget->try_add<GUI::TabWidget>());

    auto general_tab = TRY(tab_widget->try_add_tab<GUI::Widget>("General"));
    TRY(general_tab->load_from_gml(properties_window_general_tab_gml));

    m_icon = general_tab->find_descendant_of_type_named<GUI::ImageWidget>("icon");

    m_name_box = general_tab->find_descendant_of_type_named<GUI::TextBox>("name");
    m_name_box->set_text(m_name);
    m_name_box->set_mode(disable_rename ? GUI::TextBox::Mode::DisplayOnly : GUI::TextBox::Mode::Editable);
    m_name_box->on_change = [&]() {
        m_name_dirty = m_name != m_name_box->text();
        m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
    };

    auto* location = general_tab->find_descendant_of_type_named<GUI::LinkLabel>("location");
    location->set_text(m_path);
    location->on_click = [this] {
        Desktop::Launcher::open(URL::create_with_file_scheme(m_parent_path, m_name));
    };

    auto st = TRY(Core::System::lstat(m_path));

    DeprecatedString owner_name;
    DeprecatedString group_name;

    if (auto* pw = getpwuid(st.st_uid)) {
        owner_name = pw->pw_name;
    } else {
        owner_name = "n/a";
    }

    if (auto* gr = getgrgid(st.st_gid)) {
        group_name = gr->gr_name;
    } else {
        group_name = "n/a";
    }

    m_mode = st.st_mode;
    m_old_mode = st.st_mode;

    auto* type = general_tab->find_descendant_of_type_named<GUI::Label>("type");
    type->set_text(get_description(m_mode));

    if (S_ISLNK(m_mode)) {
        auto link_destination_or_error = Core::File::read_link(m_path);
        if (link_destination_or_error.is_error()) {
            perror("readlink");
        } else {
            auto link_destination = link_destination_or_error.release_value();
            auto* link_location = general_tab->find_descendant_of_type_named<GUI::LinkLabel>("link_location");
            link_location->set_text(link_destination);
            link_location->on_click = [link_destination] {
                auto link_directory = LexicalPath(link_destination);
                Desktop::Launcher::open(URL::create_with_file_scheme(link_directory.dirname(), link_directory.basename()));
            };
        }
    } else {
        auto* link_location_widget = general_tab->find_descendant_of_type_named<GUI::Widget>("link_location_widget");
        general_tab->remove_child(*link_location_widget);
    }

    auto* size = general_tab->find_descendant_of_type_named<GUI::Label>("size");
    size->set_text(human_readable_size_long(st.st_size));

    auto* owner = general_tab->find_descendant_of_type_named<GUI::Label>("owner");
    owner->set_text(DeprecatedString::formatted("{} ({})", owner_name, st.st_uid));

    auto* group = general_tab->find_descendant_of_type_named<GUI::Label>("group");
    group->set_text(DeprecatedString::formatted("{} ({})", group_name, st.st_gid));

    auto* created_at = general_tab->find_descendant_of_type_named<GUI::Label>("created_at");
    created_at->set_text(GUI::FileSystemModel::timestamp_string(st.st_ctime));

    auto* last_modified = general_tab->find_descendant_of_type_named<GUI::Label>("last_modified");
    last_modified->set_text(GUI::FileSystemModel::timestamp_string(st.st_mtime));

    auto* owner_read = general_tab->find_descendant_of_type_named<GUI::CheckBox>("owner_read");
    auto* owner_write = general_tab->find_descendant_of_type_named<GUI::CheckBox>("owner_write");
    auto* owner_execute = general_tab->find_descendant_of_type_named<GUI::CheckBox>("owner_execute");
    TRY(setup_permission_checkboxes(*owner_read, *owner_write, *owner_execute, { S_IRUSR, S_IWUSR, S_IXUSR }, m_mode));

    auto* group_read = general_tab->find_descendant_of_type_named<GUI::CheckBox>("group_read");
    auto* group_write = general_tab->find_descendant_of_type_named<GUI::CheckBox>("group_write");
    auto* group_execute = general_tab->find_descendant_of_type_named<GUI::CheckBox>("group_execute");
    TRY(setup_permission_checkboxes(*group_read, *group_write, *group_execute, { S_IRGRP, S_IWGRP, S_IXGRP }, m_mode));

    auto* others_read = general_tab->find_descendant_of_type_named<GUI::CheckBox>("others_read");
    auto* others_write = general_tab->find_descendant_of_type_named<GUI::CheckBox>("others_write");
    auto* others_execute = general_tab->find_descendant_of_type_named<GUI::CheckBox>("others_execute");
    TRY(setup_permission_checkboxes(*others_read, *others_write, *others_execute, { S_IROTH, S_IWOTH, S_IXOTH }, m_mode));

    auto button_widget = TRY(main_widget->try_add<GUI::Widget>());
    (void)TRY(button_widget->try_set_layout<GUI::HorizontalBoxLayout>());
    button_widget->set_fixed_height(22);
    button_widget->layout()->set_spacing(5);

    button_widget->layout()->add_spacer();

    auto ok_button = TRY(make_button("OK", button_widget));
    ok_button->on_click = [this](auto) {
        if (apply_changes())
            close();
    };
    auto cancel_button = TRY(make_button("Cancel", button_widget));
    cancel_button->on_click = [this](auto) {
        close();
    };

    m_apply_button = TRY(make_button("Apply", button_widget));
    m_apply_button->on_click = [this](auto) { apply_changes(); };
    m_apply_button->set_enabled(false);

    update();
    return {};
}

void PropertiesWindow::update()
{
    m_icon->set_bitmap(GUI::FileIconProvider::icon_for_path(make_full_path(m_name), m_mode).bitmap_for_size(32));
    set_title(DeprecatedString::formatted("{} - Properties", m_name));
}

void PropertiesWindow::permission_changed(mode_t mask, bool set)
{
    if (set) {
        m_mode |= mask;
    } else {
        m_mode &= ~mask;
    }

    m_permissions_dirty = m_mode != m_old_mode;
    m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
}

DeprecatedString PropertiesWindow::make_full_path(DeprecatedString const& name)
{
    return DeprecatedString::formatted("{}/{}", m_parent_path, name);
}

bool PropertiesWindow::apply_changes()
{
    if (m_name_dirty) {
        DeprecatedString new_name = m_name_box->text();
        DeprecatedString new_file = make_full_path(new_name).characters();

        if (Core::File::exists(new_file)) {
            GUI::MessageBox::show(this, DeprecatedString::formatted("A file \"{}\" already exists!", new_name), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        if (rename(make_full_path(m_name).characters(), new_file.characters())) {
            GUI::MessageBox::show(this, DeprecatedString::formatted("Could not rename file: {}!", strerror(errno)), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        m_name = new_name;
        m_name_dirty = false;
        update();
    }

    if (m_permissions_dirty) {
        if (chmod(make_full_path(m_name).characters(), m_mode)) {
            GUI::MessageBox::show(this, DeprecatedString::formatted("Could not update permissions: {}!", strerror(errno)), "Error"sv, GUI::MessageBox::Type::Error);
            return false;
        }

        m_old_mode = m_mode;
        m_permissions_dirty = false;
    }

    auto directory_view = parent()->find_descendant_of_type_named<FileManager::DirectoryView>("directory_view");
    directory_view->refresh();

    update();
    m_apply_button->set_enabled(false);
    return true;
}

ErrorOr<void> PropertiesWindow::setup_permission_checkboxes(GUI::CheckBox& box_read, GUI::CheckBox& box_write, GUI::CheckBox& box_execute, PermissionMasks masks, mode_t mode)
{
    auto st = TRY(Core::System::lstat(m_path));

    auto can_edit_checkboxes = st.st_uid == getuid();

    box_read.set_checked(mode & masks.read);
    box_read.on_checked = [&, masks](bool checked) { permission_changed(masks.read, checked); };
    box_read.set_enabled(can_edit_checkboxes);

    box_write.set_checked(mode & masks.write);
    box_write.on_checked = [&, masks](bool checked) { permission_changed(masks.write, checked); };
    box_write.set_enabled(can_edit_checkboxes);

    box_execute.set_checked(mode & masks.execute);
    box_execute.on_checked = [&, masks](bool checked) { permission_changed(masks.execute, checked); };
    box_execute.set_enabled(can_edit_checkboxes);

    return {};
}

ErrorOr<NonnullRefPtr<GUI::Button>> PropertiesWindow::make_button(DeprecatedString text, GUI::Widget& parent)
{
    auto button = TRY(parent.try_add<GUI::Button>(text));
    button->set_fixed_size(70, 22);
    return button;
}
