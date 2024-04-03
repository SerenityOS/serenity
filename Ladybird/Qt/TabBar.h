/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <QPushButton>

class QEvent;
class QIcon;
class QWidget;

namespace Ladybird {

class TabBarButton : public QPushButton {
    Q_OBJECT

public:
    explicit TabBarButton(QIcon const& icon, QWidget* parent = nullptr);

protected:
    bool event(QEvent* event);
};

}
