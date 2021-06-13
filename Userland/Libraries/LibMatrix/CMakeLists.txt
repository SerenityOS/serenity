set(SOURCES
        Connection.cpp
        Device.cpp
        )

serenity_lib(LibMatrix matrix)
target_link_libraries(LibMatrix LibCore LibProtocol)
