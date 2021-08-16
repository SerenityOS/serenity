/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jit.graph;

// This class defines the tree object.
public class RBTree {
    public final static int maxNodes = 70;      // maximum nodes allowed.
    public final static int INSERT = 0;         // constants indicating
    public final static int DELETE = 1;         // the current operation
    public final static int NOP = 2;
    public final static Node treeNull = new Node(); // the tree NULL node.

    private Node root;
    private int num_of_nodes;
    private int height;           // The tree height, it is updated
    // in each operation.

    // since the algorithm is executed in stages I have to remember data
    // on the state.
    private Node node;  // The current operation is being done on it.
    private int action; // The operation being performed (insert / delete)
    private int stage;  // The current stage of execution

    // the constructor initializes the object fields.
    public RBTree() {
        root = treeNull;
        node = treeNull;
        num_of_nodes = 0;
        height = 0;
        action = NOP;
        stage = 0;
    }

    // This method returns the root of the tree.
    public Node getRoot() {
        return root;
    }

    // This method returns the number of nodes in the tree.
    public int getNodes() {
        return num_of_nodes;
    }

    // This method returns the height of the tree.
    public int getHeight() {
        return height;
    }


    // This method inserts k into the Red Black Tree
    public boolean RBInsert(int k) {
        // checking similar to the RB_Insert method
        if (action != NOP) {
            System.out.println("Only one operation can be done at a time.");
            return false;
        }

        if (num_of_nodes == maxNodes) {
            System.out.println("The maximum nodes allowed is already reached.");
            return false;
        }

        // Check if there is already node with key k.
        if (Search(k) == treeNull) {
            action = INSERT;
            node = new Node(k);
            node.setNode(Node.Left_son, treeNull);
            node.setNode(Node.Right_son, treeNull);
            node.setNode(Node.Parent, treeNull);
            stage = 1;
            // This is the loop that perform all the operation steps.
            while (stage != 0) {
                // perform one step
                InsertStep();
                // update the tree height
                updateHeight();
            }
            // set the action to NoOPretion.
            action = NOP;
            return true;
        } else
            System.out.println("Insertion failed. This key already exist.");
        return false;
    }


    // This method deletes the element k from the Red Black tree
    public boolean RBDelete(int k) {
        // checking like in RB_Delete method
        if (action != NOP) {
            System.out.println("Only one operation can be done at a time.");
            return false;
        }
        node = Search(k);
        // Check if there is a node with key k.
        if (node != treeNull) {
            action = DELETE;
            stage = 1;
            // this loop perform all the operation steps.
            while (stage != 0) {
                // perform one step
                DeleteStep();
                // update the tree height
                updateHeight();
            }
            action = NOP;
            return true;
        } else
            System.out.println("Deletion failed. This key doesn't exist.");
        return false;
    }

    // This method performs one step in the insertion operation.
    // It performs a step according to the stage variable.
    // I will not explain exactly what each stage do, just that they
    // divided to 4 categories:
    // 1. inserting a node to the tree.
    // 2. marking nodes that will be recolored.
    // 3. recoloring nodes.
    // 4. rotating right or left.
    private void InsertStep() {
        // Pr is parent, GrPr is grandparent and Un is uncle.
        Node Pr, GrPr, Un;
        switch (stage) {
            // Inserting a node to the tree
            case 1:
                Tree_Insert();
                break;
            // mid stage that moves the algorithm to the proper next stage
            case 2:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Un = GrPr.getNode(Node.Right_son);
                    if (Un.getColor() == Node.Red) {
                        stage = 3;
                    } else if (node == Pr.getNode(Node.Right_son)) {
                        node = Pr;
                        stage = 5;
                    } else {
                        stage = 6;
                    }
                } else {
                    Un = GrPr.getNode(Node.Left_son);
                    if (Un.getColor() == Node.Red) {
                        stage = 3;
                    } else if (node == Pr.getNode(Node.Left_son)) {
                        node = Pr;
                        stage = 5;
                    } else {
                        stage = 6;
                    }
                }
                break;
            // This stage marks node that will be recolored
            case 3:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Un = GrPr.getNode(Node.Right_son);
                } else {
                    Un = GrPr.getNode(Node.Left_son);
                }
                node = GrPr;
                stage = 4;
                break;
            // This stage recolors marked nodes.
            case 4:
                node.setColor(Node.Red);
                node.getNode(Node.Left_son).setColor(Node.Black);
                node.getNode(Node.Right_son).setColor(Node.Black);

