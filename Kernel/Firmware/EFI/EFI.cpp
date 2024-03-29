/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/EFI/EFI.h>

namespace Kernel::EFI {

Optional<StringView> status_description(Status status)
{
    using enum Kernel::EFI::Status;
    switch (status) {
    case Success:
        return "The operation completed successfully."sv;
    case LoadError:
        return "The image failed to load."sv;
    case InvalidParameter:
        return "A parameter was incorrect."sv;
    case Unsupported:
        return "The operation is not supported."sv;
    case BadBufferSize:
        return "The buffer was not the proper size for the request."sv;
    case BufferTooSmall:
        return "The buffer is not large enough to hold the requested data. The required buffer size is returned in the appropriate parameter when this error occurs."sv;
    case NotReady:
        return "There is no data pending upon return."sv;
    case DeviceError:
        return "The physical device reported an error while attempting the operation."sv;
    case WriteProtected:
        return "The device cannot be written to."sv;
    case OutOfResources:
        return "A resource has run out."sv;
    case VolumeCorrupted:
        return "An inconstancy was detected on the file system causing the operating to fail."sv;
    case VolumeFull:
        return "There is no more space on the file system."sv;
    case NoMedia:
        return "The device does not contain any medium to perform the operation."sv;
    case MediaChanged:
        return "The medium in the device has changed since the last access."sv;
    case NotFound:
        return "The item was not found."sv;
    case AccessDenied:
        return "Access was denied."sv;
    case NoResponse:
        return "The server was not found or did not respond to the request."sv;
    case NoMapping:
        return "A mapping to a device does not exist."sv;
    case Timeout:
        return "The timeout time expired."sv;
    case NotStarted:
        return "The protocol has not been started."sv;
    case AlreadyStarted:
        return "The protocol has already been started."sv;
    case Aborted:
        return "The operation was aborted."sv;
    case ICMPError:
        return "An ICMP error occurred during the network operation."sv;
    case TFTPError:
        return "A TFTP error occurred during the network operation."sv;
    case ProtocolError:
        return "A protocol error occurred during the network operation."sv;
    case IncompatibleVersion:
        return "The function encountered an internal version that was incompatible with a version requested by the caller."sv;
    case SecurityViolation:
        return "The function was not performed due to a security violation."sv;
    case CRCError:
        return "A CRC error was detected."sv;
    case EndOfMedia:
        return "Beginning or end of media was reached"sv;
    case EndOfFile:
        return "The end of the file was reached."sv;
    case InvalidLanguage:
        return "The language specified was invalid."sv;
    case CompromisedData:
        return "The security status of the data is unknown or compromised and the data must be updated or replaced to restore a valid security status."sv;
    case IPAddressConflict:
        return "There is an address conflict address allocation"sv;
    case HTTPError:
        return "A HTTP error occurred during the network operation."sv;
    }

    return {};
}

}
