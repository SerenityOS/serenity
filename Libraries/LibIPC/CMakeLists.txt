set(SOURCES
    IEndpoint.cpp
    IMessage.cpp
)

serenity_lib(LibIPC ipc)
target_link_libraries(LibIPC LibCore)
