set(SOURCES
        Connection.cpp
        Device.cpp
        EventMetadata.cpp
        Message.cpp
        Room.cpp
        StateEvent.cpp
        )

serenity_lib(LibMatrix matrix)
target_link_libraries(LibMatrix LibCore LibProtocol)
