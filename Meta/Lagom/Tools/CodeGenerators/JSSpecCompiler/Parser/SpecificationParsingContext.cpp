/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/SpecificationParsing.h"

namespace JSSpecCompiler {

TranslationUnitRef SpecificationParsingContext::translation_unit()
{
    return m_translation_unit;
}

DiagnosticEngine& SpecificationParsingContext::diag()
{
    return m_translation_unit->diag();
}

LogicalLocation& SpecificationParsingContext::current_logical_scope()
{
    return *m_current_logical_scope;
}

int SpecificationParsingContext::step_list_nesting_level() const
{
    return m_step_list_nesting_level;
}

Location SpecificationParsingContext::file_scope() const
{
    return { .filename = m_translation_unit->filename() };
}

Location SpecificationParsingContext::location_from_xml_offset(LineTrackingLexer::Position position) const
{
    return {
        .filename = m_translation_unit->filename(),
        .line = position.line,
        .column = position.column,
        .logical_location = m_current_logical_scope,
    };
}

}
