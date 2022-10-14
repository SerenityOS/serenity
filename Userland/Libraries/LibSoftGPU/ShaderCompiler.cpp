/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/ShaderCompiler.h>

namespace SoftGPU {

ErrorOr<NonnullRefPtr<Shader>> ShaderCompiler::compile(void const* ownership_token, GPU::IR::Shader const&)
{
    // FIXME: implement the shader compiler
    return adopt_ref(*new Shader(ownership_token, {}));
}

}
