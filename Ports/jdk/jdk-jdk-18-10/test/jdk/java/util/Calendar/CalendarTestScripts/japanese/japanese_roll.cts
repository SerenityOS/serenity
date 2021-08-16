#
#
#

# The test cases in this file assume that the first day of week is Sunday
# and the minimal days in the first week is 1.

locale ja JP JP
new instance jcal

timezone Asia/Tokyo
new instance tokyocal

test roll HOUR_OF_DAY
    use jcal
	clear all
	set era Heisei
	set datetime 1 Jan 8 23 59 59
	roll hour_of_day 1
	check datetime 1 Jan 8 0 59 59
	check ampm AM
	check hour 0
	roll hour_of_day -1
	check datetime 1 Jan 8 23 59 59
	roll hour_of_day 240
	check datetime 1 Jan 8 23 59 59
	roll hour_of_day -240
	check datetime 1 Jan 8 23 59 59

test roll HOUR
    use jcal
	clear all
	set era Showa
	set datetime 64 Jan 7 11 59 59
	get ampm
	check era Showa
	check hour 11
	check ampm AM
	roll hour 1
	check hour 0
	check ampm AM
	check datetime 64 Jan 7  0 59 59
	roll hour -1
	check datetime 64 Jan 7 11 59 59
	roll hour 240
	check datetime 64 Jan 7 11 59 59
	roll hour -240
	check datetime 64 Jan 7 11 59 59

	clear all
	set era Showa
	set datetime 64 Jan 7 23 59 59
	get ampm
	check era Showa
	check hour 11
	check ampm PM
	roll hour 1
	check hour 0
	check ampm PM
	check datetime 64 Jan 7 12 59 59
	roll hour -1
	check datetime 64 Jan 7 23 59 59
	roll hour 240
	check datetime 64 Jan 7 23 59 59
	roll hour -240
	check datetime 64 Jan 7 23 59 59

	clear all
	set era Heisei
	set datetime 1 Jan 8 23 59 59
	get ampm
	check hour 11
	check ampm PM
	roll hour 1
	check hour 0
	check ampm PM
	check datetime 1 Jan 8 12 59 59
	roll hour -1
	check datetime 1 Jan 8 23 59 59
	roll hour 240
	check datetime 1 Jan 8 23 59 59
	roll hour -240
	check datetime 1 Jan 8 23 59 59

test roll YEAR
	clear all
	set date BeforeMeiji 1867 Jan 1
	get actualmin year
	# roll to the min year value of Gregorian (not Julian)
	roll year 1
	check date BeforeMeiji $result Jan 1
	roll year -1
	check date BeforeMeiji 1867 Jan 1

	clear all
	set date Meiji 45 Jul 29
	roll year 1
	check date Meiji 1 Jul 29
	roll year -1
	check date Meiji 45 Jul 29

	clear all
	set date Meiji 44 Jul 30
	roll year 1
	check date Meiji 1 Jul 30
	roll year -1
	check date Meiji 44 Jul 30

	clear all
	set date Taisho 15 Aug 1
	roll year 1
	check date Taisho 1 Aug 1
	roll year -1
	check date Taisho 15 Aug 1

	clear all
	set date Taisho 14 Dec 31
	roll year 1
	check date Taisho 1 Dec 31
	roll year -1
	check date Taisho 14 Dec 31

	clear all
	set date Showa 63 Feb 1
	# Neither 64 Feb 1 nor 1 Feb 1 exists in Showa.
	roll year 1
	check date Showa 2 Feb 1
	roll year -1
	check date Showa 63 Feb 1

	set date Showa 63 Dec 30
	roll year 1
	# Showa 1 Dec 30 exists.
	check date Showa 1 Dec 30
	roll year -1
	check date Showa 63 Dec 30

	set date Showa 64 Jan 7
	roll year 1
	check date Showa 2 Jan 7
	roll year -1
	check date Showa 64 Jan 7

	set date Heisei 31 Apr 30
	roll year 1
	check date Heisei 1 Apr 30
	roll year -1
	check date Heisei 31 Apr 30

	set date Reiwa 2 Apr 30
	get max year
	assign $result $hmax
	roll year -1
	check date Reiwa $hmax Apr 30
	roll year 1
	check date Reiwa 2 Apr 30

