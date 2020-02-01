set(SOURCES
    main.cpp
)

serenity_app(CPUGraph.MenuApplet)
target_link_libraries(CPUGraph.MenuApplet LibGUI)
