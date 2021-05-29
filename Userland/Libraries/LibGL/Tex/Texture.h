/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>

namespace GL {

class Texture : public RefCounted<Texture> {
public:
    virtual ~Texture() { }

    virtual bool is_texture_1d() const { return false; }
    virtual bool is_texture_2d() const { return false; }
    virtual bool is_texture_3d() const { return false; }
    virtual bool is_cube_map() const { return false; }
};

}
