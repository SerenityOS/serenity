#
# %i%
#

# The test cases in this file assume the first day of week is Sunday
# and the minimal days in the first week is 1.

locale ja JP JP
new instance jcal

timezone Asia/Tokyo
new instance tokyocal

set non-lenient

test add ERA
    use jcal
	clear all
	set date Reiwa 17 Mar 8
	add era 10
	# as of Reiwa 17 March 8
	check era Reiwa
	add era -100
	check era BeforeMeiji

test add HOUR_OF_DAY
    use jcal
	clear all
	set era Heisei
	set datetime 1 Jan 8 23 59 59
	add hour_of_day 1
	check datetime 1 Jan 9 0 59 59
	check ampm AM
	check hour 0
	add hour_of_day -1
	check datetime 1 Jan 8 23 59 59
	add hour_of_day 24
	check datetime 1 Jan 9 23 59 59
	add hour_of_day -24
	check datetime 1 Jan 8 23 59 59

test add HOUR
    use jcal
	clear all
	set era Showa
	set datetime 64 Jan 7 11 59 59
	check era Showa
	check hour 11
	check ampm AM
	add hour 1
	check hour 0
	check ampm PM
	check datetime 64 Jan 7  12 59 59
	add hour -1
	check datetime 64 Jan 7 11 59 59
	add hour 240
	check era Heisei
	check datetime 1 Jan 17 11 59 59
	add hour -240
	check era Showa
	check datetime 64 Jan 7 11 59 59

	clear all
	set era Showa
	set datetime 64 Jan 7 23 59 59
	check era Showa
	check hour 11
	check ampm PM
	add hour 1
	check hour 0
	check ampm AM
	check era Heisei
	check datetime 1 Jan 8 0 59 59
	add hour -1
	check datetime 64 Jan 7 23 59 59
	add hour 240
	check era Heisei
	check datetime 1 Jan 17 23 59 59
	add hour -240
	check era Showa
	check datetime 64 Jan 7 23 59 59

	clear all
	set era Heisei
	set datetime 1 Jan 8 23 59 59
	check date Heisei 1 Jan 8
	check hour 11
	check ampm PM
	add hour 1
	check hour 0
	check ampm AM
	check era Heisei
	check datetime 1 Jan 9  0 59 59
	add hour -1
	check datetime 1 Jan 8 23 59 59
	add hour 240
	check datetime 1 Jan 18 23 59 59
	add hour -240
	check datetime 1 Jan 8 23 59 59

test add YEAR
    use jcal
	clear all
	# check if pinDayOfMonth works correctly.
	# Heisei 12 (Y2K) is a leap year.
	set date Heisei 12 Feb 29
	add year 5
	check date Heisei 17 Feb 28
	add year -5
	check date Heisei 12 Feb 28 # not 29!

	clear all
	set date BeforeMeiji 1867 Jan 1
	add year 1
	check date Meiji 1 Jan 1
	add year -1
	check date BeforeMeiji 1867 Jan 1

	clear all
	set date Meiji 45 Jul 29
	add year 1
	check date Taisho 2 Jul 29
	add year -1
	check date Meiji 45 Jul 29

	clear all
	set date Meiji 44 Jul 30
	add year 1
	check date Taisho 1 Jul 30
	add year -1
	check date Meiji 44 Jul 30

	clear all
	set date Taisho 15 Aug 1
	add year 1
	check date Showa 2 Aug 1
	add year -1
	check date Taisho 15 Aug 1

	clear all
	set date Taisho 14 Dec 31
	add year 1
	check date Showa 1 Dec 31
	add year -1
	check date Taisho 14 Dec 31

	clear all
	set date Showa 63 Feb 1
	add year 1
	check date Heisei 1 Feb 1
	add year -1
	check date Showa 63 Feb 1

	set date Showa 63 Dec 30
	add year 1
	check date Heisei 1 Dec 30
	add year -1
	check date Showa 63 Dec 30

	set date Showa 64 Jan 7
	add year 1
	check date Heisei 2 Jan 7
	add year -1
	check date Showa 64 Jan 7

	set date Heisei 2 Jan 7
	add year -1
	check date Showa 64 Jan 7
	add year 1
	check date Heisei 2 Jan 7

