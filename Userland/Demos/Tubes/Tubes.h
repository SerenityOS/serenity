/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Vector.h>
#include <LibDesktop/Screensaver.h>
#include <LibGL/GLContext.h>
#include <LibGfx/Vector3.h>

enum class Direction : u8 {
    None = 0,
    XPositive = 1,
    XNegative = 2,
    YPositive = 3,
    YNegative = 4,
    ZPositive = 5,
    ZNegative = 6,
};

struct Tube {
    bool active { true };
    DoubleVector3 color;
    IntVector3 position;
    Direction direction { Direction::None };
    IntVector3 target_position { 0, 0, 0 };
    double progress_to_target { 0 };
};

class Tubes final : public Desktop::Screensaver {
    C_OBJECT(Tubes)
public:
    virtual ~Tubes() override = default;

    ErrorOr<void> create_buffer(Gfx::IntSize);
    void reset_tubes();
    void setup_view();
    void update_tubes();

private:
    Tubes(int);

    void choose_new_direction_for_tube(Tube&);
    u8 get_grid(IntVector3);
    bool is_valid_grid_position(IntVector3);
    void set_grid(IntVector3, u8 value);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    RefPtr<Gfx::Bitmap> m_bitmap;
    FixedArray<u8> m_grid;
    OwnPtr<GL::GLContext> m_gl_context;
    u64 m_ticks { 0 };
    Vector<Tube> m_tubes;
};
