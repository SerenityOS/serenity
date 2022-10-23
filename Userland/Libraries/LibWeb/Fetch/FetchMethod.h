/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Fetch/Request.h>

namespace Web::Fetch {

JS::NonnullGCPtr<JS::Promise> fetch_impl(JS::VM&, RequestInfo const& input, RequestInit const& init = {});
void abort_fetch(JS::VM&, JS::PromiseCapability const&, JS::NonnullGCPtr<Infrastructure::Request>, JS::GCPtr<Response>, JS::Value error);

}