test add MONTH
	clear all
	# Check pinDayOfMonth works correctly.
	# Heisei 12 is a leap year.
	set date Heisei 12 Jan 31
	add month 1
	check date Heisei 12 Feb 29
	add month -1
	check date Heisei 12 Jan 29

	# Another leap year
	set date Showa 63 Jan 31
	add month 1
	check date Showa 63 Feb 29
	add month -1
	check date Showa 63 Jan 29

	# Non leap year
	set date Heisei 15 Jan 31
	add month 1
	check date Heisei 15 Feb 28
	add month -1
	check date Heisei 15 Jan 28

	set date Heisei 15 Mar 31
	add month 1
	check date Heisei 15 Apr 30
	add month -1
	check date Heisei 15 Mar 30

	set date Heisei 15 May 31
	add month 1
	check date Heisei 15 Jun 30
	add month -1
	check date Heisei 15 May 30

	set date Heisei 15 Aug 31
	add month 1
	check date Heisei 15 Sep 30
	add month -1
	check date Heisei 15 Aug 30

	set date Heisei 15 Oct 31
	add month 1
	check date Heisei 15 Nov 30
	add month -1
	check date Heisei 15 Oct 30

	set date Heisei 15 Dec 31
	add month -1
	check date Heisei 15 Nov 30
	add month 1
	check date Heisei 15 Dec 30

	set date Heisei 15 Dec 31
	add month 2
	check date Heisei 16 Feb 29
	add month -1
	check date Heisei 16 Jan 29

	# end of pinDayOfMonth tests

	set date BeforeMeiji 1867 Dec 1
	add month 1
	check date Meiji 1 Jan 1
	add month -1
	check date BeforeMeiji 1867 Dec 1
	add month 14
	check date Meiji 2 Feb 1
	add month -14
	check date BeforeMeiji 1867 Dec 1

	set date Meiji 1 Dec 1
	add month 1
	check date Meiji 2 Jan 1
	add month -1
	check date Meiji 1 Dec 1
	add month 13
	check date Meiji 3 Jan 1
	add month -13
	check date Meiji 1 Dec 1

	set date Meiji 45 Jun 30
	add month 1
	check date Taisho 1 Jul 30
	add month -1
	check date Meiji 45 Jun 30

	set date Meiji 45 Jun 30
	add month 14
	check date Taisho 2 Aug 30
	add month -14
	check date Meiji 45 Jun 30

	# Taisho Gan-nen (year 1) has only 6 months.
	set date Taisho 1 Jul 30
	add month -1
	check date Meiji 45 Jun 30
	add month 1
	check date Taisho 1 Jul 30
	add month -18
	check date Meiji 44 Jan 30
	add month 18
	check date Taisho 1 Jul 30

	set date Taisho 15 Jan 20
	add month 11
	check date Taisho 15 Dec 20

	set date Taisho 15 Jan 25
	add month 11
	check date Showa 1 Dec 25

	set date Showa 1 Dec 25
	add month 1
	check date Showa 2 Jan 25
	add month -1
	check date Showa 1 Dec 25
	add month 17
	check date Showa 3 May 25
	add month -17
	check date Showa 1 Dec 25

	set date Showa 64 Jan 7
	add month 1
	check date Heisei 1 Feb 7

	set date Heisei 1 Feb 1
	add month -1
	# Heisei starts from Jan 8.
	check date Showa 64 Jan 1
	add month 1
	check date Heisei 1 Feb 1

	set date Heisei 1 Feb 8
	add month -1
	check date Heisei 1 Jan 8

	set date Heisei 1 Dec 1
	add month 1
	check date Heisei 2 Jan 1
	add month -1
	check date Heisei 1 Dec 1

	set date Heisei 1 Dec 8
	add month 1
	check date Heisei 2 Jan 8
	add month -1
	check date Heisei 1 Dec 8

    # time zone dependent tests
    use tokyocal
	clear all

	set date BeforeMeiji 1 Jan 1
	get min year
	assign $result $minyear
	# actual min date: -292275055.05.17T01:47:04.192+0900

	set date BeforeMeiji $minyear Dec 17
	set timeofday 1 47 4 192
	add month -7
	check date BeforeMeiji $minyear May 17
	check timeofday 1 47 4 192
	add month 7
	check date BeforeMeiji $minyear Dec 17
	check timeofday 1 47 4 192
	set date BeforeMeiji $minyear Dec 17
	set timeofday 1 47 4 191
	add month -7
	check date BeforeMeiji $minyear May 18
	check timeofday 1 47 4 191

	set date Reiwa 17 Jan 1
	get max year
	assign $result $max
	set date Reiwa $max Jul 17
	add month 1
	check date Reiwa $max Aug 17
#	set date Heisei $max Jul 28
#	set timeofday 23 59 59 999
#	add month 1
#	check date Heisei $max Aug 16
#	check timeofday 23 59 59 999

