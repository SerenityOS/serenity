/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Message.h"
#include <AK/Types.h>
#include <LibCore/File.h>

namespace SpiceAgent {

// Forward declaration
class SpiceAgent;

class FileTransferOperation : public RefCounted<FileTransferOperation> {
public:
    enum class Status {
        // If we haven't accepted the transfer yet.
        Pending,

        // If we are awaiting data from the server.
        Transferring,

        // If we've received all the data.
        Complete
    };

    static ErrorOr<NonnullRefPtr<FileTransferOperation>> create(FileTransferStartMessage& message);

    // Fired by the SpiceAgent when it wants the data transfer to begin.
    ErrorOr<void> begin_transfer(SpiceAgent& agent);

    // Fired by SpiceAgent when we have received all of the data needed for this transfer.
    ErrorOr<void> complete_transfer(SpiceAgent& agent);

    // Fired by the SpiceAgent when it recieves data related to this transfer.
    ErrorOr<void> on_data_received(FileTransferDataMessage& message);

private:
    // All file transfers start off as Pending.
    FileTransferOperation(u32 id, FileTransferStartMessage::Metadata metadata, NonnullOwnPtr<Core::File> destination)
        : m_destination(move(destination))
        , m_metadata(move(metadata))
        , m_id(id)
        , m_status(Status::Pending)
    {
    }

    void set_status(Status const& value)
    {
        m_status = value;
    }

    NonnullOwnPtr<Core::File> m_destination;
    FileTransferStartMessage::Metadata m_metadata;

    u32 m_id { 0 };
    Status m_status { Status::Pending };
};

}
