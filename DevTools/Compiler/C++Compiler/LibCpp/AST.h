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

#pragma once

#include <LibMiddleEnd/SIR.h>

namespace Cpp {

using VoidType = SIR::VoidType;
using Type = SIR::Type;
using ASTNode = SIR::ASTNode;
using Variable = SIR::Variable;

class IntegerType : public SIR::IntegerType {
public:
    enum Kind {
        SignedInt,
    };
    explicit IntegerType(Kind kind, size_t size_in_bits, size_t size_in_bytes, bool is_signed)
        : SIR::IntegerType(size_in_bits, size_in_bytes, is_signed)
        , m_integer_kind(kind)
    {
    }

private:
    Kind m_integer_kind;
};

class SignedIntType : public IntegerType {
public:
    SignedIntType()
        : IntegerType(Kind::SignedInt, 32, 4, true)
    {
    }
};

class Function : public SIR::Function {
public:
    Function(NonnullRefPtr<SIR::Type>& return_type, String& unmangled_name, NonnullRefPtrVector<SIR::Variable>& parameters)
        : SIR::Function(return_type, unmangled_name, parameters)
        , m_unmangled_name(unmangled_name)
    {
        set_name(mangle());
    }

    String mangle();

private:
    String m_unmangled_name;
};

class TranslationUnit {
public:
    NonnullRefPtrVector<Function>& functions() { return m_functions; }

private:
    NonnullRefPtrVector<Function> m_functions;
};
}
