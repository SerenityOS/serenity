/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChessGamePreview.h"
#include <AK/Array.h>
#include <AK/StringView.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGfx/Color.h>

namespace GamesSettings {

class ChessSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(ChessSettingsWidget)
public:
    static ErrorOr<NonnullRefPtr<ChessSettingsWidget>> try_create();
    ErrorOr<void> initialize();
    virtual ~ChessSettingsWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    ChessSettingsWidget() = default;

    Vector<ByteString> m_piece_sets;

    RefPtr<GamesSettings::ChessGamePreview> m_preview;
    RefPtr<GUI::ComboBox> m_piece_set_combobox;
    RefPtr<GUI::ComboBox> m_board_theme_combobox;
    RefPtr<GUI::CheckBox> m_show_coordinates_checkbox;
    RefPtr<GUI::CheckBox> m_highlight_checks_checkbox;
};

}
