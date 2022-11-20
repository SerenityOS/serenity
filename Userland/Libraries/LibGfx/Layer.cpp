/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Layer.h"
#include <AK/Function.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

Layer::Layer(NonnullRefPtr<Bitmap> target, Gfx::Color color)
    : m_target(move(target))
    , m_color(color)
{
}

void Layer::add_point(Gfx::IntPoint const& position, Gfx::Color color)
{
    VERIFY(color == m_color);

    m_points.set(position);
}

void Layer::draw()
{
    for (auto const& point : m_points) {
        auto& pixel = m_target->scanline(point.y())[point.x()];
        pixel = Color::from_argb(pixel).blend(m_color).value();
    }
}

Layer::~Layer()
{
    draw();
}

}
