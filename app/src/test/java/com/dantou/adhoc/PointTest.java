package com.dantou.adhoc;

import com.dantou.model.Point;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author cinhori
 * @date 18-9-25
 * @email lilei93s@163.com
 * @Description
 */
public class PointTest {
    @Test
    public void toPoint() throws ParseException {
        String s = "05 00 82 13 00 01 00 0F 01 00 AE 40 0C 00 2E C2 27 12 08 1B 13 05 38 9D";
        Point point = Point.parse(s.replaceAll(" ", ""));
        Date date = point.getDate();
        System.out.println(date.toString());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String temp = simpleDateFormat.format(date);
        System.out.println(temp);
        System.out.println(simpleDateFormat.parse(temp));
        System.out.println(point);
    }
}
