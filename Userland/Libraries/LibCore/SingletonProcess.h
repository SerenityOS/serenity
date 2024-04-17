/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Platform.h>
#include <AK/Span.h>
#include <LibCore/Socket.h>

#if defined(AK_OS_SERENITY)
#    error "Singleton process utilities are not to be used on SerenityOS"
#endif

namespace Core {

namespace Detail {
ErrorOr<NonnullOwnPtr<Core::LocalSocket>> launch_and_connect_to_process(StringView process_name, ReadonlySpan<ByteString> candidate_process_paths, ReadonlySpan<ByteString> command_line_arguments);
}

template<typename ClientType>
ErrorOr<NonnullRefPtr<ClientType>> launch_singleton_process(StringView process_name, ReadonlySpan<ByteString> candidate_process_paths, ReadonlySpan<ByteString> command_line_arguments = {})
{
    auto socket = TRY(Detail::launch_and_connect_to_process(process_name, candidate_process_paths, command_line_arguments));
    return adopt_nonnull_ref_or_enomem(new (nothrow) ClientType { move(socket) });
}

}
