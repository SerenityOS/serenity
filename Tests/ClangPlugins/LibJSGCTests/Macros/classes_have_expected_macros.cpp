/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// RUN: %clang++ -cc1 -verify %plugin_opts% %s 2>&1
// expected-no-diagnostics

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibWeb/Bindings/PlatformObject.h>

class TestCellClass : JS::Cell {
    JS_CELL(TestCellClass, JS::Cell);
};

class TestObjectClass : JS::Object {
    JS_OBJECT(TestObjectClass, JS::Object);
};

class TestEnvironmentClass : JS::Environment {
    JS_ENVIRONMENT(TestEnvironmentClass, JS::Environment);
};

class TestPlatformClass : Web::Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(TestPlatformClass, Web::Bindings::PlatformObject);
};

namespace JS {

class TestPrototypeClass : JS::PrototypeObject<TestCellClass, TestCellClass> {
    JS_PROTOTYPE_OBJECT(TestPrototypeClass, TestCellClass, TestCellClass);
};

}

// Nested classes
class Parent1 { };
class Parent2 : JS::Cell {
    JS_CELL(Parent2, JS::Cell);
};
class Parent3 { };
class Parent4 : public Parent2 {
    JS_CELL(Parent4, Parent2);
};

class NestedCellClass
    : Parent1
    , Parent3
    , Parent4 {
    JS_CELL(NestedCellClass, Parent4); // Not Parent2
};
