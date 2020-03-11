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
    case TokenType::AsteriskEquals:
        return "AsteriskEquals";
    case TokenType::BoolLiteral:
        return "BoolLiteral";
    case TokenType::BracketOpen:
        return "BracketOpen";
    case TokenType::BracketClose:
        return "BracketClose";
    case TokenType::Catch:
        return "Catch";
    case TokenType::Class:
        return "Class";
    case TokenType::Comma:
        return "Comma";
    case TokenType::Const:
        return "Const";
    case TokenType::CurlyClose:
        return "CurlyClose";
    case TokenType::CurlyOpen:
        return "CurlyOpen";
    case TokenType::Delete:
        return "Delete";
    case TokenType::Do:
        return "Do";
    case TokenType::DoubleAmpersand:
        return "DoubleAmpersand";
    case TokenType::DoublePipe:
        return "DoublePipe";
    case TokenType::Else:
        return "Else";
    case TokenType::Eof:
        return "Eof";
    case TokenType::Equals:
        return "Equals";
    case TokenType::EqualsEquals:
        return "EqualsEquals";
    case TokenType::ExclamationMark:
        return "ExclamationMark";
    case TokenType::ExclamationMarkEquals:
        return "ExclamationMarkEquals";
    case TokenType::Finally:
        return "Finally";
    case TokenType::Function:
        return "Function";
    case TokenType::GreaterThan:
        return "GreaterThan";
    case TokenType::Identifier:
        return "Identifier";
    case TokenType::If:
        return "If";
    case TokenType::Interface:
        return "Interface";
    case TokenType::Invalid:
        return "Invalid";
    case TokenType::LessThan:
        return "LessThan";
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
    case TokenType::RegexLiteral:
        return "RegexLiteral";
    case TokenType::Return:
        return "Return";
    case TokenType::Semicolon:
        return "Semicolon";
    case TokenType::ShiftLeft:
        return "ShiftLeft";
    case TokenType::ShiftRight:
        return "ShiftRight";
    case TokenType::Slash:
        return "Slash";
    case TokenType::SlashEquals:
        return "SlashEquals";
    case TokenType::StringLiteral:
        return "StringLiteral";
    case TokenType::Try:
        return "Try";
    case TokenType::Var:
        return "Var";
    case TokenType::While:
        return "While";
    default:
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

double Token::double_value() const
{
    // FIXME: need to parse double instead of int
    bool ok;
    return m_value.to_int(ok);
}

String Token::string_value() const
{
    // FIXME: unescape the string and remove quotes
    return m_value;
}

bool Token::bool_value() const
{
    return m_value == "true";
}

}
