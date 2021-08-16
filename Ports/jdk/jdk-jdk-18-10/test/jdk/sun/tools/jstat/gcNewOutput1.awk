#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcnew 0
#
#    S0C         S1C         S0U         S1U     TT MTT     DSS          EC           EU       YGC     YGCT   
#        0.0      2048.0         0.0      1113.5 15  15      1024.0       4096.0          0.0      4     0.228
#
# -J-XX:+UseParallelGC -gcnew 0
#
#    S0C         S1C         S0U         S1U     TT MTT     DSS          EC           EU       YGC     YGCT   
#     1024.0       512.0         0.0       512.0  6  15      1024.0       3072.0        830.1      5     0.164

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^    S0C         S1C         S0U         S1U     TT MTT     DSS          EC           EU       YGC     YGCT   $/	{

	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+\.[0-9]+$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 1) && (totallines == 2)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
