/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LocationEdit.h"
#include "Tab.h"

LocationEdit::LocationEdit(Tab* tab)
    : QLineEdit(tab)
{
    connect(this, &QLineEdit::returnPressed, this, [&] {
        clearFocus();
    });
}
