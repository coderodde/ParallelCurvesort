package net.coderodde.util;

import java.util.Objects;

public final class ParallelCurvesort {

    /**
     * Each thread should not handle less than this number of integers.
     */
    private static final int MINIMUM_INTS_PER_THREAD = 1;
    
    /**
     * This static inner class implements a node in the frequency list.
     */
    private static final class FrequencyListNode {
        
        final int integer;
        int count;
        FrequencyListNode prev;
        FrequencyListNode next;
        
        FrequencyListNode(int integer) {
            this.integer = integer;
            this.count = 1;
        }
    }
    
    private static final class ScannerThread extends Thread {
        
        private FrequencyListNode last;
        private FrequencyListNode head;
        private FrequencyListNode tail;
        private final int[] array;
        private final int fromIndex;
        private final int toIndex;
        
        ScannerThread(int[] array, int fromIndex, int toIndex) {
            this.array = array;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            
            int initialInteger = array[fromIndex];
            FrequencyListNode initialNode = 
                    new FrequencyListNode(initialInteger);
            
            this.head = initialNode;
            this.tail = initialNode;
            this.last = initialNode;
        }
        
        @Override
        public void run() {
            for (int i = fromIndex + 1; i < toIndex; ++i) {
                add(array[i]);
            }
        }
        
        FrequencyListNode getHead() {
            return head;
        }
        
        private void add(int integer) {
            if (integer < last.integer) {
                findAndUpdateSmallerNode(integer);
            } else if (integer > last.integer) {
                findAndUpdateLargerNode(integer);
            } else {
                last.count++;
            }
        }
        
        private void findAndUpdateSmallerNode(int integer) {
            FrequencyListNode tmp = last.prev;
            
            // Go down the node chain towards the nodes with smaller integers.
            while (tmp != null && tmp.integer > integer) {
                tmp = tmp.prev;
            }
            
            if (tmp == null) {
                // 'integer' is the new minimum. Create new head node and put
                // the integer in it.
                FrequencyListNode newNode = new FrequencyListNode(integer);
                newNode.next = head;
                head.prev = newNode;
                head = newNode;
                last = newNode;
            } else if (tmp.integer == integer) {
                // 'integer' already in the list. Just update the count.
                tmp.count++;
                last = tmp;
            } else {
                // Insert a new node between 'tmp' and 'tmp.next'.
                FrequencyListNode newNode = new FrequencyListNode(integer);
                newNode.prev = tmp;
                newNode.next = tmp.next;
                newNode.prev.next = newNode;
                newNode.next.prev = newNode;
                last = newNode;
            }
        }

        private void findAndUpdateLargerNode(int integer) {
            FrequencyListNode tmp = last.next;
            
            // Go up the chain towards the nodes with larger keys.
            while (tmp != null && tmp.integer < integer) {
                tmp = tmp.next;
            }
            
            if (tmp == null) {
                // 'integer' is the new maximum. Create new head node and put
                // the integer in it.
                FrequencyListNode newNode = new FrequencyListNode(integer);
                newNode.prev = tail;
                tail.next = newNode;
                tail = newNode;
                last = newNode;
            } else if (tmp.integer == integer) {
                // 'integer' already in the list. Just update the count.
                tmp.count++;
                last = tmp;
            } else {
                FrequencyListNode newNode = new FrequencyListNode(integer);
                newNode.prev = tmp.prev;
                newNode.next = tmp;
                tmp.prev.next = newNode;
                tmp.prev = newNode;
                last = newNode;
            }
        }
    }
    
    private final int[] array;
    private final int fromIndex;
    private final int toIndex;
    
    private ParallelCurvesort(int[] array, int fromIndex, int toIndex) {
        this.array = array;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }
    
