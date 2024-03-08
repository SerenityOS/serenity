/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibCore/File.h>
#include <LibXML/Parser/Parser.h>

#include "Function.h"
#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/TextParser.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

SpecificationParsingStep::SpecificationParsingStep()
    : CompilationStep("parser"sv)
{
}

SpecificationParsingStep::~SpecificationParsingStep() = default;

void SpecificationParsingStep::run(TranslationUnitRef translation_unit)
{
    SpecificationParsingContext ctx(translation_unit);
    auto filename = translation_unit->filename();

    auto file_or_error = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        ctx.diag().fatal_error(Location::global_scope(),
            "unable to open '{}': {}", filename, file_or_error.error());
        return;
    }

    auto input_or_error = file_or_error.value()->read_until_eof();
    if (input_or_error.is_error()) {
        ctx.diag().fatal_error(Location::global_scope(),
            "unable to read '{}': {}", filename, input_or_error.error());
        return;
    }
    m_input = input_or_error.release_value();

    XML::Parser parser { m_input };
    auto document_or_error = parser.parse();
    if (document_or_error.is_error()) {
        ctx.diag().fatal_error(ctx.file_scope(),
            "XML::Parser failed to parse input: {}", document_or_error.error());
        ctx.diag().note(ctx.file_scope(),
            "since XML::Parser backtracks on error, the message above is likely to point to the "
            "first tag in the input - use external XML verifier to find out the exact cause of error");
        return;
    }
    m_document = make<XML::Document>(document_or_error.release_value());

    auto const& root = m_document->root();
    if (!root.is_element() || root.as_element().name != tag_specification) {
        ctx.diag().fatal_error(ctx.location_from_xml_offset(root.offset),
            "document root must be <specification> tag");
        return;
    }

    m_specification = Specification::create(ctx, &root);
    m_specification->collect_into(translation_unit);
}

}
