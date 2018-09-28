package com.dantou.util;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.dantou.model.Point;

/**
 * @author cinhori
 * @date 18-9-27
 * @email lilei93s@163.com
 * @Description
 */
public class CoordinateConvert {

    public static LatLng getLatLng(Point point){
        //转换工具初始化
        CoordinateConverter converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
        return converter.coord(new LatLng(point.getLatitude(), point.getLongitude())).convert();
    }
}
