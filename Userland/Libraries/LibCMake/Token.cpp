/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"

namespace CMake {

Optional<ControlKeywordType> control_keyword_from_string(StringView value)
{
    if (value.equals_ignoring_ascii_case("if"sv))
        return ControlKeywordType::If;
    if (value.equals_ignoring_ascii_case("elseif"sv))
        return ControlKeywordType::ElseIf;
    if (value.equals_ignoring_ascii_case("else"sv))
        return ControlKeywordType::Else;
    if (value.equals_ignoring_ascii_case("endif"sv))
        return ControlKeywordType::EndIf;
    if (value.equals_ignoring_ascii_case("foreach"sv))
        return ControlKeywordType::ForEach;
    if (value.equals_ignoring_ascii_case("endforeach"sv))
        return ControlKeywordType::EndForEach;
    if (value.equals_ignoring_ascii_case("while"sv))
        return ControlKeywordType::While;
    if (value.equals_ignoring_ascii_case("endwhile"sv))
        return ControlKeywordType::EndWhile;
    if (value.equals_ignoring_ascii_case("break"sv))
        return ControlKeywordType::Break;
    if (value.equals_ignoring_ascii_case("continue"sv))
        return ControlKeywordType::Continue;
    if (value.equals_ignoring_ascii_case("return"sv))
        return ControlKeywordType::Return;
    if (value.equals_ignoring_ascii_case("macro"sv))
        return ControlKeywordType::Macro;
    if (value.equals_ignoring_ascii_case("endmacro"sv))
        return ControlKeywordType::EndMacro;
    if (value.equals_ignoring_ascii_case("function"sv))
        return ControlKeywordType::Function;
    if (value.equals_ignoring_ascii_case("endfunction"sv))
        return ControlKeywordType::EndFunction;
    if (value.equals_ignoring_ascii_case("block"sv))
        return ControlKeywordType::Block;
    if (value.equals_ignoring_ascii_case("endblock"sv))
        return ControlKeywordType::EndBlock;
    return {};
}

}
