/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Compiler/GenericASTPass.h"

namespace JSSpecCompiler {

// FunctionCallCanonicalizationPass simplifies ladders of BinaryOperators nodes in the function call
// arguments into nice and neat FunctionCall nodes.
//
// Ladders initially appear since I do not want to complicate expression parser, so it interprets
// `f(a, b, c, d)` as `f "function_call_operator" (a, (b, (c, d))))`.
class FunctionCallCanonicalizationPass : public GenericASTPass {
public:
    inline static constexpr StringView name = "function-call-canonicalization"sv;

    using GenericASTPass::GenericASTPass;

protected:
    void on_leave(Tree tree) override;
};

}
