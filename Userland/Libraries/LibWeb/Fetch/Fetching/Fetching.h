/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch::Fetching {

#define ENUMERATE_BOOL_PARAMS                     \
    __ENUMERATE_BOOL_PARAM(IncludeCredentials)    \
    __ENUMERATE_BOOL_PARAM(IsAuthenticationFetch) \
    __ENUMERATE_BOOL_PARAM(IsNewConnectionFetch)  \
    __ENUMERATE_BOOL_PARAM(MakeCORSPreflight)     \
    __ENUMERATE_BOOL_PARAM(Recursive)             \
    __ENUMERATE_BOOL_PARAM(UseParallelQueue)

#define __ENUMERATE_BOOL_PARAM(Name) \
    enum class Name {                \
        Yes,                         \
        No,                          \
    };
ENUMERATE_BOOL_PARAMS
#undef __ENUMERATE_BOOL_PARAM

WebIDL::ExceptionOr<JS::NonnullGCPtr<Infrastructure::FetchController>> fetch(JS::Realm&, Infrastructure::Request&, Infrastructure::FetchAlgorithms const&, UseParallelQueue use_parallel_queue = UseParallelQueue::No);
WebIDL::ExceptionOr<JS::GCPtr<PendingResponse>> main_fetch(JS::Realm&, Infrastructure::FetchParams const&, Recursive recursive = Recursive::No);
WebIDL::ExceptionOr<void> fetch_response_handover(JS::Realm&, Infrastructure::FetchParams const&, Infrastructure::Response&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> scheme_fetch(JS::Realm&, Infrastructure::FetchParams const&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> http_fetch(JS::Realm&, Infrastructure::FetchParams const&, MakeCORSPreflight make_cors_preflight = MakeCORSPreflight::No);
WebIDL::ExceptionOr<JS::GCPtr<PendingResponse>> http_redirect_fetch(JS::Realm&, Infrastructure::FetchParams const&, Infrastructure::Response&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> http_network_or_cache_fetch(JS::Realm&, Infrastructure::FetchParams const&, IsAuthenticationFetch is_authentication_fetch = IsAuthenticationFetch::No, IsNewConnectionFetch is_new_connection_fetch = IsNewConnectionFetch::No);
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> nonstandard_resource_loader_file_or_http_network_fetch(JS::Realm&, Infrastructure::FetchParams const&, IncludeCredentials include_credentials = IncludeCredentials::No, IsNewConnectionFetch is_new_connection_fetch = IsNewConnectionFetch::No);
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> cors_preflight_fetch(JS::Realm&, Infrastructure::Request&);
}
