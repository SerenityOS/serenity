/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <QBoxLayout>
#include <QDialog>
#include <QMainWindow>

#pragma once

class SettingsDialog : public QDialog {
    Q_OBJECT
public:
    explicit SettingsDialog(QMainWindow* window);

private:
    QBoxLayout* m_layout;
    QMainWindow* m_window { nullptr };
};
