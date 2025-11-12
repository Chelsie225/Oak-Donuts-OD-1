package oakdonuts.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Small utilities for date formatting and transaction id generation.
 */
public class DateUtils {
    /**
     * Generate a transaction id string given an incremental number.
     * Example: OD-20251112-0001
     */
    public static String generateTransactionId(int seq) {
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return String.format("OD-%s-%04d", datePart, seq);
    }

    /** Convert current time to SQL Timestamp */
    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
