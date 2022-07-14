/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SettingsDialog.h"

SettingsDialog::SettingsDialog(QMainWindow* window)
    : m_window(window)
{
    m_layout = new QBoxLayout(QBoxLayout::Direction::TopToBottom, this);

    setWindowTitle("Settings");
    resize(340, 400);
    setLayout(m_layout);
    show();
    setFocus();
}
