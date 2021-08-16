/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8129740 8133111
 * @summary Incorrect class file created when passing lambda in inner class constructor
 * @run main QualifiedThisAccessTest
 */

public class QualifiedThisAccessTest { // Not referenced by lambda, so should not be captured.

    public class Universe { // Not referenced by lambda, so should not be captured.

        public String name;
        public int galaxiesCount;

        public Universe(String name, int galaxiesCount) {
            this.name = name;
            this.galaxiesCount = galaxiesCount;
        }

        public String toString() {
            return "Universe" + name + " of " + galaxiesCount + " galaxies";
        }

        class Galaxy {

            String name;
            private int starsCount;

            Galaxy(String name, int starsCount) {
                this.name = name;
                this.starsCount = starsCount;
            }

            public String toString() {
                return "galaxy " + name + " of " + starsCount + " solar systems";
            }

            int starsCount() {
                return starsCount;
            }

            private String name() {
                return name;
            }

            class SolarSystem {

                String name;
                int planetsCount;

                SolarSystem(String name, int planetsCount) {
                    this.name = name;
                    this.planetsCount = planetsCount;
                }

                public String toString() {
                    return "Solar System of " + name + " with " + planetsCount + " planets";
                }

                int planetsCount() {
                    return planetsCount;
                }

                SolarSystem copy(SolarSystem s) {
                    return s;
                }

                class Planet {

                    String name;
                    int moonsCount;

                    Planet(String name, int moonsCount, Runnable r) {
                        this.name = name;
                        this.moonsCount = moonsCount;
                        r.run();
                    }
                    Planet (String name, int moonsCount) {
                        this(name, moonsCount, ()-> {
                            StringBuffer buf = new StringBuffer();
                            buf.append("This planet belongs to the galaxy "
                                        + Galaxy.this.name + " with " + starsCount + " stars\n");
                            buf.append("This planet belongs to the galaxy "
                                        + Universe.Galaxy.this.name + " with " + starsCount() + " stars\n");
                            buf.append("This planet belongs to the galaxy "
                                        + Galaxy.this.name() + " with " + starsCount() + " stars\n");
                            buf.append("This planet belongs to the galaxy "
                                        + Universe.Galaxy.this.name() + " with "
                                        + (Universe.Galaxy.this).starsCount() + " stars\n");

                            buf.append("This planet belongs to the solar system "
                                        + SolarSystem.this.name + " with " + planetsCount + " planets\n");
                            buf.append("This planet belongs to the solar system "
                                        + Galaxy.SolarSystem.this.name + " with " + planetsCount() + " planets\n");
                            buf.append("This planet belongs to the solar system "
                                        + (SolarSystem.this).name + " with " + planetsCount + " planets\n");
                            buf.append("This planet belongs to the solar system "
                                        + Universe.Galaxy.SolarSystem.this.name + " with "
                                        + Universe.Galaxy.SolarSystem.this.planetsCount + " planets\n");
                            buf.append("This planet belongs to the solar system "
                                        + Universe.Galaxy.SolarSystem.this.name.toLowerCase().toUpperCase()
                                        + " with " + Universe.Galaxy.SolarSystem.this.planetsCount + " planets\n");
                            buf.append("This planet belongs to the solar system "
                                        + copy(Universe.Galaxy.SolarSystem.this).name.toLowerCase().toUpperCase()
                                        + " with " + Universe.Galaxy.SolarSystem.this.planetsCount + " planets\n");
                            if (!buf.toString().equals(output))
                                throw new AssertionError("Unexpected value\n" + buf);
                        });
                    }

                    static final String output =
                        "This planet belongs to the galaxy Mily way with 23456789 stars\n" +
                        "This planet belongs to the galaxy Mily way with 23456789 stars\n" +
                        "This planet belongs to the galaxy Mily way with 23456789 stars\n" +
                        "This planet belongs to the galaxy Mily way with 23456789 stars\n" +
                        "This planet belongs to the solar system Sun with 9 planets\n" +
                        "This planet belongs to the solar system Sun with 9 planets\n" +
                        "This planet belongs to the solar system Sun with 9 planets\n" +
                        "This planet belongs to the solar system Sun with 9 planets\n" +
                        "This planet belongs to the solar system SUN with 9 planets\n" +
                        "This planet belongs to the solar system SUN with 9 planets\n";


                    public String toString() {
                        return "Planet " + name + " with " + moonsCount + " moon(s)";
                    }
                }
            }
        }
    }
    public static void main(String[] args) {
        new QualifiedThisAccessTest().new Universe("Universe", 12345678).new Galaxy("Mily way", 23456789).new SolarSystem("Sun", 9).new Planet("Earth", 1);
    }
}
