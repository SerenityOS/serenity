/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/FileSystem/FUSE/Definitions.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

class FUSEConnection : public RefCounted<FUSEConnection> {
public:
    static ErrorOr<NonnullRefPtr<FUSEConnection>> try_create(NonnullRefPtr<OpenFileDescription> description)
    {
        if (!description->is_device())
            return Error::from_errno(EINVAL);

        if (description->device()->class_name() != "FUSEDevice"sv)
            return Error::from_errno(EINVAL);

        auto connection = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FUSEConnection(description)));

        auto* device = bit_cast<FUSEDevice*>(description->device());
        TRY(device->initialize_instance(*description));

        return connection;
    }

    static ErrorOr<NonnullOwnPtr<KBuffer>> create_request(FUSEOpcode opcode, u32 nodeid, u32 unique, ReadonlyBytes request_body)
    {
        size_t request_length = sizeof(fuse_in_header) + request_body.size();
        auto request = TRY(KBuffer::try_create_with_size("FUSE: Request"sv, request_length));
        memset(request->data(), 0, request_length);
        fuse_in_header* header = bit_cast<fuse_in_header*>(request->data());
        header->len = request_length;
        header->opcode = to_underlying(opcode);
        header->unique = unique;
        header->nodeid = nodeid;

        auto current_process_credentials = Process::current().credentials();
        header->uid = current_process_credentials->euid().value();
        header->gid = current_process_credentials->egid().value();
        header->pid = Process::current().pid().value();

        u8* payload = bit_cast<u8*>(request->data() + sizeof(fuse_in_header));
        memcpy(payload, request_body.data(), request_body.size());

        return request;
    }

    ErrorOr<NonnullOwnPtr<KBuffer>> send_request_and_wait_for_a_reply(FUSEOpcode opcode, u32 nodeid, ReadonlyBytes request_body)
    {
        auto* device = bit_cast<FUSEDevice*>(m_description->device());

        // FIXME: Send the init request from the filesystem itself right after it has been
        // mounted. (Without blocking the mount syscall.)
        if (!m_initialized)
            TRY(handle_init());

        u32 unique = m_unique++;
        auto request = TRY(create_request(opcode, nodeid, unique, request_body));
        auto response = TRY(device->send_request_and_wait_for_a_reply(m_description, request->bytes()));

        if (validate_response(*response, unique).is_error())
            return Error::from_errno(EIO);

        return response;
    }

    ~FUSEConnection()
    {
        auto* device = bit_cast<FUSEDevice*>(m_description->device());
        // Unblock the userspace daemon and tell it to shut down.
        device->shutdown_for_description(m_description);
        (void)m_description->close();
    }

private:
    FUSEConnection(NonnullRefPtr<OpenFileDescription> description)
        : m_description(description)
    {
    }

    ErrorOr<void> validate_response(KBuffer const& response, u32 unique)
    {
        if (response.size() < sizeof(fuse_out_header)) {
            dmesgln("FUSE: Received a request with a malformed header");
            return Error::from_errno(EINVAL);
        }

        fuse_out_header* header = bit_cast<fuse_out_header*>(response.data());
        if (header->unique != unique) {
            dmesgln("FUSE: Received a mismatched request (expected #{}, received #{})", unique, header->unique);
            return Error::from_errno(EINVAL);
        }

        if (header->len > response.size()) {
            dmesgln("FUSE: Received an excessively large request");
            return Error::from_errno(EINVAL);
        }

        return {};
    }

    ErrorOr<void> handle_init()
    {
        fuse_init_in init_request;
        init_request.major = FUSE_KERNEL_VERSION;
        init_request.minor = FUSE_KERNEL_MINOR_VERSION;
        init_request.max_readahead = 512;
        init_request.flags = 0;

        auto* device = bit_cast<FUSEDevice*>(m_description->device());

        auto request = TRY(create_request(FUSEOpcode::FUSE_INIT, 0, 0, { &init_request, sizeof(init_request) }));
        auto response = TRY(device->send_request_and_wait_for_a_reply(m_description, request->bytes()));

        if (validate_response(*response, 0).is_error())
            return Error::from_errno(EIO);

        fuse_init_out* init = bit_cast<fuse_init_out*>(response->data() + sizeof(fuse_out_header));

        m_major = init->major;
        m_minor = init->minor;

        m_initialized = true;

        return {};
    }

    NonnullRefPtr<OpenFileDescription> m_description;
    bool m_initialized { false };
    Atomic<u32> m_unique { 0 };

    u32 m_major { 0 };
    u32 m_minor { 0 };
};

}
