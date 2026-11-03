package com.fou.passport.until;

public class Log {
    //TODO Turn off logging
    private static boolean log=true;//Turn on or turn off logging.
    private static String MyAutofillService = "MyAutofillService";
    private static String ListAdapter = "ListAdapter";
    private static String MainActivity = "MainActivityy";
    private static String SaveDataToDatabase = "SaveDataToDatabase";
    private static String ClientParser="ClientParser";
    private static String TypeClass="TypeClass";
    private static String ResponseAdapter="ResponseAdapter";
    private static String NewItemm="NewItemm";
    private static String ModalBottomSheet="ModalBottomSheet";
    private static String DetailActivityy="DetailActivityy";
    private static String AdapterBottomSheet="AdapterBottomSheet";
    private static String AuthActivityy="AuthActivityy";


    public static void MyAutofillService(String message) {
        if (log) {
            android.util.Log.d(MyAutofillService, message);
        }
    }

    public static void ListAdapter(String message) {
        if (log) {
            android.util.Log.d(ListAdapter, message);
        }
    }

    public static void MainActivityy(String message) {
        if (log) {
            android.util.Log.d(MainActivity, message);
        }
    }

    public static void SaveDataToDatabase(String message) {
        if (log) {
            android.util.Log.d(SaveDataToDatabase, message);
        }
    }

    public static void ClientParser(String message) {
        if (log) {
            android.util.Log.d(ClientParser, message);
        }
    }

    public static void TypeClass(String message) {
        if (log) {
            android.util.Log.d(TypeClass, message);
        }
    }

    public static void ResponseAdapter(String message) {
        if (log) {
            android.util.Log.d(ResponseAdapter, message);
        }
    }
    public static void NewItemm(String message) {
        if (log) {
            android.util.Log.d(NewItemm, message);
        }
    }

    public static void ModalBottomSheet(String message) {
        if (log) {
            android.util.Log.d(ModalBottomSheet, message);
        }
    }

    public static void DetailActivityy(String message) {
        if (log) {
            android.util.Log.d(DetailActivityy, message);
        }
    }

    public static void AdapterBottomSheet(String message) {
        if (log) {
            android.util.Log.d("AdapterBottomSheet", message);
        }
    }

    public static void AuthActivityy(String message) {
        if (log) {
            android.util.Log.d("AuthActivityy", message);
        }
    }

    public static void Special_1(String message) {
        if (log) {
            android.util.Log.d("Special_1", message);
        }
    }

    public static void Special_2(String message) {
        if (log) {
            android.util.Log.d("Special_2", message);
        }
    }




}
