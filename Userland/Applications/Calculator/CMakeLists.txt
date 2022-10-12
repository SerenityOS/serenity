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
    RoundingDialog.cpp
    Keypad.cpp
)

set(GENERATED_SOURCES
    CalculatorGML.h
)

serenity_app(Calculator ICON app-calculator)
target_link_libraries(Calculator LibGUI LibMain)
