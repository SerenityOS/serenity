serenity_component(
    SereniTrivia
    RECOMMENDED
    TARGETS SereniTrivia
)

compile_gml(TriviaWindow.gml TriviaWindowGML.h trivia_window_gml)

set(SOURCES
    TriviaWindowGML.h
    Entry.cpp
    Entry.h
    TriviaWidget.cpp
    TriviaWidget.h
    main.cpp
)

serenity_app(SereniTrivia ICON app-serenitrivia)
target_link_libraries(SereniTrivia LibGUI LibConfig)
