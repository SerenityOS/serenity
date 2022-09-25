/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Variant.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#typedefdef-xmlhttprequestbodyinit
using XMLHttpRequestBodyInit = Variant<JS::Handle<FileAPI::Blob>, JS::Handle<JS::Object>, JS::Handle<URL::URLSearchParams>, String>;

// https://fetch.spec.whatwg.org/#bodyinit
using BodyInit = Variant<JS::Handle<Streams::ReadableStream>, JS::Handle<FileAPI::Blob>, JS::Handle<JS::Object>, JS::Handle<URL::URLSearchParams>, String>;

using BodyInitOrReadbleBytes = Variant<JS::Handle<Streams::ReadableStream>, JS::Handle<FileAPI::Blob>, JS::Handle<JS::Object>, JS::Handle<URL::URLSearchParams>, String, ReadonlyBytes>;
WebIDL::ExceptionOr<Infrastructure::BodyWithType> extract_body(JS::Realm&, BodyInitOrReadbleBytes const&, bool keepalive = false);

}
