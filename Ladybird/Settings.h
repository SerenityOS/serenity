/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <AK/DeprecatedString.h>
#include <QSettings>

namespace Browser {

class Settings {
public:
    Settings(QObject* parent);

    QString homepage();
    void set_homepage(QString const& homepage);

private:
    QSettings* m_qsettings;
};

}
