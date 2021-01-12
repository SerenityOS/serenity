/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/String.h>
#include <AK/Types.h>
#ifdef __serenity__
#    include <regex.h>
#else
#    include <LibC/regex.h>
#endif

namespace regex {

enum class Error : u8 {
    NoError = __Regex_NoError,
    InvalidPattern = __Regex_InvalidPattern,                         // Invalid regular expression.
    InvalidCollationElement = __Regex_InvalidCollationElement,       // Invalid collating element referenced.
    InvalidCharacterClass = __Regex_InvalidCharacterClass,           // Invalid character class type referenced.
    InvalidTrailingEscape = __Regex_InvalidTrailingEscape,           // Trailing \ in pattern.
    InvalidNumber = __Regex_InvalidNumber,                           // Number in \digit invalid or in error.
    MismatchingBracket = __Regex_MismatchingBracket,                 // [ ] imbalance.
    MismatchingParen = __Regex_MismatchingParen,                     // ( ) imbalance.
    MismatchingBrace = __Regex_MismatchingBrace,                     // { } imbalance.
    InvalidBraceContent = __Regex_InvalidBraceContent,               // Content of {} invalid: not a number, number too large, more than two numbers, first larger than second.
    InvalidBracketContent = __Regex_InvalidBracketContent,           // Content of [] invalid.
    InvalidRange = __Regex_InvalidRange,                             // Invalid endpoint in range expression.
    InvalidRepetitionMarker = __Regex_InvalidRepetitionMarker,       // ?, * or + not preceded by valid regular expression.
    ReachedMaxRecursion = __Regex_ReachedMaxRecursion,               // MaximumRecursion has been reached.
    EmptySubExpression = __Regex_EmptySubExpression,                 // Sub expression has empty content.
    InvalidCaptureGroup = __Regex_InvalidCaptureGroup,               // Content of capture group is invalid.
    InvalidNameForCaptureGroup = __Regex_InvalidNameForCaptureGroup, // Name of capture group is invalid.
};

inline String get_error_string(Error error)
{
    switch (error) {
    case Error::NoError:
        return "No error";
    case Error::InvalidPattern:
        return "Invalid regular expression.";
    case Error::InvalidCollationElement:
        return "Invalid collating element referenced.";
    case Error::InvalidCharacterClass:
        return "Invalid character class type referenced.";
    case Error::InvalidTrailingEscape:
        return "Trailing \\ in pattern.";
    case Error::InvalidNumber:
        return "Number in \\digit invalid or in error.";
    case Error::MismatchingBracket:
        return "[ ] imbalance.";
    case Error::MismatchingParen:
        return "( ) imbalance.";
    case Error::MismatchingBrace:
        return "{ } imbalance.";
    case Error::InvalidBraceContent:
        return "Content of {} invalid: not a number, number too large, more than two numbers, first larger than second.";
    case Error::InvalidBracketContent:
        return "Content of [] invalid.";
    case Error::InvalidRange:
        return "Invalid endpoint in range expression.";
    case Error::InvalidRepetitionMarker:
        return "?, * or + not preceded by valid regular expression.";
    case Error::ReachedMaxRecursion:
        return "Maximum recursion has been reached.";
    case Error::EmptySubExpression:
        return "Sub expression has empty content.";
    case Error::InvalidCaptureGroup:
        return "Content of capture group is invalid.";
    case Error::InvalidNameForCaptureGroup:
        return "Name of capture group is invalid.";
    }
    return "Undefined error.";
}
}

using regex::Error;
using regex::get_error_string;
