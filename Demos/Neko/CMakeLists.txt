set(SOURCES
    main.cpp
)

serenity_bin(Neko ICON app-neko)
target_link_libraries(Neko LibGUI LibGfx)
