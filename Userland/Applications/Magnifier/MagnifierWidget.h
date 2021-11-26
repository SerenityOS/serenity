/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

class MagnifierWidget final : public GUI::Frame {
    C_OBJECT(MagnifierWidget);

public:
    virtual ~MagnifierWidget();
    void set_scale_factor(int scale_factor);
    void pause_capture(bool pause) { m_pause_capture = pause; }

private:
    MagnifierWidget();

    virtual void paint_event(GUI::PaintEvent&) override;

    void sync();

    int m_scale_factor { 2 };
    RefPtr<Gfx::Bitmap> m_grabbed_bitmap;
    bool m_pause_capture { false };
};
