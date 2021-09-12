/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Selector.h"
#include <AK/GenericLexer.h>
#include <ctype.h>

namespace Web::CSS {

Selector::Selector(Vector<CompoundSelector>&& compound_selectors)
    : m_compound_selectors(move(compound_selectors))
{
}

Selector::~Selector()
{
}

u32 Selector::specificity() const
{
    unsigned ids = 0;
    unsigned tag_names = 0;
    unsigned classes = 0;

    for (auto& list : m_compound_selectors) {
        for (auto& simple_selector : list.simple_selectors) {
            switch (simple_selector.type) {
            case SimpleSelector::Type::Id:
                ++ids;
                break;
            case SimpleSelector::Type::Class:
                ++classes;
                break;
            case SimpleSelector::Type::TagName:
                ++tag_names;
                break;
            default:
                break;
            }
        }
    }

    return ids * 0x10000 + classes * 0x100 + tag_names;
}

Selector::SimpleSelector::ANPlusBPattern Selector::SimpleSelector::ANPlusBPattern::parse(StringView const& args)
{
    // FIXME: Remove this when the DeprecatedCSSParser is gone.
    // The new Parser::parse_nth_child_pattern() does the same as this, using Tokens.
    CSS::Selector::SimpleSelector::ANPlusBPattern pattern;
    if (args.equals_ignoring_case("odd")) {
        pattern.step_size = 2;
        pattern.offset = 1;
    } else if (args.equals_ignoring_case("even")) {
        pattern.step_size = 2;
    } else {
        auto const consume_int = [](GenericLexer& lexer) -> Optional<int> {
            return AK::StringUtils::convert_to_int(lexer.consume_while([](char c) -> bool {
                return isdigit(c) || c == '+' || c == '-';
            }));
        };

        // Try to match any of following patterns:
        // 1. An+B
        // 2. An
        // 3. B
        // ...where "A" is "step_size", "B" is "offset" and rest are literals.
        // "A" can be omitted, in that case "A" = 1.
        // "A" may have "+" or "-" sign, "B" always must be predated by sign for pattern (1).

        int step_size_or_offset = 0;
        GenericLexer lexer { args };

        // "When a=1, or a=-1, the 1 may be omitted from the rule."
        if (lexer.consume_specific("n") || lexer.consume_specific("+n")) {
            step_size_or_offset = +1;
            lexer.retreat();
        } else if (lexer.consume_specific("-n")) {
            step_size_or_offset = -1;
            lexer.retreat();
        } else {
            auto const value = consume_int(lexer);
            if (!value.has_value())
                return {};
            step_size_or_offset = value.value();
        }

        if (lexer.consume_specific("n")) {
            lexer.ignore_while(isspace);
            if (lexer.next_is('+') || lexer.next_is('-')) {
                auto const sign = lexer.next_is('+') ? 1 : -1;
                lexer.ignore();
                lexer.ignore_while(isspace);

                // "An+B" pattern
                auto const offset = consume_int(lexer);
                if (!offset.has_value())
                    return {};
                pattern.step_size = step_size_or_offset;
                pattern.offset = sign * offset.value();
            } else {
                // "An" pattern
                pattern.step_size = step_size_or_offset;
            }
        } else {
            // "B" pattern
            pattern.offset = step_size_or_offset;
        }

        if (lexer.remaining().length() > 0)
            return {};
    }

    return pattern;
}

}
