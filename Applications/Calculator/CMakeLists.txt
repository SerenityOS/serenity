set(SOURCES
    main.cpp
    Calculator.cpp
    CalculatorWidget.cpp
    Keypad.cpp
)

serenity_bin(Calculator)
target_link_libraries(Calculator LibGUI)
