/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserSettingsWidget.h"
#include <Applications/BrowserSettings/BrowserSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/Model.h>

static String default_homepage_url = "file:///res/html/misc/welcome.html";
static String default_search_engine = "";
static String default_color_scheme = "auto";
static bool default_show_bookmarks_bar = true;
static bool default_auto_close_download_windows = false;

struct ColorScheme {
    String title;
    String setting_value;
};

class ColorSchemeModel final : public GUI::Model {

public:
    ColorSchemeModel()
    {
        m_color_schemes.empend("Follow system theme", "auto");
        m_color_schemes.empend("Dark", "dark");
        m_color_schemes.empend("Light", "light");
    }

    virtual ~ColorSchemeModel() = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 3; }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 2; }

    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole role) const override
    {
        if (role == GUI::ModelRole::TextAlignment)
            return Gfx::TextAlignment::CenterLeft;
        if (role == GUI::ModelRole::Display) {
            if (index.column() == 0)
                return m_color_schemes[index.row()].title;
            else
                return m_color_schemes[index.row()].setting_value;
        }

        return {};
    }

private:
    Vector<ColorScheme> m_color_schemes;
};

BrowserSettingsWidget::BrowserSettingsWidget()
{
    load_from_gml(browser_settings_widget_gml);

    m_homepage_url_textbox = find_descendant_of_type_named<GUI::TextBox>("homepage_url_textbox");
    m_homepage_url_textbox->set_text(Config::read_string("Browser", "Preferences", "Home", default_homepage_url));

    m_color_scheme_combobox = find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combobox");
    m_color_scheme_combobox->set_only_allow_values_from_model(true);
    m_color_scheme_combobox->set_model(adopt_ref(*new ColorSchemeModel()));
    m_color_scheme_combobox->set_selected_index(0);
    set_color_scheme(Config::read_string("Browser", "Preferences", "ColorScheme", default_color_scheme));

    m_show_bookmarks_bar_checkbox = find_descendant_of_type_named<GUI::CheckBox>("show_bookmarks_bar_checkbox");
    m_show_bookmarks_bar_checkbox->set_checked(Config::read_bool("Browser", "Preferences", "ShowBookmarksBar", default_show_bookmarks_bar), GUI::AllowCallback::No);

    m_enable_search_engine_checkbox = find_descendant_of_type_named<GUI::CheckBox>("enable_search_engine_checkbox");
    m_search_engine_combobox_group = find_descendant_of_type_named<GUI::Widget>("search_engine_combobox_group");
    m_search_engine_combobox = find_descendant_of_type_named<GUI::ComboBox>("search_engine_combobox");
    m_custom_search_engine_group = find_descendant_of_type_named<GUI::Widget>("custom_search_engine_group");
    m_custom_search_engine_textbox = find_descendant_of_type_named<GUI::TextBox>("custom_search_engine_textbox");

    m_enable_search_engine_checkbox->on_checked = [this](bool checked) {
        m_search_engine_combobox_group->set_enabled(checked);
        m_custom_search_engine_group->set_enabled(checked && m_is_custom_search_engine);
    };

    Vector<GUI::JsonArrayModel::FieldSpec> search_engine_fields;
    search_engine_fields.empend("title", "Title", Gfx::TextAlignment::CenterLeft);
    search_engine_fields.empend("url_format", "Url format", Gfx::TextAlignment::CenterLeft);
    auto search_engines_model = GUI::JsonArrayModel::create(String::formatted("{}/SearchEngines.json", Core::StandardPaths::config_directory()), move(search_engine_fields));
    search_engines_model->invalidate();
    Vector<JsonValue> custom_search_engine;
    custom_search_engine.append("Custom...");
    custom_search_engine.append("");
    search_engines_model->add(move(custom_search_engine));

    m_search_engine_combobox->set_model(move(search_engines_model));
    m_search_engine_combobox->set_only_allow_values_from_model(true);
    m_search_engine_combobox->on_change = [this](AK::String const&, GUI::ModelIndex const& cursor_index) {
        auto url_format = m_search_engine_combobox->model()->index(cursor_index.row(), 1).data().to_string();
        m_is_custom_search_engine = url_format.is_empty();
        m_custom_search_engine_group->set_enabled(m_is_custom_search_engine);
    };
    set_search_engine_url(Config::read_string("Browser", "Preferences", "SearchEngine", default_search_engine));

    m_auto_close_download_windows_checkbox = find_descendant_of_type_named<GUI::CheckBox>("auto_close_download_windows_checkbox");
    m_auto_close_download_windows_checkbox->set_checked(Config::read_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", default_auto_close_download_windows), GUI::AllowCallback::No);
}

void BrowserSettingsWidget::set_color_scheme(StringView color_scheme)
{
    bool found_color_scheme = false;
    for (int item_index = 0; item_index < m_color_scheme_combobox->model()->row_count(); ++item_index) {
        auto scheme = m_color_scheme_combobox->model()->index(item_index, 1).data().to_string();
        if (scheme == color_scheme) {
            m_color_scheme_combobox->set_selected_index(item_index);
            found_color_scheme = true;
            break;
        }
    }
    if (!found_color_scheme)
        m_color_scheme_combobox->set_selected_index(0);
}

void BrowserSettingsWidget::set_search_engine_url(StringView url)
{
    if (url.is_empty()) {
        m_enable_search_engine_checkbox->set_checked(false);
        m_search_engine_combobox_group->set_enabled(false);
        m_custom_search_engine_group->set_enabled(false);
        m_search_engine_combobox->set_selected_index(0);
    } else {
        m_enable_search_engine_checkbox->set_checked(true);
        m_search_engine_combobox_group->set_enabled(true);

        bool found_url = false;
        for (int item_index = 0; item_index < m_search_engine_combobox->model()->row_count(); ++item_index) {
            auto url_format = m_search_engine_combobox->model()->index(item_index, 1).data().to_string();
            if (url_format == url) {
                m_search_engine_combobox->set_selected_index(item_index);
                found_url = true;
                break;
            }
        }

        if (!found_url) {
            m_is_custom_search_engine = true;
            m_custom_search_engine_textbox->set_text(url);
            // We assume that "Custom" is the last item
            m_search_engine_combobox->set_selected_index(m_search_engine_combobox->model()->row_count() - 1);
            m_custom_search_engine_group->set_enabled(true);
        } else {
            m_custom_search_engine_group->set_enabled(false);
        }
    }
}

void BrowserSettingsWidget::apply_settings()
{
    // TODO: Ensure that the URL is valid, as we do in the BrowserWindow's change-homepage dialog
    Config::write_string("Browser", "Preferences", "Home", m_homepage_url_textbox->text());

    Config::write_bool("Browser", "Preferences", "ShowBookmarksBar", m_show_bookmarks_bar_checkbox->is_checked());

    auto color_scheme_index = m_color_scheme_combobox->selected_index();
    auto color_scheme = m_color_scheme_combobox->model()->index(color_scheme_index, 1).data().to_string();
    Config::write_string("Browser", "Preferences", "ColorScheme", color_scheme);

    if (!m_enable_search_engine_checkbox->is_checked()) {
        Config::write_string("Browser", "Preferences", "SearchEngine", {});
    } else if (m_is_custom_search_engine) {
        Config::write_string("Browser", "Preferences", "SearchEngine", m_custom_search_engine_textbox->text());
    } else {
        auto selected_index = m_search_engine_combobox->selected_index();
        auto url = m_search_engine_combobox->model()->index(selected_index, 1).data().to_string();
        Config::write_string("Browser", "Preferences", "SearchEngine", url);
    }

    Config::write_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", m_auto_close_download_windows_checkbox->is_checked());
}

void BrowserSettingsWidget::reset_default_values()
{
    m_homepage_url_textbox->set_text(default_homepage_url);
    m_show_bookmarks_bar_checkbox->set_checked(default_show_bookmarks_bar);
    set_color_scheme(default_color_scheme);
    m_auto_close_download_windows_checkbox->set_checked(default_auto_close_download_windows);
    set_search_engine_url(default_search_engine);
}
