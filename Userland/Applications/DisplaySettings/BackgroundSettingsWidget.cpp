/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BackgroundSettingsWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <Applications/DisplaySettings/BackgroundSettingsGML.h>
#include <LibConfig/Client.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/FileTypeFilter.h>
#include <LibGUI/IconView.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

namespace DisplaySettings {

ErrorOr<NonnullRefPtr<BackgroundSettingsWidget>> BackgroundSettingsWidget::try_create(bool& background_settings_changed)
{
    auto background_settings_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BackgroundSettingsWidget(background_settings_changed)));

    TRY(background_settings_widget->m_modes.try_append("Tile"_string));
    TRY(background_settings_widget->m_modes.try_append("Center"_string));
    TRY(background_settings_widget->m_modes.try_append("Stretch"_string));
    TRY(background_settings_widget->m_modes.try_append("Fill"_string));

    TRY(background_settings_widget->create_frame());
    TRY(background_settings_widget->load_current_settings());

    return background_settings_widget;
}

BackgroundSettingsWidget::BackgroundSettingsWidget(bool& background_settings_changed)
    : m_background_settings_changed { background_settings_changed }
{
}

/// A thin wrapper over FileSystemModel for wallpapers in /res/wallpapers. The
/// only change from a plain FileSystemModel is row zero reserved for "None"
/// (and this assumes no nested directories).
class WallpapersModel final : public GUI::Model
    , GUI::ModelClient {
public:
    static NonnullRefPtr<WallpapersModel> create()
    {
        return adopt_ref(*new WallpapersModel(GUI::FileSystemModel::create("/res/wallpapers")));
    }

    virtual ~WallpapersModel() override
    {
        m_wallpaper_folder->unregister_client(*this);
    }

    virtual int row_count(GUI::ModelIndex const&) const override
    {
        // Index zero is reserved for "None".
        return m_wallpaper_folder->row_count() + 1;
    }

    virtual int column_count(GUI::ModelIndex const&) const override
    {
        return 1;
    }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        static GUI::Icon no_wallpaper_icon = GUI::Icon::default_icon("no-wallpaper"sv);
        if (index.row() == 0) {
            if (role == GUI::ModelRole::Icon)
                return no_wallpaper_icon;
            if (role == GUI::ModelRole::Display)
                return "None";
            return {};
        }
        return m_wallpaper_folder->data(fs_index(index, role), role);
    }

    AK::ByteString full_path(GUI::ModelIndex const& index)
    {
        if (index.row() == 0)
            return "";
        return m_wallpaper_folder->full_path(fs_index(index));
    }

    virtual void model_did_update(unsigned flags) override { did_update(flags); }

    GUI::ModelIndex index_for_path(ByteString path)
    {
        auto wallpaper_index = m_wallpaper_folder->index(path, GUI::FileSystemModel::Column::Name);
        if (wallpaper_index.is_valid())
            return create_index(wallpaper_index.row() + 1, 0);
        return create_index(0, 0); // Default to "None".
    }

private:
    GUI::ModelIndex fs_index(GUI::ModelIndex const& index, GUI::ModelRole role = GUI::ModelRole::Display) const
    {
        VERIFY(index.row() > 0);
        return m_wallpaper_folder->index(index.row() - 1,
            role == GUI::ModelRole::Display ? GUI::FileSystemModel::Column::Name : GUI::FileSystemModel::Column::Icon);
    }

    WallpapersModel(NonnullRefPtr<GUI::FileSystemModel> wallpaper_folder)
        : m_wallpaper_folder(move(wallpaper_folder))
    {
        m_wallpaper_folder->register_client(*this);
    }

    NonnullRefPtr<GUI::FileSystemModel> m_wallpaper_folder;
};

