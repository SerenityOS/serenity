/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>

#include "Widget.h"

ErrorOr<NonnullRefPtr<Demo>> Demo::create()
{
    auto demo = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Demo));

    demo->m_bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, { DRAWTARGET_WIDTH, DRAWTARGET_HEIGHT }));
    demo->m_bitmap->fill(Gfx::Color::Black);

    return demo;
}

Demo::Demo()
{
    m_cycles = 0;

    stop_timer();
    start_timer(16);
}

Demo::~Demo() { }

void Demo::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_scaled_bitmap(rect(), *m_bitmap, m_bitmap->rect());
}

void Demo::timer_event(Core::TimerEvent&)
{
    m_cycles += 1;
    update_frame(m_bitmap, m_cycles);
    update();
}
