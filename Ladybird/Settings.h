/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <QSettings>

namespace Browser {

class Settings : public QObject {
public:
    Settings();

    QString homepage();
    void set_homepage(QString const& homepage);

    QString new_tab_page();
    void set_new_tab_page(QString const& page);

private:
    QSettings* m_qsettings;
};

}