                if ((node == root) ||
                        (node.getNode(Node.Parent).getColor() == Node.Black)) {
                    if (root.getColor() == Node.Red) {
                        stage = 9;
                    } else
                        stage = 0;
                } else {
                    stage = 2;
                    InsertStep();
                }
                break;
            // This stage performs rotation operation
            case 5:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(node);
                } else {
                    Right_Rotate(node);
                }
                stage = 6;
                break;
            // This stage marks nodes that will be recolor.
            case 6:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);

                stage = 7;
                break;
            // This stage recolors marked nodes.
            case 7:
                Pr = node.getNode(Node.Parent);
                Pr.setColor(Node.Black);
                GrPr = Pr.getNode(Node.Parent);
                GrPr.setColor(Node.Red);

                stage = 8;
                break;
            // This stage performs rotation operation
            case 8:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Right_Rotate(GrPr);
                } else {
                    Left_Rotate(GrPr);
                }
                if (root.getColor() == Node.Red) {
                    stage = 9;
                } else
                    stage = 0;
                break;
            // this stage marks the root.
            case 9:
                stage = 10;
                break;
            // This stage recolors the root.
            case 10:
                root.setColor(Node.Black);
                stage = 0;
                break;
        }
    }

    // This method performs one step in the deletion operation.
    // It perform sa step according to the stage variable.
    // I will explain exactly what each stage do, just that they
    // divided to 4 categories:
    // 1. deleting a node from the tree.
    // 2. marking nodes that will be recolored.
    // 3. recoloring nodes.
    // 4. rotating right or left.
    public void DeleteStep() {
        // Pr is Parent, Br is Brother
        Node Pr, Br;
        switch (stage) {
            // This stage delete a node from the tree.
            case 1:
                Tree_Delete();
                break;
            // This stage marks a nodes that will be recolored or perform other stage.
            case 2:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                if (Br.getColor() == Node.Red) {
                    stage = 3;
                } else if ((Br.getNode(Node.Right_son).getColor() == Node.Black)
                        && (Br.getNode(Node.Left_son).getColor() == Node.Black)) {
                    stage = 5;
                    DeleteStep();
                } else {
                    stage = 7;
                    DeleteStep();
                }
                break;
            // This stage recolors marked nodes.
            case 3:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                Br.setColor(Node.Black);
                Pr.setColor(Node.Red);

                stage = 4;
                break;
            // This stage performs rotation operation
            case 4:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(Pr);
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Right_Rotate(Pr);
                    Br = Pr.getNode(Node.Left_son);
                }
                if ((Br.getNode(Node.Right_son).getColor() == Node.Black)
                        && (Br.getNode(Node.Left_son).getColor() == Node.Black)) {
                    stage = 5;
                } else {
                    stage = 7;
                }

                break;
            // This stage marks nodes that will be recolor.
            case 5:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                stage = 6;
                break;
            // This stage recolors marked nodes.
            case 6:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                Br.setColor(Node.Red);
                node = Pr;

                if ((node != root) && (node.getColor() == Node.Black)) {
                    stage = 2;
                } else if (node.getColor() == Node.Red) {
                    stage = 13;
                } else
                    stage = 0;
                break;
            // This stage marks nodes that will be recolor or perform other stage.
            case 7:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    if ((Br.getNode(Node.Right_son)).getColor() == Node.Black) {
                        stage = 8;
                    } else {
                        stage = 10;
                        DeleteStep();
                    }
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    if ((Br.getNode(Node.Left_son)).getColor() == Node.Black) {
                        stage = 8;
                    } else {
                        stage = 10;
                        DeleteStep();
                    }
                }
                break;
            // This stage recolors marked nodes.
            case 8:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Br.getNode(Node.Left_son).setColor(Node.Black);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Br.getNode(Node.Right_son).setColor(Node.Black);
                }
                Br.setColor(Node.Red);
                stage = 9;
                break;
            // This stage performs rotation operation
            case 9:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Right_Rotate(Br);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Left_Rotate(Br);
                }

                stage = 10;
                break;
            // This stage marks node that will be recolor.
            case 10:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }

                stage = 11;
                break;
            // This stage recolors marked nodes.
            case 11:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Br.getNode(Node.Right_son).setColor(Node.Black);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Br.getNode(Node.Left_son).setColor(Node.Black);

                }
                if (Br.getColor() != Pr.getColor()) {
                    Br.setColor(Pr.getColor());
                }
                if (Pr.getColor() != Node.Black) {
                    Pr.setColor(Node.Black);
                }

                stage = 12;
                break;
            // This stage performs rotation operation.
            case 12:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(Pr);
                } else {
                    Right_Rotate(Pr);
                }
                node = root;
                if (node.getColor() == Node.Red) {
                    stage = 13;
                } else {
                    stage = 0;
                }
                break;
            // This stage marks a node that will be recolored
            case 13:
                stage = 14;
                break;
            // This stage recolors marked node.
            case 14:
                node.setColor(Node.Black);
                stage = 0;
                break;
        }
    }

    // This method inserts the node 'node' to the tree.
    // it called from the first stage in the InsertStep method.
    // we 'dive' from the root to a leaf according to the node key
    // and insert the node in the proper place.
    private void Tree_Insert() {
        Node n1, n2;
        n1 = root;
        n2 = treeNull;
        while (n1 != treeNull) {
            n2 = n1;
            if (node.getKey() < n1.getKey()) {
                n1 = n1.getNode(Node.Left_son);
            } else {
                n1 = n1.getNode(Node.Right_son);
            }
        }
        node.setNode(Node.Parent, n2);
        if (n2 == treeNull) {
            root = node;
        }
        else {
            if (node.getKey() < n2.getKey()) {
                n2.setNode(Node.Left_son, node);
            } else {
                n2.setNode(Node.Right_son, node);
            }
        }
        // updating the insertion stage.
        if ((node == root) ||
                (node.getNode(Node.Parent).getColor() == Node.Black)) {
            if (root.getColor() == Node.Red) {
                stage = 9;
            } else {
                stage = 0;
            }
        } else {
            stage = 2;
            InsertStep();
        }
        num_of_nodes++;   // increasing the number of nodes
    }

    // This method deletes the node 'node' from the tree.
    // it called from the first stage in the DeleteStep method.
    // if node has at most one son we just remove it and connect
    // his son and parent. If it has 2 sons we delete his successor
    // that has at most one son and replace him with the successor.
    private void Tree_Delete() {
        Node n1, n2, n3;
        if ((node.getNode(Node.Left_son) == treeNull) ||
                (node.getNode(Node.Right_son) == treeNull)) {
            n1 = node;
        } else {
            n1 = Tree_Successor(node);
        }

        if (n1.getNode(Node.Left_son) != treeNull) {
            n2 = n1.getNode(Node.Left_son);
        } else {
            n2 = n1.getNode(Node.Right_son);
        }

        n3 = n1.getNode(Node.Parent);
        n2.setNode(Node.Parent, n3);
        if (n3 == treeNull) {
            root = n2;
        } else if (n1 == n3.getNode(Node.Left_son)) {
            n3.setNode(Node.Left_son, n2);
        } else {
            n3.setNode(Node.Right_son, n2);
        }

        if (n1 != node) {
            node.setKey(n1.getKey());
        }

        node = n2;
        if (n1.getColor() == Node.Black) {
            if ((node != root) && (node.getColor() == Node.Black)) {
                stage = 2;
            } else if (node.getColor() == Node.Red) {
                stage = 13;
            } else {
                stage = 0;
            }
        } else {
            stage = 0;
        }
        // decrease the number of nodes.
        num_of_nodes--;
    }

    // This method returns the successor of the node n in the tree.
    private Node Tree_Successor(Node n) {
        Node n1;
        if (n.getNode(Node.Right_son) != treeNull) {
            n = n.getNode(Node.Right_son);
            while (n.getNode(Node.Left_son) != treeNull) {
                n = n.getNode(Node.Left_son);
            }
            return n;
        }
        n1 = n.getNode(Node.Parent);
        while ((n1 != treeNull) && (n == n1.getNode(Node.Right_son))) {
            n = n1;
            n1 = n1.getNode(Node.Parent);
        }
        return n1;
    }

    // This method performs Left Rotation with n1.
    private void Left_Rotate(Node n1) {
        Node n2;

        n2 = n1.getNode(Node.Right_son);
        n1.setNode(Node.Right_son, n2.getNode(Node.Left_son));
        if (n2.getNode(Node.Left_son) != treeNull) {
            n2.getNode(Node.Left_son).setNode(Node.Parent, n1);
        }
        n2.setNode(Node.Parent, n1.getNode(Node.Parent));
        if (n1.getNode(Node.Parent) == treeNull) {
            root = n2;
        } else if (n1 == n1.getNode(Node.Parent).getNode(Node.Left_son)) {
            n1.getNode(Node.Parent).setNode(Node.Left_son, n2);
        } else {
            n1.getNode(Node.Parent).setNode(Node.Right_son, n2);
        }
        n2.setNode(Node.Left_son, n1);
        n1.setNode(Node.Parent, n2);
    }

    // This method performs Right Rotation with n1.
    private void Right_Rotate(Node n1) {
        Node n2;

        n2 = n1.getNode(Node.Left_son);
        n1.setNode(Node.Left_son, n2.getNode(Node.Right_son));
        if (n2.getNode(Node.Right_son) != treeNull) {
            n2.getNode(Node.Right_son).setNode(Node.Parent, n1);
        }
        n2.setNode(Node.Parent, n1.getNode(Node.Parent));
        if (n1.getNode(Node.Parent) == treeNull) {
            root = n2;
        } else if (n1 == (n1.getNode(Node.Parent)).getNode(Node.Left_son)) {
            n1.getNode(Node.Parent).setNode(Node.Left_son, n2);
        } else {
            n1.getNode(Node.Parent).setNode(Node.Right_son, n2);
        }
        n2.setNode(Node.Right_son, n1);
        n1.setNode(Node.Parent, n2);
    }

    // This method searches the tree for a node with key 'key', and
    // returns the node on success otherwise treeNull.
    public Node Search(int key) {
        Node node;
        node = root;
        while ((node != treeNull) && (key != node.getKey())) {
            if (key < node.getKey()) {
                node = node.getNode(Node.Left_son);
            } else {
                node = node.getNode(Node.Right_son);
            }
        }
        return node;
    }

    // This method updates the tree height. it uses a recursive method
    // findHeight.
    private void updateHeight() {
        height = 0;
        if (root != treeNull) {
            findHeight(root, 1);
        }
    }

    // This is a recursive method that find a node height.
    private void findHeight(Node n, int curr) {
        if (height < curr) {
            height = curr;
        }
        if (n.getNode(Node.Left_son) != treeNull) {
            findHeight(n.getNode(Node.Left_son), curr + 1);
        }
        if (n.getNode(Node.Right_son) != treeNull) {
            findHeight(n.getNode(Node.Right_son), curr + 1);
        }
    }

}
