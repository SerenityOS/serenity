/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Shader.h>

namespace VirtGPU {

class Shader final : public GPU::Shader {
public:
    Shader(void const* ownership_token);
};

}
