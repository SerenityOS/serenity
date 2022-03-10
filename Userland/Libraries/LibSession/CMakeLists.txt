set(SOURCES
    Session.cpp
)

set(GENERATED_SOURCES
    ../../Services/SessionServer/SessionExitInhibitionClientEndpoint.h
    ../../Services/SessionServer/SessionExitInhibitionServerEndpoint.h
)

serenity_lib(LibSession session)
target_link_libraries(LibSession LibCore LibIPC)
