serenity_component(
    Weather
    TARGETS Weather
)

compile_gml(WeatherViewWidget.gml WeatherViewWidgetGML.cpp)
compile_gml(WeatherViewSearchResultView.gml WeatherViewSearchResultViewGML.cpp)
compile_gml(WeatherViewIndividualEntryWidget.gml WeatherViewIndividualEntryWidgetGML.cpp)

set(SOURCES
    main.jakt
    ${CMAKE_CURRENT_BINARY_DIR}/WeatherViewWidgetGML.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/WeatherViewSearchResultViewGML.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/WeatherViewIndividualEntryWidgetGML.cpp
)

serenity_app(Weather ICON app-weather)
target_link_libraries(Weather PRIVATE LibConfig LibCore LibGfx LibGUI LibProtocol LibURL)

# FIXME: These should be automatic
add_dependencies(Weather
    generate_WeatherViewWidgetGML.cpp
    generate_WeatherViewSearchResultViewGML.cpp
    generate_WeatherViewIndividualEntryWidgetGML.cpp
)
