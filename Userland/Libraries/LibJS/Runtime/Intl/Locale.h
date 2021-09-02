/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Intl {

class Locale final : public Object {
    JS_OBJECT(Locale, Object);

public:
    Locale(Object& prototype);
    virtual ~Locale() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    bool has_calendar() const { return m_calendar.has_value(); }
    String const& calendar() const { return m_calendar.value(); }
    void set_calendar(String calendar) { m_calendar = move(calendar); }

    bool has_case_first() const { return m_case_first.has_value(); }
    String const& case_first() const { return m_case_first.value(); }
    void set_case_first(String case_first) { m_case_first = move(case_first); }

    bool has_collation() const { return m_collation.has_value(); }
    String const& collation() const { return m_collation.value(); }
    void set_collation(String collation) { m_collation = move(collation); }

    bool has_hour_cycle() const { return m_hour_cycle.has_value(); }
    String const& hour_cycle() const { return m_hour_cycle.value(); }
    void set_hour_cycle(String hour_cycle) { m_hour_cycle = move(hour_cycle); }

    bool has_numbering_system() const { return m_numbering_system.has_value(); }
    String const& numbering_system() const { return m_numbering_system.value(); }
    void set_numbering_system(String numbering_system) { m_numbering_system = move(numbering_system); }

    bool numeric() const { return m_numeric; }
    void set_numeric(bool numeric) { m_numeric = numeric; }

private:
    String m_locale;                     // [[Locale]]
    Optional<String> m_calendar;         // [[Calendar]]
    Optional<String> m_case_first;       // [[CaseFirst]]
    Optional<String> m_collation;        // [[Collation]]
    Optional<String> m_hour_cycle;       // [[HourCycle]]
    Optional<String> m_numbering_system; // [[NumberingSystem]]
    bool m_numeric { false };            // [[Numeric]]
};

}
