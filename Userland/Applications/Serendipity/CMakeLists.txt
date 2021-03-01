compile_gml(SerendipityWindow.gml SerendipityWindowGML.h serendipity_window_gml)

set(SOURCES
    SerendipityWindowGML.h
    SerendipityWidget.cpp
    SerendipityWidget.h
    main.cpp
)

serenity_app(Serendipity ICON app-serendipity)
target_link_libraries(Serendipity LibGUI LibWeb)
