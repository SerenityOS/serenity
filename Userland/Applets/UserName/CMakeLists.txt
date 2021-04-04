set(SOURCES
    main.cpp
)

serenity_bin(UserName.Applet)
target_link_libraries(UserName.Applet LibGUI LibGfx)
