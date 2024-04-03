/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <QPushButton>
#include <QTabBar>
#include <QTabWidget>

class QEvent;
class QIcon;
class QWidget;

namespace Ladybird {

class TabBar : public QTabBar {
    Q_OBJECT

public:
    virtual QSize tabSizeHint(int index) const override;
};

class TabWidget : public QTabWidget {
    Q_OBJECT

public:
    explicit TabWidget(QWidget* parent = nullptr);
};

class TabBarButton : public QPushButton {
    Q_OBJECT

public:
    explicit TabBarButton(QIcon const& icon, QWidget* parent = nullptr);

protected:
    bool event(QEvent* event);
};

}
