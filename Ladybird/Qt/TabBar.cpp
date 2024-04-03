/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <Ladybird/Qt/TabBar.h>
#include <QEvent>
#include <QPushButton>

namespace Ladybird {

QSize TabBar::tabSizeHint(int index) const
{
    auto width = this->width() / count();
    width = min(225, width);
    width = max(64, width);

    auto hint = QTabBar::tabSizeHint(index);
    hint.setWidth(width);
    return hint;
}

TabWidget::TabWidget(QWidget* parent)
    : QTabWidget(parent)
{
    // This must be called first, otherwise several of the options below have no effect.
    setTabBar(new TabBar());

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
