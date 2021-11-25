serenity_component(
    LibGfxDemo
    TARGETS LibGfxDemo
)

set(SOURCES
    main.cpp
)

serenity_app(LibGfxDemo ICON app-libgfx-demo)
target_link_libraries(LibGfxDemo LibGUI LibIPC LibGfx LibCore LibMain)
