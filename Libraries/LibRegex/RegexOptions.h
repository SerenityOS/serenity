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

#include <AK/Types.h>
#include <stdio.h>
#ifdef __serenity__
#    include <regex.h>
#else
#    include <LibC/regex.h>
#endif

namespace regex {

using FlagsUnderlyingType = u16;

enum class AllFlags {
    Global = __Regex_Global,                             // All matches (don't return after first match)
    Insensitive = __Regex_Insensitive,                   // Case insensitive match (ignores case of [a-zA-Z])
    Ungreedy = __Regex_Ungreedy,                         // The match becomes lazy by default. Now a ? following a quantifier makes it greedy
    Unicode = __Regex_Unicode,                           // Enable all unicode features and interpret all unicode escape sequences as such
    Extended = __Regex_Extended,                         // Ignore whitespaces. Spaces and text after a # in the pattern are ignored
    Extra = __Regex_Extra,                               // Disallow meaningless escapes. A \ followed by a letter with no special meaning is faulted
    MatchNotBeginOfLine = __Regex_MatchNotBeginOfLine,   // Pattern is not forced to ^ -> search in whole string!
    MatchNotEndOfLine = __Regex_MatchNotEndOfLine,       // Don't Force the dollar sign, $, to always match end of the string, instead of end of the line. This option is ignored if the Multiline-flag is set
    SkipSubExprResults = __Regex_SkipSubExprResults,     // Do not return sub expressions in the result
    StringCopyMatches = __Regex_StringCopyMatches,       // Do explicitly copy results into new allocated string instead of StringView to original string.
    SingleLine = __Regex_SingleLine,                     // Dot matches newline characters
    Sticky = __Regex_Sticky,                             // Force the pattern to only match consecutive matches from where the previous match ended.
    Multiline = __Regex_Multiline,                       // Handle newline characters. Match each line, one by one.
    SkipTrimEmptyMatches = __Regex_SkipTrimEmptyMatches, // Do not remove empty capture group results.
    Internal_Stateful = __Regex_Internal_Stateful,       // Make global matches match one result at a time, and further match() calls on the same instance continue where the previous one left off.
    Last = Internal_Stateful,
};

enum class PosixFlags : FlagsUnderlyingType {
    Global = (FlagsUnderlyingType)AllFlags::Global,
    Insensitive = (FlagsUnderlyingType)AllFlags::Insensitive,
    Ungreedy = (FlagsUnderlyingType)AllFlags::Ungreedy,
    Unicode = (FlagsUnderlyingType)AllFlags::Unicode,
    Extended = (FlagsUnderlyingType)AllFlags::Extended,
    Extra = (FlagsUnderlyingType)AllFlags::Extra,
    MatchNotBeginOfLine = (FlagsUnderlyingType)AllFlags::MatchNotBeginOfLine,
    MatchNotEndOfLine = (FlagsUnderlyingType)AllFlags::MatchNotEndOfLine,
    SkipSubExprResults = (FlagsUnderlyingType)AllFlags::SkipSubExprResults,
    SkipTrimEmptyMatches = (FlagsUnderlyingType)AllFlags::SkipTrimEmptyMatches,
    Multiline = (FlagsUnderlyingType)AllFlags::Multiline,
    StringCopyMatches = (FlagsUnderlyingType)AllFlags::StringCopyMatches,
};

enum class ECMAScriptFlags : FlagsUnderlyingType {
    Global = (FlagsUnderlyingType)AllFlags::Global | (FlagsUnderlyingType)AllFlags::Internal_Stateful, // Note: ECMAScript "Global" creates a stateful regex.
    Insensitive = (FlagsUnderlyingType)AllFlags::Insensitive,
    Ungreedy = (FlagsUnderlyingType)AllFlags::Ungreedy,
    Unicode = (FlagsUnderlyingType)AllFlags::Unicode,
    Extended = (FlagsUnderlyingType)AllFlags::Extended,
    Extra = (FlagsUnderlyingType)AllFlags::Extra,
    SingleLine = (FlagsUnderlyingType)AllFlags::SingleLine,
    Sticky = (FlagsUnderlyingType)AllFlags::Sticky,
    Multiline = (FlagsUnderlyingType)AllFlags::Multiline,
    StringCopyMatches = (FlagsUnderlyingType)AllFlags::StringCopyMatches,
};

template<class T>
class RegexOptions {
public:
    using FlagsType = T;

    RegexOptions() = default;

    RegexOptions(T flags)
        : m_flags(flags)
    {
    }

    template<class U>
    RegexOptions(RegexOptions<U> other)
        : m_flags((T) static_cast<FlagsUnderlyingType>(other.value()))
    {
    }

    operator bool() const { return !!*this; }
    bool operator!() const { return (FlagsUnderlyingType)m_flags == 0; }

    RegexOptions<T> operator|(T flag) const { return RegexOptions<T> { (T)((FlagsUnderlyingType)m_flags | (FlagsUnderlyingType)flag) }; }
    RegexOptions<T> operator&(T flag) const { return RegexOptions<T> { (T)((FlagsUnderlyingType)m_flags & (FlagsUnderlyingType)flag) }; }

    RegexOptions<T>& operator|=(T flag)
    {
        m_flags = (T)((FlagsUnderlyingType)m_flags | (FlagsUnderlyingType)flag);
        return *this;
    }

    RegexOptions<T>& operator&=(T flag)
    {
        m_flags = (T)((FlagsUnderlyingType)m_flags & (FlagsUnderlyingType)flag);
        return *this;
    }

    void reset_flags() { m_flags = (T)0; }
    void reset_flag(T flag) { m_flags = (T)((FlagsUnderlyingType)m_flags & ~(FlagsUnderlyingType)flag); }
    void set_flag(T flag) { *this |= flag; }
    bool has_flag_set(T flag) const { return (FlagsUnderlyingType)flag == ((FlagsUnderlyingType)m_flags & (FlagsUnderlyingType)flag); }
    T value() const { return m_flags; }

private:
    T m_flags { 0 };
};

template<class T>
inline RegexOptions<T> operator|(T lhs, T rhs)
{
    return RegexOptions<T> { lhs } |= rhs;
}

template<class T>
inline RegexOptions<T> operator&(T lhs, T rhs)
{
    return RegexOptions<T> { lhs } &= rhs;
}

template<class T>
inline T operator~(T flag)
{
    return (T) ~((FlagsUnderlyingType)flag);
}

using AllOptions = RegexOptions<AllFlags>;
using ECMAScriptOptions = RegexOptions<ECMAScriptFlags>;
using PosixOptions = RegexOptions<PosixFlags>;

}

using regex::ECMAScriptFlags;
using regex::ECMAScriptOptions;
using regex::PosixFlags;
using regex::PosixOptions;
