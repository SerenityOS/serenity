serenity_testjs_test(test-spreadsheet.cpp test-spreadsheet)
install(TARGETS test-spreadsheet RUNTIME DESTINATION bin OPTIONAL)
link_with_unicode_data(test-spreadsheet)
