package com.appyhigh.newsfeedsdk.encryption;

import static com.appyhigh.newsfeedsdk.Constants.AUTHORIZATION;
import static com.appyhigh.newsfeedsdk.Constants.COUNTRY_CODE;
import static com.appyhigh.newsfeedsdk.Constants.FEED_TYPE;
import static com.appyhigh.newsfeedsdk.Constants.INTERESTS;
import static com.appyhigh.newsfeedsdk.Constants.PAGE_NUMBER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appyhigh.newsfeedsdk.FeedSdk;
import com.appyhigh.newsfeedsdk.apicalls.ApiCreateOrUpdateUser;
import com.appyhigh.newsfeedsdk.apicalls.ResponseListener;
import com.appyhigh.newsfeedsdk.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;


public class AuthSocket {
    private static final String TAG = AuthSocket.class.getSimpleName();

    //Native SDK Get Value of String
    //These Details can be captured from Main App Also
    public interface INTENT_CONSTANTS {
        String AUTH_USER_NAME = "UserName";
        String AUTH_USER_NUMBER = "UserNumber";
        String AUTH_USER_ID = "UserID";
        String AUTH_USER_EMAIL = "UserEmail";
        String AUTH_USER_FCM = "UserFCM";

        String AUTH_CUSTOM_1 = "custom_1";
        String AUTH_CUSTOM_2 = "custom_2";

        String AUTH_USER_LATITUDE = "UserLatitude";
        String AUTH_USER_LONGITUDE = "UserLongitude";
    }

    public interface CoLabSocketListener {
        void onConfigResponse(Object[] args);
    }

    private Context mContext = null;

    @Nullable
    private String license = null;

    //Socket Response to be Configured
    public static final String SOCKET_EMIT_CONFIG_REQUEST = "request";
    public static final String SOCKET_EMIT_CONFIG_RESPONSE = "response";
    public static final String SOCKET_EMIT_CONFIG_ERROR_RESPONSE = "error_response";
    private boolean started = false;
    private AuthenticationSuccess authenticationSuccess;
//    private ResponseListener responseListener;

    //Socket Server
    private final String SERVER_IP = "https://authencrypt.apyhi.com";
    private final String SERVER_PATH = "/auth/socket.io";

    //Authentication Encryption
    public AESCBCPKCS5Encryption instanceEncryption;
    private Socket mSocket = null;

    public Context getmContext() {
        return mContext;
    }

    private static AuthSocket mAuthSocketObj = null;
    private static CoLabSocketListener mAuthSocketListener = null;
    private String encryptionKey;

    public static AuthSocket Instance() {
        if (mAuthSocketObj == null) {
            mAuthSocketObj = new AuthSocket();
        }

        return mAuthSocketObj;
    }

    public static AuthSocket Instance(CoLabSocketListener coLabSocketListener) {
        mAuthSocketListener = coLabSocketListener;
        return Instance();
    }

    public AuthSocket() {

    }


    @NonNull
    public String license() {
        if (this.license == null)
            return "";
        return this.license;
    }

    public synchronized void license(@NonNull String license) {
        boolean changed = !license.equals(this.license);
        if (changed && this.started)
            throw new RuntimeException("Error: cannot change license once Auth Session is started");
        this.license = license;
    }

