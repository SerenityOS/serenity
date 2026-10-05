/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Shader.h>

namespace V3DGPU {

class Shader final : public GPU::Shader {
public:
    Shader(void const* ownership_token);
};

}
