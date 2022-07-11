## Name

IPC - Interprocess communication files (.ipc)

## Description

IPC files are used to - with the help of the IPC compiler - generate message classes that will **wrap** messages for
interprocess communication in the system. Generated IPC message classes support encode and decode functions to pass
messages between the processes. Behavior resembles protobuf messages.

Formally, IPC files are written in domain-specific language, which is loosely inspired by C++ headers, to define IPC
protocols that are used to communicate with any services in the system.
Every IPC pair in the system has a client endpoint and a server endpoint that are described in the IPC files.
Each IPC endpoint should have a unique hashable name that will uniquely identify endpoints in the system.

There are 2 types of APIs that are supported by the IPC files: synchronous and asynchronous.
Synchronous function calls always wait for a response from the other side, while the asynchronous counterparts do not.
In other words, in case of the synchronous calls, the IPC library will not return until it has a response for a caller.

All APIs for the server endpoint should be asynchronous.

## Usage

To create a new connection, you first need to generate client and server endpoints.
These endpoints should implement the communication logic using the IPC compiler-generated API messages.

Then, you need to inherit your connection class from `IPC::ConnectionFromClient` with created server and client
endpoints as template parameters if it is a server connection. Otherwise, your class need to be inherited
from `IPC::ConnectionToServer` with created server and client endpoints as template parameters and from the client
endpoint class.

Note, there are 2 types of functions for sending the messages: synchronous and asynchronous.
Synchronous function is `post_message`, asynchronous variant is `send_sync`.

## Examples

*IPC file, MyServer.ipc*

```
endpoint MyServer
{
    SyncAPI(String text) => (i32 status)
    AsyncAPI(i32 mode) =|
}
```

*Part of the generated C++ messages, for more details please consult `Meta/Lagom/Tools/CodeGenerators/IPCCompiler/`*

```c++
class SyncAPI final : public IPC::Message {
public:
    typedef class SyncAPIResponse ResponseType;
    SyncAPI(const String& text) : m_text(text) {}
    virtual ~SyncAPI() override {}
    static OwnPtr<SyncAPI> decode(...)
    virtual IPC::MessageBuffer encode(...) const override
};
```

*Part of the connection implementations*

```c++
// Server side.
namespace MyServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<MyClientEndpoint, MyServerEndpoint> {};

}

// Client side.
namespace MyClient {
    
    class Client final
        : public IPC::ConnectionToServer<MyClientEndpoint, MyServerEndpoint>
        , public MyClientEndpoint {};
    
}
```
