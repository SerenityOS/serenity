#include "PropertiesDialog.h"
#include <AK/StringBuilder.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GTabWidget.h>
#include <pwd.h>
#include <stdio.h>
#include <unistd.h>

PropertiesDialog::PropertiesDialog(GFileSystemModel& model, String path, bool disable_rename, CObject* parent)
    : GDialog(parent)
    , m_model(model)
{
    auto file_path = FileSystemPath(path);
    ASSERT(file_path.is_valid());

    auto main_widget = GWidget::construct();
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });
    main_widget->set_fill_with_background_color(true);

    set_main_widget(main_widget);
    set_rect({ 0, 0, 360, 420 });
    set_resizable(false);

    auto tab_widget = GTabWidget::construct(main_widget);

    auto general_tab = GWidget::construct(tab_widget.ptr());
    general_tab->set_layout(make<GBoxLayout>(Orientation::Vertical));
    general_tab->layout()->set_margins({ 12, 8, 12, 8 });
    general_tab->layout()->set_spacing(10);
    tab_widget->add_widget("General", general_tab);

    general_tab->layout()->add_spacer();

    auto file_container = GWidget::construct(general_tab.ptr());
    file_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    file_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    file_container->layout()->set_spacing(20);
    file_container->set_preferred_size(0, 34);

    m_icon = GLabel::construct(file_container);
    m_icon->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    m_icon->set_preferred_size(32, 32);

    m_name = file_path.basename();

    m_name_box = GTextBox::construct(file_container);
    m_name_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_name_box->set_preferred_size({ 0, 22 });
    m_name_box->set_text(m_name);
    m_name_box->on_change = [&, disable_rename]() {
        if (disable_rename) {
            m_name_box->set_text(m_name); //FIXME: GTextBox does not support set_enabled yet...
        } else {
            m_name_dirty = m_name != m_name_box->text();
            m_apply_button->set_enabled(true);
        }
    };

    set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/properties.png"));
    make_divider(general_tab);

    struct stat st;
    if (lstat(path.characters(), &st)) {
        perror("stat");
        return;
    }

    struct passwd* user_pw = getpwuid(st.st_uid);
    struct passwd* group_pw = getpwuid(st.st_gid);
    ASSERT(user_pw && group_pw);

    m_mode = st.st_mode;

    auto properties = Vector<PropertyValuePair>();
    properties.append({ "Type:", get_description(m_mode) });
    properties.append({ "Location:", path });

    if (S_ISLNK(m_mode)) {
        char link_destination[PATH_MAX];
        if (readlink(path.characters(), link_destination, sizeof(link_destination))) {
            perror("readlink");
            return;
        }

        properties.append({ "Link target:", link_destination });
    }

    properties.append({ "Size:", String::format("%zu bytes", st.st_size) });
    properties.append({ "Owner:", String::format("%s (%lu)", user_pw->pw_name, static_cast<u32>(user_pw->pw_uid)) });
    properties.append({ "Group:", String::format("%s (%lu)", group_pw->pw_name, static_cast<u32>(group_pw->pw_uid)) });
    properties.append({ "Created at:", GFileSystemModel::timestamp_string(st.st_ctime) });
    properties.append({ "Last modified:", GFileSystemModel::timestamp_string(st.st_mtime) });

    make_property_value_pairs(properties, general_tab);

    make_divider(general_tab);

    make_permission_checkboxes(general_tab, { S_IRUSR, S_IWUSR, S_IXUSR }, "Owner:", m_mode);
    make_permission_checkboxes(general_tab, { S_IRGRP, S_IWGRP, S_IXGRP }, "Group:", m_mode);
    make_permission_checkboxes(general_tab, { S_IROTH, S_IWOTH, S_IXOTH }, "Others:", m_mode);

    general_tab->layout()->add_spacer();

    auto button_widget = GWidget::construct(main_widget.ptr());
    button_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    button_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_widget->set_preferred_size(0, 24);
    button_widget->layout()->set_spacing(5);

    button_widget->layout()->add_spacer();

    make_button("OK", button_widget)->on_click = [&](auto&) {if(apply_changes()) close(); };
    make_button("Cancel", button_widget)->on_click = [&](auto&) { close(); };

    m_apply_button = make_button("Apply", button_widget);
    m_apply_button->on_click = [&](auto&) { apply_changes(); };
    m_apply_button->set_enabled(false);

    update();
}

PropertiesDialog::~PropertiesDialog() {}

