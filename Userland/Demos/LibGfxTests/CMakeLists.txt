serenity_component(
    LibGfxTests
    TARGETS LibGfxTests
)

set(SOURCES
    main.cpp
)

serenity_app(LibGfxTests ICON app-libgfx-demo)
target_link_libraries(LibGfxTests LibGUI LibIPC LibGfx LibCore LibMain)