ErrorOr<void> BackgroundSettingsWidget::create_frame()
{
    TRY(load_from_gml(background_settings_gml));

    m_monitor_widget = *find_descendant_of_type_named<DisplaySettings::MonitorWidget>("monitor_widget");

    m_wallpaper_view = *find_descendant_of_type_named<GUI::IconView>("wallpaper_view");
    m_wallpaper_view->set_model(WallpapersModel::create());
    m_wallpaper_view->on_selection_change = [this] {
        ByteString path;
        if (!m_wallpaper_view->selection().is_empty()) {
            auto index = m_wallpaper_view->selection().first();
            path = static_cast<WallpapersModel*>(m_wallpaper_view->model())->full_path(index);
        }

        m_monitor_widget->set_wallpaper(path);
        set_modified(true);
    };

    m_context_menu = GUI::Menu::construct();
    auto const file_manager_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"sv));
    m_show_in_file_manager_action = GUI::Action::create("Show in File Manager", file_manager_icon, [this](GUI::Action const&) {
        LexicalPath path { m_monitor_widget->wallpaper().value().to_byte_string() };
        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
    });
    m_context_menu->add_action(*m_show_in_file_manager_action);

    m_context_menu->add_separator();
    m_copy_action = GUI::CommonActions::make_copy_action(
        [this](auto&) {
            auto wallpaper = m_monitor_widget->wallpaper();
            if (wallpaper.has_value()) {
                auto url = URL::create_with_file_scheme(wallpaper.value()).to_byte_string();
                GUI::Clipboard::the().set_data(url.bytes(), "text/uri-list");
            }
        },
        this);
    m_context_menu->add_action(*m_copy_action);

    m_wallpaper_view->on_context_menu_request = [&](GUI::ModelIndex const& index, GUI::ContextMenuEvent const& event) {
        if (index.is_valid()) {
            m_context_menu->popup(event.screen_position(), m_show_in_file_manager_action);
        }
    };

    auto& button = *find_descendant_of_type_named<GUI::Button>("wallpaper_open_button");
    button.on_click = [this](auto) {
        FileSystemAccessClient::OpenFileOptions options {
            .window_title = "Select Wallpaper"sv,
            .path = "/res/wallpapers"sv,
            .allowed_file_types = { { GUI::FileTypeFilter::image_files() } }
        };
        auto response = FileSystemAccessClient::Client::the().open_file(window(), options);
        if (response.is_error())
            return;
        m_wallpaper_view->selection().clear();
        m_monitor_widget->set_wallpaper(response.release_value().filename());
        m_background_settings_changed = true;
        set_modified(true);
    };

    m_mode_combo = *find_descendant_of_type_named<GUI::ComboBox>("mode_combo");
    m_mode_combo->set_only_allow_values_from_model(true);
    m_mode_combo->set_model(*GUI::ItemListModel<String>::create(m_modes));
    m_mode_combo->on_change = [this](auto&, GUI::ModelIndex const& index) {
        m_monitor_widget->set_wallpaper_mode(m_modes.at(index.row()));
        m_background_settings_changed = true;
        set_modified(true);
    };

    m_color_input = *find_descendant_of_type_named<GUI::ColorInput>("color_input");
    m_color_input->set_color_has_alpha_channel(false);
    m_color_input->set_color_picker_title("Select Desktop Color");
    m_color_input->on_change = [this] {
        m_monitor_widget->set_background_color(m_color_input->color());
        m_background_settings_changed = true;
        set_modified(true);
    };

    return {};
}

ErrorOr<void> BackgroundSettingsWidget::load_current_settings()
{
    auto ws_config = TRY(Core::ConfigFile::open("/etc/WindowServer.ini"));

    auto selected_wallpaper = GUI::Desktop::the().wallpaper_path();
    auto index = static_cast<WallpapersModel*>(m_wallpaper_view->model())->index_for_path(selected_wallpaper);
    m_wallpaper_view->set_cursor(index, GUI::AbstractView::SelectionUpdate::Set);
    m_monitor_widget->set_wallpaper(selected_wallpaper);

    auto mode = TRY(String::from_byte_string(ws_config->read_entry("Background", "Mode", "Center")));
    if (!m_modes.contains_slow(mode)) {
        warnln("Invalid background mode '{}' in WindowServer config, falling back to 'Center'", mode);
        mode = "Center"_string;
    }
    m_monitor_widget->set_wallpaper_mode(mode);
    m_mode_combo->set_selected_index(m_modes.find_first_index(mode).value_or(0), GUI::AllowCallback::No);

    auto palette_desktop_color = palette().desktop_background();
    auto background_color = ws_config->read_entry("Background", "Color", "");

    if (!background_color.is_empty()) {
        auto opt_color = Color::from_string(background_color);
        if (opt_color.has_value())
            palette_desktop_color = opt_color.value();
    }

    m_color_input->set_color(palette_desktop_color, GUI::AllowCallback::No);
    m_monitor_widget->set_background_color(palette_desktop_color);
    m_background_settings_changed = false;

    return {};
}

void BackgroundSettingsWidget::apply_settings()
{
    // We need to provide an empty path (not OptionalNone) to set_wallpaper to save a solid color wallpaper.
    auto wallpaper_path = m_monitor_widget->wallpaper().value_or(""sv);
    if (!GUI::Desktop::the().set_wallpaper(wallpaper_path)) {
        if (!wallpaper_path.is_empty())
            GUI::MessageBox::show_error(window(), MUST(String::formatted("Unable to load file {} as wallpaper", wallpaper_path)));
        else
            GUI::MessageBox::show_error(window(), "Unable to set wallpaper"sv);
    }

    GUI::Desktop::the().set_background_color(m_color_input->text());
    GUI::Desktop::the().set_wallpaper_mode(m_monitor_widget->wallpaper_mode());
}

}
