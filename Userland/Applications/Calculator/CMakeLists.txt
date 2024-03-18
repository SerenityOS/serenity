serenity_component(
    Calculator
    RECOMMENDED
    TARGETS Calculator
)

compile_gml(CalculatorWindow.gml CalculatorGML.cpp)

set(SOURCES
    main.cpp
    Calculator.cpp
    CalculatorGML.cpp
    CalculatorWidget.cpp
    Keypad.cpp
)

serenity_app(Calculator ICON app-calculator)
target_link_libraries(Calculator PRIVATE LibCore LibCrypto LibDesktop LibGfx LibGUI LibMain LibURL)
