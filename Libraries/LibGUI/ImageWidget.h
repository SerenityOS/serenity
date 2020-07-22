/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>

namespace GUI {

class ImageWidget : public Frame {
    C_OBJECT(ImageWidget)
public:
    virtual ~ImageWidget() override;

    void set_bitmap(const Gfx::Bitmap*);
    Gfx::Bitmap* bitmap() { return m_bitmap.ptr(); }

    void set_should_stretch(bool value) { m_should_stretch = value; }
    bool should_stretch() const { return m_should_stretch; }

    void set_auto_resize(bool value);
    bool auto_resize() const { return m_auto_resize; }

    void animate();
    void load_from_file(const StringView&);

    Function<void()> on_click;

protected:
    explicit ImageWidget(const StringView& text = {});

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
    bool m_should_stretch { false };
    bool m_auto_resize { false };

    RefPtr<Gfx::ImageDecoder> m_image_decoder;
    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    NonnullRefPtr<Core::Timer> m_timer;
};

}
