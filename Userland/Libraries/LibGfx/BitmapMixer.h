/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Bitmap.h"

namespace Gfx {

class BitmapMixer {
public:
    enum class MixingMethod {
        Add,
        Lightest,
    };

    BitmapMixer(Bitmap& bitmap)
        : m_bitmap(bitmap) {};

    void mix_with(Bitmap&, MixingMethod);

private:
    Bitmap& m_bitmap;
};

}