test roll MONTH
	set date BeforeMeiji 1867 Dec 1
	roll month 1
	check date BeforeMeiji 1867 Jan 1
	roll month -1
	check date BeforeMeiji 1867 Dec 1
	roll month 14
	check date BeforeMeiji 1867 Feb 1
	roll month -14
	check date BeforeMeiji 1867 Dec 1

	set date Meiji 1 Dec 1
	roll month 1
	check date Meiji 1 Jan 1
	roll month -1
	check date Meiji 1 Dec 1
	roll month 13
	check date Meiji 1 Jan 1
	roll month -13
	check date Meiji 1 Dec 1

	set date Meiji 45 Jun 30
	roll month 1
	# Meiji 45 Jun 30 is actually Taisho 1 Jun 30. By the rule of
	# roll() that year can't be changed, the day of month value
	# has to be changed ("pin date to month").
	check date Meiji 45 Jul 29
	roll month -1
	# doesn't roll back to Jun 30, but to Jun 29.
	check date Meiji 45 Jun 29

	set date Meiji 45 Jun 30
	# Meiji 45 (year) has only 7 months. rolling 14 months must
	# bring the given date to the same date.
	roll month 14
	check date Meiji 45 Jun 30
	roll month -14
	check date Meiji 45 Jun 30

	# Taisho Gan-nen (year 1) has only 6 months.
	set date Taisho 1 Jul 30
	roll month -1
	check date Taisho 1 Dec 30
	roll month 1
	check date Taisho 1 Jul 30
	roll month -18
	check date Taisho 1 Jul 30
	roll month 18
	check date Taisho 1 Jul 30

	set date Taisho 15 Jan 20
	roll month 11
	check date Taisho 15 Dec 20

	set date Taisho 15 Jan 25
	roll month 11
	# Taisho 15 Dec 25 is actually Showa 1 Dec 25. Day of month is
	# adjusted to the last day of month. ("pin date to month")
	check date Taisho 15 Dec 24

	set date Showa 1 Dec 25
	roll month 1
	check date Showa 1 Dec 25
	roll month -1
	check date Showa 1 Dec 25
	roll month 17
	check date Showa 1 Dec 25
	roll month -17
	check date Showa 1 Dec 25

	set date Showa 64 Jan 7
	roll month 1
	check date Showa 64 Jan 7

	set date Heisei 1 Feb 1
	roll month -1
	# Heisei starts from Jan 8.
	check date Heisei 1 Jan 8
	roll month 1
	check date Heisei 1 Feb 8

	set date Heisei 1 Feb 8
	roll month -1
	check date Heisei 1 Jan 8

	set date Heisei 1 Dec 1
	roll month 1
	check date Heisei 1 Jan 8
	roll month -1
	check date Heisei 1 Dec 8

	set date Heisei 1 Dec 8
	roll month 1
	check date Heisei 1 Jan 8
	roll month -1
	check date Heisei 1 Dec 8

    # time zone dependent tests
    use tokyocal
	clear all

	set date BeforeMeiji 1 Jan 1
	get min year
	assign $result $minyear
	# actual min date: -292275055.05.17T01:47:04.192+0900
	set date BeforeMeiji $minyear Dec 31
	roll month 1
	check date BeforeMeiji $minyear May 31

	set date BeforeMeiji $minyear Dec 1
	set timeofday 1 47 4 192
	roll month 1
	check date BeforeMeiji $minyear May 17
	check timeofday 1 47 4 192
	
	set date BeforeMeiji $minyear Dec 1
	set timeofday 1 47 4 191
	roll month 1
	check date BeforeMeiji $minyear May 18
	check timeofday 1 47 4 191

	set date Reiwa 17 Jan 1
	get max year
	assign $result $max
	set date Reiwa $max Jul 28
	roll month 1
	check date Reiwa $max Aug 17
	set date Reiwa $max Jul 28
	set timeofday 23 59 59 999
	roll month 1
	check date Reiwa $max Aug 16
	check timeofday 23 59 59 999

