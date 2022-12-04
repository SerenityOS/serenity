/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGUI/Dialog.h>

class SettingsDialog : public GUI::Dialog {
    C_OBJECT(SettingsDialog)
public:
    size_t board_rows() const { return m_board_rows; }
    size_t board_columns() const { return m_board_columns; }
    StringView color_scheme() const { return m_color_scheme; }

private:
    SettingsDialog(GUI::Window* parent, size_t board_rows, size_t board_columns, StringView color_scheme);

    size_t m_board_rows;
    size_t m_board_columns;
    DeprecatedString m_color_scheme;
};