    public void start(Context context, AuthenticationSuccess authenticationSuccess) {
        try {
            this.authenticationSuccess = authenticationSuccess;
            if (this.license == null || this.license.equals("")) {
                Log.e("Auth Session", "You must set a license before calling start(...)");
                return;
            }
            this.started = true;
            mContext = context;

            SessionUser.Instance().setAppId("1941598539");
            SessionUser.Instance().setSdkId("com.appyhigh.mylibrary");

            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            SessionUser.Instance().setApp_versionCode(String.valueOf(info.versionCode));
            SessionUser.Instance().setApp_versionName(String.valueOf(info.versionName));
            SessionUser.Instance().setManufacturer(Build.MANUFACTURER);
            SessionUser.Instance().setDevice_model(Build.MODEL);
            SessionUser.Instance().setOs_version(Build.VERSION.BASE_OS);


            String SHA1 = getSHA1(AuthSocket.Instance().getmContext(), "SHA1");
            String NativeKey = String.valueOf("TmF0aXZlNWVjcmV0UEBzc3cwcmQx");
            byte[] decodedBytes = Base64.decode(NativeKey, Base64.DEFAULT);
            String decodedString = new String(decodedBytes);

            LogDetail.LogDE("Actual Value from Native Key", String.valueOf("TmF0aXZlNWVjcmV0UEBzc3cwcmQx"));

            LogDetail.LogDE("License", license);

            LogDetail.LogDE("APP ID", SessionUser.Instance().getAppId());
            LogDetail.LogDE("SDK ID", SessionUser.Instance().getSdkId());
            //Predetermined Values in SDK Session
            LogDetail.LogDE("Version Code", String.valueOf(SessionUser.Instance().getVersionCode()));
            LogDetail.LogDE("Version Name", SessionUser.Instance().getVersionName());
            LogDetail.LogDE("Native Decrypted", decodedString);
            LogDetail.LogDE("SHA1", SHA1);

            //Another Key can Be configured in Remote Config of Firebase - Challenge it would have to come from Client Firebase
            String encryptionKey = String.valueOf(SessionUser.Instance().getVersionCode()).trim() + SessionUser.Instance().getVersionName().trim() + decodedString.trim() + SHA1.trim();
            LogDetail.LogDE("encryptionKey", encryptionKey);
            this.encryptionKey = encryptionKey;
            instanceEncryption = new AESCBCPKCS5Encryption().getInstance(encryptionKey.trim());
            instanceEncryption.updateKEY_IV(encryptionKey.trim());
            LogDetail.LogD(TAG, " initializeSocket " + SERVER_IP);

            clearSocket();

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new RelaxedHostNameVerifier())
                    .sslSocketFactory(sc.getSocketFactory(), tmr)
                    .build(); // default settings for all sockets

            IO.setDefaultOkHttpWebSocketFactory((WebSocket.Factory) okHttpClient);
            IO.setDefaultOkHttpCallFactory((Call.Factory) okHttpClient);
            //IO.setDefaultSSLContext(sc);
            //HttpsURLConnection.setDefaultHostnameVerifier(new RelaxedHostNameVerifier());

            IO.Options opts = new IO.Options();
            opts.forceNew = false;
            opts.reconnection = true;
            opts.reconnectionDelay = 3000;
            opts.reconnectionDelayMax = 6000;
            opts.reconnectionAttempts = 9999;
            //opts.sslContext = sc;
            opts.callFactory = (Call.Factory) okHttpClient;
            opts.webSocketFactory = (WebSocket.Factory) okHttpClient;
            opts.secure = true;
            opts.upgrade = true;
            opts.path = SERVER_PATH;
            String[] transports = {io.socket.engineio.client.transports.WebSocket.NAME};
            opts.transports = transports;
            mSocket = IO.socket(SERVER_IP, opts);
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            //mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeOut);
            mSocket.on(AuthSocket.SOCKET_EMIT_CONFIG_REQUEST, onConfigRequest);
            mSocket.on(AuthSocket.SOCKET_EMIT_CONFIG_RESPONSE, onConfigResponse);
            mSocket.on(AuthSocket.SOCKET_EMIT_CONFIG_ERROR_RESPONSE, onConfigError);
            mSocket.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void clearSocket() {
        if (mSocket != null) {
            if (mSocket.connected()) {
                mSocket.disconnect();
            }

            mSocket = null;
        }
    }

    //GET SHA1 Key for Application
    static String getSHA1(Context context, String key) {
        try {

            @SuppressLint("PackageManagerGetSignatures") final PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance(key);
                md.update(signature.toByteArray());

                final byte[] digest = md.digest();

                final StringBuilder toRet = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i != 0) toRet.append(":");
                    int b = digest[i] & 0xff;
                    String hex = Integer.toHexString(b);
                    if (hex.length() == 1) toRet.append("0");
                    toRet.append(hex);
                }

                /*LogDetail.LogDE(TAG, key + " " + Base64.encodeToString(md.digest(), Base64.DEFAULT));
                LogDetail.LogDE(TAG, key + " " + toRet.toString());*/
                return Base64.encodeToString(md.digest(), Base64.DEFAULT);
            }
        } catch (PackageManager.NameNotFoundException e1) {
            LogDetail.LogDE("name not found", e1.toString());
            return "";
        } catch (NoSuchAlgorithmException e) {
            LogDetail.LogDE("no such an algorithm", e.toString());
            return "";
        } catch (Exception e) {
            LogDetail.LogDE("exception", e.toString());
            return "";
        }
        return "";
    }


    //API Calling Using Encrypted Data
    public void postData(String sendingData, com.appyhigh.newsfeedsdk.apicalls.ResponseListener responseListener) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("data", sendingData)
                .build();
        LogDetail.LogD("auth-token", SessionUser.Instance().getToken());
        Request request = new Request.Builder()
                .url("https://encrypt.apyhi.com/list/data")   //URL
                .header("auth-token", SessionUser.Instance().getToken())
                .post(formBody)
                .build();


        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                responseListener.onError(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                LogDetail.LogD("API RESPONSE - ", response.toString());
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        String resStr = response.body().string();
                        JSONObject rb = new JSONObject(resStr);
                        LogDetail.LogDE("TAG_", rb.toString());
                        String[] SplitData = rb.getString("data").split("\\.", 2);
                        String publickey = SplitData[1];
                        AESCBCPKCS5Encryption instanceEncryption = new AESCBCPKCS5Encryption().getInstance(SessionUser.Instance().getPrivateKey(publickey));
                        String DecryptedText = instanceEncryption.decrypt(SplitData[0].getBytes(StandardCharsets.UTF_8));
                        LogDetail.LogD("Decrypted Response  => ", DecryptedText);
                        JSONObject RespJson = new JSONObject(DecryptedText);
                        LogDetail.LogD("Decrypted Response API URL  => ", RespJson.getString("apiURL"));
                        JSONObject encryptionData = new JSONObject(String.valueOf(RespJson.getJSONObject("encryptionData")));
                        SessionUser.Instance().addPairToMap(encryptionData.getString("publicKey"), encryptionData.getString("privateKey"));
                        SessionUser.Instance().setPublicKey(encryptionData.getString("publicKey"));
                        SessionUser.Instance().setToken(encryptionData.getString("token"));

                        new Handler(Looper.getMainLooper()).post(() -> {
                            JSONObject dataJSON;
                            try {
                                dataJSON = new JSONObject(String.valueOf(RespJson.getJSONObject("data")));
                                responseListener.onSuccess(RespJson.getString("apiURL"), dataJSON);
                            } catch (JSONException e) {
                            }
                        });

                        new Handler(Looper.getMainLooper()).post(() -> {
                            JSONArray dataArray;
                            try {
                                dataArray = RespJson.getJSONArray("data");
                                responseListener.onSuccess(RespJson.getString("apiURL"), dataArray);
                            } catch (JSONException e) {
                            }
                        });


                        new Handler(Looper.getMainLooper()).post(() -> {
                            String dataString;
                            try {
                                dataString = String.valueOf(RespJson.getString("data"));
                                responseListener.onSuccess(RespJson.getString("apiURL"), dataString);
                            } catch (JSONException e) {
                            }
                        });

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                }else {
                    Log.d(TAG, "onResponse: "+call.request());
                }
            }

        });
    }

    //Socket Emitters
    Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD("SERVER_IP", SERVER_IP);
            LogDetail.LogD(TAG, " Connect " + new Gson().toJson(args));

            JsonObject allDetails = new JsonObject();

            try {
                allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
                allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Data to be converted to string
            LogDetail.LogDE("DATA - > ", allDetails.toString());

            //Encrypting String
            String initialIEncryptionString = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + license;
            LogDetail.LogDE("ENCRYPTED TEXT - ", initialIEncryptionString);

            //Emitting socket -- Config Request
            mSocket.emit(SOCKET_EMIT_CONFIG_REQUEST, initialIEncryptionString);

        }
    };

    Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " Disconnect " + new Gson().toJson(args));
            if (getSocket() != null) {
                getSocket().disconnect();
            }
        }
    };

    Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " Error " + new Gson().toJson(args));
            //Toast.makeText(CoLabFileProvider.Instance().getmContext(), "AUTH_CONNECT_ERROR", Toast.LENGTH_SHORT).show();
            if (getSocket() != null) {
                getSocket().disconnect();
            }
        }
    };

    Emitter.Listener onConnectTimeOut = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " TimeOut " + new Gson().toJson(args));
        }
    };

    Emitter.Listener onConfigRequest = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " ConfigRequest ");

        }
    };

    Emitter.Listener onConfigResponse = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " ConfigResponse ");

            String encryptedResponse = args[0].toString();//new Gson().toJson(args[0]).replaceAll("('|\")", "");

            String[] data = encryptedResponse.split("\\.", 2);

            if (data[1].equals(license)) {
                LogDetail.LogD(TAG, " Equals LICENSE KEY ");
                LogDetail.println(data[0]);

                String initialIDecryptionString = instanceEncryption.decrypt(data[0].getBytes());

                LogDetail.LogDE(TAG + "--dec", initialIDecryptionString);

                try {
                    JSONObject jsonObject = new JSONObject(initialIDecryptionString);
                    //EVENTNAME - Can be used for VERSION CONTROL
                    String eventName = jsonObject.getString("eventName");

                    //TODO Manage Version Response
                    switch (eventName) {
                        case "versionSuccess":
                            LogDetail.LogDE("Version - ", "This is Latest");
                            break;
                        case "versionCanBeUpdated":
                            LogDetail.LogDE("Version - ", "This version can be updated");
                            break;
                        case "versionHasToBeUpdated":
                            LogDetail.LogDE("Version - ", "This version Has to be updated");
                            break;
                        case "versionSystemMaintenance":
                            LogDetail.LogDE("Version - ", "Server is Under Maintenance");
                            break;
                        case "versionNoLongerSupported":
                            LogDetail.LogDE("Version - ", "This Version is no Longer Supported");
                            break;
                        default:
                            LogDetail.LogDE("Version - ", "This version is Not compatible");
                            break;
                    }

                    String versionResponse = String.valueOf(jsonObject.getJSONObject("versionResponse"));
                    String encryptionData = String.valueOf(jsonObject.getJSONObject("encryptionData"));

                    //This Object can be used in case of EventNames other than versionSuccess
                    JSONObject versionObject = new JSONObject(versionResponse);

                    JSONObject encryptionObject = new JSONObject(encryptionData);

                    instanceEncryption.updateKEY_IV(encryptionObject.getString("privateKey"));
                    SessionUser.Instance().setPublicKey(encryptionObject.getString("publicKey"));
                    SessionUser.Instance().setToken(encryptionObject.getString("token"));
                    SessionUser.Instance().addPairToMap(encryptionObject.getString("publicKey"), encryptionObject.getString("privateKey"));
                    authenticationSuccess.onAuthSuccess();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            if (mAuthSocketListener != null) {
                mAuthSocketListener.onConfigResponse(args);
            }
        }
    };

    public interface ResponseListener {
        void responseListener(String apiUrl, JSONObject response);

        void responseListener(String apiUrl, JSONArray response);

        void responseListener(String apiUrl, String response);
    }

