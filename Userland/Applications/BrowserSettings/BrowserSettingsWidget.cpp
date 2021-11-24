/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserSettingsWidget.h"
#include <Applications/BrowserSettings/BrowserSettingsWidgetGML.h>
#include <LibConfig/Client.h>

BrowserSettingsWidget::BrowserSettingsWidget()
{
    load_from_gml(browser_settings_widget_gml);

    m_homepage_url_textbox = find_descendant_of_type_named<GUI::TextBox>("homepage_url_textbox");
    m_homepage_url_textbox->set_text(Config::read_string("Browser", "Preferences", "Home", "about:blank"));

    m_auto_close_download_windows_checkbox = find_descendant_of_type_named<GUI::CheckBox>("auto_close_download_windows_checkbox");
    m_auto_close_download_windows_checkbox->set_checked(Config::read_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", false), GUI::AllowCallback::No);
}

BrowserSettingsWidget::~BrowserSettingsWidget()
{
}

void BrowserSettingsWidget::apply_settings()
{
    // TODO: Ensure that the URL is valid, as we do in the BrowserWindow's change-homepage dialog
    Config::write_string("Browser", "Preferences", "Home", m_homepage_url_textbox->text());

    Config::write_bool("Browser", "Preferences", "CloseDownloadWidgetOnFinish", m_auto_close_download_windows_checkbox->is_checked());
}
