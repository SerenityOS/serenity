serenity_component(
    Calculator
    RECOMMENDED
    TARGETS Calculator
)

compile_gml(CalculatorWindow.gml CalculatorGML.h calculator_gml)
set(SOURCES
    main.cpp
    Calculator.cpp
    CalculatorWidget.cpp
    Keypad.cpp
    CalculatorGML.h
)

serenity_app(Calculator ICON app-calculator)
target_link_libraries(Calculator LibGUI LibMain)
