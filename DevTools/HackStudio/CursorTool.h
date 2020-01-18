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

#include "Tool.h"
#include <AK/HashMap.h>
#include <LibDraw/Point.h>

class GWidget;

class CursorTool final : public Tool {
public:
    explicit CursorTool(FormEditorWidget& editor)
        : Tool(editor)
    {
    }
    virtual ~CursorTool() override {}

private:
    virtual const char* class_name() const override { return "CursorTool"; }
    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_keydown(GKeyEvent&) override;
    virtual void on_second_paint(GPainter&, GPaintEvent&) override;

    void set_rubber_band_position(const Point&);
    Rect rubber_band_rect() const;

    Point m_drag_origin;
    HashMap<GWidget*, Point> m_positions_before_drag;
    bool m_dragging { false };

    bool m_rubber_banding { false };
    Point m_rubber_band_origin;
    Point m_rubber_band_position;
};
