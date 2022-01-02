/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Widget.h>

namespace PixelPaint {

class Filter {
public:
    virtual void apply() const = 0;
    virtual RefPtr<GUI::Widget> get_settings_widget();

    virtual StringView filter_name() = 0;

    virtual ~Filter() {};

    Filter(ImageEditor* editor)
        : m_editor(editor) {};

protected:
    ImageEditor* m_editor { nullptr };
    StringView m_filter_name;
    RefPtr<GUI::Widget> m_settings_widget { nullptr };
};

}
