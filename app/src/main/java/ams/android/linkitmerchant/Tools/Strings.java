package ams.android.linkitmerchant.Tools;

/**
 * Created by Aidin on 2/6/2015.
 */

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;


public class Strings {

    static String UTF8 = "utf-8";

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static <T> String join(Collection<T> coll, String separator,
                                  String terminator) {
        return join(coll.toArray(new Object[coll.size()]), separator,
                terminator);
    }

    public static String join(Object[] arr, String separator, String terminator) {
        StringBuilder sb = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) {
                sb.append(separator);
            } else if (terminator != null && arr.length > 0) {
                sb.append(terminator);
            }
        }
        return sb.toString();
    }

    public static final String SHA1 = "SHA-1";
    public static final String MD5 = "MD5";

    public static String getHash(String str, String algorithm, int length)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = str.getBytes(UTF8);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(bytes);
        BigInteger bigInt = new BigInteger(1, digest);
        String hash = bigInt.toString(16);
        while (hash.length() < length) {
            hash = "0" + hash;
        }
        return hash;
    }


}
