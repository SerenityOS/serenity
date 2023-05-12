/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::Layout {

class ImageProvider {
public:
    virtual ~ImageProvider() { }

    virtual RefPtr<Gfx::Bitmap const> current_image_bitmap() const = 0;
    virtual void set_visible_in_viewport(bool) = 0;
};

}
