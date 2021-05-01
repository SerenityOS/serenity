/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <LibPDF/Parser.h>

namespace PDF {

NonnullRefPtr<Document> Document::from(const ReadonlyBytes& bytes)
{
    Parser parser({}, bytes);
    return adopt_ref(*new Document(move(parser)));
}

Document::Document(Parser&& parser)
    : m_parser(parser)
{
    VERIFY(m_parser.perform_validation());
    auto [xref_table, trailer] = m_parser.parse_last_xref_table_and_trailer();

    m_xref_table = xref_table;
    m_trailer = trailer;
}

}
