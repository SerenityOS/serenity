/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <QCheckBox>
#include <QDialog>
#include <QFormLayout>
#include <QLineEdit>
#include <QMainWindow>
#include <QPushButton>
#include <QToolButton>

#pragma once

namespace Ladybird {

class SettingsDialog : public QDialog {
    Q_OBJECT
public:
    explicit SettingsDialog(QMainWindow* window);

    void save();

    virtual void closeEvent(QCloseEvent*) override;

private:
    void setup_search_engines();

    QFormLayout* m_layout;
    QPushButton* m_ok_button { nullptr };
    QMainWindow* m_window { nullptr };
    OwnPtr<QLineEdit> m_new_tab_page;
    OwnPtr<QCheckBox> m_enable_search;
    OwnPtr<QToolButton> m_search_engine_dropdown;
    OwnPtr<QCheckBox> m_enable_autocomplete;
    OwnPtr<QToolButton> m_autocomplete_engine_dropdown;
};

}
