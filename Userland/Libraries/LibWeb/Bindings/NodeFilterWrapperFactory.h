/*
 * Copyright (c) 2022, MillerTime <miller.time.baby@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

NodeFilterWrapper* wrap(JS::GlobalObject&, DOM::NodeFilter&);

}
}
