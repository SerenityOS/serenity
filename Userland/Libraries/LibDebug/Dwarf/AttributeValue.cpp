/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AttributeValue.h"
#include "CompilationUnit.h"

namespace Debug::Dwarf {

ErrorOr<FlatPtr> AttributeValue::as_addr() const
{
    switch (m_form) {
    case AttributeDataForm::Addr:
        return m_data.as_addr;
    case AttributeDataForm::AddrX:
    case AttributeDataForm::AddrX1:
    case AttributeDataForm::AddrX2:
    case AttributeDataForm::AddrX3:
    case AttributeDataForm::AddrX4: {
        auto index = m_data.as_unsigned;
        return m_compilation_unit->get_address(index);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<char const*> AttributeValue::as_string() const
{
    switch (m_form) {
    case AttributeDataForm::String:
    case AttributeDataForm::StringPointer:
    case AttributeDataForm::LineStrP:
        return m_data.as_string;
    case AttributeDataForm::StrX:
    case AttributeDataForm::StrX1:
    case AttributeDataForm::StrX2:
    case AttributeDataForm::StrX3:
    case AttributeDataForm::StrX4: {
        auto index = m_data.as_unsigned;
        return m_compilation_unit->get_string(index);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}
}
