set(SOURCES
        main.cpp
        Calculator.cpp
        CalculatorWidget.cpp
        Token.cpp
        TokenStream.cpp
        )

serenity_bin(Calculator)
target_link_libraries(Calculator LibGUI)
