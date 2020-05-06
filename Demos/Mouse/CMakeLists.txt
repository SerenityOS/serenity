set(SOURCES
    main.cpp
)

serenity_bin(Mouse)
target_link_libraries(Mouse LibGUI LibGfx)
