/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace GUI {

class ImageWidget : public Frame {
    C_OBJECT(ImageWidget)
public:
    virtual ~ImageWidget() override = default;

    void set_bitmap(Gfx::Bitmap const*);
    Gfx::Bitmap const* bitmap() const { return m_bitmap.ptr(); }

    void set_should_stretch(bool value) { m_should_stretch = value; }
    bool should_stretch() const { return m_should_stretch; }

    void set_auto_resize(bool value);
    bool auto_resize() const { return m_auto_resize; }

    void animate();
    void load_from_file(StringView);

    int opacity_percent() const { return m_opacity_percent; }
    void set_opacity_percent(int percent);

    Function<void()> on_click;

protected:
    explicit ImageWidget(StringView text = {});

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void paint_event(PaintEvent&) override;

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
    bool m_should_stretch { false };
    bool m_auto_resize { false };

    RefPtr<Gfx::ImageDecoder> m_image_decoder;
    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    NonnullRefPtr<Core::Timer> m_timer;

    int m_opacity_percent { 100 };
};

}
