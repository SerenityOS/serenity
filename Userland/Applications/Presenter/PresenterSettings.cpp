/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PresenterSettings.h"
#include <Applications/Presenter/PresenterSettingsFooterGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/TextBox.h>

PresenterSettingsFooterWidget::PresenterSettingsFooterWidget()
{
    load_from_gml(presenter_settings_footer_gml);
    m_override_footer = find_descendant_of_type_named<GUI::CheckBox>("override_footer");
    m_enable_footer = find_descendant_of_type_named<GUI::CheckBox>("enable_footer");
    m_footer_text = find_descendant_of_type_named<GUI::TextEditor>("footer_text");

    m_override_footer->on_checked = [this](auto) { this->on_footer_settings_override_change(); this->set_modified(true); };
    m_enable_footer->on_checked = [this](auto) { this->set_modified(true); };
    m_footer_text->on_change = [this] { this->set_modified(true); };

    // Our implementation resets the UI settings input widgets to the stored config values.
    cancel_settings();
    on_footer_settings_override_change();
}

void PresenterSettingsFooterWidget::apply_settings()
{
    Config::write_bool("Presenter"sv, "Footer"sv, "OverrideFooter"sv, m_override_footer->is_checked());
    Config::write_bool("Presenter"sv, "Footer"sv, "EnableFooter"sv, m_enable_footer->is_checked());
    Config::write_string("Presenter"sv, "Footer"sv, "FooterText"sv, m_footer_text->text());
}

// Enables or disables footer override settings input to make it clear when they have effect and when not.
void PresenterSettingsFooterWidget::on_footer_settings_override_change()
{
    auto is_overridden = m_override_footer->is_checked();
    m_enable_footer->set_enabled(is_overridden);
    m_footer_text->set_enabled(is_overridden);
}

void PresenterSettingsFooterWidget::cancel_settings()
{
    auto override_state = Config::read_bool("Presenter"sv, "Footer"sv, "OverrideFooter"sv, false);
    auto enable_state = Config::read_bool("Presenter"sv, "Footer"sv, "EnableFooter"sv, true);
    auto footer_text_state = Config::read_string("Presenter"sv, "Footer"sv, "FooterText"sv, "{presentation_title} - {slide_title}"sv);

    m_override_footer->set_checked(override_state);
    m_enable_footer->set_checked(enable_state);
    m_footer_text->set_text(footer_text_state);
}
