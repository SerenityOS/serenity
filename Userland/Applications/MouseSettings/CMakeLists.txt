set(SOURCES
    main.cpp
)

serenity_app(MouseSettings ICON app-mouse)
target_link_libraries(MouseSettings LibGUI)
