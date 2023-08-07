/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserSettingsWidget.h"
#include <Applications/BrowserSettings/BrowserSettingsWidgetGML.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>

struct ColorScheme {
    DeprecatedString title;
    DeprecatedString setting_value;
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

ErrorOr<NonnullRefPtr<BrowserSettingsWidget>> BrowserSettingsWidget::create()
{
    auto widget = TRY(try_make_ref_counted<BrowserSettingsWidget>());

    TRY(widget->load_from_gml(browser_settings_widget_gml));
    TRY(widget->setup());

    return widget;
}

ErrorOr<void> BrowserSettingsWidget::setup()
{
    m_homepage_url_textbox = find_descendant_of_type_named<GUI::TextBox>("homepage_url_textbox");
    m_homepage_url_textbox->set_text(Config::read_string("Browser"sv, "Preferences"sv, "Home"sv, Browser::default_homepage_url), GUI::AllowCallback::No);
    m_homepage_url_textbox->on_change = [&]() { set_modified(true); };

    m_new_tab_url_textbox = find_descendant_of_type_named<GUI::TextBox>("new_tab_url_textbox");
    m_new_tab_url_textbox->set_text(Config::read_string("Browser"sv, "Preferences"sv, "NewTab"sv, Browser::default_new_tab_url), GUI::AllowCallback::No);
    m_new_tab_url_textbox->on_change = [&]() { set_modified(true); };

    m_color_scheme_combobox = find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combobox");
    m_color_scheme_combobox->set_only_allow_values_from_model(true);
    m_color_scheme_combobox->set_model(adopt_ref(*new ColorSchemeModel()));
    m_color_scheme_combobox->set_selected_index(0, GUI::AllowCallback::No);
    set_color_scheme(Config::read_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, Browser::default_color_scheme));
    m_color_scheme_combobox->on_change = [&](auto, auto) { set_modified(true); };

    m_show_bookmarks_bar_checkbox = find_descendant_of_type_named<GUI::CheckBox>("show_bookmarks_bar_checkbox");
    m_show_bookmarks_bar_checkbox->set_checked(Config::read_bool("Browser"sv, "Preferences"sv, "ShowBookmarksBar"sv, Browser::default_show_bookmarks_bar), GUI::AllowCallback::No);
    m_show_bookmarks_bar_checkbox->on_checked = [&](auto) { set_modified(true); };

    m_enable_search_engine_checkbox = find_descendant_of_type_named<GUI::CheckBox>("enable_search_engine_checkbox");
    m_search_engine_combobox_group = find_descendant_of_type_named<GUI::Widget>("search_engine_combobox_group");
    m_search_engine_combobox = find_descendant_of_type_named<GUI::ComboBox>("search_engine_combobox");
    m_custom_search_engine_group = find_descendant_of_type_named<GUI::Widget>("custom_search_engine_group");
    m_custom_search_engine_textbox = find_descendant_of_type_named<GUI::TextBox>("custom_search_engine_textbox");
    m_custom_search_engine_textbox->on_change = [&]() { set_modified(true); };

    m_enable_search_engine_checkbox->on_checked = [this](bool checked) {
        m_search_engine_combobox_group->set_enabled(checked);
        m_custom_search_engine_group->set_enabled(checked && m_is_custom_search_engine);
        set_modified(true);
    };

    Vector<GUI::JsonArrayModel::FieldSpec> search_engine_fields;
    search_engine_fields.empend("title", "Title"_short_string, Gfx::TextAlignment::CenterLeft);
    search_engine_fields.empend("url_format", "Url format"_string, Gfx::TextAlignment::CenterLeft);
    auto search_engines_model = GUI::JsonArrayModel::create(DeprecatedString::formatted("{}/SearchEngines.json", Core::StandardPaths::config_directory()), move(search_engine_fields));
    search_engines_model->invalidate();
    Vector<JsonValue> custom_search_engine;
    custom_search_engine.append("Custom...");
    custom_search_engine.append("");
    TRY(search_engines_model->add(move(custom_search_engine)));

    m_search_engine_combobox->set_model(move(search_engines_model));
    m_search_engine_combobox->set_only_allow_values_from_model(true);
    m_search_engine_combobox->on_change = [this](AK::DeprecatedString const&, GUI::ModelIndex const& cursor_index) {
        auto url_format = m_search_engine_combobox->model()->index(cursor_index.row(), 1).data().to_deprecated_string();
        m_is_custom_search_engine = url_format.is_empty();
        m_custom_search_engine_group->set_enabled(m_is_custom_search_engine);
        set_modified(true);
    };
    set_search_engine_url(Config::read_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, Browser::default_search_engine));

    m_auto_close_download_windows_checkbox = find_descendant_of_type_named<GUI::CheckBox>("auto_close_download_windows_checkbox");
    m_auto_close_download_windows_checkbox->set_checked(Config::read_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, Browser::default_close_download_widget_on_finish), GUI::AllowCallback::No);
    m_auto_close_download_windows_checkbox->on_checked = [&](auto) { set_modified(true); };

    return {};
}

