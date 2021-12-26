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

#include "PropertiesDialog.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TabWidget.h>
#include <grp.h>
#include <limits.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

PropertiesDialog::PropertiesDialog(const String& path, bool disable_rename, Window* parent_window)
    : Dialog(parent_window)
{
    auto lexical_path = LexicalPath(path);
    ASSERT(lexical_path.is_valid());

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins({ 4, 4, 4, 4 });
    main_widget.set_fill_with_background_color(true);

    set_rect({ 0, 0, 360, 420 });
    set_resizable(false);

    auto& tab_widget = main_widget.add<GUI::TabWidget>();

    auto& general_tab = tab_widget.add_tab<GUI::Widget>("General");
    general_tab.set_layout<GUI::VerticalBoxLayout>();
    general_tab.layout()->set_margins({ 12, 8, 12, 8 });
    general_tab.layout()->set_spacing(10);

    general_tab.layout()->add_spacer();

    auto& file_container = general_tab.add<GUI::Widget>();
    file_container.set_layout<GUI::HorizontalBoxLayout>();
    file_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    file_container.layout()->set_spacing(20);
    file_container.set_preferred_size(0, 34);

    m_icon = file_container.add<GUI::ImageWidget>();
    m_icon->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_icon->set_preferred_size(32, 32);

    m_name = lexical_path.basename();
    m_path = lexical_path.string();
    m_parent_path = lexical_path.dirname();

    m_name_box = file_container.add<GUI::TextBox>();
    m_name_box->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_name_box->set_preferred_size({ 0, 22 });
    m_name_box->set_text(m_name);
    m_name_box->set_mode(disable_rename ? GUI::TextBox::Mode::DisplayOnly : GUI::TextBox::Mode::Editable);
    m_name_box->on_change = [&]() {
        m_name_dirty = m_name != m_name_box->text();
        m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
    };

    set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/properties.png"));
    make_divider(general_tab);

    struct stat st;
    if (lstat(path.characters(), &st)) {
        perror("stat");
        return;
    }

    String owner_name;
    String group_name;

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

    auto properties = Vector<PropertyValuePair>();
    properties.append({ "Type:", get_description(m_mode) });
    properties.append({ "Location:", path });

    if (S_ISLNK(m_mode)) {
        auto link_destination = Core::File::read_link(path);
        if (link_destination.is_null()) {
            perror("readlink");
        } else {
            properties.append({ "Link target:", link_destination });
        }
    }

    properties.append({ "Size:", String::formatted("{} bytes", st.st_size) });
    properties.append({ "Owner:", String::formatted("{} ({})", owner_name, st.st_uid) });
    properties.append({ "Group:", String::formatted("{} ({})", group_name, st.st_gid) });
    properties.append({ "Created at:", GUI::FileSystemModel::timestamp_string(st.st_ctime) });
    properties.append({ "Last modified:", GUI::FileSystemModel::timestamp_string(st.st_mtime) });

    make_property_value_pairs(properties, general_tab);

    make_divider(general_tab);

    make_permission_checkboxes(general_tab, { S_IRUSR, S_IWUSR, S_IXUSR }, "Owner:", m_mode);
    make_permission_checkboxes(general_tab, { S_IRGRP, S_IWGRP, S_IXGRP }, "Group:", m_mode);
    make_permission_checkboxes(general_tab, { S_IROTH, S_IWOTH, S_IXOTH }, "Others:", m_mode);

    general_tab.layout()->add_spacer();

    auto& button_widget = main_widget.add<GUI::Widget>();
    button_widget.set_layout<GUI::HorizontalBoxLayout>();
    button_widget.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_widget.set_preferred_size(0, 24);
    button_widget.layout()->set_spacing(5);

    button_widget.layout()->add_spacer();

    make_button("OK", button_widget).on_click = [this](auto) {
        if (apply_changes())
            close();
    };
    make_button("Cancel", button_widget).on_click = [this](auto) {
        close();
    };

    m_apply_button = make_button("Apply", button_widget);
    m_apply_button->on_click = [this](auto) { apply_changes(); };
    m_apply_button->set_enabled(false);

    update();
}

PropertiesDialog::~PropertiesDialog() { }

void PropertiesDialog::update()
{
    auto bitmap = GUI::FileIconProvider::icon_for_path(m_name, m_mode).bitmap_for_size(32);
    m_icon->set_bitmap(bitmap);
    set_title(String::formatted("{} - Properties", m_name));
}

