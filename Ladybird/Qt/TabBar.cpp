/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include <AK/StdLibExtras.h>
#include <AK/TypeCasts.h>
#include <Ladybird/Qt/TabBar.h>
#include <QContextMenuEvent>
#include <QEvent>
#include <QPushButton>

namespace Ladybird {

TabBar::TabBar(QWidget* parent)
    : QTabBar(parent)
{
}

QSize TabBar::tabSizeHint(int index) const
{
    auto width = this->width() / count();
    width = min(225, width);
    width = max(128, width);

    auto hint = QTabBar::tabSizeHint(index);
    hint.setWidth(width);
    return hint;
}

void TabBar::contextMenuEvent(QContextMenuEvent* event)
{
    auto* tab_widget = verify_cast<QTabWidget>(this->parent());
    auto* tab = verify_cast<Tab>(tab_widget->widget(tabAt(event->pos())));
    if (tab)
        tab->context_menu()->exec(event->globalPos());
}

TabWidget::TabWidget(QWidget* parent)
    : QTabWidget(parent)
{
    // This must be called first, otherwise several of the options below have no effect.
    setTabBar(new TabBar(this));

    setDocumentMode(true);
    setElideMode(Qt::TextElideMode::ElideRight);
    setMovable(true);
    setTabsClosable(true);

    installEventFilter(parent);
}

TabBarButton::TabBarButton(QIcon const& icon, QWidget* parent)
    : QPushButton(icon, {}, parent)
{
    resize({ 20, 20 });
    setFlat(true);
}

bool TabBarButton::event(QEvent* event)
{
    if (event->type() == QEvent::Enter)
        setFlat(false);
    if (event->type() == QEvent::Leave)
        setFlat(true);

    return QPushButton::event(event);
}

}
