set(SOURCES
    AppFile.cpp
    Launcher.cpp
    Screensaver.cpp
)

set(GENERATED_SOURCES
    ../../Services/LaunchServer/LaunchClientEndpoint.h
    ../../Services/LaunchServer/LaunchServerEndpoint.h
)

serenity_lib(LibDesktop desktop)
target_link_libraries(LibDesktop PRIVATE LibCore LibIPC LibGfx LibGUI LibFileSystem LibURL)
