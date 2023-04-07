/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/Network/TCP.h>
#include <Kernel/Net/Routing.h>
#include <Kernel/Net/TCPSocket.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSNetworkTCPStats::SysFSNetworkTCPStats(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSNetworkTCPStats> SysFSNetworkTCPStats::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSNetworkTCPStats(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSNetworkTCPStats::try_generate(KBufferBuilder& builder)
{
    auto array = TRY(JsonArraySerializer<>::try_create(builder));
    TRY(TCPSocket::try_for_each([&array](auto& socket) -> ErrorOr<void> {
        auto obj = TRY(array.add_object());
        auto local_address = TRY(socket.local_address().to_string());
        TRY(obj.add("local_address"sv, local_address->view()));
        TRY(obj.add("local_port"sv, socket.local_port()));
        auto peer_address = TRY(socket.peer_address().to_string());
        TRY(obj.add("peer_address"sv, peer_address->view()));
        TRY(obj.add("peer_port"sv, socket.peer_port()));
        TRY(obj.add("state"sv, TCPSocket::to_string(socket.state())));
        TRY(obj.add("ack_number"sv, socket.ack_number()));
        TRY(obj.add("sequence_number"sv, socket.sequence_number()));
        TRY(obj.add("packets_in"sv, socket.packets_in()));
        TRY(obj.add("bytes_in"sv, socket.bytes_in()));
        TRY(obj.add("packets_out"sv, socket.packets_out()));
        TRY(obj.add("bytes_out"sv, socket.bytes_out()));
        auto current_process_credentials = Process::current().credentials();
        if (current_process_credentials->is_superuser() || current_process_credentials->uid() == socket.origin_uid()) {
            TRY(obj.add("origin_pid"sv, socket.origin_pid().value()));
            TRY(obj.add("origin_uid"sv, socket.origin_uid().value()));
            TRY(obj.add("origin_gid"sv, socket.origin_gid().value()));
        }
        TRY(obj.finish());
        return {};
    }));
    TRY(array.finish());
    return {};
}

}
