/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/StringView.h>

#include "Forward.h"

namespace JSSpecCompiler {

class CompilationStep {
public:
    CompilationStep(StringView name)
        : m_name(name)
    {
    }

    virtual ~CompilationStep() = default;
    virtual void run(TranslationUnitRef translation_unit) = 0;

    StringView name() const { return m_name; }

private:
    StringView m_name;
};

class NonOwningCompilationStep : public CompilationStep {
public:
    template<typename Func>
    NonOwningCompilationStep(StringView name, Func&& func)
        : CompilationStep(name)
        , m_func(func)
    {
    }

    void run(TranslationUnitRef translation_unit) override { m_func(translation_unit); }

private:
    AK::Function<void(TranslationUnitRef)> m_func;
};

}
