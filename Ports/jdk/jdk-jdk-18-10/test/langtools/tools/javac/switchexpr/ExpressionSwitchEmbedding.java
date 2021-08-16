/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214031 8214114 8236546
 * @summary Verify switch expressions embedded in various statements work properly.
 * @compile ExpressionSwitchEmbedding.java
 * @run main ExpressionSwitchEmbedding
 */

public class ExpressionSwitchEmbedding {
    public static void main(String... args) {
        new ExpressionSwitchEmbedding().run();
    }

    private void run() {
        {
            int i = 6;
            int o = 0;
            while (switch (i) {
                case 1: i = 0; yield true;
                case 2: i = 1; yield true;
                case 3, 4: i--;
                    if (i == 2 || i == 4) {
                        yield switch (i) {
                            case 2 -> true;
                            case 4 -> false;
                            default -> throw new IllegalStateException();
                        };
                    } else {
                        yield true;
                    }
                default: i--; yield switch (i) {
                    case -1 -> false;
                    case 3 -> true;
                    default -> true;
                };
            }) {
                o++;
            }
            if (o != 6 && i >= 0) {
                throw new IllegalStateException();
            }
        }
        {
            int i = 6;
            int o = 0;
            while (switch (i) {
                case 1: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 0; yield true; }
                case 2: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 1; yield true; }
                case 3, 4:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        if (i == 2 || i == 4) {
                            try {
                                yield switch (i) {
                                    case 2 -> throw new ResultException(true);
                                    case 4 -> false;
                                    default -> throw new IllegalStateException();
                                };
                            } catch (ResultException ex) {
                                yield ex.result;
                            }
                        } else {
                            yield true;
                        }
                    }
                default:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        yield switch (i) {
                            case -1 -> false;
                            case 3 -> true;
                            default -> true;
                        };
                    }
                    throw new AssertionError();
            }) {
                o++;
            }
            if (o != 6 && i >= 0) {
                throw new IllegalStateException();
            }
        }
        {
            int i = 6;
            int o = 0;
            if (switch (i) {
                case 1: i = 0; yield true;
                case 2: i = 1; yield true;
                case 3, 4: i--;
                    if (i == 2 || i == 4) {
                        yield (switch (i) {
                            case 2 -> 3;
                            case 4 -> 5;
                            default -> throw new IllegalStateException();
                        }) == i + 1;
                    } else {
                        yield true;
                    }
                default: i--; yield switch (i) {
                    case -1 -> false;
                    case 3 -> true;
                    default -> true;
                };
            }) {
                o++;
            }
            if (o != 1 && i != 5) {
                throw new IllegalStateException();
            }
        }
        {
            int i = 6;
            int o = 0;
            if (switch (i) {
                case 1: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 0; yield true; }
                case 2: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 1; yield true; }
                case 3, 4:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        if (i == 2 || i == 4) {
                            try {
                                yield switch (i) {
                                    case 2 -> throw new ResultException(true);
                                    case 4 -> false;
                                    default -> throw new IllegalStateException();
                                };
                            } catch (ResultException ex) {
                                yield ex.result;
                            }
                        } else {
                            yield true;
                        }
                    }
                default:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        yield switch (i) {
                            case -1 -> false;
                            case 3 -> true;
                            default -> true;
                        };
                    }
                    throw new AssertionError();
            }) {
                o++;
            }
            if (o != 1 && i != 5) {
                throw new IllegalStateException();
            }
        }
        {
            int o = 0;
            for (int i = 6; (switch (i) {
                case 1: i = 0; yield true;
                case 2: i = 1; yield true;
                case 3, 4: i--;
                    if (i == 2 || i == 4) {
                        yield switch (i) {
                            case 2 -> true;
                            case 4 -> false;
                            default -> throw new IllegalStateException();
                        };
                    } else {
                        yield true;
                    }
                default: i--; yield switch (i) {
                    case -1 -> false;
                    case 3 -> true;
                    default -> true;
                };
            }); ) {
                o++;
            }
            if (o != 6) {
                throw new IllegalStateException();
            }
        }
        {
            int o = 0;
            for (int i = 6; (switch (i) {
                case 1: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 0; yield true; }
                case 2: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 1; yield true; }
                case 3, 4:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        if (i == 2 || i == 4) {
                            try {
                                yield switch (i) {
                                    case 2 -> throw new ResultException(true);
                                    case 4 -> false;
                                    default -> throw new IllegalStateException();
                                };
                            } catch (ResultException ex) {
                                yield ex.result;
                            }
                        } else {
                            yield true;
                        }
                    }
                default:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        yield switch (i) {
                            case -1 -> false;
                            case 3 -> true;
                            default -> true;
                        };
                    }
                    throw new AssertionError();
            }); ) {
                o++;
            }
            if (o != 6) {
                throw new IllegalStateException();
            }
        }
        {
            int i = 6;
            int o = 0;
            do {
                o++;
            } while (switch (i) {
                case 1: i = 0; yield true;
                case 2: i = 1; yield true;
                case 3, 4: i--;
                    if (i == 2 || i == 4) {
                        yield switch (i) {
                            case 2 -> true;
                            case 4 -> false;
                            default -> throw new IllegalStateException();
                        };
                    } else {
                        yield true;
                    }
                default: i--; yield switch (i) {
                    case -1 -> false;
                    case 3 -> true;
                    default -> true;
                };
            });
            if (o != 6 && i >= 0) {
                throw new IllegalStateException();
            }
        }
        {
            int i = 6;
            int o = 0;
            do {
                o++;
            } while (switch (i) {
                case 1: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 0; yield true; }
                case 2: try { new ExpressionSwitchEmbedding().throwException(); } catch (Throwable t) { i = 1; yield true; }
                case 3, 4:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        if (i == 2 || i == 4) {
                            try {
                                yield switch (i) {
                                    case 2 -> throw new ResultException(true);
                                    case 4 -> false;
                                    default -> throw new IllegalStateException();
                                };
                            } catch (ResultException ex) {
                                yield ex.result;
                            }
                        } else {
                            yield true;
                        }
                    }
                default:
                    try {
                        new ExpressionSwitchEmbedding().throwException();
                    } catch (Throwable t) {
                        i--;
                        yield switch (i) {
                            case -1 -> false;
                            case 3 -> true;
                            default -> true;
                        };
                    }
                    throw new AssertionError();
            });
            if (o != 6 && i >= 0) {
                throw new IllegalStateException();
            }
        }
        {
            String s = "";
            Object o = switch (s) { default -> s != null && s == s; };
            if (!(Boolean) o) {
                throw new IllegalStateException();
            }
        }
    }

    private void throwException() {
        throw new RuntimeException();
    }

    private static final class ResultException extends RuntimeException {
        public final boolean result;
        public ResultException(boolean result) {
            this.result = result;
        }
    }
}
