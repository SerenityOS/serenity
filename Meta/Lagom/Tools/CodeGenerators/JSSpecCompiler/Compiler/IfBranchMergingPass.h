/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Compiler/GenericASTPass.h"

namespace JSSpecCompiler {

// IfBranchMergingPass, unsurprisingly, merges if-elseif-else chains, represented as a separate
// nodes after parsing, into one IfElseIfChain node. It also deals with the following nonsense from
// the spec:
// ```
//   1. If <condition>, then
//      ...
//   2. Else,
//      a. If <condition>, then
//         ...
//   3. Else,
//      ...
// ```
class IfBranchMergingPass : public GenericASTPass {
public:
    using GenericASTPass::GenericASTPass;

protected:
    RecursionDecision on_entry(Tree tree) override;

private:
    static Tree merge_branches(Vector<Tree> const& unmerged_branches);
};

}
