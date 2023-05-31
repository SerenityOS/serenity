/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SettingsDialog.h"
#include "Settings.h"
#include "StringUtils.h"
#include <AK/URL.h>
#include <QCloseEvent>
#include <QLabel>
#include <QMenu>

namespace Ladybird {

SettingsDialog::SettingsDialog(QMainWindow* window)
    : m_window(window)
{
    m_layout = new QFormLayout(this);
    m_ok_button = new QPushButton("&Save", this);

    m_enable_search = make<QCheckBox>(this);
    m_enable_search->setChecked(Settings::the()->enable_search());

    m_search_engine_dropdown = make<QToolButton>(this);
    m_search_engine_dropdown->setText(Settings::the()->search_engine().name);

    m_enable_autocomplete = make<QCheckBox>(this);
    m_enable_autocomplete->setChecked(Settings::the()->enable_autocomplete());

    m_autocomplete_engine_dropdown = make<QToolButton>(this);
    m_autocomplete_engine_dropdown->setText(Settings::the()->autocomplete_engine().name);

    m_new_tab_page = make<QLineEdit>(this);
    m_new_tab_page->setText(Settings::the()->new_tab_page());

    setup_search_engines();

    m_layout->addRow(new QLabel("Page on New Tab", this), m_new_tab_page);
    m_layout->addRow(new QLabel("Enable Search", this), m_enable_search);
    m_layout->addRow(new QLabel("Search Engine", this), m_search_engine_dropdown);
    m_layout->addRow(new QLabel("Enable Autocomplete", this), m_enable_autocomplete);
    m_layout->addRow(new QLabel("Autocomplete Engine", this), m_autocomplete_engine_dropdown);
    m_layout->addRow(m_ok_button);

    QObject::connect(m_ok_button, &QPushButton::released, this, [this] {
        close();
    });

    setWindowTitle("Settings");
    setFixedWidth(300);
    setFixedHeight(170);
    setLayout(m_layout);
    show();
    setFocus();
}

void SettingsDialog::setup_search_engines()
{
    // FIXME: These should be in a config file.
    Vector<Settings::EngineProvider> search_engines = {
        { "Bing", "https://www.bing.com/search?q={}" },
        { "Brave", "https://search.brave.com/search?q={}" },
        { "DuckDuckGo", "https://duckduckgo.com/?q={}" },
        { "GitHub", "https://github.com/search?q={}" },
        { "Google", "https://google.com/search?q={}" },
        { "Mojeek", "https://www.mojeek.com/search?q={}" },
        { "Yahoo", "https://search.yahoo.com/search?p={}" },
        { "Yandex", "https://yandex.com/search/?text={}" },
    };

    Vector<Settings::EngineProvider> autocomplete_engines = {
        { "DuckDuckGo", "https://duckduckgo.com/ac/?q={}" },
        { "Google", "https://www.google.com/complete/search?client=chrome&q={}" },
        { "Yahoo", "https://search.yahoo.com/sugg/gossip/gossip-us-ura/?output=sd1&command={}" },
    };

    QMenu* search_engine_menu = new QMenu(this);
    for (auto& search_engine : search_engines) {
        QAction* action = new QAction(search_engine.name, this);
        connect(action, &QAction::triggered, this, [&, search_engine] {
            Settings::the()->set_search_engine(search_engine);
            m_search_engine_dropdown->setText(search_engine.name);
        });
        search_engine_menu->addAction(action);
    }
    m_search_engine_dropdown->setMenu(search_engine_menu);
    m_search_engine_dropdown->setPopupMode(QToolButton::InstantPopup);
    m_search_engine_dropdown->setEnabled(Settings::the()->enable_search());

    QMenu* autocomplete_engine_menu = new QMenu(this);
    for (auto& autocomplete_engine : autocomplete_engines) {
        QAction* action = new QAction(autocomplete_engine.name, this);
        connect(action, &QAction::triggered, this, [&, autocomplete_engine] {
            Settings::the()->set_autocomplete_engine(autocomplete_engine);
            m_autocomplete_engine_dropdown->setText(autocomplete_engine.name);
        });
        autocomplete_engine_menu->addAction(action);
    }
    m_autocomplete_engine_dropdown->setMenu(autocomplete_engine_menu);
    m_autocomplete_engine_dropdown->setPopupMode(QToolButton::InstantPopup);
    m_autocomplete_engine_dropdown->setEnabled(Settings::the()->enable_autocomplete());

    connect(m_enable_search, &QCheckBox::stateChanged, this, [&](int state) {
        Settings::the()->set_enable_search(state == Qt::Checked);
        m_search_engine_dropdown->setEnabled(state == Qt::Checked);
    });

    connect(m_enable_autocomplete, &QCheckBox::stateChanged, this, [&](int state) {
        Settings::the()->set_enable_autocomplete(state == Qt::Checked);
        m_autocomplete_engine_dropdown->setEnabled(state == Qt::Checked);
    });
}

void SettingsDialog::closeEvent(QCloseEvent* event)
{
    save();
    event->accept();
}

void SettingsDialog::save()
{
    auto url_string = MUST(ak_string_from_qstring(m_new_tab_page->text()));
    if (!URL(url_string).is_valid())
        return;
    Settings::the()->set_new_tab_page(m_new_tab_page->text());
}

}
