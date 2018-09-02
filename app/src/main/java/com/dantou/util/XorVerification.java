package com.dantou.util;

import android.util.Log;

/**
 * @author cinhori
 * @date 18-8-29
 * @email lilei93s@163.com
 * @Description 16进制字符串校验码计算工具类
 */
public class XorVerification {

    //23个字节，16进制
    public static String getChecksum(String para){
        String[] dateArr = new String[para.length()/2];
        try {
            for(int i = 0; i < para.length()/2; ++i){
                dateArr[i] = para.substring(i*2, i*2+2);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        String code = "";
        for (int i = 0; i < dateArr.length-1; i++) {
            if(i == 0){
                code = xor(dateArr[i], dateArr[i+1]);
            }else{
                code = xor(code, dateArr[i+1]);
            }
        }
        //Log.d("Checksum", code);
        if (code.length() == 1) return "0" + code;
        else return code;
    }

    private static String xor(String strHex_X,String strHex_Y){
        //将x、y转成二进制形式
        String anotherBinary=Integer.toBinaryString(Integer.valueOf(strHex_X,16));
        String thisBinary=Integer.toBinaryString(Integer.valueOf(strHex_Y,16));
        String result = "";
        //判断是否为8位二进制，否则左补零
        if(anotherBinary.length() != 8){
            for (int i = anotherBinary.length(); i <8; i++) {
                anotherBinary = "0"+anotherBinary;
            }
        }
        if(thisBinary.length() != 8){
            for (int i = thisBinary.length(); i <8; i++) {
                thisBinary = "0"+thisBinary;
            }
        }
        //异或运算
        for(int i=0;i<anotherBinary.length();i++){
            //如果相同位置数相同，则补0，否则补1
            if(thisBinary.charAt(i)==anotherBinary.charAt(i))
                result += "0";
            else{
                result += "1";
            }
        }

        return Integer.toHexString(Integer.parseInt(result, 2));
    }


    public static void main(String[] args){
        String s = "05008213000F7B0F0100AE4049002EC29B12081D0E2212";
        //String s = "1312f70f900168d900007df57b4884";
        String checkSum = getChecksum(s);
        System.out.println(checkSum);
        System.out.println("a8".toUpperCase());

    }
}
