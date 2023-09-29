/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RecursionDecision.h>

#include "Forward.h"

namespace JSSpecCompiler {

class CompilerPass {
public:
    CompilerPass(TranslationUnitRef translation_unit)
        : m_translation_unit(translation_unit)
    {
    }

    virtual ~CompilerPass() = default;

    virtual void run() = 0;

protected:
    TranslationUnitRef m_translation_unit;
};

class IntraproceduralCompilerPass : public CompilerPass {
public:
    IntraproceduralCompilerPass(TranslationUnitRef translation_unit)
        : CompilerPass(translation_unit)
    {
    }

    void run() override final;

protected:
    virtual void process_function() = 0;

    FunctionDefinitionRef m_function;
};

}
