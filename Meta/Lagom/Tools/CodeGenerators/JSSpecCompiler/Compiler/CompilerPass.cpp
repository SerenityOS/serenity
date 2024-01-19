/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compiler/CompilerPass.h"
#include "Function.h"

namespace JSSpecCompiler {

void IntraproceduralCompilerPass::run()
{
    for (auto const& function : m_translation_unit->functions_to_compile()) {
        m_function = function;
        process_function();
    }
}

}
