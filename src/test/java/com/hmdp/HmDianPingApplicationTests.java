package com.hmdp;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HmDianPingApplicationTests {

    @Test
    public void testCreateArray() {
        int max = Integer.MAX_VALUE;
        int[][] arr = new int[max >> 17][max >> 17];
        System.out.println(arr);
    }

}
