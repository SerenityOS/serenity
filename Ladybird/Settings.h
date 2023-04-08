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

    QString new_tab_page();
    void set_new_tab_page(QString const& page);

    QStringList bookmarks();
    void set_bookmarks(QStringList urls);

private:
    QSettings* m_qsettings;
};

}
