/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/Label.h>

class WidgetWithLabel {
public:
    WidgetWithLabel(RefPtr<GUI::Label> value_label)
        : m_value_label(move(value_label))
    {
    }
    RefPtr<GUI::Label> value_label() { return m_value_label; }

protected:
    RefPtr<GUI::Label> m_value_label;
};
