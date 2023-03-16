/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Forward.h>

namespace Web::WebAssembly {

void visit_edges(JS::Cell::Visitor&);

bool validate(JS::VM&, JS::Handle<JS::Object>& bytes);
WebIDL::ExceptionOr<JS::Value> compile(JS::VM&, JS::Handle<JS::Object>& bytes);

WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM&, JS::Handle<JS::Object>& bytes, Optional<JS::Handle<JS::Object>>& import_object);
WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM&, Module const& module_object, Optional<JS::Handle<JS::Object>>& import_object);

}
