    class Universe { // Not referenced by lambda, so should not be captured.

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
                            String n = name;
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
