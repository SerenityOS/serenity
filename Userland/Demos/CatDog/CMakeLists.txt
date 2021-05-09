set(SOURCES
    CatDog.cpp
    main.cpp
)

serenity_app(CatDog ICON app-catdog)
target_link_libraries(CatDog LibGUI LibGfx)
