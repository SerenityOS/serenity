serenity_component(
    Dictionary
    RECOMMENDED
    TARGETS Dictionary
)

compile_gml(DictionaryWindow.gml DictionaryWindowGML.h dictionary_window_gml)

set(SOURCES
    main.cpp
    MainWidget.cpp
    DictionaryWindowGML.h
    DictionaryModel.cpp
    Dictionary.cpp
)

serenity_app(Dictionary ICON app-dictionary)
target_link_libraries(Dictionary LibGUI LibDesktop)