test roll WEEK_OF_YEAR
    use jcal
	clear all
	# 1867 Dec 23 is Monday.
	set date BeforeMeiji 1867 Dec 23
	roll week_of_year 1
	check day_of_week Mon
	check date BeforeMeiji 1867 Jan 7
	roll week_of_year -1
	check day_of_week Mon
	check date BeforeMeiji 1867 Dec 23
	roll week_of_year 26
	check day_of_week Mon
	check date BeforeMeiji 1867 Jul 1
	roll week_of_year -26
	check day_of_week Mon
	check date BeforeMeiji 1867 Dec 23

	# 1867 Dec 23 is Wednesday.
	set date Meiji 1 Dec 23
	roll week_of_year 1
	check day_of_week Wed
	check date Meiji 1 Jan 1
	roll week_of_year -1
	check day_of_week Wed
	check date Meiji 1 Dec 23
	roll week_of_year 26
	check day_of_week Wed
	check date Meiji 1 Jun 24
	roll week_of_year -26
	check day_of_week Wed
	check date Meiji 1 Dec 23

	# Meiji 45 July 22 is Monday.
	set date Meiji 45 Jul 22
	# the next week if the first week of Taisho 1
	roll week_of_year 1
	check day_of_week Mon
	check date Meiji 45 Jan 1
	roll week_of_year -1
	check day_of_week Mon
	check date Meiji 45 Jul 22
	roll week_of_year 26
	check day_of_week Mon
	check date Meiji 45 Jun 24

	# Taisho Gan-nen (year 1) July 30 is Tuesday.
	set date Taisho 1 Jul 30
	roll week_of_year -1
	# Taisho Gen-nen December 31 is the first week of the next year.
	check day_of_week Tue
	check date Taisho 1 Dec 24
	roll week_of_year 1
	check day_of_week Tue
	check date Taisho 1 Jul 30
	roll week_of_year 26
	check day_of_week Tue
	check date Taisho 1 Aug 27
	roll week_of_year -26
	check day_of_week Tue
	check date Taisho 1 Jul 30

	# Taisho 15 January 7 is Thursday.
	set date Taisho 15 Jan 7
	roll week_of_year -1
	check day_of_week Thu
	check date Taisho 15 Dec 16
	roll week_of_year 1
	check day_of_week Thu
	check date Taisho 15 Jan 7

	roll week_of_year 51
	check day_of_week Thu
	check date Taisho 15 Jan 14

	# Showa Gan-nen December 30 is Thursday.  Showa Gan-nen has
	# only one week. Rolling any number of weeks brings to the
	# same date.
	set date Showa 1 Dec 30
	roll week_of_year 1
	check day_of_week Thu
	check date Showa 1 Dec 30
	roll week_of_year -1
	check day_of_week Thu
	check date Showa 1 Dec 30
	roll week_of_year 26
	check day_of_week Thu
	check date Showa 1 Dec 30
	roll week_of_year -26
	check day_of_week Thu
	check date Showa 1 Dec 30

	# Showa 64 January 7 is Saturday. The year has only one week.
	set date Showa 64 Jan 7
	roll week_of_year 1
	check day_of_week Sat
	check date Showa 64 Jan 7
	roll week_of_year -1
	check day_of_week Sat
	check date Showa 64 Jan 7
	roll week_of_year 26
	check day_of_week Sat
	check date Showa 64 Jan 7
	roll week_of_year -26
	check day_of_week Sat
	check date Showa 64 Jan 7

	# Heisei Gan-nen January 14 is Saturday.
	set date Heisei 1 Jan 14
	roll week_of_year -1
	check day_of_week Sat
	check date Heisei 1 Dec 30
	roll week_of_year 1
	check day_of_week Sat
	check date Heisei 1 Jan 14
	roll week_of_year -26
	check day_of_week Sat
	check date Heisei 1 Jul 8
	roll week_of_year 26
	check day_of_week Sat
	check date Heisei 1 Jan 14

	# Heisei Gan-nen December 1 is Friday.
	set date Heisei 1 Dec 1
	roll week_of_year 5
	check day_of_week Fri
	check date Heisei 1 Jan 13
	roll week_of_year -5
	check day_of_week Fri
	check date Heisei 1 Dec 1
	roll week_of_year 55
	check day_of_week Fri
	check date Heisei 1 Dec 29

    use tokyocal
	clear all

	set date BeforeMeiji $minyear Dec 25
	check day_of_week Sat
	roll week_of_year 1
	check day_of_week Sat
	check date BeforeMeiji $minyear May 22
	roll week_of_year -1
	check day_of_week Sat
	check date BeforeMeiji $minyear Dec 25

test WEEK_OF_MONTH
	use jcal
	clear all

	# Make sure this test does not throw AIOOBE
	set date Heisei 1 Aug 1
	roll week_of_month 1
	check date Heisei 1 Aug 8

	# Check transition dates
	set date Showa 64 Jan 7
	roll week_of_month 1
	check date Showa 64 Jan 7
	roll week_of_month -1
	check date Showa 64 Jan 7

	set date Heisei 1 Jan 31
	roll week_of_month 1
	check date Heisei 1 Jan 10
	roll week_of_month -1
	check date Heisei 1 Jan 31

