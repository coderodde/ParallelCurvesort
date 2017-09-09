package net.coderodde.util;

import java.util.Arrays;
import java.util.Random;

public final class Demo {

    private static final int ARRAY_LENGTH = 30;
    private static final int FROM_INDEX = 2;
    private static final int TO_INDEX = ARRAY_LENGTH - 3;
    private static final int PERIOD_LENGTH = 10;
    private static final int MINIMUM = -10;
    private static final int MAXIMUM = 10;
    
    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        
        int[] array1 = getWaveArray(ARRAY_LENGTH, 
                                    MINIMUM, 
                                    MAXIMUM,
                                    PERIOD_LENGTH);
        
        int[] array2 = array1.clone();
        
        ParallelCurvesort.sort(array1);
        Arrays.sort(array2);
        System.out.println(Arrays.toString(array1));
        System.out.println(Arrays.toString(array2));
        System.out.println("Algorithms agree: " + Arrays.equals(array1, 
                                                                array2));
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
