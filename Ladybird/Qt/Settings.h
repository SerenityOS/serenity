/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/OwnPtr.h>
#include <QPoint>
#include <QSettings>
#include <QSize>

namespace Ladybird {

class Settings : public QObject {
public:
    Settings(Settings const&) = delete;
    Settings& operator=(Settings const&) = delete;

    static Settings* the()
    {
        static Settings instance;
        return &instance;
    }

    Optional<QPoint> last_position();
    void set_last_position(QPoint const& last_position);

    QSize last_size();
    void set_last_size(QSize const& last_size);

    bool is_maximized();
    void set_is_maximized(bool is_maximized);

    QString new_tab_page();
    void set_new_tab_page(QString const& page);

    struct EngineProvider {
        QString name;
        QString url;
    };

    EngineProvider search_engine();
    void set_search_engine(EngineProvider const& engine);

    EngineProvider autocomplete_engine();
    void set_autocomplete_engine(EngineProvider const& engine);

    bool enable_autocomplete();
    void set_enable_autocomplete(bool enable);

    bool enable_search();
    void set_enable_search(bool enable);

protected:
    Settings();

private:
    OwnPtr<QSettings> m_qsettings;
};

}
