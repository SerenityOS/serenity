/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include "BrowserWindow.h"
#include <QCoreApplication>
#include <QStatusBar>
#include <utility>

extern String s_serenity_resource_root;

Tab::Tab(QMainWindow* window)
    : m_window(window)
{
    m_layout = new QBoxLayout(QBoxLayout::Direction::TopToBottom, this);
    m_layout->setContentsMargins(0, 0, 0, 0);

    m_view = new WebView;
    m_toolbar = new QToolBar;
    m_location_edit = new QLineEdit;

    m_layout->addWidget(m_toolbar);
    m_layout->addWidget(m_view);

    auto reload_icon_path = QString("%1/res/icons/16x16/reload.png").arg(s_serenity_resource_root.characters());
    auto* reload_action = new QAction(QIcon(reload_icon_path), "Reload");
    reload_action->setShortcut(QKeySequence("Ctrl+R"));
    m_toolbar->addAction(reload_action);
    m_toolbar->addWidget(m_location_edit);

    QObject::connect(m_view, &WebView::linkHovered, m_window->statusBar(), &QStatusBar::showMessage);
    QObject::connect(m_view, &WebView::linkUnhovered, m_window->statusBar(), &QStatusBar::clearMessage);

    QObject::connect(m_view, &WebView::loadStarted, m_location_edit, &QLineEdit::setText);
    QObject::connect(m_location_edit, &QLineEdit::returnPressed, this, &Tab::location_edit_return_pressed);
    QObject::connect(m_view, &WebView::title_changed, this, &Tab::page_title_changed);
    QObject::connect(m_view, &WebView::favicon_changed, this, &Tab::page_favicon_changed);

    QObject::connect(reload_action, &QAction::triggered, this, &Tab::reload);
}

void Tab::reload()
{
    view().reload();
}

void Tab::location_edit_return_pressed()
{
    view().load(m_location_edit->text().toUtf8().data());
}

void Tab::page_title_changed(QString title)
{
    emit title_changed(tab_index(), std::move(title));
}

void Tab::page_favicon_changed(QIcon icon)
{
    emit favicon_changed(tab_index(), std::move(icon));
}

int Tab::tab_index()
{
    // FIXME: I hear you like footguns...
    //        There has to be a better way of doing this
    auto browser_window = reinterpret_cast<BrowserWindow*>(m_window);
    return browser_window->tab_index(this);
}
