set(SOURCES
    main.cpp
)

serenity_app(Clock.MenuApplet)
target_link_libraries(Clock.MenuApplet LibGUI)
