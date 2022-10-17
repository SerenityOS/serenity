/*
 * Copyright (c) 2022, LI YUBEI <leeight@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibJS/Forward.h>

namespace JS {

// https://github.com/tc39/proposal-regexp-legacy-features#regexp
// The %RegExp% instrinsic object, which is the builtin RegExp constructor, has the following additional internal slots:
// [[RegExpInput]]
// [[RegExpLastMatch]]
// [[RegExpLastParen]]
// [[RegExpLeftContext]]
// [[RegExpRightContext]]
// [[RegExpParen1]] ... [[RegExpParen9]]
class RegExpLegacyStaticProperties {
public:
    Optional<String> const& input() const { return m_input; }
    Optional<String> const& last_match() const { return m_last_match; }
    Optional<String> const& last_paren() const { return m_last_paren; }
    Optional<String> const& left_context() const { return m_left_context; }
    Optional<String> const& right_context() const { return m_right_context; }
    Optional<String> const& $1() const { return m_$1; }
    Optional<String> const& $2() const { return m_$2; }
    Optional<String> const& $3() const { return m_$3; }
    Optional<String> const& $4() const { return m_$4; }
    Optional<String> const& $5() const { return m_$5; }
    Optional<String> const& $6() const { return m_$6; }
    Optional<String> const& $7() const { return m_$7; }
    Optional<String> const& $8() const { return m_$8; }
    Optional<String> const& $9() const { return m_$9; }

    void set_input(String input) { m_input = move(input); }
    void set_last_match(String last_match) { m_last_match = move(last_match); }
    void set_last_paren(String last_paren) { m_last_paren = move(last_paren); }
    void set_left_context(String left_context) { m_left_context = move(left_context); }
    void set_right_context(String right_context) { m_right_context = move(right_context); }
    void set_$1(String value) { m_$1 = move(value); }
    void set_$2(String value) { m_$2 = move(value); }
    void set_$3(String value) { m_$3 = move(value); }
    void set_$4(String value) { m_$4 = move(value); }
    void set_$5(String value) { m_$5 = move(value); }
    void set_$6(String value) { m_$6 = move(value); }
    void set_$7(String value) { m_$7 = move(value); }
    void set_$8(String value) { m_$8 = move(value); }
    void set_$9(String value) { m_$9 = move(value); }
    void invalidate();

private:
    Optional<String> m_input;
    Optional<String> m_last_match;
    Optional<String> m_last_paren;
    Optional<String> m_left_context;
    Optional<String> m_right_context;
    Optional<String> m_$1;
    Optional<String> m_$2;
    Optional<String> m_$3;
    Optional<String> m_$4;
    Optional<String> m_$5;
    Optional<String> m_$6;
    Optional<String> m_$7;
    Optional<String> m_$8;
    Optional<String> m_$9;
};

ThrowCompletionOr<void> set_legacy_regexp_static_property(VM& vm, RegExpConstructor& constructor, Value this_value, void (RegExpLegacyStaticProperties::*property_setter)(String), Value value);
ThrowCompletionOr<Value> get_legacy_regexp_static_property(VM& vm, RegExpConstructor& constructor, Value this_value, Optional<String> const& (RegExpLegacyStaticProperties::*property_getter)() const);
void update_legacy_regexp_static_properties(RegExpConstructor& constructor, Utf16String const& string, size_t start_index, size_t end_index, Vector<String> const& captured_values);
void invalidate_legacy_regexp_static_properties(RegExpConstructor& constructor);

}
