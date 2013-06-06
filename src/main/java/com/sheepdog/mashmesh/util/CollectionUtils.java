package com.sheepdog.mashmesh.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionUtils {
    public static <T> List<T>listOfIterator(Iterator<T> iterator) {
        List<T> newList = new ArrayList<T>();

        while (iterator.hasNext()) {
            newList.add(iterator.next());
        }

        return newList;
    }
}
