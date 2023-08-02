/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <QDialog>
#include <QFormLayout>
#include <QLineEdit>
#include <QMainWindow>
#include <QPushButton>

#pragma once

namespace Ladybird {

class SettingsDialog : public QDialog {
    Q_OBJECT
public:
    explicit SettingsDialog(QMainWindow* window);

    void save();

    virtual void closeEvent(QCloseEvent*) override;

private:
    QFormLayout* m_layout;
    QPushButton* m_ok_button { nullptr };
    QLineEdit* m_new_tab_page { nullptr };
    QMainWindow* m_window { nullptr };
};

}
