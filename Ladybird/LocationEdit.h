/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <QLineEdit>

namespace Ladybird {

class LocationEdit final : public QLineEdit {
    Q_OBJECT
public:
    explicit LocationEdit(QWidget*);

private:
    virtual void focusInEvent(QFocusEvent* event) override;
    virtual void focusOutEvent(QFocusEvent* event) override;

    void highlight_location();
};

}