void PropertiesDialog::permission_changed(mode_t mask, bool set)
{
    if (set) {
        m_mode |= mask;
    } else {
        m_mode &= ~mask;
    }

    m_permissions_dirty = m_mode != m_old_mode;
    m_apply_button->set_enabled(m_name_dirty || m_permissions_dirty);
}

String PropertiesDialog::make_full_path(const String& name)
{
    return String::formatted("{}/{}", m_parent_path, name);
}

bool PropertiesDialog::apply_changes()
{
    if (m_name_dirty) {
        String new_name = m_name_box->text();
        String new_file = make_full_path(new_name).characters();

        if (GUI::FilePicker::file_exists(new_file)) {
            GUI::MessageBox::show(this, String::formatted("A file \"{}\" already exists!", new_name), "Error", GUI::MessageBox::Type::Error);
            return false;
        }

        if (rename(make_full_path(m_name).characters(), new_file.characters())) {
            GUI::MessageBox::show(this, String::formatted("Could not rename file: {}!", strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return false;
        }

        m_name = new_name;
        m_name_dirty = false;
        update();
    }

    if (m_permissions_dirty) {
        if (chmod(make_full_path(m_name).characters(), m_mode)) {
            GUI::MessageBox::show(this, String::formatted("Could not update permissions: {}!", strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return false;
        }

        m_old_mode = m_mode;
        m_permissions_dirty = false;
    }

    update();
    m_apply_button->set_enabled(false);
    return true;
}

void PropertiesDialog::make_permission_checkboxes(GUI::Widget& parent, PermissionMasks masks, String label_string, mode_t mode)
{
    auto& widget = parent.add<GUI::Widget>();
    widget.set_layout<GUI::HorizontalBoxLayout>();
    widget.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    widget.set_preferred_size(0, 16);
    widget.layout()->set_spacing(10);

    auto& label = widget.add<GUI::Label>(label_string);
    label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    struct stat st;
    if (lstat(m_path.characters(), &st)) {
        perror("stat");
        return;
    }

    auto can_edit_checkboxes = st.st_uid == getuid();

    auto& box_read = widget.add<GUI::CheckBox>("Read");
    box_read.set_checked(mode & masks.read);
    box_read.on_checked = [&, masks](bool checked) { permission_changed(masks.read, checked); };
    box_read.set_enabled(can_edit_checkboxes);

    auto& box_write = widget.add<GUI::CheckBox>("Write");
    box_write.set_checked(mode & masks.write);
    box_write.on_checked = [&, masks](bool checked) { permission_changed(masks.write, checked); };
    box_write.set_enabled(can_edit_checkboxes);

    auto& box_execute = widget.add<GUI::CheckBox>("Execute");
    box_execute.set_checked(mode & masks.execute);
    box_execute.on_checked = [&, masks](bool checked) { permission_changed(masks.execute, checked); };
    box_execute.set_enabled(can_edit_checkboxes);
}

void PropertiesDialog::make_property_value_pairs(const Vector<PropertyValuePair>& pairs, GUI::Widget& parent)
{
    int max_width = 0;
    Vector<NonnullRefPtr<GUI::Label>> property_labels;

    property_labels.ensure_capacity(pairs.size());
    for (auto pair : pairs) {
        auto& label_container = parent.add<GUI::Widget>();
        label_container.set_layout<GUI::HorizontalBoxLayout>();
        label_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        label_container.set_preferred_size(0, 14);
        label_container.layout()->set_spacing(12);

        auto& label_property = label_container.add<GUI::Label>(pair.property);
        label_property.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        label_property.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);

        label_container.add<GUI::Label>(pair.value).set_text_alignment(Gfx::TextAlignment::CenterLeft);

        max_width = max(max_width, label_property.font().width(pair.property));
        property_labels.append(label_property);
    }

    for (auto label : property_labels)
        label->set_preferred_size({ max_width, 0 });
}

GUI::Button& PropertiesDialog::make_button(String text, GUI::Widget& parent)
{
    auto& button = parent.add<GUI::Button>(text);
    button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    button.set_preferred_size(70, 22);
    return button;
}

void PropertiesDialog::make_divider(GUI::Widget& parent)
{
    parent.layout()->add_spacer();

    auto& divider = parent.add<GUI::Frame>();
    divider.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    divider.set_preferred_size({ 0, 2 });

    parent.layout()->add_spacer();
}
