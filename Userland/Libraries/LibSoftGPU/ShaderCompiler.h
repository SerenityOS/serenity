/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <LibGPU/IR.h>
#include <LibSoftGPU/Shader.h>

namespace SoftGPU {

class ShaderCompiler final {
public:
    ErrorOr<NonnullRefPtr<Shader>> compile(void const* ownership_token, GPU::IR::Shader const&);
};

}
