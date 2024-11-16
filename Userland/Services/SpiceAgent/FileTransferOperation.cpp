/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileTransferOperation.h"
#include "SpiceAgent.h"
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Notification.h>
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

static Gfx::Bitmap const& downloads_folder_icon()
{
    static NonnullRefPtr<Gfx::Bitmap> s_icon = MUST(Gfx::Bitmap::load_from_file("/res/icons/32x32/downloads.png"sv));
    return *s_icon;
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

    // Notify the user that the file transfer is complete :^)
    auto notification = GUI::Notification::construct();
    notification->set_icon(&downloads_folder_icon());
    notification->set_title("File transfer complete!"_string);
    notification->set_text(TRY(String::formatted("{} is now in your Downloads folder.", m_metadata.name.to_byte_string())));
    notification->set_launch_url(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory(), m_metadata.name.to_byte_string()));
    notification->show();

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
