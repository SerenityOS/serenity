#!/usr/bin/env python3

"""
Creates a PDF that embeds a jbig2 image. Useful for viewing .jbig2 files in
PDF viewers, since all PDF viewers support .jbig2 but few image viewers do.

Usage is a bit clunky (use Build/lagom/bin/file to get the dimensions):
% Meta/jbig2_to_pdf.py -o foo.pdf path/to/bitmap.jbig2 399 400
% open foo.pdf
"""

import argparse
import sys
import textwrap


def dedent(b):
    return textwrap.dedent(b.decode('latin1')).encode('latin1')


def main():
    parser = argparse.ArgumentParser(
        epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("image", help="Input image")
    parser.add_argument("width", type=int, help="image width")
    parser.add_argument("height", type=int, help="image height")
    parser.add_argument("-o", "--output", help="Path to output PDF")
    args = parser.parse_args()

    width, height = args.width, args.height

    with open(args.image, 'rb') as f:
        image_data = f.read()
        print(f'dims {width}x{height}')

    # strip jbig2 header
    image_data = image_data[8:]
    if image_data[0] & 1 == 0:
        print('random-access jbig2 does not work', file=sys.stderr)
        sys.exit(1)
    if image_data[0] & 2 == 0:
        image_data = image_data[4:]
    image_data = image_data[1:]

    start = dedent(b'''\
              %PDF-1.4
              %\265\266

              ''')

    objs = [dedent(b'''\
              1 0 obj
              <<
                /Type /Catalog
                /Pages 2 0 R
              >>
              endobj
              '''),

            dedent(b'''\
              2 0 obj
              <<
                /Type /Pages
                /Kids [3 0 R]
                /Count 1
              >>
              endobj
              '''),

            dedent(b'''\
              3 0 obj
              <<
                /Type /Page
                /Parent 2 0 R
                /MediaBox [0 0 %d %d]
                /Contents 4 0 R
                /Resources <<
                  /XObject <<
                    /Im 5 0 R
                  >>
                >>
              >>
              endobj
              ''' % (width, height)),

            dedent(b'''\
              4 0 obj
              <</Length 25>>
              stream
              %d 0 0 %d 0 0 cm
              /Im Do
              endstream
              endobj
              ''' % (width, height)),

            dedent(b'''\
              5 0 obj
              <<
                /Length %d
                /Type /XObject
                /Subtype /Image
                /Width %d
                /Height %d
                /ColorSpace /DeviceGray
                /Filter /JBIG2Decode
                /BitsPerComponent 1
              >>
              stream
              ''' % (len(image_data), width, height)) +
            image_data +
            dedent(b'''
              endstream
              endobj
              '''),
            ]

    with open(args.output, 'wb') as f:
        f.write(start)

        offsets = []
        for obj in objs:
            offsets.append(f.tell())
            f.write(obj)
            f.write(b'\n')

        xref_offset = f.tell()
        f.write(b'xref\n')
        f.write(b'0 %d\n' % (len(objs) + 1))
        f.write(b'0000000000 65536 f \n')
        for offset in offsets:
            f.write(b'%010d 00000 n \n' % offset)
        f.write(b'\n')

        f.write(dedent(b'''\
            trailer
            <<
              /Size %d
              /Root 1 0 R
            >>
            startxref
            %d
            %%%%EOF
            ''' % (len(objs) + 1, xref_offset)))


if __name__ == '__main__':
    main()
