set(SOURCES
    Client.cpp
    Download.cpp
)

serenity_lib(LibProtocol protocol)
add_dependencies(LibProtocol ProtocolServer)
target_link_libraries(LibProtocol LibIPC LibCore)
target_include_directories(LibProtocol PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/../../Servers)
