/*
 * Copyright (c) 2022 Daniel Ehrenberg <dan@littledan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <AK/Types.h>
#include <AK/Result.h>
#include <LibJS/Forward.h>

// Structured serialize is an entirely different format from IPC because:
// - It contains representation of type information
// - It may contain circularities
// - It is restricted to JS values

namespace Web::HTML {

JS::ThrowCompletionOr<Vector<u32>> structured_serialize(JS::GlobalObject& global_object, JS::Value);
ErrorOr<JS::Value> structured_deserialize(JS::GlobalObject& global_object, const Vector<u32>&);

}
