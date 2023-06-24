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
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
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
    TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>(4, 6));
    main_widget->set_fill_with_background_color(true);

    auto tab_widget = TRY(main_widget->try_add<GUI::TabWidget>());
    TRY(create_general_tab(tab_widget, disable_rename));

    auto button_widget = TRY(main_widget->try_add<GUI::Widget>());
    TRY(button_widget->try_set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 5));
    button_widget->set_fixed_height(22);

    TRY(button_widget->add_spacer());

    auto ok_button = TRY(make_button("OK"_short_string, button_widget));
    ok_button->on_click = [this](auto) {
        if (apply_changes())
            close();
    };
    auto cancel_button = TRY(make_button("Cancel"_short_string, button_widget));
    cancel_button->on_click = [this](auto) {
        close();
    };

    m_apply_button = TRY(make_button("Apply"_short_string, button_widget));
    m_apply_button->on_click = [this](auto) { apply_changes(); };
    m_apply_button->set_enabled(false);

    if (S_ISDIR(m_old_mode)) {
        m_directory_statistics_calculator = make_ref_counted<DirectoryStatisticsCalculator>(m_path);
        m_directory_statistics_calculator->on_update = [this, origin_event_loop = &Core::EventLoop::current()](off_t total_size_in_bytes, size_t file_count, size_t directory_count) {
            origin_event_loop->deferred_invoke([=, weak_this = make_weak_ptr<PropertiesWindow>()] {
                if (auto strong_this = weak_this.strong_ref())
                    strong_this->m_size_label->set_text(String::formatted("{}\n{} files, {} subdirectories", human_readable_size_long(total_size_in_bytes, UseThousandsSeparator::Yes), file_count, directory_count).release_value_but_fixme_should_propagate_errors());
            });
        };
        m_directory_statistics_calculator->start();
    }

    m_on_escape = GUI::Action::create("Close properties", { Key_Escape }, [this](GUI::Action&) {
        if (!m_apply_button->is_enabled())
            close();
    });

    update();
    return {};
}

ErrorOr<void> PropertiesWindow::create_general_tab(GUI::TabWidget& tab_widget, bool disable_rename)
{
    auto general_tab = TRY(tab_widget.try_add_tab<GUI::Widget>("General"_short_string));
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
    location->set_text(TRY(String::from_deprecated_string(m_path)));
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
    type->set_text(TRY(String::from_utf8(get_description(m_mode))));

    if (S_ISLNK(m_mode)) {
        auto link_destination_or_error = FileSystem::read_link(m_path);
        if (link_destination_or_error.is_error()) {
            perror("readlink");
        } else {
            auto link_destination = link_destination_or_error.release_value();
            auto* link_location = general_tab->find_descendant_of_type_named<GUI::LinkLabel>("link_location");
            link_location->set_text(link_destination);
            link_location->on_click = [link_destination] {
                auto link_directory = LexicalPath(link_destination.to_deprecated_string());
                Desktop::Launcher::open(URL::create_with_file_scheme(link_directory.dirname(), link_directory.basename()));
            };
        }
    } else {
        auto* link_location_widget = general_tab->find_descendant_of_type_named<GUI::Widget>("link_location_widget");
        general_tab->remove_child(*link_location_widget);
    }

    m_size_label = general_tab->find_descendant_of_type_named<GUI::Label>("size");
    m_size_label->set_text(S_ISDIR(st.st_mode)
            ? TRY("Calculating..."_string)
            : TRY(String::from_deprecated_string(human_readable_size_long(st.st_size, UseThousandsSeparator::Yes))));

    auto* owner = general_tab->find_descendant_of_type_named<GUI::Label>("owner");
    owner->set_text(String::formatted("{} ({})", owner_name, st.st_uid).release_value_but_fixme_should_propagate_errors());

    auto* group = general_tab->find_descendant_of_type_named<GUI::Label>("group");
    group->set_text(String::formatted("{} ({})", group_name, st.st_gid).release_value_but_fixme_should_propagate_errors());

    auto* created_at = general_tab->find_descendant_of_type_named<GUI::Label>("created_at");
    created_at->set_text(String::from_deprecated_string(GUI::FileSystemModel::timestamp_string(st.st_ctime)).release_value_but_fixme_should_propagate_errors());

    auto* last_modified = general_tab->find_descendant_of_type_named<GUI::Label>("last_modified");
    last_modified->set_text(String::from_deprecated_string(GUI::FileSystemModel::timestamp_string(st.st_mtime)).release_value_but_fixme_should_propagate_errors());

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

        if (FileSystem::exists(new_file)) {
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

ErrorOr<NonnullRefPtr<GUI::Button>> PropertiesWindow::make_button(String text, GUI::Widget& parent)
{
    auto button = TRY(parent.try_add<GUI::Button>(text));
    button->set_fixed_size(70, 22);
    return button;
}

void PropertiesWindow::close()
{
    GUI::Window::close();
    if (m_directory_statistics_calculator)
        m_directory_statistics_calculator->stop();
}

PropertiesWindow::DirectoryStatisticsCalculator::DirectoryStatisticsCalculator(DeprecatedString path)
{
    m_work_queue.enqueue(path);
}

void PropertiesWindow::DirectoryStatisticsCalculator::start()
{
    using namespace AK::TimeLiterals;
    VERIFY(!m_background_action);

    m_background_action = Threading::BackgroundAction<int>::construct(
        [this, strong_this = NonnullRefPtr(*this)](auto& task) -> ErrorOr<int> {
            auto timer = Core::ElapsedTimer();
            while (!m_work_queue.is_empty()) {
                auto base_directory = m_work_queue.dequeue();
                auto result = Core::Directory::for_each_entry(base_directory, Core::DirIterator::SkipParentAndBaseDir, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
                    if (task.is_canceled())
                        return Error::from_errno(ECANCELED);

                    struct stat st = {};
                    if (fstatat(directory.fd(), entry.name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0) {
                        perror("fstatat");
                        return IterationDecision::Continue;
                    }

                    if (S_ISDIR(st.st_mode)) {
                        auto full_path = LexicalPath::join(directory.path().string(), entry.name).string();
                        m_directory_count++;
                        m_work_queue.enqueue(full_path);
                    } else if (S_ISREG(st.st_mode) || S_ISLNK(st.st_mode)) {
                        m_file_count++;
                        m_total_size_in_bytes += st.st_size;
                    }

                    // Show the first update, then show any subsequent updates every 100ms.
                    if (!task.is_canceled() && on_update && (!timer.is_valid() || timer.elapsed_time() > 100_ms)) {
                        timer.start();
                        on_update(m_total_size_in_bytes, m_file_count, m_directory_count);
                    }

                    return IterationDecision::Continue;
                });
                if (result.is_error() && result.error().code() == ECANCELED)
                    return Error::from_errno(ECANCELED);
            }
            return 0;
        },
        [this](auto) -> ErrorOr<void> {
            if (on_update)
                on_update(m_total_size_in_bytes, m_file_count, m_directory_count);

            return {};
        },
        [](auto) {
            // Ignore the error.
        });
}

void PropertiesWindow::DirectoryStatisticsCalculator::stop()
{
    VERIFY(m_background_action);
    m_background_action->cancel();
}
