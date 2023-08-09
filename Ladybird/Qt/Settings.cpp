/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Settings.h"
#include "StringUtils.h"
#include <AK/URL.h>
#include <BrowserSettings/Defaults.h>
#include <Ladybird/Utilities.h>

namespace Ladybird {

static QString rebase_default_url_on_serenity_resource_root(StringView default_url)
{
    URL url { default_url };
    Vector<DeprecatedString> paths;

    for (auto segment : s_serenity_resource_root.split('/'))
        paths.append(move(segment));

    for (size_t i = 0; i < url.path_segment_count(); ++i)
        paths.append(url.path_segment_at_index(i));

    url.set_paths(move(paths));

    return qstring_from_ak_deprecated_string(url.to_deprecated_string());
}

Settings::Settings()
{
    m_qsettings = new QSettings("Serenity", "Ladybird", this);
}

Optional<QPoint> Settings::last_position()
{
    if (m_qsettings->contains("last_position"))
        return m_qsettings->value("last_position", QPoint()).toPoint();
    return {};
}

void Settings::set_last_position(QPoint const& last_position)
{
    m_qsettings->setValue("last_position", last_position);
}

QSize Settings::last_size()
{
    return m_qsettings->value("last_size", QSize(800, 600)).toSize();
}

void Settings::set_last_size(QSize const& last_size)
{
    m_qsettings->setValue("last_size", last_size);
}

bool Settings::is_maximized()
{
    return m_qsettings->value("is_maximized", QVariant(false)).toBool();
}

void Settings::set_is_maximized(bool is_maximized)
{
    m_qsettings->setValue("is_maximized", is_maximized);
}

QString Settings::new_tab_page()
{
    static auto const default_new_tab_url = rebase_default_url_on_serenity_resource_root(Browser::default_new_tab_url);
    return m_qsettings->value("new_tab_page", default_new_tab_url).toString();
}

void Settings::set_new_tab_page(QString const& page)
{
    m_qsettings->setValue("new_tab_page", page);
}

}
