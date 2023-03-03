/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGUI/Event.h>
#include <LibGUI/Frame.h>

namespace VideoPlayer {

enum class VideoSizingMode : u8 {
    Fit,
    Fill,
    Stretch,
    FullSize,
    Sentinel
};

class VideoFrameWidget final : public GUI::Frame {
    C_OBJECT(VideoFrameWidget)
public:
    virtual ~VideoFrameWidget() override = default;

    void set_bitmap(Gfx::Bitmap const*);
    Gfx::Bitmap const* bitmap() const { return m_bitmap.ptr(); }

    void set_sizing_mode(VideoSizingMode value);
    VideoSizingMode sizing_mode() const { return m_sizing_mode; }

    void set_auto_resize(bool value);
    bool auto_resize() const { return m_auto_resize; }

    Function<void()> on_click;
    Function<void()> on_doubleclick;

protected:
    explicit VideoFrameWidget();

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
    VideoSizingMode m_sizing_mode { VideoSizingMode::Fit };
    bool m_auto_resize { false };
};

constexpr StringView video_sizing_mode_name(VideoSizingMode mode)
{
    switch (mode) {
    case VideoSizingMode::Fit:
        return "Fit"sv;
        break;
    case VideoSizingMode::Fill:
        return "Fill"sv;
        break;
    case VideoSizingMode::Stretch:
        return "Stretch"sv;
        break;
    case VideoSizingMode::FullSize:
        return "Full size"sv;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
