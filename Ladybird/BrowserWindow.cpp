/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "WebView.h"
#include <LibCore/EventLoop.h>
#include <QAction>
#include <QStatusBar>

BrowserWindow::BrowserWindow(Core::EventLoop& event_loop)
    : m_event_loop(event_loop)
{
    m_tabs_container = new QTabWidget;
    m_tabs_container->setElideMode(Qt::TextElideMode::ElideRight);
    m_tabs_container->setMovable(true);
    m_tabs_container->setTabsClosable(true);

    auto menu = menuBar()->addMenu("File");
    auto new_tab_action = menu->addAction("New Tab", QKeySequence(Qt::CTRL | Qt::Key_T));
    auto quit_action = menu->addAction("Quit", QKeySequence(Qt::CTRL | Qt::Key_Q));

    QObject::connect(new_tab_action, &QAction::triggered, this, &BrowserWindow::new_tab);
    QObject::connect(quit_action, &QAction::triggered, this, &QMainWindow::close);
    QObject::connect(m_tabs_container, &QTabWidget::currentChanged, [this](int index) {
        setWindowTitle(m_tabs_container->tabText(index));
        setWindowIcon(m_tabs_container->tabIcon(index));
    });
    QObject::connect(m_tabs_container, &QTabWidget::tabCloseRequested, this, &BrowserWindow::close_tab);

    new_tab();

    setCentralWidget(m_tabs_container);
}

void BrowserWindow::new_tab()
{
    auto tab = make<Tab>(this);
    auto tab_ptr = tab.ptr();
    m_tabs.append(std::move(tab));

    if (m_current_tab == nullptr) {
        m_current_tab = tab_ptr;
    }

    m_tabs_container->addTab(tab_ptr, "New Tab");
    m_tabs_container->setCurrentWidget(tab_ptr);

    QObject::connect(tab_ptr, &Tab::title_changed, this, &BrowserWindow::tab_title_changed);
    QObject::connect(tab_ptr, &Tab::favicon_changed, this, &BrowserWindow::tab_favicon_changed);
}

void BrowserWindow::close_tab(int index)
{
    auto* tab = m_tabs_container->widget(index);
    m_tabs_container->removeTab(index);
    m_tabs.remove_first_matching([&](auto& entry) {
        return entry == tab;
    });
}

int BrowserWindow::tab_index(Tab* tab)
{
    return m_tabs_container->indexOf(tab);
}

void BrowserWindow::tab_title_changed(int index, QString const& title)
{
    if (title.isEmpty()) {
        m_tabs_container->setTabText(index, "...");
        setWindowTitle("Ladybird");
    } else {
        m_tabs_container->setTabText(index, title);
        setWindowTitle(QString("%1 - Ladybird").arg(title));
    }
}

void BrowserWindow::tab_favicon_changed(int index, QIcon icon)
{
    m_tabs_container->setTabIcon(index, icon);
    setWindowIcon(icon);
}

void BrowserWindow::closeEvent(QCloseEvent* event)
{
    QWidget::closeEvent(event);

    // FIXME: Ladybird only supports one window at the moment. When we support
    //        multiple windows, we'll only want to fire off the quit event when
    //        all of the browser windows have closed.
    m_event_loop.quit(0);
}
