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

#include <LibGUI/Frame.h>

class QSLabel;

class QSWidget final : public GUI::Frame {
    C_OBJECT(QSWidget)
public:
    virtual ~QSWidget() override;

    void set_bitmap(NonnullRefPtr<Gfx::Bitmap>);
    const Gfx::Bitmap* bitmap() const { return m_bitmap.ptr(); }

    void set_path(const String&);
    const String& path() const { return m_path; }

    Function<void(int)> on_scale_change;

private:
    explicit QSWidget(GUI::Widget* parent = nullptr);
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void relayout();

    RefPtr<Gfx::Bitmap> m_bitmap;
    Gfx::Rect m_bitmap_rect;
    int m_scale { 100 };
    Gfx::Point m_pan_origin;
    Gfx::Point m_pan_bitmap_origin;
    String m_path;
};
