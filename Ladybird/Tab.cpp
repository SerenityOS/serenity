/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include "BrowserWindow.h"
#include "History.h"
#include <QCoreApplication>
#include <QStatusBar>

extern String s_serenity_resource_root;

Tab::Tab(QMainWindow* window)
    : m_window(window)
{
    m_layout = new QBoxLayout(QBoxLayout::Direction::TopToBottom, this);
    m_layout->setContentsMargins(0, 0, 0, 0);

    m_view = new WebView;
    m_toolbar = new QToolBar;
    m_location_edit = new QLineEdit;

    auto* focus_location_edit_action = new QAction("Edit Location");
    focus_location_edit_action->setShortcut(QKeySequence("Ctrl+L"));
    addAction(focus_location_edit_action);

    m_layout->addWidget(m_toolbar);
    m_layout->addWidget(m_view);

    auto back_icon_path = QString("%1/res/icons/16x16/go-back.png").arg(s_serenity_resource_root.characters());
    auto forward_icon_path = QString("%1/res/icons/16x16/go-forward.png").arg(s_serenity_resource_root.characters());
    auto home_icon_path = QString("%1/res/icons/16x16/go-home.png").arg(s_serenity_resource_root.characters());
    auto reload_icon_path = QString("%1/res/icons/16x16/reload.png").arg(s_serenity_resource_root.characters());
    m_back_action = make<QAction>(QIcon(back_icon_path), "Back");
    m_back_action->setShortcut(QKeySequence("Alt+Left"));
    m_forward_action = make<QAction>(QIcon(forward_icon_path), "Forward");
    m_forward_action->setShortcut(QKeySequence("Alt+Right"));
    m_home_action = make<QAction>(QIcon(home_icon_path), "Home");
    m_reload_action = make<QAction>(QIcon(reload_icon_path), "Reload");
    m_reload_action->setShortcut(QKeySequence("Ctrl+R"));

    m_toolbar->addAction(m_back_action);
    m_toolbar->addAction(m_forward_action);
    m_toolbar->addAction(m_reload_action);
    m_toolbar->addAction(m_home_action);
    m_toolbar->addWidget(m_location_edit);

    QObject::connect(m_view, &WebView::linkHovered, m_window->statusBar(), &QStatusBar::showMessage);
    QObject::connect(m_view, &WebView::linkUnhovered, m_window->statusBar(), &QStatusBar::clearMessage);

    QObject::connect(m_view, &WebView::loadStarted, [this](const URL& url) {
        m_location_edit->setText(url.to_string().characters());
        m_history.push(url, m_title.toUtf8().data());
    });
    QObject::connect(m_location_edit, &QLineEdit::returnPressed, this, &Tab::location_edit_return_pressed);
    QObject::connect(m_view, &WebView::title_changed, this, &Tab::page_title_changed);
    QObject::connect(m_view, &WebView::favicon_changed, this, &Tab::page_favicon_changed);

    QObject::connect(m_back_action, &QAction::triggered, this, &Tab::back);
    QObject::connect(m_forward_action, &QAction::triggered, this, &Tab::forward);
    QObject::connect(m_home_action, &QAction::triggered, this, &Tab::home);
    QObject::connect(m_reload_action, &QAction::triggered, this, &Tab::reload);
    QObject::connect(focus_location_edit_action, &QAction::triggered, m_location_edit, qOverload<>(&QWidget::setFocus));
    QObject::connect(focus_location_edit_action, &QAction::triggered, m_location_edit, &QLineEdit::selectAll);
}

void Tab::navigate(QString const& url)
{
    view().load(url.toUtf8().data());
}

void Tab::back()
{
    if (!m_history.can_go_back())
        return;

    m_history.go_back();
    view().load(m_history.current().url.to_string());
}

void Tab::forward()
{
    if (!m_history.can_go_forward())
        return;

    m_history.go_forward();
    view().load(m_history.current().url.to_string());
}

void Tab::home()
{
    navigate("https://www.serenityos.org/");
}

void Tab::reload()
{
    view().reload();
}

void Tab::location_edit_return_pressed()
{
    navigate(m_location_edit->text());
}

void Tab::page_title_changed(QString title)
{
    m_title = title;
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

void Tab::debug_request(String const& request, String const& argument)
{
    m_view->debug_request(request, argument);
}
