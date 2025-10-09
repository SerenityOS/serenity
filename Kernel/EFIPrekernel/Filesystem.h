/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/EFIPrekernel/Error.h>
#include <Kernel/Firmware/EFI/Protocols/LoadedImage.h>
#include <Kernel/Firmware/EFI/Protocols/MediaAccess.h>

namespace Kernel {

EFIErrorOr<EFI::FileProtocol*> open_root_directory(EFI::LoadedImageProtocol* loaded_image_protocol);
EFIErrorOr<EFI::FileProtocol*> open_file(EFI::FileProtocol* base_directory, char16_t const* path, EFI::FileOpenMode open_mode, EFI::FileAttribute attributes = EFI::FileAttribute::None);
EFIErrorOr<void> close_file(EFI::FileProtocol*);
EFIErrorOr<ByteBuffer> read_entire_file(EFI::FileProtocol* file);

}