void BrowserSettingsWidget::set_color_scheme(StringView color_scheme)
{
    bool found_color_scheme = false;
    for (int item_index = 0; item_index < m_color_scheme_combobox->model()->row_count(); ++item_index) {
        auto scheme = m_color_scheme_combobox->model()->index(item_index, 1).data().to_deprecated_string();
        if (scheme == color_scheme) {
            m_color_scheme_combobox->set_selected_index(item_index, GUI::AllowCallback::No);
            found_color_scheme = true;
            break;
        }
    }
    if (!found_color_scheme)
        m_color_scheme_combobox->set_selected_index(0, GUI::AllowCallback::No);
}

void BrowserSettingsWidget::set_search_engine_url(StringView url)
{
    if (url.is_empty()) {
        m_enable_search_engine_checkbox->set_checked(false, GUI::AllowCallback::No);
        m_search_engine_combobox_group->set_enabled(false);
        m_custom_search_engine_group->set_enabled(false);
        m_search_engine_combobox->set_selected_index(0, GUI::AllowCallback::No);
    } else {
        m_enable_search_engine_checkbox->set_checked(true, GUI::AllowCallback::No);
        m_search_engine_combobox_group->set_enabled(true);

        bool found_url = false;
        for (int item_index = 0; item_index < m_search_engine_combobox->model()->row_count(); ++item_index) {
            auto url_format = m_search_engine_combobox->model()->index(item_index, 1).data().to_deprecated_string();
            if (url_format == url) {
                m_search_engine_combobox->set_selected_index(item_index, GUI::AllowCallback::No);
                found_url = true;
                break;
            }
        }

        if (!found_url) {
            m_is_custom_search_engine = true;
            m_custom_search_engine_textbox->set_text(url, GUI::AllowCallback::No);
            // We assume that "Custom" is the last item
            m_search_engine_combobox->set_selected_index(m_search_engine_combobox->model()->row_count() - 1, GUI::AllowCallback::No);
            m_custom_search_engine_group->set_enabled(true);
        } else {
            m_custom_search_engine_group->set_enabled(false);
        }
    }
}

void BrowserSettingsWidget::apply_settings()
{
    auto homepage_url = m_homepage_url_textbox->text();
    if (!URL(homepage_url).is_valid()) {
        GUI::MessageBox::show_error(this->window(), "The homepage URL you have entered is not valid"sv);
        m_homepage_url_textbox->select_all();
        m_homepage_url_textbox->set_focus(true);
        return;
    }
    Config::write_string("Browser"sv, "Preferences"sv, "Home"sv, homepage_url);

    auto new_tab_url = m_new_tab_url_textbox->text();
    if (!URL(new_tab_url).is_valid()) {
        GUI::MessageBox::show_error(this->window(), "The new tab URL you have entered is not valid"sv);
        m_new_tab_url_textbox->select_all();
        m_new_tab_url_textbox->set_focus(true);
        return;
    }
    Config::write_string("Browser"sv, "Preferences"sv, "NewTab"sv, new_tab_url);

    Config::write_bool("Browser"sv, "Preferences"sv, "ShowBookmarksBar"sv, m_show_bookmarks_bar_checkbox->is_checked());

    auto color_scheme_index = m_color_scheme_combobox->selected_index();
    auto color_scheme = m_color_scheme_combobox->model()->index(color_scheme_index, 1).data().to_deprecated_string();
    Config::write_string("Browser"sv, "Preferences"sv, "ColorScheme"sv, color_scheme);

    if (!m_enable_search_engine_checkbox->is_checked()) {
        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, {});
    } else if (m_is_custom_search_engine) {
        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, m_custom_search_engine_textbox->text());
    } else {
        auto selected_index = m_search_engine_combobox->selected_index();
        auto url = m_search_engine_combobox->model()->index(selected_index, 1).data().to_deprecated_string();
        Config::write_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, url);
    }

    Config::write_bool("Browser"sv, "Preferences"sv, "CloseDownloadWidgetOnFinish"sv, m_auto_close_download_windows_checkbox->is_checked());
}

void BrowserSettingsWidget::reset_default_values()
{
    m_homepage_url_textbox->set_text(Browser::default_homepage_url);
    m_new_tab_url_textbox->set_text(Browser::default_new_tab_url);
    m_show_bookmarks_bar_checkbox->set_checked(Browser::default_show_bookmarks_bar);
    set_color_scheme(Browser::default_color_scheme);
    m_auto_close_download_windows_checkbox->set_checked(Browser::default_close_download_widget_on_finish);
    set_search_engine_url(Browser::default_search_engine);
}
