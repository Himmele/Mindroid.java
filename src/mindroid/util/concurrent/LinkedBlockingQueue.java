/*
 * Copyright (C) 2012 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.util.concurrent;

public class LinkedBlockingQueue {
    private Node mHeadNode;

    public LinkedBlockingQueue() {
        mHeadNode = null;
    }

    public synchronized boolean put(Object item) {
        Node node = new Node(item);
        Node curNode = mHeadNode;
        if (curNode == null) {
            node.nextNode = curNode;
            mHeadNode = node;
            notify();
        } else {
            Node prevNode = null;
            while (curNode != null) {
                prevNode = curNode;
                curNode = curNode.nextNode;
            }
            node.nextNode = prevNode.nextNode;
            prevNode.nextNode = node;
            notify();
        }
        return true;
    }

    public synchronized Object take() throws InterruptedException {
        while (true) {
            Node node = getNextNode();
            if (node != null) {
                return node.item;
            } else {
                wait();
            }
        }
    }

    public synchronized boolean remove(Object item) {
        boolean foundItem = false;
        Node curNode = mHeadNode;
        // Remove all matching nodes at the front of the queue.
        while (curNode != null && curNode.item == item) {
            foundItem = true;
            Node nextNode = curNode.nextNode;
            mHeadNode = nextNode;
            curNode = nextNode;
        }

        // Remove all matching nodes after the front of the queue.
        while (curNode != null) {
            Node nextNode = curNode.nextNode;
            if (nextNode != null) {
                if (nextNode.item == item) {
                    foundItem = true;
                    Node nextButOneNode = nextNode.nextNode;
                    curNode.nextNode = nextButOneNode;
                    continue;
                }
            }
            curNode = nextNode;
        }
        return foundItem;
    }

    class Node {
        Object item;
        Node nextNode;

        Node(Object t) {
            item = t;
        }
    };

    Node getNextNode() {
        Node node = mHeadNode;
        if (node != null) {
            mHeadNode = node.nextNode;
            return node;
        }
        return null;
    }
}
