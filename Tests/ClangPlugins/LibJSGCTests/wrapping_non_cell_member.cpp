/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// RUN: %clang++ -cc1 -verify %plugin_opts% %s 2>&1

#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Heap/MarkedVector.h>

struct NotACell { };

class TestClass {
    // expected-error@+1 {{Specialization type must inherit from JS::Cell}}
    JS::GCPtr<NotACell> m_member_1;
    // expected-error@+1 {{Specialization type must inherit from JS::Cell}}
    JS::NonnullGCPtr<NotACell> m_member_2;
    // expected-error@+1 {{Specialization type must inherit from JS::Cell}}
    JS::RawGCPtr<NotACell> m_member_3;
};
