/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

namespace SQL {

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt(*new T(forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() { }

protected:
    ASTNode() = default;
};

class Statement : public ASTNode {
};

class ErrorStatement final : public Statement {
};

class SignedNumber final : public ASTNode {
public:
    explicit SignedNumber(double value)
        : m_value(value)
    {
    }

    double value() const { return m_value; }

private:
    double m_value;
};

class TypeName : public ASTNode {
public:
    TypeName(String name, NonnullRefPtrVector<SignedNumber> signed_numbers)
        : m_name(move(name))
        , m_signed_numbers(move(signed_numbers))
    {
        VERIFY(m_signed_numbers.size() <= 2);
    }

    const String& name() const { return m_name; }
    const NonnullRefPtrVector<SignedNumber> signed_numbers() const { return m_signed_numbers; }

private:
    String m_name;
    NonnullRefPtrVector<SignedNumber> m_signed_numbers;
};

class ColumnDefinition : public ASTNode {
public:
    ColumnDefinition(String name, NonnullRefPtr<TypeName> type_name)
        : m_name(move(name))
        , m_type_name(move(type_name))
    {
    }

    const String& name() const { return m_name; }
    const NonnullRefPtr<TypeName>& type_name() const { return m_type_name; }

private:
    String m_name;
    NonnullRefPtr<TypeName> m_type_name;
};

class CreateTable : public Statement {
public:
    CreateTable(String schema_name, String table_name, NonnullRefPtrVector<ColumnDefinition> columns, bool is_temporary, bool is_error_if_table_exists)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_columns(move(columns))
        , m_is_temporary(is_temporary)
        , m_is_error_if_table_exists(is_error_if_table_exists)
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    const NonnullRefPtrVector<ColumnDefinition> columns() const { return m_columns; }
    bool is_temporary() const { return m_is_temporary; }
    bool is_error_if_table_exists() const { return m_is_error_if_table_exists; }

private:
    String m_schema_name;
    String m_table_name;
    NonnullRefPtrVector<ColumnDefinition> m_columns;
    bool m_is_temporary;
    bool m_is_error_if_table_exists;
};

class DropTable : public Statement {
public:
    DropTable(String schema_name, String table_name, bool is_error_if_table_does_not_exist)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_is_error_if_table_does_not_exist(is_error_if_table_does_not_exist)
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    bool is_error_if_table_does_not_exist() const { return m_is_error_if_table_does_not_exist; }

private:
    String m_schema_name;
    String m_table_name;
    bool m_is_error_if_table_does_not_exist;
};

}
