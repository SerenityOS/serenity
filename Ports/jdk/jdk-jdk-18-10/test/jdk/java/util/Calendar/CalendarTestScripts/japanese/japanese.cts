#
#
#

locale ja JP JP
new instance jcal
new gregorian gcal

test Default dates
    # Default for all unset fields
    # 1970-01-01T00:00:00.000 local time (Gregorian)
    # which is equivalent to Showa 45.
    use gcal
        clear all
	get millis
	# get the default milliseconds from the Epoch. It's time zone
	# dependent.
	assign $result $defmillis
    use jcal
	clear all
	get millis
	eval $result == $defmillis
	check era Showa
	check datetime 45 Jan 1 0 0 0
	check millisecond 0

    # If each era is set, then January 1 of each Gan-nen is the
    # default.
	clear all
	set era BeforeMeiji
	check era BeforeMeiji
	check datetime 1 Jan 1 0 0 0
	check millisecond 0

	clear all
	set era Meiji
	check era Meiji
	check datetime 1 Jan 1 0 0 0
	check millisecond 0

	clear all
	set era Taisho
	check era Meiji
	check datetime 45 Jan 1 0 0 0
	check millisecond 0

	clear all
	set era Showa
	check era Taisho
	check datetime 15 Jan 1 0 0 0
	check millisecond 0

	clear all
	set era Heisei
	check era Showa
	check datetime 64 Jan 1 0 0 0
	check millisecond 0

	clear all
	set era Reiwa
	check era Heisei
	check datetime 31 Jan 1 0 0 0
	check millisecond 0

#
# Field resolution tests
#
	clear all
	get firstdayofweek
	# The test cases below assume that the first day of week is
	# Sunday. So we should make sure it is.
	eval $result == Sun
	assign $result $fdow

test Field resolution: YEAR + MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
	set era Showa
	set year 64
	set week_of_month 1
	check day_of_week $fdow
	check date 64 Jan 1

	clear all
	set era Showa
	set year 64
	set week_of_month 1
	set day_of_week Thu
	check era Showa
	check day_of_week Thu
	check date 64 Jan 5

	clear all
	# Heise 1 January and Showa 64 January are the same month. Its
	# first week should be the same week. (January is the default
	# month.)
	set era Heisei
	set year 1
	set week_of_month 1
	check day_of_week $fdow
	check era Showa
	check date 64 Jan 1

	# Test aggregation
	clear all
	set date Heisei 17 Mar 16
	set week_of_month 1
	set day_of_week Tue
	check date Heisei 17 Mar 1

	clear all
	set week_of_month 1
	set date Heisei 17 Mar 16
	set day_of_week Tue
	check date Heisei 17 Mar 1

	clear all
	set day_of_week Tue
	set date Heisei 17 Mar 16
	set week_of_month 1
	check date Heisei 17 Mar 1

	clear all
	set day_of_week Tue
	set date Heisei 17 Mar 16
	set week_of_year 10
	set week_of_month 1
	check date Heisei 17 Mar 1

	clear all
	set day_of_week Tue
	set date Heisei 17 Mar 16
	set day_of_year 300
	set week_of_month 1
	check date Heisei 17 Mar 1

