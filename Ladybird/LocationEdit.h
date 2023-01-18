/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <QLineEdit>

class Tab;

class LocationEdit final : public QLineEdit {
    Q_OBJECT
public:
    explicit LocationEdit(Tab*);
};
