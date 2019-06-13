package com.google.android.apps.forscience.whistlepunk.blew;

import java.util.List;

public class BleObservable {

    private static List<BleObserver> observerList;

    public static void registerObserver(BleObserver observer){observerList.add(observer);}
    public static void unregisterObserver(BleObserver observer){observerList.remove(observer);}

    public static void broadcast(float value){
        for(BleObserver observer : observerList)
            observer.onValueChange(value);
    }

}
