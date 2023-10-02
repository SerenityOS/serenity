/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Compiler/GenericASTPass.h"

namespace JSSpecCompiler {

// ReferenceResolvingPass collects all variable names declared in the function and replaces
// UnresolvedReference nodes with either SlotName, Variable, or FunctionPointer nodes.
class ReferenceResolvingPass : public GenericASTPass {
public:
    using GenericASTPass::GenericASTPass;

protected:
    RecursionDecision on_entry(Tree tree) override;

    void on_leave(Tree tree) override;
};

}
