/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Forward.h"
#include <AK/String.h>
#include <LibGUI/ScrollableWidget.h>
#include <LibGfx/Color.h>

namespace Spreadsheet {

struct Format {
    Optional<Color> foreground_color;
    Optional<Color> background_color;
};

struct ConditionalFormat : public Format {
    String condition;
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
    NonnullRefPtrVector<GUI::Widget> m_widgets;
};

}
