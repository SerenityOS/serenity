/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <QPoint>
#include <QSettings>
#include <QSize>

namespace Ladybird {

class Settings : public QObject {
public:
    Settings();

    Optional<QPoint> last_position();
    void set_last_position(QPoint const& last_position);

    QSize last_size();
    void set_last_size(QSize const& last_size);

    bool is_maximized();
    void set_is_maximized(bool is_maximized);

    QString new_tab_page();
    void set_new_tab_page(QString const& page);

private:
    QSettings* m_qsettings;
};

}