test DAY_OF_MONTH
	use jcal
	clear all

	# Make sure this test does not throw AIOOBE
	Set date Heisei 1 Aug 1
	roll day_of_month 1
	check date Heisei 1 Aug 2

	# Check transition dates
	set date Showa 64 Jan 7
	roll day_of_month 1
	check date Showa 64 Jan 1
	roll day_of_month -1
	check date Showa 64 Jan 7

	set date Heisei 1 Jan 31
	roll day_of_month 1
	check date Heisei 1 Jan 8
	roll day_of_month -1
	check date Heisei 1 Jan 31

test DAY_OF_YEAR
    use jcal
	clear all

	# 1867 is a regular Gregorian year.
	set date BeforeMeiji 1867 Dec 31
	roll day_of_year 1
	check date BeforeMeiji 1867 Jan 1
	roll day_of_year -1
	check date BeforeMeiji 1867 Dec 31
	roll day_of_year 26
	check date BeforeMeiji 1867 Jan 26
	roll day_of_year -26
	check date BeforeMeiji 1867 Dec 31

	# Meiji 1 starts from Jan 1. It's a regular year as well.
	set date Meiji 1 Dec 31
	roll day_of_year 1
	check date Meiji 1 Jan 1
	roll day_of_year -1
	check date Meiji 1 Dec 31
	roll day_of_year 26
	check date Meiji 1 Jan 26
	roll day_of_year -26
	check date Meiji 1 Dec 31

	# The last year of Meiji (45) has an irregularity. Meiji 45
	# July 30 is actually Taisho 1 July 30.
	set date Meiji 45 Jul 29
	roll day_of_year 1
	check date Meiji 45 Jan 1
	roll day_of_year -1
	check date Meiji 45 Jul 29
	roll day_of_year 26
	check date Meiji 45 Jan 26
	roll day_of_year -26
	check date Meiji 45 Jul 29

	# The first day of Taisho, July 30.
	set date Taisho 1 Jul 30
	roll day_of_year -1
	check date Taisho 1 Dec 31
	roll day_of_year 1
	check date Taisho 1 Jul 30
	roll day_of_year 26
	check date Taisho 1 Aug 25
	roll day_of_year -26
	check date Taisho 1 Jul 30

	set date Taisho 15 Jan 1
	roll day_of_year -1
	check date Taisho 15 Dec 24
	roll day_of_year 1
	check date Taisho 15 Jan 1

	set date Showa 1 Dec 31
	roll day_of_year 1
	check date Showa 1 Dec 25
	roll day_of_year -1
	check date Showa 1 Dec 31
	roll day_of_year 26
	# 26 % 7 = 5
	check date Showa 1 Dec 29
	roll day_of_year -26
	check date Showa 1 Dec 31

	set date Showa 64 Jan 7
	roll day_of_year 1
	check date Showa 64 Jan 1
	roll day_of_year -1
	check date Showa 64 Jan 7
	roll day_of_year 26
	# 26 % 7 = 5
	check date Showa 64 Jan 5
	roll day_of_year -26
	check date Showa 64 Jan 7

	set date Heisei 1 Jan 8
	roll day_of_year -1
	check date Heisei 1 Dec 31
	roll day_of_year 1
	check date Heisei 1 Jan 8
	roll day_of_year -26
	check date Heisei 1 Dec 6
	roll day_of_year 26
	check date Heisei 1 Jan 8

	set date Heisei 1 Dec 31
	roll day_of_year 5
	check date Heisei 1 Jan 12
	roll day_of_year -5
	check date Heisei 1 Dec 31
	roll day_of_year 55
	check date Heisei 1 Mar 3
	roll day_of_year -55
	check date Heisei 1 Dec 31

    use tokyocal
	clear all

	set date BeforeMeiji $minyear Dec 31
	set timeofday 1 47 4 192
	roll day_of_year 1
	check date BeforeMeiji $minyear May 17
	check timeofday 1 47 4 192
	roll day_of_year -1
	check date BeforeMeiji $minyear Dec 31
	check timeofday 1 47 4 192

test DAY_OF_WEEK_IN_MONTH
    use jcal
	clear all
