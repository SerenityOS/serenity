#
# matching the following output specified as a pattern that verifies
# that the numerical values conform to a specific pattern, rather than
# specific values.
#
# -gcnewcapacity 0
#
#   NGCMN        NGCMX         NGC         S0CMX        S0C        S1CMX        S1C         ECMX          EC       YGC    FGC   CGC 
#         0.0    4194304.0       6144.0         0.0         0.0   4194304.0      2048.0    4194304.0       4096.0      4     0     0
#
# -J-XX:+UseParallelGC -gcnewcapacity 0
#
#   NGCMN        NGCMX         NGC         S0CMX        S0C        S1CMX        S1C         ECMX          EC       YGC    FGC   CGC 
#      1536.0    1397760.0       7168.0    465920.0       512.0    465920.0       512.0    1396736.0       6144.0      4     0     -

BEGIN	{
	    headerlines=0; datalines=0; totallines=0
	}

/^   NGCMN        NGCMX         NGC         S0CMX        S0C        S1CMX        S1C         ECMX          EC       YGC    FGC   CGC $/	{
	    headerlines++;
	}

/^[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+\.[0-9]+[ ]*[0-9]+[ ]*[0-9]+[ ]*(-|[0-9]+)$/	{
	    datalines++;
	}

	{ totallines++; print $0 }

END	{
	    if ((headerlines == 1) && (datalines == 1)) {
	        exit 0
	    }
	    else {
	        exit 1
	    }
	}
