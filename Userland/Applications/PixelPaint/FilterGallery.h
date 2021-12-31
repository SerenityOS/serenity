/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ImageEditor.h"
#include <LibGUI/Dialog.h>

namespace PixelPaint {

class FilterGallery final : public GUI::Dialog {
    C_OBJECT(FilterGallery);

private:
    FilterGallery(GUI::Window* parent_window, ImageEditor*);
};

}
