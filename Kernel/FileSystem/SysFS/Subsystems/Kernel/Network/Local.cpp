/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/Local.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSLocalNetStats::SysFSLocalNetStats(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSLocalNetStats> SysFSLocalNetStats::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSLocalNetStats(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSLocalNetStats::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(LocalSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
        auto obj = TRY(array.add_object());
        TRY(obj.add("path"sv, socket.socket_path()));
        TRY(obj.add("origin_pid"sv, socket.origin_pid().value()));
        TRY(obj.add("origin_uid"sv, socket.origin_uid().value()));
        TRY(obj.add("origin_gid"sv, socket.origin_gid().value()));
        TRY(obj.add("acceptor_pid"sv, socket.acceptor_pid().value()));
        TRY(obj.add("acceptor_uid"sv, socket.acceptor_uid().value()));
        TRY(obj.add("acceptor_gid"sv, socket.acceptor_gid().value()));
        TRY(obj.finish());
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
