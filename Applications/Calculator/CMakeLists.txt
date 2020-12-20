set(SOURCES
    main.cpp
    Calculator.cpp
    CalculatorWidget.cpp
    Keypad.cpp
)

serenity_app(Calculator ICON app-calculator)
target_link_libraries(Calculator LibGUI)
