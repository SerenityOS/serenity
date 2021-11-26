/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    InvalidNameForProperty = __Regex_InvalidNameForProperty,         // Name of property is invalid.
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
    case Error::InvalidNameForProperty:
        return "Name of property is invalid.";
    }
    return "Undefined error.";
}
}

using regex::get_error_string;
