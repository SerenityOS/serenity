set(SOURCES
    Launcher.cpp
    App.cpp
)

set(GENERATED_SOURCES
    ../../Services/LaunchServer/LaunchClientEndpoint.h
    ../../Services/LaunchServer/LaunchServerEndpoint.h
)

serenity_lib(LibDesktop desktop)
target_link_libraries(LibDesktop LibIPC)
