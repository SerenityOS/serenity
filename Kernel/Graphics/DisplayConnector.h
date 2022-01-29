/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

class DisplayConnector {
public:
    struct Resolution {
        size_t width;
        size_t height;
        size_t bpp;
        Optional<size_t> refresh_rate;
    };

public:
    virtual ~DisplayConnector() = default;

    virtual bool modesetting_capable() const = 0;
    virtual bool double_framebuffering_capable() const = 0;
    virtual ErrorOr<ByteBuffer> get_edid() const = 0;
    virtual ErrorOr<void> set_resolution(Resolution const&) = 0;
    virtual ErrorOr<void> set_safe_resolution() = 0;
    virtual ErrorOr<Resolution> get_resolution() = 0;
    virtual ErrorOr<void> set_y_offset(size_t y) = 0;
    virtual ErrorOr<void> unblank() = 0;

protected:
    DisplayConnector() = default;
};
}