    private void sort() {
        int rangeLength = toIndex - fromIndex;
        int numberOfThreads = 
                Math.min(rangeLength / MINIMUM_INTS_PER_THREAD,
                         Runtime.getRuntime().availableProcessors());
        
        numberOfThreads = Math.max(numberOfThreads, 1);
        numberOfThreads = ceilToPowerOfTwo(numberOfThreads);
        
        ScannerThread[] scannerThreads = new ScannerThread[numberOfThreads - 1];
        int threadRangeLength = rangeLength / numberOfThreads;
        int startIndex = fromIndex;
        
        for (int i = 0; 
                i < numberOfThreads - 1; 
                i++, startIndex += threadRangeLength) {
            scannerThreads[i] = 
                    new ScannerThread(array,
                                      startIndex,
                                      startIndex + threadRangeLength);
            
            scannerThreads[i].start();
        }
        
        ScannerThread thisThread = new ScannerThread(array, 
                                                     startIndex, 
                                                     toIndex);
        thisThread.run();
        
        for (ScannerThread thread : scannerThreads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException("A thread was interrupted.", ex);
            }
        }
        
        FrequencyListNode[] listHeads = new FrequencyListNode[numberOfThreads];
        
        for (int i = 0; i < scannerThreads.length; ++i) {
            listHeads[i] = scannerThreads[i].getHead();
        }
        
        listHeads[listHeads.length - 1] = thisThread.getHead();
        FrequencyListNode mergedListHead = mergeLists(listHeads);
        dump(mergedListHead, array, fromIndex);
    }
    
    private static int ceilToPowerOfTwo(int number) {
        int ret = 1;
        
        while (ret < number) {
            ret <<= 1;
        }
        
        return ret;
    }
    
    private static void dump(FrequencyListNode head, 
                             int[] array, 
                             int fromIndex) {
        for (FrequencyListNode node = head; node != null; node = node.next) {
            int integer = node.integer;
            int count = node.count;
            
            for (int i = 0; i != count; ++i) {
                array[fromIndex++] = integer;
            }
        }
    }
    
    private static FrequencyListNode mergeLists(FrequencyListNode[] heads) {
        return mergeLists(heads, 0, heads.length);
    }
    
    private static FrequencyListNode mergeLists(FrequencyListNode[] heads,
                                                int fromIndex,
                                                int toIndex) {
        int lists = toIndex - fromIndex;
        
        if (lists == 1) {
            return heads[fromIndex];
        }
        
        if (lists == 2) {
            return mergeLists(heads[fromIndex], heads[fromIndex + 1]);
        }
        
        int middleIndex = lists / 2;
        
        return mergeLists(mergeLists(heads, fromIndex, middleIndex),
                          mergeLists(heads, middleIndex, toIndex));
    }
    
    private static FrequencyListNode mergeLists(FrequencyListNode head1,
                                                FrequencyListNode head2) {
        FrequencyListNode initialNode;
        
        if (head1.integer < head2.integer) {
            initialNode = head1;
            head1 = head1.next;
        } else if (head1.integer > head2.integer) {
            initialNode = head2;
            head2 = head2.next;
        } else {
            initialNode = head1;
            initialNode.count += head2.count;
            head1 = head1.next;
            head2 = head2.next;
        }
        
        FrequencyListNode newHead = initialNode;
        FrequencyListNode newTail = initialNode;
        
        while (head1 != null && head2 != null) {
            if (head1.integer < head2.integer) {
                newTail.next = head1;
                newTail = head1;
                head1 = head1.next;
            } else if (head1.integer > head2.integer) {
                newTail.next = head2;
                newTail = head2;
                head2 = head2.next;
            } else {
                FrequencyListNode nextHead1 = head1.next;
                FrequencyListNode nextHead2 = head2.next;
                newTail.next = head1;
                newTail = head1;
                newTail.count += head2.count;
                head1 = nextHead1;
                head2 = nextHead2;
            }
        }
        
        if (head1 != null) {
            newTail.next = head1;
            newTail = head1;
        } else if (head2 != null) {
            newTail.next = head2;
            newTail = head2;
        }
        
        newTail.next = null;
        return newHead;
    }
    
    public static void sort(int[] array) {
        Objects.requireNonNull(array, "The input array is null.");
        sort(array, 0, array.length);
    }
    
    public static void sort(int[] array, int fromIndex, int toIndex) {
        Objects.requireNonNull(array, "The input array is null.");
        rangeCheck(array.length, fromIndex, toIndex);
        new ParallelCurvesort(array, fromIndex, toIndex).sort();
    }
    
    /**
     * Checks that {@code fromIndex} and {@code toIndex} are in the range and
     * throws an exception if they aren't.
     */
    private static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }

        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }

        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }
}
