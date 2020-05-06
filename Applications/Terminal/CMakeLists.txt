set(SOURCES
    main.cpp
)

serenity_bin(Terminal)
target_link_libraries(Terminal LibGUI LibVT)
