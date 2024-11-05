## Name

cal - Display a calendar

## Synopsis

```**sh
$ cal [--starting-day weekday] [--three-month-view] [--year] [[month] year]
```

## Description

This program displays a simple calendar. If no arguments are specified, the current month is displayed with the current day highlighted.
An overview of a whole year is displayed when a `year` is passed without a `month`.

The current day is always highlighted.

Months and years are specified with numbers. Weeks start at what's configured in the Calendar system settings,
unless the `--starting-day` option is passed.

Days, months and years are specified with numbers. Week starts at Sunday.

## Options

-   `-s`, `--starting-day`: Specify which day should start the week. Accepts either short or long weekday names or indexes (0 being Sunday).
-   `-3`, `--three-month-view`: Display the previous, current, and next months side-by-side.
-   `-y`, `--year`: Display an entire year by laying out months on a grid. If no year number is specified, the current year is used as a default.

## Examples

```sh
# Display the current month
$ cal
    March - 2023
Su Mo Tu We Th Fr Sa
          1  2  3  4
 5  6  7  8  9 10 11
12 13 14 15 16 17 18
19 20 21 22 23 24 25
26 27 28 29 30 31

# Display any month
$ cal 1 1999
   January - 1999
Su Mo Tu We Th Fr Sa
                1  2
 3  4  5  6  7  8  9
10 11 12 13 14 15 16
17 18 19 20 21 22 23
24 25 26 27 28 29 30
31

# Display three months side-by-side
$ cal -3 3 2023
  February - 2023         March - 2023          April - 2023
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
          1  2  3  4            1  2  3  4                     1
 5  6  7  8  9 10 11   5  6  7  8  9 10 11   2  3  4  5  6  7  8
12 13 14 15 16 17 18  12 13 14 15 16 17 18   9 10 11 12 13 14 15
19 20 21 22 23 24 25  19 20 21 22 23 24 25  16 17 18 19 20 21 22
26 27 28              26 27 28 29 30 31     23 24 25 26 27 28 29

# Display an entire year
$ cal 2023
                           Year 2023


      January               February               March
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 1  2  3  4  5  6  7            1  2  3  4            1  2  3  4
 8  9 10 11 12 13 14   5  6  7  8  9 10 11   5  6  7  8  9 10 11
15 16 17 18 19 20 21  12 13 14 15 16 17 18  12 13 14 15 16 17 18
22 23 24 25 26 27 28  19 20 21 22 23 24 25  19 20 21 22 23 24 25
29 30 31              26 27 28              26 27 28 29 30 31


       April                  May                   June
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
                   1      1  2  3  4  5  6               1  2  3
 2  3  4  5  6  7  8   7  8  9 10 11 12 13   4  5  6  7  8  9 10
 9 10 11 12 13 14 15  14 15 16 17 18 19 20  11 12 13 14 15 16 17
16 17 18 19 20 21 22  21 22 23 24 25 26 27  18 19 20 21 22 23 24
23 24 25 26 27 28 29  28 29 30 31           25 26 27 28 29 30
30


        July                 August              September
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
                   1         1  2  3  4  5                  1  2
 2  3  4  5  6  7  8   6  7  8  9 10 11 12   3  4  5  6  7  8  9
 9 10 11 12 13 14 15  13 14 15 16 17 18 19  10 11 12 13 14 15 16
16 17 18 19 20 21 22  20 21 22 23 24 25 26  17 18 19 20 21 22 23
23 24 25 26 27 28 29  27 28 29 30 31        24 25 26 27 28 29 30
30 31


      October               November              December
Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 1  2  3  4  5  6  7            1  2  3  4                  1  2
 8  9 10 11 12 13 14   5  6  7  8  9 10 11   3  4  5  6  7  8  9
15 16 17 18 19 20 21  12 13 14 15 16 17 18  10 11 12 13 14 15 16
22 23 24 25 26 27 28  19 20 21 22 23 24 25  17 18 19 20 21 22 23
29 30 31              26 27 28 29 30        24 25 26 27 28 29 30

```
