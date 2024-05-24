/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// RUN: %clang++ -cc1 -verify %plugin_opts% %s 2>&1

#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrototypeObject.h>

// Note: Using WEB_PLATFORM_OBJECT() on a class that doesn't inherit from Web::Bindings::PlatformObject
//       is a compilation error, so that is not tested here.
// Note: It's pretty hard to have the incorrect type in a JS::PrototypeObject, since the base name would
//       have a comma in it, and wouldn't be passable as the basename without a typedef.

class CellWithObjectMacro : JS::Cell {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_CELL}}
    JS_OBJECT(CellWithObjectMacro, JS::Cell);
};

class CellWithEnvironmentMacro : JS::Cell {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_CELL}}
    JS_ENVIRONMENT(CellWithEnvironmentMacro, JS::Cell);
};

class ObjectWithCellMacro : JS::Object {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_OBJECT}}
    JS_CELL(ObjectWithCellMacro, JS::Object);
};

class ObjectWithEnvironmentMacro : JS::Object {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_OBJECT}}
    JS_ENVIRONMENT(ObjectWithEnvironmentMacro, JS::Object);
};

// JS_PROTOTYPE_OBJECT can only be used in the JS namespace
namespace JS {

class CellWithPrototypeMacro : Cell {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_CELL}}
    JS_PROTOTYPE_OBJECT(CellWithPrototypeMacro, Cell, Cell);
};

class ObjectWithPrototypeMacro : Object {
    // expected-error@+1 {{Invalid JS-CELL-like macro invocation; expected JS_OBJECT}}
    JS_PROTOTYPE_OBJECT(ObjectWithPrototypeMacro, Object, Object);
};

}
