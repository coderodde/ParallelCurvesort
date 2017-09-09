package net.coderodde.util;

import java.util.Arrays;

public final class Demo {

    private static final int ARRAY_LENGTH = 20_000_000;
    private static final int FROM_INDEX = 2;
    private static final int TO_INDEX = ARRAY_LENGTH - 3;
    private static final int PERIOD_LENGTH = 10_000;
    private static final int MINIMUM = -3_000;
    private static final int MAXIMUM = 3_000;
    
    public static void main(String[] args) {
        warmup();
        benchmark();
    }
    
    private static void warmup() {
        System.out.println("Warming up...");
        int[] array = getWaveArray(ARRAY_LENGTH,
                                   MINIMUM, 
                                   MAXIMUM, 
                                   PERIOD_LENGTH);
        perform(array, false);
        System.out.println("Warming up done!");
    }
    
    private static void benchmark() {
        int[] array = getWaveArray(ARRAY_LENGTH,
                                   MINIMUM,
                                   MAXIMUM, 
                                   PERIOD_LENGTH);
        perform(array, true);
    }
    
    private static void perform(int[] array, boolean output) {
        int[] array1 = array.clone();
        int[] array2 = array.clone();
        int[] array3 = array.clone();
        
        long start = System.currentTimeMillis();
        Arrays.sort(array1, FROM_INDEX, TO_INDEX);
        long end = System.currentTimeMillis();
        
        if (output) {
            System.out.println("Arrays.sort in " + (end - start) +
                               " milliseconds.");
        }
        
        start = System.currentTimeMillis();
        Arrays.parallelSort(array2, FROM_INDEX, TO_INDEX);
        end = System.currentTimeMillis();
        
        if (output) {
            System.out.println("Arrays.parallelSort in " + (end - start) +
                               " milliseconds.");
        }
        
        start = System.currentTimeMillis();
        ParallelCurvesort.sort(array3, FROM_INDEX, TO_INDEX);
        end = System.currentTimeMillis();
        
        if (output) {
            System.out.println("ParallelCurvesort.sort in " + (end - start) + 
                               " milliseconds.");
            
            System.out.println("Algorithms agree: " + 
                    (Arrays.equals(array1, array2) &&
                     Arrays.equals(array2, array3)));
        }
    }
    
    private static int[] getWaveArray(int length,
                                      int minimum,
                                      int maximum,
                                      int periodLength) {
        int[] array = new int[length];
        int halfAmplitude = (maximum - minimum +1) / 2;
        
        for (int i = 0; i < length; ++i) {
            array[i] = generateWaveInt(i, periodLength, halfAmplitude);
        }
        
        return array;
    }
    
    private static int generateWaveInt(int i, 
                                         int periodLength, 
                                         int halfAmplitude) {
        double stage = (2.0 * Math.PI * i) / periodLength;
        return (int)(halfAmplitude * Math.sin(stage));
    }
}
