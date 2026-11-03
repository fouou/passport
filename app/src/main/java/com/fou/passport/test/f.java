package com.fou.passport.test;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import com.google.gson.Gson;

public class f {

    private static final String BINLIST_API_URL = "https://lookup.binlist.net/";
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        String cardNumber = "622202******1234"; // 银行卡号
        String bin = cardNumber.substring(0, 6); // 提取银行卡号前 6 位作为 BIN
        String url = BINLIST_API_URL + bin;
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 请求成功
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                String json = response.toString();
                Map<String, Object> map = gson.fromJson(json, Map.class);
                String bankName = (String) map.get("bank");
                String country = (String) map.get("country");
                System.out.println("Bank Name: " + bankName);
                System.out.println("Country: " + country);
            } else {
                System.out.println("Request failed: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
        }
    }
}
