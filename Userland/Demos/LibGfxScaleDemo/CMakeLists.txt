serenity_component(
    LibGfxScaleDemo
    TARGETS LibGfxScaleDemo
)

set(SOURCES
    main.cpp
)

serenity_app(LibGfxScaleDemo ICON app-libgfx-demo)
target_link_libraries(LibGfxScaleDemo PRIVATE LibGUI LibIPC LibCore LibMain)
