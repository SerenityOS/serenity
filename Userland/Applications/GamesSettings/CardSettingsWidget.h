/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CardGamePreview.h"
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Frame.h>
#include <LibGUI/IconView.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/ModelIndex.h>
#include <LibGUI/SettingsWindow.h>

namespace GamesSettings {

class CardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(CardSettingsWidget)
public:
    static ErrorOr<NonnullRefPtr<CardSettingsWidget>> try_create();
    ErrorOr<void> initialize();
    virtual ~CardSettingsWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    CardSettingsWidget() = default;

    bool set_card_back_image_path(StringView);
    String card_back_image_path() const;
    String card_front_images_set_name() const;

    Vector<ByteString> m_card_front_sets;

    RefPtr<CardGamePreview> m_preview_frame;
    RefPtr<GUI::ColorInput> m_background_color_input;
    RefPtr<GUI::ComboBox> m_card_front_images_combo_box;
    RefPtr<GUI::IconView> m_card_back_image_view;

    GUI::ModelIndex m_last_selected_card_back;
};

}
