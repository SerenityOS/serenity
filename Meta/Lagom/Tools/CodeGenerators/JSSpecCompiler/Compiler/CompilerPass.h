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
    CompilerPass(FunctionRef function)
        : m_function(function)
    {
    }

    virtual ~CompilerPass() = default;

    virtual void run() = 0;

protected:
    FunctionRef m_function;
};

}
