//package cs149.stm;

import org.deuce.Atomic;
import java.util.concurrent.atomic.AtomicLong;

public class STMTreap implements IntSet {
    static class Node {
        final int key;
        final int priority;
        Node left;
        Node right;

        Node(final int key, final int priority) {
            this.key = key;
            this.priority = priority;
        }

        public String toString() {
            return "Node[key=" + key + ", prio=" + priority +
                    ", left=" + (left == null ? "null" : String.valueOf(left.key)) +
                    ", right=" + (right == null ? "null" : String.valueOf(right.key)) + "]";
        }
    }

    private AtomicLong randState = new AtomicLong(0);
    private Node root;

    @Atomic
    public boolean contains(final int key) {
        Node node = root;
        while (node != null) {
            if (key == node.key) {
                return true;
            }
            node = key < node.key ? node.left : node.right;
        }
        return false;
    }

    @Atomic
    public void add(final int key) {
        final Node newRoot = addImpl(root, key);
        if (root != newRoot) {
            root = newRoot;
        }
    }

    @Atomic 
    private Node addImpl(final Node node, final int key) {
        if (node == null) {
            return new Node(key, randPriority());
        }
        else if (key == node.key) {
            // no insert needed
            return node;
        }
        else if (key < node.key) {
            final Node newNodeLeft = addImpl(node.left, key);
            if (node.left != newNodeLeft) {
                node.left = newNodeLeft;
            }
            if (node.left.priority > node.priority) {
                return rotateRight(node);
            }
            return node;
        }
        else {
            final Node newNodeRight = addImpl(node.right, key);
            if (node.right != newNodeRight) {
                node.right = newNodeRight;
            }
            if (node.right.priority > node.priority) {
                return rotateLeft(node);
            }
            return node;
        }
    }

    private int randPriority() {
        // The constants in this 64-bit linear congruential random number
        // generator are from http://nuclear.llnl.gov/CNP/rng/rngman/node4.html

        while (true) {
            long oldVal = randState.get();
            long newVal = oldVal * 2862933555777941757L + 3037000493L;
            boolean flag = randState.compareAndSet(oldVal, newVal);
            if (flag)
                return (int)(newVal >> 30);
        }
    }

    @Atomic
    private Node rotateRight(final Node node) {
        //       node                  nL
        //     /      \             /      \
        //    nL       z     ==>   x       node
        //  /   \                         /   \
        // x   nLR                      nLR   z
        final Node nL = node.left;
        node.left = nL.right;
        nL.right = node;
        return nL;
    }

    @Atomic
    private Node rotateLeft(final Node node) {
        final Node nR = node.right;
        node.right = nR.left;
        nR.left = node;
        return nR;
    }

    @Atomic   
    public void remove(final int key) {
        final Node newRoot = removeImpl(root, key);
        if (root != newRoot) {
            root = newRoot;
        }
    }

    @Atomic
    private Node removeImpl(final Node node, final int key) {
        if (node == null) {
            // not present, nothing to do
            return null;
        }
        else if (key == node.key) {
            if (node.left == null) {
                // splice out this node
                return node.right;
            }
            else if (node.right == null) {
                return node.left;
            }
            else {
                // Two children, this is the hardest case.  We will pretend
                // that node has -infinite priority, move it down, then retry
                // the removal.
                if (node.left.priority > node.right.priority) {
                    // node.left needs to end up on top
                    final Node top = rotateRight(node);
                    final Node newTopRight = removeImpl(top.right, key);
                    if (top.right != newTopRight) {
                        top.right = newTopRight;
                    }
                    return top;
                } else {
                    final Node top = rotateLeft(node);
                    final Node newTopLeft = removeImpl(top.left, key);
                    if (top.left != newTopLeft) {
                        top.left = newTopLeft;
                    }
                    return top;
                }
            }
        }
        else if (key < node.key) {
            final Node newNodeLeft = removeImpl(node.left, key);
            if (node.left != newNodeLeft) {
                node.left = newNodeLeft;
            }
            return node;
        }
        else {
            final Node newNodeRight = removeImpl(node.right, key);
            if (node.right != newNodeRight) {
                node.right = newNodeRight;
            }
            return node;
        }
    }
}
