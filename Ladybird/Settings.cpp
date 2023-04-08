/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Settings.h"

namespace Browser {

Settings::Settings()
{
    m_qsettings = new QSettings("Serenity", "Ladybird", this);
}

QString Settings::new_tab_page()
{
    return m_qsettings->value("new_tab_page", "about:blank").toString();
}

void Settings::set_new_tab_page(QString const& page)
{
    m_qsettings->setValue("new_tab_page", page);
}

QStringList Settings::bookmarks()
{
    return m_qsettings->value("bookmarks", {}).toStringList();
}

void Settings::set_bookmarks(QStringList urls)
{
    m_qsettings->setValue("bookmarks", urls);
}

}