test Field resolution: YEAR + MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
	clear all
	set era Meiji
	set year 45
 	set month Jul
	set day_of_week_in_month 5
	set day_of_week Mon
	check date Meiji 45 Jul 29

	clear all
	set era Meiji
	set year 45
 	set month Jul
	set day_of_week_in_month 4
	check date Meiji 45 Jul 28

	clear all
	set era Meiji
	set year 45
 	set month Jul
	set day_of_week_in_month 5
	set day_of_week Tue
	check date Taisho 1 Jul 30

	clear all
	set era Taisho
	set year 1
 	set month Jul
	set day_of_week_in_month 1
	set day_of_week Tue
	check date Meiji 45 Jul 2

	# Test aggregation
	clear all
	set date Heisei 17 Mar 16
	set day_of_week_in_month 1
	set day_of_week Wed
	check date Heisei 17 Mar 2

	clear all
	set day_of_week_in_month 1
	set date Heisei 17 Mar 16
	set day_of_week Wed
	check date Heisei 17 Mar 2

	clear all
	set day_of_week Wed
	set date Heisei 17 Mar 16
	set day_of_week_in_month 1
	check date Heisei 17 Mar 2

	clear all
	set day_of_week Wed
	set date Heisei 17 Mar 16
	set week_of_month 4
	set day_of_week_in_month 1
	check date Heisei 17 Mar 2

	clear all
	set day_of_week Wed
	set date Heisei 17 Mar 16
	set day_of_year 365
	set day_of_week_in_month 1
	check date Heisei 17 Mar 2

	clear all
	set day_of_week Wed
	set date Heisei 17 Mar 16
	set week_of_year 50
	set day_of_week_in_month 1
	check date Heisei 17 Mar 2

test Field resolution: YEAR + DAY_OF_YEAR
	clear all
	set era Showa
	set year 64
	set day_of_year 7
	check date Showa 64 Jan 7

	clear all
	set era Showa
	set year 64
	set day_of_year 10
	check date Heisei 1 Jan 10

	clear all
	set era Showa
	set year 64
	check date Showa 64 Jan 1
	check day_of_year 1

	clear all
	set era Heisei
	set year 1
	set day_of_year 10
	check date Heisei 1 Jan 17

	clear all
	set era Heisei
	set year 1
	set day_of_year 1
	check date Heisei 1 Jan 8

	clear all
	set era Heisei
	set year 1
	set day_of_year -1
	check date Showa 64 Jan 6

	clear all
	set date Heisei 17 Mar 16
	set day_of_year 31
	check date Heisei 17 Jan 31

	clear all
	set date Heisei 17 Mar 16
	set week_of_year 50
	set day_of_week Wed
	set day_of_year 31
	check date Heisei 17 Jan 31

	clear all
	set date Heisei 17 Mar 16
	set week_of_month 5
	set day_of_week Wed
	set day_of_year 31
	check date Heisei 17 Jan 31

test Field resolution: YEAR + DAY_OF_WEEK + WEEK_OF_YEAR
	clear all
	set era Showa
	set year 64
	set week_of_year 1
	check day_of_week $fdow
	check date 64 Jan 1

	clear all
	set era Showa
	set year 64
	set week_of_year 1
	set day_of_week Wed
	check date Showa 64 Jan 4

	clear all
	set era Heisei
	set year 1
	set week_of_year 1
	check day_of_week $fdow
	check date 1 Jan 8

	clear all
	set date Heisei 17 Mar 16
	set week_of_year 2
	set day_of_week Thu
	check date Heisei 17 Jan 6

	clear all
	set week_of_year 2
	set date Heisei 17 Mar 16
	set day_of_week Thu
	check date Heisei 17 Jan 6

	clear all
	set day_of_week Thu
	set date Heisei 17 Mar 16
	set week_of_year 2
	check date Heisei 17 Jan 6

test zone offsets
    # Tests here depend on the GMT offset.
    timezone GMT+09:00
    new instance cal0900
    use cal0900
	clear all
	set date Heisei 17 Mar 12
	get millis
	assign $result $H17Mar12
	clear all
	set date Heisei 17 Mar 12
	set zone_offset 0
	get millis
	eval $result - 32400000 # -9 hours
	eval $result == $H17Mar12

	clear all
	set date Heisei 17 Mar 12
	set zone_offset 28800000 # 8 hours
	set dst_offset 3600000 # 1 hour
	get millis
	eval $result == $H17Mar12

	clear all
	set date Heisei 17 Mar 12
	set zone_offset 18000000 # 5 hours
	set dst_offset 14400000 # 4 hours
	get millis
	eval $result == $H17Mar12
