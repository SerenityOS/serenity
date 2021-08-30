/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGUI/Dialog.h>

class SettingsDialog : public GUI::Dialog {
    C_OBJECT(SettingsDialog)
public:
    int columns() const { return m_columns; }
    int rows() const { return m_rows; }
    int cell_size() const { return m_cell_size; }

private:
    SettingsDialog(GUI::Window* parent, int, int, int);

    int m_columns, m_rows, m_cell_size;
};
