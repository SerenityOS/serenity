#!/bin/csh -f
#
# BEFORE running this test, you need to set up the environment as follows.
# 1. Create a 'sample' service principal in the KDC.
# 2. Create a keytab for the server principal 'sample/fqdn@REALM'
#    where 'fqdn' is the fully qualified domain name of the server and
#    REALM is the KDC's realm. The principal must be a host-based service.
#    For example, a principal name might be
#      'sample/machineX.imc.org@IMC.ORG'. 
#    On Windows, for example, you use the ktpass utility to create a host keytab 
#    file.
#    c:> ktpass -princ sample/machineX.imc.org@IMC.ORG -mapuser sample \
#        -ptype KRB5_NT_SRV_HST \
#        -pass servertest123 -out machineX.keytab
# 3. Create a user principal in the KDC.
# 4. Set up a JAAS login module configuration file like gsseg_jaas.conf, updating
#    the client and server entries according to the principal and machine names
#    used.
# 5. Update AuthOnly.SERVER_FQDN with fqdn of server machine.
# 6. To examine exchange, turn on logging by adding
#        -Djava.util.logging.config.file=log.properties
# 7. Update the realm and kdc settings in this script.
#
java -Djava.security.krb5.realm=IMC.ORG -Djava.security.krb5.kdc=machineX.imc.org -Djava.security.auth.login.config=gsseg_jaas.conf  NoSecurityLayer
