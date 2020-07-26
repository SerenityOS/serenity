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
#include <LibGfx/Bitmap.h>
#include <LibGfx/Point.h>

class QSLabel;

class QSWidget final : public GUI::Frame {
    C_OBJECT(QSWidget)
public:
    enum Directions {
        First,
        Back,
        Forward,
        Last
    };

    virtual ~QSWidget() override;

    const Gfx::Bitmap* bitmap() const { return m_bitmap.ptr(); }
    const String& path() const { return m_path; }
    void set_scale(int);
    int scale() { return m_scale; }
    void set_toolbar_height(int height) { m_toolbar_height = height; }
    int toolbar_height() { return m_toolbar_height; }

    void clear();
    void flip(Gfx::Orientation);
    void rotate(Gfx::RotationDirection);
    void navigate(Directions);
    void load_from_file(const String&);

    Function<void(int, Gfx::IntRect)> on_scale_change;
    Function<void()> on_doubleclick;
    Function<void(const GUI::DropEvent&)> on_drop;

private:
    QSWidget();
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    void relayout();
    void resize_window();

    String m_path;
    RefPtr<Gfx::Bitmap> m_bitmap;
    int m_toolbar_height { 28 };

    Gfx::IntRect m_bitmap_rect;
    int m_scale { -1 };
    Gfx::FloatPoint m_pan_origin;

    Gfx::IntPoint m_click_position;
    Gfx::FloatPoint m_saved_pan_origin;
    Vector<String> m_files_in_same_dir;
};
