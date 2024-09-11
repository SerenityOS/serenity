## Name

UI Dimensions

# Description

UI Dimension (or [GUI::UIDimension](file:///usr/src/serenity/Userland/Libraries/LibGUI/UIDimensions.h) in c++) is a special, union — of positive integer and enum — like, type that is used to represent dimensions in a user interface context.

It can either store positive integers ≥0 or special meaning values from a pre determined set.

## Basic Syntax

In GML UI Dimensions that are "regular" values (integer ≥0) are represented by JSON's int type,
while "special" values are represented by their name as a JSON string type.

# Special Values

Special Values carry size information that would otherwise not be intuitively possible to be transported by an integer (positive or negative) alone.

Importantly, while any "regular" (i.e. int ≥0) UI Dimension values might (by convention) be assigned to any UI Dimension property, many properties only allow a subset of the "special" values to be assigned to them.

| Name              | c++ name                                   | GML/JSON representation | General meaning                                                                                                                                                       |
| ----------------- | ------------------------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Regular           | `GUI::SpecialDimension::Regular` (mock)    | int ≥0                  | This is a regular integer value specifying a specific size                                                                                                            |
| Grow              | `GUI::SpecialDimension::Grow`              | `"grow"`                | Grow to the maximum size the surrounding allows                                                                                                                       |
| OpportunisticGrow | `GUI::SpecialDimension::OpportunisticGrow` | `"opportunistic_grow"`  | Grow when the opportunity arises, meaning — only when all other widgets have already grown to their maximum size, and only opportunistically growing widgets are left |
| Fit               | `GUI::SpecialDimension::Fit`               | `"fit"`                 | Grow exactly to the size of the surrounding as determined by other factors, but do not call for e.g. expansion of the parent container itself                         |
| Shrink            | `GUI::SpecialDimension::Shrink`            | `"shrink"`              | Shrink to the smallest size possible                                                                                                                                  |
