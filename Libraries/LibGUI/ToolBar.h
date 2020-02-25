/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Widget.h>

namespace GUI {

class ToolBar : public Widget {
    C_OBJECT(ToolBar)
public:
    virtual ~ToolBar() override;

    void add_action(Action&);
    void add_separator();

    bool has_frame() const { return m_has_frame; }
    void set_has_frame(bool has_frame) { m_has_frame = has_frame; }

protected:
    explicit ToolBar(Gfx::Orientation = Gfx::Orientation::Horizontal, int button_size = 16);

    virtual void paint_event(PaintEvent&) override;

private:
    struct Item {
        enum class Type {
            Invalid,
            Separator,
            Action
        };
        Type type { Type::Invalid };
        RefPtr<Action> action;
    };
    NonnullOwnPtrVector<Item> m_items;
    int m_button_size { 16 };
    bool m_has_frame { true };
};

}