test add WEEK_OF_YEAR
    use jcal
	clear all
	# 1867 Dec 23 is Monday.
	set date BeforeMeiji 1867 Dec 23
	add week_of_year 2
	check day_of_week Mon
	check date Meiji 1 Jan 6
	add week_of_year -2
	check day_of_week Mon
	check date BeforeMeiji 1867 Dec 23

	# 1867 Dec 23 is Wednesday.
	set date Meiji 1 Dec 23
	add week_of_year 2
	check day_of_week Wed
	check date Meiji 2 Jan 6
	add week_of_year -2
	check day_of_week Wed
	check date Meiji 1 Dec 23

	# Meiji 45 July 23 is Tuesday.
	set date Meiji 45 Jul 23
	add week_of_year 1
	check day_of_week Tue
	check date Taisho 1 Jul 30
	add week_of_year -1
	check day_of_week Tue
	check date Meiji 45 Jul 23

	# Taisho 15 December 23 is Thursday.
	set date Taisho 15 Dec 23
	add week_of_year 1
	check day_of_week Thu
	check date Showa 1 Dec 30
	add week_of_year -1
	check day_of_week Thu
	check date Taisho 15 Dec 23

	# Showa Gan-nen December 30 is Thursday.  Showa Gan-nen has
	# only one week. Rolling any number of weeks brings to the
	# same date.
	set date Showa 1 Dec 30
	add week_of_year 1
	check day_of_week Thu
	check date Showa 2 Jan 6
	add week_of_year -1
	check day_of_week Thu
	check date Showa 1 Dec 30

	# Showa 64 January 7 is Saturday. The year has only one week.
	set date Showa 64 Jan 7
	add week_of_year 1
	check day_of_week Sat
	check date Heisei 1 Jan 14
	add week_of_year -1
	check day_of_week Sat
	check date Showa 64 Jan 7

    use tokyocal
	clear all

	set date BeforeMeiji $minyear Dec 25
	check day_of_week Sat
	eval $minyear + 1
	assign $result $minyear_plus_1
	add week_of_year 1
	check day_of_week Sat
	check date BeforeMeiji $minyear_plus_1 Jan 1
	add week_of_year -1
	check day_of_week Sat
	check date BeforeMeiji $minyear Dec 25

test WEEK_OF_MONTH
    use jcal
	clear all

test DAY_OF_MONTH
    use jcal
	clear all

test DAY_OF_YEAR
    use jcal
	clear all

	# 1867 is a regular Gregorian year.
	set date BeforeMeiji 1867 Dec 31
	add day_of_year 1
	check date Meiji 1 Jan 1
	add day_of_year -1
	check date BeforeMeiji 1867 Dec 31
	add day_of_year 26
	check date Meiji 1 Jan 26
	add day_of_year -26
	check date BeforeMeiji 1867 Dec 31

	# Meiji 1 starts from Jan 1. It's a regular year as well.
	set date Meiji 1 Dec 31
	add day_of_year 1
	check date Meiji 2 Jan 1
	add day_of_year -1
	check date Meiji 1 Dec 31
	add day_of_year 26
	check date Meiji 2 Jan 26
	add day_of_year -26
	check date Meiji 1 Dec 31

	# The last year of Meiji (45) has an irregularity. Meiji 45
	# July 30 is actually Taisho 1 July 30.
	set date Meiji 45 Jul 29
	add day_of_year 1
	check date Taisho 1 Jul 30
	add day_of_year -1
	check date Meiji 45 Jul 29

	# The first day of Taisho, July 30.
	set date Taisho 1 Jul 30
	add day_of_year -1
	check date Meiji 45 Jul 29
	add day_of_year 1
	check date Taisho 1 Jul 30

	set date Taisho 15 Dec 24
	add day_of_year 1
	check date Showa 1 Dec 25
	add day_of_year -1
	check date Taisho 15 Dec 24

	set date Showa 1 Dec 31
	add day_of_year 1
	check date Showa 2 Jan 1
	add day_of_year -1
	check date Showa 1 Dec 31
	add day_of_year 25
	check date Showa 2 Jan 25
	add day_of_year -25
	check date Showa 1 Dec 31

	set date Showa 64 Jan 7
	add day_of_year 1
	check date Heisei 1 Jan 8
	add day_of_year -1
	check date Showa 64 Jan 7

	set date Heisei 1 Dec 31
	add day_of_year 5
	check date Heisei 2 Jan 5
	add day_of_year -5
	check date Heisei 1 Dec 31

    use tokyocal
	clear all

	set date BeforeMeiji $minyear Dec 31
	set timeofday 1 47 4 192
	add day_of_year 1
	check date BeforeMeiji $minyear_plus_1 Jan 1
	check timeofday 1 47 4 192
	add day_of_year -1
	check date BeforeMeiji $minyear Dec 31
	check timeofday 1 47 4 192

test DAY_OF_WEEK_IN_MONTH
    use jcal
	clear all
