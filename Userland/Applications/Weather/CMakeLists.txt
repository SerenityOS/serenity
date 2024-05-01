serenity_component(
    Weather
    TARGETS Weather
)

compile_gml(WeatherViewWidget.gml WeatherViewWidgetGML.cpp)
compile_gml(WeatherViewSearchResultView.gml WeatherViewSearchResultViewGML.cpp)

set(SOURCES
    main.jakt
    ${CMAKE_CURRENT_BINARY_DIR}/WeatherViewWidgetGML.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/WeatherViewSearchResultViewGML.cpp
)

serenity_bin(Weather) # TODO: set the correct icon
target_link_libraries(Weather PRIVATE LibConfig LibCore LibGfx LibGUI LibProtocol LibURL)
# FIXME: These should be automatic
add_dependencies(Weather
    generate_WeatherViewWidgetGML.cpp
    generate_WeatherViewSearchResultViewGML.cpp
)
