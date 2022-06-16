# Serenity Formats

This document summarizes all non-standard file formats, data formats, and mime-types used by Serenity.

## Inter Process Communication (through Unix sockets)

The various services each have their own formats, all very similar, and automatically implemented through LibIPC. The specifics depend on the corresponding source `.ipc` file.

The format can be identified by the format magic, which is derived in [`Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp`](../Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp)
from the service-endpoint name, e.g. "ClipboardClient" (which hashes to 4008793515) or "ClipboardServer" (which hashes to 1329211611).

In general, communication works by packets, which might have been sent in response to other packets. Everything is host endianness. Each packet consists of:
- a 32-bit message size (see `Connection::try_parse_messages` in [`Userland/Libraries/LibIPC/Connection.h`](../Userland/Libraries/LibIPC/Connection.h))
- the 32-bit endpoint magic (note that responses use the endpoint of the requesting packet, so the Clipboard server might use the endpoint magic 4008793515 to signal that this packet is a response)
- the 32-bit message ID within that endpoint (sequentially assigned, starting at 1)
- the data of that message itself (e.g. see `Messages::ClipboardServer::SetClipboardData::{en,de}code` in `Build/*/Userland/Services/Clipboard/ClipboardServerEndpoint.h`).
