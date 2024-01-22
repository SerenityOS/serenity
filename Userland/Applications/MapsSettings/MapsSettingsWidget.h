/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>

namespace MapsSettings {

class MapsSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(MapsSettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<MapsSettingsWidget>> try_create();
    ErrorOr<void> initialize();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    MapsSettingsWidget() = default;

    void set_tile_provider(StringView url);

    RefPtr<GUI::ComboBox> m_tile_provider_combobox;
    RefPtr<GUI::Widget> m_custom_tile_provider_group;
    RefPtr<GUI::TextBox> m_custom_tile_provider_textbox;
    bool m_is_custom_tile_provider { false };
};

}
