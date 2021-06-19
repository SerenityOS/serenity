/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Value require_object_coercible(GlobalObject&, Value);
Function* get_method(GlobalObject& global_object, Value, PropertyName const&);
size_t length_of_array_like(GlobalObject&, Object const&);
MarkedValueList create_list_from_array_like(GlobalObject&, Value, AK::Function<Result<void, ErrorType>(Value)> = {});
Function* species_constructor(GlobalObject&, Object const&, Function& default_constructor);
GlobalObject* get_function_realm(GlobalObject&, Function const&);

}
