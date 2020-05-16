set(SOURCES
    main.cpp
)

serenity_bin(LibGfxDemo)
target_link_libraries(LibGfxDemo LibGUI LibIPC LibGfx LibCore)
