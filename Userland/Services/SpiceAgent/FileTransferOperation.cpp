/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileTransferOperation.h"
#include "SpiceAgent.h"
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibURL/URL.h>

namespace SpiceAgent {

ErrorOr<NonnullRefPtr<FileTransferOperation>> FileTransferOperation::create(FileTransferStartMessage& message)
{
    // Attempt to construct a path.
    StringBuilder destination_builder;
    TRY(destination_builder.try_append(Core::StandardPaths::downloads_directory()));
    TRY(destination_builder.try_append('/'));
    TRY(destination_builder.try_append(message.metadata().name));

    auto destination_path = TRY(destination_builder.to_string());

    // Ensure that the file doesn't already exist, and if it does, remove it.
    if (FileSystem::exists(destination_path)) {
        // If that "file" is a directory, we should stop doing anything else.
        if (FileSystem::is_directory(destination_path)) {
            return Error::from_string_literal("The name of the file being transferred is already taken by a directory!");
        }

        TRY(FileSystem::remove(destination_path, FileSystem::RecursionMode::Disallowed));
    }

    auto file = TRY(Core::File::open(destination_path, Core::File::OpenMode::ReadWrite));
    return TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) FileTransferOperation(message.id(), message.metadata(), move(file))));
}

ErrorOr<void> FileTransferOperation::begin_transfer(SpiceAgent& agent)
{
    // Ensure that we are in the `Pending` status.
    if (m_status != Status::Pending) {
        return Error::from_string_literal("Attempt to start a file transfer which has already been started!");
    }

    // Send the CanSendData status to the server.
    auto status_message = FileTransferStatusMessage(m_id, FileTransferStatus::CanSendData);
    TRY(agent.send_message(status_message));

    // We are now in the transferring stage!
    set_status(Status::Transferring);

    return {};
}

ErrorOr<void> FileTransferOperation::complete_transfer(SpiceAgent& agent)
{
    // Ensure that we are in the `Transferring` status.
    if (m_status != Status::Transferring) {
        return Error::from_string_literal("Attempt to call `on_data_received` on a file transfer which has already been completed!");
    }

    // We are now in the complete stage :^)
    set_status(Status::Complete);

    // Send the Success status to the server, since we have received the data, and handled it correctly
    auto status_message = FileTransferStatusMessage(m_id, FileTransferStatus::Success);
    TRY(agent.send_message(status_message));

    // Open the file manager for the user :^)
    // FIXME: This currently opens a new window for each successful file transfer...
    //        Is there a way/can we make a way for it to highlight a new file in an already-open window?
    Desktop::Launcher::open(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory(), m_metadata.name.to_byte_string()));

    return {};
}

ErrorOr<void> FileTransferOperation::on_data_received(FileTransferDataMessage& message)
{
    // Ensure that we are in the `Transferring` status.
    if (m_status != Status::Transferring) {
        return Error::from_string_literal("Attempt to call `on_data_received` on a file transfer which has already been completed!");
    }

    // Attempt to write more data to the file.
    TRY(m_destination->write_until_depleted(message.contents()));

    return {};
}

}
