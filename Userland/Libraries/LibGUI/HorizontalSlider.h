/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Slider.h>

namespace GUI {

class HorizontalSlider final : public Slider {
    C_OBJECT(HorizontalSlider);

public:
    virtual ~HorizontalSlider() override = default;

private:
    HorizontalSlider()
        : Slider(Orientation::Horizontal)
    {
    }
};

}
