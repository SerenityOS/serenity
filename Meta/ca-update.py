import configparser
import fileinput
from base64 import b64encode

from cryptography import x509
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import Encoding
from cryptography.x509.oid import NameOID


def main():
    with open("cacert.pem", "rb") as source, open("ca_certs.ini", "w", encoding="utf-8") as dest:
        certs = x509.load_pem_x509_certificates(source.read())
        output = configparser.RawConfigParser()
        output.optionxform = lambda option: option

        for cert in certs:

            # We can only parse RSA, so trash everything else
            if not isinstance(cert.public_key(), rsa.RSAPublicKey):
                continue

            commonName = cert.subject.get_attributes_for_oid(NameOID.COMMON_NAME)
            if not commonName:
                commonName = cert.subject.get_attributes_for_oid(NameOID.ORGANIZATIONAL_UNIT_NAME)

            if not commonName:
                continue

            commonName = commonName[0].value

            organizationName = cert.subject.get_attributes_for_oid(NameOID.ORGANIZATION_NAME)
            if organizationName:
                organizationName = organizationName[0].value
            else:
                organizationName = ""

            raw_cert = b64encode(cert.public_bytes(Encoding.DER))
            if not output.has_section(organizationName):
                output.add_section(organizationName)
            output.set(organizationName, commonName, raw_cert.decode("utf-8"))

        output.write(dest)

    # This could be removed if it is ok to keep [DEFAULT] instead of [], configparser does not allow empty section names
    with fileinput.FileInput("ca_certs.ini", inplace=True) as file:
        for line in file:
            print(line.replace("[DEFAULT]", "[]"), end='')


if __name__ == "__main__":
    main()