//    public void setResponseListener(ResponseListener responseListener) {
//        if (this.responseListener == null)
//            this.responseListener = responseListener;
//    }

    public void makeEncryptedAPICallForUpdateUser(String apiUrl, String firebaseToken, String sdkCountryCode, User user, String token) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", "POST");
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
//        dataJO.addProperty("lang", "hi");
        dataJO.addProperty("push_token", firebaseToken);
        dataJO.addProperty("country_code", sdkCountryCode);
        dataJO.addProperty("first_name", user.getFirstName());
        dataJO.addProperty("last_name", user.getLastName());
        dataJO.addProperty("email", user.getEmail());
        dataJO.addProperty("phone_number", user.getPhoneNumber());
        dataJO.addProperty("dailling_code", user.getDailling_code());
        if (user.getUsername().isEmpty() || !FeedSdk.Companion.isExistingUser()) {
            dataJO.addProperty("username", user.getUsername());
        }
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        headerJO.addProperty(AUTHORIZATION, token);
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }


    public void makeEncryptedAPICallForFeed(String apiUrl, String token, String interests, String pageNo, String feedType) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", SessionUser.Instance().getApiMethod());
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
        dataJO.addProperty(INTERESTS, interests);
        dataJO.addProperty(PAGE_NUMBER, pageNo);
        dataJO.addProperty(COUNTRY_CODE, "in");
        dataJO.addProperty(INTERESTS, interests);
        dataJO.addProperty(FEED_TYPE, feedType);
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        headerJO.addProperty(AUTHORIZATION, token);
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }

    public void makeEncryptedAPICallForInterestsAppWise(String apiUrl, String token, String interests) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", SessionUser.Instance().getApiMethod());
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
        dataJO.addProperty(INTERESTS, interests);
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        headerJO.addProperty(AUTHORIZATION, token);
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }

    public void makeEncryptedAPICallForUserDetails(String apiUrl, String token) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", SessionUser.Instance().getApiMethod());
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        headerJO.addProperty(AUTHORIZATION, token);
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }

    public void makeEncryptedAPICallForLanguages(String apiUrl) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", SessionUser.Instance().getApiMethod());
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
        dataJO.addProperty("country_code", "in");
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }

    public void makeEncryptedAPICallForInterests(String apiUrl) {
        //Preparing data for request
        JsonObject allDetails = new JsonObject();
        JsonObject main = new JsonObject();
        main.addProperty("apiURL", apiUrl);
        main.addProperty("apiMethod", SessionUser.Instance().getApiMethod());
        main.addProperty("apiInternal", SessionUser.Instance().getApiInternal());

        JsonObject dataJO = new JsonObject();
        dataJO.addProperty("lang", "hi");
        main.add("apiData", dataJO);

        JsonObject headerJO = new JsonObject();
        //ADD Header if required
        main.add("apiHeader", headerJO);
//        LogDetail.LogDE("Test Data", main.toString());

        try {
            allDetails.add("apiCalling", main);
            allDetails.add("userDetail", SessionUser.Instance().getUserDetails());
            allDetails.add("deviceDetail", SessionUser.Instance().getDeviceDetails());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String SendingData = instanceEncryption.encrypt(allDetails.toString().getBytes(StandardCharsets.UTF_8)) + "." + SessionUser.Instance().getPublicKey();
        LogDetail.LogD("Data to be Sent -> ", SendingData);

//        postData(SendingData);
    }

    Emitter.Listener onConfigError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            LogDetail.LogD(TAG, " ConfigErrorResponse " + new Gson().toJson(args));
            if (mAuthSocketListener != null) {
                mAuthSocketListener.onConfigResponse(args);
            }
        }
    };

    @SuppressLint("CustomX509TrustManager")
    X509TrustManager tmr = new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }

        @SuppressLint("TrustAllX509TrustManager")
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
        }
    };

    private final TrustManager[] trustAllCerts = new TrustManager[]{tmr};

    public class RelaxedHostNameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public interface AuthenticationSuccess {
        void onAuthSuccess();
    }
}
