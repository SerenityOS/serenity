set(SOURCES
    main.cpp
)

serenity_app(CatDog ICON app-catdog)
target_link_libraries(CatDog LibGUI LibGfx)
