/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RecursionDecision.h>
#include <LibMarkdown/BlockQuote.h>
#include <LibMarkdown/CodeBlock.h>
#include <LibMarkdown/CommentBlock.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Heading.h>
#include <LibMarkdown/HorizontalRule.h>
#include <LibMarkdown/List.h>
#include <LibMarkdown/Paragraph.h>
#include <LibMarkdown/Table.h>

namespace Markdown {

class Visitor {
public:
    Visitor() = default;
    virtual ~Visitor() = default;

    virtual RecursionDecision visit(Document const&) { return RecursionDecision::Recurse; }

    virtual RecursionDecision visit(BlockQuote const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(CodeBlock const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(CommentBlock const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(ContainerBlock const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Heading const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(HorizontalRule const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(List const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Paragraph const&) { return RecursionDecision::Recurse; }

    virtual RecursionDecision visit(Table const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Table::Column const&) { return RecursionDecision::Recurse; }

    virtual RecursionDecision visit(Text const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::BreakNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::CodeNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::EmphasisNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::LinkNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::MultiNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::StrikeThroughNode const&) { return RecursionDecision::Recurse; }
    virtual RecursionDecision visit(Text::TextNode const&) { return RecursionDecision::Recurse; }

    virtual RecursionDecision visit(ByteString const&) { return RecursionDecision::Recurse; }
};

}
