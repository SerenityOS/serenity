## Name

IPC - Inter-Process Communication endpoint definition format (.ipc)

## Synopsis

The IPC format of SerenityOS is a domain-specific language (DSL) used to define communication endpoints for IPC.

## Description

Informally, IPC files - with the help of the IPC compiler - are used to generate message classes that will wrap messages
for interprocess communication in the system. IPC syntax is loosely inspired by C++ headers. Generated IPC message
classes support encode and decode functions to pass messages between the processes.

Every IPC pair in the system has a client endpoint and a server endpoint that are described in the IPC files.
Each IPC endpoint should have a unique hashable name that will uniquely identify endpoints in the system.

There are 2 types of APIs that are supported by the IPC files: synchronous and asynchronous.
Synchronous function calls always wait for a response from the other side, while the asynchronous counterparts do not.
In other words, in case of the synchronous calls, the IPC library will not return until it has a response for a caller.

Ideally, all APIs for the server endpoint should be asynchronous.

### Syntax Overview

Each IPC endpoint definition has the form:

```ipc
endpoint MyServer
{
    // messages...
}
```

You can use C++ `#include` directives before the `endpoint` keyword, which are copied to the resulting endpoint stub file. This is important if your messages use specific types of the library or application.

Each message must appear on its own line. Synchronous messages are defined with an arrow `=>` like

```ipc
message_name(arguments...) => (return values...)
```

and asynchronous messages are defined with a "stopped arrow" `=|` like

```ipc
message_name(arguments...) =|
```

The argument and return value lists define what data the message passes to the other side, and what data will be retrieved back. There is no limitation to a single return value, since it will be converted to a `Messages` struct that can hold as much data as needed. The lists are defined like normal C++ formal parameter lists:

```ipc
message_name(int first_param, bool second_param, My::Library::Type third_param) => (Optional<int> first_return, float second_return)
```

Currently, parameter types cannot contain spaces, so be careful with templates.

Parameters can contain attributes, which are a comma-separated list within a `[]` block preceding the type. The only currently implemented attribute is the `UTF8` attribute for string types, which will add a run-time validation that the string is valid UTF-8. For example:

```ipc
set_my_name([UTF8] String name) =|
```

For the String type in particular, this is not necessary.

### Formal Syntax

In Extended Backus-Naur form (and disregarding unwanted leniencies in the code generator's parser), IPC file syntax looks like the following:

```ebnf
IPCFile = { Endpoint } ;
Endpoint = Includes, "endpoint", Identifier, "{", { Message }, "}" ;
Identifier = (* C++ identifier *) ;
Includes = { (* C++ preprocessor #include directive *) } ;

Message = Identifier, "(", [ ParameterList ], ")", (SynchronousTrailer | AsynchronousTrailer) ;
SynchronousTrailer = "=>", "(", [ ParameterList ], ")";
AsynchronousTrailer = "=|" ;
ParameterList = Parameter, { ",", Parameter } ;
Parameter = [ "[", AttributeList, "]" ], TypeName, Identifier ;
AttributeList = Identifier, { ",", Identifier } ;
TypeName = (* C++ type name, without spaces *) ;
```

## Examples

To create a new connection, you first need to generate client and server endpoints.
These endpoints should implement the communication logic using the IPC compiler-generated API messages.

Start from defining an endpoint in the IPC file in `MyServer.ipc`.

```ipc
endpoint MyServer
{
    SyncAPI(ByteString text) => (i32 status)
    AsyncAPI(i32 mode) =|
}
```

Part of the generated C++ messages:

```cpp
class SyncAPI final : public IPC::Message {
public:
    using ResponseType = SyncAPIResponse;
    SyncAPI(const ByteString& text) : m_text(text) {}
    virtual ~SyncAPI() override {}
    static OwnPtr<SyncAPI> decode(...);
    virtual IPC::MessageBuffer encode(...) const override;
};
```

Then, you need to inherit your connection class from `IPC::ConnectionFromClient` with created server and client
endpoints as template parameters if it is a server connection. Otherwise, your class need to be inherited
from `IPC::ConnectionToServer` with created server and client endpoints as template parameters and from the client
endpoint class.

Part of the connection implementations:

```cpp
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

Note, there are two types of functions for sending the messages: synchronous and asynchronous. The generated
asynchronous functions are prefixed with `async_` and the names of the synchronous functions are not changed.

## See also

-   [`Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp`](../../../../../Meta/Lagom/Tools/CodeGenerators/IPCCompiler/main.cpp)
-   [ipc(4)](help://man/4/ipc) (IPC Unix socket documentation)
