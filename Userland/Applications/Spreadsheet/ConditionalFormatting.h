/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include <AK/ByteString.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGfx/Color.h>

namespace Spreadsheet {

struct Format {
    Optional<Color> foreground_color;
    Optional<Color> background_color;
};

struct ConditionalFormat : public Format {
    ByteString condition;
};

enum class FormatType {
    Background = 0,
    Foreground = 1
};

class ConditionView : public GUI::Widget {
    C_OBJECT(ConditionView)
public:
    virtual ~ConditionView() override;

private:
    ConditionView(ConditionalFormat&);

    ConditionalFormat& m_format;
};

class ConditionsView : public GUI::Widget {
    C_OBJECT(ConditionsView)
public:
    virtual ~ConditionsView() override;

    void set_formats(Vector<ConditionalFormat>*);

    void add_format();
    void remove_top();

private:
    ConditionsView();

    Vector<ConditionalFormat>* m_formats { nullptr };
    Vector<NonnullRefPtr<GUI::Widget>> m_widgets;
};

}
