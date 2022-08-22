/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

NodeWrapper* wrap(JS::Realm&, DOM::Node&);

}
}
