package com.demo.job.core.util;

import com.demo.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class XxlJobRemotingUtil {

    private static Logger logger = LoggerFactory.getLogger(XxlJobRemotingUtil.class);

    public static final String XXL_JOB_ACCESS_TOKEN = "XXL_JOB_ACCESS_TOKEN";

    //trust https start
    public static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            SSLSocketFactory socketFactory = sc.getSocketFactory();
            connection.setSSLSocketFactory(socketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }

    public static TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }};

    public static ReturnT postBody(String url, String accessToken, int timeout, Object requestObj, Class returnTargClassOfT) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;

        try {
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            boolean useHttps = url.startsWith("https");
            if (useHttps) {
                trustAllHosts((HttpsURLConnection) connection);
            }
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            if (accessToken != null && accessToken.trim().length() > 0) {
                connection.setRequestProperty(XXL_JOB_ACCESS_TOKEN, accessToken);
            }
            connection.connect();

            if (requestObj != null) {
                String requestBody = GsonTool.toJson(requestObj);
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            //valid status code
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting fail, StatusCode(" + statusCode + ") invalid. for url : " + url);
            }
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            String resultJson = sb.toString();
            ReturnT returnT = GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
            return returnT;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting error(" + e.getMessage() + "), for url : " + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
    }
}
