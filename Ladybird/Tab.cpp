/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include "BrowserWindow.h"
#include "History.h"
#include "Settings.h"
#include <QCoreApplication>
#include <QFont>
#include <QFontMetrics>
#include <QPoint>
#include <QResizeEvent>

extern String s_serenity_resource_root;
extern Browser::Settings* s_settings;

Tab::Tab(BrowserWindow* window)
    : m_window(window)
{
    m_layout = new QBoxLayout(QBoxLayout::Direction::TopToBottom, this);
    m_layout->setSpacing(0);
    m_layout->setContentsMargins(0, 0, 0, 0);

    m_view = new WebView;
    m_toolbar = new QToolBar;
    m_location_edit = new QLineEdit;

    m_hover_label = new QLabel(this);
    m_hover_label->hide();
    m_hover_label->setFrameShape(QFrame::Shape::Box);
    m_hover_label->setAutoFillBackground(true);

    auto* focus_location_editor_action = new QAction("Edit Location");
    focus_location_editor_action->setShortcut(QKeySequence("Ctrl+L"));
    addAction(focus_location_editor_action);

    m_layout->addWidget(m_toolbar);
    m_layout->addWidget(m_view);

    auto back_icon_path = QString("%1/res/icons/16x16/go-back.png").arg(s_serenity_resource_root.characters());
    auto forward_icon_path = QString("%1/res/icons/16x16/go-forward.png").arg(s_serenity_resource_root.characters());
    auto home_icon_path = QString("%1/res/icons/16x16/go-home.png").arg(s_serenity_resource_root.characters());
    auto reload_icon_path = QString("%1/res/icons/16x16/reload.png").arg(s_serenity_resource_root.characters());
    m_back_action = make<QAction>(QIcon(back_icon_path), "Back");
    m_back_action->setEnabled(false);
    m_back_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Back));
    m_forward_action = make<QAction>(QIcon(forward_icon_path), "Forward");
    m_forward_action->setEnabled(false);
    m_forward_action->setShortcuts(QKeySequence::keyBindings(QKeySequence::StandardKey::Forward));
    m_home_action = make<QAction>(QIcon(home_icon_path), "Home");
    m_reload_action = make<QAction>(QIcon(reload_icon_path), "Reload");
    m_reload_action->setShortcut(QKeySequence("Ctrl+R"));

    m_toolbar->addAction(m_back_action);
    m_toolbar->addAction(m_forward_action);
    m_toolbar->addAction(m_reload_action);
    m_toolbar->addAction(m_home_action);
    m_toolbar->addWidget(m_location_edit);

    QObject::connect(m_view, &WebView::link_hovered, [this](QString const& title) {
        m_hover_label->setText(title);
        update_hover_label();
        m_hover_label->show();
    });
    QObject::connect(m_view, &WebView::link_unhovered, [this] {
        m_hover_label->hide();
    });

    QObject::connect(m_view, &WebView::load_started, [this](const URL& url) {
        m_location_edit->setText(url.to_string().characters());
        m_history.push(url, m_title.toUtf8().data());
        m_back_action->setEnabled(m_history.can_go_back());
        m_forward_action->setEnabled(m_history.can_go_forward());
    });
    QObject::connect(m_location_edit, &QLineEdit::returnPressed, this, &Tab::location_edit_return_pressed);
    QObject::connect(m_view, &WebView::title_changed, this, &Tab::page_title_changed);
    QObject::connect(m_view, &WebView::favicon_changed, this, &Tab::page_favicon_changed);

    QObject::connect(m_back_action, &QAction::triggered, this, &Tab::back);
    QObject::connect(m_forward_action, &QAction::triggered, this, &Tab::forward);
    QObject::connect(m_home_action, &QAction::triggered, this, &Tab::home);
    QObject::connect(m_reload_action, &QAction::triggered, this, &Tab::reload);
    QObject::connect(focus_location_editor_action, &QAction::triggered, this, &Tab::focus_location_editor);
}

void Tab::focus_location_editor()
{
    m_location_edit->setFocus();
    m_location_edit->selectAll();
}

void Tab::navigate(QString url)
{
    if (!url.startsWith("http://", Qt::CaseInsensitive) && !url.startsWith("https://", Qt::CaseInsensitive) && !url.startsWith("file://", Qt::CaseInsensitive))
        url = "http://" + url;
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
    navigate(s_settings->homepage());
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
    return m_window->tab_index(this);
}

void Tab::debug_request(String const& request, String const& argument)
{
    m_view->debug_request(request, argument);
}

void Tab::resizeEvent(QResizeEvent* event)
{
    QWidget::resizeEvent(event);
    if (m_hover_label->isVisible())
        update_hover_label();
}

void Tab::update_hover_label()
{
    m_hover_label->resize(QFontMetrics(m_hover_label->font()).boundingRect(m_hover_label->text()).adjusted(-4, -2, 4, 2).size());
    m_hover_label->move(6, height() - m_hover_label->height() - 8);
    m_hover_label->raise();
}
