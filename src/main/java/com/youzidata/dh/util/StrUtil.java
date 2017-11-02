package com.youzidata.dh.util;

import java.util.List;

public class StrUtil {

    public static String join(List<String> list, String tag) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(tag);
            }
            sb.append(list.get(i).trim());
        }
        return sb.toString();
    }

}
