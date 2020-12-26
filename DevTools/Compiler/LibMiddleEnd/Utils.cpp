/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Utils.h"

namespace MiddleEnd::Utils {
NonnullRefPtr<SIR::Variable> create_store(NonnullRefPtr<SIR::Type>& type, String& name)
{
    return create_ast_node<SIR::Variable>(type, name);
}

NonnullRefPtr<SIR::BinaryExpression> create_binary_operation(NonnullRefPtr<SIR::Variable>& left, NonnullRefPtr<SIR::Variable>& right, SIR::BinaryExpression::Kind operation)
{
    assert(left->node_type()->kind() == right->node_type()->kind());
    assert(left->node_type()->size_in_bytes() == right->node_type()->size_in_bytes());
    assert(left->node_type()->size_in_bits() == right->node_type()->size_in_bits());
    auto result = RefPtr(create_store(left->node_type(), left->name()));

    return create_ast_node<SIR::BinaryExpression>(operation, reinterpret_cast<NonnullRefPtr<SIR::ASTNode>&>(left), reinterpret_cast<NonnullRefPtr<SIR::ASTNode>&>(right), reinterpret_cast<RefPtr<SIR::Variable>&>(result));
}

}