/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ContentFilterSettingsWidget.h"
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Menu.h>
#include <LibGUI/SettingsWindow.h>

class AutoplayAllowlistModel : public DomainListModel {
public:
    virtual ErrorOr<String> filter_list_file_path() const override;
    virtual void reset_default_values() override;
};

namespace BrowserSettings {

class AutoplaySettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(AutoplaySettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<AutoplaySettingsWidget>> try_create();
    ErrorOr<void> initialize();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    AutoplaySettingsWidget() = default;

    void set_allowlist_model(NonnullRefPtr<AutoplayAllowlistModel> model);

    RefPtr<GUI::Menu> m_entry_context_menu;
    RefPtr<GUI::CheckBox> m_allow_autoplay_on_all_websites_checkbox;
    RefPtr<GUI::Button> m_add_website_button;
    RefPtr<GUI::ListView> m_allowlist_view;
    RefPtr<AutoplayAllowlistModel> m_allowlist_model;
};

}
