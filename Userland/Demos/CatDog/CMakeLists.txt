serenity_component(
    CatDog
    RECOMMENDED
    TARGETS CatDog
)

set(SOURCES
    CatDog.cpp
    SpeechBubble.cpp
    main.cpp
)

serenity_app(CatDog ICON app-catdog)
target_link_libraries(CatDog PRIVATE LibGUI LibGfx LibCore LibMain)
