/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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

#include "Token.h"
#include <AK/Assertions.h>
#include <AK/StringBuilder.h>

namespace JS {

const char* Token::name(TokenType type)
{
    switch (type) {
    case TokenType::Ampersand:
        return "Ampersand";
    case TokenType::AmpersandEquals:
        return "AmpersandEquals";
    case TokenType::Asterisk:
        return "Asterisk";
    case TokenType::AsteriskAsteriskEquals:
        return "AsteriskAsteriskEquals";
    case TokenType::AsteriskEquals:
        return "AsteriskEquals";
    case TokenType::Await:
        return "Await";
    case TokenType::BoolLiteral:
        return "BoolLiteral";
    case TokenType::BracketOpen:
        return "BracketOpen";
    case TokenType::BracketClose:
        return "BracketClose";
    case TokenType::Break:
        return "Break";
    case TokenType::Caret:
        return "Caret";
    case TokenType::Case:
        return "Case";
    case TokenType::Catch:
        return "Catch";
    case TokenType::Class:
        return "Class";
    case TokenType::Colon:
        return "Colon";
    case TokenType::Comma:
        return "Comma";
    case TokenType::Const:
        return "Const";
    case TokenType::CurlyClose:
        return "CurlyClose";
    case TokenType::CurlyOpen:
        return "CurlyOpen";
    case TokenType::Default:
        return "Default";
    case TokenType::Delete:
        return "Delete";
    case TokenType::Do:
        return "Do";
    case TokenType::DoubleAmpersand:
        return "DoubleAmpersand";
    case TokenType::DoubleAsterisk:
        return "DoubleAsterisk";
    case TokenType::DoublePipe:
        return "DoublePipe";
    case TokenType::DoubleQuestionMark:
        return "DoubleQuestionMark";
    case TokenType::Else:
        return "Else";
    case TokenType::Eof:
        return "Eof";
    case TokenType::Equals:
        return "Equals";
    case TokenType::EqualsEquals:
        return "EqualsEquals";
    case TokenType::EqualsEqualsEquals:
        return "EqualsEqualsEquals";
    case TokenType::ExclamationMark:
        return "ExclamationMark";
    case TokenType::ExclamationMarkEquals:
        return "ExclamationMarkEquals";
    case TokenType::ExclamationMarkEqualsEquals:
        return "ExclamationMarkEqualsEquals";
    case TokenType::Finally:
        return "Finally";
    case TokenType::For:
        return "For";
    case TokenType::Function:
        return "Function";
    case TokenType::GreaterThan:
        return "GreaterThan";
    case TokenType::GreaterThanEquals:
        return "GreaterThanEquals";
    case TokenType::Identifier:
        return "Identifier";
    case TokenType::If:
        return "If";
    case TokenType::In:
        return "In";
    case TokenType::Instanceof:
        return "Instanceof";
    case TokenType::Interface:
        return "Interface";
    case TokenType::Invalid:
        return "Invalid";
    case TokenType::LessThan:
        return "LessThan";
    case TokenType::LessThanEquals:
        return "LessThanEquals";
    case TokenType::Let:
        return "Let";
    case TokenType::Minus:
        return "Minus";
    case TokenType::MinusEquals:
        return "MinusEquals";
    case TokenType::MinusMinus:
        return "MinusMinus";
    case TokenType::New:
        return "New";
    case TokenType::NullLiteral:
        return "NullLiteral";
    case TokenType::NumericLiteral:
        return "NumericLiteral";
    case TokenType::ParenClose:
        return "ParenClose";
    case TokenType::ParenOpen:
        return "ParenOpen";
    case TokenType::Percent:
        return "Percent";
    case TokenType::PercentEquals:
        return "PercentEquals";
    case TokenType::Period:
        return "Period";
    case TokenType::Pipe:
        return "Pipe";
    case TokenType::PipeEquals:
        return "PipeEquals";
    case TokenType::Plus:
        return "Plus";
    case TokenType::PlusEquals:
        return "PlusEquals";
    case TokenType::PlusPlus:
        return "PlusPlus";
    case TokenType::QuestionMark:
        return "QuestionMark";
    case TokenType::QuestionMarkPeriod:
        return "QuestionMarkPeriod";
    case TokenType::RegexLiteral:
        return "RegexLiteral";
    case TokenType::Return:
        return "Return";
    case TokenType::Semicolon:
        return "Semicolon";
    case TokenType::ShiftLeft:
        return "ShiftLeft";
    case TokenType::ShiftLeftEquals:
        return "ShiftLeftEquals";
    case TokenType::ShiftRight:
        return "ShiftRight";
    case TokenType::ShiftRightEquals:
        return "ShiftRightEquals";
    case TokenType::Slash:
        return "Slash";
    case TokenType::SlashEquals:
        return "SlashEquals";
    case TokenType::StringLiteral:
        return "StringLiteral";
    case TokenType::Switch:
        return "Switch";
    case TokenType::Tilde:
        return "Tilde";
    case TokenType::Try:
        return "Try";
    case TokenType::Typeof:
        return "Typeof";
    case TokenType::UndefinedLiteral:
        return "UndefinedLiteral";
    case TokenType::UnsignedShiftRight:
        return "UnsignedShiftRight";
    case TokenType::UnsignedShiftRightEquals:
        return "UnsignedShiftRightEquals";
    case TokenType::UnterminatedStringLiteral:
        return "UnterminatedStringLiteral";
    case TokenType::Var:
        return "Var";
    case TokenType::Void:
        return "Void";
    case TokenType::While:
        return "While";
    case TokenType::Yield:
        return "Yield";
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

double Token::double_value() const
{
    ASSERT(type() == TokenType::NumericLiteral);
    // FIXME: need to parse double instead of int
    bool ok;
    return m_value.to_int(ok);
}

String Token::string_value() const
{
    ASSERT(type() == TokenType::StringLiteral);
    StringBuilder builder;
    for (size_t i = 1; i < m_value.length() - 1; ++i) {
        if (m_value[i] == '\\' && i + 1 < m_value.length() - 1) {
            i++;
            switch (m_value[i]) {
            case 'b':
                builder.append('\b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 't':
                builder.append('\t');
                break;
            case 'v':
                builder.append('\v');
                break;
            case '0':
                builder.append((char)0);
                break;
            case '\'':
                builder.append('\'');
                break;
            case '"':
                builder.append('"');
                break;
            case '\\':
                builder.append('\\');
                break;
            default:
                // FIXME: Also parse octal, hex and unicode sequences
                // should anything else generate a syntax error?
                builder.append(m_value[i]);
            }

        } else {
            builder.append(m_value[i]);
        }
    }
    return builder.to_string();
}

bool Token::bool_value() const
{
    ASSERT(type() == TokenType::BoolLiteral);
    return m_value == "true";
}

}