void PropertiesDialog::update()
{
    m_icon->set_icon(const_cast<GraphicsBitmap*>(m_model.icon_for_file(m_mode, m_name).bitmap_for_size(32)));
    set_title(String::format("Properties of \"%s\"", m_name.characters()));
}

void PropertiesDialog::permission_changed(mode_t mask, bool set)
{
    if (set) {
        m_mode |= mask;
    } else {
        m_mode &= ~mask;
    }

    m_permissions_dirty = true;
    m_apply_button->set_enabled(true);
}

String PropertiesDialog::make_full_path(String name)
{
    return String::format("%s/%s", m_model.root_path().characters(), name.characters());
}

bool PropertiesDialog::apply_changes()
{
    if (m_name_dirty) {
        String new_name = m_name_box->text();
        String new_file = make_full_path(new_name).characters();

        if (GFilePicker::file_exists(new_file)) {
            GMessageBox::show(String::format("A file \"%s\" already exists!", new_name.characters()), "Error", GMessageBox::Type::Error);
            return false;
        }

        if (rename(make_full_path(m_name).characters(), new_file.characters())) {
            GMessageBox::show(String::format("Could not rename file: %s!", strerror(errno)), "Error", GMessageBox::Type::Error);
            return false;
        }

        m_name = new_name;
        m_name_dirty = false;
        update();
    }

    if (m_permissions_dirty) {
        if (chmod(make_full_path(m_name).characters(), m_mode)) {
            GMessageBox::show(String::format("Could not update permissions: %s!", strerror(errno)), "Error", GMessageBox::Type::Error);
            return false;
        }

        m_permissions_dirty = false;
    }

    update();
    m_apply_button->set_enabled(false);
    return true;
}

void PropertiesDialog::make_permission_checkboxes(NonnullRefPtr<GWidget>& parent, PermissionMasks masks, String label_string, mode_t mode)
{
    auto widget = GWidget::construct(parent.ptr());
    widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    widget->set_preferred_size(0, 16);
    widget->layout()->set_spacing(10);

    auto label = GLabel::construct(label_string, widget);
    label->set_text_alignment(TextAlignment::CenterLeft);

    auto box_read = GCheckBox::construct("Read", widget);
    box_read->set_checked(mode & masks.read);
    box_read->on_checked = [&, masks](bool checked) { permission_changed(masks.read, checked); };

    auto box_write = GCheckBox::construct("Write", widget);
    box_write->set_checked(mode & masks.write);
    box_write->on_checked = [&, masks](bool checked) { permission_changed(masks.write, checked); };

    auto box_execute = GCheckBox::construct("Execute", widget);
    box_execute->set_checked(mode & masks.execute);
    box_execute->on_checked = [&, masks](bool checked) { permission_changed(masks.execute, checked); };
}

void PropertiesDialog::make_property_value_pairs(const Vector<PropertyValuePair>& pairs, NonnullRefPtr<GWidget>& parent)
{
    int max_width = 0;
    Vector<NonnullRefPtr<GLabel>> property_labels;

    property_labels.ensure_capacity(pairs.size());
    for (auto pair : pairs) {
        auto label_container = GWidget::construct(parent.ptr());
        label_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
        label_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        label_container->set_preferred_size(0, 14);
        label_container->layout()->set_spacing(12);

        auto label_property = GLabel::construct(pair.property, label_container);
        label_property->set_text_alignment(TextAlignment::CenterLeft);
        label_property->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);

        GLabel::construct(pair.value, label_container)->set_text_alignment(TextAlignment::CenterLeft);

        max_width = max(max_width, label_property->font().width(pair.property));
        property_labels.append(label_property);
    }

    for (auto label : property_labels)
        label->set_preferred_size({ max_width, 0 });
}

NonnullRefPtr<GButton> PropertiesDialog::make_button(String text, NonnullRefPtr<GWidget>& parent)
{
    auto button = GButton::construct(text, parent.ptr());
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    button->set_preferred_size(70, 22);
    return button;
}

void PropertiesDialog::make_divider(NonnullRefPtr<GWidget>& parent)
{
    parent->layout()->add_spacer();

    auto divider = GFrame::construct(parent.ptr());
    divider->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    divider->set_preferred_size({ 0, 2 });
    divider->set_frame_shape(FrameShape::HorizontalLine);
    divider->set_frame_shadow(FrameShadow::Sunken);
    divider->set_frame_thickness(2);

    parent->layout()->add_spacer();
}
